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

import android.app.Activity
import android.content.Context
import android.hardware.input.InputManager
import android.util.DisplayMetrics
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.window.TestConsumer
import androidx.window.layout.WindowEngagementInfo.EngagementMode
import androidx.window.layout.util.EngagementModeHelper
import androidx.window.layout.util.InputDeviceInfo
import androidx.window.layout.util.InputHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for [EngagementModeBackendApi0] class. */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class EngagementModeBackendTest {
    private val context = mock<Activity>()
    private val appContext = mock<Context>()
    private val engagementModeHelper = mock<EngagementModeHelper>()
    private val inputHelper = mock<InputHelper>()
    private lateinit var inputDeviceTracker: InputDeviceTracker
    private lateinit var tracker: EngagementModeBackendApi0

    @Before
    fun setUp() {
        whenever(context.applicationContext).thenReturn(appContext)
        whenever(context.resources).thenReturn(mock())
        whenever(context.resources.displayMetrics).thenReturn(DisplayMetrics())

        inputDeviceTracker = InputDeviceTracker(inputHelper)
        tracker = EngagementModeBackendApi0(engagementModeHelper, inputDeviceTracker)
    }

    @Test
    fun testDefaultEngagementMode() {
        assertThat(tracker.engagementMode(context)).isEqualTo(EngagementMode.TOUCH)
    }

    @Test
    fun testRegisterCallbackStartsTracking() {
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())
        val callback = TestConsumer<EngagementMode>()
        val executor = Runnable::run

        tracker.addEngagementLayoutChangeCallback(context, executor, callback)

        verify(inputHelper).registerInputDeviceListener(any(), anyOrNull())
        verify(context).registerComponentCallbacks(any())
    }

    @Test
    fun testUnregisterCallbackStopsTracking() {
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())
        val callback = TestConsumer<EngagementMode>()
        val executor = Runnable::run

        tracker.addEngagementLayoutChangeCallback(context, executor, callback)
        tracker.removeEngagementLayoutChangeCallback(callback)

        verify(inputHelper).unregisterInputDeviceListener(any())
        verify(context).unregisterComponentCallbacks(any())
    }

    @Test
    fun testRegisterTwoActivities() {
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())
        val callback = TestConsumer<EngagementMode>()
        val executor = Runnable::run

        tracker.addEngagementLayoutChangeCallback(context, executor, callback)

        verify(inputHelper).registerInputDeviceListener(any(), anyOrNull())
        verify(context).registerComponentCallbacks(any())

        val secondActivity = mock<Activity>()

        tracker.addEngagementLayoutChangeCallback(secondActivity, executor, callback)
        verify(context).registerComponentCallbacks(any())
    }

    @Test
    fun testRegisterSameCallbackTwice() {
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())
        val callback = TestConsumer<EngagementMode>()
        val executor = Runnable::run

        tracker.addEngagementLayoutChangeCallback(context, executor, callback)
        tracker.addEngagementLayoutChangeCallback(context, executor, callback)

        callback.assertValue(EngagementMode.TOUCH)
        verify(context, times(1)).registerComponentCallbacks(any())
    }

    @Test
    fun testUnregisterTwoActivities_differentCallbacks() {
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())
        val callback = TestConsumer<EngagementMode>()
        val secondCallback = TestConsumer<EngagementMode>()
        val executor = Runnable::run
        val secondActivity = mock<Activity>()

        tracker.addEngagementLayoutChangeCallback(context, executor, callback)
        tracker.addEngagementLayoutChangeCallback(secondActivity, executor, secondCallback)
        tracker.removeEngagementLayoutChangeCallback(callback)

        verify(context).unregisterComponentCallbacks(any())

        tracker.removeEngagementLayoutChangeCallback(secondCallback)

        verify(secondActivity).unregisterComponentCallbacks(any())
        verify(inputHelper).unregisterInputDeviceListener(any())
    }

    @Test
    fun testUnregisterTwoActivities_sameCallback() {
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())
        val callback = TestConsumer<EngagementMode>()
        val executor = Runnable::run
        val secondActivity = mock<Activity>()

        tracker.addEngagementLayoutChangeCallback(context, executor, callback)
        verify(context).registerComponentCallbacks(any())

        tracker.addEngagementLayoutChangeCallback(secondActivity, executor, callback)
        verify(secondActivity, never()).registerComponentCallbacks(any())

        tracker.removeEngagementLayoutChangeCallback(callback)

        // The first activity should be unregistered, but NOT the second one as it was never
        // registered.
        verify(context).unregisterComponentCallbacks(any())
        verify(secondActivity, never()).unregisterComponentCallbacks(any())
        verify(inputHelper).unregisterInputDeviceListener(any())
    }

    @Test
    fun testEngagementModeChanges_whenCriteriaMet() {
        val callback = TestConsumer<EngagementMode>()
        val executor = Runnable::run

        // Mock large display but NO devices initially
        whenever(engagementModeHelper.hasLargeEnoughDisplay(any())).thenReturn(true)
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())

        tracker.addEngagementLayoutChangeCallback(context, executor, callback)

        // Should receive initial TOUCH
        callback.assertValues(EngagementMode.TOUCH)

        val inputCaptor = argumentCaptor<InputManager.InputDeviceListener>()
        verify(inputHelper).registerInputDeviceListener(inputCaptor.capture(), anyOrNull())
        val inputListener = inputCaptor.firstValue

        // Mock Keyboard added
        val keyboardDescriptor = InputDeviceInfo(1, false, 0, 0, true)
        whenever(inputHelper.getInputDevice(1)).thenReturn(keyboardDescriptor)
        whenever(inputHelper.isPhysicalKeyboardDevice(keyboardDescriptor)).thenReturn(true)
        inputListener.onInputDeviceAdded(1)

        // Still TOUCH as mouse is missing
        callback.assertValues(EngagementMode.TOUCH)

        // Mock Mouse added
        val mouseDescriptor = InputDeviceInfo(2, false, 0, 0, true)
        whenever(inputHelper.getInputDevice(2)).thenReturn(mouseDescriptor)
        whenever(inputHelper.isMouseDeviceEnabled(mouseDescriptor)).thenReturn(true)
        inputListener.onInputDeviceAdded(2)

        assertThat(tracker.engagementMode(context)).isEqualTo(EngagementMode.PRECISE_POINTER)
        callback.assertValues(EngagementMode.TOUCH, EngagementMode.PRECISE_POINTER)
    }

    @Test
    fun testEngagementModeChanges_mouseAndKeyboardSource() {
        val callback = TestConsumer<EngagementMode>()
        val executor = Runnable::run

        // Mock large display but NO devices initially
        whenever(engagementModeHelper.hasLargeEnoughDisplay(any())).thenReturn(true)
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())

        tracker.addEngagementLayoutChangeCallback(context, executor, callback)

        // Should receive initial DIRECT_TOUCH
        callback.assertValues(EngagementMode.TOUCH)

        val inputCaptor = argumentCaptor<InputManager.InputDeviceListener>()
        verify(inputHelper).registerInputDeviceListener(inputCaptor.capture(), anyOrNull())
        val inputListener = inputCaptor.firstValue

        // Mock Keyboard added
        val descriptor = InputDeviceInfo(1, false, 0, 0, true)
        whenever(inputHelper.getInputDevice(1)).thenReturn(descriptor)
        whenever(inputHelper.isPhysicalKeyboardDevice(descriptor)).thenReturn(true)
        whenever(inputHelper.isMouseDeviceEnabled(descriptor)).thenReturn(true)
        inputListener.onInputDeviceAdded(1)

        assertThat(tracker.engagementMode(context)).isEqualTo(EngagementMode.PRECISE_POINTER)
        callback.assertValues(EngagementMode.TOUCH, EngagementMode.PRECISE_POINTER)
    }

    @Test
    fun testEngagementMode_multipleActivities() {
        val activity1 = mock<Activity>()
        val activity2 = mock<Activity>()
        whenever(activity1.applicationContext).thenReturn(appContext)
        whenever(activity2.applicationContext).thenReturn(appContext)

        val callback1 = TestConsumer<EngagementMode>()
        val callback2 = TestConsumer<EngagementMode>()
        val executor = Runnable::run

        // Set up: activity1 on large display, activity2 on small display
        whenever(engagementModeHelper.hasLargeEnoughDisplay(activity1)).thenReturn(true)
        whenever(engagementModeHelper.hasLargeEnoughDisplay(activity2)).thenReturn(false)
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())

        tracker.addEngagementLayoutChangeCallback(activity1, executor, callback1)
        tracker.addEngagementLayoutChangeCallback(activity2, executor, callback2)

        val inputCaptor = argumentCaptor<InputManager.InputDeviceListener>()
        verify(inputHelper).registerInputDeviceListener(inputCaptor.capture(), anyOrNull())
        val inputListener = inputCaptor.firstValue

        // Simulate mouse and keyboard added
        val keyboardDescriptor = InputDeviceInfo(1, false, 0, 0, true)
        val mouseDescriptor = InputDeviceInfo(2, false, 0, 0, true)
        whenever(inputHelper.getInputDevice(1)).thenReturn(keyboardDescriptor)
        whenever(inputHelper.getInputDevice(2)).thenReturn(mouseDescriptor)

        whenever(inputHelper.isPhysicalKeyboardDevice(keyboardDescriptor)).thenReturn(true)
        whenever(inputHelper.isMouseDeviceEnabled(mouseDescriptor)).thenReturn(true)
        inputListener.onInputDeviceAdded(1)
        inputListener.onInputDeviceAdded(2)

        // Activity1 should be PRECISE_POINTER, Activity2 should be DIRECT_TOUCH
        assertThat(tracker.engagementMode(activity1)).isEqualTo(EngagementMode.PRECISE_POINTER)
        assertThat(tracker.engagementMode(activity2)).isEqualTo(EngagementMode.TOUCH)

        callback1.assertValues(EngagementMode.TOUCH, EngagementMode.PRECISE_POINTER)
        callback2.assertValues(EngagementMode.TOUCH)
    }

    @Test
    fun testUnregisterOneOfTwoCallbacks_sameContext() {
        whenever(inputHelper.getInputDeviceIds()).thenReturn(intArrayOf())
        val callback1 = TestConsumer<EngagementMode>()
        val callback2 = TestConsumer<EngagementMode>()
        val executor = Runnable::run

        tracker.addEngagementLayoutChangeCallback(context, executor, callback1)
        tracker.addEngagementLayoutChangeCallback(context, executor, callback2)

        verify(context, times(1)).registerComponentCallbacks(any())

        tracker.removeEngagementLayoutChangeCallback(callback1)

        // Context should NOT be unregistered yet because callback2 is still active.
        verify(context, never()).unregisterComponentCallbacks(any())

        tracker.removeEngagementLayoutChangeCallback(callback2)

        // Now it should be unregistered.
        verify(context, times(1)).unregisterComponentCallbacks(any())
    }
}
