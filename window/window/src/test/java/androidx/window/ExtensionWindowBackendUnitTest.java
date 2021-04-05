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
import static org.junit.Assert.assertNotNull;
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
