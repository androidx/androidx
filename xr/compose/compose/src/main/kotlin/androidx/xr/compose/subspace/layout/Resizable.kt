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
import androidx.xr.compose.subspace.SpatialCurvedRow
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
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * When the resizable modifier is present and enabled, draggable UI controls will be shown that
 * allow the user to resize the element in 3D space. The final size is not automatically applied to
 * the Composable. The developer must use the [onResize] event and apply the result themselves.
 * (e.g., by updating a state backed by [SubspaceModifier.width] and [SubspaceModifier.height]) The
 * resize affordance will not curve and thus adding it to a Composable with curvature like
 * [SpatialCurvedRow] is not recommended.
 *
 * @param minimumSize The minimum allowable size for the object, represented by a [DpVolumeSize].
 *   The object cannot be scaled down beyond these dimensions. Defaults to [DpVolumeSize.Zero].
 * @param maximumSize The maximum allowable size for the object, represented by a [DpVolumeSize].
 *   The object cannot be scaled up beyond these dimensions. Defaults to a [DpVolumeSize] with all
 *   dimensions set to [Dp.Infinity], meaning no upper limit by default.
 * @param maintainAspectRatio If `true`, the object's aspect ratio (proportions) will be preserved
 *   during resizing. If `false`, individual dimensions can be changed independently.
 * @param onResize Mandatory callback invoked continuously during the interaction that receives a
 *   [SpatialResizeEvent] containing the calculated target size. The size contained in this event is
 *   the resulting size after the resize gesture and should be used to manually resize the
 *   corresponding layout.
 * @sample androidx.xr.compose.samples.ResizableWithStateSample
 * @see transformingResizable for standard resizing behaviors.
 */
public fun SubspaceModifier.resizable(
    minimumSize: DpVolumeSize = DpVolumeSize.Zero,
    maximumSize: DpVolumeSize = DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity),
    maintainAspectRatio: Boolean = false,
    onResize: (SpatialResizeEvent) -> Unit,
): SubspaceModifier =
    this.then(
        CustomResizableElement(
            minimumSize = minimumSize,
            maximumSize = maximumSize,
            maintainAspectRatio = maintainAspectRatio,
            onResize = onResize,
        )
    )

/**
 * When the resizable modifier is present and enabled, draggable UI controls will be shown that
 * allow the user to resize the element in 3D space.
 *
 * @param enabled Whether resizing is enabled for this object. If `false`, the object cannot be
 *   resized. When resizing behavior is handled by the API, changing the [enabled] state of the
 *   modifier does not clear the user resize state; whereas, removing the modifier will reset the
 *   user resize state, causing the object to revert to its layout size. Defaults to `true`.
 * @param minimumSize The minimum allowable size for the object, represented by a [DpVolumeSize].
 *   The object cannot be scaled down beyond these dimensions. Defaults to [DpVolumeSize.Zero].
 * @param maximumSize The maximum allowable size for the object, represented by a [DpVolumeSize].
 *   The object cannot be scaled up beyond these dimensions. Defaults to a [DpVolumeSize] with all
 *   dimensions set to [Dp.Infinity], meaning no upper limit by default.
 * @param maintainAspectRatio If `true`, the object's aspect ratio (proportions) will be preserved
 *   during resizing. If `false`, individual dimensions can be changed independently. Defaults to
 *   `false`.
 * @param onResizeStart A callback to be called when the resize event starts.
 * @param onResizeUpdate A callback to be called when the size changes during a resize event.
 * @param onResizeEnd A callback to be called when the object's size changes, after a resize event
 *   has ended. It receives an [IntVolumeSize] representing the new size. Returning `true` from this
 *   callback indicates that the developer intends to handle the size change, and the API should not
 *   resize the object. Returning `false` indicates that the developer will not handle the size
 *   change, and the API should proceed with changing the size of the object itself. By default, if
 *   [onResizeEnd] is not provided, the API will change the size of the object.
 */
@Deprecated(
    message =
        "Use transformingResizable() for default system-handled resizing that automatically applies transformations to the layout. For custom resizing where you manually apply the resulting size (e.g., via width/height), use the updated resizable() modifier signature."
)
public fun SubspaceModifier.resizable(
    enabled: Boolean = true,
    minimumSize: DpVolumeSize = DpVolumeSize.Zero,
    maximumSize: DpVolumeSize = DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity),
    maintainAspectRatio: Boolean = false,
    onResizeStart: ((IntVolumeSize) -> Unit) = {},
    onResizeUpdate: ((IntVolumeSize) -> Unit) = {},
    onResizeEnd: ((IntVolumeSize) -> Boolean) = { false },
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
        )
    )

/**
 * An event representing a change in size and scale during a resize operation.
 *
 * @property type The current type of the resize event.
 * @property size The new size of the composable, expressed in virtual pixels.
 */
public class SpatialResizeEvent(
    public val type: SpatialResizeEventType,
    public val size: IntVolumeSize,
)

/** An enum representing the phases of resizing. */
@JvmInline
public value class SpatialResizeEventType private constructor(private val value: Int) {

    public companion object {
        /** The phase where the resize event starts. */
        public val Start: SpatialResizeEventType = SpatialResizeEventType(0)

        /** The phase where the user continuously resizes. */
        public val Resizing: SpatialResizeEventType = SpatialResizeEventType(1)

        /** The phase where the resize event ends. */
        public val End: SpatialResizeEventType = SpatialResizeEventType(2)
    }
}

private class CustomResizableElement(
    private val minimumSize: DpVolumeSize,
    private val maximumSize: DpVolumeSize,
    private val maintainAspectRatio: Boolean,
    private val onResize: (SpatialResizeEvent) -> Unit,
) : SubspaceModifierNodeElement<CustomResizableNode>() {

    init {
        require(
            minimumSize.depth <= maximumSize.depth &&
                minimumSize.height <= maximumSize.height &&
                minimumSize.width <= maximumSize.width
        ) {
            "minimumSize must be less than or equal to maximumSize"
        }
    }

    override fun create(): CustomResizableNode =
        CustomResizableNode(
            minimumSize = minimumSize,
            maximumSize = maximumSize,
            maintainAspectRatio = maintainAspectRatio,
            onResize = onResize,
        )

    override fun update(node: CustomResizableNode) {
        node.minimumSize = minimumSize
        node.maximumSize = maximumSize
        node.maintainAspectRatio = maintainAspectRatio
        node.onResize = onResize
        node.updateState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomResizableElement

        if (minimumSize != other.minimumSize) return false
        if (maximumSize != other.maximumSize) return false
        if (maintainAspectRatio != other.maintainAspectRatio) return false
        if (onResize !== other.onResize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minimumSize.hashCode()
        result = 31 * result + maximumSize.hashCode()
        result = 31 * result + maintainAspectRatio.hashCode()
        result = 31 * result + onResize.hashCode()
        return result
    }
}

private class CustomResizableNode(
    var minimumSize: DpVolumeSize,
    var maximumSize: DpVolumeSize,
    var maintainAspectRatio: Boolean,
    var onResize: (SpatialResizeEvent) -> Unit,
) :
    SubspaceModifier.Node(),
    CompositionLocalConsumerSubspaceModifierNode,
    CoreEntityNode,
    SubspaceLayoutModifierNode {

    private inline val density: Density
        get() = currentValueOf(LocalDensity)

    private inline val session: Session
        get() = checkNotNull(currentValueOf(LocalSession)) { "Resizable requires a Session." }

    private var component: ResizableComponent? = null

    override fun onAttach() {
        super.onAttach()
        updateState()
    }

    override fun onDetach() {
        if (component != null) {
            disableComponent()
        }
    }

    internal fun updateState() {
        if (component == null) {
            enableComponent()
        }

        component?.let {
            it.minimumEntitySize = minimumSize.toDimensionsInMeters()
            it.maximumEntitySize = maximumSize.toDimensionsInMeters()
        }
    }

    private fun enableComponent() {
        check(component == null) { "ResizableComponent already enabled." }
        component =
            ResizableComponent.create(session = session, executor = MainExecutor) {
                handleResizeEvent(it)
            }

        coreEntity.onEntityAttached { entity ->
            val currentComponent = component
            if (currentComponent != null) {
                val success = entity.addComponent(currentComponent)
                if (!success) {
                    component = null
                    throw IllegalStateException(
                        "Failed to add ResizableComponent to Core Entity. The entity may have been " +
                            "detached or entered an invalid state during composition."
                    )
                }
            }
        }
    }

    private fun disableComponent() {
        check(component != null) { "ResizableComponent already disabled." }
        component?.let { coreEntity.removeComponent(it) }
        component = null
    }

    private fun handleResizeEvent(resizeEvent: ResizeEvent) {
        val eventType =
            when (resizeEvent.resizeState) {
                ResizeEvent.ResizeState.START -> {
                    component?.isFixedAspectRatioEnabled = maintainAspectRatio
                    SpatialResizeEventType.Start
                }
                ResizeEvent.ResizeState.ONGOING -> SpatialResizeEventType.Resizing
                ResizeEvent.ResizeState.END -> SpatialResizeEventType.End
                else -> return
            }
        onResize.invoke(SpatialResizeEvent(eventType, resizeEvent.newSize.toIntVolumeSize(density)))
    }

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val placeable = measurable.measure(constraints)

        component?.affordanceSize =
            IntVolumeSize(placeable.width, placeable.height, placeable.depth)
                .toDimensionsInMeters(Density(density))

        return layout(placeable.width, placeable.height, placeable.depth) {
            placeable.place(Pose.Identity)
        }
    }

    private companion object {
        val MainExecutor: Executor = Dispatchers.Main.asExecutor()
    }
}

private class ResizableElement(
    private val enabled: Boolean,
    private val minimumSize: DpVolumeSize,
    private val maximumSize: DpVolumeSize,
    private val maintainAspectRatio: Boolean,
    private val onResizeStart: ((IntVolumeSize) -> Unit),
    private val onResizeUpdate: ((IntVolumeSize) -> Unit),
    private val onResizeEnd: ((IntVolumeSize) -> Boolean),
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
        )

    override fun update(node: ResizableNode) {
        node.enabled = enabled
        node.minimumSize = minimumSize
        node.maximumSize = maximumSize
        node.maintainAspectRatio = maintainAspectRatio
        node.onResizeStart = onResizeStart
        node.onResizeUpdate = onResizeUpdate
        node.onResizeEnd = onResizeEnd
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + maintainAspectRatio.hashCode()
        result = 31 * result + minimumSize.hashCode()
        result = 31 * result + maximumSize.hashCode()
        result = 31 * result + onResizeStart.hashCode()
        result = 31 * result + onResizeUpdate.hashCode()
        result = 31 * result + onResizeEnd.hashCode()
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

        return true
    }
}

internal class ResizableNode(
    internal var enabled: Boolean,
    internal var minimumSize: DpVolumeSize,
    internal var maximumSize: DpVolumeSize,
    internal var maintainAspectRatio: Boolean,
    internal var onResizeStart: ((IntVolumeSize) -> Unit),
    internal var onResizeUpdate: ((IntVolumeSize) -> Unit),
    internal var onResizeEnd: ((IntVolumeSize) -> Boolean),
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
                onResizeStart(resizeEvent.newSize.toIntVolumeSize(density))
            }
            ResizeEvent.ResizeState.ONGOING ->
                onResizeUpdate(resizeEvent.newSize.toIntVolumeSize(density))
            ResizeEvent.ResizeState.END -> {
                val nextSize = resizeEvent.newSize.toIntVolumeSize(density)
                if (!onResizeEnd(nextSize)) {
                    userSize = nextSize
                    invalidateMeasurement()
                }
            }
        }
    }

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
                    originalSize = IntVolumeSize(it.width, it.height, it.depth)
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
            IntVolumeSize(placeable.width, placeable.height, placeable.depth)
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
