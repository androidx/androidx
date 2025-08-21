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

package androidx.compose.ui.input

import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.input.indirect.IndirectTouchEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.indirect.indirectPrimaryDirectionalScrollAxis
import androidx.test.filters.SdkSuppress
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@OptIn(ExperimentalIndirectTouchTypeApi::class)
@RunWith(MockitoJUnitRunner::class)
@SdkSuppress(minSdkVersion = 28) // Mocks for final classes can only be done on 28 and higher
class IndirectTouchEventWithInputDeviceMockTest {
    @Mock lateinit var mockInputDevice: InputDevice // Mock InputDevice

    @Mock lateinit var mockMotionRangeX: InputDevice.MotionRange // Mock MotionRange for X-axis

    @Mock lateinit var mockMotionRangeY: InputDevice.MotionRange // Mock MotionRange for Y-axis

    // We'll also mock MotionEvent to control what its getDevice() method returns
    @Mock lateinit var mockMotionEvent: MotionEvent

    private fun mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
        xRange: Float?,
        yRange: Float?,
    ): MotionEvent {
        if (xRange != null) {
            // Mock max motion ranges
            `when`(mockMotionRangeX.range).thenReturn(xRange)
            // Mock InputDevice returns our mocked MotionRanges
            `when`(mockInputDevice.getMotionRange(MotionEvent.AXIS_X)).thenReturn(mockMotionRangeX)
        } else {
            `when`(mockInputDevice.getMotionRange(MotionEvent.AXIS_X)).thenReturn(null)
        }

        if (yRange != null) {
            // Mock max motion ranges
            `when`(mockMotionRangeY.range).thenReturn(yRange)
            // Mock InputDevice returns our mocked MotionRanges
            `when`(mockInputDevice.getMotionRange(MotionEvent.AXIS_Y)).thenReturn(mockMotionRangeY)
        } else {
            `when`(mockInputDevice.getMotionRange(MotionEvent.AXIS_Y)).thenReturn(null)
        }

        // Mock MotionEvent returns our mocked InputDevice
        `when`(mockMotionEvent.device).thenReturn(mockInputDevice)

        // Mock isFromSource for SOURCE_TOUCH_NAVIGATION on the MotionEvent
        `when`(mockMotionEvent.isFromSource(InputDevice.SOURCE_TOUCH_NAVIGATION)).thenReturn(true)

        return mockMotionEvent
    }

    @Test
    fun indirectScrollAxis_notSourceTouchNavigation_throwsException() {
        var exceptionThrown = false

        try {
            indirectPrimaryDirectionalScrollAxis(mockMotionEvent)
        } catch (exception: IllegalArgumentException) {
            exceptionThrown = true
            assertEquals("MotionEvent must be a touch navigation source", exception.message)
        }
        assertEquals(true, exceptionThrown)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_bothXRangeAndYRangeAreNull_scrollAxisNone() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = null,
                    yRange = null,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.None, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_xRangeNotNullAndYRangeNull_scrollAxisX() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 100f,
                    yRange = null,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.X, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_yRangeNotNullAndXRangeNull_scrollAxisX() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = null,
                    yRange = 100f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.Y, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_xRange100AndYRange0_scrollAxisX() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 100f,
                    yRange = 0f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.X, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_yRange100AndXRange0_scrollAxisY() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 0f,
                    yRange = 100f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.Y, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_1to1AspectRatio_scrollAxisNone() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 1000f,
                    yRange = 1000f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.None, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_2to1XAspectRatio_scrollAxisNone() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 2000f,
                    yRange = 1000f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.None, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_3to1XAspectRatio_scrollAxisNone() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 3000f,
                    yRange = 1000f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.None, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_4to1XAspectRatio_scrollAxisNone() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 4000f,
                    yRange = 1000f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.None, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_4to1YAspectRatio_scrollAxisNone() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 1000f,
                    yRange = 4000f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.None, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_5to1XAspectRatio_scrollAxisX() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 5000f,
                    yRange = 1000f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.X, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_5to1YAspectRatio_scrollAxisY() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 1000f,
                    yRange = 5000f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.Y, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_6to1XAspectRatio_scrollAxisX() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 6000f,
                    yRange = 1000f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.X, indirectScrollAxis)
        mockMotionEvent.recycle()
    }

    @Test
    fun indirectScrollAxis_6to1YAspectRatio_scrollAxisY() {
        val indirectScrollAxis =
            indirectPrimaryDirectionalScrollAxis(
                mockMotionEventWithSourceTouchNavigationAndMaxMotionRanges(
                    xRange = 1000f,
                    yRange = 6000f,
                )
            )
        assertEquals(IndirectTouchEventPrimaryDirectionalMotionAxis.Y, indirectScrollAxis)
        mockMotionEvent.recycle()
    }
}
