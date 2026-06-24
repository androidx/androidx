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

package androidx.navigation3.runtime.deeplink

/**
 * Represents a requested deep link.
 *
 * @param uri The URI for the deep link.
 * @param extras The map of additional information for the request.
 */
public class DeepLinkRequest(
    public val uri: DeepLinkUri?,
    public val extras: Map<String, Any> = emptyMap(),
) {
    /**
     * Constructs a [DeepLinkRequest] with a string uri.
     *
     * @param uriString the string uri of the requested deep link
     * @param extras The map holding pairs of [String] to [Any] to provide extra information to the
     *   [DeepLinkRequest], such a mimeType.
     */
    public constructor(
        uriString: String,
        extras: Map<String, Any> = emptyMap(),
    ) : this(DeepLinkUri(uriString), extras)

    public override fun toString(): String {
        return buildString {
            append("DeepLinkRequest")
            append("{")
            if (uri != null) {
                append(" uri=")
                append(uri.toString())
            }
            if (extras.isNotEmpty()) {
                append(" extras=")
                append("$extras")
            }
            append(" }")
        }
    }

    public companion object {
        /** The key of the mimeType stored inside the map returned by [mimeTypeExtra]. */
        public object MimeTypeExtrasKey : RequestExtrasKey<String>

        /**
         * Returns a Map<String, Any> that stores the provided [mimeTypeExtra] with the key
         * [MimeTypeExtrasKey].
         *
         * The value can be retrieved via map.get([MimeTypeExtrasKey]).
         */
        public fun mimeTypeExtra(mimeType: String): Map<String, Any> = requestExtras {
            put(MimeTypeExtrasKey, mimeType)
        }
    }
}
