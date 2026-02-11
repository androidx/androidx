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

package androidx.compose.ui.test.injectionscope.trackpad

import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.testutils.WithViewConfiguration
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.TrackpadInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTrackpadInput
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.DataPoint
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.verify
import androidx.compose.ui.test.util.verifyEvents
import androidx.compose.ui.test.v2.runComposeUiTest

@OptIn(ExperimentalTestApi::class)
object Common {
    val PrimaryButton = PointerButtons(MouseButton.Primary.buttonId)
    val PrimarySecondaryButton =
        PointerButtons(MouseButton.Primary.buttonId or MouseButton.Secondary.buttonId)
    val SecondaryButton = PointerButtons(MouseButton.Secondary.buttonId)

    private const val DoubleClickMin = 40L
    private const val DoubleClickMax = 200L
    const val DefaultDoubleClickTimeMillis = (DoubleClickMin + DoubleClickMax) / 2
    const val DefaultLongClickTimeMillis = 300L
    private val testViewConfiguration =
        TestViewConfiguration(
            doubleTapMinTimeMillis = DoubleClickMin,
            doubleTapTimeoutMillis = DoubleClickMax,
            longPressTimeoutMillis = DefaultLongClickTimeMillis,
        )

    fun runTrackpadInputInjectionTest(
        trackpadInput: TrackpadInjectionScope.() -> Unit,
        vararg eventVerifiers: DataPoint.() -> Unit,
    ): Unit = runComposeUiTest {
        mainClock.autoAdvance = false
        val recorder = SinglePointerInputRecorder()
        setContent { WithViewConfiguration(testViewConfiguration) { ClickableTestBox(recorder) } }
        onNodeWithTag(ClickableTestBox.defaultTag).performTrackpadInput(trackpadInput)
        runOnIdle { recorder.verifyEvents(*eventVerifiers) }
    }

    /** Verifies [DataPoint]s for events that are expected to come from a trackpad */
    @OptIn(ExperimentalComposeUiApi::class)
    fun DataPoint.verifyTrackpadEvent(
        expectedTimestamp: Long,
        expectedEventType: PointerEventType,
        expectedDown: Boolean,
        expectedPosition: Offset,
        expectedButtons: PointerButtons = PointerButtons(0),
        expectedPointerType: PointerType =
            if (ComposeUiFlags.isTrackpadGestureHandlingEnabled) {
                PointerType.Mouse
            } else {
                PointerType.Touch
            },
    ) {
        verify(
            expectedTimestamp = expectedTimestamp,
            expectedId = null,
            expectedDown = expectedDown,
            expectedPosition = expectedPosition,
            expectedPointerType = expectedPointerType,
            expectedEventType = expectedEventType,
            expectedButtons = expectedButtons,
        )
    }
}
