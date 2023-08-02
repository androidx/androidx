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

package androidx.compose.ui.input.pointer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SuspendingPointerInputFilterCoroutineJobTest {
    @OptIn(ExperimentalTestApi::class)
    @get:Rule
    val rule = createComposeRule(Dispatchers.Main)

    @Test
    @LargeTest
    fun isPointerInputJobStillActive_cancelPointerEvent_assertsTrue() {
        val latch = CountDownLatch(1)

        var repeatActualNumber = 0
        val repeatExpectedNumber = 4

        // To create Pointer Events:
        val emitter = PointerInputChangeEmitter()
        val change = emitter.nextChange(Offset(5f, 5f))

        // Used to manually trigger a PointerEvent created from our PointerInputChange.
        val suspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
            coroutineScope {
                awaitEachGesture {
                    // First down event (triggered)
                    awaitFirstDown()
                    // Up event won't ever come since we never trigger the event, times out as
                    // a cancellation
                    waitForUpOrCancellation()

                    // By running this code after down/up, we are making sure the block of code
                    // for handling pointer input events is still active despite the cancel pointer
                    // input being called later.
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        repeat(repeatExpectedNumber) {
                            repeatActualNumber++
                            delay(100)
                        }
                        latch.countDown()
                    }
                }
            }
        }

        rule.setContent {
            Box(
                modifier = elementFor(
                    key1 = Unit,
                    instance = suspendingPointerInputModifierNode as Modifier.Node
                )
            )
        }

        rule.runOnIdle {
            suspendingPointerInputModifierNode.onPointerEvent(
                change.toPointerEvent(),
                PointerEventPass.Main,
                IntSize(10, 10)
            )
        }

        suspendingPointerInputModifierNode.onCancelPointerInput()

        val resultOfLatch = latch.await(3000, TimeUnit.MILLISECONDS)
        assertTrue("Waiting for coroutine's tasks to finish", resultOfLatch)
        assertEquals(repeatExpectedNumber, repeatActualNumber)
    }
}
