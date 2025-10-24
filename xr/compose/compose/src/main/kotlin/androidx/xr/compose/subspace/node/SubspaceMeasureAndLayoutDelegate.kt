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

import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.xr.compose.platform.Logger
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose

internal class SubspaceMeasureAndLayoutDelegate(private val root: SubspaceLayoutNode) {
    var rootVolumeConstraints: VolumeConstraints = VolumeConstraints()
        set(value) {
            if (field != value) {
                field = value
                if (root.isAttached) {
                    requestMeasure(root)
                }
            }
        }

    val snapshotStateObserver: SnapshotStateObserver = SnapshotStateObserver(::run)

    private val onCommitAffectingMeasure: (SubspaceLayoutNode) -> Unit = { layoutNode ->
        layoutNode.requestMeasure()
    }

    private val onCommitAffectingLayout: (SubspaceLayoutNode) -> Unit = { layoutNode ->
        layoutNode.requestLayout()
    }

    /**
     * Whether a layout request has been made. If a layout request is made while a layout is in
     * progress, the new request will be handled after the current layout is complete.
     */
    private var isLayoutRequested = false

    /**
     * Tracks whether a layout is currently in progress to avoid recursively triggering a layout.
     */
    private var isLayoutInProgress = false

    /**
     * Requests remeasure for this [node] and nodes affected by its measure result.
     *
     * @return true if the [measureAndLayout] execution should be scheduled as a result of the
     *   request.
     */
    fun requestMeasure(node: SubspaceLayoutNode, forceRequest: Boolean = false): Boolean {
        return true
    }

    /**
     * Requests relayout for this [node] and nodes affected by its position.
     *
     * @return true if the [measureAndLayout] execution should be scheduled as a result of the
     *   request.
     */
    fun requestLayout(node: SubspaceLayoutNode, forceRequest: Boolean = false): Boolean {
        return true
    }

    /**
     * Iterates through all SubspaceLayoutNodes that have requested layout and measures and lays
     * them out.
     */
    fun measureAndLayout() {
        if (isLayoutInProgress) {
            isLayoutRequested = true
            return
        }

        isLayoutRequested = false
        isLayoutInProgress = true

        snapshotStateObserver.observeReads(root, onCommitAffectingMeasure) {
            root.measurableLayout.measure(rootVolumeConstraints)
        }

        snapshotStateObserver.observeReads(root, onCommitAffectingLayout) {
            root.measurableLayout.placeAt(Pose.Identity)
        }

        Logger.log("AndroidComposeSpatialElement") { root.debugTreeToString() }
        Logger.log("AndroidComposeSpatialElement") { root.debugEntityTreeToString() }

        isLayoutInProgress = false
        if (isLayoutRequested) {
            measureAndLayout()
        }
    }
}
