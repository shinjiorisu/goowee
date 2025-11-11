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
package goowee.utils

import grails.util.Environment
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Utility class for detecting the current Grails application environment.
 * <p>
 * Provides helper methods to check if the application is running in development,
 * test, or production environments, as well as retrieving the current environment name.
 * </p>
 *
 * Author: Gianluca Sartori
 */
@Slf4j
@CompileStatic
class EnvUtils {

    /**
     * Checks if the application is running in the Grails development environment.
     *
     * @return true if the current environment is 'development', false otherwise
     */
    static Boolean isDevelopment() {
        return currentEnvironment == 'development'
    }

    /**
     * Checks if the application is running in the Grails test environment.
     *
     * @return true if the current environment is 'test', false otherwise
     */
    static Boolean isTesting() {
        return currentEnvironment == 'test'
    }

    /**
     * Checks if the application is running in the Grails production environment.
     *
     * @return true if the current environment is 'production', false otherwise
     */
    static Boolean isProduction() {
        return currentEnvironment == 'production'
    }

    /**
     * Returns the name of the currently running Grails environment.
     * <p>
     * Typical return values are 'development', 'test', or 'production'.
     * </p>
     *
     * @return the name of the current environment as a string
     */
    static String getCurrentEnvironment() {
        return Environment.currentEnvironment.name
    }
}
