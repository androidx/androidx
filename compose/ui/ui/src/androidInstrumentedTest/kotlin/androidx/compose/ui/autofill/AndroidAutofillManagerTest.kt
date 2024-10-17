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
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags.isSemanticAutofillEnabled
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@SdkSuppress(minSdkVersion = 26)
@RequiresApi(Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class AndroidAutofillManagerTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var composeView: View
    private lateinit var autofillManagerMock: AutofillManagerWrapper

    private val autofillEventLoopIntervalMs = 100L

    private val height = 200.dp
    private val width = 200.dp

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_empty() {
        val usernameTag = "username_tag"

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics { contentType = ContentType.Username }
                    .size(height, width)
                    .testTag(usernameTag)
            )
        }

        rule.runOnIdle { verifyNoMoreInteractions(autofillManagerMock) }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyValueChanged() {
        val usernameTag = "username_tag"
        var changeText by mutableStateOf(false)

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                        editableText = AnnotatedString(if (changeText) "1234" else "****")
                    }
                    .size(height, width)
                    .testTag(usernameTag)
            )
        }

        rule.runOnIdle { changeText = true }

        rule.runOnIdle { verify(autofillManagerMock).notifyValueChanged(any(), any()) }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyViewEntered_previousFocusFalse() {
        val usernameTag = "username_tag"
        var hasFocus by mutableStateOf(false)

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                        focused = hasFocus
                    }
                    .size(height, width)
                    .testTag(usernameTag)
            )
        }

        rule.runOnIdle { hasFocus = true }

        rule.runOnIdle { verify(autofillManagerMock).notifyViewEntered(any(), any()) }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 33)
    fun autofillManager_showAutofillDialog_previousFocusFalse() {
        val usernameTag = "username_tag"
        var hasFocus by mutableStateOf(false)

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                        focused = hasFocus
                    }
                    .size(height, width)
                    .testTag(usernameTag)
            )
        }

        rule.runOnIdle { hasFocus = true }

        rule.runOnIdle { verify(autofillManagerMock).showAutofillDialog(any()) }
        rule.runOnIdle { verify(autofillManagerMock).notifyViewEntered(any(), any()) }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyViewEntered_previousFocusNull() {
        val usernameTag = "username_tag"
        var hasFocus by mutableStateOf(false)

        rule.setContentWithAutofillEnabled {
            Box(
                modifier =
                    if (hasFocus)
                        Modifier.semantics {
                                contentType = ContentType.Username
                                contentDataType = ContentDataType.Text
                                focused = hasFocus
                            }
                            .size(height, width)
                            .testTag(usernameTag)
                    else plainVisibleModifier(usernameTag)
            )
        }

        rule.runOnIdle { hasFocus = true }

        rule.runOnIdle { verify(autofillManagerMock).notifyViewEntered(any(), any()) }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 33)
    fun autofillManager_showAutofillDialog_previousFocusNull() {
        val usernameTag = "username_tag"
        var hasFocus by mutableStateOf(false)

        rule.setContentWithAutofillEnabled {
            Box(
                modifier =
                    if (hasFocus)
                        Modifier.semantics {
                                contentType = ContentType.Username
                                contentDataType = ContentDataType.Text
                                focused = hasFocus
                            }
                            .size(height, width)
                            .testTag(usernameTag)
                    else plainVisibleModifier(usernameTag)
            )
        }

        rule.runOnIdle { hasFocus = true }

        rule.runOnIdle { verify(autofillManagerMock).showAutofillDialog(any()) }
        rule.runOnIdle { verify(autofillManagerMock).notifyViewEntered(any(), any()) }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyViewExited_previousFocusTrue() {
        val usernameTag = "username_tag"
        var hasFocus by mutableStateOf(true)

        rule.setContentWithAutofillEnabled {
            Box(
                Modifier.semantics {
                        contentType = ContentType.Username
                        contentDataType = ContentDataType.Text
                        focused = hasFocus
                    }
                    .size(height, width)
                    .testTag(usernameTag)
            )
        }

        rule.runOnIdle { hasFocus = false }

        rule.runOnIdle { verify(autofillManagerMock).notifyViewExited(any()) }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 27)
    fun autofillManager_notifyVisibilityChanged_disappeared() {
        val usernameTag = "username_tag"
        var isVisible by mutableStateOf(true)

        rule.setContentWithAutofillEnabled {
            Box(
                modifier =
                    if (isVisible) plainVisibleModifier(usernameTag)
                    else invisibleModifier(usernameTag)
            )
        }

        rule.runOnIdle { isVisible = false }

        rule.runOnIdle { verify(autofillManagerMock).notifyViewVisibilityChanged(any(), any()) }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 27)
    fun autofillManager_notifyVisibilityChanged_appeared() {
        val usernameTag = "username_tag"
        var isVisible by mutableStateOf(false)

        rule.setContentWithAutofillEnabled {
            Box(
                modifier =
                    if (isVisible) plainVisibleModifier(usernameTag)
                    else invisibleModifier(usernameTag)
            )
        }

        rule.runOnIdle { isVisible = true }

        rule.runOnIdle { verify(autofillManagerMock).notifyViewVisibilityChanged(any(), any()) }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyCommit() {
        val forwardTag = "forward_button_tag"
        var autofillManager: AutofillManager?

        rule.setContentWithAutofillEnabled {
            autofillManager = LocalAutofillManager.current
            Box(
                modifier =
                    Modifier.clickable { autofillManager?.commit() }
                        .size(height, width)
                        .testTag(forwardTag)
            )
        }

        rule.onNodeWithTag(forwardTag).performClick()

        rule.runOnIdle { verify(autofillManagerMock).commit() }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_notifyCancel() {
        val backTag = "back_button_tag"
        var autofillManager: AutofillManager?

        rule.setContentWithAutofillEnabled {
            autofillManager = LocalAutofillManager.current
            Box(
                modifier =
                    Modifier.clickable { autofillManager?.cancel() }
                        .size(height, width)
                        .testTag(backTag)
            )
        }
        rule.onNodeWithTag(backTag).performClick()

        rule.runOnIdle { verify(autofillManagerMock).cancel() }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun autofillManager_requestAutofillAfterFocus() {
        val contextMenuTag = "menu_tag"
        var autofillManager: AutofillManager?
        var hasFocus by mutableStateOf(false)

        rule.setContentWithAutofillEnabled {
            autofillManager = LocalAutofillManager.current
            Box(
                modifier =
                    if (hasFocus)
                        Modifier.semantics {
                                contentType = ContentType.Username
                                contentDataType = ContentDataType.Text
                                focused = hasFocus
                            }
                            .clickable { autofillManager?.requestAutofill() }
                            .size(height, width)
                            .testTag(contextMenuTag)
                    else plainVisibleModifier(contextMenuTag)
            )
        }

        // `requestAutofill` is always called after an element is focused
        rule.runOnIdle { hasFocus = true }
        rule.runOnIdle { verify(autofillManagerMock).notifyViewEntered(any(), any()) }

        // then `requestAutofill` is called on that same previously focused element
        rule.onNodeWithTag(contextMenuTag).performClick()
        rule.runOnIdle { verify(autofillManagerMock).requestAutofill(any(), any()) }
    }

    // ============================================================================================
    // Helper functions
    // ============================================================================================

    private fun plainVisibleModifier(testTag: String): Modifier {
        return Modifier.semantics {
                contentType = ContentType.Username
                contentDataType = ContentDataType.Text
            }
            .size(width, height)
            .testTag(testTag)
    }

    private fun invisibleModifier(testTag: String): Modifier {
        return Modifier.alpha(0f) // this will make the component invisible
            .semantics {
                contentType = ContentType.Username
                contentDataType = ContentDataType.Text
            }
            .size(width, height)
            .testTag(testTag)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.setContentWithAutofillEnabled(
        content: @Composable () -> Unit
    ) {
        autofillManagerMock = mock()

        setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            androidComposeView._autofillManager?.currentSemanticsNodesInvalidated = true
            androidComposeView._autofillManager?.autofillManager = autofillManagerMock
            isSemanticAutofillEnabled = true

            composeView = LocalView.current

            content()
        }

        runOnIdle { mainClock.advanceTimeBy(autofillEventLoopIntervalMs) }
    }
}
