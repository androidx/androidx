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

import static androidx.window.FoldingFeature.OCCLUSION_FULL;
import static androidx.window.FoldingFeature.OCCLUSION_NONE;
import static androidx.window.FoldingFeature.ORIENTATION_HORIZONTAL;
import static androidx.window.FoldingFeature.ORIENTATION_VERTICAL;
import static androidx.window.FoldingFeature.OcclusionType;
import static androidx.window.FoldingFeature.Orientation;
import static androidx.window.FoldingFeature.STATE_FLAT;
import static androidx.window.FoldingFeature.STATE_HALF_OPENED;
import static androidx.window.FoldingFeature.TYPE_FOLD;
import static androidx.window.FoldingFeature.TYPE_HINGE;
import static androidx.window.FoldingFeature.occlusionTypeToString;
import static androidx.window.FoldingFeature.orientationToString;
import static androidx.window.TestFoldingFeatureUtil.allFoldStates;
import static androidx.window.TestFoldingFeatureUtil.allFoldingFeatureTypeAndStates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link FoldingFeature} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class FoldingFeatureTest {

    @Test(expected = IllegalArgumentException.class)
    public void tesEmptyRect() {
        new FoldingFeature(new Rect(), TYPE_HINGE, STATE_HALF_OPENED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHorizontalHingeWithNonZeroOrigin() {
        new FoldingFeature(new Rect(1, 10, 20, 10), TYPE_HINGE, STATE_HALF_OPENED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerticalHingeWithNonZeroOrigin() {
        new FoldingFeature(new Rect(10, 1, 19, 29), TYPE_HINGE, STATE_HALF_OPENED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHorizontalFoldWithNonZeroOrigin() {
        new FoldingFeature(new Rect(1, 10, 20, 10), TYPE_FOLD, STATE_HALF_OPENED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerticalFoldWithNonZeroOrigin() {
        new FoldingFeature(new Rect(10, 1, 10, 20), TYPE_FOLD, STATE_HALF_OPENED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidType() {
        new FoldingFeature(new Rect(0, 10, 30, 10), -1, STATE_HALF_OPENED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidState() {
        new FoldingFeature(new Rect(0, 10, 30, 10), TYPE_FOLD, -1);
    }

    @Test
    @SuppressWarnings("deprecation") // TODO(b/173739071) remove when getType is package private
    public void testSetBoundsAndType() {
        Rect bounds = new Rect(0, 10, 30, 10);
        int type = TYPE_HINGE;
        int state = STATE_HALF_OPENED;
        FoldingFeature feature = new FoldingFeature(bounds, type, state);

        assertEquals(bounds, feature.getBounds());
        assertEquals(type, feature.getType());
        assertEquals(state, feature.getState());
    }

    @Test
    public void testEquals_sameAttributes() {
        Rect bounds = new Rect(1, 0, 1, 10);
        int type = TYPE_FOLD;
        int state = STATE_FLAT;

        FoldingFeature original = new FoldingFeature(bounds, type, state);
        FoldingFeature copy = new FoldingFeature(bounds, type, state);

        assertEquals(original, copy);
    }

    @Test
    public void testEquals_differentRect() {
        Rect originalRect = new Rect(1, 0, 1, 10);
        Rect otherRect = new Rect(2, 0, 2, 10);
        int type = TYPE_FOLD;
        int state = STATE_FLAT;

        FoldingFeature original = new FoldingFeature(originalRect, type, state);
        FoldingFeature other = new FoldingFeature(otherRect, type, state);

        assertNotEquals(original, other);
    }

    @Test
    public void testEquals_differentType() {
        Rect rect = new Rect(1, 0, 1, 10);
        int originalType = TYPE_FOLD;
        int otherType = TYPE_HINGE;
        int state = STATE_FLAT;

        FoldingFeature original = new FoldingFeature(rect, originalType, state);
        FoldingFeature other = new FoldingFeature(rect, otherType, state);

        assertNotEquals(original, other);
    }

    @Test
    public void testEquals_differentState() {
        Rect rect = new Rect(1, 0, 1, 10);
        int type = TYPE_FOLD;
        int originalState = STATE_FLAT;
        int otherState = STATE_HALF_OPENED;

        FoldingFeature original = new FoldingFeature(rect, type, originalState);
        FoldingFeature other = new FoldingFeature(rect, type, otherState);

        assertNotEquals(original, other);
    }

    @Test
    public void testHashCode_matchesIfEqual() {
        Rect originalRect = new Rect(1, 0, 1, 10);
        Rect matchingRect = new Rect(1, 0, 1, 10);
        int type = TYPE_FOLD;
        int state = STATE_FLAT;

        FoldingFeature original = new FoldingFeature(originalRect, type, state);
        FoldingFeature matching = new FoldingFeature(matchingRect, type, state);

        assertEquals(original, matching);
        assertEquals(original.hashCode(), matching.hashCode());
    }

    @Test
    public void testIsSeparating_trueForHinge() {
        Rect bounds = new Rect(1, 0, 1, 10);

        for (FoldingFeature feature : allFoldStates(bounds, TYPE_HINGE)) {
            assertTrue(separatingModeErrorMessage(true, feature), feature.isSeparating());
        }
    }

    @Test
    public void testIsSeparating_falseForFlatFold() {
        Rect bounds = new Rect(1, 0, 1, 10);

        FoldingFeature feature = new FoldingFeature(bounds, TYPE_FOLD, STATE_FLAT);

        assertFalse(separatingModeErrorMessage(false, feature), feature.isSeparating());
    }

    @Test
    public void testIsSeparating_trueForNotFlatFold() {
        Rect bounds = new Rect(1, 0, 1, 10);

        List<FoldingFeature> nonFlatFeatures = new ArrayList<>();
        for (FoldingFeature feature : allFoldStates(bounds, TYPE_FOLD)) {
            if (feature.getState() != STATE_FLAT) {
                nonFlatFeatures.add(feature);
            }
        }

        for (FoldingFeature feature : nonFlatFeatures) {
            assertTrue(separatingModeErrorMessage(true, feature), feature.isSeparating());
        }
    }

    @Test
    public void testOcclusionTypeNone_emptyFeature() {
        Rect bounds = new Rect(0, 100, 100, 100);

        for (FoldingFeature feature: allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(occlusionTypeErrorMessage(OCCLUSION_NONE, feature),
                    OCCLUSION_NONE, feature.getOcclusionMode());
        }
    }

    @Test
    public void testOcclusionTypeFull_nonEmptyHingeFeature() {
        Rect bounds = new Rect(0, 100, 100, 101);

        for (FoldingFeature feature: allFoldStates(bounds, TYPE_HINGE)) {
            assertEquals(occlusionTypeErrorMessage(OCCLUSION_FULL, feature),
                    OCCLUSION_FULL, feature.getOcclusionMode());
        }
    }

    @Test
    public void testGetFeatureOrientation_isHorizontalWhenWidthIsGreaterThanHeight() {
        Rect bounds = new Rect(0, 100, 200, 100);

        for (FoldingFeature feature: allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(featureOrientationErrorMessage(ORIENTATION_HORIZONTAL, feature),
                    ORIENTATION_HORIZONTAL, feature.getOrientation());
        }
    }

    @Test
    public void testGetFeatureOrientation_isVerticalWhenHeightIsGreaterThanWidth() {
        Rect bounds = new Rect(100, 0, 100, 200);

        for (FoldingFeature feature: allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(featureOrientationErrorMessage(ORIENTATION_VERTICAL, feature),
                    ORIENTATION_VERTICAL, feature.getOrientation());
        }
    }

    @Test
    public void testGetFeatureOrientation_isVerticalWhenHeightIsEqualToWidth() {
        Rect bounds = new Rect(0, 0, 100, 100);

        for (FoldingFeature feature: allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(featureOrientationErrorMessage(ORIENTATION_VERTICAL, feature),
                    ORIENTATION_VERTICAL, feature.getOrientation());
        }
    }

    @NonNull
    private String separatingModeErrorMessage(boolean expected, @NonNull FoldingFeature feature) {
        return errorMessage(FoldingFeature.class.getSimpleName(), "isSeparating",
                Boolean.toString(expected), Boolean.toString(feature.isSeparating()), feature);
    }

    @NonNull
    private static String occlusionTypeErrorMessage(@OcclusionType int expected,
            FoldingFeature feature) {
        return errorMessage(FoldingFeature.class.getSimpleName(), "getOcclusionMode",
                occlusionTypeToString(expected),
                occlusionTypeToString(feature.getOcclusionMode()), feature);
    }

    @NonNull
    private static String featureOrientationErrorMessage(@Orientation int expected,
            FoldingFeature feature) {
        return errorMessage(FoldingFeature.class.getSimpleName(), "getFeatureOrientation",
                orientationToString(expected),
                orientationToString(feature.getOrientation()), feature);
    }

    private static String errorMessage(String className, String methodName, String expected,
            String actual, Object value) {
        return String.format("%s#%s was expected to be %s but was %s. %s: %s", className,
                methodName, expected, actual, className, value.toString());
    }
}
