/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

/**
 * A holder of the measured bounds for the layout (MeasureBox).
 */
// TODO(Andrey): Add Matrix transformation here when we would have this logic.
interface LayoutCoordinates {

    /**
     * The position within the parent of this layout.
     */
    val position: PxPosition

    /**
     * The size of this layout in the local coordinates space.
     */
    val size: PxSize

    /**
     * Converts a global position into a local position within this layout.
     */
    fun globalToLocal(global: PxPosition): PxPosition

    /**
     * Converts a local position within this layout into a global one.
     */
    fun localToGlobal(local: PxPosition): PxPosition

    /**
     * Converts a child layout position into a local position within this layout.
     */
    fun childToLocal(child: LayoutCoordinates, childLocal: PxPosition): PxPosition

    /**
     * Returns a coordinates of the parent layout. If there is no parent returns null.
     */
    // TODO(Andrey): It could work a bit wrong right now, as we create this object at the moment
    // of placing the current MeasureBox. Which means the parent MeasureBox is not yet placed.
    // So x and y positions could be changed after it, so if we call getParentCoordinates
    // right when we receive an object it could have outdated x and y values.
    // Will work if we use it quite later, for example after a tap.
    // We need to figure out how to solve it.
    fun getParentCoordinates(): LayoutCoordinates?
}

/**
 * A LayoutCoordinates implementation based on LayoutNode.
 */
internal class LayoutNodeCoordinates(
    private val layoutNode: LayoutNode
) : LayoutCoordinates {

    override val position get() = PxPosition(layoutNode.x, layoutNode.y)

    override val size get() = PxSize(layoutNode.width, layoutNode.height)

    override fun globalToLocal(global: PxPosition) = layoutNode.globalToLocal(global)

    override fun localToGlobal(local: PxPosition) = layoutNode.localToGlobal(local)

    override fun childToLocal(child: LayoutCoordinates, childLocal: PxPosition): PxPosition {
        if (child !is LayoutNodeCoordinates) {
            throw IllegalArgumentException("Incorrect child provided.")
        }
        return layoutNode.childToLocal(child.layoutNode, childLocal)
    }

    override fun getParentCoordinates(): LayoutCoordinates? {
        val parent = layoutNode.parentLayoutNode
        return if (parent != null) LayoutNodeCoordinates(parent) else null
    }
}
