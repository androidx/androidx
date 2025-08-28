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

import android.graphics.Rect
import android.text.InputType
import android.util.SparseArray
import android.view.View
import android.view.View.AUTOFILL_TYPE_TEXT
import android.view.ViewStructure
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import androidx.autofill.HintConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.remember
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.maxTextLength
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.Ignore
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
// TODO(MNUZEN): split into filling / saving etc. when more of Autofill goes live and more
// data types are supported.
class PerformAndroidAutofillManagerTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()
    private val height = 200.dp
    private val width = 200.dp

    private val contentTag = "content_tag"

    @OptIn(ExperimentalComposeUiApi::class)
    private val previousFlagValue = ComposeUiFlags.isSemanticAutofillEnabled

    @Before
    fun enableAutofill() {
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isSemanticAutofillEnabled = true
    }

    @After
    fun teardown() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = rule.activity
        while (!activity.isDestroyed) {
            instrumentation.runOnMainSync {
                if (!activity.isDestroyed) {
                    activity.finish()
                }
            }
        }
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isSemanticAutofillEnabled = previousFlagValue
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun autofillModifier_contentType() {
        rule.setContent { Box(Modifier.testTag("TestTag").contentType(ContentType.NewUsername)) {} }

        rule
            .onNodeWithTag("TestTag")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentType,
                    ContentType.NewUsername,
                )
            )
    }

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
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent { view = LocalView.current }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure.childCount).isEqualTo(0)
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_defaultValues() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
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

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    autofillId = view.autofillId
                    bounds = Rect(0, 0, width.dpToPx(), height.dpToPx())
                    children.add(
                        FakeViewStructure().apply {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            autofillId = view.autofillId
                            bounds = Rect(0, 0, width.dpToPx(), height.dpToPx())
                            isEnabled = true
                            isFocusable = false
                            isFocused = false
                            isLongClickable = false
                            packageName = view.context.applicationInfo.packageName
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                            visibility = View.VISIBLE
                        }
                    )
                    isEnabled = true
                    packageName = view.context.applicationInfo.packageName
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_contentType() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(height, width)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_contentDataType() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentDataType = ContentDataType.Text }
                    .size(height, width)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillType = AUTOFILL_TYPE_TEXT
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_clickable() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .clickable {}
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            isClickable = true
                            isFocusable = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_contentDescription() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        contentDescription = contentTag
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            contentDescription = contentTag
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_role_tab() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .selectable(
                        selected = true,
                        onClick = {},
                        enabled = true,
                        role = Role.Tab,
                        interactionSource = null,
                        indication = null,
                    )
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            isClickable = true
                            isFocusable = true
                            isSelected = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_role_radioButton() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .selectable(
                        selected = true,
                        onClick = {},
                        enabled = true,
                        role = Role.RadioButton,
                        interactionSource = null,
                        indication = null,
                    )
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            className = "android.widget.RadioButton"
                            isCheckable = true
                            isChecked = true
                            isClickable = true
                            isFocusable = true
                            isSelected = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_role_dropdownList() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .selectable(
                        selected = true,
                        onClick = {},
                        enabled = true,
                        role = Role.DropdownList,
                        interactionSource = null,
                        indication = null,
                    )
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            className = "android.widget.Spinner"
                            isCheckable = true
                            isChecked = true
                            isClickable = true
                            isFocusable = true
                            isSelected = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_role_valuePicker() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .selectable(
                        selected = true,
                        onClick = {},
                        enabled = true,
                        role = Role.ValuePicker,
                        interactionSource = null,
                        indication = null,
                    )
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            className = "android.widget.NumberPicker"
                            isCheckable = true
                            isChecked = true
                            isClickable = true
                            isFocusable = true
                            isSelected = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_hideFromAccessibility() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        hideFromAccessibility()
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert that even if a component is unimportant for accessibility, it can still be
        // accessed by autofill.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                            visibility = View.VISIBLE
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Ignore // TODO(b/383198004): Add support for notifyVisibilityChanged.
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_invisibility() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.alpha(0f)
                    .semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                            visibility = View.INVISIBLE
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Ignore // TODO(b/383198004): Add support for notifyVisibilityChanged.
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_visibility() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                            visibility = View.VISIBLE
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Ignore // TODO(b/383198004): Add support for notifyVisibilityChanged.
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_invisibility_alpha() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .alpha(0f) // this node should now be invisible
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                            visibility = View.INVISIBLE
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
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        onLongClick { true }
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            isLongClickable = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_focusable() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        requestFocus { true }
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            isFocusable = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_focusable_focused() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .focusable()
                    .size(width, height)
                    .testTag(contentTag)
            )
        }
        rule.onNodeWithTag(contentTag).requestFocus()

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            isFocusable = true
                            isFocused = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_enabled() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            isEnabled = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_disabled() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        disabled()
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            isEnabled = false
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 28)
    fun populateViewStructure_setMaxLength() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        setText { true }
                        maxTextLength = 5
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            autofillType = AUTOFILL_TYPE_TEXT
                            className = "android.widget.EditText"
                            maxTextLength = 5
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 28)
    fun populateViewStructure_setMaxLength_notSetForNonTextItems() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        maxTextLength = 5
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            maxTextLength = -1
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_checkable_unchecked() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        toggleableState = ToggleableState.Off
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder.
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            autofillType = View.AUTOFILL_TYPE_TOGGLE
                            isCheckable = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_checkable_checked() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        toggleableState = ToggleableState.On
                    }
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            autofillType = View.AUTOFILL_TYPE_TOGGLE
                            isCheckable = true
                            isChecked = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_checkable() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .toggleable(true) {}
                    .size(width, height)
                    .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            autofillType = View.AUTOFILL_TYPE_TOGGLE
                            isCheckable = true
                            isChecked = true
                            isClickable = true
                            isFocusable = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_username_empty() {
        // Arrange.
        lateinit var view: View
        val viewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Column {
                BasicTextField(
                    state = remember { TextFieldState() },
                    modifier =
                        Modifier.semantics { contentType = ContentType.Username }
                            .size(height, width)
                            .testTag(contentTag),
                )
            }
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            autofillType = AUTOFILL_TYPE_TEXT
                            autofillValue = AutofillValue.forText("")
                            className = "android.widget.EditText"
                            isClickable = true
                            isFocusable = true
                            isLongClickable = true
                            text = ""
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                            visibility = View.VISIBLE
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_username_specified() {
        // Arrange.
        lateinit var view: View
        val viewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Column {
                BasicTextField(
                    state = remember { TextFieldState("testUsername") },
                    modifier =
                        Modifier.semantics { contentType = ContentType.Username }
                            .size(height, width)
                            .testTag(contentTag),
                )
            }
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            autofillType = AUTOFILL_TYPE_TEXT
                            autofillValue = AutofillValue.forText("testUsername")
                            className = "android.widget.EditText"
                            isClickable = true
                            isFocusable = true
                            isLongClickable = true
                            text = ""
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                            visibility = View.VISIBLE
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_password_empty() {
        // Arrange.
        lateinit var view: View
        val viewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            val passwordState = remember { TextFieldState() }

            Column {
                BasicTextField(
                    state = passwordState,
                    modifier =
                        Modifier.semantics { contentType = ContentType.Password }
                            .size(height, width)
                            .testTag(contentTag),
                )
            }
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_PASSWORD)
                            autofillType = AUTOFILL_TYPE_TEXT
                            autofillValue = AutofillValue.forText("")
                            className = "android.widget.EditText"
                            dataIsSensitive = true
                            inputType =
                                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                            isClickable = true
                            isFocusable = true
                            isLongClickable = true
                            text = ""
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                            visibility = View.VISIBLE
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_password_asContentType() {
        // Arrange.
        lateinit var view: View
        val viewStructure = FakeViewStructure()
        rule.setContent {
            view = LocalView.current
            Column {
                BasicTextField(
                    state = remember { TextFieldState("testPassword") },
                    modifier =
                        Modifier.semantics { contentType = ContentType.Password }
                            .size(height, width)
                            .testTag(contentTag),
                )
            }
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_PASSWORD)
                            autofillType = AUTOFILL_TYPE_TEXT
                            autofillValue = AutofillValue.forText("testPassword")
                            className = "android.widget.EditText"
                            dataIsSensitive = true
                            inputType =
                                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                            isClickable = true
                            isFocusable = true
                            isLongClickable = true
                            text = ""
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                            visibility = View.VISIBLE
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
            )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun populateViewStructure_password_asSemanticProperty() {
        // Arrange.
        lateinit var view: View
        val viewStructure = FakeViewStructure()

        rule.setContent {
            view = LocalView.current
            Box(
                modifier =
                    Modifier.semantics {
                            contentType = ContentType.Username
                            password()
                        }
                        .size(height, width)
                        .testTag(contentTag)
            )
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                ViewStructure(view) {
                    children.add(
                        ViewStructure(view) {
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            dataIsSensitive = true
                            virtualId = rule.onNodeWithTag(contentTag).semanticsId()
                        }
                    )
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
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
        // Arrange.
        lateinit var view: View
        val usernameTag = "username_tag"
        val passwordTag = "password_tag"
        rule.setContent {
            view = LocalView.current
            Column {
                BasicTextField(
                    state = remember { TextFieldState() },
                    modifier =
                        Modifier.semantics { contentType = ContentType.Username }
                            .testTag(usernameTag),
                )
                BasicTextField(
                    state = remember { TextFieldState() },
                    modifier =
                        Modifier.semantics { contentType = ContentType.Password }
                            .testTag(passwordTag),
                )
            }
        }

        // Act.
        val usernameId = rule.onNodeWithTag(usernameTag).semanticsId()
        val passwordId = rule.onNodeWithTag(passwordTag).semanticsId()
        rule.runOnIdle {
            view.autofill(
                SparseArray<AutofillValue>().apply {
                    append(usernameId, AutofillValue.forText("testUsername"))
                    append(passwordId, AutofillValue.forText("testPassword"))
                }
            )
        }

        // Assert,
        rule.onNodeWithTag(usernameTag).assertTextEquals("testUsername")
        rule.onNodeWithTag(passwordTag).assertTextEquals("testPassword")
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun performAutofill_payment_customBTF() {
        // Arrange.
        lateinit var view: View
        val creditCardTag = "credit_card_tag"
        val securityCodeTag = "security_code_tag"

        rule.setContent {
            view = LocalView.current
            Column {
                BasicTextField(
                    state = remember { TextFieldState() },
                    modifier =
                        Modifier.semantics { contentType = ContentType.CreditCardNumber }
                            .testTag(creditCardTag),
                )
                BasicTextField(
                    state = remember { TextFieldState() },
                    modifier =
                        Modifier.semantics { contentType = ContentType.CreditCardSecurityCode }
                            .testTag(securityCodeTag),
                )
            }
        }

        // Act.
        val creditCardId = rule.onNodeWithTag(creditCardTag).semanticsId()
        val securityCodeId = rule.onNodeWithTag(securityCodeTag).semanticsId()
        rule.runOnIdle {
            view.autofill(
                SparseArray<AutofillValue>().apply {
                    append(creditCardId, AutofillValue.forText("0123 4567 8910 1112"))
                    append(securityCodeId, AutofillValue.forText("123"))
                }
            )
        }

        // Assert.
        rule.onNodeWithTag(creditCardTag).assertTextEquals("0123 4567 8910 1112")
        rule.onNodeWithTag(securityCodeTag).assertTextEquals("123")
    }

    // ============================================================================================
    // Helper functions
    // ============================================================================================
    private fun Dp.dpToPx() = with(rule.density) { this@dpToPx.roundToPx() }

    private inline fun ViewStructure(
        view: View,
        block: FakeViewStructure.() -> Unit,
    ): FakeViewStructure {
        return FakeViewStructure().apply {
            autofillId = view.autofillId
            bounds = Rect(0, 0, width.dpToPx(), height.dpToPx())
            isEnabled = true
            packageName = view.context.applicationInfo.packageName
            block()
        }
    }
}
