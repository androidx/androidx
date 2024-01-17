/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.filters

/** Filter class for text values, contains either some exact String, or a [TextMatcher] instance. */
class TextFilter private constructor(
    @get:JvmName("asMatcher")
    val asMatcher: TextMatcher?,
    @get:JvmName("asExactText")
    val asExactText: String?
) {
    /** Creates a [TextFilter] instance with a [TextMatcher]. */
    constructor(matcher: TextMatcher) : this(matcher, null)

    /** Creates a [TextFilter] instance with some text to match exactly. */
    constructor(exactText: String) : this(null, exactText)
}

/** Matches some text. */
class TextMatcher(
    /** The filtered text should start with this String. */
    val startsWith: String?,

    /** The filtered text should contain this String. */
    val contains: String?
) {
    init {
        require(startsWith != null || contains != null) {
            "at least one of 'startsWith' and 'contains' must be non-null"
        }
    }
}
