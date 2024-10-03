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

package androidx.wear.protolayout.testing

import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement

/**
 * Wrapper for element matcher lambdas that allows to build string explaining to the developer what
 * conditions are being tested.
 *
 * @param description a string explaining to the developer what conditions were being tested.
 * @param matcher a lambda performing the actual logic of matching on the layout element.
 */
public class LayoutElementMatcher(
    internal val description: String,
    private val matcher: (LayoutElement) -> Boolean
) {
    /** Returns whether the given element is matched by this matcher. */
    internal fun matches(element: LayoutElement): Boolean = matcher(element)

    /**
     * Returns whether the given element is matched by both this and the other mather.
     *
     * @param other mather that should also match in addition to current matcher.
     */
    public infix fun and(other: LayoutElementMatcher): LayoutElementMatcher =
        LayoutElementMatcher("($description) && (${other.description})") {
            matcher(it) && other.matches(it)
        }

    /**
     * Returns whether the given element is matched by this or the other mather.
     *
     * @param other mather that can be tested to match if the current matcher does not.
     */
    public infix fun or(other: LayoutElementMatcher): LayoutElementMatcher =
        LayoutElementMatcher("($description) || (${other.description})") {
            matcher(it) || other.matches(it)
        }

    /** Returns whether the given element does not match the matcher. */
    public operator fun not(): LayoutElementMatcher =
        LayoutElementMatcher("NOT ($description)") { !matcher(it) }
}
