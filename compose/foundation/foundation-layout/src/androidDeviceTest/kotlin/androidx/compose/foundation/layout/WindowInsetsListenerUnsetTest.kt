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

package androidx.compose.foundation.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso
import androidx.testutils.AnimationSystemSettingsTestRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WindowInsetsListenerUnsetTest {
    @get:Rule(order = 0) val animationRule = AnimationSystemSettingsTestRule(5f)
    @get:Rule(order = 1) val rule = createAndroidComposeRule<WindowInsetsNoActionBarActivity>()

    @Before
    fun setup() {
        rule.activity.createdLatch.await(1, TimeUnit.SECONDS)
    }

    // Repro for b/491346046
    @Test
    fun validateInsetsAreUpdatedAfterListenerUnsetDuringAnimation() {
        // Setup inset listeners outside the Compose view to avoid interacting with the Compose
        // listeners
        var bottomSystemBars = 0
        var insetsAnimationEndCount = 0

        ViewCompat.setOnApplyWindowInsetsListener(
            rule.activity.window.decorView,
            { _, insets ->
                bottomSystemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                insets
            },
        )
        ViewCompat.setWindowInsetsAnimationCallback(
            rule.activity.window.decorView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: List<WindowInsetsAnimationCompat?>,
                ): WindowInsetsCompat = insets

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    insetsAnimationEndCount++
                    super.onEnd(animation)
                }
            },
        )

        var imeSpacerHeight: Int? = null
        var isTextFieldShown by mutableStateOf(false)

        rule.setContent {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = isTextFieldShown,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                ) { targetState ->
                    Column(
                        modifier =
                            Modifier.background(if (targetState) Color.Blue else Color.Red)
                                .fillMaxSize()
                    ) {
                        if (targetState) {
                            val focusRequester = remember { FocusRequester() }
                            Column(modifier = Modifier.fillMaxSize()) {
                                BasicTextField(
                                    state = rememberTextFieldState(),
                                    modifier =
                                        Modifier.fillMaxWidth().focusRequester(focusRequester),
                                )
                            }
                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Spacer(Modifier.weight(1f))
                                Box(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .background(Color.Yellow)
                                            .onSizeChanged { imeSpacerHeight = it.height }
                                ) {
                                    Spacer(
                                        Modifier.windowInsetsBottomHeight(
                                            WindowInsets.systemBars.union(WindowInsets.ime)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.waitForIdle()

        // The IME isn't shown, so the height should be the bottomSystemBars
        assertThat(imeSpacerHeight).isEqualTo(bottomSystemBars)

        isTextFieldShown = true

        // Advance time to allow the IME animation to end causing the keyboard to be fully visible
        rule.waitUntil(5000) { insetsAnimationEndCount == 1 }
        rule.waitForIdle()

        // The transition animation should have finished while the IME was animating out, leaving
        // the ime spacer height above the bottomSystemBars
        assertThat(imeSpacerHeight).isGreaterThan(bottomSystemBars)

        // Close the keyboard
        Espresso.pressBack()

        // Advance time to allow the IME animation to end causing the keyboard to be fully hidden
        rule.waitUntil(5000) { insetsAnimationEndCount == 2 }
        rule.waitForIdle()

        // Navigate back to page 1
        isTextFieldShown = false

        rule.waitForIdle()

        // The IME is no longer shown, so the height should be the bottomSystemBars again
        assertThat(imeSpacerHeight).isEqualTo(bottomSystemBars)
    }
}
