/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.compose.subspace.node

import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose

/**
 * Interface for debug/test logging internal tree manipulation of layout nodes and modifier nodes.
 */
internal interface Logger {
    fun nodeInserted(node: Any, parent: Any?, atIndex: Int)

    fun nodeMoved(node: Any, parent: Any?, fromIndex: Int, toIndex: Int)

    fun nodeUpdated(node: Any, parent: Any?, atIndex: Int)

    fun nodeRemoved(node: Any, parent: Any?, fromIndex: Int)

    fun measureRequested(node: Any)

    fun layoutRequested(node: Any)

    fun nodeMeasured(node: Any, constraints: VolumeConstraints, size: IntVolumeSize)

    fun nodePlaced(node: Any, pose: Pose)
}

/** Debug implementation of Logger that prints actions to stdout. */
internal class DebugLogger : Logger {
    override fun nodeInserted(node: Any, parent: Any?, atIndex: Int) {
        println("Inserted node $node into $parent at $atIndex")
    }

    override fun nodeMoved(node: Any, parent: Any?, fromIndex: Int, toIndex: Int) {
        println("Moved node $node in $parent from $fromIndex to $toIndex")
    }

    override fun nodeUpdated(node: Any, parent: Any?, atIndex: Int) {
        println("Updated node $node in $parent at $atIndex")
    }

    override fun nodeRemoved(node: Any, parent: Any?, fromIndex: Int) {
        println("Removed node $node in $parent from $fromIndex")
    }

    override fun measureRequested(node: Any) {
        println("Measurement requested for node $node")
    }

    override fun layoutRequested(node: Any) {
        println("Layout requested for node $node")
    }

    override fun nodeMeasured(node: Any, constraints: VolumeConstraints, size: IntVolumeSize) {
        println("Measured node $node with constraints $constraints to size $size")
    }

    override fun nodePlaced(node: Any, pose: Pose) {
        println("Placed node $node at pose $pose")
    }
}
