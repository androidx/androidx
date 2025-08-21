/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.window

import android.content.pm.ActivityInfo
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class provides coverage for fullscreen dialog where status bar is not shown.
 * [DialogTest] covers test cases with status bar.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class DialogWithNoStatusBarTest {

    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun fullScreenDialogPortraitNotDefaultWidthDecorFitsMatchesContainerSize() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        verifyFullScreenDialogMatchesContainer()
    }

    @Test
    fun fullScreenDialogLandscapeNotDefaultWidthDecorFitsMatchesContainerSize() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        verifyFullScreenDialogMatchesContainer()
    }

    private fun verifyFullScreenDialogMatchesContainer() {
        var mainContentWidth = 0
        var mainContentHeight = 0
        var dialogWidth = 0
        var dialogHeight = 0
        rule.activityRule.scenario.onActivity {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
        rule.setContent {
            Box(
                modifier =
                    Modifier.safeDrawingPadding().fillMaxSize().onGloballyPositioned {
                        mainContentWidth = it.size.width
                        mainContentHeight = it.size.height
                    }
            ) {
                Dialog(
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = true,
                        ),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().onGloballyPositioned {
                                dialogWidth = it.size.width
                                dialogHeight = it.size.height
                            }
                    ) {}
                }
            }
        }

        rule.runOnIdle {
            assertThat(mainContentWidth).isEqualTo(dialogWidth)
            assertThat(mainContentHeight).isEqualTo(dialogHeight)
        }
    }

    @Test
    fun fullScreenDialogWithImeNotDefaultWidthDecorFitsMatchesContainerSize() {
        var mainContentWidth = 0
        var mainContentHeight = 0
        var dialogWidth = 0
        var dialogHeight = 0
        rule.activityRule.scenario.onActivity {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
        rule.setContent {
            val keyboardController = LocalSoftwareKeyboardController.current
            keyboardController?.show()
            Box(
                modifier =
                    Modifier.safeDrawingPadding().fillMaxSize().onGloballyPositioned {
                        mainContentWidth = it.size.width
                        mainContentHeight = it.size.height
                    }
            ) {
                Dialog(
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = true,
                        ),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().onGloballyPositioned {
                                dialogWidth = it.size.width
                                dialogHeight = it.size.height
                            }
                    ) {
                        TextField(
                            state = rememberTextFieldState(initialText = "Hello"),
                            label = { Text("Label") },
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(mainContentWidth).isEqualTo(dialogWidth)
            assertThat(mainContentHeight).isEqualTo(dialogHeight)
        }
    }

    @Test
    fun fullScreenDialogNotDefaultWidthNoDecorFitsMatchesContainerSize() {
        var mainContentWidth = 0
        var mainContentHeight = 0
        var dialogWidth = 0
        var dialogHeight = 0
        rule.activityRule.scenario.onActivity {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
        rule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize().onGloballyPositioned {
                        mainContentWidth = it.size.width
                        mainContentHeight = it.size.height
                    }
            ) {
                Dialog(
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false,
                        ),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().onGloballyPositioned {
                                dialogWidth = it.size.width
                                dialogHeight = it.size.height
                            }
                    ) {}
                }
            }
        }

        rule.runOnIdle {
            assertThat(mainContentWidth).isEqualTo(dialogWidth)
            assertThat(mainContentHeight).isEqualTo(dialogHeight)
        }
    }

    @Test
    fun fullScreenDialogPortraitInEdgeToEdgeNotDefaultWidthDecorFitsMatchesContainerSize() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        verifyFullScreenDialogInEdgeToEdgeMatchesContainerSize()
    }

    @Test
    fun fullScreenDialogInEdgeToEdgeLandscapeNotDefaultWidthDecorFitsMatchesContainerSize() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        verifyFullScreenDialogInEdgeToEdgeMatchesContainerSize()
    }

    private fun verifyFullScreenDialogInEdgeToEdgeMatchesContainerSize() {
        var mainContentWidth = 0
        var mainContentHeight = 0
        var dialogWidth = 0
        var dialogHeight = 0
        rule.runOnUiThread { rule.activity.enableEdgeToEdge() }
        rule.activityRule.scenario.onActivity {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().onGloballyPositioned {
                            mainContentWidth = it.size.width
                            mainContentHeight = it.size.height
                        }
                ) {
                    Dialog(
                        onDismissRequest = {},
                        properties =
                            DialogProperties(
                                usePlatformDefaultWidth = false,
                                decorFitsSystemWindows = true,
                            ),
                    ) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize().onGloballyPositioned {
                                    dialogWidth = it.size.width
                                    dialogHeight = it.size.height
                                }
                        ) {}
                    }
                }
            }
        }
        rule.runOnIdle {
            assertThat(mainContentWidth).isEqualTo(dialogWidth)
            assertThat(mainContentHeight).isEqualTo(dialogHeight)
        }
    }

    @Test
    fun fullScreenDialogInEdgeToEdgeNotDefaultWidthNoDecorFitsMatchesContainerSize() {
        var mainContentWidth = 0
        var mainContentHeight = 0
        var dialogWidth = 0
        var dialogHeight = 0
        rule.runOnUiThread { rule.activity.enableEdgeToEdge() }
        rule.activityRule.scenario.onActivity {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
        rule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize().onGloballyPositioned {
                        mainContentWidth = it.size.width
                        mainContentHeight = it.size.height
                    }
            ) {
                Dialog(
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false,
                        ),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().onGloballyPositioned {
                                dialogWidth = it.size.width
                                dialogHeight = it.size.height
                            }
                    ) {}
                }
            }
        }

        rule.runOnIdle {
            assertThat(mainContentWidth).isEqualTo(dialogWidth)
            assertThat(mainContentHeight).isEqualTo(dialogHeight)
        }
    }
}
