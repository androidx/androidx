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

package androidx.window.layout.adapter

import android.hardware.input.InputManager
import androidx.window.TestConsumer
import androidx.window.layout.util.InputDeviceInfo
import androidx.window.layout.util.InputHelper
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [InputDeviceTracker]. */
class InputDeviceTrackerTest {
    private val inputHelper = mock<InputHelper>()
    private lateinit var tracker: InputDeviceTracker
    private val executor = Runnable::run

    @Before
    fun setUp() {
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())
        whenever(inputHelper.isPhysicalKeyboardDevice(any())).thenReturn(false)
        whenever(inputHelper.isMouseDeviceEnabled(any())).thenReturn(false)
        tracker = InputDeviceTracker(inputHelper)
    }

    @After
    fun tearDown() {
        tracker.close()
    }

    @Test
    fun testAddListenerStartsTracking() {
        val listener = TestConsumer<Boolean>()
        tracker.registerListener(executor, listener)

        verify(inputHelper).registerInputDeviceListener(any(), anyOrNull())
        verify(inputHelper).getInputDeviceIds()
    }

    @Test
    fun testRemoveLastListenerStopsTracking() {
        val listener = TestConsumer<Boolean>()
        tracker.registerListener(executor, listener)
        tracker.unregisterListener(listener)

        verify(inputHelper).unregisterInputDeviceListener(any())
    }

    @Test
    fun testRestartTracking() {
        val listener = TestConsumer<Boolean>()

        // Start tracking
        tracker.registerListener(executor, listener)
        verify(inputHelper, times(1)).registerInputDeviceListener(any(), anyOrNull())

        // Stop tracking
        tracker.unregisterListener(listener)
        verify(inputHelper, times(1)).unregisterInputDeviceListener(any())

        // Restart tracking - should work!
        tracker.registerListener(executor, listener)
        verify(inputHelper, times(2)).registerInputDeviceListener(any(), anyOrNull())
    }

    @Test
    fun testOnInputDeviceChanged_triggersNotification() {
        val listener = TestConsumer<Boolean>()
        tracker.registerListener(executor, listener)

        val inputCaptor = argumentCaptor<InputManager.InputDeviceListener>()
        verify(inputHelper).registerInputDeviceListener(inputCaptor.capture(), anyOrNull())
        val inputListener = inputCaptor.firstValue

        // Initial state: Keyboard (1) connected, Mouse (2) NOT enabled
        val keyboardDescriptor = InputDeviceInfo(1, false, 0, 0, true)
        val mouseDisabledDescriptor = InputDeviceInfo(2, false, 0, 0, false)
        whenever(inputHelper.getInputDevice(1)).thenReturn(keyboardDescriptor)
        whenever(inputHelper.getInputDevice(2)).thenReturn(mouseDisabledDescriptor)
        whenever(inputHelper.isPhysicalKeyboardDevice(keyboardDescriptor)).thenReturn(true)
        whenever(inputHelper.isMouseDeviceEnabled(mouseDisabledDescriptor)).thenReturn(false)

        inputListener.onInputDeviceAdded(1)
        inputListener.onInputDeviceAdded(2)
        assertThat(tracker.isMouseAndKeyboardConnected()).isFalse()

        // Change Mouse (2) to ENABLED
        val mouseEnabledDescriptor = InputDeviceInfo(2, false, 0, 0, true)
        whenever(inputHelper.getInputDevice(2)).thenReturn(mouseEnabledDescriptor)
        whenever(inputHelper.isMouseDeviceEnabled(mouseEnabledDescriptor)).thenReturn(true)

        inputListener.onInputDeviceChanged(2)

        assertThat(tracker.isMouseAndKeyboardConnected()).isTrue()
        listener.assertValues(false, true)
    }

    @Test
    fun testIsMouseAndKeyboardConnected_initial() {
        val listener = TestConsumer<Boolean>()
        tracker.registerListener(executor, listener)
        assertThat(tracker.isMouseAndKeyboardConnected()).isFalse()
        listener.assertValues(false)
    }

    @Test
    fun testNotifyListener_whenKeyboardAndMouseAdded() {
        val listener = TestConsumer<Boolean>()
        tracker.registerListener(executor, listener)

        // Listener gets an initial update (false) after tracking starts/scan finishes
        listener.assertValues(false)

        val inputCaptor = argumentCaptor<InputManager.InputDeviceListener>()
        verify(inputHelper).registerInputDeviceListener(inputCaptor.capture(), anyOrNull())
        val inputListener = inputCaptor.firstValue

        // Add Keyboard
        val keyboardDescriptor = InputDeviceInfo(1, false, 0, 0, true)
        whenever(inputHelper.getInputDevice(1)).thenReturn(keyboardDescriptor)
        whenever(inputHelper.isPhysicalKeyboardDevice(keyboardDescriptor)).thenReturn(true)
        inputListener.onInputDeviceAdded(1)

        assertThat(tracker.isMouseAndKeyboardConnected()).isFalse()
        // No change yet, so no additional notification (still only 1 total)
        listener.assertValues(false)

        // Add Mouse
        val mouseDescriptor = InputDeviceInfo(2, false, 0, 0, true)
        whenever(inputHelper.getInputDevice(2)).thenReturn(mouseDescriptor)
        whenever(inputHelper.isMouseDeviceEnabled(mouseDescriptor)).thenReturn(true)
        inputListener.onInputDeviceAdded(2)

        assertThat(tracker.isMouseAndKeyboardConnected()).isTrue()
        listener.assertValues(false, true)
    }

    @Test
    fun testNotifyListener_whenMouseRemoved() {
        val listener = TestConsumer<Boolean>()

        // Initial state: Keyboard (1) and Mouse (2) connected
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf(1, 2))
        val keyboardDescriptor = InputDeviceInfo(1, false, 0, 0, true)
        val mouseDescriptor = InputDeviceInfo(2, false, 0, 0, true)
        whenever(inputHelper.getInputDevice(1)).thenReturn(keyboardDescriptor)
        whenever(inputHelper.getInputDevice(2)).thenReturn(mouseDescriptor)
        whenever(inputHelper.isPhysicalKeyboardDevice(keyboardDescriptor)).thenReturn(true)
        whenever(inputHelper.isMouseDeviceEnabled(mouseDescriptor)).thenReturn(true)

        tracker.registerListener(executor, listener)
        assertThat(tracker.isMouseAndKeyboardConnected()).isTrue()
        // Initial update (true)
        listener.assertValues(true, true)

        val inputCaptor = argumentCaptor<InputManager.InputDeviceListener>()
        verify(inputHelper).registerInputDeviceListener(inputCaptor.capture(), anyOrNull())
        val inputListener = inputCaptor.firstValue

        // Remove Mouse
        inputListener.onInputDeviceRemoved(2)
        assertThat(tracker.isMouseAndKeyboardConnected()).isFalse()
        listener.assertValues(true, true, false)
    }

    @Test
    fun testMultipleListeners() {
        val listener1 = TestConsumer<Boolean>()
        val listener2 = TestConsumer<Boolean>()

        tracker.registerListener(executor, listener1)
        tracker.registerListener(executor, listener2)

        // With DirectExecutor, the scan finishes immediately after listener1 registers.
        // listener1 gets one update after scan (false)
        listener1.assertValues(false)
        // listener2 gets an immediate update upon registration (false).
        // Since scan already finished, it doesn't get a second one from scan.
        listener2.assertValues(false)

        val inputCaptor = argumentCaptor<InputManager.InputDeviceListener>()
        verify(inputHelper).registerInputDeviceListener(inputCaptor.capture(), anyOrNull())
        val inputListener = inputCaptor.firstValue

        val keyboardDescriptor = InputDeviceInfo(1, false, 0, 0, true)
        val mouseDescriptor = InputDeviceInfo(2, false, 0, 0, true)
        whenever(inputHelper.getInputDevice(1)).thenReturn(keyboardDescriptor)
        whenever(inputHelper.getInputDevice(2)).thenReturn(mouseDescriptor)
        whenever(inputHelper.isPhysicalKeyboardDevice(keyboardDescriptor)).thenReturn(true)
        whenever(inputHelper.isMouseDeviceEnabled(mouseDescriptor)).thenReturn(true)

        inputListener.onInputDeviceAdded(1)
        inputListener.onInputDeviceAdded(2)

        listener1.assertValues(false, true)
        listener2.assertValues(false, true)

        tracker.unregisterListener(listener1)
        inputListener.onInputDeviceRemoved(2)

        // listener1 was removed, so it shouldn't get the second update
        listener1.assertValues(false, true)
        // listener2 was called with true once, and with false total 2 times
        // (initial and final-after-removal)
        listener2.assertValues(false, true, false)
    }
}
