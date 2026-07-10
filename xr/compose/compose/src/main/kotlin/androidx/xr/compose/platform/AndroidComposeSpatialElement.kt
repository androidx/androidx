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

import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.compose.subspace.layout.CoreMainPanelEntity
import androidx.xr.compose.subspace.node.Logger
import androidx.xr.compose.subspace.node.SubspaceLayoutNode
import androidx.xr.compose.subspace.node.SubspaceMeasureAndLayoutDelegate
import androidx.xr.compose.subspace.node.SubspaceOwner
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
internal class AndroidComposeSpatialElement(
    override val coroutineContext: CoroutineContext = AndroidUiDispatcher.Main
) : SpatialElement(), SubspaceOwner, DefaultLifecycleObserver {
    override val root: SubspaceLayoutNode =
        SubspaceLayoutNode().apply { measurePolicy = SubspaceLayoutNode.RootMeasurePolicy }

    // For debug output set this to androidx.xr.compose.subspace.node.DebugLogger().
    override var logger: Logger? = null

    // This coroutine scope will launch tasks to the Choreographer on the main thread.
    private val coroutineScope = CoroutineScope(coroutineContext)

    internal var wrappedComposition: WrappedComposition? = null

    /**
     * Callback that is registered in [setOnSubspaceAvailable] to be executed when this element is
     * attached a subspace.
     */
    private var onSubspaceAvailable: ((LifecycleOwner) -> Unit)? = null

    private var windowLeashLayoutNode: SubspaceLayoutNode? = null

    private val measureAndLayoutDelegate = SubspaceMeasureAndLayoutDelegate(root)

    private var isMeasureAndLayoutScheduled: Boolean = false

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
        measureAndLayoutDelegate.snapshotStateObserver.start()
        onSubspaceAvailable?.invoke(spatialComposeScene)
        onSubspaceAvailable = null
    }

    override fun onDetachedFromSubspace(spatialComposeScene: SpatialComposeScene) {
        super.onDetachedFromSubspace(spatialComposeScene)

        spatialComposeScene.lifecycle.removeObserver(this)
        measureAndLayoutDelegate.snapshotStateObserver.stop()
        measureAndLayoutDelegate.snapshotStateObserver.clear()
    }

    override fun onAttach(node: SubspaceLayoutNode) {
        node.coreEntity?.let { entity ->
            if (entity is CoreMainPanelEntity) {
                windowLeashLayoutNode = node
            }
        }
    }

    override fun onDetach(node: SubspaceLayoutNode) {
        node.coreEntity?.let { entity ->
            if (entity is CoreMainPanelEntity && node == windowLeashLayoutNode) {
                windowLeashLayoutNode = null
            }
        }
    }

    override fun onRecompositionComplete() {
        if (!root.isPlaced) {
            measureAndLayoutDelegate.requestMeasure(root, true)
        }
        scheduleMeasureAndLayout()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        scheduleMeasureAndLayout()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        root.detach()
    }

    override fun requestMeasure(node: SubspaceLayoutNode, forceRequest: Boolean) {
        logger?.measureRequested(node)

        if (!root.isPlaced) return

        if (measureAndLayoutDelegate.requestMeasure(node, forceRequest)) {
            scheduleMeasureAndLayout()
        }
    }

    override fun requestLayout(node: SubspaceLayoutNode, forceRequest: Boolean) {
        logger?.layoutRequested(node)

        if (!root.isPlaced) return

        if (measureAndLayoutDelegate.requestLayout(node, forceRequest)) {
            scheduleMeasureAndLayout()
        }
    }

    override fun requestEntityUpdate(node: SubspaceLayoutNode, forceRequest: Boolean) {
        if (measureAndLayoutDelegate.requestEntityUpdate(node, forceRequest)) {
            scheduleMeasureAndLayout()
        }
    }

    // TODO: Consider adding stricter control over how this is called here, or at call sites, if it
    // becomes too easy to generate superfluous layouts.
    private fun scheduleMeasureAndLayout() {
        if (isMeasureAndLayoutScheduled) return

        isMeasureAndLayoutScheduled = true
        coroutineScope.launch {
            isMeasureAndLayoutScheduled = false
            measureAndLayoutDelegate.measureAndLayout()
        }
    }
}
