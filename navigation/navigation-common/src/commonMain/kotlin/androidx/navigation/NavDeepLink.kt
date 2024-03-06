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

import androidx.annotation.RestrictTo
import kotlin.jvm.JvmStatic

/**
 * NavDeepLink encapsulates the parsing and matching of a navigation deep link.
 *
 * This should be added to a [NavDestination] using
 * [NavDestination.addDeepLink].
 */
public expect class NavDeepLink internal constructor(
    uriPattern: String?,
    action: String?,
    mimeType: String?
) {
    /**
     * The uri pattern from the NavDeepLink.
     */
    public val uriPattern: String?

    /**
     * The action from the NavDeepLink.
     */
    public val action: String?

    /**
     * The mimeType from the NavDeepLink.
     */
    public val mimeType: String?

    /**
     * A builder for constructing [NavDeepLink] instances.
     */
    public class Builder {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public constructor()

        /**
         * Set the uri pattern for the [NavDeepLink].
         *
         * @param uriPattern The uri pattern to add to the NavDeepLink
         *
         * @return This builder.
         */
        public fun setUriPattern(uriPattern: String): Builder

        /**
         * Set the action for the [NavDeepLink].
         *
         * @throws IllegalArgumentException if the action is empty.
         *
         * @param action the intent action for the NavDeepLink
         *
         * @return This builder.
         */
        public fun setAction(action: String): Builder

        /**
         * Set the mimeType for the [NavDeepLink].
         *
         * @param mimeType the mimeType for the NavDeepLink
         *
         * @return This builder.
         */
        public fun setMimeType(mimeType: String): Builder

        /**
         * Build the [NavDeepLink] specified by this builder.
         *
         * @return the newly constructed NavDeepLink.
         */
        public fun build(): NavDeepLink

        internal companion object {
            /**
             * Creates a [NavDeepLink.Builder] with a set uri pattern.
             *
             * @param uriPattern The uri pattern to add to the NavDeepLink
             * @return a [Builder] instance
             */
            @JvmStatic
            fun fromUriPattern(uriPattern: String): Builder
            /**
             * Creates a [NavDeepLink.Builder] with a set action.
             *
             * @throws IllegalArgumentException if the action is empty.
             *
             * @param action the intent action for the NavDeepLink
             * @return a [Builder] instance
             */
            @JvmStatic
            fun fromAction(action: String): Builder

            /**
             * Creates a [NavDeepLink.Builder] with a set mimeType.
             *
             * @param mimeType the mimeType for the NavDeepLink
             * @return a [Builder] instance
             */
            @JvmStatic
            fun fromMimeType(mimeType: String): Builder
        }
    }
}
