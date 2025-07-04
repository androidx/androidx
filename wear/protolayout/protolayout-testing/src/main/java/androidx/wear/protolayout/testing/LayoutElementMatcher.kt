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
import androidx.wear.protolayout.testing.TestContext.Companion.EMPTY_CONTEXT

/**
 * Wrapper for element matcher lambdas that allows to build string explaining to the developer what
 * conditions are being tested.
 *
 * @param description a string explaining to the developer what conditions were being tested.
 * @param matcher a lambda performing the actual logic of matching on the layout element, with a
 *   [TextContext] accessible for retrieving context data such as the dynamic data map for
 *   evaluating the dynamic values.
 */
public class LayoutElementMatcher(
    internal val description: String,
    private val matcher: (LayoutElement, TestContext) -> Boolean,
) {
    /**
     * Constructor for the Wrapper of element matcher lambdas that allows to build string explaining
     * to the developer what conditions are being tested.
     *
     * @param description a string explaining to the developer what conditions were being tested.
     * @param matcher a lambda performing the actual logic of matching on the layout element.
     */
    public constructor(
        description: String,
        matcher: (LayoutElement) -> Boolean,
    ) : this(description, { element, _ -> matcher(element) })

    /** Returns whether the given element is matched by this matcher under the given context. */
    internal fun matches(
        element: LayoutElement,
        assertionContext: TestContext = EMPTY_CONTEXT,
    ): Boolean = matcher(element, assertionContext)

    /**
     * Returns whether the given element is matched by both this and the other mather.
     *
     * @param other mather that should also match in addition to current matcher.
     */
    public infix fun and(other: LayoutElementMatcher): LayoutElementMatcher =
        LayoutElementMatcher("($description) && (${other.description})") { element, context ->
            matcher(element, context) && other.matches(element, context)
        }

    /**
     * Returns whether the given element is matched by this or the other mather.
     *
     * @param other mather that can be tested to match if the current matcher does not.
     */
    public infix fun or(other: LayoutElementMatcher): LayoutElementMatcher =
        LayoutElementMatcher("($description) || (${other.description})") { element, context ->
            matcher(element, context) || other.matches(element, context)
        }

    /** Returns whether the given element does not match the matcher. */
    public operator fun not(): LayoutElementMatcher =
        LayoutElementMatcher("NOT ($description)") { element, context ->
            !matcher(element, context)
        }
}
