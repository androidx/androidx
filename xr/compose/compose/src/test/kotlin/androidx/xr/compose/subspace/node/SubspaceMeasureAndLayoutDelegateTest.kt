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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.AndroidComposeSpatialElement
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceMeasureAndLayoutDelegate]. */
@RunWith(AndroidJUnit4::class)
class SubspaceMeasureAndLayoutDelegateTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun requestMeasure_alwaysReturnsTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root

        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)

        assertTrue(delegate.requestMeasure(rootNode))
    }

    @Test
    fun requestMeasure_changesMeasurePendingFlagToTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)
        rootNode.insertAt(0, parentNode)
        parentNode.insertAt(0, childNode)

        delegate.requestMeasure(rootNode)
        delegate.requestMeasure(parentNode)
        delegate.requestMeasure(childNode)

        assertThat(rootNode.measurePending).isTrue()
        assertThat(parentNode.measurePending).isTrue()
        assertThat(childNode.measurePending).isTrue()
    }

    @Test
    fun requestMeasure_measurePendingTrueWillReturnFalse() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)

        rootNode.measurePending = true

        assertThat(delegate.requestMeasure(rootNode)).isFalse()
    }

    @Test
    fun requestMeasure_layoutPendingWillReturnTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)

        delegate.requestMeasure(rootNode)

        assertThat(rootNode.layoutPending).isTrue()
    }

    @Test
    fun requestLayout_alwaysReturnsTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root

        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)

        assertTrue(delegate.requestLayout(rootNode))
    }

    @Test
    fun requestLayout_changesLayoutPendingFlagToTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)
        rootNode.insertAt(0, parentNode)
        parentNode.insertAt(0, childNode)

        delegate.requestLayout(rootNode)
        delegate.requestLayout(parentNode)
        delegate.requestLayout(childNode)

        assertThat(rootNode.layoutPending).isTrue()
        assertThat(parentNode.layoutPending).isTrue()
        assertThat(childNode.layoutPending).isTrue()
    }

    @Test
    fun requestLayout_layoutPendingTrueWillReturnFalse() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)

        rootNode.layoutPending = true

        assertThat(delegate.requestLayout(rootNode)).isFalse()
    }

    @Test
    fun requestLayout_doesNotChangeMeasurePendingFlag() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)

        rootNode.measurePending = false
        delegate.requestLayout(rootNode)

        assertThat(rootNode.measurePending).isFalse()
    }

    @Test
    fun measureAndLayout_childSizeChanges_parentRemeasures() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val parent = SubspaceLayoutNode()
        val child = SubspaceLayoutNode()

        root.insertAt(0, parent)
        parent.insertAt(0, child)

        var childWidth = 1
        val childMeasurePolicy = SubspaceMeasurePolicy { _, _ -> layout(childWidth, 1, 1) {} }
        child.measurePolicy = childMeasurePolicy

        var parentRemeasures = 0
        parent.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            parentRemeasures++
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }
        root.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }
        child.measurePolicy = SubspaceMeasurePolicy { _, _ -> layout(childWidth, 0, 0) {} }

        delegate.requestMeasure(root)
        delegate.measureAndLayout()

        assertThat(parentRemeasures).isEqualTo(1)
        assertThat(parent.measurableLayout.measuredWidth).isEqualTo(1)

        // No change, so no remeasure
        delegate.requestMeasure(child)
        delegate.measureAndLayout()
        assertThat(parentRemeasures).isEqualTo(1)
        assertThat(parent.measurableLayout.measuredWidth).isEqualTo(1)
        assertThat(child.measurableLayout.measuredWidth).isEqualTo(1)

        // Change child size
        childWidth = 2
        delegate.requestMeasure(child)
        delegate.measureAndLayout()

        // The parent remeasure count is 3. The first is from the initial layout. The second is a
        // direct request because the child changed size. The third happens because the parent's
        // size change triggers a remeasure of the root, which then remeasures the parent again.
        assertThat(parentRemeasures).isEqualTo(3)
        assertThat(parent.measurableLayout.measuredWidth).isEqualTo(2)
        assertThat(child.measurableLayout.measuredWidth).isEqualTo(2)
    }

    @Test
    fun measureAndLayout_childPositionChanges_parentRelayouts() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val parent = SubspaceLayoutNode()
        val child = SubspaceLayoutNode()

        root.insertAt(0, parent)
        parent.insertAt(0, child)

        var childX = 0f
        val parentMeasurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(10, 10, 10) {
                placeables.forEach {
                    it.place(
                        Pose(translation = Vector3(childX, 0f, 0f), rotation = Quaternion.Identity)
                    )
                }
            }
        }
        parent.measurePolicy = parentMeasurePolicy
        root.measurePolicy = parentMeasurePolicy
        child.measurePolicy = parentMeasurePolicy

        delegate.requestMeasure(root)
        delegate.measureAndLayout()

        assertThat(child.measurableLayout.pose.translation.x).isEqualTo(0f)

        // Request layout on parent, but position doesn't change
        delegate.requestLayout(parent)
        delegate.measureAndLayout()
        assertThat(child.measurableLayout.pose.translation.x).isEqualTo(0f)

        // Change child position
        childX = 1f
        delegate.requestLayout(parent)
        delegate.measureAndLayout()

        assertThat(child.measurableLayout.pose.translation.x).isEqualTo(1f)
    }

    @Test
    fun requestLayout_noPendingRequests_returnsTrue() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        assertTrue(delegate.requestLayout(root))
    }

    @Test
    fun requestLayout_ancestorAndDescendantRequested_descendantLaidOutOnce() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val parent = SubspaceLayoutNode()
        val child = SubspaceLayoutNode()

        root.insertAt(0, parent)
        parent.insertAt(0, child)

        var parentLayoutCount = 0
        parent.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(10, 10, 10) {
                parentLayoutCount++
                placeables.forEach { it.place(Pose.Identity) }
            }
        }

        var childLayoutCount = 0
        child.measurePolicy = SubspaceMeasurePolicy { _, _ ->
            layout(1, 1, 1) { childLayoutCount++ }
        }

        root.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(10, 10, 10) { placeables.forEach { it.place(Pose.Identity) } }
        }

        // --- Initial Layout ---
        delegate.requestMeasure(root)
        delegate.measureAndLayout()

        // Assert initial state
        assertThat(parentLayoutCount).isEqualTo(1)
        assertThat(childLayoutCount).isEqualTo(1)

        // --- Request Relayout on Ancestor and Descendant ---
        delegate.requestLayout(parent)
        delegate.requestLayout(child)
        delegate.measureAndLayout()

        // --- Assert Correct Behavior ---
        // The parent's relayout will trigger a relayout of the child, so the explicit
        // request on the child should be ignored.
        assertThat(parentLayoutCount).isEqualTo(2)
        assertThat(childLayoutCount).isEqualTo(2)
    }

    @Test
    fun requestMeasure_noPendingRequests_returnsTrue() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)

        assertTrue(delegate.requestMeasure(root))
    }

    @Test
    fun requestMeasure_alreadyRequested_isNotDropped() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val child1 = SubspaceLayoutNode()
        val child2 = SubspaceLayoutNode()
        root.insertAt(0, child1)
        root.insertAt(1, child2)

        var child1Remeasures = 0
        child1.measurePolicy = SubspaceMeasurePolicy { _, _ ->
            child1Remeasures++
            layout(1, 1, 1) {}
        }

        var child2Remeasures = 0
        child2.measurePolicy = SubspaceMeasurePolicy { _, _ ->
            child2Remeasures++
            layout(1, 1, 1) {}
        }

        root.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(1, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        delegate.requestMeasure(root)
        delegate.measureAndLayout()

        assertThat(child1Remeasures).isEqualTo(1)
        assertThat(child2Remeasures).isEqualTo(1)

        delegate.requestMeasure(child1)
        delegate.requestMeasure(child2)
        delegate.measureAndLayout()

        assertThat(child1Remeasures).isEqualTo(2)
        assertThat(child2Remeasures).isEqualTo(2)
    }

    @Test
    fun requestMeasure_ancestorAndDescendantRequested_descendantMeasuredOnce() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val parent = SubspaceLayoutNode()
        val child = SubspaceLayoutNode()

        root.insertAt(0, parent)
        parent.insertAt(0, child)

        var parentMeasureCount = 0
        parent.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            parentMeasureCount++
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        var childMeasureCount = 0
        child.measurePolicy = SubspaceMeasurePolicy { _, _ ->
            childMeasureCount++
            layout(1, 1, 1) {}
        }

        root.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        // Request measure for both parent and child
        delegate.requestMeasure(root)
        delegate.requestMeasure(parent)
        delegate.requestMeasure(child)
        delegate.measureAndLayout()

        // The child should only be measured once, as the parent's measure will trigger it.
        assertThat(childMeasureCount).isEqualTo(1)
        // The parent should also be measured once.
        assertThat(parentMeasureCount).isEqualTo(1)
    }

    @Test
    fun requestMeasure_ancestorAndDescendantWithIntermediateNodeRequested_descendantMeasuredOnce() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val ancestor = SubspaceLayoutNode()
        val intermediate = SubspaceLayoutNode()
        val descendant = SubspaceLayoutNode()

        root.insertAt(0, ancestor)
        ancestor.insertAt(0, intermediate)
        intermediate.insertAt(0, descendant)

        var ancestorMeasureCount = 0
        ancestor.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            ancestorMeasureCount++
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        var intermediateMeasureCount = 0
        intermediate.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            intermediateMeasureCount++
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        var descendantMeasureCount = 0
        descendant.measurePolicy = SubspaceMeasurePolicy { _, _ ->
            descendantMeasureCount++
            layout(1, 1, 1) {}
        }

        root.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        // Request measure for both ancestor and descendant
        delegate.requestMeasure(root)
        delegate.requestMeasure(ancestor)
        delegate.requestMeasure(descendant)
        delegate.measureAndLayout()

        // The descendant should only be measured once, as the ancestor's measure will trigger it.
        assertThat(descendantMeasureCount).isEqualTo(1)
        // The intermediate node should also be measured once.
        assertThat(intermediateMeasureCount).isEqualTo(1)
        // The ancestor should also be measured once.
        assertThat(ancestorMeasureCount).isEqualTo(1)
    }

    @Test
    fun requestMeasure_descendantThenAncestorRequested_descendantMeasuredOnce() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val ancestor = SubspaceLayoutNode()
        val intermediate = SubspaceLayoutNode()
        val descendant = SubspaceLayoutNode()

        root.insertAt(0, ancestor)
        ancestor.insertAt(0, intermediate)
        intermediate.insertAt(0, descendant)

        var ancestorMeasureCount = 0
        ancestor.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            ancestorMeasureCount++
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        var intermediateMeasureCount = 0
        intermediate.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            intermediateMeasureCount++
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        var descendantMeasureCount = 0
        descendant.measurePolicy = SubspaceMeasurePolicy { _, _ ->
            descendantMeasureCount++
            layout(1, 1, 1) {}
        }

        root.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        // --- Initial Layout ---
        delegate.requestMeasure(root)
        delegate.measureAndLayout()

        // Assert initial state
        assertThat(ancestorMeasureCount).isEqualTo(1)
        assertThat(intermediateMeasureCount).isEqualTo(1)
        assertThat(descendantMeasureCount).isEqualTo(1)

        // --- Request measure on descendant first, then ancestor ---
        delegate.requestMeasure(descendant)
        delegate.requestMeasure(ancestor)
        delegate.measureAndLayout()

        // --- Assert Correct Behavior ---
        // The descendant should only be measured once more, as part of the ancestor's
        // measure pass. The explicit request for the descendant should be removed.
        assertThat(descendantMeasureCount).isEqualTo(2)
        assertThat(intermediateMeasureCount).isEqualTo(2)
        assertThat(ancestorMeasureCount).isEqualTo(2)
    }

    @Test
    fun requestMeasure_descendantThenAncestorRequested_descendantMeasuredOnceMore() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val ancestor = SubspaceLayoutNode()
        val intermediate = SubspaceLayoutNode()
        val descendant = SubspaceLayoutNode()

        root.insertAt(0, ancestor)
        ancestor.insertAt(0, intermediate)
        intermediate.insertAt(0, descendant)

        var ancestorMeasureCount = 0
        ancestor.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            ancestorMeasureCount++
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        var intermediateMeasureCount = 0
        intermediate.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            intermediateMeasureCount++
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        var descendantMeasureCount = 0
        descendant.measurePolicy = SubspaceMeasurePolicy { _, _ ->
            descendantMeasureCount++
            layout(1, 1, 1) {}
        }

        root.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxOfOrNull { it.measuredWidth } ?: 0
            layout(width, 1, 1) { placeables.forEach { it.place(Pose.Identity) } }
        }

        // --- Initial Layout ---
        delegate.requestMeasure(root)
        delegate.measureAndLayout()

        // Assert initial state
        assertThat(ancestorMeasureCount).isEqualTo(1)
        assertThat(intermediateMeasureCount).isEqualTo(1)
        assertThat(descendantMeasureCount).isEqualTo(1)

        // --- Request measure on descendant first, then ancestor ---
        delegate.requestMeasure(descendant)
        delegate.requestMeasure(ancestor)
        delegate.measureAndLayout()

        // --- Assert Correct Behavior ---
        // The descendant should only be measured once more, as part of the ancestor's
        // measure pass. The explicit request for the descendant should be removed.
        // With the bug, descendantMeasureCount will be 3, causing this to fail.
        assertThat(descendantMeasureCount).isEqualTo(2)
        assertThat(intermediateMeasureCount).isEqualTo(2)
        assertThat(ancestorMeasureCount).isEqualTo(2)
    }

    @Test
    fun requestMeasure_ancestorThenDescendantRequested_descendantMeasuredOnce() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val ancestor = SubspaceLayoutNode()
        val intermediate = SubspaceLayoutNode()
        val descendant = SubspaceLayoutNode()

        root.insertAt(0, ancestor)
        ancestor.insertAt(0, intermediate)
        intermediate.insertAt(0, descendant)

        var descendantMeasureCount = 0
        descendant.measurePolicy = SubspaceMeasurePolicy { _, _ ->
            descendantMeasureCount++
            layout(1, 1, 1) {}
        }

        // Add minimal measure policies for other nodes
        val emptyPolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(
                placeables.maxOfOrNull { it.measuredWidth } ?: 0,
                placeables.maxOfOrNull { it.measuredHeight } ?: 0,
                placeables.maxOfOrNull { it.measuredDepth } ?: 0,
            ) {
                placeables.forEach { it.place(Pose.Identity) }
            }
        }
        root.measurePolicy = emptyPolicy
        ancestor.measurePolicy = emptyPolicy
        intermediate.measurePolicy = emptyPolicy

        // --- Initial Layout ---
        delegate.requestMeasure(root)
        delegate.measureAndLayout()
        assertThat(descendantMeasureCount).isEqualTo(1)

        // --- Request measure on ancestor first, then descendant ---
        delegate.requestMeasure(ancestor)
        delegate.requestMeasure(descendant)
        delegate.measureAndLayout()

        // --- Assert Correct Behavior ---
        // The descendant should only be measured once more as part of the ancestor's
        // measure pass. The explicit request for the descendant should be ignored.
        // With the current implementation, this will fail as descendantMeasureCount will be 3.
        assertThat(descendantMeasureCount).isEqualTo(2)
    }

    @Test
    fun measureAndLayout_reparentingPendingLayoutNode_doesNotCrash() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val intermediate = SubspaceLayoutNode()
        val child = SubspaceLayoutNode()

        // Set up the initial hierarchy: Root -> Child
        root.insertAt(0, child)

        child.measurePolicy = SubspaceMeasurePolicy { _, _ -> layout(1, 1, 1) {} }
        intermediate.measurePolicy = SubspaceMeasurePolicy { _, _ -> layout(1, 1, 1) {} }

        // Flag to control the execution of the side-effect
        var shouldReparent = false
        var hasReparented = false

        // Configure the Root measure policy to simulate a side-effect that alters the hierarchy
        // during layout.
        root.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(100, 100, 100) {
                // Perform reparenting only when explicitly triggered.
                if (shouldReparent && !hasReparented) {
                    hasReparented = true

                    // Reparent the child: Root -> Intermediate -> Child.
                    // This increases the child's depth from 1 to 2.
                    root.removeAt(0, 1)
                    root.insertAt(0, intermediate)
                    intermediate.insertAt(0, child)
                }
                placeables.forEach { it.place(Pose.Identity) }
            }
        }

        // Initialize the layout state. The side-effect is disabled, ensuring the child remains at
        // Depth 1.
        delegate.requestMeasure(root)
        delegate.measureAndLayout()

        // --- Reproduction of the Crash Scenario ---

        // Enable the side-effect for the subsequent layout pass.
        shouldReparent = true

        // Invalidate the layout for both Root and Child.
        // This adds them to the processing queue with their current depths (Child at Depth 1).
        delegate.requestLayout(root)
        delegate.requestLayout(child)

        // Execute the measure and layout pass.
        // The Root is processed first, triggering the hierarchy change (Child -> Depth 2) while the
        // Child is still pending in the queue.
        // We explicitly capture the result to assert that no exception is thrown.
        val result = delegate.measureAndLayout()

        // Explicitly assert that the operation succeeded without throwing "invalid node depth".
        assertThat(result).isNotEqualTo("invalid node depth")

        // Verify that the hierarchy was updated correctly.
        assertThat(child.parent).isEqualTo(intermediate)
        assertThat(child.depth).isEqualTo(2)
    }

    @Test
    fun requestEntityUpdate_alwaysReturnsTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)

        assertTrue(delegate.requestEntityUpdate(rootNode))
    }

    @Test
    fun requestEntityUpdate_changesEntityUpdatePendingFlagToTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root
        val childNode = SubspaceLayoutNode()
        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)
        rootNode.insertAt(0, childNode)

        delegate.requestEntityUpdate(rootNode)
        delegate.requestEntityUpdate(childNode)

        assertThat(rootNode.entityUpdatePending).isTrue()
        assertThat(childNode.entityUpdatePending).isTrue()
    }

    @Test
    fun requestEntityUpdate_doesNotTriggerMeasureOrLayout() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(rootNode)

        rootNode.measurePending = false
        rootNode.layoutPending = false

        delegate.requestEntityUpdate(rootNode)

        assertThat(rootNode.measurePending).isFalse()
        assertThat(rootNode.layoutPending).isFalse()
        assertThat(rootNode.entityUpdatePending).isTrue()
    }

    @Test
    fun measureAndLayout_processesEntityUpdatesWithoutMeasuring() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val child = SubspaceLayoutNode()

        root.insertAt(0, child)

        var childMeasureCount = 0
        child.measurePolicy = SubspaceMeasurePolicy { _, _ ->
            childMeasureCount++
            layout(1, 1, 1) {}
        }

        root.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(10, 10, 10) { placeables.forEach { it.place(Pose.Identity) } }
        }

        delegate.requestMeasure(root)
        delegate.measureAndLayout()

        assertThat(childMeasureCount).isEqualTo(1)

        child.entityUpdatePending = false

        delegate.requestEntityUpdate(child)
        assertThat(child.entityUpdatePending).isTrue()

        delegate.measureAndLayout()

        assertThat(child.entityUpdatePending).isFalse()
        assertThat(childMeasureCount).isEqualTo(1)
    }

    @Test
    fun requestEntityUpdate_whileMeasuringOrLayingOut_queuesButReturnsFalse() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val delegate = SubspaceMeasureAndLayoutDelegate(root)
        val child = SubspaceLayoutNode()

        root.insertAt(0, child)

        var requestResultDuringMeasure = true
        var pendingFlagDuringMeasure = false

        child.measurePolicy = SubspaceMeasurePolicy { _, _ ->
            // Request an update while the layout pass is actively running
            requestResultDuringMeasure = delegate.requestEntityUpdate(child)
            pendingFlagDuringMeasure = child.entityUpdatePending
            layout(1, 1, 1) {}
        }

        root.measurePolicy = SubspaceMeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(10, 10, 10) { placeables.forEach { it.place(Pose.Identity) } }
        }

        delegate.requestMeasure(root)
        delegate.measureAndLayout()

        assertThat(requestResultDuringMeasure).isFalse()

        assertThat(pendingFlagDuringMeasure).isTrue()
    }
}
