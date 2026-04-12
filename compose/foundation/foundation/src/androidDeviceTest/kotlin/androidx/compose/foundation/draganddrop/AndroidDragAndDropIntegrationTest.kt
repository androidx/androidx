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

package androidx.compose.foundation.draganddrop

import android.app.UiAutomation
import android.content.ClipData
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.content.assertClipData
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Note to maintainers: Because this test injects motion events using [UiAutomation], if a test
 * fails, it is possible that the input stream is left in an undefined state. Therefore, if a test
 * fails, it is possible that other tests may also fail, even though they may pass if run
 * individually from a clean state.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidDragAndDropIntegrationTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun touchscreen_longPress_plainTextDragAndDrop() {
        val sourceClipData = ClipData.newPlainText("testClipData", "Test Clip Data")
        var droppedClipData: ClipData? = null
        var isActive = false

        val dropTarget =
            object : DragAndDropTarget {
                override fun onStarted(event: DragAndDropEvent) {
                    isActive = true
                }

                override fun onEnded(event: DragAndDropEvent) {
                    isActive = false
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    droppedClipData = ClipData(event.toAndroidDragEvent().clipData)
                    return true
                }
            }
        lateinit var viewConfiguration: ViewConfiguration

        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            Column {
                Spacer(
                    Modifier.size(100.dp)
                        .background(Color.Blue)
                        .testTag("TestDropSource")
                        .dragAndDropSource { _ ->
                            DragAndDropTransferData(
                                clipData = sourceClipData,
                                flags = View.DRAG_FLAG_GLOBAL,
                            )
                        }
                )
                Spacer(
                    Modifier.size(100.dp)
                        .background(Color.Red)
                        .testTag("TestDropTarget")
                        .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dropTarget)
                )
            }
        }

        // Compute the locations of the source and target in screen coordinates in preparation
        // for raw motion event injection
        val testDropSourceCenterScreenCoordinates: Offset =
            rule.onNodeWithTag("TestDropSource").fetchSemanticsNode().let { node ->
                node.positionOnScreen + node.size.center.toOffset()
            }
        val testDropTargetCenterScreenCoordinates: Offset =
            rule.onNodeWithTag("TestDropTarget").fetchSemanticsNode().let { node ->
                node.positionOnScreen + node.size.center.toOffset()
            }

        // Use raw motion event injection to move around the dragged object at the system level,
        // which can leave the bounds of the app in the general case
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()

        // Press down on the source component
        automation.injectMotionEvent(
            downTime = downTime,
            action = MotionEvent.ACTION_DOWN,
            screenOffset = testDropSourceCenterScreenCoordinates,
            source = InputDevice.SOURCE_TOUCHSCREEN,
            toolType = MotionEvent.TOOL_TYPE_FINGER,
        )

        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)
        rule.waitForIdle()

        assertTrue(isActive)

        // Move to the drop component
        automation.injectMotionEvent(
            downTime = downTime,
            action = MotionEvent.ACTION_MOVE,
            screenOffset = testDropTargetCenterScreenCoordinates,
            source = InputDevice.SOURCE_TOUCHSCREEN,
            toolType = MotionEvent.TOOL_TYPE_FINGER,
        )

        assertTrue(isActive)

        // Release to trigger the drop
        automation.injectMotionEvent(
            downTime = downTime,
            action = MotionEvent.ACTION_UP,
            screenOffset = testDropTargetCenterScreenCoordinates,
            source = InputDevice.SOURCE_TOUCHSCREEN,
            toolType = MotionEvent.TOOL_TYPE_FINGER,
        )

        rule.waitForIdle()

        assertClipData(assertNotNull(droppedClipData)).isEqualToClipData(sourceClipData, true)
        assertFalse(isActive)
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun touchscreen_drag_doesNotCompleteDragAndDrop() {
        val sourceClipData = ClipData.newPlainText("testClipData", "Test Clip Data")
        var droppedClipData: ClipData? = null
        var isActive = false

        val dropTarget =
            object : DragAndDropTarget {
                override fun onStarted(event: DragAndDropEvent) {
                    isActive = true
                }

                override fun onEnded(event: DragAndDropEvent) {
                    isActive = false
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    droppedClipData = ClipData(event.toAndroidDragEvent().clipData)
                    return true
                }
            }

        rule.setContent {
            WithTouchSlop(0f) {
                Column {
                    Spacer(
                        Modifier.size(100.dp)
                            .background(Color.Blue)
                            .testTag("TestDropSource")
                            .dragAndDropSource { _ ->
                                DragAndDropTransferData(
                                    clipData = sourceClipData,
                                    flags = View.DRAG_FLAG_GLOBAL,
                                )
                            }
                    )
                    Spacer(
                        Modifier.size(100.dp)
                            .background(Color.Red)
                            .testTag("TestDropTarget")
                            .dragAndDropTarget(
                                shouldStartDragAndDrop = { true },
                                target = dropTarget,
                            )
                    )
                }
            }
        }

        // Compute the locations of the source and target in screen coordinates in preparation
        // for raw motion event injection
        val testDropSourceCenterScreenCoordinates: Offset =
            rule.onNodeWithTag("TestDropSource").fetchSemanticsNode().let { node ->
                node.positionOnScreen + node.size.center.toOffset()
            }
        val testDropTargetCenterScreenCoordinates: Offset =
            rule.onNodeWithTag("TestDropTarget").fetchSemanticsNode().let { node ->
                node.positionOnScreen + node.size.center.toOffset()
            }

        // Use raw motion event injection to move around the dragged object at the system level,
        // which can leave the bounds of the app in the general case
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()

        // Press down on the source component
        automation.injectMotionEvent(
            downTime = downTime,
            action = MotionEvent.ACTION_DOWN,
            screenOffset = testDropSourceCenterScreenCoordinates,
            source = InputDevice.SOURCE_TOUCHSCREEN,
            toolType = MotionEvent.TOOL_TYPE_FINGER,
        )

        rule.waitForIdle()

        assertFalse(isActive)

        repeat(10) { index ->
            // Move incrementally to the drop component
            automation.injectMotionEvent(
                downTime = downTime,
                action = MotionEvent.ACTION_MOVE,
                screenOffset =
                    lerp(
                        testDropSourceCenterScreenCoordinates,
                        testDropTargetCenterScreenCoordinates,
                        (index + 1) / 10f,
                    ),
                source = InputDevice.SOURCE_TOUCHSCREEN,
                toolType = MotionEvent.TOOL_TYPE_FINGER,
            )
            rule.waitForIdle()
            assertFalse(isActive)
        }

        // Release to trigger the drop
        automation.injectMotionEvent(
            downTime = downTime,
            action = MotionEvent.ACTION_UP,
            screenOffset = testDropTargetCenterScreenCoordinates,
            source = InputDevice.SOURCE_TOUCHSCREEN,
            toolType = MotionEvent.TOOL_TYPE_FINGER,
        )

        rule.waitForIdle()

        assertNull(droppedClipData)
        assertFalse(isActive)
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun mouse_drag_plainTextDragAndDrop() {
        val sourceClipData = ClipData.newPlainText("testClipData", "Test Clip Data")
        var droppedClipData: ClipData? = null
        var isActive = false

        val dropTarget =
            object : DragAndDropTarget {
                override fun onStarted(event: DragAndDropEvent) {
                    isActive = true
                }

                override fun onEnded(event: DragAndDropEvent) {
                    isActive = false
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    droppedClipData = ClipData(event.toAndroidDragEvent().clipData)
                    return true
                }
            }

        rule.setContent {
            Column {
                Spacer(
                    Modifier.size(100.dp)
                        .background(Color.Blue)
                        .testTag("TestDropSource")
                        .dragAndDropSource { _ ->
                            DragAndDropTransferData(
                                clipData = sourceClipData,
                                flags = View.DRAG_FLAG_GLOBAL,
                            )
                        }
                )
                Spacer(
                    Modifier.size(100.dp)
                        .background(Color.Red)
                        .testTag("TestDropTarget")
                        .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dropTarget)
                )
            }
        }

        // Compute the locations of the source and target in screen coordinates in preparation
        // for raw motion event injection
        val testDropSourceCenterScreenCoordinates: Offset =
            rule.onNodeWithTag("TestDropSource").fetchSemanticsNode().let { node ->
                node.positionOnScreen + node.size.center.toOffset()
            }
        val testDropTargetCenterScreenCoordinates: Offset =
            rule.onNodeWithTag("TestDropTarget").fetchSemanticsNode().let { node ->
                node.positionOnScreen + node.size.center.toOffset()
            }

        // Use raw motion event injection to move around the dragged object at the system level,
        // which can leave the bounds of the app in the general case
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()

        // Press down on the source component
        automation.injectMotionEvent(
            downTime = downTime,
            action = MotionEvent.ACTION_DOWN,
            screenOffset = testDropSourceCenterScreenCoordinates,
            source = InputDevice.SOURCE_MOUSE,
            toolType = MotionEvent.TOOL_TYPE_MOUSE,
        )

        rule.waitForIdle()

        assertFalse(isActive)

        repeat(10) { index ->
            // Move incrementally to the drop component
            automation.injectMotionEvent(
                downTime,
                MotionEvent.ACTION_MOVE,
                lerp(
                    testDropSourceCenterScreenCoordinates,
                    testDropTargetCenterScreenCoordinates,
                    (index + 1) / 10f,
                ),
                source = InputDevice.SOURCE_MOUSE,
                toolType = MotionEvent.TOOL_TYPE_MOUSE,
            )
            rule.waitForIdle()
        }

        assertTrue(isActive)

        // Release to trigger the drop
        automation.injectMotionEvent(
            downTime = downTime,
            action = MotionEvent.ACTION_UP,
            screenOffset = testDropTargetCenterScreenCoordinates,
            source = InputDevice.SOURCE_MOUSE,
            toolType = MotionEvent.TOOL_TYPE_MOUSE,
        )

        rule.waitForIdle()

        assertClipData(assertNotNull(droppedClipData)).isEqualToClipData(sourceClipData, true)
        assertFalse(isActive)
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun mouse_longPress_doesNotTriggerDragAndDrop() {
        val sourceClipData = ClipData.newPlainText("testClipData", "Test Clip Data")
        var droppedClipData: ClipData? = null
        var isActive = false

        val dropTarget =
            object : DragAndDropTarget {
                override fun onStarted(event: DragAndDropEvent) {
                    isActive = true
                }

                override fun onEnded(event: DragAndDropEvent) {
                    isActive = false
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    droppedClipData = ClipData(event.toAndroidDragEvent().clipData)
                    return true
                }
            }
        lateinit var viewConfiguration: ViewConfiguration

        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            Column {
                Spacer(
                    Modifier.size(100.dp)
                        .background(Color.Blue)
                        .testTag("TestDropSource")
                        .dragAndDropSource { _ ->
                            DragAndDropTransferData(
                                clipData = sourceClipData,
                                flags = View.DRAG_FLAG_GLOBAL,
                            )
                        }
                )
                Spacer(
                    Modifier.size(100.dp)
                        .background(Color.Red)
                        .testTag("TestDropTarget")
                        .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dropTarget)
                )
            }
        }

        // Compute the locations of the source and target in screen coordinates in preparation
        // for raw motion event injection
        val testDropSourceCenterScreenCoordinates: Offset =
            rule.onNodeWithTag("TestDropSource").fetchSemanticsNode().let { node ->
                node.positionOnScreen + node.size.center.toOffset()
            }

        // Use raw motion event injection to move around the dragged object at the system level,
        // which can leave the bounds of the app in the general case
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()

        // Press down on the source component
        automation.injectMotionEvent(
            downTime = downTime,
            action = MotionEvent.ACTION_DOWN,
            screenOffset = testDropSourceCenterScreenCoordinates,
            source = InputDevice.SOURCE_MOUSE,
            toolType = MotionEvent.TOOL_TYPE_MOUSE,
        )

        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)
        rule.waitForIdle()

        assertFalse(isActive)

        // Release to clean up
        automation.injectMotionEvent(
            downTime = downTime,
            action = MotionEvent.ACTION_UP,
            screenOffset = testDropSourceCenterScreenCoordinates,
            source = InputDevice.SOURCE_MOUSE,
            toolType = MotionEvent.TOOL_TYPE_MOUSE,
        )

        rule.waitForIdle()

        assertNull(droppedClipData)
        assertFalse(isActive)
    }
}

private fun UiAutomation.injectMotionEvent(
    downTime: Long,
    action: Int,
    screenOffset: Offset,
    source: Int,
    toolType: Int,
    eventTime: Long = SystemClock.uptimeMillis(),
) {
    MotionEvent.obtain(
            /* downTime = */ downTime,
            /* eventTime = */ eventTime,
            /* action = */ action,
            /* pointerCount = */ 1,
            /* pointerProperties = */ arrayOf(
                PointerProperties().apply {
                    id = 0
                    this.toolType = toolType
                }
            ),
            /* pointerCoords = */ arrayOf(
                PointerCoords().apply {
                    x = screenOffset.x
                    y = screenOffset.y
                }
            ),
            /* metaState = */ 0,
            /* buttonState = */ 0,
            /* xPrecision = */ 1f,
            /* yPrecision = */ 1f,
            /* deviceId = */ 0,
            /* edgeFlags = */ 0,
            /* source = */ source,
            /* flags = */ 0,
        )
        .run {
            injectInputEvent(this, false)
            recycle()
        }
}
