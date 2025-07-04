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

package androidx.compose.foundation.benchmark

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.benchmark.lazy.MotionEventHelper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.DelegatableNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ClickableBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun performPointerInputClick() {
        benchmarkRule.runBenchmarkFor({ ClickablePerformClickTestCase() }) {
            lateinit var case: ClickablePerformClickTestCase
            lateinit var rootView: View

            benchmarkRule.runOnUiThread {
                doFramesUntilNoChangesPending()
                case = getTestCase()
                rootView = getHostView()
            }

            val motionEventHelper = MotionEventHelper(rootView)
            var expectedClickCount = case.clickCount
            benchmarkRule.measureRepeatedOnUiThread {
                assertThat(case.isPressed).isFalse()
                val viewCenter = Offset(rootView.measuredWidth / 2f, rootView.measuredHeight / 2f)
                motionEventHelper.sendEvent(MotionEvent.ACTION_DOWN, viewCenter)
                assertThat(case.isPressed).isTrue()
                motionEventHelper.sendEvent(MotionEvent.ACTION_UP, Offset.Zero)
                assertThat(case.isPressed).isFalse()
                expectedClickCount++
                assertThat(case.clickCount).isEqualTo(expectedClickCount)
            }
        }
    }
}

private class ClickablePerformClickTestCase : ComposeTestCase {
    var clickCount = 0
        private set

    var isPressed = false
        private set

    private val indication = EmptyIndication()

    @Composable
    override fun Content() {
        CompositionLocalProvider(LocalIndication provides indication) {
            Box(Modifier.fillMaxSize().clickable(onClick = { clickCount++ }))
        }
    }

    private inner class EmptyIndication : IndicationNodeFactory {

        override fun create(interactionSource: InteractionSource): DelegatableNode =
            EmptyIndicationInstance(interactionSource)

        override fun hashCode(): Int = -1

        override fun equals(other: Any?) = other === this

        private inner class EmptyIndicationInstance(
            private val interactionSource: InteractionSource
        ) : Modifier.Node() {

            // it is a simplified version of what is happening in the real indications like ripple.
            // as delivering interactions updates is a crucial part of what is happening during
            // click, we should have it as part of this benchmark.
            override fun onAttach() {
                coroutineScope.launch {
                    var pressCount = 0
                    interactionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> pressCount++
                            is PressInteraction.Release -> pressCount--
                        }
                        isPressed = pressCount > 0
                    }
                }
            }
        }
    }
}
