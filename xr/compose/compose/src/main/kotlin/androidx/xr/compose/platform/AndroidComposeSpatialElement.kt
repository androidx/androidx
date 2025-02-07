/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.platform

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.compose.subspace.node.SubspaceLayoutNode
import androidx.xr.compose.subspace.node.SubspaceOwner
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.PanelEntity

/**
 * An implementation of the [SubspaceOwner] interface, bridging the Compose layout and rendering
 * phases.
 *
 * Compose decouples layout computation from rendering. This necessitates two distinct trees that
 * [SubspaceOwner] defines and owns:
 * 1. The layout tree: Created by [SubspaceLayoutNode], this represents the app's defined hierarchy
 *    and is used for layout calculations.
 * 2. The rendering tree: A flat tree rooted at the [SubspaceOwner]. This is used for rendering. The
 *    hierarchy is necessary to ensure [SpatialElement] instances are visible on a screen (detached
 *    [SpatialElement] instances are always hidden). The positions of [SpatialElement] are updated
 *    post-layout computation, during the placement phase.
 *
 * This class draws inspiration from the [androidx/compose/ui/platform/AndroidComposeView].
 */
internal class AndroidComposeSpatialElement :
    SpatialElement(), SubspaceOwner, DefaultLifecycleObserver {
    override val root: SubspaceLayoutNode = SubspaceLayoutNode()

    internal var wrappedComposition: WrappedComposition? = null

    /**
     * Callback that is registered in [setOnSubspaceAvailable] to be executed when this element is
     * attached a subspace.
     */
    private var onSubspaceAvailable: ((LifecycleOwner) -> Unit)? = null

    private var windowLeashLayoutNode: SubspaceLayoutNode? = null

    /**
     * Whether a layout request has been made. If a layout request is made while a layout is in
     * progress, the new request will be handled after the current layout is complete.
     */
    private var isLayoutRequested = false

    /**
     * Tracks whether a layout is currently in progress to avoid recursively triggering a layout.
     */
    private var isLayoutInProgress = false

    init {
        root.attach(this)
    }

    /**
     * Registers the [callback] to be executed when this element is attached to a
     * [spatialComposeScene].
     *
     * Note that the [callback] will be invoked immediately if [spatialComposeScene] is already
     * available.
     */
    internal fun setOnSubspaceAvailable(callback: (LifecycleOwner) -> Unit) {
        if (spatialComposeScene != null) {
            callback(spatialComposeScene!!)
        } else {
            onSubspaceAvailable = callback
        }
    }

    override fun onAttachedToSubspace(spatialComposeScene: SpatialComposeScene) {
        super.onAttachedToSubspace(spatialComposeScene)

        spatialComposeScene.lifecycle.addObserver(this)
        onSubspaceAvailable?.invoke(spatialComposeScene)
        onSubspaceAvailable = null
    }

    override fun onDetachedFromSubspace(spatialComposeScene: SpatialComposeScene) {
        super.onDetachedFromSubspace(spatialComposeScene)

        spatialComposeScene.lifecycle.removeObserver(this)
    }

    override fun onAttach(node: SubspaceLayoutNode) {
        node.coreEntity?.entity.let { entity ->
            if (entity is PanelEntity && entity.isMainPanelEntity) {
                check(windowLeashLayoutNode == null) {
                    "Cannot add $node as there is already another SubspaceLayoutNode for the Window Leash Node"
                }
                windowLeashLayoutNode = node
            }
        }
    }

    override fun onDetach(node: SubspaceLayoutNode) {
        node.coreEntity?.entity.let { entity ->
            if (entity is PanelEntity && entity.isMainPanelEntity) {
                windowLeashLayoutNode = null
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        // TODO: "Refresh the layout hierarchy." <- Can we just call refreshLayout() here?
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        root.detach()
    }

    // TODO: Consider adding stricter control over how this is called here, or at call sites, if it
    // becomes too easy to generate superfluous layouts.
    override fun requestRelayout() {
        refreshLayout()
    }

    // TODO: Add unit tests.
    private fun refreshLayout() {
        if (isLayoutInProgress) {
            isLayoutRequested = true
            return
        }

        isLayoutRequested = false
        isLayoutInProgress = true

        val measureResults =
            root.measurableLayout.measure(
                VolumeConstraints(0, VolumeConstraints.INFINITY, 0, VolumeConstraints.INFINITY)
            )

        (measureResults as SubspaceLayoutNode.MeasurableLayout).placeAt(Pose.Identity)

        Logger.log("AndroidComposeSpatialElement") { root.debugTreeToString() }
        Logger.log("AndroidComposeSpatialElement") { root.debugEntityTreeToString() }

        isLayoutInProgress = false
        if (isLayoutRequested) {
            refreshLayout()
        }
    }
}
