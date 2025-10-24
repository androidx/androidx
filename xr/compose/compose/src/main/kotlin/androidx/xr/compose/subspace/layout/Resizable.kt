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
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.currentValueOf
import androidx.xr.compose.subspace.node.invalidateMeasurement
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.compose.unit.toDimensionsInMeters
import androidx.xr.compose.unit.toIntVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * When the resizable modifier is present and enabled, draggable UI controls will be shown that
 * allow the user to resize the element in 3D space. This feature is only available for instances of
 * [SpatialPanel][androidx.xr.compose.subspace.SpatialPanel] at the moment.
 *
 * @param enabled true if this composable should be resizable.
 * @param minimumSize the smallest allowed dimensions for this composable.
 * @param maximumSize the largest allowed dimensions for this composable.
 * @param maintainAspectRatio true if the new size should maintain the same aspect ratio as the
 *   existing size.
 * @param onResizeStart will be called when a resize event has started with the original size
 * @param onResizeUpdate will be called when the resize amount has changed during a resize event
 * @param onResizeEnd will be called when a resize event has finished with the new size
 * @param onSizeChange a callback to process the size change in pixels during resizing. This will
 *   only be called if [enabled] is true. If the callback returns false or isn't specified, the
 *   default behavior of resizing this composable will be executed. If it returns true, it is the
 *   responsibility of the callback to process the event.
 *
 * TODO(b/427974119): Investigate fix for resizing from size Zero.
 */
internal fun SubspaceModifier.resizable(
    enabled: Boolean = true,
    minimumSize: DpVolumeSize = DpVolumeSize.Zero,
    maximumSize: DpVolumeSize = DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity),
    maintainAspectRatio: Boolean = false,
    onResizeStart: ((IntVolumeSize) -> Unit)? = null,
    onResizeUpdate: ((IntVolumeSize) -> Unit)? = null,
    onResizeEnd: ((IntVolumeSize) -> Unit)? = null,
    onSizeChange: ((IntVolumeSize) -> Boolean)? = null,
): SubspaceModifier =
    this.then(
        ResizableElement(
            enabled,
            minimumSize,
            maximumSize,
            maintainAspectRatio,
            onResizeStart,
            onResizeUpdate,
            onResizeEnd,
            onSizeChange,
        )
    )

private class ResizableElement(
    private val enabled: Boolean,
    private val minimumSize: DpVolumeSize,
    private val maximumSize: DpVolumeSize,
    private val maintainAspectRatio: Boolean,
    private val onResizeStart: ((IntVolumeSize) -> Unit)? = null,
    private val onResizeUpdate: ((IntVolumeSize) -> Unit)? = null,
    private val onResizeEnd: ((IntVolumeSize) -> Unit)? = null,
    private val onSizeChange: ((IntVolumeSize) -> Boolean)?,
) : SubspaceModifierNodeElement<ResizableNode>() {

    init {
        require(
            minimumSize.depth <= maximumSize.depth &&
                minimumSize.height <= maximumSize.height &&
                minimumSize.width <= maximumSize.width
        ) {
            "minimumSize must be less than or equal to maximumSize"
        }
    }

    override fun create(): ResizableNode =
        ResizableNode(
            enabled,
            minimumSize,
            maximumSize,
            maintainAspectRatio,
            onResizeStart,
            onResizeUpdate,
            onResizeEnd,
            onSizeChange,
        )

    override fun update(node: ResizableNode) {
        node.enabled = enabled
        node.minimumSize = minimumSize
        node.maximumSize = maximumSize
        node.maintainAspectRatio = maintainAspectRatio
        node.onResizeStart = onResizeStart
        node.onResizeUpdate = onResizeUpdate
        node.onResizeEnd = onResizeEnd
        node.onSizeChange = onSizeChange
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + maintainAspectRatio.hashCode()
        result = 31 * result + minimumSize.hashCode()
        result = 31 * result + maximumSize.hashCode()
        result = 31 * result + (onResizeStart?.hashCode() ?: 0)
        result = 31 * result + (onResizeUpdate?.hashCode() ?: 0)
        result = 31 * result + (onResizeEnd?.hashCode() ?: 0)
        result = 31 * result + (onSizeChange?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResizableElement

        if (enabled != other.enabled) return false
        if (maintainAspectRatio != other.maintainAspectRatio) return false
        if (minimumSize != other.minimumSize) return false
        if (maximumSize != other.maximumSize) return false
        if (onResizeStart !== other.onResizeStart) return false
        if (onResizeUpdate !== other.onResizeUpdate) return false
        if (onResizeEnd !== other.onResizeEnd) return false
        if (onSizeChange !== other.onSizeChange) return false

        return true
    }
}

internal class ResizableNode(
    internal var enabled: Boolean,
    internal var minimumSize: DpVolumeSize,
    internal var maximumSize: DpVolumeSize,
    internal var maintainAspectRatio: Boolean,
    internal var onResizeStart: ((IntVolumeSize) -> Unit)? = null,
    internal var onResizeUpdate: ((IntVolumeSize) -> Unit)? = null,
    internal var onResizeEnd: ((IntVolumeSize) -> Unit)? = null,
    internal var onSizeChange: ((IntVolumeSize) -> Boolean)?,
) :
    SubspaceModifier.Node(),
    CompositionLocalConsumerSubspaceModifierNode,
    CoreEntityNode,
    SubspaceLayoutModifierNode {
    private inline val density: Density
        get() = currentValueOf(LocalDensity)

    private inline val session: Session
        get() = checkNotNull(currentValueOf(LocalSession)) { "Expected Session to be available." }

    /** Size based on user adjustments from ResizeEvents from SceneCore. */
    private var userSize: IntVolumeSize? = null

    /** Size based on measurement of the content without user adjustments. */
    private var originalSize: IntVolumeSize = IntVolumeSize.Zero

    /** Whether the resizableComponent is attached to the entity. */
    private var isComponentAttached: Boolean = false

    private val component: ResizableComponent by lazy {
        ResizableComponent.create(session = session, executor = MainExecutor) {
            resizeEvent: ResizeEvent ->
            handleResizeEvent(resizeEvent)
        }
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
            coreEntity.onEntityAttached {
                check(coreEntity.addComponent(component) == true) {
                    "Could not add ResizableComponent to Core Entity"
                }
            }
            isComponentAttached = true
        }

        minimumSize.toDimensionsInMeters().let {
            if (component.minimumEntitySize != it) {
                component.minimumEntitySize = it
            }
        }
        maximumSize.toDimensionsInMeters().let {
            if (component.maximumEntitySize != it) {
                component.maximumEntitySize = it
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

    /**
     * During a resize, the size of the entity does not change, only its reform window. We do not
     * need to respond to every event, e.g., RESIZE_STATE_ONGOING, like we do for Movable.
     */
    fun handleResizeEvent(resizeEvent: ResizeEvent) {
        when (resizeEvent.resizeState) {
            ResizeEvent.ResizeState.START -> {
                component.isFixedAspectRatioEnabled = maintainAspectRatio
                onResizeStart?.invoke(resizeEvent.newSize.toIntVolumeSize(density))
            }
            ResizeEvent.ResizeState.ONGOING ->
                onResizeUpdate?.invoke(resizeEvent.newSize.toIntVolumeSize(density))
            ResizeEvent.ResizeState.END -> {
                val nextSize = resizeEvent.newSize.toIntVolumeSize(density)
                onResizeEnd?.invoke(nextSize)
                if (onSizeChange?.invoke(nextSize) == true) {
                    // We're done, the user app will handle the event.
                    return
                }
                userSize = nextSize
                invalidateMeasurement()
            }
        }
    }

    override fun CoreEntityScope.modifyCoreEntity() {}

    override fun onDetach() {
        disableComponent()
    }

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        updateState()
        val userSize = userSize
        val placeable =
            if (userSize == null) {
                measurable.measure(constraints).also {
                    originalSize =
                        IntVolumeSize(it.measuredWidth, it.measuredHeight, it.measuredDepth)
                }
            } else {
                // Measuring this node using userSize as the constraints to force the rendered size.
                measurable.measure(
                    VolumeConstraints(
                        minWidth = userSize.width,
                        maxWidth = userSize.width,
                        minHeight = userSize.height,
                        maxHeight = userSize.height,
                        minDepth = userSize.depth,
                        maxDepth = userSize.depth,
                    )
                )
            }

        component.affordanceSize =
            IntVolumeSize(
                    placeable.measuredWidth,
                    placeable.measuredHeight,
                    placeable.measuredDepth,
                )
                .toDimensionsInMeters(Density(density))

        // We use the original size of the component here, before any user changes were made. This
        // allows us to maintain the same size in the parent layout.
        return layout(originalSize.width, originalSize.height, originalSize.depth) {
            placeable.place(Pose.Identity)
        }
    }

    private companion object {
        val MainExecutor: Executor = Dispatchers.Main.asExecutor()
    }
}
