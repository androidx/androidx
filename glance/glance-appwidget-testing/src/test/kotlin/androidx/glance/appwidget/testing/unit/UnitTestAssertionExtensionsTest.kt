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

package androidx.glance.appwidget.testing.unit

import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.CheckboxDefaults.checkBoxColors
import androidx.glance.appwidget.EmittableCheckBox
import androidx.glance.appwidget.EmittableRadioButton
import androidx.glance.appwidget.EmittableSwitch
import androidx.glance.appwidget.RadioButtonDefaults
import androidx.glance.appwidget.SwitchDefaults.switchColors
import androidx.glance.layout.EmittableColumn
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.testing.unit.getGlanceNodeAssertionFor
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.text.EmittableText
import androidx.glance.unit.ColorProvider
import com.google.common.truth.ExpectFailure.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

// Tests appWidget-specific convenience assertions and the underlying filters / matchers that are
// relevant to unit tests
class UnitTestAssertionExtensionsTest {
    @Test
    fun assertIsChecked_checkboxChecked() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableCheckBox(CHECKBOX_COLORS)
                .apply {
                    checked = true
                    modifier = GlanceModifier.semantics { testTag = "checkbox" }
                },
            onNodeMatcher = hasTestTag(
                "checkbox"
            )
        )

        nodeAssertion.assertIsChecked()
        // no error
    }

    @Test
    fun assertIsChecked_checkboxUnchecked_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableCheckBox(CHECKBOX_COLORS).apply {
                    checked = false
                    modifier = GlanceModifier.semantics { testTag = "checkbox" }
                })
            },
            onNodeMatcher = hasTestTag("checkbox")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertIsChecked()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (is checked)")
    }

    @Test
    fun assertIsChecked_switchChecked() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableSwitch(SWITCH_COLORS)
                .apply {
                    checked = true
                    modifier = GlanceModifier.semantics { testTag = "switch" }
                },
            onNodeMatcher = hasTestTag("switch")
        )

        nodeAssertion.assertIsChecked()
        // no error
    }

    @Test
    fun assertIsChecked_switchUnchecked_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableSwitch(SWITCH_COLORS)
                .apply {
                    checked = false
                    modifier = GlanceModifier.semantics { testTag = "switch" }
                },
            onNodeMatcher = hasTestTag("switch")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertIsChecked()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (is checked)")
    }

    @Test
    fun assertIsChecked_radioChecked() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableRadioButton(RADIO_BUTTON_COLORS)
                .apply {
                    checked = true
                    modifier = GlanceModifier.semantics { testTag = "radio" }
                },
            onNodeMatcher = hasTestTag("radio")
        )

        nodeAssertion.assertIsChecked()
        // no error
    }

    @Test
    fun assertIsChecked_radioUnchecked_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableRadioButton(RADIO_BUTTON_COLORS)
                .apply {
                    checked = false
                    modifier = GlanceModifier.semantics { testTag = "radio" }
                },
            onNodeMatcher = hasTestTag("radio")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertIsChecked()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (is checked)")
    }

    @Test
    fun assertIsChecked_nonCheckableElement_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test"
                    modifier = GlanceModifier.semantics { testTag = "test-tag" }
                })
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertIsChecked()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (is checked)")
    }

    @Test
    fun assertIsNotChecked_checkboxUnChecked() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableCheckBox(CHECKBOX_COLORS)
                .apply {
                    checked = false
                    modifier = GlanceModifier.semantics { testTag = "checkbox" }
                },
            onNodeMatcher = hasTestTag(
                "checkbox"
            )
        )

        nodeAssertion.assertIsNotChecked()
        // no error
    }

    @Test
    fun assertIsNotChecked_checkboxChecked_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableCheckBox(CHECKBOX_COLORS).apply {
                    checked = true
                    modifier = GlanceModifier.semantics { testTag = "checkbox" }
                })
            },
            onNodeMatcher = hasTestTag("checkbox")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertIsNotChecked()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (is not checked)")
    }

    @Test
    fun assertIsNotChecked_switchUnChecked() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableSwitch(SWITCH_COLORS)
                .apply {
                    checked = false
                    modifier = GlanceModifier.semantics { testTag = "switch" }
                },
            onNodeMatcher = hasTestTag("switch")
        )

        nodeAssertion.assertIsNotChecked()
        // no error
    }

    @Test
    fun assertIsNotChecked_switchChecked_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableSwitch(SWITCH_COLORS)
                .apply {
                    checked = true
                    modifier = GlanceModifier.semantics { testTag = "switch" }
                },
            onNodeMatcher = hasTestTag("switch")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertIsNotChecked()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (is not checked)")
    }

    @Test
    fun assertIsNotChecked_radioUnChecked() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableRadioButton(RADIO_BUTTON_COLORS)
                .apply {
                    checked = false
                    modifier = GlanceModifier.semantics { testTag = "radio" }
                },
            onNodeMatcher = hasTestTag("radio")
        )

        nodeAssertion.assertIsNotChecked()
        // no error
    }

    @Test
    fun assertIsNotChecked_radioChecked_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableRadioButton(RADIO_BUTTON_COLORS)
                .apply {
                    checked = true
                    modifier = GlanceModifier.semantics { testTag = "radio" }
                },
            onNodeMatcher = hasTestTag("radio")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertIsNotChecked()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (is not checked)")
    }

    @Test
    fun assertIsNotChecked_nonCheckableElement_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test"
                    modifier = GlanceModifier.semantics { testTag = "test-tag" }
                })
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertIsNotChecked()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (is not checked)")
    }

    companion object {
        private val CHECKBOX_COLORS =
            checkBoxColors(
                checkedColor = ColorProvider(Color.Red),
                uncheckedColor = ColorProvider(Color.Blue)
            )
        private val SWITCH_COLORS =
            switchColors(
                checkedThumbColor = ColorProvider(Color.Green),
                uncheckedThumbColor = ColorProvider(Color.Gray),
                checkedTrackColor = ColorProvider(Color.Blue),
                uncheckedTrackColor = ColorProvider(Color.Red)
            )
        private val RADIO_BUTTON_COLORS =
            RadioButtonDefaults.colors(
                checkedColor = ColorProvider(Color.Green),
                uncheckedColor = ColorProvider(Color.Gray)
            )
    }
}
