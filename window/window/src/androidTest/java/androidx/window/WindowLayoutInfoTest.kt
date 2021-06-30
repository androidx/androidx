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
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.FoldingFeature.Type.Companion.HINGE
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [WindowLayoutInfo] class.  */
@SmallTest
@RunWith(AndroidJUnit4::class)
public class WindowLayoutInfoTest {

    @Test
    public fun testBuilder_empty() {
        val builder = WindowLayoutInfo.Builder()
        val windowLayoutInfo = builder.build()
        assertThat(windowLayoutInfo.displayFeatures).isEmpty()
    }

    @Test
    public fun testBuilder_setDisplayFeatures() {
        val feature1: DisplayFeature = FoldingFeature(Bounds(1, 0, 3, 4), HINGE, FLAT)
        val feature2: DisplayFeature = FoldingFeature(Bounds(1, 0, 1, 4), HINGE, HALF_OPENED)
        val displayFeatures = listOf(feature1, feature2)
        val builder = WindowLayoutInfo.Builder()
        builder.setDisplayFeatures(displayFeatures)
        val windowLayoutInfo = builder.build()
        assertEquals(displayFeatures, windowLayoutInfo.displayFeatures)
    }

    @Test
    public fun testEquals_sameFeatures() {
        val features = listOf<DisplayFeature>()
        val original = WindowLayoutInfo(features)
        val copy = WindowLayoutInfo(features)
        assertEquals(original, copy)
    }

    @Test
    public fun testEquals_differentFeatures() {
        val originalFeatures = listOf<DisplayFeature>()
        val rect = Bounds(1, 0, 1, 10)
        val differentFeatures = listOf(FoldingFeature(rect, HINGE, FLAT))
        val original = WindowLayoutInfo(originalFeatures)
        val different = WindowLayoutInfo(differentFeatures)
        assertNotEquals(original, different)
    }

    @Test
    public fun testHashCode_matchesIfEqual() {
        val firstFeatures = emptyList<DisplayFeature>()
        val secondFeatures = emptyList<DisplayFeature>()
        val first = WindowLayoutInfo(firstFeatures)
        val second = WindowLayoutInfo(secondFeatures)
        assertEquals(first, second)
        assertEquals(first.hashCode().toLong(), second.hashCode().toLong())
    }

    @Test
    public fun testHashCode_matchesIfEqualFeatures() {
        val originalFeature: DisplayFeature = FoldingFeature(Bounds(0, 0, 100, 0), HINGE, FLAT)
        val matchingFeature: DisplayFeature = FoldingFeature(Bounds(0, 0, 100, 0), HINGE, FLAT)
        val firstFeatures = listOf(originalFeature)
        val secondFeatures = listOf(matchingFeature)
        val first = WindowLayoutInfo(firstFeatures)
        val second = WindowLayoutInfo(secondFeatures)
        assertEquals(first, second)
        assertEquals(first.hashCode().toLong(), second.hashCode().toLong())
    }
}
