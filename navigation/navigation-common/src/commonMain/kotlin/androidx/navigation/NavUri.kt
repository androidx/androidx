/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation

/** @see android.net.Uri */
public expect abstract class NavUri {
    public abstract fun getFragment(): String?

    public abstract fun getQuery(): String?

    public abstract fun getPathSegments(): List<String>

    public fun getQueryParameters(key: String): List<String>

    public fun getQueryParameterNames(): Set<String>

    abstract override fun toString(): String
}

/**
 * Creates a NavUri which parses the given encoded URI string.
 *
 * @param uriString an RFC 2396-compliant, encoded URI
 * @return NavUri for this given uri string
 */
public fun NavUri(uriString: String): NavUri = NavUriUtils.parse(uriString)

internal expect object NavUriUtils {

    /**
     * Encodes characters in the given string as '%'-escaped octets using the UTF-8 scheme. Leaves
     * letters ("A-Z", "a-z"), numbers ("0-9"), and unreserved characters ("_-!.~'()*") intact.
     * Encodes all other characters with the exception of those specified in the allow argument.
     *
     * @param s string to encode
     * @param allow set of additional characters to allow in the encoded form, null if no characters
     *   should be skipped
     * @return an encoded version of s suitable for use as a URI component
     */
    fun encode(s: String, allow: String? = null): String

    /**
     * Decodes '%'-escaped octets in the given string using the UTF-8 scheme. Replaces invalid
     * octets with the unicode replacement character ("\\uFFFD").
     *
     * @param s encoded string to decode
     * @return the given string with escaped octets decoded
     */
    fun decode(s: String): String

    /**
     * Creates a Uri which parses the given encoded URI string.
     *
     * @param uriString an RFC 2396-compliant, encoded URI
     * @return Uri for this given uri string
     */
    fun parse(uriString: String): NavUri
}
