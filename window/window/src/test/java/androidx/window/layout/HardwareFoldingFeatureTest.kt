/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.layout

import androidx.window.core.Bounds
import androidx.window.layout.TestFoldingFeatureUtil.allFoldStates
import androidx.window.layout.TestFoldingFeatureUtil.allFoldingFeatureTypeAndStates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [HardwareFoldingFeature] that run on the JVM to verify the logic.
 */
class HardwareFoldingFeatureTest {

    @Test(expected = IllegalArgumentException::class)
    fun tesEmptyRect() {
        HardwareFoldingFeature(
            Bounds(0, 0, 0, 0),
            HardwareFoldingFeature.Type.HINGE,
            FoldingFeature.State.HALF_OPENED
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testHorizontalHingeWithNonZeroOrigin() {
        HardwareFoldingFeature(
            Bounds(1, 10, 20, 10),
            HardwareFoldingFeature.Type.HINGE,
            FoldingFeature.State.HALF_OPENED
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVerticalHingeWithNonZeroOrigin() {
        HardwareFoldingFeature(
            Bounds(10, 1, 19, 29),
            HardwareFoldingFeature.Type.HINGE,
            FoldingFeature.State.HALF_OPENED
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testHorizontalFoldWithNonZeroOrigin() {
        HardwareFoldingFeature(
            Bounds(1, 10, 20, 10),
            HardwareFoldingFeature.Type.FOLD,
            FoldingFeature.State.HALF_OPENED
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVerticalFoldWithNonZeroOrigin() {
        HardwareFoldingFeature(
            Bounds(10, 1, 10, 20),
            HardwareFoldingFeature.Type.FOLD,
            FoldingFeature.State.HALF_OPENED
        )
    }

    @Test
    fun testEquals_sameAttributes() {
        val bounds = Bounds(1, 0, 1, 10)
        val type = HardwareFoldingFeature.Type.FOLD
        val state = FoldingFeature.State.FLAT
        val original = HardwareFoldingFeature(bounds, type, state)
        val copy = HardwareFoldingFeature(bounds, type, state)
        assertEquals(original, copy)
    }

    @Test
    fun testEquals_differentRect() {
        val originalRect = Bounds(1, 0, 1, 10)
        val otherRect = Bounds(2, 0, 2, 10)
        val type = HardwareFoldingFeature.Type.FOLD
        val state = FoldingFeature.State.FLAT
        val original = HardwareFoldingFeature(originalRect, type, state)
        val other = HardwareFoldingFeature(otherRect, type, state)
        assertNotEquals(original, other)
    }

    @Test
    fun testEquals_differentType() {
        val rect = Bounds(1, 0, 1, 10)
        val originalType = HardwareFoldingFeature.Type.FOLD
        val otherType = HardwareFoldingFeature.Type.HINGE
        val state = FoldingFeature.State.FLAT
        val original = HardwareFoldingFeature(rect, originalType, state)
        val other = HardwareFoldingFeature(rect, otherType, state)
        assertNotEquals(original, other)
    }

    @Test
    fun testEquals_differentState() {
        val rect = Bounds(1, 0, 1, 10)
        val type = HardwareFoldingFeature.Type.FOLD
        val originalState = FoldingFeature.State.FLAT
        val otherState = FoldingFeature.State.HALF_OPENED
        val original = HardwareFoldingFeature(rect, type, originalState)
        val other = HardwareFoldingFeature(rect, type, otherState)
        assertNotEquals(original, other)
    }

    @Test
    fun testHashCode_matchesIfEqual() {
        val originalRect = Bounds(1, 0, 1, 10)
        val matchingRect = Bounds(1, 0, 1, 10)
        val type = HardwareFoldingFeature.Type.FOLD
        val state = FoldingFeature.State.FLAT
        val original = HardwareFoldingFeature(originalRect, type, state)
        val matching = HardwareFoldingFeature(matchingRect, type, state)
        assertEquals(original, matching)
        assertEquals(original.hashCode().toLong(), matching.hashCode().toLong())
    }

    @Test
    fun testIsSeparating_trueForHinge() {
        val bounds = Bounds(1, 0, 1, 10)
        for (feature in allFoldStates(bounds, HardwareFoldingFeature.Type.HINGE)) {
            assertTrue(feature.isSeparating)
        }
    }

    @Test
    fun testIsSeparating_falseForFlatFold() {
        val bounds = Bounds(1, 0, 1, 10)
        val feature = HardwareFoldingFeature(bounds,
            HardwareFoldingFeature.Type.FOLD,
            FoldingFeature.State.FLAT
        )
        assertFalse(feature.isSeparating)
    }

    @Test
    fun testIsSeparating_trueForNotFlatFold() {
        val bounds = Bounds(1, 0, 1, 10)
        val nonFlatFeatures = mutableListOf<FoldingFeature>()
        for (feature in allFoldStates(bounds, HardwareFoldingFeature.Type.FOLD)) {
            if (feature.state != FoldingFeature.State.FLAT) {
                nonFlatFeatures.add(feature)
            }
        }
        for (feature in nonFlatFeatures) {
            assertTrue(feature.isSeparating)
        }
    }

    @Test
    fun testOcclusionTypeNone_emptyFeature() {
        val bounds = Bounds(0, 100, 100, 100)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(FoldingFeature.OcclusionType.NONE, feature.occlusionType)
        }
    }

    @Test
    fun testOcclusionTypeFull_nonEmptyHingeFeature() {
        val bounds = Bounds(0, 100, 100, 101)
        for (feature in allFoldStates(bounds, HardwareFoldingFeature.Type.HINGE)) {
            assertEquals(FoldingFeature.OcclusionType.FULL, feature.occlusionType)
        }
    }

    @Test
    fun testGetFeatureOrientation_isHorizontalWhenWidthIsGreaterThanHeight() {
        val bounds = Bounds(0, 100, 200, 100)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(FoldingFeature.Orientation.HORIZONTAL, feature.orientation)
        }
    }

    @Test
    fun testGetFeatureOrientation_isVerticalWhenHeightIsGreaterThanWidth() {
        val bounds = Bounds(100, 0, 100, 200)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(FoldingFeature.Orientation.VERTICAL, feature.orientation)
        }
    }

    @Test
    fun testGetFeatureOrientation_isVerticalWhenHeightIsEqualToWidth() {
        val bounds = Bounds(0, 0, 100, 100)
        for (feature in allFoldingFeatureTypeAndStates(bounds)) {
            assertEquals(FoldingFeature.Orientation.VERTICAL, feature.orientation)
        }
    }
}
