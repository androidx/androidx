/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test

import android.app.Dialog
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog as ComposeDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SetContentWindowFocusTest {

    // Regression test for b/579273335, b/579463810, and b/581075796.
    @Test
    @SdkSuppress(maxSdkVersion = 32)
    fun setContent_delayedInitialWindowFocus_doesNotResetKeyboardInputMode() =
        runAndroidComposeUiTest<ComponentActivity> {
            val hostActivity = checkNotNull(activity)
            lateinit var transientDialog: Dialog
            runOnUiThread { transientDialog = Dialog(hostActivity).apply { show() } }
            waitUntil("transientDialog has window focus", timeoutMillis = 5_000) {
                runOnUiThread {
                    transientDialog.window?.decorView?.hasWindowFocus() == true &&
                        !hostActivity.hasWindowFocus()
                }
            }

            val dismissedLatch = CountDownLatch(1)
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var inputModeManager: InputModeManager
            lateinit var scope: CoroutineScope
            var clickCount = 0

            setContent {
                scope = rememberCoroutineScope()
                inputModeManager = LocalInputModeManager.current
                DisposableEffect(Unit) {
                    @Suppress("BanThreadSleep")
                    thread(name = "DismissTransientDialog") {
                        Thread.sleep(500)
                        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
                        hostActivity.runOnUiThread {
                            transientDialog.dismiss()
                            dismissedLatch.countDown()
                        }
                    }
                    onDispose {}
                }
                Box(
                    Modifier.size(48.dp)
                        .testTag("clickable")
                        .focusRequester(focusRequester)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            clickCount++
                        }
                )
            }

            runOnIdle {
                inputModeManager.requestInputMode(InputMode.Keyboard)
                focusRequester.requestFocus()
            }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            assertThat(dismissedLatch.await(5, TimeUnit.SECONDS)).isTrue()
            waitUntil("hostActivity has window focus", timeoutMillis = 5_000) {
                runOnUiThread { hostActivity.hasWindowFocus() }
            }
            waitForIdle()

            runOnIdle {
                assertThat(inputModeManager.inputMode).isEqualTo(InputMode.Keyboard)
                assertThat(interactions).isEmpty()
            }

            onNodeWithTag("clickable").performKeyInput { pressKey(Key.Enter) }

            runOnIdle { assertThat(clickCount).isEqualTo(1) }
        }

    @Test
    fun setContent_withComposeDialog_returnsWithoutWaitingForHostActivityFocus() =
        runAndroidComposeUiTest<ComponentActivity> {
            val startNanos = System.nanoTime()
            setContent { ComposeDialog(onDismissRequest = {}) { BasicText("Dialog content") } }
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
            onNodeWithText("Dialog content").assertIsDisplayed()
            assertThat(elapsedMillis).isLessThan(WindowFocusTimeoutMillis)
        }
}
