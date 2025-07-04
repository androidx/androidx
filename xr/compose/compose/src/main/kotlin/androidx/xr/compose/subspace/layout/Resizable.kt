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

package androidx.xr.compose.subspace.layout

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.node.CompositionLocalConsumerSubspaceModifierNode
import androidx.xr.compose.subspace.node.LayoutCoordinatesAwareModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.currentValueOf
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.toDimensionsInMeters
import androidx.xr.compose.unit.toIntVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeListener
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * When the resizable modifier is present and enabled, draggable UI controls will be shown that
 * allow the user to resize the element in 3D space. This feature is only available for
 * [SpatialPanels] at the moment.
 *
 * @param enabled true if this composable should be resizable.
 * @param minimumSize the smallest allowed dimensions for this composable.
 * @param maximumSize the largest allowed dimensions for this composable.
 * @param maintainAspectRatio true if the new size should maintain the same aspect ratio as the
 *   existing size.
 * @param onSizeChange a callback to process the size change in pixels during resizing. This will
 *   only be called if [enabled] is true. If the callback returns false or isn't specified, the
 *   default behavior of resizing this composable will be executed. If it returns true, it is the
 *   responsibility of the callback to process the event.
 *
 * TODO(b/427974119): Investigate fix for resizing from size Zero.
 */
public fun SubspaceModifier.resizable(
    enabled: Boolean = true,
    minimumSize: DpVolumeSize = DpVolumeSize.Zero,
    maximumSize: DpVolumeSize = DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity),
    maintainAspectRatio: Boolean = false,
    onSizeChange: ((IntVolumeSize) -> Boolean)? = null,
): SubspaceModifier =
    this.then(
        ResizableElement(enabled, minimumSize, maximumSize, maintainAspectRatio, onSizeChange)
    )

private class ResizableElement(
    private val enabled: Boolean,
    private val minimumSize: DpVolumeSize,
    private val maximumSize: DpVolumeSize,
    private val maintainAspectRatio: Boolean,
    private val onSizeChange: ((IntVolumeSize) -> Boolean)?,
) : SubspaceModifierNodeElement<ResizableNode>() {

    init {
        // TODO(b/345303299): Decide on implementation for min/max size bound checking against
        // current
        //  size.
        require(
            minimumSize.depth <= maximumSize.depth &&
                minimumSize.height <= maximumSize.height &&
                minimumSize.width <= maximumSize.width
        ) {
            "minimumSize must be less than or equal to maximumSize"
        }
    }

    override fun create(): ResizableNode =
        ResizableNode(enabled, minimumSize, maximumSize, maintainAspectRatio, onSizeChange)

    override fun update(node: ResizableNode) {
        node.enabled = enabled
        node.minimumSize = minimumSize
        node.maximumSize = maximumSize
        node.maintainAspectRatio = maintainAspectRatio
        node.onSizeChange = onSizeChange
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + minimumSize.hashCode()
        result = 31 * result + maximumSize.hashCode()
        result = 31 * result + maintainAspectRatio.hashCode()
        result = 31 * result + onSizeChange.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherElement = other as? ResizableNode ?: return false

        return enabled == otherElement.enabled &&
            minimumSize == otherElement.minimumSize &&
            maximumSize == otherElement.maximumSize &&
            maintainAspectRatio == otherElement.maintainAspectRatio &&
            onSizeChange === otherElement.onSizeChange
    }
}

internal class ResizableNode(
    internal var enabled: Boolean,
    internal var minimumSize: DpVolumeSize,
    internal var maximumSize: DpVolumeSize,
    internal var maintainAspectRatio: Boolean,
    internal var onSizeChange: ((IntVolumeSize) -> Boolean)?,
) :
    SubspaceModifier.Node(),
    CompositionLocalConsumerSubspaceModifierNode,
    CoreEntityNode,
    LayoutCoordinatesAwareModifierNode,
    ResizeListener {
    private inline val density: Density
        get() = currentValueOf(LocalDensity)

    private inline val session: Session
        get() = checkNotNull(currentValueOf(LocalSession)) { "Expected Session to be available." }

    /** Size based on user adjustments from ResizeEvents from SceneCore. */
    private var userSize: IntVolumeSize? = null

    /** Whether the resizableComponent is attached to the entity. */
    private var isComponentAttached: Boolean = false

    private val component: ResizableComponent by lazy {
        ResizableComponent.create(session).also { it.addResizeListener(MainExecutor, this) }
    }

    /** Updates the resizable state of this CoreEntity. */
    private fun updateState() {
        if (coreEntity !is ResizableCoreEntity) {
            return
        }
        // Enabled is on the Node. It means "should be enabled" for the Component.
        if (enabled) {
            enableAndUpdateComponent()
        } else {
            disableComponent()
        }
    }

    /** Enables the ResizableComponent for this CoreEntity and updates its values. */
    private fun enableAndUpdateComponent() {
        if (!isComponentAttached) {
            check(coreEntity.addComponent(component)) {
                "Could not add ResizableComponent to Core Entity"
            }
            isComponentAttached = true
        }

        minimumSize.toDimensionsInMeters().let {
            if (component.minimumSize != it) {
                component.minimumSize = it
            }
        }
        maximumSize.toDimensionsInMeters().let {
            if (component.maximumSize != it) {
                component.maximumSize = it
            }
        }
    }

    /** Returns 0.0f if the aspect ratio of x to y is not well defined. */
    private fun getAspectRatioY(size: FloatSize3d): Float {
        if (size.width == 0f || size.height == 0f) return 0.0f
        return size.width / size.height
    }

    /**
     * Disables the ResizableComponent for this CoreEntity. Takes care of life cycle tasks for the
     * underlying component in SceneCore.
     */
    private fun disableComponent() {
        if (isComponentAttached) {
            coreEntity.removeComponent(component)
            isComponentAttached = false
            userSize = null
        }
    }

    override fun onResizeStart(entity: Entity, originalSize: FloatSize3d) {
        component.fixedAspectRatio =
            if (maintainAspectRatio) getAspectRatioY(originalSize) else 0.0f
    }

    /**
     * During a resize, the size of the entity does not change, only its reform window. We do not
     * need to respond to every event, e.g., onResizeUpdate, like we do for Movable.
     */
    override fun onResizeEnd(entity: Entity, finalSize: FloatSize3d) {
        resizeListener(finalSize)
    }

    /**
     * Called every time there is an onResizeEnd event in SceneCore, if this CoreEntity is
     * resizable.
     */
    private fun resizeListener(newSize: FloatSize3d) {
        if (onSizeChange?.invoke(newSize.toIntVolumeSize(density)) == true) {
            // We're done, the user app will handle the event.
            return
        }
        userSize = newSize.toIntVolumeSize(density)
        requestRelayout()
    }

    override fun CoreEntityScope.modifyCoreEntity() {
        updateState()
        userSize?.let { setRenderedSize(it) }
    }

    override fun onLayoutCoordinates(coordinates: SubspaceLayoutCoordinates) {
        // Update the size of the component to match the final size of the layout.
        component.size = coordinates.size.toDimensionsInMeters(density)
    }

    override fun onDetach() {
        disableComponent()
    }

    private companion object {
        val MainExecutor: Executor = Dispatchers.Main.asExecutor()
    }
}
