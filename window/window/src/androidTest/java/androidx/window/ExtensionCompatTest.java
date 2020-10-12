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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionDisplayFeature;
import androidx.window.extensions.ExtensionInterface;
import androidx.window.extensions.ExtensionWindowLayoutInfo;

import org.junit.After;
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
    private static final Rect WINDOW_BOUNDS = new Rect(1, 1, 50, 100);

    private ExtensionInterface mMockExtensionInterface;
    private Activity mActivity;

    @Before
    public void setUp() {
        mMockExtensionInterface = mock(ExtensionInterface.class);
        mExtensionCompat = new ExtensionCompat(mMockExtensionInterface);
        mActivity = mock(Activity.class);

        TestWindowBoundsHelper mWindowBoundsHelper = new TestWindowBoundsHelper();
        mWindowBoundsHelper.setCurrentBounds(WINDOW_BOUNDS);
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper);

        // Setup mocked extension responses
        ExtensionDeviceState defaultDeviceState =
                new ExtensionDeviceState(ExtensionDeviceState.POSTURE_HALF_OPENED);
        when(mExtensionCompat.mWindowExtension.getDeviceState()).thenReturn(defaultDeviceState);

        Rect bounds = new Rect(0, 1, WINDOW_BOUNDS.width(), 1);
        ExtensionDisplayFeature extensionDisplayFeatureDisplayFeature =
                new ExtensionDisplayFeature(bounds, ExtensionDisplayFeature.TYPE_HINGE);
        List<ExtensionDisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(extensionDisplayFeatureDisplayFeature);
        ExtensionWindowLayoutInfo extensionWindowLayoutInfo =
                new ExtensionWindowLayoutInfo(displayFeatures);
        when(mExtensionCompat.mWindowExtension.getWindowLayoutInfo(any()))
                .thenReturn(extensionWindowLayoutInfo);
    }

    @After
    public void tearDown() {
        WindowBoundsHelper.setForTesting(null);
    }

    @Test
    public void testGetWindowLayout_featureWithEmptyBounds() {
        // Add a feature with an empty bounds to the reported list
        List<ExtensionDisplayFeature> features = new ArrayList<>();
        ExtensionDisplayFeature emptyRectFeature = new ExtensionDisplayFeature(new Rect(),
                ExtensionDisplayFeature.TYPE_HINGE);
        features.add(emptyRectFeature);
        ExtensionWindowLayoutInfo infoWithEmptyRect = new ExtensionWindowLayoutInfo(features);
        when(mMockExtensionInterface.getWindowLayoutInfo(any()))
                .thenReturn(infoWithEmptyRect);

        // Verify that this feature is skipped.
        WindowLayoutInfo windowLayoutInfo = mExtensionCompat.getWindowLayoutInfo(mActivity);

        assertEquals(features.size() - 1,
                windowLayoutInfo.getDisplayFeatures().size());
    }

    @Test
    public void testGetWindowLayout_foldWithNonZeroArea() {
        List<ExtensionDisplayFeature> features = new ArrayList<>();
        ExtensionWindowLayoutInfo originalWindowLayoutInfo =
                mExtensionCompat.mWindowExtension.getWindowLayoutInfo(mActivity);
        List<ExtensionDisplayFeature> extensionDisplayFeatures =
                originalWindowLayoutInfo.getDisplayFeatures();
        // Original features.
        features.addAll(extensionDisplayFeatures);
        // Horizontal fold.
        features.add(
                new ExtensionDisplayFeature(new Rect(0, 1, WINDOW_BOUNDS.width(), 2),
                        ExtensionDisplayFeature.TYPE_FOLD));
        // Vertical fold.
        features.add(
                new ExtensionDisplayFeature(new Rect(1, 0, 2, WINDOW_BOUNDS.height()),
                        ExtensionDisplayFeature.TYPE_FOLD));

        when(mMockExtensionInterface.getWindowLayoutInfo(any()))
                .thenReturn(new ExtensionWindowLayoutInfo(features));

        // Verify that these features are skipped.
        WindowLayoutInfo windowLayoutInfo =
                mExtensionCompat.getWindowLayoutInfo(mActivity);

        assertEquals(features.size() - 2,
                windowLayoutInfo.getDisplayFeatures().size());
    }

    @Test
    public void testGetWindowLayout_hingeNotSpanningEntireWindow() {
        List<ExtensionDisplayFeature> features = new ArrayList<>();
        ExtensionWindowLayoutInfo originalWindowLayoutInfo =
                mExtensionCompat.mWindowExtension.getWindowLayoutInfo(mActivity);
        List<ExtensionDisplayFeature> extensionDisplayFeatures =
                originalWindowLayoutInfo.getDisplayFeatures();
        // Original features.
        features.addAll(extensionDisplayFeatures);
        // Horizontal hinge.
        features.add(
                new ExtensionDisplayFeature(new Rect(0, 1, WINDOW_BOUNDS.width() - 1, 2),
                        ExtensionDisplayFeature.TYPE_HINGE));
        // Vertical hinge.
        features.add(
                new ExtensionDisplayFeature(new Rect(1, 0, 2, WINDOW_BOUNDS.height() - 1),
                        ExtensionDisplayFeature.TYPE_HINGE));

        when(mMockExtensionInterface.getWindowLayoutInfo(any()))
                .thenReturn(new ExtensionWindowLayoutInfo(features));

        // Verify that these features are skipped.
        WindowLayoutInfo windowLayoutInfo =
                mExtensionCompat.getWindowLayoutInfo(mActivity);

        assertEquals(features.size() - 2,
                windowLayoutInfo.getDisplayFeatures().size());
    }

    @Test
    public void testGetWindowLayout_foldNotSpanningEntireWindow() {
        List<ExtensionDisplayFeature> features = new ArrayList<>();
        ExtensionWindowLayoutInfo originalWindowLayoutInfo =
                mExtensionCompat.mWindowExtension.getWindowLayoutInfo(mActivity);
        List<ExtensionDisplayFeature> extensionDisplayFeatures =
                originalWindowLayoutInfo.getDisplayFeatures();
        // Original features.
        features.addAll(extensionDisplayFeatures);
        // Horizontal fold.
        features.add(
                new ExtensionDisplayFeature(new Rect(0, 1, WINDOW_BOUNDS.width() - 1, 2),
                        ExtensionDisplayFeature.TYPE_FOLD));
        // Vertical fold.
        features.add(
                new ExtensionDisplayFeature(new Rect(1, 0, 2, WINDOW_BOUNDS.height() - 1),
                        ExtensionDisplayFeature.TYPE_FOLD));

        when(mMockExtensionInterface.getWindowLayoutInfo(any()))
                .thenReturn(new ExtensionWindowLayoutInfo(features));

        // Verify that these features are skipped.
        WindowLayoutInfo windowLayoutInfo =
                mExtensionCompat.getWindowLayoutInfo(mActivity);

        assertEquals(features.size() - 2,
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

        // Verify that the callback set for extension propagates the window layout callback when
        // a listener has been registered.
        mExtensionCompat.onWindowLayoutChangeListenerAdded(mActivity);
        Rect bounds = new Rect(0, 1, WINDOW_BOUNDS.width(), 1);
        ExtensionDisplayFeature extensionDisplayFeature =
                new ExtensionDisplayFeature(bounds, ExtensionDisplayFeature.TYPE_HINGE);
        List<ExtensionDisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(extensionDisplayFeature);
        ExtensionWindowLayoutInfo extensionWindowLayoutInfo =
                new ExtensionWindowLayoutInfo(displayFeatures);

        extensionCallbackCaptor.getValue().onWindowLayoutChanged(mActivity,
                extensionWindowLayoutInfo);
        ArgumentCaptor<WindowLayoutInfo> windowLayoutInfoCaptor =
                ArgumentCaptor.forClass(WindowLayoutInfo.class);
        verify(callback).onWindowLayoutChanged(eq(mActivity), windowLayoutInfoCaptor.capture());

        WindowLayoutInfo capturedLayout = windowLayoutInfoCaptor.getValue();
        assertEquals(1, capturedLayout.getDisplayFeatures().size());
        DisplayFeature capturedDisplayFeature = capturedLayout.getDisplayFeatures().get(0);
        assertEquals(DisplayFeature.TYPE_HINGE, capturedDisplayFeature.getType());
        assertEquals(bounds, capturedDisplayFeature.getBounds());
    }

    @Test
    @Override
    public void testOnWindowLayoutChangeListenerAdded() {
        mExtensionCompat.onWindowLayoutChangeListenerAdded(mActivity);
        verify(mExtensionCompat.mWindowExtension).onWindowLayoutChangeListenerAdded(eq(mActivity));
    }

    @Test
    @Override
    public void testOnWindowLayoutChangeListenerRemoved() {
        mExtensionCompat.onWindowLayoutChangeListenerRemoved(mActivity);
        verify(mExtensionCompat.mWindowExtension)
                .onWindowLayoutChangeListenerRemoved(eq(mActivity));
    }

    @Test
    @Override
    public void testOnDeviceStateListenersChanged() {
        mExtensionCompat.onDeviceStateListenersChanged(true);
        verify(mExtensionCompat.mWindowExtension).onDeviceStateListenersChanged(eq(true));
    }

    @Test
    public void testValidateExtensionInterface() {
        assertTrue(mExtensionCompat.validateExtensionInterface());
    }
}
