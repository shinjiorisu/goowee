/*
 * Copyright 2021 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package goowee.commons.http

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.apache.hc.client5.http.classic.methods.*
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.HttpEntityContainer
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.apache.hc.core5.util.Timeout

import javax.net.ssl.SSLContext

/**
 * Utility class for executing HTTP requests using Apache HttpClient 5.
 * We use Apache HTTP Client since it is compatible with SAP Cloud SDK
 * (https://sap.github.io/cloud-sdk/).
 *
 * <p>This class provides static methods to perform standard REST operations:
 * GET, POST, PUT, PATCH, DELETE. It supports JSON payloads and responses,
 * and allows setting custom headers such as authorization tokens.</p>
 *
 * <p>Typical usage:
 * <pre>{@code
 * def client = HttpClient.create(30)
 * def headers = ['Authorization': 'Bearer myToken']
 * def response = HttpClient.get(client, "https://api.example.com/data", headers)
 *}</pre>
 * </p>
 *
 * <p>Features:
 * <ul>
 *   <li>Configurable timeouts (connection request and response) via `create()`</li>
 *   <li>Automatic JSON serialization/deserialization for request/response bodies</li>
 *   <li>Support for custom headers including Authorization tokens</li>
 *   <li>Exception handling for non-2xx HTTP responses</li>
 * </ul>
 * </p>
 *
 * <p>This class is designed for general-purpose REST API interactions
 * and is not tied to any specific backend or service.</p>
 *
 * @author Gianluca Sartori
 */
@CompileStatic
class HttpClient {

    /**
     * Creates a reusable CloseableHttpClient with configurable timeouts.
     *
     * @param timeoutSeconds Timeout in seconds for connection request and response
     * @return CloseableHttpClient ready for use
     */
    static CloseableHttpClient create(Integer timeoutSeconds = 30, Boolean forceCertificateVerification = false) {
        return forceCertificateVerification
                ? createValidCertHttpClient(timeoutSeconds)
                : createNoCertHttpClient(timeoutSeconds)

    }

    private static CloseableHttpClient createValidCertHttpClient(Integer timeoutSeconds) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(timeoutSeconds))
                .setResponseTimeout(Timeout.ofSeconds(timeoutSeconds))
                .build()

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()
    }

    private static CloseableHttpClient createNoCertHttpClient(Integer timeoutSeconds) {
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                .build()

        DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(
                sslContext,
                HostnameVerificationPolicy.BOTH,
                NoopHostnameVerifier.INSTANCE
        )

        PoolingHttpClientConnectionManager connManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy)
                .build()

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(timeoutSeconds))
                .setResponseTimeout(Timeout.ofSeconds(timeoutSeconds))
                .build()

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connManager)
                .build()

        return httpClient
    }

    private static HttpUriRequestBase buildHttpRequest(String baseUrl, HttpRequest request) {
        String url = request.buildUrl(baseUrl)

        HttpUriRequestBase httpRequest
        switch (request.method) {
            case HttpMethod.GET: httpRequest = new HttpGet(url); break
            case HttpMethod.POST: httpRequest = new HttpPost(url); break
            case HttpMethod.PUT: httpRequest = new HttpPut(url); break
            case HttpMethod.PATCH: httpRequest = new HttpPatch(url); break
            case HttpMethod.DELETE: httpRequest = new HttpDelete(url); break
            default: throw new IllegalArgumentException("Unsupported HTTP method: ${request.method}")
        }

        // Headers
        request.headers.each { k, v -> httpRequest.setHeader(k, v) }
        if (!request.headers.containsKey("Accept")) {
            httpRequest.setHeader("Accept", "application/json")
        }

        // Body
        if ((httpRequest instanceof HttpEntityContainer) && request.body != null) {
            def entityContainer = httpRequest as HttpEntityContainer

            if (request.body instanceof HttpEntity) {
                // HttpEntity, no need to configure the content type
                entityContainer.entity = request.body as HttpEntity

            } else if (request.body instanceof String) {
                // Generic string payload
                ContentType contentType = request.headers.containsKey("Content-Type")
                        ? ContentType.parse(request.headers["Content-Type"])
                        : ContentType.TEXT_PLAIN.withCharset("UTF-8")
                entityContainer.entity = new StringEntity(request.body as String, contentType)

            } else {
                // Oggetto generico -> serializza in JSON
                ContentType contentType = request.headers.containsKey("Content-Type")
                        ? ContentType.parse(request.headers["Content-Type"])
                        : ContentType.APPLICATION_JSON.withCharset("UTF-8")
                entityContainer.entity = new StringEntity(JsonOutput.toJson(request.body), contentType)
            }
        }

        return httpRequest
    }

    static String callAsString(CloseableHttpClient client, String baseUrl, HttpRequest request) {
        def httpRequest = buildHttpRequest(baseUrl, request)

        return client.execute(httpRequest) { response ->
            int status = response.code
            String bodyText = response.entity ? EntityUtils.toString(response.entity) : ""
            if (status >= 200 && status < 300) return bodyText
            else throw new Exception("REST API Error: HTTP ${status} - ${bodyText}")
        }
    }

    static Map callAsJson(CloseableHttpClient client, String baseUrl, HttpRequest request) {
        String responseText = callAsString(client, baseUrl, request)
        if (!responseText) {
            return [:]
        }

        try {
            return new JsonSlurper().parseText(responseText) as Map
        } catch (Exception ignore) {
            return [:]
        }
    }

    static byte[] callAsByteArray(CloseableHttpClient client, String baseUrl, HttpRequest request) {
        def httpRequest = buildHttpRequest(baseUrl, request)

        if (!request.headers.containsKey("Accept")) {
            httpRequest.setHeader("Accept", "*/*")
        }

        return client.execute(httpRequest) { response ->
            int status = response.code
            byte[] bytes = response.entity ? EntityUtils.toByteArray(response.entity) : new byte[0]
            if (status >= 200 && status < 300) return bytes
            else {
                String errorText = ""
                try {
                    errorText = new String(bytes)
                } catch (ignored) {
                }
                throw new Exception("API Call Error: HTTP ${status} - ${errorText}")
            }
        }
    }


    /**
     * Parses a JSON string into a Map.
     * <p>
     * Useful for converting the string response of HTTP calls into structured data.
     * </p>
     *
     * @param json The JSON string to parse
     * @return A Map representing the JSON content; empty map if input is null or invalid
     */
    static Map jsonToMap(String json) {
        if (!json) {
            return [:]
        }

        try {
            return new JsonSlurper().parseText(json) as Map
        } catch (Exception ignore) {
            return [:]
        }
    }

    /**
     * Converts a Map into a JSON string.
     * <p>
     * Useful for preparing request bodies for HTTP POST, PUT, or PATCH calls.
     * Automatically returns "{}" if the input is null or invalid.
     * </p>
     *
     * @param map The Map to convert to JSON
     * @return A JSON string representation of the Map; "{}" if input is null or conversion fails
     */
    static String mapToJson(Map map) {
        if (!map) {
            return "{}"
        }

        try {
            return JsonOutput.toJson(map)
        } catch (Exception ignore) {
            return "{}"
        }
    }

}
