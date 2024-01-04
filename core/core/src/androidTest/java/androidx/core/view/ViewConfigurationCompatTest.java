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

package androidx.core.view;

import static android.view.MotionEvent.AXIS_X;
import static android.view.MotionEvent.AXIS_Y;

import static androidx.core.view.InputDeviceCompat.SOURCE_ROTARY_ENCODER;
import static androidx.core.view.InputDeviceCompat.SOURCE_TOUCHSCREEN;
import static androidx.core.view.MotionEventCompat.AXIS_SCROLL;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.input.InputManager;
import android.view.InputDevice;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ViewConfigurationCompatTest {
    @Mock Resources mResourcesMock;
    @Mock ViewConfiguration mViewConfigMock;
    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    @SdkSuppress(minSdkVersion = 16, maxSdkVersion = 33)
    public void scaledFlingThresholds_withDeviceParams_apiPre34() {
        InputDevice device = findInputDevice(SOURCE_TOUCHSCREEN);
        if (device == null) {
            return;
        }
        when(mViewConfigMock.getScaledMinimumFlingVelocity()).thenReturn(10);
        when(mViewConfigMock.getScaledMaximumFlingVelocity()).thenReturn(20);

        assertFlingThresholds(
                mViewConfigMock,
                device.getId(),
                AXIS_X,
                SOURCE_TOUCHSCREEN,
                /* minVel= */ 10,
                /* maxVel= */ 20);
        assertFlingThresholds(
                mViewConfigMock,
                device.getId(),
                AXIS_Y,
                SOURCE_TOUCHSCREEN,
                /* minVel= */ 10,
                /* maxVel= */ 20);
    }

    @Test
    @SdkSuppress(minSdkVersion = 16, maxSdkVersion = 33)
    public void scaledFlingThresholds_realTouchScreenDevice_apiPre34() {
        InputDevice touchScreenDevice = findInputDevice(SOURCE_TOUCHSCREEN);
        if (touchScreenDevice == null) {
            return;
        }
        ViewConfiguration vc = ViewConfiguration.get(mContext);

        assertFlingThresholds(
                vc,
                touchScreenDevice.getId(),
                AXIS_X,
                SOURCE_TOUCHSCREEN,
                vc.getScaledMinimumFlingVelocity(),
                vc.getScaledMaximumFlingVelocity());
        assertFlingThresholds(
                vc,
                touchScreenDevice.getId(),
                AXIS_Y,
                SOURCE_TOUCHSCREEN,
                vc.getScaledMinimumFlingVelocity(),
                vc.getScaledMaximumFlingVelocity());
    }

    @Test
    @SdkSuppress(minSdkVersion = 16, maxSdkVersion = 33)
    public void scaledFlingThresholds_realRotaryEncoderDevice_hasNoAndroidResForFling_apiPre34() {
        InputDevice rotaryEncoderDevice = findInputDevice(SOURCE_ROTARY_ENCODER);
        if (rotaryEncoderDevice == null) {
            return;
        }
        when(mViewConfigMock.getScaledMinimumFlingVelocity()).thenReturn(10);
        when(mViewConfigMock.getScaledMaximumFlingVelocity()).thenReturn(20);
        mockAndroidResource("config_viewMinRotaryEncoderFlingVelocity", "dimen", /* resId= */ 0);
        mContext = spy(mContext);
        when(mContext.getResources()).thenReturn(mResourcesMock);

        assertFlingThresholds(
                mViewConfigMock,
                rotaryEncoderDevice.getId(),
                AXIS_SCROLL,
                SOURCE_ROTARY_ENCODER,
                /* minVel= */ Integer.MAX_VALUE,
                /* maxVel= */ Integer.MIN_VALUE);
    }

    @Test
    @SdkSuppress(minSdkVersion = 16, maxSdkVersion = 33)
    public void scaledMinFlingVelocity_realRotaryEncoderDevice_hasAndroidResForFling_apiPre34() {
        InputDevice rotaryEncoderDevice = findInputDevice(SOURCE_ROTARY_ENCODER);
        if (rotaryEncoderDevice == null) {
            return;
        }
        int deviceId = rotaryEncoderDevice.getId();
        mockAndroidResource("config_viewMinRotaryEncoderFlingVelocity", "dimen", /* resId= */ 1);
        when(mResourcesMock.getDimensionPixelSize(1)).thenReturn(100);
        mContext = spy(mContext);
        when(mContext.getResources()).thenReturn(mResourcesMock);

        assertEquals(
                100,
                ViewConfigurationCompat.getScaledMinimumFlingVelocity(
                        mContext, mViewConfigMock, deviceId, AXIS_SCROLL, SOURCE_ROTARY_ENCODER));
    }

    @Test
    @SdkSuppress(minSdkVersion = 16, maxSdkVersion = 33)
    public void scaledMinFlingVelocity_realRotaryEncoderDevice_hasAndroidResForNoFling_apiPre34() {
        InputDevice rotaryEncoderDevice = findInputDevice(SOURCE_ROTARY_ENCODER);
        if (rotaryEncoderDevice == null) {
            return;
        }
        int deviceId = rotaryEncoderDevice.getId();
        mockAndroidResource("config_viewMinRotaryEncoderFlingVelocity", "dimen", /* resId= */ 1);
        when(mResourcesMock.getDimensionPixelSize(1)).thenReturn(-1);
        mContext = spy(mContext);
        when(mContext.getResources()).thenReturn(mResourcesMock);

        assertEquals(
                Integer.MAX_VALUE,
                ViewConfigurationCompat.getScaledMinimumFlingVelocity(
                        mContext, mViewConfigMock, deviceId, AXIS_SCROLL, SOURCE_ROTARY_ENCODER));
    }

    @Test
    @SdkSuppress(minSdkVersion = 16, maxSdkVersion = 33)
    public void scaledMaxFlingVelocity_hasAndroidResForFling_realRotaryEncoderDevice_apiPre34() {
        InputDevice rotaryEncoderDevice = findInputDevice(SOURCE_ROTARY_ENCODER);
        if (rotaryEncoderDevice == null) {
            return;
        }
        int deviceId = rotaryEncoderDevice.getId();
        mockAndroidResource("config_viewMaxRotaryEncoderFlingVelocity", "dimen", /* resId= */ 1);
        when(mResourcesMock.getDimensionPixelSize(1)).thenReturn(100);
        mContext = spy(mContext);
        when(mContext.getResources()).thenReturn(mResourcesMock);

        assertEquals(
                100,
                ViewConfigurationCompat.getScaledMaximumFlingVelocity(
                        mContext, mViewConfigMock, deviceId, AXIS_SCROLL, SOURCE_ROTARY_ENCODER));
    }

    @Test
    @SdkSuppress(minSdkVersion = 16, maxSdkVersion = 33)
    public void scaledMaxFlingVelocity_hasAndroidResForNoFling_realRotaryEncoderDevice_apiPre34() {
        InputDevice rotaryEncoderDevice = findInputDevice(SOURCE_ROTARY_ENCODER);
        if (rotaryEncoderDevice == null) {
            return;
        }
        int deviceId = rotaryEncoderDevice.getId();
        mockAndroidResource("config_viewMaxRotaryEncoderFlingVelocity", "dimen", 1);
        when(mResourcesMock.getDimensionPixelSize(1)).thenReturn(-1);
        mContext = spy(mContext);
        when(mContext.getResources()).thenReturn(mResourcesMock);

        assertEquals(
                Integer.MIN_VALUE,
                ViewConfigurationCompat.getScaledMaximumFlingVelocity(
                        mContext, mViewConfigMock, deviceId, AXIS_SCROLL, SOURCE_ROTARY_ENCODER));
    }

    @Test
    @SdkSuppress(minSdkVersion = 16, maxSdkVersion = 33)
    public void scaledFlingThresholds_invalidInputDeviceParameters_apiPre34() {
        assertFlingThresholds(
                mViewConfigMock,
                -10, // Bad InputDevice ID.
                AXIS_X,
                SOURCE_TOUCHSCREEN,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE);

        InputDevice touchScreenDevice = findInputDevice(SOURCE_TOUCHSCREEN);
        if (touchScreenDevice == null) {
            return;
        }

        assertFlingThresholds(
                mViewConfigMock,
                touchScreenDevice.getId(),
                /* axis= */ -1, // Axis cannot be a negative value.
                SOURCE_TOUCHSCREEN,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE);
        assertFlingThresholds(
                mViewConfigMock,
                touchScreenDevice.getId(),
                AXIS_SCROLL, // Touch does not report on AXIS_SCROLL.
                SOURCE_TOUCHSCREEN,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE);
        assertFlingThresholds(
                mViewConfigMock,
                touchScreenDevice.getId(),
                AXIS_X,
                /* source = */ -1, // Source cannot be a negative value.
                Integer.MAX_VALUE,
                Integer.MIN_VALUE);
        assertFlingThresholds(
                mViewConfigMock,
                touchScreenDevice.getId(),
                AXIS_X,
                SOURCE_ROTARY_ENCODER, // Touch does not have rotary encoder source.
                Integer.MAX_VALUE,
                Integer.MIN_VALUE);
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void scaledVelocityThresholds_withDeviceParams_api34Plus() {
        when(mViewConfigMock.getScaledMinimumFlingVelocity(1, 2, 3)).thenReturn(100);
        when(mViewConfigMock.getScaledMaximumFlingVelocity(1, 2, 3)).thenReturn(200);

        assertEquals(
                100,
                ViewConfigurationCompat.getScaledMinimumFlingVelocity(
                        mContext, mViewConfigMock, 1, 2, 3));
        assertEquals(
                200,
                ViewConfigurationCompat.getScaledMaximumFlingVelocity(
                        mContext, mViewConfigMock, 1, 2, 3));
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void scaledFlingThresholds_realTouchScreenDevice_api34Plus() {
        InputDevice touchScreenDevice = findInputDevice(SOURCE_TOUCHSCREEN);
        if (touchScreenDevice == null) {
            return;
        }

        assertFlingThresholdsEqualPlatformImpl(
                ViewConfiguration.get(mContext),
                touchScreenDevice.getId(),
                SOURCE_TOUCHSCREEN,
                AXIS_X);
        assertFlingThresholdsEqualPlatformImpl(
                ViewConfiguration.get(mContext),
                touchScreenDevice.getId(),
                SOURCE_TOUCHSCREEN,
                AXIS_Y);
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void scaledFlingThresholds_realRotaryEncoderDevice_api34Plus() {
        InputDevice rotaryEncoderDevice = findInputDevice(SOURCE_ROTARY_ENCODER);
        if (rotaryEncoderDevice == null) {
            return;
        }

        assertFlingThresholdsEqualPlatformImpl(
                ViewConfiguration.get(mContext),
                rotaryEncoderDevice.getId(),
                SOURCE_ROTARY_ENCODER,
                AXIS_SCROLL);
    }

    @SdkSuppress(minSdkVersion = 34)
    private void assertFlingThresholdsEqualPlatformImpl(
            ViewConfiguration vc, int inputDeviceId, int axis, int source) {
        assertFlingThresholds(
                vc,
                inputDeviceId,
                axis,
                source,
                vc.getScaledMinimumFlingVelocity(inputDeviceId, axis, source),
                vc.getScaledMaximumFlingVelocity(inputDeviceId, axis, source));
    }

    private void assertFlingThresholds(
            ViewConfiguration vc, int inputDeviceId, int axis, int source, int minVel, int maxVel) {
        assertEquals(
                minVel,
                ViewConfigurationCompat.getScaledMinimumFlingVelocity(
                        mContext, vc, inputDeviceId, axis, source));
        assertEquals(
                maxVel,
                ViewConfigurationCompat.getScaledMaximumFlingVelocity(
                        mContext, vc, inputDeviceId, axis, source));
    }

    @SdkSuppress(minSdkVersion = 16)
    @Nullable
    private InputDevice findInputDevice(int source) {
        InputManager inputManager =
                (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
        int[] deviceIds = inputManager.getInputDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice inputDevice = inputManager.getInputDevice(deviceId);
            if (inputDevice == null) {
                continue;
            }
            List<InputDevice.MotionRange> motionRangeList = inputDevice.getMotionRanges();
            for (InputDevice.MotionRange motionRange : motionRangeList) {
                if (motionRange.getSource() == source) {
                    return inputDevice;
                }
            }
        }
        return null;
    }

    private void mockAndroidResource(String name, String defType, int resId) {
        when(mResourcesMock.getIdentifier(name, defType, /* defPackage= */ "android"))
                .thenReturn(resId);
    }
}
