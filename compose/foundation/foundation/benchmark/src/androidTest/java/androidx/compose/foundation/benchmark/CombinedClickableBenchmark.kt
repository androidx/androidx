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

package androidx.compose.foundation.benchmark

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.benchmark.lazy.MotionEventHelper
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class CombinedClickableBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun performPointerInputClick_combinedClickable_withNoLongClickDefined() {
        benchmarkRule.runBenchmarkFor({
            CombinedClickablePerformClickTestCase(longClickDefined = false)
        }) {
            lateinit var case: CombinedClickablePerformClickTestCase
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

    @Test
    fun performPointerInputClick_combinedClickable_withLongClickDefined() {
        benchmarkRule.runBenchmarkFor({
            CombinedClickablePerformClickTestCase(longClickDefined = true)
        }) {
            lateinit var case: CombinedClickablePerformClickTestCase
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

    @Test
    fun performPointerInputDoubleClick_combinedClickable_withLongClickDefined() {
        benchmarkRule.runBenchmarkFor({ CombinedClickablePerformDoubleClickTestCase() }) {
            lateinit var case: CombinedClickablePerformDoubleClickTestCase
            lateinit var rootView: View

            benchmarkRule.runOnUiThread {
                doFramesUntilNoChangesPending()
                case = getTestCase()
                rootView = getHostView()
            }

            val motionEventHelper = MotionEventHelper(rootView)
            var expectedDoubleClickCount = case.doubleClickCount
            val doubleTapMinTimeMillis = case.doubleTapMinTimeMillis
            benchmarkRule.measureRepeatedOnUiThread {
                assertThat(case.isPressed).isFalse()
                val viewCenter = Offset(rootView.measuredWidth / 2f, rootView.measuredHeight / 2f)
                motionEventHelper.sendEvent(MotionEvent.ACTION_DOWN, viewCenter)
                assertThat(case.isPressed).isTrue()
                motionEventHelper.sendEvent(MotionEvent.ACTION_UP, Offset.Zero)
                assertThat(case.isPressed).isFalse()
                motionEventHelper.sendEvent(
                    MotionEvent.ACTION_DOWN,
                    viewCenter,
                    timeDelta = doubleTapMinTimeMillis!! * 2,
                )
                assertThat(case.isPressed).isTrue()
                motionEventHelper.sendEvent(MotionEvent.ACTION_UP, Offset.Zero)
                assertThat(case.isPressed).isFalse()
                expectedDoubleClickCount++
                assertThat(case.doubleClickCount).isEqualTo(expectedDoubleClickCount)
                assertThat(case.clickCount).isEqualTo(0)
            }
        }
    }
}

private class CombinedClickablePerformClickTestCase(private val longClickDefined: Boolean) :
    ComposeTestCase {
    var clickCount = 0
        private set

    var isPressed = false
        private set

    private val indication = EmptyIndication { isPressed = it }

    @Composable
    override fun Content() {
        CompositionLocalProvider(LocalIndication provides indication) {
            Box(
                Modifier.fillMaxSize()
                    .combinedClickable(
                        onClick = { clickCount++ },
                        onLongClick =
                            if (longClickDefined) {
                                {}
                            } else null,
                    )
            )
        }
    }
}

private class CombinedClickablePerformDoubleClickTestCase : ComposeTestCase {
    var clickCount = 0
        private set

    var doubleClickCount = 0
        private set

    var isPressed = false
        private set

    var doubleTapMinTimeMillis: Long? = null
        private set

    private val indication = EmptyIndication { isPressed = it }

    @Composable
    override fun Content() {
        CompositionLocalProvider(LocalIndication provides indication) {
            doubleTapMinTimeMillis = LocalViewConfiguration.current.doubleTapMinTimeMillis
            Box(
                Modifier.fillMaxSize()
                    .combinedClickable(
                        onClick = { clickCount++ },
                        onDoubleClick = { doubleClickCount++ },
                        onLongClick = {},
                    )
            )
        }
    }
}
