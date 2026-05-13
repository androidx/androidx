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

package androidx.compose.foundation

import android.app.Instrumentation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.LocalSoundEffect
import androidx.compose.ui.platform.SoundEffect
import androidx.compose.ui.platform.SoundEffectOnInteraction
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performIndirectPointerInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTrackpadInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ClickableSoundTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun playSound_calledOnAccessibilityClick() {
        val soundEffects = FakeSoundEffect()
        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides soundEffects) {
                Box(Modifier.testTag("clickable").clickable {})
            }
        }

        rule.onNodeWithTag("clickable").performSemanticsAction(SemanticsActions.OnClick)

        assertThat(soundEffects.clickCount).isEqualTo(1)
    }

    @Test
    fun playSound_calledOnPointerClick() {
        val soundEffects = FakeSoundEffect()
        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides soundEffects) {
                Box(Modifier.testTag("clickable").size(100.dp).clickable {})
            }
        }

        rule.onNodeWithTag("clickable").performClick()

        assertThat(soundEffects.clickCount).isEqualTo(1)
    }

    @Test
    fun playSound_calledOnMouseClick() {
        val soundEffects = FakeSoundEffect()
        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides soundEffects) {
                Box(Modifier.testTag("clickable").size(100.dp).clickable {})
            }
        }

        rule.onNodeWithTag("clickable").performMouseInput { click() }

        assertThat(soundEffects.clickCount).isEqualTo(1)
    }

    @Test
    fun playSound_calledOnTrackpadClick() {
        val soundEffects = FakeSoundEffect()
        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides soundEffects) {
                Box(Modifier.testTag("clickable").size(100.dp).clickable {})
            }
        }

        rule.onNodeWithTag("clickable").performTrackpadInput { click() }

        assertThat(soundEffects.clickCount).isEqualTo(1)
    }

    @Test
    @OptIn(ExperimentalFoundationApi::class)
    fun playSound_calledOnceOnDoubleClick_playedOnFirstClick() {
        val soundEffects = FakeSoundEffect()
        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides soundEffects) {
                Box(
                    Modifier.testTag("clickable")
                        .size(100.dp)
                        .combinedClickable(onDoubleClick = {}, onClick = {})
                )
            }
        }

        // Perform first click tap
        rule.onNodeWithTag("clickable").performTouchInput {
            down(center)
            up()
        }
        // Confirm it plays immediately on the first click
        assertThat(soundEffects.clickCount).isEqualTo(1)

        // Perform second click tap in quick succession
        rule.onNodeWithTag("clickable").performTouchInput {
            down(center)
            up()
        }
        // Confirm it DOES NOT play a second time for the double-click completion
        assertThat(soundEffects.clickCount).isEqualTo(1)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun playSound_calledOnIndirectPointerClick() {
        val soundEffects = FakeSoundEffect()
        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides soundEffects) {
                Box(
                    Modifier.testTag("clickable")
                        .size(100.dp)
                        .combinedClickable(onDoubleClick = {}, onClick = {})
                )
            }
        }

        // Request focus and inject indirect directional motion down/up events inside inKeyboardMode
        InstrumentationRegistry.getInstrumentation().inKeyboardMode {
            // Request focus directly on the test thread, then wait for focus layout to settle
            // completely
            rule.onNodeWithTag("clickable").requestFocus()
            rule.waitForIdle()

            // Inject indirect directional motion down/up events in a single block with time
            // progression
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                IntSize(3082, 616),
            ) {
                down(position = Offset(50f, 50f))
                advanceEventTime(20L)
                up()
            }
        }

        assertThat(soundEffects.clickCount).isEqualTo(1)
    }

    @Test
    @OptIn(ExperimentalFoundationApi::class)
    fun playSound_notCalledOnLongClick() {
        val soundEffects = FakeSoundEffect()
        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides soundEffects) {
                Box(
                    Modifier.testTag("clickable")
                        .size(100.dp)
                        .combinedClickable(onLongClick = {}, onClick = {})
                )
            }
        }

        rule.onNodeWithTag("clickable").performTouchInput { longClick() }

        assertThat(soundEffects.clickCount).isEqualTo(0)
    }

    private fun Instrumentation.inKeyboardMode(block: () -> Unit) {
        try {
            // setInTouchMode(false) is flaky, so we press a key to put the system in non-touch
            // mode.
            sendKeyDownUpSync(Key.Grave.nativeKeyCode)
            block()
        } finally {
            setInTouchMode(true)
        }
    }

    @Test
    fun playSound_calledOnKeyClick() {
        val soundEffects = FakeSoundEffect()
        val focusRequester = FocusRequester()
        var clicked = false
        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides soundEffects) {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .clickable { clicked = true }
                        .testTag("clickable")
                )
            }
        }

        InstrumentationRegistry.getInstrumentation().inKeyboardMode {
            rule.runOnIdle { focusRequester.requestFocus() }
            rule.onNodeWithTag("clickable").performKeyInput {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            rule.waitForIdle()
        }

        assertThat(clicked).isTrue() // validate that click actually happened for debugging
        assertThat(soundEffects.clickCount).isEqualTo(1)
    }

    @Test
    fun playSound_droppedWhenSoundEffectsOnInteractionDisabled() {
        val soundEffects = FakeSoundEffect()
        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides soundEffects) {
                SoundEffectOnInteraction(enabled = false) {
                    Box(Modifier.testTag("clickable").size(100.dp).clickable {})
                }
            }
        }

        rule.onNodeWithTag("clickable").performClick()

        assertThat(soundEffects.clickCount).isEqualTo(0)
    }

    @Test
    @OptIn(ExperimentalFoundationApi::class)
    fun playSound_droppedWhenFlagDisabled() {
        val soundEffects = FakeSoundEffect()
        val originalFlag = ComposeFoundationFlags.isInteractionSoundEffectOnClickEnabled
        ComposeFoundationFlags.isInteractionSoundEffectOnClickEnabled = false
        try {
            rule.setContent {
                CompositionLocalProvider(LocalSoundEffect provides soundEffects) {
                    Box(Modifier.testTag("clickable").size(100.dp).clickable {})
                }
            }

            rule.onNodeWithTag("clickable").performClick()

            assertThat(soundEffects.clickCount).isEqualTo(0)
        } finally {
            ComposeFoundationFlags.isInteractionSoundEffectOnClickEnabled = originalFlag
        }
    }

    private class FakeSoundEffect : SoundEffect {
        var clickCount = 0

        override fun playClickSound() {
            clickCount++
        }
    }
}
