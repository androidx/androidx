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
import android.os.Build
import android.util.SparseArray
import android.view.View
import android.view.autofill.AutofillValue
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.elementOf
import androidx.compose.ui.node.requestAutofill
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.fillableData
import androidx.compose.ui.semantics.inputText
import androidx.compose.ui.semantics.onAutofillText
import androidx.compose.ui.semantics.onFillData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Ignore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.verifyZeroInteractions

@MediumTest
@SdkSuppress(minSdkVersion = 26)
@RunWith(AndroidJUnit4::class)
class AndroidAutofillManagerTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private val height = 200.dp
    private val width = 200.dp
    private val am: PlatformAutofillManager = mock()

    private lateinit var view: View
    private lateinit var focusManager: FocusManager
    private lateinit var inputModeManager: InputModeManager

    @OptIn(ExperimentalComposeUiApi::class)
    private val previousFlagValue = ComposeUiFlags.isSemanticAutofillEnabled

    @Before
    fun enableAutofill() {
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isSemanticAutofillEnabled = true
    }

    @After
    fun teardown() {
        verifyNoMoreInteractions(am)
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
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_initialization() {
        rule.setContent {
            view = LocalView.current
            (LocalAutofillManager.current as AndroidAutofillManager).platformAutofillManager = am
            Box(
                Modifier.semantics {
                        testTag = "username"
                        contentType = ContentType.Username
                    }
                    .size(height, width)
            )
        }

        // Upon initialization, we send `notifyViewVisibility` for all of the components that appear
        // onscreen. For other tests, we use a helper setTestContent function that calls
        // `clearInvocations(am)` to avoid testing this call.
        rule.waitForIdle()
        verify(am, times(1))
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(true),
            )
    }

    @Test
    fun autofillManager_doNotCallCommit_nodesAppeared() {
        var isVisible by mutableStateOf(false)

        rule.setTestContent {
            if (isVisible) {
                Box(
                    Modifier.semantics {
                            testTag = "username"
                            contentType = ContentType.Username
                        }
                        .size(height, width)
                )
            }
        }

        rule.runOnIdle { isVisible = true }

        // `commit` should not be called when an autofillable component appears onscreen.
        rule.waitForIdle()
        verify(am, never()).commit()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(true),
            )
    }

    @Test
    fun autofillManager_doNotCallCommit_autofillTagsAdded() {
        var hasContentType by mutableStateOf(false)

        rule.setTestContent {
            Box(
                modifier =
                    Modifier.then(
                            if (hasContentType) {
                                Modifier.semantics { contentType = ContentType.Username }
                            } else {
                                Modifier
                            }
                        )
                        .size(height, width)
            )
        }

        rule.runOnIdle { hasContentType = true }

        // `commit` should not be called a component becomes relevant to autofill.
        rule.runOnIdle { verify(am, times(0)).commit() }
    }

    @Test
    fun autofillManager_doNotCallCommit_partialRemoval() {
        var revealFirstUsername by mutableStateOf(true)

        rule.setTestContent {
            Box(Modifier.semantics { contentType = ContentType.Username }.size(height, width))
            if (revealFirstUsername) {
                Box(
                    Modifier.semantics {
                            contentType = ContentType.Username
                            testTag = "username"
                        }
                        .size(height, width)
                )
            }
        }
        val semanticsId = rule.onNodeWithTag("username").semanticsId()
        rule.runOnIdle { revealFirstUsername = false }

        // `commit` should not be called unless all autofill-able components leave the screen.
        rule.runOnIdle { verify(am, times(0)).commit() }
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(semanticsId),
                isVisible = eq(false),
            )
    }

    @Test
    fun autofillManager_doNotCallCommit_partialAddition() {
        var revealFirstUsername by mutableStateOf(false)

        rule.setTestContent {
            Box(Modifier.semantics { contentType = ContentType.Username }.size(height, width))
            if (revealFirstUsername) {
                Box(
                    Modifier.semantics {
                            contentType = ContentType.Username
                            testTag = "username"
                        }
                        .size(height, width)
                )
            }
        }
        rule.runOnIdle { revealFirstUsername = true }

        // `commit` should not be called unless all autofill-able components leave the screen.
        rule.waitForIdle()
        verify(am, times(0)).commit()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(true),
            )
    }

    @Test
    fun autofillManager_doNotCallCommit_noAutofillableComponents() {
        var revealFirstUsername by mutableStateOf(true)

        rule.setTestContent {
            if (revealFirstUsername) {
                Box(Modifier.size(height, width))
            }
        }

        rule.runOnIdle { revealFirstUsername = false }

        // `commit` should not be called unless there were previously autofillable components
        // onscreen.
        rule.runOnIdle { verify(am, times(0)).commit() }
    }

    @Test
    fun autofillManager_callCommit_nodesDisappeared() {
        var revealFirstUsername by mutableStateOf(true)

        rule.setTestContent {
            if (revealFirstUsername) {
                Box(
                    Modifier.semantics {
                            testTag = "username"
                            contentType = ContentType.Username
                        }
                        .size(height, width)
                )
            }
        }
        val semanticsId = rule.onNodeWithTag("username").semanticsId()

        rule.runOnIdle { revealFirstUsername = false }

        // `commit` should be called when all autofill-able components leaves the screen.
        rule.waitForIdle()
        verify(am, times(1)).commit()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(semanticsId),
                isVisible = eq(false),
            )
    }

    @Test
    fun autofillManager_callCommit_nodesDisappearedAndAppeared() {
        var revealFirstUsername by mutableStateOf(true)
        var revealSecondUsername by mutableStateOf(false)

        rule.setTestContent {
            if (revealFirstUsername) {
                Box(
                    Modifier.semantics {
                            testTag = "first username"
                            contentType = ContentType.Username
                        }
                        .size(height, width)
                )
            }
            if (revealSecondUsername) {
                Box(
                    Modifier.semantics {
                            testTag = "second username"
                            contentType = ContentType.Username
                        }
                        .size(height, width)
                )
            }
        }

        val firstSemanticsId = rule.onNodeWithTag("first username").semanticsId()
        rule.runOnIdle { revealFirstUsername = false }
        rule.runOnIdle { revealSecondUsername = true }
        val secondSemanticsId = rule.onNodeWithTag("second username").semanticsId()

        // `commit` should be called when an autofill-able component leaves onscreen, even when
        // another, different autofill-able component is added.
        rule.waitForIdle()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(firstSemanticsId),
                isVisible = eq(false),
            )
        verify(am, times(1)).commit()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(secondSemanticsId),
                isVisible = eq(true),
            )
    }

    @Test
    fun autofillManager_doNotCallCommit_nonAutofillRelatedNodesAddedAndDisappear() {
        var isVisible by mutableStateOf(true)
        var semanticsExist by mutableStateOf(false)

        rule.setTestContent {
            if (isVisible) {
                Box(
                    modifier =
                        Modifier.then(
                                if (semanticsExist) {
                                    Modifier.semantics { contentDescription = "contentDescription" }
                                } else {
                                    Modifier
                                }
                            )
                            .size(height, width)
                )
            }
        }

        rule.runOnIdle { semanticsExist = true }
        rule.runOnIdle { isVisible = false }

        // Adding in semantics not related to autofill should not trigger commit
        rule.runOnIdle { verify(am, never()).commit() }
    }

    @Test
    fun autofillManager_callCommit_nodesBecomeAutofillRelatedAndDisappear() {
        var isVisible by mutableStateOf(true)
        var hasContentType by mutableStateOf(false)

        rule.setTestContent {
            if (isVisible) {
                Box(
                    modifier =
                        Modifier.then(
                                if (hasContentType) {
                                    Modifier.semantics {
                                        testTag = "username"
                                        contentType = ContentType.Username
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .size(height, width)
                )
            }
        }

        rule.runOnIdle { hasContentType = true }
        val semanticsId = rule.onNodeWithTag("username").semanticsId()
        rule.runOnIdle { isVisible = false }

        // `commit` should be called when component becomes autofillable, then leaves the screen.
        rule.waitForIdle()
        verify(am, times(1)).commit()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(semanticsId),
                isVisible = eq(false),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyFillableDataChanged() {
        var changeText by mutableStateOf(false)

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        onFillData { true }
                        fillableData =
                            AndroidFillableData(
                                if (changeText) AutofillValue.forText("1234")
                                else AutofillValue.forText("5678")
                            )
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { changeText = true }

        rule.waitForIdle()
        verify(am)
            .notifyValueChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                autofillValue = eq(AutofillValue.forText("1234")),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyFillableDataChanged_fromEmpty() {
        var changeText by mutableStateOf(false)
        val initialValue = AutofillValue.forText("")
        val finalValue = AutofillValue.forText("1234")

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        onFillData { true }
                        fillableData =
                            if (changeText) {
                                AndroidFillableData(finalValue)
                            } else {
                                AndroidFillableData(initialValue)
                            }
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { changeText = true }

        rule.waitForIdle()
        verify(am)
            .notifyValueChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                autofillValue = eq(finalValue),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyFillableDataChanged_toEmpty() {
        var changeText by mutableStateOf(false)
        val initialValue = AutofillValue.forText("1234")
        val finalValue = AutofillValue.forText("")

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        onFillData { true }
                        fillableData =
                            if (changeText) {
                                AndroidFillableData(finalValue)
                            } else {
                                AndroidFillableData(initialValue)
                            }
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { changeText = true }

        rule.waitForIdle()
        verify(am)
            .notifyValueChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                autofillValue = eq(finalValue),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyFillableDataChanged_removed() {
        var hasFillableData by mutableStateOf(true)

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        onFillData { true }
                        if (hasFillableData) {
                            fillableData = AndroidFillableData(AutofillValue.forText("1234"))
                        }
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { hasFillableData = false }

        rule.waitForIdle()
        verify(am, never()).notifyValueChanged(any(), any(), any())
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(false),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyFillableDataChanged_addedEmpty() {
        var hasFillableData by mutableStateOf(false)
        val autofillValue = AutofillValue.forText("")

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        onFillData { true }
                        if (hasFillableData) {
                            fillableData = AndroidFillableData(autofillValue)
                        }
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { hasFillableData = true }

        rule.waitForIdle()
        rule.runOnIdle { verify(am, never()).notifyValueChanged(any(), any(), any()) }
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(true),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyFillableDataChanged_removedEmpty() {
        var hasFillableData by mutableStateOf(true)

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        onFillData { true }
                        if (hasFillableData) {
                            fillableData = AndroidFillableData(AutofillValue.forText(""))
                        }
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { hasFillableData = false }

        rule.waitForIdle()
        verify(am, never()).notifyValueChanged(any(), any(), any())
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(false),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyValueChanged() {
        var changeText by mutableStateOf(false)

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                        inputText = AnnotatedString(if (changeText) "1234" else "")
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { changeText = true }

        rule.waitForIdle()
        verify(am)
            .notifyValueChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                autofillValue = argThat { isText && textValue == "1234" },
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyValueChanged_toEmpty() {
        var changeText by mutableStateOf(false)

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                        inputText = AnnotatedString(if (changeText) "" else "1234")
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { changeText = true }

        rule.waitForIdle()
        verify(am)
            .notifyValueChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                autofillValue = argThat { isText && textValue == "" },
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyValueChanged_inputTextAdded() {
        var hasInputText by mutableStateOf(false)

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                        if (hasInputText) inputText = AnnotatedString("1234")
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { hasInputText = true }

        rule.waitForIdle()
        verify(am, never()).notifyValueChanged(any(), any(), any())
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(true),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyValueChanged_inputTextRemoved() {
        var hasInputText by mutableStateOf(true)

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                        if (hasInputText) inputText = AnnotatedString("1234")
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { hasInputText = false }

        rule.waitForIdle()
        verify(am, never()).notifyValueChanged(any(), any(), any())
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(false),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyValueChanged_addedEmptyInputText() {
        var hasInputText by mutableStateOf(false)

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                        if (hasInputText) inputText = AnnotatedString("")
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { hasInputText = true }

        rule.waitForIdle()
        rule.runOnIdle { verify(am, never()).notifyValueChanged(any(), any(), any()) }
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(true),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyValueChanged_removedEmptyInputText() {
        var hasInputText by mutableStateOf(true)

        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                        if (hasInputText) inputText = AnnotatedString("")
                    }
                    .size(height, width)
            )
        }

        rule.runOnIdle { hasInputText = false }

        rule.waitForIdle()
        verify(am, never()).notifyValueChanged(any(), any(), any())
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(false),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyViewEntered_previousFocusFalse() {
        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        onFillData { true }
                    }
                    .size(height, width)
                    .focusable()
            )
        }

        rule.onNodeWithTag("username").requestFocus()

        rule.waitForIdle()
        verify(am)
            .notifyViewEntered(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                bounds =
                    eq(
                        with(rule.density) {
                            Rect(0, 0, width.toPx().toInt(), height.toPx().toInt())
                        }
                    ),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyViewEntered_previousFocusFalse_onAutofillText() {
        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        onAutofillText { true }
                    }
                    .size(height, width)
                    .focusable()
            )
        }

        rule.onNodeWithTag("username").requestFocus()

        rule.waitForIdle()
        verify(am)
            .notifyViewEntered(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                bounds =
                    eq(
                        with(rule.density) {
                            Rect(0, 0, width.toPx().toInt(), height.toPx().toInt())
                        }
                    ),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notAutofillable_notifyViewEntered_previousFocusFalse() {
        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                    }
                    .size(height, width)
                    .focusable()
            )
        }

        rule.onNodeWithTag("username").requestFocus()

        rule.runOnIdle { verifyNoMoreInteractions(am) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyViewEntered_previousFocusNull() {
        rule.setTestContent {
            Box(
                modifier =
                    Modifier.semantics {
                            testTag = "username"
                            onFillData { true }
                        }
                        .size(height, width)
                        .focusable()
            )
        }

        rule.onNodeWithTag("username").requestFocus()

        rule.waitForIdle()
        verify(am)
            .notifyViewEntered(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                bounds =
                    eq(
                        with(rule.density) {
                            Rect(0, 0, width.toPx().toInt(), height.toPx().toInt())
                        }
                    ),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyViewEntered_previousFocusNull_onAutofillText() {
        rule.setTestContent {
            Box(
                modifier =
                    Modifier.semantics {
                            testTag = "username"
                            onAutofillText { true }
                        }
                        .size(height, width)
                        .focusable()
            )
        }

        rule.onNodeWithTag("username").requestFocus()

        rule.waitForIdle()
        verify(am)
            .notifyViewEntered(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                bounds =
                    eq(
                        with(rule.density) {
                            Rect(0, 0, width.toPx().toInt(), height.toPx().toInt())
                        }
                    ),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyViewExited_previousFocusTrue() {
        // Arrange.
        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        onFillData { true }
                    }
                    .size(height, width)
                    .focusable()
            )
        }
        rule.onNodeWithTag("username").requestFocus()
        val semanticsId = rule.onNodeWithTag("username").semanticsId()
        rule.runOnIdle { clearInvocations(am) }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.waitForIdle()
        verify(am).notifyViewExited(view = eq(view), semanticsId = eq(semanticsId))

        // Clearing focus in Keyboard mode reassigns initial focus.
        // Before API 28, we reassigned initial focus even in touch mode.
        // https://developer.android.com/about/versions/pie/android-9.0-changes-28#focus
        if (inputModeManager.inputMode == InputMode.Keyboard || Build.VERSION.SDK_INT < 28) {
            rule.waitForIdle()
            verify(am)
                .notifyViewEntered(
                    view = eq(view),
                    semanticsId = eq(semanticsId),
                    bounds =
                        eq(
                            with(rule.density) {
                                Rect(0, 0, width.toPx().toInt(), height.toPx().toInt())
                            }
                        ),
                )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyViewExited_previousFocusTrue_onAutofillText() {
        // Arrange.
        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "username"
                        onAutofillText { true }
                    }
                    .size(height, width)
                    .focusable()
            )
        }
        rule.onNodeWithTag("username").requestFocus()
        val semanticsId = rule.onNodeWithTag("username").semanticsId()
        rule.runOnIdle { clearInvocations(am) }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.waitForIdle()
        verify(am).notifyViewExited(view = eq(view), semanticsId = eq(semanticsId))

        // Clearing focus in Keyboard mode reassigns initial focus.
        // Before API 28, we reassigned initial focus even in touch mode.
        // https://developer.android.com/about/versions/pie/android-9.0-changes-28#focus
        if (inputModeManager.inputMode == InputMode.Keyboard || Build.VERSION.SDK_INT < 28) {
            rule.waitForIdle()
            verify(am)
                .notifyViewEntered(
                    view = eq(view),
                    semanticsId = eq(semanticsId),
                    bounds =
                        eq(
                            with(rule.density) {
                                Rect(0, 0, width.toPx().toInt(), height.toPx().toInt())
                            }
                        ),
                )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyViewExited_previouslyFocusedItemNotAutofillable() {
        rule.setTestContent {
            Box(Modifier.semantics { testTag = "username" }.size(height, width).focusable())
        }

        rule.onNodeWithTag("username").requestFocus()
        rule.runOnIdle { focusManager.clearFocus() }

        rule.runOnIdle { verifyZeroInteractions(am) }
    }

    @Ignore // TODO(b/383198004): Add support for notifyVisibilityChanged.
    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun autofillManager_notifyVisibilityChanged_disappeared() {
        var isVisible by mutableStateOf(true)

        rule.setTestContent {
            Box(
                modifier =
                    Modifier.then(if (isVisible) Modifier else Modifier.alpha(0f))
                        .semantics {
                            // visibility is related to commit, so we must have a contentType set
                            contentType = ContentType.Username
                            testTag = "username"
                        }
                        .size(width, height)
                        .focusable()
            )
        }

        rule.runOnIdle { isVisible = false }

        // After switching the flag, the autofill manager is then notified that the box has
        // become transparent.
        rule.waitForIdle()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(false),
            )
    }

    @Ignore // TODO(b/383198004): Add support for notifyVisibilityChanged.
    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun autofillManager_notifyVisibilityChanged_appeared() {
        var isVisible by mutableStateOf(false)

        rule.setTestContent {
            Box(
                modifier =
                    Modifier.then(if (isVisible) Modifier else Modifier.alpha(0f))
                        // visibility is related to commit, so we must have a contentType set
                        .semantics {
                            testTag = "username"
                            contentType = ContentType.Username
                        }
                        .size(width, height)
                        .focusable()
            )
        }

        rule.runOnIdle { isVisible = true }

        // After switching the flag, the autofill manager is then notified that the box has
        // become opaque.
        rule.waitForIdle()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("username").semanticsId()),
                isVisible = eq(true),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun autofillManager_notifyVisibilityChanged_lazyScroll() {
        // Arrange.
        lateinit var lazyListState: LazyListState

        rule.setTestContent {
            lazyListState = rememberLazyListState()
            with(LocalDensity.current) {
                LazyRow(state = lazyListState, modifier = Modifier.size(10.toDp())) {
                    items(2) {
                        Box(
                            Modifier.size(10.toDp())
                                // visibility is related to commit, so we must have a contentType
                                // set
                                .semantics {
                                    testTag = "username"
                                    contentType = ContentType.Username
                                }
                                .focusable()
                        )
                    }
                }
            }
        }

        // Act.
        val beforeId = rule.onNodeWithTag("username").semanticsId()
        rule.runOnIdle { lazyListState.requestScrollToItem(1) }
        val afterId = rule.onNodeWithTag("username").semanticsId()

        // After scrolling, one element should be removed and one should be added.
        rule.waitForIdle()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(beforeId),
                isVisible = eq(false),
            )
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(afterId),
                isVisible = eq(true),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyCommit() {
        val forwardTag = "forward_button_tag"
        var autofillManager: AutofillManager?

        rule.setTestContent {
            autofillManager = LocalAutofillManager.current
            Box(
                modifier =
                    Modifier.clickable { autofillManager?.commit() }
                        .size(height, width)
                        .testTag(forwardTag)
            )
        }

        rule.onNodeWithTag(forwardTag).performClick()

        rule.runOnIdle { verify(am).commit() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyCancel() {
        val backTag = "back_button_tag"
        var autofillManager: AutofillManager?

        rule.setTestContent {
            autofillManager = LocalAutofillManager.current
            Box(
                modifier =
                    Modifier.clickable { autofillManager?.cancel() }
                        .size(height, width)
                        .testTag(backTag)
            )
        }
        rule.onNodeWithTag(backTag).performClick()

        rule.runOnIdle { verify(am).cancel() }
    }

    @Test
    fun autofillManager_lazyColumnScroll_callsCommit() {
        lateinit var state: LazyListState
        lateinit var coroutineScope: CoroutineScope
        val count = 100

        rule.setTestContent {
            coroutineScope = rememberCoroutineScope()
            state = rememberLazyListState()

            with(LocalDensity.current) {
                LazyColumn(Modifier.fillMaxWidth().height(50.dp), state) {
                    item {
                        Box(
                            Modifier.semantics {
                                    testTag = "username"
                                    contentType = ContentType.Username
                                }
                                .size(10.toDp())
                        )
                    }
                    items(count) { Box(Modifier.size(10.toDp())) }
                }
            }
        }

        val semanticsId = rule.onNodeWithTag("username").semanticsId()
        rule.runOnIdle { coroutineScope.launch { state.scrollToItem(10) } }

        rule.waitForIdle()
        verify(am).commit()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(semanticsId),
                isVisible = eq(false),
            )
    }

    @Test
    fun autofillManager_columnScroll_doesNotCallCommit() {
        lateinit var scrollState: ScrollState
        lateinit var coroutineScope: CoroutineScope

        rule.setTestContent {
            coroutineScope = rememberCoroutineScope()
            scrollState = rememberScrollState()

            with(LocalDensity.current) {
                Column(Modifier.fillMaxWidth().height(50.dp).verticalScroll(scrollState)) {
                    Row {
                        Box(
                            Modifier.semantics { contentType = ContentType.Username }
                                .size(10.toDp())
                        )
                    }
                    repeat(50) { Box(Modifier.size(10.toDp())) }
                }
            }
        }

        rule.runOnIdle { coroutineScope.launch { scrollState.scrollTo(scrollState.maxValue / 2) } }

        // Scrolling past an element in a column is not enough to call commit
        rule.runOnIdle { verify(am, never()).commit() }
    }

    @Test
    fun autofillManager_column_nodesDisappearingCallsCommit() {
        // Arrange.
        lateinit var scrollState: ScrollState
        var autofillComponentsVisible by mutableStateOf(true)

        rule.setTestContent {
            scrollState = rememberScrollState()

            with(LocalDensity.current) {
                Column(Modifier.fillMaxWidth().height(50.dp).verticalScroll(scrollState)) {
                    if (autofillComponentsVisible) {
                        Row {
                            Box(
                                Modifier.semantics {
                                        testTag = "username"
                                        contentType = ContentType.Username
                                    }
                                    .size(10.toDp())
                            )
                        }
                    }
                    repeat(50) { Box(Modifier.size(10.toDp())) }
                }
            }
        }
        val semanticsId = rule.onNodeWithTag("username").semanticsId()

        // Act.
        rule.runOnIdle { autofillComponentsVisible = false }

        // Assert - A column disappearing will call commit
        rule.waitForIdle()
        verify(am)
            .notifyViewVisibilityChanged(
                view = eq(view),
                semanticsId = eq(semanticsId),
                isVisible = eq(false),
            )
        verify(am).commit()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_requestAutofill() {
        // Arrange.
        val semanticsModifier = TestSemanticsModifier { testTag = "TestTag" }
        rule.setTestContent { Box(Modifier.size(height, width).elementOf(semanticsModifier)) }

        // Act
        rule.runOnIdle { semanticsModifier.requestAutofill() }

        // Assert
        rule.waitForIdle()
        verify(am)
            .requestAutofill(
                view = eq(view),
                semanticsId = eq(rule.onNodeWithTag("TestTag").semanticsId()),
                bounds =
                    eq(
                        with(rule.density) {
                            Rect(0, 0, width.toPx().toInt(), height.toPx().toInt())
                        }
                    ),
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_performAutofill_callsOnFillDataAndOnAutofillText_separateSemantics() {
        // Arrange
        var autoFilledValueNewApi: FillableData? = null
        var autoFilledValueOldApi: String? = null
        var autofillManager: AndroidAutofillManager? = null

        rule.setTestContent {
            autofillManager = LocalAutofillManager.current as AndroidAutofillManager
            Box(
                Modifier.semantics {
                        onFillData {
                            autoFilledValueNewApi = it
                            true
                        }
                    }
                    .semantics {
                        onAutofillText {
                            autoFilledValueOldApi = it.text
                            true
                        }
                    }
                    .testTag("autofill_node")
            )
        }
        val semanticsId = rule.onNodeWithTag("autofill_node").semanticsId()
        val autofillValue = AutofillValue.forText("autofill text")
        val values = SparseArray<AutofillValue>().apply { put(semanticsId, autofillValue) }

        // Act
        rule.runOnIdle { autofillManager?.performAutofill(values) }

        // Assert
        rule.runOnIdle {
            assertNotNull(autoFilledValueNewApi)
            assertEquals("autofill text", autoFilledValueNewApi?.toAutofillValue()?.textValue)
            assertEquals("autofill text", autoFilledValueOldApi)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_performAutofill_callsOnFillDataAndOnAutofillText() {
        // Arrange
        var autoFilledValueNewApi: FillableData? = null
        var autoFilledValueOldApi: String? = null
        var autofillManager: AndroidAutofillManager? = null

        rule.setTestContent {
            autofillManager = LocalAutofillManager.current as AndroidAutofillManager
            Box(
                Modifier.semantics {
                    testTag = "autofill_node"
                    onFillData {
                        autoFilledValueNewApi = it
                        true
                    }
                    onAutofillText {
                        autoFilledValueOldApi = it.text
                        true
                    }
                }
            )
        }
        val semanticsId = rule.onNodeWithTag("autofill_node").semanticsId()
        val autofillValue = AutofillValue.forText("autofill text")
        val values = SparseArray<AutofillValue>().apply { put(semanticsId, autofillValue) }

        // Act
        rule.runOnIdle { autofillManager?.performAutofill(values) }

        // Assert
        rule.runOnIdle {
            assertNotNull(autoFilledValueNewApi)
            assertEquals(autofillValue, autoFilledValueNewApi?.toAutofillValue())
            assertEquals("autofill text", autoFilledValueOldApi)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_performAutofill_callsOnFillData() {
        // Arrange
        var autoFilledValue: FillableData? = null
        var autofillManager: AndroidAutofillManager? = null

        rule.setTestContent {
            autofillManager = LocalAutofillManager.current as AndroidAutofillManager
            Box(
                Modifier.semantics {
                    testTag = "autofill_node"
                    onFillData {
                        autoFilledValue = it
                        true
                    }
                }
            )
        }
        val semanticsId = rule.onNodeWithTag("autofill_node").semanticsId()
        val autofillValue = AutofillValue.forText("autofill text")
        val values = SparseArray<AutofillValue>().apply { put(semanticsId, autofillValue) }

        // Act
        rule.runOnIdle { autofillManager?.performAutofill(values) }

        // Assert
        rule.runOnIdle {
            assertNotNull(autoFilledValue)
            assertEquals(autofillValue, autoFilledValue?.toAutofillValue())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_performAutofill_callsOnAutofillText() {
        // Arrange
        var autoFilledValue: String? = null
        var autofillManager: AndroidAutofillManager? = null

        rule.setTestContent {
            autofillManager = LocalAutofillManager.current as AndroidAutofillManager
            Box(
                Modifier.semantics {
                    testTag = "autofill_node"
                    onAutofillText {
                        autoFilledValue = it.text
                        true
                    }
                }
            )
        }
        val semanticsId = rule.onNodeWithTag("autofill_node").semanticsId()
        val autofillValue = AutofillValue.forText("autofill text")
        val values = SparseArray<AutofillValue>().apply { put(semanticsId, autofillValue) }

        // Act
        rule.runOnIdle { autofillManager?.performAutofill(values) }

        // Assert
        rule.runOnIdle { assertEquals("autofill text", autoFilledValue) }
    }

    private fun ComposeContentTestRule.setTestContent(composable: @Composable () -> Unit) {
        setContent {
            view = LocalView.current
            focusManager = LocalFocusManager.current
            inputModeManager = LocalInputModeManager.current
            (LocalAutofillManager.current as AndroidAutofillManager).platformAutofillManager = am
            composable()
        }
        runOnIdle { clearInvocations(am) }
    }

    private class TestSemanticsModifier(
        private val onApplySemantics: SemanticsPropertyReceiver.() -> Unit
    ) : SemanticsModifierNode, Modifier.Node() {

        override fun SemanticsPropertyReceiver.applySemantics() {
            contentType = ContentType.Username
            onApplySemantics.invoke(this)
        }
    }
}
