/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.window

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.window.FoldingFeature.OcclusionType
import androidx.window.TestFoldingFeatureUtil.allFoldStates
import androidx.window.TestFoldingFeatureUtil.allFoldingFeatureTypeAndStates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [FoldingFeature] class.  */
@SmallTest
@RunWith(AndroidJUnit4::class)
public class FoldingFeatureTest {

    @Test(expected = IllegalArgumentException::class)
    public fun tesEmptyRect() {
        FoldingFeature(Rect(), FoldingFeature.TYPE_HINGE, FoldingFeature.STATE_HALF_OPENED)
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testHorizontalHingeWithNonZeroOrigin() {
        FoldingFeature(
            Rect(1, 10, 20, 10),
            FoldingFeature.TYPE_HINGE,
            FoldingFeature.STATE_HALF_OPENED
        )
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testVerticalHingeWithNonZeroOrigin() {
        FoldingFeature(
            Rect(10, 1, 19, 29),
            FoldingFeature.TYPE_HINGE,
            FoldingFeature.STATE_HALF_OPENED
        )
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testHorizontalFoldWithNonZeroOrigin() {
        FoldingFeature(
            Rect(1, 10, 20, 10),
            FoldingFeature.TYPE_FOLD,
            FoldingFeature.STATE_HALF_OPENED
        )
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testVerticalFoldWithNonZeroOrigin() {
        FoldingFeature(
            Rect(10, 1, 10, 20),
            FoldingFeature.TYPE_FOLD,
            FoldingFeature.STATE_HALF_OPENED
        )
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testInvalidType() {
        FoldingFeature(Rect(0, 10, 30, 10), -1, FoldingFeature.STATE_HALF_OPENED)
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testInvalidState() {
        FoldingFeature(Rect(0, 10, 30, 10), FoldingFeature.TYPE_FOLD, -1)
    }

    @Test // TODO(b/173739071) remove when getType is package private
    public fun testSetBoundsAndType() {
        val bounds = Rect(0, 10, 30, 10)
        val type = FoldingFeature.TYPE_HINGE
        val state = FoldingFeature.STATE_HALF_OPENED
        val feature = FoldingFeature(bounds, type, state)
        assertEquals(bounds, feature.bounds)
        assertEquals(type.toLong(), feature.type.toLong())
        assertEquals(state.toLong(), feature.state.toLong())
    }

    @Test
    public fun testEquals_sameAttributes() {
        val bounds = Rect(1, 0, 1, 10)
        val type = FoldingFeature.TYPE_FOLD
        val state = FoldingFeature.STATE_FLAT
        val original = FoldingFeature(bounds, type, state)
        val copy = FoldingFeature(bounds, type, state)
        assertEquals(original, copy)
    }

    @Test
    public fun testEquals_differentRect() {
        val originalRect = Rect(1, 0, 1, 10)
        val otherRect = Rect(2, 0, 2, 10)
        val type = FoldingFeature.TYPE_FOLD
        val state = FoldingFeature.STATE_FLAT
        val original = FoldingFeature(originalRect, type, state)
        val other = FoldingFeature(otherRect, type, state)
        assertNotEquals(original, other)
    }

    @Test
    public fun testEquals_differentType() {
        val rect = Rect(1, 0, 1, 10)
        val originalType = FoldingFeature.TYPE_FOLD
        val otherType = FoldingFeature.TYPE_HINGE
        val state = FoldingFeature.STATE_FLAT
        val original = FoldingFeature(rect, originalType, state)
        val other = FoldingFeature(rect, otherType, state)
        assertNotEquals(original, other)
    }

    @Test
    public fun testEquals_differentState() {
        val rect = Rect(1, 0, 1, 10)
        val type = FoldingFeature.TYPE_FOLD
        val originalState = FoldingFeature.STATE_FLAT
        val otherState = FoldingFeature.STATE_HALF_OPENED
        val original = FoldingFeature(rect, type, originalState)
        val other = FoldingFeature(rect, type, otherState)
        assertNotEquals(original, other)
    }

    @Test
    public fun testHashCode_matchesIfEqual() {
        val originalRect = Rect(1, 0, 1, 10)
        val matchingRect = Rect(1, 0, 1, 10)
        val type = FoldingFeature.TYPE_FOLD
        val state = FoldingFeature.STATE_FLAT
        val original = FoldingFeature(originalRect, type, state)
        val matching = FoldingFeature(matchingRect, type, state)
        assertEquals(original, matching)
        assertEquals(original.hashCode().toLong(), matching.hashCode().toLong())
    }

    @Test
    public fun testIsSeparating_trueForHinge() {
        val bounds = Rect(1, 0, 1, 10)
        for (feature in allFoldStates(bounds, FoldingFeature.TYPE_HINGE)) {
            assertTrue(separatingModeErrorMessage(true, feature), feature.isSeparating)
        }
    }

    @Test
    public fun testIsSeparating_falseForFlatFold() {
        val bounds = Rect(1, 0, 1, 10)
        val feature = FoldingFeature(bounds, FoldingFeature.TYPE_FOLD, FoldingFeature.STATE_FLAT)
        assertFalse(separatingModeErrorMessage(false, feature), feature.isSeparating)
    }

    @Test
    public fun testIsSeparating_trueForNotFlatFold() {
        val bounds = Rect(1, 0, 1, 10)
        val nonFlatFeatures = mutableListOf<FoldingFeature>()
        for (feature in allFoldStates(bounds, FoldingFeature.TYPE_FOLD)) {
            if (feature.state != FoldingFeature.STATE_FLAT) {
                nonFlatFeatures.add(feature)
            }
        }
        for (feature in nonFlatFeatures) {
            assertTrue(separatingModeErrorMessage(true, feature), feature.isSeparating)
        }
    }

    @Test
    public fun testOcclusionTypeNone_emptyFeature() {
        val bounds = Rect(0, 100, 100, 100)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(
                occlusionTypeErrorMessage(FoldingFeature.OCCLUSION_NONE, feature),
                FoldingFeature.OCCLUSION_NONE.toLong(), feature.occlusionMode.toLong()
            )
        }
    }

    @Test
    public fun testOcclusionTypeFull_nonEmptyHingeFeature() {
        val bounds = Rect(0, 100, 100, 101)
        for (feature in allFoldStates(bounds, FoldingFeature.TYPE_HINGE)) {
            assertEquals(
                occlusionTypeErrorMessage(FoldingFeature.OCCLUSION_FULL, feature),
                FoldingFeature.OCCLUSION_FULL.toLong(), feature.occlusionMode.toLong()
            )
        }
    }

    @Test
    public fun testGetFeatureOrientation_isHorizontalWhenWidthIsGreaterThanHeight() {
        val bounds = Rect(0, 100, 200, 100)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(
                featureOrientationErrorMessage(FoldingFeature.ORIENTATION_HORIZONTAL, feature),
                FoldingFeature.ORIENTATION_HORIZONTAL.toLong(), feature.orientation.toLong()
            )
        }
    }

    @Test
    public fun testGetFeatureOrientation_isVerticalWhenHeightIsGreaterThanWidth() {
        val bounds = Rect(100, 0, 100, 200)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(
                featureOrientationErrorMessage(FoldingFeature.ORIENTATION_VERTICAL, feature),
                FoldingFeature.ORIENTATION_VERTICAL.toLong(), feature.orientation.toLong()
            )
        }
    }

    @Test
    public fun testGetFeatureOrientation_isVerticalWhenHeightIsEqualToWidth() {
        val bounds = Rect(0, 0, 100, 100)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(
                featureOrientationErrorMessage(FoldingFeature.ORIENTATION_VERTICAL, feature),
                FoldingFeature.ORIENTATION_VERTICAL.toLong(), feature.orientation.toLong()
            )
        }
    }

    private fun separatingModeErrorMessage(expected: Boolean, feature: FoldingFeature): String {
        return errorMessage(
            FoldingFeature::class.java.simpleName,
            "isSeparating",
            expected.toString(),
            feature.isSeparating.toString(),
            feature
        )
    }

    internal companion object {
        private fun occlusionTypeErrorMessage(
            @OcclusionType expected: Int,
            feature: FoldingFeature
        ): String {
            return errorMessage(
                FoldingFeature::class.java.simpleName, "getOcclusionMode",
                FoldingFeature.occlusionTypeToString(expected),
                FoldingFeature.occlusionTypeToString(feature.occlusionMode), feature
            )
        }

        private fun featureOrientationErrorMessage(
            @FoldingFeature.Orientation expected: Int,
            feature: FoldingFeature
        ): String {
            return errorMessage(
                FoldingFeature::class.java.simpleName, "getFeatureOrientation",
                FoldingFeature.orientationToString(expected),
                FoldingFeature.orientationToString(feature.orientation), feature
            )
        }

        private fun errorMessage(
            className: String,
            methodName: String,
            expected: String,
            actual: String,
            value: Any
        ): String {
            return String.format(
                "%s#%s was expected to be %s but was %s. %s: %s", className,
                methodName, expected, actual, className, value.toString()
            )
        }
    }
}
