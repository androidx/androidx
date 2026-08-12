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
 * @param extras The [RequestExtras] to provide extra information to the [DeepLinkRequest]
 */
public class DeepLinkRequest(
    public val uri: DeepLinkUri? = null,
    public val extras: RequestExtras = emptyRequestExtras(),
) {

    init {
        require(uri != null || extras.isNotEmpty()) {
            "DeepLinkRequest must have either a uri or extras."
        }
    }

    /**
     * Constructs a [DeepLinkRequest] with a string uri.
     *
     * @param uri the string uri of the requested deep link
     * @param extras The [RequestExtras] to provide extra information to the [DeepLinkRequest]
     */
    public constructor(
        uri: String,
        extras: RequestExtras = emptyRequestExtras(),
    ) : this(DeepLinkUri(uri), extras)

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
        /**
         * The [RequestExtrasKey] for the mimeType stored inside the [RequestExtras] returned by
         * [mimeTypeExtra].
         */
        public object MimeTypeExtrasKey : RequestExtrasKey<String>

        /**
         * Returns a [RequestExtras] that stores the provided [mimeTypeExtra] with the key
         * [MimeTypeExtrasKey].
         *
         * The value can be retrieved via extras[[MimeTypeExtrasKey]].
         */
        public fun mimeTypeExtra(mimeType: String): RequestExtras = requestExtras {
            put(MimeTypeExtrasKey, mimeType)
        }
    }
}
