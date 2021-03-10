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

import androidx.window.extensions.ExtensionDisplayFeature;
import androidx.window.extensions.ExtensionFoldingFeature;
import androidx.window.extensions.ExtensionWindowLayoutInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link ExtensionAdapter} checking that the data is translated correctly from
 * window.extensions to window.window.  Implements {@link TranslatorTestInterface} to ensure same
 * requirements are met as {@link SidecarAdapter}
 *
 * Run: ./gradlew window:window:connectedAndroidTest
 */
public class ExtensionAdapterTest implements TranslatorTestInterface {

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
    @Override
    public void testTranslate_validFeature() {
        Activity mockActivity = mock(Activity.class);
        Rect bounds = new Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0);
        ExtensionDisplayFeature foldFeature = new ExtensionFoldingFeature(bounds,
                ExtensionFoldingFeature.TYPE_FOLD, ExtensionFoldingFeature.STATE_FLAT);

        List<ExtensionDisplayFeature> extensionDisplayFeatures = new ArrayList<>();
        extensionDisplayFeatures.add(foldFeature);
        ExtensionWindowLayoutInfo windowLayoutInfo =
                new ExtensionWindowLayoutInfo(extensionDisplayFeatures);

        List<DisplayFeature> expectedFeatures = new ArrayList<>();
        expectedFeatures.add(new FoldingFeature(foldFeature.getBounds(), FoldingFeature.TYPE_FOLD,
                FoldingFeature.STATE_FLAT));
        WindowLayoutInfo expected = new WindowLayoutInfo(expectedFeatures);

        ExtensionAdapter adapter = new ExtensionAdapter();

        WindowLayoutInfo actual = adapter.translate(mockActivity, windowLayoutInfo);

        assertEquals(expected, actual);
    }

    @Test
    @Override
    public void testTranslateWindowLayoutInfo_filterRemovesHingeFeatureNotSpanningFullDimension() {
        List<ExtensionDisplayFeature> extensionDisplayFeatures = new ArrayList<>();
        Rect fullWidthBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top,
                WINDOW_BOUNDS.right / 2, 2);
        Rect fullHeightBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, 2,
                WINDOW_BOUNDS.bottom / 2);
        extensionDisplayFeatures.add(new ExtensionFoldingFeature(fullWidthBounds,
                ExtensionFoldingFeature.TYPE_HINGE, ExtensionFoldingFeature.STATE_FLAT));
        extensionDisplayFeatures.add(new ExtensionFoldingFeature(fullHeightBounds,
                ExtensionFoldingFeature.TYPE_HINGE, ExtensionFoldingFeature.STATE_FLAT));

        ExtensionAdapter extensionCallbackAdapter = new ExtensionAdapter();
        ExtensionWindowLayoutInfo windowLayoutInfo =
                new ExtensionWindowLayoutInfo(extensionDisplayFeatures);

        Activity mockActivity = mock(Activity.class);

        WindowLayoutInfo actual = extensionCallbackAdapter.translate(mockActivity,
                windowLayoutInfo);

        assertTrue("Remove hinge feature not spanning full dimension",
                actual.getDisplayFeatures().isEmpty());
    }

    @Test
    @Override
    public void testTranslateWindowLayoutInfo_filterRemovesFoldFeatureNotSpanningFullDimension() {
        List<ExtensionDisplayFeature> extensionDisplayFeatures = new ArrayList<>();
        Rect fullWidthBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top,
                WINDOW_BOUNDS.right / 2, WINDOW_BOUNDS.top);
        Rect fullHeightBounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, WINDOW_BOUNDS.left,
                WINDOW_BOUNDS.bottom / 2);
        extensionDisplayFeatures.add(new ExtensionFoldingFeature(fullWidthBounds,
                ExtensionFoldingFeature.TYPE_HINGE, ExtensionFoldingFeature.STATE_FLAT));
        extensionDisplayFeatures.add(new ExtensionFoldingFeature(fullHeightBounds,
                ExtensionFoldingFeature.TYPE_HINGE, ExtensionFoldingFeature.STATE_FLAT));

        ExtensionAdapter adapter = new ExtensionAdapter();
        ExtensionWindowLayoutInfo windowLayoutInfo =
                new ExtensionWindowLayoutInfo(extensionDisplayFeatures);

        Activity mockActivity = mock(Activity.class);

        WindowLayoutInfo actual = adapter.translate(mockActivity, windowLayoutInfo);

        assertTrue("Remove fold feature not spanning full dimension",
                actual.getDisplayFeatures().isEmpty());
    }

    @Test
    @Override
    public void testTranslateWindowLayoutInfo_filterRemovesUnknownFeature() {
        Rect bounds = new Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0);
        ExtensionDisplayFeature foldFeature = new ExtensionFoldingFeature(bounds,
                0 /* unknown */, ExtensionFoldingFeature.STATE_FLAT);

        List<ExtensionDisplayFeature> extensionDisplayFeatures = new ArrayList<>();
        extensionDisplayFeatures.add(foldFeature);
        ExtensionWindowLayoutInfo windowLayoutInfo =
                new ExtensionWindowLayoutInfo(extensionDisplayFeatures);

        Activity mockActivity = mock(Activity.class);

        ExtensionAdapter adapter = new ExtensionAdapter();
        WindowLayoutInfo actual = adapter.translate(mockActivity, windowLayoutInfo);

        assertTrue("Remove fold feature not spanning full dimension",
                actual.getDisplayFeatures().isEmpty());
    }
}
