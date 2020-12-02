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
import static org.mockito.Mockito.mock;

import android.app.Activity;
import android.graphics.Rect;

import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SidecarAdapterTest implements TranslatorTestInterface {

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

    private static SidecarDisplayFeature sidecarDisplayFeature(Rect bounds, int type) {
        SidecarDisplayFeature feature = new SidecarDisplayFeature();
        feature.setRect(bounds);
        feature.setType(type);
        return feature;
    }

    private static SidecarWindowLayoutInfo sidecarWindowLayoutInfo(
            List<SidecarDisplayFeature> features) {
        SidecarWindowLayoutInfo layoutInfo = new SidecarWindowLayoutInfo();
        layoutInfo.displayFeatures = new ArrayList<>();
        layoutInfo.displayFeatures.addAll(features);
        return layoutInfo;
    }

    private static SidecarDeviceState sidecarDeviceState(int posture) {
        SidecarDeviceState deviceState = new SidecarDeviceState();
        deviceState.posture = posture;
        return deviceState;
    }

    @Test
    @Override
    public void testTranslate_validFeature() {
        Activity mockActivity = mock(Activity.class);
        Rect bounds = new Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0);
        SidecarDisplayFeature foldFeature = sidecarDisplayFeature(bounds,
                SidecarDisplayFeature.TYPE_FOLD);

        List<SidecarDisplayFeature> sidecarDisplayFeatures = new ArrayList<>();
        sidecarDisplayFeatures.add(foldFeature);
        SidecarWindowLayoutInfo windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures);

        List<DisplayFeature> expectedFeatures = new ArrayList<>();
        expectedFeatures.add(new DisplayFeature(foldFeature.getRect(), DisplayFeature.TYPE_FOLD));
        WindowLayoutInfo expected = new WindowLayoutInfo(expectedFeatures);

        SidecarAdapter sidecarAdapter = new SidecarAdapter();

        WindowLayoutInfo actual = sidecarAdapter.translate(mockActivity, windowLayoutInfo);

        assertEquals(expected, actual);
    }

    @Test
    @Override
    public void testTranslateWindowLayoutInfo_filterRemovesEmptyBoundsFeature() {
        List<SidecarDisplayFeature> sidecarDisplayFeatures = new ArrayList<>();
        sidecarDisplayFeatures.add(
                sidecarDisplayFeature(new Rect(), SidecarDisplayFeature.TYPE_FOLD));

        SidecarAdapter sidecarAdapter = new SidecarAdapter();
        SidecarWindowLayoutInfo windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures);
        Activity mockActivity = mock(Activity.class);

        WindowLayoutInfo actual = sidecarAdapter.translate(mockActivity, windowLayoutInfo);

        assertTrue(actual.getDisplayFeatures().isEmpty());
    }


    @Test
    @Override
    public void testTranslateWindowLayoutInfo_filterRemovesNonEmptyAreaFoldFeature() {
        List<SidecarDisplayFeature> sidecarDisplayFeatures = new ArrayList<>();
        Rect fullWidthBounds = new Rect(0, 1, WINDOW_BOUNDS.width(), 2);
        Rect fullHeightBounds = new Rect(1, 0, 2, WINDOW_BOUNDS.height());
        sidecarDisplayFeatures.add(sidecarDisplayFeature(fullWidthBounds,
                SidecarDisplayFeature.TYPE_FOLD));
        sidecarDisplayFeatures.add(sidecarDisplayFeature(fullHeightBounds,
                SidecarDisplayFeature.TYPE_FOLD));

        ExtensionInterfaceCompat.ExtensionCallbackInterface mockCallback = mock(
                ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        SidecarAdapter sidecarCallbackAdapter = new SidecarAdapter();
        SidecarWindowLayoutInfo windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures);
        Activity mockActivity = mock(Activity.class);

        WindowLayoutInfo actual = sidecarCallbackAdapter.translate(mockActivity, windowLayoutInfo);

        assertTrue(actual.getDisplayFeatures().isEmpty());
    }


    @Test
    @Override
    public void testTranslateWindowLayoutInfo_filterRemovesHingeFeatureNotSpanningFullDimension() {
        List<SidecarDisplayFeature> sidecarDisplayFeatures = new ArrayList<>();
        Rect fullWidthBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top,
                WINDOW_BOUNDS.right / 2, 2);
        Rect fullHeightBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, 2,
                WINDOW_BOUNDS.bottom / 2);
        sidecarDisplayFeatures.add(sidecarDisplayFeature(fullWidthBounds,
                SidecarDisplayFeature.TYPE_HINGE));
        sidecarDisplayFeatures.add(sidecarDisplayFeature(fullHeightBounds,
                SidecarDisplayFeature.TYPE_HINGE));

        SidecarAdapter sidecarAdapter = new SidecarAdapter();
        SidecarWindowLayoutInfo windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures);

        Activity mockActivity = mock(Activity.class);

        WindowLayoutInfo actual = sidecarAdapter.translate(mockActivity, windowLayoutInfo);

        assertTrue(actual.getDisplayFeatures().isEmpty());
    }

    @Test
    @Override
    public void testTranslateWindowLayoutInfo_filterRemovesFoldFeatureNotSpanningFullDimension() {
        List<SidecarDisplayFeature> extensionDisplayFeatures = new ArrayList<>();
        Rect fullWidthBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top,
                WINDOW_BOUNDS.right / 2, WINDOW_BOUNDS.top);
        Rect fullHeightBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, WINDOW_BOUNDS.left,
                WINDOW_BOUNDS.bottom / 2);
        extensionDisplayFeatures.add(sidecarDisplayFeature(fullWidthBounds,
                SidecarDisplayFeature.TYPE_HINGE));
        extensionDisplayFeatures.add(sidecarDisplayFeature(fullHeightBounds,
                SidecarDisplayFeature.TYPE_HINGE));

        SidecarAdapter sidecarCallbackAdapter = new SidecarAdapter();
        SidecarWindowLayoutInfo windowLayoutInfo = sidecarWindowLayoutInfo(
                extensionDisplayFeatures);

        Activity mockActivity = mock(Activity.class);

        WindowLayoutInfo actual = sidecarCallbackAdapter.translate(mockActivity, windowLayoutInfo);

        assertTrue(actual.getDisplayFeatures().isEmpty());
    }

    @Test
    @Override
    public void testTranslateDeviceState() {
        ExtensionInterfaceCompat.ExtensionCallbackInterface mockCallback = mock(
                ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        SidecarAdapter sidecarCallbackAdapter = new SidecarAdapter();
        List<DeviceState> values = new ArrayList<>();

        values.add(sidecarCallbackAdapter.translate(sidecarDeviceState(
                SidecarDeviceState.POSTURE_UNKNOWN)));
        values.add(sidecarCallbackAdapter.translate(sidecarDeviceState(
                SidecarDeviceState.POSTURE_CLOSED)));
        values.add(sidecarCallbackAdapter.translate(sidecarDeviceState(
                SidecarDeviceState.POSTURE_HALF_OPENED)));
        values.add(sidecarCallbackAdapter.translate(sidecarDeviceState(
                SidecarDeviceState.POSTURE_OPENED)));
        values.add(sidecarCallbackAdapter.translate(sidecarDeviceState(
                SidecarDeviceState.POSTURE_FLIPPED)));

        assertEquals(DeviceState.POSTURE_UNKNOWN, values.get(0).getPosture());
        assertEquals(DeviceState.POSTURE_CLOSED, values.get(1).getPosture());
        assertEquals(DeviceState.POSTURE_HALF_OPENED, values.get(2).getPosture());
        assertEquals(DeviceState.POSTURE_OPENED, values.get(3).getPosture());
        assertEquals(DeviceState.POSTURE_FLIPPED, values.get(4).getPosture());
        assertEquals(5, values.size());
    }
}
