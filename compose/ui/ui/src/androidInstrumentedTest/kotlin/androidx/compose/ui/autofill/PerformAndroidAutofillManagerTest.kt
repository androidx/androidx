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

package androidx.compose.ui.autofill

import android.os.Build
import android.text.InputType
import android.util.SparseArray
import android.view.View
import android.view.View.AUTOFILL_TYPE_TEXT
import android.view.ViewStructure
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import androidx.autofill.HintConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ComposeUiFlags.isSemanticAutofillEnabled
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.maxTextLength
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
@RequiresApi(Build.VERSION_CODES.O)
// TODO(MNUZEN): split into filling / saving etc. when more of Autofill goes live and more
// data types are supported.
class PerformAndroidAutofillManagerTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()
    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var composeView: View

    private val height = 200.dp
    private val width = 200.dp

    private val contentTag = "content_tag"

    // The "filling" user journey consists of populating a viewStructure for the Autofill framework
    // followed by actually performing autofill a.k.a. populating fillable fields with the provided
    // credential values.

    // ============================================================================================
    // Tests to verify Autofillable components can properly provide ViewStructures.
    // ============================================================================================

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_empty() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        // Act.
        rule.setContentWithAutofillEnabled {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure.childCount).isEqualTo(0)
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_defaultValues_26() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        // Act.
        rule.setContentWithAutofillEnabled {
            Box(
                Modifier
                    // TODO(333102566): for now we need this Autofill contentType to get the
                    // ViewStructure populated. Once Autofill is triggered for all semantics nodes
                    // (not just ones related to Autofill) the semantics below will no longer be
                    // necessary.
                    .semantics { contentType = ContentType.Username }
                    .size(height, width)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setVisibility(View.VISIBLE)
                            setLongClickable(false)
                            setFocusable(false)
                            setFocused(false)
                            setEnabled(false)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 28)
    fun populateViewStructure_defaultValues_28() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        // Act.
        rule.setContentWithAutofillEnabled {
            Box(
                Modifier
                    // TODO(333102566): for now we need this Autofill contentType to get the
                    // ViewStructure populated. Once Autofill is triggered for all semantics nodes
                    // (not just ones related to Autofill) the semantics below will no longer be
                    // necessary.
                    .semantics { contentType = ContentType.Username }
                    .size(height, width)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setVisibility(View.VISIBLE)
                            setLongClickable(false)
                            setFocusable(false)
                            setFocused(false)
                            setEnabled(false)
                            setMaxTextLength(-1)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_contentType() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(height, width)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_contentDataType() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics { contentDataType = ContentDataType.Text }
                    .size(height, width)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillType(AUTOFILL_TYPE_TEXT)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_clickable() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .clickable {}
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setClickable(true)
                            setFocusable(true)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_contentDescription() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        contentDescription = contentTag
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setContentDescription(contentTag)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_role_tab() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .selectable(
                        selected = true,
                        onClick = {},
                        enabled = true,
                        role = Role.Tab,
                        interactionSource = null,
                        indication = null
                    )
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setClickable(true)
                            setFocusable(true)
                            setEnabled(true)
                            setSelected(true)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_role_button() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .selectable(
                        selected = true,
                        onClick = {},
                        enabled = true,
                        role = Role.RadioButton,
                        interactionSource = null,
                        indication = null
                    )
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setClickable(true)
                            setFocusable(true)
                            setEnabled(true)
                            setSelected(true)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_hideFromAccessibility() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        hideFromAccessibility()
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert that even if a component is unimportant for accessibility, it can still be
        // accessed by autofill.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setVisibility(View.VISIBLE)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_invisibility() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.alpha(0f)
                    .semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setVisibility(View.INVISIBLE)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_visibility() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setVisibility(View.VISIBLE)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_invisibility_alpha() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .alpha(0f) // this node should now be invisible
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setVisibility(View.INVISIBLE)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_longClickable() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        onLongClick { true }
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setLongClickable(true)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_focused_focusable() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        isFocusable()
                        isFocused()
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setFocusable(true)
                            setFocused(true)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_enabled() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        isEnabled()
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setEnabled(true)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 28)
    fun populateViewStructure_setMaxLength() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        maxTextLength = 5
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setMaxTextLength(5)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_checkable_unchecked() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        toggleableState = ToggleableState.Off
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setAutofillType(View.AUTOFILL_TYPE_TOGGLE)
                            setCheckable(true)
                            setFocusable(true)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_checkable_checked() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        toggleableState = ToggleableState.On
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setAutofillType(View.AUTOFILL_TYPE_TOGGLE)
                            setCheckable(true)
                            setChecked(true)
                            setFocusable(true)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_usernameChild() {
        // Arrange.
        val viewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            Column {
                BasicTextField(
                    state = remember { TextFieldState() },
                    modifier =
                        Modifier.semantics { contentType = ContentType.Username }
                            .testTag(contentTag)
                )
            }
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            text = ""
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setAutofillType(AUTOFILL_TYPE_TEXT)
                            setAutofillValue(AutofillValue.forText(""))
                            setClassName(
                                AndroidComposeViewAccessibilityDelegateCompat.TextFieldClassName
                            )
                            setClickable(true)
                            setFocusable(true)
                            setLongClickable(true)
                            setVisibility(View.VISIBLE)
                        }
                    )
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_passwordChild() {
        // Arrange.
        val viewStructure = FakeViewStructure()

        rule.setContentWithAutofillEnabled {
            val passwordState = remember { TextFieldState() }

            Column {
                BasicTextField(
                    state = passwordState,
                    modifier =
                        Modifier.semantics { contentType = ContentType.Password }
                            .testTag(contentTag)
                )
            }
        }

        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            androidComposeView.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        Truth.assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    children.add(
                        FakeViewStructure {
                            virtualId = contentTag.semanticsId()
                            text = ""
                            setAutofillHints(arrayOf(HintConstants.AUTOFILL_HINT_USERNAME))
                            setAutofillType(AUTOFILL_TYPE_TEXT)
                            setAutofillValue(AutofillValue.forText(""))
                            setClassName(
                                AndroidComposeViewAccessibilityDelegateCompat.TextFieldClassName
                            )
                            setClickable(true)
                            setDataIsSensitive(true)
                            setInputType(
                                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                            )
                            setFocusable(true)
                            setLongClickable(true)
                            setVisibility(View.VISIBLE)
                        }
                    )
                }
            )
    }

    // ============================================================================================
    // Tests to verify Autofillable components can properly perform autofill.
    // ============================================================================================

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun performAutofill_credentials_customBTF() {
        val expectedUsername = "test_username"
        val expectedPassword = "test_password1111"

        val usernameTag = "username_tag"
        val passwordTag = "password_tag"

        rule.setContentWithAutofillEnabled {
            Column {
                BasicTextField(
                    state = remember { TextFieldState() },
                    modifier =
                        Modifier.semantics { contentType = ContentType.Username }
                            .testTag(usernameTag)
                )
                BasicTextField(
                    state = remember { TextFieldState() },
                    modifier =
                        Modifier.semantics { contentType = ContentType.Password }
                            .testTag(passwordTag)
                )
            }
        }

        val autofillValues =
            SparseArray<AutofillValue>().apply {
                append(usernameTag.semanticsId(), AutofillValue.forText(expectedUsername))
                append(passwordTag.semanticsId(), AutofillValue.forText(expectedPassword))
            }

        rule.runOnIdle { androidComposeView.autofill(autofillValues) }

        rule.onNodeWithTag(usernameTag).assertTextEquals(expectedUsername)
        rule.onNodeWithTag(passwordTag).assertTextEquals(expectedPassword)
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun performAutofill_payment_customBTF() {
        val expectedCreditCardNumber = "123 456 789"
        val expectedSecurityCode = "123"

        val creditCardTag = "credit_card_tag"
        val securityCodeTag = "security_code_tag"

        rule.setContentWithAutofillEnabled {
            val creditCardInput = remember { TextFieldState() }
            val securityCodeInput = remember { TextFieldState() }
            Column {
                BasicTextField(
                    state = creditCardInput,
                    modifier =
                        Modifier.semantics { contentType = ContentType.CreditCardNumber }
                            .testTag(creditCardTag)
                )
                BasicTextField(
                    state = securityCodeInput,
                    modifier =
                        Modifier.semantics { contentType = ContentType.CreditCardSecurityCode }
                            .testTag(securityCodeTag)
                )
            }
        }

        val autofillValues =
            SparseArray<AutofillValue>().apply {
                append(creditCardTag.semanticsId(), AutofillValue.forText(expectedCreditCardNumber))
                append(securityCodeTag.semanticsId(), AutofillValue.forText(expectedSecurityCode))
            }

        rule.runOnIdle { androidComposeView.autofill(autofillValues) }

        rule.onNodeWithTag(creditCardTag).assertTextEquals(expectedCreditCardNumber)
        rule.onNodeWithTag(securityCodeTag).assertTextEquals(expectedSecurityCode)
    }

    // ============================================================================================
    // Helper functions
    // ============================================================================================

    private fun String.semanticsId() = rule.onNodeWithTag(this).fetchSemanticsNode().id

    private fun Dp.dpToPx() = with(rule.density) { this@dpToPx.roundToPx() }

    private inline fun FakeViewStructure(block: FakeViewStructure.() -> Unit): FakeViewStructure {
        return FakeViewStructure()
            .apply {
                packageName = composeView.context.applicationInfo.packageName
                setDimens(0, 0, 0, 0, width.dpToPx(), height.dpToPx())
            }
            .apply(block)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun ComposeContentTestRule.setContentWithAutofillEnabled(
        content: @Composable () -> Unit
    ) {
        setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            androidComposeView._autofillManager?.currentSemanticsNodesInvalidated = true
            isSemanticAutofillEnabled = true

            composeView = LocalView.current
            LaunchedEffect(Unit) {
                // Make sure the delay between batches of events is set to zero.
                (composeView as RootForTest).setAccessibilityEventBatchIntervalMillis(0L)
            }
            content()
        }
    }
}
