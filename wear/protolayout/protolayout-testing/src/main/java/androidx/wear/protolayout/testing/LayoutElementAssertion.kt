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

import androidx.wear.protolayout.LayoutElementBuilders
import java.lang.AssertionError

/**
 * Represents a layout element that can be asserted on.
 *
 * <p>An instance of [LayoutElementAssertion] can be obtained from 'onElement' method on a
 * [LayoutElementAssertionsProvider].
 */
public class LayoutElementAssertion
internal constructor(
    private val elementDescription: String,
    internal val element: LayoutElementBuilders.LayoutElement?,
) {
    /** Asserts that the element was found in the element tree. */
    public fun assertExists() {
        if (element == null) {
            throw AssertionError("Expected $elementDescription to exist, but it does not.")
        }
    }

    /** Asserts that no element was found in the element tree. */
    public fun assertDoesNotExist() {
        if (element != null) {
            throw AssertionError("Expected $elementDescription to not exist, but it does.")
        }
    }

    /** Asserts that the provided [LayoutElementMatcher] is satisfied for this element. */
    public fun assert(matcher: LayoutElementMatcher): LayoutElementAssertion {
        assertExists()
        if (!matcher.matches(element!!)) {
            throw AssertionError(
                "Expected $elementDescription to match '${matcher.description}'," +
                    " but it does not."
            )
        }
        return this
    }
}
