/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;

import androidx.core.util.Consumer;

import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.Executor;

/**
 * Tests for {@link InitialValueWindowBackendDecorator}
 */
public class InitialValueWindowBackendDecoratorTests {

    private WindowBackend mFakeBackend;
    private InitialValueWindowBackendDecorator mWindowBackendUnderTest;

    @Before
    public void setUp() {
        mFakeBackend = mock(WindowBackend.class);
        mWindowBackendUnderTest = new InitialValueWindowBackendDecorator(mFakeBackend);
    }

    @Test
    public void getDeviceState_delegatesCall() {
        DeviceState expected = new DeviceState(DeviceState.POSTURE_CLOSED);
        when(mFakeBackend.getDeviceState()).thenReturn(expected);

        DeviceState actual = mWindowBackendUnderTest.getDeviceState();

        assertEquals(expected, actual);
    }

    @Test
    public void getWindowLayoutInfo_delegatesCall() {
        Rect rect = new Rect(1, 2, 3, 4);
        DisplayFeature feature = new DisplayFeature(rect, DisplayFeature.TYPE_FOLD);
        WindowLayoutInfo expected = new WindowLayoutInfo(Collections.singletonList(feature));
        when(mFakeBackend.getWindowLayoutInfo(any())).thenReturn(expected);

        WindowLayoutInfo actual = mWindowBackendUnderTest.getWindowLayoutInfo(mock(Context.class));

        assertEquals(expected, actual);
    }

    @Test
    public void registerDeviceStateChangeCallback_emitsInitialValue() {
        DeviceState expected = new DeviceState(DeviceState.POSTURE_CLOSED);
        when(mFakeBackend.getDeviceState()).thenReturn(expected);
        CaptureConsumer<DeviceState> consumer = new CaptureConsumer<>();
        Executor trampolineExecutor = MoreExecutors.directExecutor();

        mWindowBackendUnderTest.registerDeviceStateChangeCallback(trampolineExecutor, consumer);

        assertEquals(expected, consumer.getCurrentState());
        verify(mFakeBackend).registerDeviceStateChangeCallback(trampolineExecutor, consumer);
    }

    @Test
    public void unregisterDeviceStateChangeCallback_delegates() {
        Consumer<DeviceState> fakeConsumer = new CaptureConsumer<>();

        mWindowBackendUnderTest.unregisterDeviceStateChangeCallback(fakeConsumer);

        verify(mFakeBackend).unregisterDeviceStateChangeCallback(fakeConsumer);
    }

    @Test
    public void registerLayoutChangeCallback_emitsInitialValue() {
        Rect rect = new Rect(1, 2, 3, 4);
        DisplayFeature feature = new DisplayFeature(rect, DisplayFeature.TYPE_FOLD);
        WindowLayoutInfo expected = new WindowLayoutInfo(Collections.singletonList(feature));
        when(mFakeBackend.getWindowLayoutInfo(any())).thenReturn(expected);
        CaptureConsumer<WindowLayoutInfo> consumer = new CaptureConsumer<>();
        Executor trampolineExecutor = MoreExecutors.directExecutor();
        Context context = mock(Context.class);

        mWindowBackendUnderTest.registerLayoutChangeCallback(context,
                trampolineExecutor, consumer);

        assertEquals(expected, consumer.getCurrentState());
        verify(mFakeBackend).registerLayoutChangeCallback(context, trampolineExecutor, consumer);
    }

    @Test
    public void unregisterLayoutChangeCallback_delegates() {
        Consumer<WindowLayoutInfo> fakeConsumer = new CaptureConsumer<>();

        mWindowBackendUnderTest.unregisterLayoutChangeCallback(fakeConsumer);

        verify(mFakeBackend).unregisterLayoutChangeCallback(fakeConsumer);
    }

    private static class CaptureConsumer<T> implements Consumer<T> {
        private T mData = null;

        @Override
        public void accept(T deviceState) {
            mData = deviceState;
        }

        public T getCurrentState() {
            return mData;
        }
    }
}
