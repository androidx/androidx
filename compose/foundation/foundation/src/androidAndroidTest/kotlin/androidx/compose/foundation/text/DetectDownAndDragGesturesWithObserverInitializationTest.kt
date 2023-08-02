/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.sign
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DetectDownAndDragGesturesWithObserverInitializationTest {

    /**
     * The regular test dispatcher will already run the [kotlinx.coroutines.launch]es as if they
     * were [kotlinx.coroutines.CoroutineStart.UNDISPATCHED], so overwrite it with a standard
     * single parallelism dispatcher. Without this, a regression on this functionality would
     * not be caught in this test.
     */
    @OptIn(ExperimentalTestApi::class)
    @get:Rule
    val rule = createComposeRule(Dispatchers.Main)

    private val testTag = "testTag"
    private val observer = RecordingTextDragObserver()
    private val records = mutableListOf<String>()

    @Before
    fun setup() {
        rule.setContent {
            CompositionLocalProvider(
                LocalViewConfiguration provides TestViewConfiguration(
                    minimumTouchTargetSize = DpSize.Zero
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 10.dp)
                            .background(Color.Black)
                            .align(Alignment.Center)
                            .pointerInput(Unit) {
                                detectDownAndDragGesturesWithObserver(observer)
                            }
                            .testTag(testTag)
                    )
                }
            }
        }
    }

    @Test
    fun whenPressingAndMoving_expectedInteractionsRecorded() {
        rule.onNodeWithTag(testTag).performTouchInput {
            down(center)
            movePastSlopBy(Offset(1f, 1f))
            movePastSlopBy(Offset(1f, 1f))
            up()
        }

        rule.waitForIdle()

        assertThat(records)
            .containsExactly("down", "start", "drag", "drag", "stop", "up")
            .inOrder()
    }

    private fun TouchInjectionScope.movePastSlopBy(delta: Offset) {
        val slop = Offset(
            x = viewConfiguration.touchSlop * delta.x.sign,
            y = viewConfiguration.touchSlop * delta.y.sign
        )
        moveBy(delta + slop)
    }

    private inner class RecordingTextDragObserver : TextDragObserver {
        override fun onDown(point: Offset) = add("down")
        override fun onUp() = add("up")
        override fun onStart(startPoint: Offset) = add("start")
        override fun onDrag(delta: Offset) = add("drag")
        override fun onStop() = add("stop")
        override fun onCancel() = add("cancel")

        private fun add(str: String) {
            records += str
        }
    }
}
