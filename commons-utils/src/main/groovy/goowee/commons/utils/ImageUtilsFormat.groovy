/*
 * Copyright 2021 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package goowee.commons.utils

import groovy.transform.CompileStatic

/**
 * Enum representing supported image file formats.
 * <p>
 * Provides constants for GIF, PNG, and JPEG formats,
 * along with their file extensions.
 * </p>
 */
@CompileStatic
enum ImageUtilsFormat {
    /** GIF image format with extension 'gif' */
    GIF('gif'),

    /** PNG image format with extension 'png' */
    PNG('png'),

    /** JPEG image format with extension 'jpg' */
    JPEG('jpg')

    /** The file extension associated with the image format */
    final String extension

    /**
     * Constructor for ImageUtilsFormat.
     *
     * @param extension the file extension corresponding to the image format
     */
    ImageUtilsFormat(String extension) {
        this.extension = extension
    }

    /**
     * Returns the ImageUtilsFormat matching the given file extension.
     *
     * @param extension the file extension (e.g., 'gif', 'png', 'jpg')
     * @return the corresponding ImageUtilsFormat, or null if no match is found
     */
    static ImageUtilsFormat get(String extension) {
        return values().find { it.extension == extension }
    }
}
