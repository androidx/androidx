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
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;

import androidx.core.util.Consumer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link ExtensionWindowBackend} class. */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ExtensionWindowBackendTest extends WindowTestBase {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
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
    public void testInitAndVerifyExtension() {
        Version extensionVersion = ExtensionCompat.getExtensionVersion();
        assumeTrue(extensionVersion != null);
        assertTrue(ExtensionWindowBackend.isExtensionVersionSupported(extensionVersion));

        ExtensionInterfaceCompat extension =
                ExtensionWindowBackend.initAndVerifyExtension(mContext);
        assertNotNull(extension);
        assertTrue(extension instanceof ExtensionCompat);
        assertTrue(extension.validateExtensionInterface());
    }

    @Test
    public void testInitAndVerifySidecar() {
        Version sidecarVersion = SidecarCompat.getSidecarVersion();
        assumeTrue(sidecarVersion != null);
        assertTrue(ExtensionWindowBackend.isExtensionVersionSupported(sidecarVersion));

        ExtensionInterfaceCompat sidecar =
                ExtensionWindowBackend.initAndVerifyExtension(mContext);
        assertNotNull(sidecar);
        assertTrue(sidecar instanceof SidecarCompat);
        assertTrue(sidecar.validateExtensionInterface());
    }

    @Test
    public void testRegisterDeviceStateChangeCallback_noExtension() {
        // Verify method with extension
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        assumeTrue(backend.mWindowExtension == null);
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
        Consumer<WindowLayoutInfo> consumer = mock(Consumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);

        assertEquals(1, backend.mWindowLayoutChangeCallbacks.size());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerAdded(activity);

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer);

        assertTrue(backend.mWindowLayoutChangeCallbacks.isEmpty());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(eq(activity));
    }

    @Test
    public void testRegisterLayoutChangeCallback_callsExtensionOnce() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check registering the layout change callback
        Consumer<WindowLayoutInfo> consumer = mock(Consumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);
        backend.registerLayoutChangeCallback(activity, Runnable::run, mock(Consumer.class));

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
        Consumer<WindowLayoutInfo> firstConsumer = mock(Consumer.class);
        Consumer<WindowLayoutInfo> secondConsumer = mock(Consumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        backend.registerLayoutChangeCallback(activity, Runnable::run, firstConsumer);
        backend.registerLayoutChangeCallback(activity, Runnable::run, secondConsumer);

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(firstConsumer);
        backend.unregisterLayoutChangeCallback(secondConsumer);

        assertTrue(backend.mWindowLayoutChangeCallbacks.isEmpty());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(activity);
    }

    @Test
    public void testRegisterLayoutChangeCallback_relayLastEmittedValue() {
        WindowLayoutInfo expectedWindowLayoutInfo = newTestWindowLayoutInfo();
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        Consumer<WindowLayoutInfo> consumer = mock(Consumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        backend.registerLayoutChangeCallback(activity, Runnable::run, mock(Consumer.class));
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);
        backend.mLastReportedWindowLayouts.put(activity, expectedWindowLayoutInfo);

        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);

        verify(consumer).accept(expectedWindowLayoutInfo);
    }

    @Test
    public void testLayoutChangeCallback_emitNewValue() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check that callbacks from the extension are propagated correctly
        Consumer<WindowLayoutInfo> consumer = mock(Consumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());

        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);
        WindowLayoutInfo windowLayoutInfo = newTestWindowLayoutInfo();

        ExtensionWindowBackend.ExtensionListenerImpl backendListener =
                backend.new ExtensionListenerImpl();
        backendListener.onWindowLayoutChanged(activity, windowLayoutInfo);

        verify(consumer).accept(eq(windowLayoutInfo));
        assertEquals(windowLayoutInfo, backend.mLastReportedWindowLayouts.get(activity));

        // Test that the same value wouldn't be reported again
        reset(consumer);
        backendListener.onWindowLayoutChanged(activity, windowLayoutInfo);
        verify(consumer, never()).accept(any());
    }

    @Test
    public void testRegisterDeviceChangeCallback() {
        DeviceState expectedState = newTestDeviceState();
        ExtensionInterfaceCompat mockInterface = mock(ExtensionInterfaceCompat.class);
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mockInterface;

        // Check registering the device state change callback
        Consumer<DeviceState> consumer = mock(Consumer.class);
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
        Consumer<DeviceState> consumer = mock(Consumer.class);

        backend.registerDeviceStateChangeCallback(Runnable::run, consumer);
        DeviceState deviceState = newTestDeviceState();
        ExtensionWindowBackend.ExtensionListenerImpl backendListener =
                backend.new ExtensionListenerImpl();
        backendListener.onDeviceStateChanged(deviceState);

        verify(consumer).accept(eq(deviceState));
        assertEquals(deviceState, backend.mLastReportedDeviceState);

        // Test that the same value wouldn't be reported again
        reset(consumer);
        backendListener.onDeviceStateChanged(deviceState);
        verify(consumer, never()).accept(any());
    }

    @Test
    public void testDeviceChangeChangeCallback_callsExtensionOnce() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check registering the layout change callback
        Consumer<DeviceState> consumer = mock(Consumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        backend.registerDeviceStateChangeCallback(Runnable::run, consumer);
        backend.registerDeviceStateChangeCallback(Runnable::run, mock(Consumer.class));

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
        Consumer<DeviceState> firstConsumer = mock(Consumer.class);
        Consumer<DeviceState> secondConsumer = mock(Consumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
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
        Consumer<DeviceState> consumer = mock(Consumer.class);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);
        backend.mLastReportedDeviceState = expectedState;

        backend.registerDeviceStateChangeCallback(Runnable::run, consumer);

        verify(consumer).accept(expectedState);
    }

    @Test
    public void testDeviceChangeCallback_clearLastEmittedValue() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        Consumer<DeviceState> consumer = mock(Consumer.class);

        backend.registerDeviceStateChangeCallback(Runnable::run, consumer);
        backend.unregisterDeviceStateChangeCallback(consumer);

        assertTrue(backend.mDeviceStateChangeCallbacks.isEmpty());
        assertNull(backend.mLastReportedDeviceState);
    }

    private static WindowLayoutInfo newTestWindowLayoutInfo() {
        WindowLayoutInfo.Builder builder = new WindowLayoutInfo.Builder();
        WindowLayoutInfo windowLayoutInfo = builder.build();

        assertTrue(windowLayoutInfo.getDisplayFeatures().isEmpty());

        DisplayFeature.Builder featureBuilder = new DisplayFeature.Builder();
        featureBuilder.setType(DisplayFeature.TYPE_HINGE);
        featureBuilder.setBounds(new Rect(0, 2, 3, 4));
        DisplayFeature feature1 = featureBuilder.build();

        featureBuilder = new DisplayFeature.Builder();
        featureBuilder.setBounds(new Rect(0, 1, 5, 1));
        DisplayFeature feature2 = featureBuilder.build();

        List<DisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(feature1);
        displayFeatures.add(feature2);

        builder = new WindowLayoutInfo.Builder();
        builder.setDisplayFeatures(displayFeatures);
        return builder.build();
    }

    private static DeviceState newTestDeviceState() {
        DeviceState.Builder builder = new DeviceState.Builder();
        builder.setPosture(DeviceState.POSTURE_OPENED);
        return builder.build();
    }

    private static class SimpleConsumer<T> implements Consumer<T> {
        private List<T> mValues;

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
}
