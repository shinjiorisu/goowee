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
import groovy.transform.CompileStatic

/**
 * Represents an HTTP request to be executed by {@link HttpClient}.
 * Provides a fluent API for setting method, path, headers, query parameters, and body.
 *
 * Example:
 * <pre>{@code
 * def request = HttpRequest.post("/api/users")
 *     .header("Authorization", "Bearer token")
 *     .query("active", true)
 *     .jsonBody([name: "Alice"])
 * }</pre>
 *
 * @author Gianluca Sartori
 */
@CompileStatic
class HttpRequest {

    final HttpMethod method
    final String path
    final Map<String, String> headers = [:]
    final Map<String, Object> query = [:]
    Object body

    private HttpRequest(HttpMethod method, String path) {
        this.method = method
        this.path = path.startsWith("/") ? path : "/" + path
    }

    // --- Static factory methods ---
    static HttpRequest get(String path)     { new HttpRequest(HttpMethod.GET, path) }
    static HttpRequest post(String path)    { new HttpRequest(HttpMethod.POST, path) }
    static HttpRequest put(String path)     { new HttpRequest(HttpMethod.PUT, path) }
    static HttpRequest delete(String path)  { new HttpRequest(HttpMethod.DELETE, path) }
    static HttpRequest patch(String path)   { new HttpRequest(HttpMethod.PATCH, path) }

    // --- Fluent modifiers ---
    HttpRequest header(String name, String value) {
        if (value != null) headers[name] = value
        return this
    }

    HttpRequest headers(Map<String, String> values) {
        if (values) headers.putAll(values)
        return this
    }

    HttpRequest query(String name, Object value) {
        if (value != null) query[name] = value
        return this
    }

    HttpRequest query(Map<String, Object> params) {
        if (params) query.putAll(params)
        return this
    }

    HttpRequest body(String body) {
        this.body = body
        return this
    }

    HttpRequest jsonBody(Object body) {
        header("Content-Type", "application/json; charset=UTF-8")
        this.body = JsonOutput.toJson(body)
        return this
    }

    HttpRequest multipartBody(HttpMultipartBody multipart) {
        headers.remove("Content-Type")
        this.body = multipart.build()
        return this
    }


    // --- Helpers ---
    String buildUrl(String baseUrl) {
        StringBuilder sb = new StringBuilder()
        sb << baseUrl
        if (path) {
            if (baseUrl.endsWith("/") && path.startsWith("/"))
                sb << path.substring(1)
            else
                sb << path
        }
        if (!query.isEmpty()) {
            sb << "?"
            sb << query.collect { k, v -> "${URLEncoder.encode(k, 'UTF-8')}=${URLEncoder.encode(v.toString(), 'UTF-8')}" }.join("&")
        }
        return sb.toString()
    }

    @Override
    String toString() {
        return "${method} ${path} headers=${headers.keySet()} query=${query}"
    }
}
