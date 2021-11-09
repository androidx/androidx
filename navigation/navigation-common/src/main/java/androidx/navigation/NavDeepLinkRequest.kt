/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.Intent
import android.net.Uri
import androidx.annotation.RestrictTo
import java.lang.StringBuilder

/**
 * A request for a deep link in a [NavDestination].
 *
 * NavDeepLinkRequest are used to check if a [NavDeepLink] exists for a
 * [NavDestination] and to navigate to a [NavDestination] with a matching
 * [NavDeepLink].
 */
public open class NavDeepLinkRequest {
    public open val uri: Uri?
    public open val action: String?
    public open val mimeType: String?
    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        /**
         * The uri from the NavDeepLinkRequest.
         *
         * @see NavDeepLink.uriPattern
         */
        uri: Uri?,
        /**
         * The action from the NavDeepLinkRequest.
         *
         * @see NavDeepLink.action
         */
        action: String?,
        /**
         * The mimeType from the NavDeepLinkRequest.
         *
         * @see NavDeepLink.mimeType
         */
        mimeType: String?
    ) { this.uri = uri; this.action = action; this.mimeType = mimeType }

    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(intent: Intent) : this(intent.data, intent.action, intent.type)

    public override fun toString(): String {
        val sb = StringBuilder()
        sb.append("NavDeepLinkRequest")
        sb.append("{")
        if (uri != null) {
            sb.append(" uri=")
            sb.append(uri.toString())
        }
        if (action != null) {
            sb.append(" action=")
            sb.append(action)
        }
        if (mimeType != null) {
            sb.append(" mimetype=")
            sb.append(mimeType)
        }
        sb.append(" }")
        return sb.toString()
    }

    /**
     * A builder for constructing [NavDeepLinkRequest] instances.
     */
    public class Builder private constructor() {
        private var uri: Uri? = null
        private var action: String? = null
        private var mimeType: String? = null

        /**
         * Set the uri for the [NavDeepLinkRequest].
         *
         * @param uri The uri to add to the NavDeepLinkRequest
         *
         * @return This builder.
         */
        public fun setUri(uri: Uri): Builder {
            this.uri = uri
            return this
        }

        /**
         * Set the action for the [NavDeepLinkRequest].
         *
         * @throws IllegalArgumentException if the action is empty.
         *
         * @param action the intent action for the NavDeepLinkRequest
         *
         * @return This builder.
         */
        public fun setAction(action: String): Builder {
            require(action.isNotEmpty()) { "The NavDeepLinkRequest cannot have an empty action." }
            this.action = action
            return this
        }

        /**
         * Set the mimeType for the [NavDeepLinkRequest].
         *
         * @param mimeType the mimeType for the NavDeepLinkRequest
         *
         * @throws IllegalArgumentException if the given mimeType does not match th3e required
         * "type/subtype" format.
         *
         * @return This builder.
         */
        public fun setMimeType(mimeType: String): Builder {
            val mimeTypeMatcher = mimeType.matches("^[-\\w*.]+/[-\\w+*.]+$".toRegex())
            require(mimeTypeMatcher) {
                "The given mimeType $mimeType does not match to required \"type/subtype\" format"
            }
            this.mimeType = mimeType
            return this
        }

        /**
         * Build the [NavDeepLinkRequest] specified by this builder.
         *
         * @return the newly constructed NavDeepLinkRequest
         */
        public fun build(): NavDeepLinkRequest {
            return NavDeepLinkRequest(uri, action, mimeType)
        }

        public companion object {
            /**
             * Creates a [NavDeepLinkRequest.Builder] with a set uri.
             *
             * @param uri The uri to add to the NavDeepLinkRequest
             * @return a [Builder] instance
             */
            @JvmStatic
            public fun fromUri(uri: Uri): Builder {
                val builder = Builder()
                builder.setUri(uri)
                return builder
            }

            /**
             * Creates a [NavDeepLinkRequest.Builder] with a set action.
             *
             * @throws IllegalArgumentException if the action is empty.
             *
             * @param action the intent action for the NavDeepLinkRequest
             * @return a [Builder] instance
             */
            @JvmStatic
            public fun fromAction(action: String): Builder {
                require(action.isNotEmpty()) {
                    "The NavDeepLinkRequest cannot have an empty action."
                }
                val builder = Builder()
                builder.setAction(action)
                return builder
            }

            /**
             * Creates a [NavDeepLinkRequest.Builder] with a set mimeType.
             *
             * @param mimeType the mimeType for the NavDeepLinkRequest
             * @return a [Builder] instance
             */
            @JvmStatic
            public fun fromMimeType(mimeType: String): Builder {
                val builder = Builder()
                builder.setMimeType(mimeType)
                return builder
            }
        }
    }
}
