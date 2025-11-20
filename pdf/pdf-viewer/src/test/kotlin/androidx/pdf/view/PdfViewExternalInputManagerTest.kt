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

package androidx.pdf.view

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.pdf.featureflag.PdfFeatureFlags
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class PdfViewExternalInputManagerTest {

    @Mock private lateinit var pdfView: PdfView
    private lateinit var externalInputManager: PdfViewExternalInputManager

    @Before
    fun setUp() {
        externalInputManager = PdfViewExternalInputManager(pdfView)
        PdfFeatureFlags.isExternalHardwareInteractionEnabled = true
    }

    @After
    fun tearDown() {
        PdfFeatureFlags.isExternalHardwareInteractionEnabled = false
    }

    @Test
    fun handleKeyEvent_actionUp_isNotHandledAndReturnsFalse() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_UP)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isFalse()
    }

    @Test
    fun handleKeyEvent_otherKeyCode_isNotHandledAndReturnsFalse() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_A)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isFalse()
    }

    @Test
    fun handleKeyEvent_dpadDown_ReturnsTrue() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_DPAD_DOWN)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleKeyEvent_dpadUp_ReturnsTrue() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_DPAD_UP)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleKeyEvent_ctrlZero_callsZoomToBaselineAndReturnsTrue() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_0)
        whenever(event.isCtrlPressed).thenReturn(true)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleKeyEvent_ctrlNumpadZero_callsZoomToBaselineAndReturnsTrue() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_NUMPAD_0)
        whenever(event.isCtrlPressed).thenReturn(true)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleKeyEvent_zeroWithoutCtrl_isNotHandledAndReturnsFalse() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_0)
        whenever(event.isCtrlPressed).thenReturn(false)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isFalse()
    }

    @Test
    fun handleKeyEvent_numpadZeroWithoutCtrl_isNotHandledAndReturnsFalse() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_NUMPAD_0)
        whenever(event.isCtrlPressed).thenReturn(false)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isFalse()
    }

    @Test
    fun handleKeyEvent_ctrlMinus_callsZoomOutAndReturnsTrue() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_MINUS)
        whenever(event.isCtrlPressed).thenReturn(true)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleKeyEvent_ctrlEquals_callsZoomInAndReturnsTrue() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_EQUALS)
        whenever(event.isCtrlPressed).thenReturn(true)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleKeyEvent_ctrlPlus_callsZoomInAndReturnsTrue() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_PLUS)
        whenever(event.isCtrlPressed).thenReturn(true)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleKeyEvent_minusWithoutCtrl_isNotHandledAndReturnsFalse() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_MINUS)
        whenever(event.isCtrlPressed).thenReturn(false)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isFalse()
    }

    @Test
    fun handleKeyEvent_equalsWithoutCtrl_isNotHandledAndReturnsFalse() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_EQUALS)
        whenever(event.isCtrlPressed).thenReturn(false)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isFalse()
    }

    @Test
    fun handleKeyEvent_plusWithoutCtrl_isNotHandledAndReturnsFalse() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_PLUS)
        whenever(event.isCtrlPressed).thenReturn(false)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isFalse()
    }

    @Test
    fun handleMouseEvents_actionScrollWithNegativeHscroll_ReturnsTrue() {
        val event = mock<MotionEvent>()
        whenever(event.source).thenReturn(InputDevice.SOURCE_MOUSE)
        whenever(event.action).thenReturn(MotionEvent.ACTION_SCROLL)
        whenever(event.getAxisValue(MotionEvent.AXIS_VSCROLL)).thenReturn(0f)
        whenever(event.getAxisValue(MotionEvent.AXIS_HSCROLL)).thenReturn(-1.0f)

        val handled = externalInputManager.handleMouseEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleMouseEvents_actionScrollWithNegativeVscroll_ReturnsTrue() {
        val event = mock<MotionEvent>()
        whenever(event.source).thenReturn(InputDevice.SOURCE_MOUSE)
        whenever(event.action).thenReturn(MotionEvent.ACTION_SCROLL)
        whenever(event.getAxisValue(MotionEvent.AXIS_VSCROLL)).thenReturn(-1.0f)
        whenever(event.getAxisValue(MotionEvent.AXIS_HSCROLL)).thenReturn(0f)

        val handled = externalInputManager.handleMouseEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleMouseEvents_actionScrollWithPositiveHscroll_ReturnsTrue() {
        val event = mock<MotionEvent>()
        whenever(event.source).thenReturn(InputDevice.SOURCE_MOUSE)
        whenever(event.action).thenReturn(MotionEvent.ACTION_SCROLL)
        whenever(event.getAxisValue(MotionEvent.AXIS_VSCROLL)).thenReturn(0f)
        whenever(event.getAxisValue(MotionEvent.AXIS_HSCROLL)).thenReturn(1.0f)

        val handled = externalInputManager.handleMouseEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleMouseEvents_actionScrollWithPositiveVscroll_ReturnsTrue() {
        val event = mock<MotionEvent>()
        whenever(event.source).thenReturn(InputDevice.SOURCE_MOUSE)
        whenever(event.action).thenReturn(MotionEvent.ACTION_SCROLL)
        whenever(event.getAxisValue(MotionEvent.AXIS_VSCROLL)).thenReturn(1.0f)
        whenever(event.getAxisValue(MotionEvent.AXIS_HSCROLL)).thenReturn(0f)

        val handled = externalInputManager.handleMouseEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleMouseEvents_actionScrollWithZeroVscrollAndZeroHscroll_ReturnsFalse() {
        val event = mock<MotionEvent>()
        whenever(event.source).thenReturn(InputDevice.SOURCE_MOUSE)
        whenever(event.action).thenReturn(MotionEvent.ACTION_SCROLL)
        whenever(event.getAxisValue(MotionEvent.AXIS_VSCROLL)).thenReturn(0f)
        whenever(event.getAxisValue(MotionEvent.AXIS_HSCROLL)).thenReturn(0f)

        val handled = externalInputManager.handleMouseEvent(event)

        assertThat(handled).isFalse()
    }

    @Test
    fun handleKeyEvent_ctrlC_ReturnsTrue() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_C)
        whenever(event.isCtrlPressed).thenReturn(true)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isTrue()
    }

    @Test
    fun handleKeyEvent_CtrlC_isNotHandledAndReturnsFalse() {
        val event = mock<KeyEvent>()
        whenever(event.action).thenReturn(KeyEvent.ACTION_DOWN)
        whenever(event.keyCode).thenReturn(KeyEvent.KEYCODE_C)
        whenever(event.isCtrlPressed).thenReturn(false)

        val handled = externalInputManager.handleKeyEvent(event)

        assertThat(handled).isFalse()
    }
}
