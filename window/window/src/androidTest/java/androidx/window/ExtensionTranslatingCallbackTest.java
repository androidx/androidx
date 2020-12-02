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

import static androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.graphics.Rect;

import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionDisplayFeature;
import androidx.window.extensions.ExtensionWindowLayoutInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

public class ExtensionTranslatingCallbackTest {

    private static final Rect WINDOW_BOUNDS = new Rect(0, 0, 50, 100);

    private TestWindowBoundsHelper mWindowBoundsHelper;

    @Before
    public void setUp() {
        mWindowBoundsHelper = new TestWindowBoundsHelper();
        mWindowBoundsHelper.setCurrentBounds(WINDOW_BOUNDS);
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper);
    }

    @After
    public void tearDown() {
        WindowBoundsHelper.setForTesting(null);
    }

    @Test
    public void testOnWindowLayoutChange_validFeature() {
        Activity mockActivity = mock(Activity.class);
        Rect bounds = new Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0);
        ExtensionDisplayFeature foldFeature = new ExtensionDisplayFeature(bounds,
                ExtensionDisplayFeature.TYPE_FOLD);

        List<ExtensionDisplayFeature> extensionDisplayFeatures = new ArrayList<>();
        extensionDisplayFeatures.add(foldFeature);
        ExtensionWindowLayoutInfo windowLayoutInfo =
                new ExtensionWindowLayoutInfo(extensionDisplayFeatures);

        List<DisplayFeature> expectedFeatures = new ArrayList<>();
        expectedFeatures.add(new DisplayFeature(foldFeature.getBounds(), DisplayFeature.TYPE_FOLD));
        WindowLayoutInfo expected = new WindowLayoutInfo(expectedFeatures);

        ExtensionCallbackInterface mockCallback = mock(ExtensionCallbackInterface.class);
        ExtensionTranslatingCallback extensionTranslatingCallback =
                new ExtensionTranslatingCallback(mockCallback, new ExtensionAdapter());

        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo);

        ArgumentCaptor<WindowLayoutInfo> captor = ArgumentCaptor.forClass(WindowLayoutInfo.class);
        verify(mockCallback).onWindowLayoutChanged(eq(mockActivity), captor.capture());
        assertEquals(expected, captor.getValue());
    }

    @Test
    public void testOnWindowLayoutChange_filterRemovesEmptyBoundsFeature() {
        List<ExtensionDisplayFeature> extensionDisplayFeatures = new ArrayList<>();
        extensionDisplayFeatures.add(
                new ExtensionDisplayFeature(new Rect(), ExtensionDisplayFeature.TYPE_FOLD));

        ExtensionCallbackInterface mockCallback = mock(ExtensionCallbackInterface.class);
        ExtensionTranslatingCallback extensionTranslatingCallback =
                new ExtensionTranslatingCallback(mockCallback, new ExtensionAdapter());
        ExtensionWindowLayoutInfo windowLayoutInfo =
                new ExtensionWindowLayoutInfo(extensionDisplayFeatures);
        Activity mockActivity = mock(Activity.class);

        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo);

        verify(mockCallback).onWindowLayoutChanged(eq(mockActivity),
                argThat((layoutInfo) -> layoutInfo.getDisplayFeatures().isEmpty()));
    }


    @Test
    public void testOnWindowLayoutChange_filterRemovesNonEmptyAreaFoldFeature() {
        List<ExtensionDisplayFeature> extensionDisplayFeatures = new ArrayList<>();
        Rect fullWidthBounds = new Rect(0, 1, WINDOW_BOUNDS.width(), 2);
        Rect fullHeightBounds = new Rect(1, 0, 2, WINDOW_BOUNDS.height());
        extensionDisplayFeatures.add(new ExtensionDisplayFeature(fullWidthBounds,
                ExtensionDisplayFeature.TYPE_FOLD));
        extensionDisplayFeatures.add(new ExtensionDisplayFeature(fullHeightBounds,
                ExtensionDisplayFeature.TYPE_FOLD));

        ExtensionCallbackInterface mockCallback = mock(ExtensionCallbackInterface.class);
        ExtensionTranslatingCallback extensionTranslatingCallback =
                new ExtensionTranslatingCallback(mockCallback, new ExtensionAdapter());
        ExtensionWindowLayoutInfo windowLayoutInfo =
                new ExtensionWindowLayoutInfo(extensionDisplayFeatures);
        Activity mockActivity = mock(Activity.class);

        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo);

        verify(mockCallback).onWindowLayoutChanged(eq(mockActivity),
                argThat((layoutInfo) -> layoutInfo.getDisplayFeatures().isEmpty()));
    }

    @Test
    public void testOnWindowLayoutChange_filterRemovesHingeFeatureNotSpanningFullDimension() {
        List<ExtensionDisplayFeature> extensionDisplayFeatures = new ArrayList<>();
        Rect fullWidthBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top,
                WINDOW_BOUNDS.right / 2, 2);
        Rect fullHeightBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, 2,
                WINDOW_BOUNDS.bottom / 2);
        extensionDisplayFeatures.add(new ExtensionDisplayFeature(fullWidthBounds,
                ExtensionDisplayFeature.TYPE_HINGE));
        extensionDisplayFeatures.add(new ExtensionDisplayFeature(fullHeightBounds,
                ExtensionDisplayFeature.TYPE_HINGE));

        ExtensionCallbackInterface mockCallback = mock(ExtensionCallbackInterface.class);
        ExtensionTranslatingCallback extensionTranslatingCallback =
                new ExtensionTranslatingCallback(mockCallback, new ExtensionAdapter());
        ExtensionWindowLayoutInfo windowLayoutInfo =
                new ExtensionWindowLayoutInfo(extensionDisplayFeatures);

        Activity mockActivity = mock(Activity.class);

        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo);

        verify(mockCallback).onWindowLayoutChanged(eq(mockActivity),
                argThat((layoutInfo) -> layoutInfo.getDisplayFeatures().isEmpty()));
    }

    @Test
    public void testOnWindowLayoutChange_filterRemovesFoldFeatureNotSpanningFullDimension() {
        List<ExtensionDisplayFeature> extensionDisplayFeatures = new ArrayList<>();
        Rect fullWidthBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top,
                WINDOW_BOUNDS.right / 2, WINDOW_BOUNDS.top);
        Rect fullHeightBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, WINDOW_BOUNDS.left,
                WINDOW_BOUNDS.bottom / 2);
        extensionDisplayFeatures.add(new ExtensionDisplayFeature(fullWidthBounds,
                ExtensionDisplayFeature.TYPE_HINGE));
        extensionDisplayFeatures.add(new ExtensionDisplayFeature(fullHeightBounds,
                ExtensionDisplayFeature.TYPE_HINGE));

        ExtensionCallbackInterface mockCallback = mock(ExtensionCallbackInterface.class);
        ExtensionTranslatingCallback extensionTranslatingCallback =
                new ExtensionTranslatingCallback(mockCallback, new ExtensionAdapter());
        ExtensionWindowLayoutInfo windowLayoutInfo =
                new ExtensionWindowLayoutInfo(extensionDisplayFeatures);

        Activity mockActivity = mock(Activity.class);

        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo);

        verify(mockCallback).onWindowLayoutChanged(eq(mockActivity),
                argThat((layoutInfo) -> layoutInfo.getDisplayFeatures().isEmpty()));
    }

    @Test
    public void testOnDeviceStateChange_translateStates() {
        ExtensionCallbackInterface mockCallback = mock(ExtensionCallbackInterface.class);
        ExtensionTranslatingCallback extensionTranslatingCallback =
                new ExtensionTranslatingCallback(mockCallback, new ExtensionAdapter());

        extensionTranslatingCallback.onDeviceStateChanged(new ExtensionDeviceState(
                ExtensionDeviceState.POSTURE_UNKNOWN));
        extensionTranslatingCallback.onDeviceStateChanged(new ExtensionDeviceState(
                ExtensionDeviceState.POSTURE_CLOSED));
        extensionTranslatingCallback.onDeviceStateChanged(new ExtensionDeviceState(
                ExtensionDeviceState.POSTURE_HALF_OPENED));
        extensionTranslatingCallback.onDeviceStateChanged(new ExtensionDeviceState(
                ExtensionDeviceState.POSTURE_OPENED));
        extensionTranslatingCallback.onDeviceStateChanged(new ExtensionDeviceState(
                ExtensionDeviceState.POSTURE_FLIPPED));

        ArgumentCaptor<DeviceState> captor = ArgumentCaptor.forClass(DeviceState.class);
        verify(mockCallback, atLeastOnce()).onDeviceStateChanged(captor.capture());

        List<DeviceState> values = captor.getAllValues();
        assertEquals(DeviceState.POSTURE_UNKNOWN, values.get(0).getPosture());
        assertEquals(DeviceState.POSTURE_CLOSED, values.get(1).getPosture());
        assertEquals(DeviceState.POSTURE_HALF_OPENED, values.get(2).getPosture());
        assertEquals(DeviceState.POSTURE_OPENED, values.get(3).getPosture());
        assertEquals(DeviceState.POSTURE_FLIPPED, values.get(4).getPosture());
        assertEquals(5, values.size());
    }
}
