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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Rect;
import android.os.IBinder;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionDisplayFeature;
import androidx.window.extensions.ExtensionInterface;
import androidx.window.extensions.ExtensionWindowLayoutInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link ExtensionCompat} implementation of {@link ExtensionInterfaceCompat}. This
 * class uses a mocked Extension to verify the behavior of the implementation on any hardware.
 * <p>Because this class extends {@link ExtensionCompatDeviceTest}, it will also run the mocked
 * versions of methods defined in {@link CompatDeviceTestInterface}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ExtensionCompatTest extends ExtensionCompatDeviceTest
        implements CompatTestInterface {

    @Before
    public void setUp() {
        mExtensionCompat = new ExtensionCompat(mock(ExtensionInterface.class));

        // Setup mocked extension responses
        ExtensionDeviceState defaultDeviceState =
                new ExtensionDeviceState(ExtensionDeviceState.POSTURE_HALF_OPENED);
        when(mExtensionCompat.mWindowExtension.getDeviceState()).thenReturn(defaultDeviceState);

        Rect bounds = new Rect(1, 2, 3, 4);
        ExtensionDisplayFeature extensionDisplayFeatureDisplayFeature =
                new ExtensionDisplayFeature(bounds, ExtensionDisplayFeature.TYPE_HINGE);
        List<ExtensionDisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(extensionDisplayFeatureDisplayFeature);
        ExtensionWindowLayoutInfo extensionWindowLayoutInfo =
                new ExtensionWindowLayoutInfo(displayFeatures);
        when(mExtensionCompat.mWindowExtension.getWindowLayoutInfo(any()))
                .thenReturn(extensionWindowLayoutInfo);
    }

    @Test
    public void testGetWindowLayout_featureWithEmptyBounds() {
        // Add a feature with an empty bounds to the reported list
        ExtensionWindowLayoutInfo originalWindowLayoutInfo =
                mExtensionCompat.mWindowExtension.getWindowLayoutInfo(mock(IBinder.class));
        List<ExtensionDisplayFeature> extensionDisplayFeatures =
                originalWindowLayoutInfo.getDisplayFeatures();
        extensionDisplayFeatures.add(
                new ExtensionDisplayFeature(new Rect(), ExtensionDisplayFeature.TYPE_HINGE));

        // Verify that this feature is skipped.
        WindowLayoutInfo windowLayoutInfo =
                mExtensionCompat.getWindowLayoutInfo(mock(IBinder.class));

        assertEquals(extensionDisplayFeatures.size() - 1,
                windowLayoutInfo.getDisplayFeatures().size());
    }

    @Test
    @Override
    public void testSetExtensionCallback() {
        ArgumentCaptor<ExtensionInterface.ExtensionCallback> extensionCallbackCaptor =
                ArgumentCaptor.forClass(ExtensionInterface.ExtensionCallback.class);

        // Verify that the extension got the callback set
        ExtensionInterfaceCompat.ExtensionCallbackInterface callback =
                mock(ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        mExtensionCompat.setExtensionCallback(callback);

        verify(mExtensionCompat.mWindowExtension).setExtensionCallback(
                extensionCallbackCaptor.capture());

        // Verify that the callback set for extension propagates the device state callback
        ExtensionDeviceState extensionDeviceState = new ExtensionDeviceState(
                ExtensionDeviceState.POSTURE_HALF_OPENED);

        extensionCallbackCaptor.getValue().onDeviceStateChanged(extensionDeviceState);
        ArgumentCaptor<DeviceState> deviceStateCaptor = ArgumentCaptor.forClass(DeviceState.class);
        verify(callback).onDeviceStateChanged(deviceStateCaptor.capture());
        assertEquals(DeviceState.POSTURE_HALF_OPENED, deviceStateCaptor.getValue().getPosture());

        // Verify that the callback set for extension propagates the window layout callback
        Rect bounds = new Rect(1, 2, 3, 4);
        ExtensionDisplayFeature extensionDisplayFeature =
                new ExtensionDisplayFeature(bounds, ExtensionDisplayFeature.TYPE_HINGE);
        List<ExtensionDisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(extensionDisplayFeature);
        ExtensionWindowLayoutInfo extensionWindowLayoutInfo =
                new ExtensionWindowLayoutInfo(displayFeatures);
        IBinder windowToken = mock(IBinder.class);

        extensionCallbackCaptor.getValue().onWindowLayoutChanged(windowToken,
                extensionWindowLayoutInfo);
        ArgumentCaptor<WindowLayoutInfo> windowLayoutInfoCaptor =
                ArgumentCaptor.forClass(WindowLayoutInfo.class);
        verify(callback).onWindowLayoutChanged(eq(windowToken), windowLayoutInfoCaptor.capture());

        WindowLayoutInfo capturedLayout = windowLayoutInfoCaptor.getValue();
        assertEquals(1, capturedLayout.getDisplayFeatures().size());
        DisplayFeature capturedDisplayFeature = capturedLayout.getDisplayFeatures().get(0);
        assertEquals(DisplayFeature.TYPE_HINGE, capturedDisplayFeature.getType());
        assertEquals(bounds, capturedDisplayFeature.getBounds());
    }

    @Test
    @Override
    public void testOnWindowLayoutChangeListenerAdded() {
        IBinder windowToken = mock(IBinder.class);
        mExtensionCompat.onWindowLayoutChangeListenerAdded(windowToken);
        verify(mExtensionCompat.mWindowExtension)
                .onWindowLayoutChangeListenerAdded(eq(windowToken));
    }

    @Test
    @Override
    public void testOnWindowLayoutChangeListenerRemoved() {
        IBinder windowToken = mock(IBinder.class);
        mExtensionCompat.onWindowLayoutChangeListenerRemoved(windowToken);
        verify(mExtensionCompat.mWindowExtension)
                .onWindowLayoutChangeListenerRemoved(eq(windowToken));
    }

    @Test
    @Override
    public void testOnDeviceStateListenersChanged() {
        mExtensionCompat.onDeviceStateListenersChanged(true);
        verify(mExtensionCompat.mWindowExtension).onDeviceStateListenersChanged(eq(true));
    }
}
