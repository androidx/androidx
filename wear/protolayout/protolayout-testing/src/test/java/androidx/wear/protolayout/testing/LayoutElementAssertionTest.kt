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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Text
import com.google.common.truth.ExpectFailure.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class LayoutElementAssertionTest {

    @Test
    fun assertExists_success() {
        val assertion = LayoutElementAssertion(ELEMENT_DESCRIPTION, Box.Builder().build())

        assertion.assertExists() // no error
    }

    @Test
    fun assertExists_error() {
        val assertion = LayoutElementAssertion(ELEMENT_DESCRIPTION, null)

        val assertionError = assertThrows(AssertionError::class.java) { assertion.assertExists() }

        assertThat(assertionError)
            .hasMessageThat()
            .isEqualTo("Expected $ELEMENT_DESCRIPTION to exist, but it does not.")
    }

    @Test
    fun assertDoesNotExist_success() {
        val assertion = LayoutElementAssertion(ELEMENT_DESCRIPTION, null)

        assertion.assertDoesNotExist() // no error
    }

    @Test
    fun assertDoesNotExist_error() {
        val assertion = LayoutElementAssertion(ELEMENT_DESCRIPTION, Box.Builder().build())

        val assertionError =
            assertThrows(AssertionError::class.java) { assertion.assertDoesNotExist() }

        assertThat(assertionError)
            .hasMessageThat()
            .isEqualTo("Expected $ELEMENT_DESCRIPTION to not exist, but it does.")
    }

    @Test
    fun assert_withMatcher_success() {
        val assertion = LayoutElementAssertion(ELEMENT_DESCRIPTION, Box.Builder().build())
        assertion.assert(LayoutElementMatcher("Element type is Box") { it is Box })
    }

    @Test
    fun assert_withMatcher_error() {
        val assertion = LayoutElementAssertion(ELEMENT_DESCRIPTION, Box.Builder().build())
        val matcher = LayoutElementMatcher("Element type is Text") { it is Text }

        val assertionError = assertThrows(AssertionError::class.java) { assertion.assert(matcher) }

        assertThat(assertionError)
            .hasMessageThat()
            .isEqualTo(
                "Expected $ELEMENT_DESCRIPTION to match '${matcher.description}'," +
                    " but it does not."
            )
    }

    @Test
    fun chainAssertions() {
        val textContent = "testing text"
        val assertion =
            LayoutElementAssertion(ELEMENT_DESCRIPTION, Text.Builder().setText(textContent).build())
        val typeMatcher = LayoutElementMatcher("Element type is Text") { it is Text }
        val contentMatcher =
            LayoutElementMatcher("Element text = '$textContent'") {
                it is Text && it.text?.value == textContent
            }

        assertion.assert(typeMatcher).assert(contentMatcher)
    }

    @Test
    fun chainAssertions_failureInFirst() {
        val textContent = "testing text"
        val assertion =
            LayoutElementAssertion(ELEMENT_DESCRIPTION, Text.Builder().setText(textContent).build())
        val firstMatcher = LayoutElementMatcher("Element type is Box") { it is Box }
        val secondMatcher = LayoutElementMatcher("Element type is Text") { it is Text }

        val assertionError =
            assertThrows(AssertionError::class.java) {
                assertion.assert(firstMatcher).assert(secondMatcher)
            }

        assertThat(assertionError)
            .hasMessageThat()
            .isEqualTo(
                "Expected $ELEMENT_DESCRIPTION to match " +
                    "'${firstMatcher.description}', " +
                    "but it does not."
            )
    }

    @Test
    fun chainAssertions_failureInSecond() {
        val textContent = "testing text"
        val assertion =
            LayoutElementAssertion(ELEMENT_DESCRIPTION, Text.Builder().setText(textContent).build())
        val firstMatcher = LayoutElementMatcher("Element type is Text") { it is Text }
        val secondMatcher = LayoutElementMatcher("Element type is Box") { it is Box }

        val assertionError =
            assertThrows(AssertionError::class.java) {
                assertion.assert(firstMatcher).assert(secondMatcher)
            }

        assertThat(assertionError)
            .hasMessageThat()
            .isEqualTo(
                "Expected $ELEMENT_DESCRIPTION to match " +
                    "'${secondMatcher.description}', " +
                    "but it does not."
            )
    }

    companion object {
        const val ELEMENT_DESCRIPTION = "testing element"
    }
}
