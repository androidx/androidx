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

package androidx.compose.ui.interop

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.up
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIButton
import platform.UIKit.UIEvent

internal class InteropInteractionModeTest {
    @Test
    fun testUIButtonTapCooperativeDefault() = runUIKitInstrumentedTestWithInterop { overlay ->
        var beganCount = 0
        var endedCount = 0

        setContent {
            UIKitView(
                factory = {
                    TouchReactingView(
                        onTouchBegin = { beganCount++ },
                        onTouchEnd = { endedCount++ })
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("Button"),
                properties = UIKitInteropProperties(
                    UIKitInteropInteractionMode.Cooperative(),
                    placedAsOverlay = overlay
                )
            )
        }

        val touch = findNodeWithTag("Button")
            .touchDown()

        assertEquals(0, beganCount)
        assertEquals(0, endedCount)

        delay(UIKitInteropInteractionMode.Cooperative.DefaultDelayMillis + 50L)

        assertEquals(1, beganCount)
        assertEquals(0, endedCount)

        touch.up()

        assertEquals(1, beganCount)
        assertEquals(1, endedCount)
    }

    @Test
    fun testUIButtonCooperativeTouchUpBeforeDelay() = runUIKitInstrumentedTestWithInterop { overlay ->
        var beganCount = 0
        var endedCount = 0

        setContent {
            UIKitView(
                factory = { TouchReactingView(onTouchBegin = { beganCount++ }, onTouchEnd = { endedCount++ }) },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("Button"),
                properties = UIKitInteropProperties(
                    interactionMode = UIKitInteropInteractionMode.Cooperative(delayMillis = 1000),
                    placedAsOverlay = overlay
                )
            )
        }

        val touch = findNodeWithTag("Button")
            .touchDown()

        assertEquals(0, beganCount)
        assertEquals(0, endedCount)

        delay(500)

        touch.up()

        assertEquals(1, beganCount)
        assertEquals(1, endedCount)
    }

    @Test
    fun testUIButtonCooperativeTouchUpAfterDelay() = runUIKitInstrumentedTestWithInterop { overlay ->
        var beganCount = 0
        var endedCount = 0

        setContent {
            UIKitView(
                factory = { TouchReactingView(onTouchBegin = { beganCount++ }, onTouchEnd = { endedCount++ }) },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("Button"),
                properties = UIKitInteropProperties(
                    interactionMode = UIKitInteropInteractionMode.Cooperative(delayMillis = 800),
                    placedAsOverlay = overlay
                )
            )
        }

        val touch = findNodeWithTag("Button")
            .touchDown()

        assertEquals(0, beganCount)
        assertEquals(0, endedCount)

        delay(500)

        assertEquals(0, beganCount)
        assertEquals(0, endedCount)

        delay(350)

        assertEquals(1, beganCount)
        assertEquals(0, endedCount)

        touch.up()

        assertEquals(1, beganCount)
        assertEquals(1, endedCount)
    }

    @Test
    fun testUIButtonNonInteractive() = runUIKitInstrumentedTest { // overlay ->
        var beganCount = 0
        var endedCount = 0

        setContent {
            UIKitView(
                factory = { TouchReactingView(onTouchBegin = { beganCount++ }, onTouchEnd = { endedCount++ }) },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("Button"),
                properties = UIKitInteropProperties(
                    interactionMode = null,
                    placedAsOverlay = true
                )
            )
        }

        val touch = findNodeWithTag("Button")
            .touchDown()

        assertEquals(0, beganCount)
        assertEquals(0, endedCount)

        touch.up()

        assertEquals(0, beganCount)
        assertEquals(0, endedCount)
    }

    @Test
    fun testUIButtonTapNonCooperative() = runUIKitInstrumentedTestWithInterop { overlay ->
        var beganCount = 0
        var endedCount = 0

        setContent {
            UIKitView(
                factory = { TouchReactingView(onTouchBegin = { beganCount++ }, onTouchEnd = { endedCount++ }) },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("Button"),
                properties = UIKitInteropProperties(
                    UIKitInteropInteractionMode.NonCooperative,
                    placedAsOverlay = overlay
                )
            )
        }

        val touch = findNodeWithTag("Button")
            .touchDown()
        waitForIdle()

        assertEquals(1, beganCount, "Touch down - beganCount")
        assertEquals(0, endedCount, "Touch down - endedCount")

        touch.up()

        assertEquals(1,beganCount, "Touch up - beganCount")
        assertEquals(1, endedCount, "Touch up - endedCount")
    }

    @Test
    fun testUIButtonLongTapNonCooperative() = runUIKitInstrumentedTestWithInterop { overlay ->
        var beganCount = 0
        var endedCount = 0

        setContent {
            UIKitView(
                factory = { TouchReactingView(onTouchBegin = { beganCount++ }, onTouchEnd = { endedCount++ }) },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("Button"),
                properties = UIKitInteropProperties(
                    UIKitInteropInteractionMode.NonCooperative,
                    placedAsOverlay = overlay
                )
            )
        }

        val touch = findNodeWithTag("Button")
            .touchDown()

        assertEquals(1, beganCount)
        assertEquals(0, endedCount)

        delay(500)

        touch.up()

        assertEquals(1,beganCount)
        assertEquals(1, endedCount)
    }

    @Test
    fun testUIButtonDoubleTapNonCooperative() = runUIKitInstrumentedTestWithInterop { overlay ->
        var beganCount = 0
        var endedCount = 0

        setContent {
            UIKitView(
                factory = { TouchReactingView(onTouchBegin = { beganCount++ }, onTouchEnd = { endedCount++ }) },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("Button"),
                properties = UIKitInteropProperties(
                    UIKitInteropInteractionMode.NonCooperative,
                    placedAsOverlay = overlay
                )
            )
        }

        findNodeWithTag("Button")
            .doubleTap()

        assertEquals(2, beganCount)
        assertEquals(2, endedCount)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class TouchReactingView(
    val onTouchBegin: () -> Unit,
    val onTouchEnd: () -> Unit,
): UIButton(frame = CGRectZero.readValue()) {
    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)
        onTouchBegin()
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)
        onTouchEnd()
    }
}