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

@DslMarker
public annotation class NavDeepLinkDsl

/**
 * Construct a new [NavDeepLink]
 */
public fun navDeepLink(deepLinkBuilder: NavDeepLinkDslBuilder.() -> Unit): NavDeepLink =
    NavDeepLinkDslBuilder().apply(deepLinkBuilder).build()

/**
 * DSL for constructing a new [NavDeepLink]
 */
@NavDeepLinkDsl
public class NavDeepLinkDslBuilder {
    private val builder = NavDeepLink.Builder()

    /**
     * The uri pattern of the deep link
     */
    public var uriPattern: String? = null

    /**
     * Intent action for the deep link
     *
     * @throws IllegalArgumentException if attempting to set to empty.
     */
    public var action: String? = null
        @Suppress("DocumentExceptions")
        set(p) {
            if (p != null && p.isEmpty()) {
                throw IllegalArgumentException("The NavDeepLink cannot have an empty action.")
            }
            field = p
        }

    /**
     * MimeType for the deep link
     */
    public var mimeType: String? = null

    internal fun build() = builder.apply {
        check(!(uriPattern == null && action == null && mimeType == null)) {
            ("The NavDeepLink must have an uri, action, and/or mimeType.")
        }
        uriPattern?.let { setUriPattern(it) }
        action?.let { setAction(it) }
        mimeType?.let { setMimeType(it) }
    }.build()
}
