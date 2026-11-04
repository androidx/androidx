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

package androidx.compose.ui.test.injectionscope.indirecttouch

import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import androidx.compose.testutils.expectError
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.TestContext
import androidx.compose.ui.test.TestOwner
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.sendIndirectPointerInput
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [sendIndirectPointerInput] root and window focus resolution. */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SendIndirectPointerInputTest {
    companion object {
        private val inputDeviceSize = IntSize(3082, 616)
    }

    @get:Rule val rule = createComposeRule()

    @Test
    fun sendIndirectPointerInput_noFocus_throwsAssertionError() {
        val recorder = SinglePointerInputRecorder()
        rule.setContent { ClickableTestBox(recorder) }

        expectError<AssertionError>(expectedMessage = "No focused nodes within a focused window!") {
            rule.sendIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                click()
            }
        }

        rule.runOnIdle { assertThat(recorder.events).isEmpty() }
    }

    @Test
    fun sendIndirectPointerInput_waitsForWindowFocusWhenRootNodeHasFocus() {
        val recorder = SinglePointerInputRecorder()
        lateinit var mainView: View

        rule.setContent {
            mainView = LocalView.current
            ClickableTestBox(recorder)
        }

        rule.onNodeWithTag(ClickableTestBox.defaultTag).requestFocus()

        lateinit var popupWindow: PopupWindow
        rule.runOnIdle {
            assertThat(mainView.hasFocus()).isTrue()
            popupWindow =
                PopupWindow(View(mainView.context), 100, 100, /* focusable= */ true).apply {
                    showAtLocation(mainView, Gravity.NO_GRAVITY, 0, 0)
                }
        }

        try {
            rule.waitUntil(timeoutMillis = 5_000) {
                rule.runOnUiThread { !mainView.hasWindowFocus() }
            }
            rule.runOnIdle { assertThat(mainView.hasFocus()).isTrue() }

            val unfocusedRootCheckLatch = CountDownLatch(1)
            val dismissedLatch = CountDownLatch(1)
            val dismissThread =
                thread(name = "DismissPopupAfterUnfocusedCheck") {
                    if (unfocusedRootCheckLatch.await(5, TimeUnit.SECONDS)) {
                        rule.runOnUiThread {
                            popupWindow.dismiss()
                            dismissedLatch.countDown()
                        }
                    }
                }

            val provider =
                object : SemanticsNodeInteractionsProvider by rule {
                    override fun onAllNodes(
                        matcher: SemanticsMatcher,
                        useUnmergedTree: Boolean,
                    ): SemanticsNodeInteractionCollection {
                        val delegate =
                            rule.onAllNodes(matcher, useUnmergedTree).testContext.testOwner
                        val testOwner =
                            object : TestOwner by delegate {
                                override fun <T> runOnUiThread(action: () -> T): T =
                                    delegate.runOnUiThread(action).also { result ->
                                        if (result == null) {
                                            unfocusedRootCheckLatch.countDown()
                                        }
                                    }
                            }
                        return SemanticsNodeInteractionCollection(
                            TestContext(testOwner),
                            useUnmergedTree,
                            matcher,
                        )
                    }
                }

            provider.sendIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                click()
            }

            assertThat(dismissedLatch.await(5, TimeUnit.SECONDS)).isTrue()
            dismissThread.join(5_000)
            rule.runOnIdle { assertThat(recorder.events).hasSize(3) }
        } finally {
            rule.runOnUiThread { popupWindow.dismiss() }
        }
    }
}
