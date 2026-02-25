/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.xr.compose.subspace.node

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.subspace.layout.ParentLayoutParamsAdjustable
import androidx.xr.compose.subspace.layout.ParentLayoutParamsModifier
import androidx.xr.compose.subspace.layout.SubspaceLayoutCoordinates
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceMeasureScope
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.unit.VolumeConstraints
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceNodeKind] */
@RunWith(AndroidJUnit4::class)
class SubspaceNodeKindTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun subspaceNodeKind_individualMasksAreUniquePowersOfTwo() {
        val allKinds =
            listOf(
                SubspaceNodes.Any,
                SubspaceNodes.Layout,
                SubspaceNodes.CoreEntity,
                SubspaceNodes.ParentData,
                SubspaceNodes.LayoutAware,
                SubspaceNodes.Locals,
            )
        // Ensure no two masks collide (0b1, 0b10, 0b100, etc.)
        val masks = allKinds.map { it.mask }
        assertEquals(masks.size, masks.toSet().size)
    }

    @Test
    fun subspaceNodeKind_orOperator_correctlyCombinesMasks() {
        val mask = SubspaceNodes.Layout or SubspaceNodes.CoreEntity
        val expected = SubspaceNodes.Layout.mask or SubspaceNodes.CoreEntity.mask
        assertEquals(expected, mask)
    }

    @Test
    fun testCalculateKindSet_simpleNode() {
        val node = SimpleModifierNode()
        val kindSet = calculateSubspaceNodeKindSetFrom(node)
        val expected = SubspaceNodes.Any.mask
        assertEquals(expected, kindSet)
        assertTrue(SubspaceNodes.Any in kindSet)
        assertTrue(SubspaceNodes.Layout !in kindSet)
    }

    @Test
    fun testCalculateKindSet_layoutNode() {
        val node = LayingOutModifier()
        val kindSet = calculateSubspaceNodeKindSetFrom(node)
        // Expected: Any + Layout
        val expected = SubspaceNodes.Any or SubspaceNodes.Layout
        assertTrue(SubspaceNodes.Layout in kindSet)
        assertTrue(SubspaceNodes.CoreEntity !in kindSet)
        assertEquals(expected, kindSet)
    }

    @Test
    fun testCalculateKindSet_layoutCoordinatesAwareModifierNode() {
        val node = CoordinatesAwareModifier()
        val kindSet = calculateSubspaceNodeKindSetFrom(node)
        // Expected: Any + LayoutCoordinates
        val expected = SubspaceNodes.Any or SubspaceNodes.LayoutAware
        assertTrue(SubspaceNodes.LayoutAware in kindSet)
        assertTrue(SubspaceNodes.Locals !in kindSet)
        assertEquals(expected, kindSet)
    }

    @Test
    fun testCalculateKindSet_compositionLocalConsumerSubspaceModifierNode() {
        val node = LocalConsumerSubspaceModifier()
        val kindSet = calculateSubspaceNodeKindSetFrom(node)
        // Expected: Any + CompositionLocalConsumerSubspaceModifierNode
        val expected = SubspaceNodes.Any or SubspaceNodes.Locals
        assertTrue(SubspaceNodes.Locals in kindSet)
        assertTrue(SubspaceNodes.LayoutAware !in kindSet)
        assertEquals(expected, kindSet)
    }

    @Test
    fun testCalculateKindSet_MultiKindNode_allKindsAggregated() {
        val node = MultiKindModifier() // Layout, ParentData
        val kindSet = calculateSubspaceNodeKindSetFrom(node)
        // Expected: Any + Layout + ParentData
        val expected = SubspaceNodes.Any or SubspaceNodes.Layout or SubspaceNodes.ParentData
        assertTrue(SubspaceNodes.Layout in kindSet)
        assertTrue(SubspaceNodes.ParentData in kindSet)
        assertTrue(SubspaceNodes.LayoutAware !in kindSet)
        assertEquals(expected, kindSet)
    }

    @Test
    fun testKindSetCaching_usesCacheOnSecondInstance() {
        val node1 = ParentLayoutModifier()
        val node2 = ParentLayoutModifier()
        // 1. Calculate node1 (Slow Path: performs 'is' checks & populates KClass cache)
        calculateSubspaceNodeKindSetFrom(node1)
        // Ensure node2 starts clean (simulate first access for the instance)
        node2.kindSet = 0
        // 2. Calculate node2 (Fast Path: hits the KClass map cache)
        val kindSet2 = calculateSubspaceNodeKindSetFrom(node2)
        val expected = SubspaceNodes.Any or SubspaceNodes.ParentData
        // Assert that the result is correct and the node's local cache is populated
        assertEquals(expected, kindSet2)
        assertEquals(expected, node2.kindSet)
    }
}

class LayingOutModifier() : SubspaceLayoutModifierNode, SubspaceModifier.Node() {
    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        return layout(0, 0, 0, {})
    }
}

class ParentLayoutModifier() : ParentLayoutParamsModifier, SubspaceModifier.Node() {
    override fun adjustParams(params: ParentLayoutParamsAdjustable) {}
}

class CoordinatesAwareModifier() : SubspaceLayoutAwareModifierNode, SubspaceModifier.Node() {
    override fun onPlaced(coordinates: SubspaceLayoutCoordinates) {}
}

class LocalConsumerSubspaceModifier() :
    CompositionLocalConsumerSubspaceModifierNode, SubspaceModifier.Node() {}

class SimpleModifierNode : SubspaceModifier.Node()

class MultiKindModifier :
    SubspaceLayoutModifierNode, ParentLayoutParamsModifier, SubspaceModifier.Node() {
    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        return layout(0, 0, 0, {})
    }

    override fun adjustParams(params: ParentLayoutParamsAdjustable) {}
}
