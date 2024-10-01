/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.testing.embedding;

import static androidx.window.embedding.SplitAttributes.SplitType.SPLIT_TYPE_HINGE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.window.core.ExperimentalWindowApi;
import androidx.window.embedding.SplitAttributes;
import androidx.window.embedding.SplitAttributesCalculatorParams;
import androidx.window.layout.DisplayFeature;
import androidx.window.layout.FoldingFeature;
import androidx.window.layout.WindowLayoutInfo;
import androidx.window.layout.WindowMetrics;
import androidx.window.testing.layout.DisplayFeatureTesting;
import androidx.window.testing.layout.WindowLayoutInfoTesting;

import kotlin.OptIn;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Test class to verify {@link TestSplitAttributesCalculatorParams} in Java. */
@RunWith(RobolectricTestRunner.class)
public class SplitAttributesCalculatorParamsTestingJavaTest {
    private static final Rect TEST_BOUNDS = new Rect(0, 0, 2000, 2000);
    private static final WindowMetrics TEST_METRICS = new WindowMetrics(TEST_BOUNDS,
            1f /* density */);
    private static final SplitAttributes DEFAULT_SPLIT_ATTRIBUTES =
            new SplitAttributes.Builder().build();
    private static final SplitAttributes TABLETOP_HINGE_ATTRIBUTES = new SplitAttributes.Builder()
            .setSplitType(SPLIT_TYPE_HINGE)
            .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
            .build();

    /**
     * Verifies if the default value of {@link TestSplitAttributesCalculatorParams} is as expected.
     */
    @Test
    public void testDefaults() {
        final SplitAttributesCalculatorParams params = TestSplitAttributesCalculatorParams
                .createTestSplitAttributesCalculatorParams(TEST_METRICS);

        assertEquals(TEST_METRICS, params.getParentWindowMetrics());
        assertEquals(0, params.getParentConfiguration().diff(new Configuration()));
        assertEquals(DEFAULT_SPLIT_ATTRIBUTES, params.getDefaultSplitAttributes());
        assertTrue(params.areDefaultConstraintsSatisfied());
        assertEquals(new WindowLayoutInfo(Collections.emptyList()),
                params.getParentWindowLayoutInfo());
        assertNull(params.getSplitRuleTag());

        assertEquals(DEFAULT_SPLIT_ATTRIBUTES, testSplitAttributesCalculator(params));
    }

    @OptIn(markerClass = ExperimentalWindowApi.class)
    @Test
    public void testParamsWithTabletopFoldingFeature() {
        final FoldingFeature tabletopFoldingFeature =
                DisplayFeatureTesting.createFoldingFeature(TEST_BOUNDS);
        final List<DisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(tabletopFoldingFeature);
        final WindowLayoutInfo parentWindowLayoutInfo =
                WindowLayoutInfoTesting.createWindowLayoutInfo(displayFeatures);
        SplitAttributesCalculatorParams params = TestSplitAttributesCalculatorParams
                .createTestSplitAttributesCalculatorParams(TEST_METRICS, new Configuration(),
                        parentWindowLayoutInfo, DEFAULT_SPLIT_ATTRIBUTES,  true);

        assertEquals(TABLETOP_HINGE_ATTRIBUTES, testSplitAttributesCalculator(params));
    }

    private SplitAttributes testSplitAttributesCalculator(
            @NonNull SplitAttributesCalculatorParams params) {
        List<DisplayFeature> displayFeatures = params.getParentWindowLayoutInfo()
                .getDisplayFeatures();
        List<FoldingFeature> foldingFeatures = new ArrayList<>();
        for (DisplayFeature feature : displayFeatures) {
            if (feature instanceof FoldingFeature) {
                foldingFeatures.add((FoldingFeature) feature);
            }
        }
        final FoldingFeature foldingFeature = (foldingFeatures.size() == 1)
                ? foldingFeatures.get(0) : null;
        if (foldingFeature != null
                && foldingFeature.getState().equals(FoldingFeature.State.HALF_OPENED)
                && foldingFeature.getOrientation().equals(
                        FoldingFeature.Orientation.HORIZONTAL)) {
            return TABLETOP_HINGE_ATTRIBUTES;
        }
        if (params.areDefaultConstraintsSatisfied()) {
            return params.getDefaultSplitAttributes();
        } else {
            return new SplitAttributes.Builder()
                    .setSplitType(SplitAttributes.SplitType.SPLIT_TYPE_EXPAND)
                    .build();
        }
    }
}
