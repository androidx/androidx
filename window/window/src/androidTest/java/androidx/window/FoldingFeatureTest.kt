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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.window.FoldingFeature.OcclusionType
import androidx.window.FoldingFeature.Orientation
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.FoldingFeature.Type.Companion.FOLD
import androidx.window.FoldingFeature.Type.Companion.HINGE
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
        FoldingFeature(Bounds(0, 0, 0, 0), HINGE, HALF_OPENED)
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testHorizontalHingeWithNonZeroOrigin() {
        FoldingFeature(Bounds(1, 10, 20, 10), HINGE, HALF_OPENED)
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testVerticalHingeWithNonZeroOrigin() {
        FoldingFeature(Bounds(10, 1, 19, 29), HINGE, HALF_OPENED)
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testHorizontalFoldWithNonZeroOrigin() {
        FoldingFeature(Bounds(1, 10, 20, 10), FOLD, HALF_OPENED)
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testVerticalFoldWithNonZeroOrigin() {
        FoldingFeature(Bounds(10, 1, 10, 20), FOLD, HALF_OPENED)
    }

    @Test // TODO(b/173739071) remove when getType is package private
    public fun testSetBoundsAndType() {
        val bounds = Bounds(0, 10, 30, 10)
        val type = HINGE
        val state = HALF_OPENED
        val feature = FoldingFeature(bounds, type, state)
        assertEquals(bounds.toRect(), feature.bounds)
        assertEquals(type, feature.type)
        assertEquals(state, feature.state)
    }

    @Test
    public fun testEquals_sameAttributes() {
        val bounds = Bounds(1, 0, 1, 10)
        val type = FOLD
        val state = FLAT
        val original = FoldingFeature(bounds, type, state)
        val copy = FoldingFeature(bounds, type, state)
        assertEquals(original, copy)
    }

    @Test
    public fun testEquals_differentRect() {
        val originalRect = Bounds(1, 0, 1, 10)
        val otherRect = Bounds(2, 0, 2, 10)
        val type = FOLD
        val state = FLAT
        val original = FoldingFeature(originalRect, type, state)
        val other = FoldingFeature(otherRect, type, state)
        assertNotEquals(original, other)
    }

    @Test
    public fun testEquals_differentType() {
        val rect = Bounds(1, 0, 1, 10)
        val originalType = FOLD
        val otherType = HINGE
        val state = FLAT
        val original = FoldingFeature(rect, originalType, state)
        val other = FoldingFeature(rect, otherType, state)
        assertNotEquals(original, other)
    }

    @Test
    public fun testEquals_differentState() {
        val rect = Bounds(1, 0, 1, 10)
        val type = FOLD
        val originalState = FLAT
        val otherState = HALF_OPENED
        val original = FoldingFeature(rect, type, originalState)
        val other = FoldingFeature(rect, type, otherState)
        assertNotEquals(original, other)
    }

    @Test
    public fun testHashCode_matchesIfEqual() {
        val originalRect = Bounds(1, 0, 1, 10)
        val matchingRect = Bounds(1, 0, 1, 10)
        val type = FOLD
        val state = FLAT
        val original = FoldingFeature(originalRect, type, state)
        val matching = FoldingFeature(matchingRect, type, state)
        assertEquals(original, matching)
        assertEquals(original.hashCode().toLong(), matching.hashCode().toLong())
    }

    @Test
    public fun testIsSeparating_trueForHinge() {
        val bounds = Bounds(1, 0, 1, 10)
        for (feature in allFoldStates(bounds, HINGE)) {
            assertTrue(feature.isSeparating)
        }
    }

    @Test
    public fun testIsSeparating_falseForFlatFold() {
        val bounds = Bounds(1, 0, 1, 10)
        val feature = FoldingFeature(bounds, FOLD, FLAT)
        assertFalse(feature.isSeparating)
    }

    @Test
    public fun testIsSeparating_trueForNotFlatFold() {
        val bounds = Bounds(1, 0, 1, 10)
        val nonFlatFeatures = mutableListOf<FoldingFeature>()
        for (feature in allFoldStates(bounds, FOLD)) {
            if (feature.state != FLAT) {
                nonFlatFeatures.add(feature)
            }
        }
        for (feature in nonFlatFeatures) {
            assertTrue(feature.isSeparating)
        }
    }

    @Test
    public fun testOcclusionTypeNone_emptyFeature() {
        val bounds = Bounds(0, 100, 100, 100)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(OcclusionType.NONE, feature.occlusionType)
        }
    }

    @Test
    public fun testOcclusionTypeFull_nonEmptyHingeFeature() {
        val bounds = Bounds(0, 100, 100, 101)
        for (feature in allFoldStates(bounds, HINGE)) {
            assertEquals(OcclusionType.FULL, feature.occlusionType)
        }
    }

    @Test
    public fun testGetFeatureOrientation_isHorizontalWhenWidthIsGreaterThanHeight() {
        val bounds = Bounds(0, 100, 200, 100)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(Orientation.HORIZONTAL, feature.orientation)
        }
    }

    @Test
    public fun testGetFeatureOrientation_isVerticalWhenHeightIsGreaterThanWidth() {
        val bounds = Bounds(100, 0, 100, 200)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(Orientation.VERTICAL, feature.orientation)
        }
    }

    @Test
    public fun testGetFeatureOrientation_isVerticalWhenHeightIsEqualToWidth() {
        val bounds = Bounds(0, 0, 100, 100)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(Orientation.VERTICAL, feature.orientation)
        }
    }
}
