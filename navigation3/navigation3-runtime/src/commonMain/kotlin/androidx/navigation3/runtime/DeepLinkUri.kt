/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3.runtime

/** see [android.net.Uri](https://developer.android.com/reference/android/net/Uri) */
public expect abstract class DeepLinkUri {

    /** Gets the decoded fragment part of this URI, everything after the '#'. */
    public abstract fun getFragment(): String?

    /**
     * Gets the decoded query component from this URI. The query comes after the query separator
     * ('?') and before the fragment separator ('#'). This method would return "q=android" for
     * "http://www.google.com/search?q=android".
     */
    public abstract fun getQuery(): String?

    /** Gets the decoded path segments. */
    public abstract fun getPathSegments(): List<String>

    /** Searches the query string for parameter values with the given key. */
    public fun getQueryParameters(key: String): List<String>

    /**
     * Returns a set of the unique names of all query parameters. Iterating over the set will return
     * the names in order of their first occurrence.
     *
     * @return a set of decoded names
     * @throws UnsupportedOperationException – if this isn't a hierarchical URI
     */
    public fun getQueryParameterNames(): Set<String>

    /** Gets the scheme of this URI. Example: "http" */
    public abstract fun getScheme(): String?

    /**
     * Gets the decoded authority part of this URI. For server addresses, the authority is
     * structured as follows: [ userinfo '@' ] host [ ':' port ]
     *
     * Examples: "google.com", "bob@google.com:80"
     */
    public abstract fun getAuthority(): String?

    /**
     * Returns true if this URI is hierarchical like "http://google.com". Absolute URIs are
     * hierarchical if the scheme-specific part starts with a '/'. Relative URIs are always
     * hierarchical.
     */
    public abstract fun isHierarchical(): Boolean

    /** Returns the encoded string representation of this URI. Example: "http://google.com/" */
    abstract override fun toString(): String
}

/**
 * Creates a [DeepLinkUri] which parses the given encoded URI string.
 *
 * @param uriString an RFC 2396-compliant, encoded URI
 * @return NavUri for this given uri string
 */
public fun DeepLinkUri(uriString: String): DeepLinkUri = DeepLinkUriUtils.parse(uriString)

/** Copy of commonMain androidx.navigation.NavUriUtils */
internal expect object DeepLinkUriUtils {

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
     * octets with the Unicode replacement character ("\\uFFFD").
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
    fun parse(uriString: String): DeepLinkUri
}
