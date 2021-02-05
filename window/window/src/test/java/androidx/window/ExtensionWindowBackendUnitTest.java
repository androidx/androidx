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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link ExtensionWindowBackend} that run on the JVM.
 */
@SuppressWarnings("deprecation") // TODO(b/173739071) Remove DeviceState
public class ExtensionWindowBackendUnitTest {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = mock(Context.class);
        ExtensionWindowBackend.resetInstance();
    }

    @Test
    public void testGetInstance() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        assertNotNull(backend);

        // Verify that getInstance always returns the same value
        ExtensionWindowBackend newBackend = ExtensionWindowBackend.getInstance(mContext);
        assertEquals(backend, newBackend);
    }

    @Test
    public void testRegisterDeviceStateChangeCallback_noExtension() {
        // Verify method with extension
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = null;
        SimpleConsumer<DeviceState> simpleConsumer = new SimpleConsumer<>();

        backend.registerDeviceStateChangeCallback(directExecutor(), simpleConsumer);

        DeviceState deviceState = simpleConsumer.lastValue();
        assertNotNull(deviceState);
        assertThat(deviceState.getPosture()).isIn(Range.range(
                DeviceState.POSTURE_UNKNOWN, BoundType.CLOSED,
                DeviceState.POSTURE_MAX_KNOWN, BoundType.CLOSED));
        DeviceState initialLastReportedState = backend.mLastReportedDeviceState;

        // Verify method without extension
        backend.mWindowExtension = null;
        SimpleConsumer<DeviceState> noExtensionConsumer = new SimpleConsumer<>();
        backend.registerDeviceStateChangeCallback(directExecutor(), noExtensionConsumer);
        deviceState = noExtensionConsumer.lastValue();
        assertNotNull(deviceState);
        assertEquals(DeviceState.POSTURE_UNKNOWN, deviceState.getPosture());
        // Verify that last reported state does not change when using the getter
        assertEquals(initialLastReportedState, backend.mLastReportedDeviceState);
    }

    @Test
    public void testRegisterLayoutChangeCallback() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check registering the layout change callback
        Consumer<WindowLayoutInfo> consumer = mock(WindowLayoutInfoConsumer.class);
        Activity activity = mock(Activity.class);
        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);

        assertEquals(1, backend.mWindowLayoutChangeCallbacks.size());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerAdded(activity);

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer);

        assertTrue(backend.mWindowLayoutChangeCallbacks.isEmpty());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(eq(activity));
    }

    @Test
    public void testRegisterLayoutChangeCallback_noExtension() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = null;

        // Check registering the layout change callback
        Consumer<WindowLayoutInfo> consumer = mock(WindowLayoutInfoConsumer.class);
        Activity activity = mock(Activity.class);
        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);

        verify(consumer).accept(any());
    }

    @Test
    public void testRegisterLayoutChangeCallback_synchronousExtension() {
        WindowLayoutInfo expectedInfo = newTestWindowLayoutInfo();
        ExtensionInterfaceCompat extensionInterfaceCompat =
                new SynchronousExtensionInterface(expectedInfo,
                newTestDeviceState());
        ExtensionWindowBackend backend = new ExtensionWindowBackend(extensionInterfaceCompat);

        // Check registering the layout change callback
        Consumer<WindowLayoutInfo> consumer = mock(WindowLayoutInfoConsumer.class);
        Activity activity = mock(Activity.class);
        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);

        // Check unregistering the layout change callback
        verify(consumer).accept(expectedInfo);
    }

    @Test
    public void testRegisterLayoutChangeCallback_callsExtensionOnce() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check registering the layout change callback
        Consumer<WindowLayoutInfo> consumer = mock(WindowLayoutInfoConsumer.class);
        Activity activity = mock(Activity.class);
        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);
        backend.registerLayoutChangeCallback(activity, Runnable::run,
                mock(WindowLayoutInfoConsumer.class));

        assertEquals(2, backend.mWindowLayoutChangeCallbacks.size());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerAdded(activity);

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer);

        assertEquals(1, backend.mWindowLayoutChangeCallbacks.size());
        verify(backend.mWindowExtension, times(0))
                .onWindowLayoutChangeListenerRemoved(eq(activity));
    }

    @Test
    public void testRegisterLayoutChangeCallback_clearListeners() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check registering the layout change callback
        Consumer<WindowLayoutInfo> firstConsumer = mock(WindowLayoutInfoConsumer.class);
        Consumer<WindowLayoutInfo> secondConsumer = mock(WindowLayoutInfoConsumer.class);
        Activity activity = mock(Activity.class);
        backend.registerLayoutChangeCallback(activity, Runnable::run, firstConsumer);
        backend.registerLayoutChangeCallback(activity, Runnable::run, secondConsumer);

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(firstConsumer);
        backend.unregisterLayoutChangeCallback(secondConsumer);

        assertTrue(backend.mWindowLayoutChangeCallbacks.isEmpty());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(activity);
    }

    @Test
    public void testRegisterDeviceChangeCallback() {
        ExtensionInterfaceCompat mockInterface = mock(ExtensionInterfaceCompat.class);
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mockInterface;

        // Check registering the device state change callback
        Consumer<DeviceState> consumer = mock(DeviceStateConsumer.class);
        backend.registerDeviceStateChangeCallback(Runnable::run, consumer);

        assertEquals(1, backend.mDeviceStateChangeCallbacks.size());
        verify(backend.mWindowExtension).onDeviceStateListenersChanged(eq(false));

        // Check unregistering the device state change callback
        backend.unregisterDeviceStateChangeCallback(consumer);

        assertTrue(backend.mDeviceStateChangeCallbacks.isEmpty());
        verify(backend.mWindowExtension).onDeviceStateListenersChanged(eq(true));
    }

    @Test
    public void testDeviceChangeCallback() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check that callbacks from the extension are propagated correctly
        Consumer<DeviceState> consumer = mock(DeviceStateConsumer.class);

        backend.registerDeviceStateChangeCallback(Runnable::run, consumer);
        DeviceState deviceState = newTestDeviceState();
        ExtensionWindowBackend.ExtensionListenerImpl backendListener =
                backend.new ExtensionListenerImpl();
        backendListener.onDeviceStateChanged(deviceState);

        verify(consumer, times(1)).accept(eq(deviceState));
        assertEquals(deviceState, backend.mLastReportedDeviceState);

        // Test that the same value wouldn't be reported again
        backendListener.onDeviceStateChanged(deviceState);
        verify(consumer, times(1)).accept(any());
    }

    @Test
    public void testDeviceChangeChangeCallback_callsExtensionOnce() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check registering the layout change callback
        Consumer<DeviceState> consumer = mock(DeviceStateConsumer.class);
        backend.registerDeviceStateChangeCallback(Runnable::run, consumer);
        backend.registerDeviceStateChangeCallback(Runnable::run, mock(DeviceStateConsumer.class));

        assertEquals(2, backend.mDeviceStateChangeCallbacks.size());
        verify(backend.mWindowExtension).onDeviceStateListenersChanged(false);

        // Check unregistering the layout change callback
        backend.unregisterDeviceStateChangeCallback(consumer);

        assertEquals(1, backend.mDeviceStateChangeCallbacks.size());
        verify(backend.mWindowExtension, times(0))
                .onDeviceStateListenersChanged(true);
    }

    @Test
    public void testDeviceChangeChangeCallback_clearListeners() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check registering the layout change callback
        Consumer<DeviceState> firstConsumer = mock(DeviceStateConsumer.class);
        Consumer<DeviceState> secondConsumer = mock(DeviceStateConsumer.class);
        backend.registerDeviceStateChangeCallback(Runnable::run, firstConsumer);
        backend.registerDeviceStateChangeCallback(Runnable::run, secondConsumer);

        // Check unregistering the layout change callback
        backend.unregisterDeviceStateChangeCallback(firstConsumer);
        backend.unregisterDeviceStateChangeCallback(secondConsumer);

        assertTrue(backend.mDeviceStateChangeCallbacks.isEmpty());
        verify(backend.mWindowExtension).onDeviceStateListenersChanged(true);
    }

    @Test
    public void testDeviceChangeCallback_relayLastEmittedValue() {
        DeviceState expectedState = newTestDeviceState();
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        Consumer<DeviceState> consumer = mock(DeviceStateConsumer.class);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);
        backend.mLastReportedDeviceState = expectedState;

        backend.registerDeviceStateChangeCallback(Runnable::run, consumer);

        verify(consumer).accept(expectedState);
    }

    @Test
    public void testDeviceChangeCallback_clearLastEmittedValue() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        Consumer<DeviceState> consumer = mock(DeviceStateConsumer.class);

        backend.registerDeviceStateChangeCallback(Runnable::run, consumer);
        backend.unregisterDeviceStateChangeCallback(consumer);

        assertTrue(backend.mDeviceStateChangeCallbacks.isEmpty());
        assertNull(backend.mLastReportedDeviceState);
    }

    private static WindowLayoutInfo newTestWindowLayoutInfo() {
        WindowLayoutInfo.Builder builder = new WindowLayoutInfo.Builder();
        return builder.build();
    }

    private static DeviceState newTestDeviceState() {
        DeviceState.Builder builder = new DeviceState.Builder();
        builder.setPosture(DeviceState.POSTURE_OPENED);
        return builder.build();
    }

    private interface DeviceStateConsumer extends Consumer<DeviceState> { }

    private interface WindowLayoutInfoConsumer extends Consumer<WindowLayoutInfo> { }

    private static class SimpleConsumer<T> implements Consumer<T> {
        private final List<T> mValues;

        SimpleConsumer() {
            mValues = new ArrayList<>();
        }

        @Override
        public void accept(T t) {
            mValues.add(t);
        }

        T lastValue() {
            return mValues.get(mValues.size() - 1);
        }
    }

    private static class SynchronousExtensionInterface implements ExtensionInterfaceCompat {

        private ExtensionCallbackInterface mInterface;
        private final DeviceState mDeviceState;
        private final WindowLayoutInfo mWindowLayoutInfo;

        SynchronousExtensionInterface(WindowLayoutInfo windowLayoutInfo, DeviceState deviceState) {
            mInterface = new ExtensionCallbackInterface() {
                @Override
                public void onDeviceStateChanged(@NonNull DeviceState newDeviceState) {

                }

                @Override
                public void onWindowLayoutChanged(@NonNull Activity activity,
                        @NonNull WindowLayoutInfo newLayout) {

                }
            };
            mWindowLayoutInfo = windowLayoutInfo;
            mDeviceState = deviceState;
        }

        @Override
        public boolean validateExtensionInterface() {
            return true;
        }

        @Override
        public void setExtensionCallback(
                @NonNull ExtensionCallbackInterface extensionCallback) {
            mInterface = extensionCallback;
        }

        @Override
        public void onWindowLayoutChangeListenerAdded(@NonNull Activity activity) {
            mInterface.onWindowLayoutChanged(activity, mWindowLayoutInfo);
        }

        @Override
        public void onWindowLayoutChangeListenerRemoved(@NonNull Activity activity) {

        }

        @Override
        public void onDeviceStateListenersChanged(boolean isEmpty) {
            mInterface.onDeviceStateChanged(mDeviceState);
        }
    }
}
