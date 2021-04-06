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

import static androidx.window.SidecarAdapter.setSidecarDevicePosture;
import static androidx.window.SidecarAdapter.setSidecarDisplayFeatures;

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
        setSidecarDisplayFeatures(layoutInfo, features);
        return layoutInfo;
    }

    private static SidecarDeviceState sidecarDeviceState(int posture) {
        SidecarDeviceState deviceState = new SidecarDeviceState();
        setSidecarDevicePosture(deviceState, posture);
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

        SidecarDeviceState state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED);

        List<DisplayFeature> expectedFeatures = new ArrayList<>();
        expectedFeatures.add(new FoldingFeature(foldFeature.getRect(), FoldingFeature.TYPE_FOLD,
                FoldingFeature.STATE_FLAT));
        WindowLayoutInfo expected = new WindowLayoutInfo(expectedFeatures);

        SidecarAdapter sidecarAdapter = new SidecarAdapter();

        WindowLayoutInfo actual = sidecarAdapter.translate(mockActivity, windowLayoutInfo, state);

        assertEquals(expected, actual);
    }

    @Test
    public void testTranslateWindowLayoutInfo_filterRemovesEmptyBoundsFeature() {
        List<SidecarDisplayFeature> sidecarDisplayFeatures = new ArrayList<>();
        sidecarDisplayFeatures.add(
                sidecarDisplayFeature(new Rect(), SidecarDisplayFeature.TYPE_FOLD));

        SidecarAdapter sidecarAdapter = new SidecarAdapter();
        SidecarWindowLayoutInfo windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures);
        Activity mockActivity = mock(Activity.class);
        SidecarDeviceState state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED);

        WindowLayoutInfo actual = sidecarAdapter.translate(mockActivity, windowLayoutInfo, state);

        assertTrue(actual.getDisplayFeatures().isEmpty());
    }


    @Test
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

        SidecarDeviceState state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED);

        WindowLayoutInfo actual = sidecarCallbackAdapter.translate(mockActivity, windowLayoutInfo,
                state);

        assertTrue(actual.getDisplayFeatures().isEmpty());
    }

    // TODO(b/175507310): Reinstate after fix.
    // @Test
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
        SidecarDeviceState state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED);

        Activity mockActivity = mock(Activity.class);

        WindowLayoutInfo actual = sidecarAdapter.translate(mockActivity, windowLayoutInfo, state);

        assertTrue(actual.getDisplayFeatures().isEmpty());
    }

    // TODO(b/175507310): Reinstate after fix.
    // @Test
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
        SidecarDeviceState state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED);

        Activity mockActivity = mock(Activity.class);

        WindowLayoutInfo actual = sidecarCallbackAdapter.translate(mockActivity, windowLayoutInfo,
                state);

        assertTrue(actual.getDisplayFeatures().isEmpty());
    }

    @Test
    @Override
    public void testTranslateWindowLayoutInfo_filterRemovesUnknownFeature() {
        List<SidecarDisplayFeature> sidecarDisplayFeatures = new ArrayList<>();
        Rect bounds = new Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0);
        SidecarDisplayFeature unknownFeature = sidecarDisplayFeature(bounds, 0 /* unknown */);
        sidecarDisplayFeatures.add(unknownFeature);

        SidecarAdapter sidecarAdapter = new SidecarAdapter();
        SidecarWindowLayoutInfo windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures);
        Activity mockActivity = mock(Activity.class);
        SidecarDeviceState state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED);

        WindowLayoutInfo actual = sidecarAdapter.translate(mockActivity, windowLayoutInfo, state);

        assertTrue(actual.getDisplayFeatures().isEmpty());
    }

    @Test
    public void testTranslateWindowLayoutInfo_filterRemovesInvalidPostureFeature() {
        List<SidecarDisplayFeature> sidecarDisplayFeatures = new ArrayList<>();
        Rect bounds = new Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0);
        SidecarDisplayFeature unknownFeature = sidecarDisplayFeature(bounds, -1000 /* invalid */);
        sidecarDisplayFeatures.add(unknownFeature);

        SidecarAdapter sidecarAdapter = new SidecarAdapter();
        SidecarWindowLayoutInfo windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures);
        Activity mockActivity = mock(Activity.class);
        SidecarDeviceState state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED);

        WindowLayoutInfo actual = sidecarAdapter.translate(mockActivity, windowLayoutInfo, state);

        assertTrue(actual.getDisplayFeatures().isEmpty());
    }
}
