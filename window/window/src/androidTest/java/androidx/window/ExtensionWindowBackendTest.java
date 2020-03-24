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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.IBinder;

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
import java.util.concurrent.Executor;

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

    @Test(expected = IllegalArgumentException.class)
    public void testGetWindowLayoutInfo_applicationContext() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        backend.getWindowLayoutInfo(mContext);
    }

    @Test
    public void testGetWindowLayoutInfo_activityContext_deviceExtension() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        assumeNotNull(backend.mWindowExtension);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        WindowLayoutInfo layoutInfo = backend.getWindowLayoutInfo(activity);
        assertNotNull(layoutInfo);
        assertNotNull(layoutInfo.getDisplayFeatures());
        IBinder windowToken = getActivityWindowToken(activity);
    }

    @Test
    public void testGetWindowLayoutInfo_activityContext_noExtension() {
        // Verify method with extension
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        assumeTrue(backend.mWindowExtension == null);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        WindowLayoutInfo layoutInfo = backend.getWindowLayoutInfo(activity);
        assertNotNull(layoutInfo);
        assertNotNull(layoutInfo.getDisplayFeatures());
        IBinder windowToken = getActivityWindowToken(activity);
        verify(backend.mWindowExtension).getWindowLayoutInfo(eq(windowToken));
        WindowLayoutInfo initialLastReportedState =
                backend.mLastReportedWindowLayouts.get(windowToken);

        // Verify method without extension
        backend.mWindowExtension = null;
        layoutInfo = backend.getWindowLayoutInfo(activity);
        assertNotNull(layoutInfo);
        assertNotNull(layoutInfo.getDisplayFeatures());
        assertTrue(layoutInfo.getDisplayFeatures().isEmpty());

        // Verify that last reported state does not change when using the getter
        assertEquals(initialLastReportedState, backend.mLastReportedWindowLayouts.get(windowToken));
    }

    @Test
    public void testGetDeviceState_deviceExtension() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        assumeNotNull(backend.mWindowExtension);

        DeviceState deviceState = backend.getDeviceState();
        assertNotNull(deviceState);
        assertThat(deviceState.getPosture()).isIn(Range.range(
                DeviceState.POSTURE_UNKNOWN, BoundType.CLOSED,
                DeviceState.POSTURE_MAX_KNOWN, BoundType.CLOSED));
    }

    @Test
    public void testGetDeviceState_noExtension() {
        // Verify method with extension
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        assumeTrue(backend.mWindowExtension == null);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        DeviceState deviceState = backend.getDeviceState();
        assertNotNull(deviceState);
        assertThat(deviceState.getPosture()).isIn(Range.range(
                DeviceState.POSTURE_UNKNOWN, BoundType.CLOSED,
                DeviceState.POSTURE_MAX_KNOWN, BoundType.CLOSED));
        verify(backend.mWindowExtension).getDeviceState();
        DeviceState initialLastReportedState = backend.mLastReportedDeviceState;

        // Verify method without extension
        backend.mWindowExtension = null;
        deviceState = backend.getDeviceState();
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
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerAdded(
                eq(getActivityWindowToken(activity)));

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer);

        assertTrue(backend.mWindowLayoutChangeCallbacks.isEmpty());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(
                eq(getActivityWindowToken(activity)));
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterLayoutChangeCallback_activityNoWindow() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        backend.registerLayoutChangeCallback(mock(Activity.class), mock(Executor.class),
                mock(Consumer.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterLayoutChangeCallback_applicationContext() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        backend.registerLayoutChangeCallback(mContext, mock(Executor.class),
                mock(Consumer.class));
    }

    @Test
    public void testLayoutChangeCallback() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check that callbacks from the extension are propagated correctly
        Consumer<WindowLayoutInfo> consumer = mock(Consumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        IBinder windowToken = getActivityWindowToken(activity);

        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);
        WindowLayoutInfo windowLayoutInfo = newTestWindowLayoutInfo();

        ExtensionWindowBackend.ExtensionListenerImpl backendListener =
                backend.new ExtensionListenerImpl();
        backendListener.onWindowLayoutChanged(windowToken, windowLayoutInfo);

        verify(consumer).accept(eq(windowLayoutInfo));
        assertEquals(windowLayoutInfo, backend.mLastReportedWindowLayouts.get(windowToken));

        // Test that the same value wouldn't be reported again
        reset(consumer);
        backendListener.onWindowLayoutChanged(windowToken, windowLayoutInfo);
        verify(consumer, never()).accept(any());
    }

    @Test
    public void testRegisterDeviceChangeCallback() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

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

    private static WindowLayoutInfo newTestWindowLayoutInfo() {
        WindowLayoutInfo.Builder builder = new WindowLayoutInfo.Builder();
        WindowLayoutInfo windowLayoutInfo = builder.build();

        assertTrue(windowLayoutInfo.getDisplayFeatures().isEmpty());

        DisplayFeature.Builder featureBuilder = new DisplayFeature.Builder();
        featureBuilder.setType(DisplayFeature.TYPE_HINGE);
        featureBuilder.setBounds(new Rect(1, 2, 3, 4));
        DisplayFeature feature1 = featureBuilder.build();

        featureBuilder = new DisplayFeature.Builder();
        featureBuilder.setBounds(new Rect(5, 6, 7, 8));
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
}
