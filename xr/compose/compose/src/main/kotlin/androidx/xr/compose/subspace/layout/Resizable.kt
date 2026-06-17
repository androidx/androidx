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
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/** Defines how resizing interactions are applied and handled for [SubspaceModifier.resizable]. */
public sealed interface ResizePolicy {

    public companion object {
        /**
         * Default resizing policy where the system automatically handles the resize gesture and
         * applies the new dimensions to the content under the hood. This policy will not update the
         * layout size of the object inside its parent. For instance, if used on an object inside a
         * [androidx.xr.compose.subspace.SpatialRow], the object size will change but the row size
         * will not, potentially resulting in 3D object overlaps, unless the object is also paired
         * with a [movable] modifier. Removing a modifier using this policy will reset the user
         * resize state, causing the object to revert to its layout size.
         *
         * @param onResize Optional observer callback invoked during the interaction to monitor
         *   resize events. Since the system automatically applies the resize, this callback is
         *   strictly for monitoring changes and should not be used to update the content size.
         */
        public fun default(onResize: ((SpatialResizeEvent) -> Unit) = {}): ResizePolicy =
            DefaultResizePolicy(onResize)

        /**
         * Custom resizing policy where the developer is fully responsible for applying the new
         * dimensions to the content (e.g., by updating a state that backs [width]/[height]). This
         * is necessary for situations where the overall layout should be readjusted after the
         * resize event. Using this policy has higher latency than [default].
         *
         * @param onResize Callback invoked during the interaction containing the calculated target
         *   size.
         */
        public fun custom(onResize: (SpatialResizeEvent) -> Unit): ResizePolicy =
            CustomResizePolicy(onResize)
    }
}

internal data class DefaultResizePolicy(val onResize: ((SpatialResizeEvent) -> Unit)?) :
    ResizePolicy

internal data class CustomResizePolicy(val onResize: (SpatialResizeEvent) -> Unit) : ResizePolicy

/**
 * When the resizable modifier is present and enabled, UI controls will be shown that allow the user
 * to resize the element in 3D space.
 *
 * @param enabled Whether resizing is enabled for this object. If `false`, the object cannot be
 *   resized, but any size modifications that already occurred under the current [resizePolicy] will
 *   remain.
 * @param minimumSize The minimum allowable size for the object, represented by a [DpVolumeSize].
 *   The object cannot be scaled down beyond these dimensions. Defaults to [DpVolumeSize.Zero].
 * @param maximumSize The maximum allowable size for the object, represented by a [DpVolumeSize].
 *   The object cannot be scaled up beyond these dimensions. Defaults to a [DpVolumeSize] with all
 *   dimensions set to [Dp.Infinity], meaning no upper limit by default.
 * @param maintainAspectRatio If `true`, the object's aspect ratio (proportions) will be preserved
 *   during resizing. If `false`, individual dimensions can be changed independently. Defaults to
 *   `false`.
 * @param resizePolicy The policy that determines how the size change is applied. Defaults to
 *   [ResizePolicy.default()] which automatically handles resizing under the hood.
 * @sample androidx.xr.compose.samples.BasicResizableSample
 * @sample androidx.xr.compose.samples.ResizableWithStateSample
 */
public fun SubspaceModifier.resizable(
    enabled: Boolean = true,
    minimumSize: DpVolumeSize = DpVolumeSize.Zero,
    maximumSize: DpVolumeSize = DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity),
    maintainAspectRatio: Boolean = false,
    resizePolicy: ResizePolicy = ResizePolicy.default(),
): SubspaceModifier =
    this.then(
        ResizableElement(
            enabled = enabled,
            minimumSize = minimumSize,
            maximumSize = maximumSize,
            maintainAspectRatio = maintainAspectRatio,
            resizePolicy = resizePolicy,
        )
    )

/**
 * When the resizable modifier is present and enabled, UI controls will be shown that allow the user
 * to resize the element in 3D space.
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

/**
 * When the resizable modifier is present and enabled, UI controls will be shown that allow the user
 * to resize the element in 3D space.
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
@Deprecated("Use the resizable modifier with ResizePolicy instead.")
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
        DeprecatedResizableElement(
            enabled = enabled,
            minimumSize = minimumSize,
            maximumSize = maximumSize,
            maintainAspectRatio = maintainAspectRatio,
            onResizeStart = onResizeStart,
            onResizeUpdate = onResizeUpdate,
            onResizeEnd = onResizeEnd,
        )
    )

private class ResizableElement(
    private val enabled: Boolean,
    private val minimumSize: DpVolumeSize,
    private val maximumSize: DpVolumeSize,
    private val maintainAspectRatio: Boolean,
    private val resizePolicy: ResizePolicy,
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
            enabled = enabled,
            minimumSize = minimumSize,
            maximumSize = maximumSize,
            maintainAspectRatio = maintainAspectRatio,
            resizePolicy = resizePolicy,
        )

    override fun update(node: ResizableNode) {
        node.enabled = enabled
        node.minimumSize = minimumSize
        node.maximumSize = maximumSize
        node.maintainAspectRatio = maintainAspectRatio
        node.resizePolicy = resizePolicy
        node.updateState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResizableElement

        if (enabled != other.enabled) return false
        if (minimumSize != other.minimumSize) return false
        if (maximumSize != other.maximumSize) return false
        if (maintainAspectRatio != other.maintainAspectRatio) return false
        if (resizePolicy != other.resizePolicy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + minimumSize.hashCode()
        result = 31 * result + maximumSize.hashCode()
        result = 31 * result + maintainAspectRatio.hashCode()
        result = 31 * result + resizePolicy.hashCode()
        return result
    }
}

internal class ResizableNode(
    internal var enabled: Boolean,
    internal var minimumSize: DpVolumeSize,
    internal var maximumSize: DpVolumeSize,
    internal var maintainAspectRatio: Boolean,
    internal var resizePolicy: ResizePolicy,
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

    /**
     * Size based on user adjustments from ResizeEvents from SceneCore. Only used for system-handled
     * (Default) resizing.
     */
    private var userSize: IntVolumeSize? = null

    /**
     * Size based on measurement of the content without user adjustments. Only used for
     * system-handled (Default) resizing.
     */
    private var originalSize: IntVolumeSize = IntVolumeSize.Zero

    override fun onAttach() {
        super.onAttach()
        updateState()
    }

    override fun onDetach() {
        if (component != null) {
            disableComponent()
        }
    }

    /** Updates the resizable state of this CoreEntity. */
    internal fun updateState() {
        if (enabled && component == null) {
            enableComponent()
        } else if (!enabled && component != null) {
            disableComponent()
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
        val policy = resizePolicy
        val eventType =
            when (resizeEvent.resizeState) {
                ResizeEvent.ResizeState.START -> {
                    component?.isFixedAspectRatioEnabled = maintainAspectRatio
                    SpatialResizeEventType.Start
                }
                ResizeEvent.ResizeState.ONGOING -> SpatialResizeEventType.Resizing
                ResizeEvent.ResizeState.END -> {
                    if (policy is DefaultResizePolicy) {
                        val nextSize = resizeEvent.newSize.toIntVolumeSize(density)
                        userSize = nextSize
                        invalidateMeasurement()
                    }
                    SpatialResizeEventType.End
                }
                else -> return
            }

        val size = resizeEvent.newSize.toIntVolumeSize(density)
        val event = SpatialResizeEvent(eventType, size)

        when (policy) {
            is DefaultResizePolicy -> policy.onResize?.invoke(event)
            is CustomResizePolicy -> policy.onResize.invoke(event)
        }
    }

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val isSystemHandled = resizePolicy is DefaultResizePolicy
        val userSize = userSize

        val placeable =
            if (!isSystemHandled || userSize == null) {
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

        component?.affordanceSize =
            IntVolumeSize(placeable.width, placeable.height, placeable.depth)
                .toDimensionsInMeters(this@ResizableNode.density)

        val layoutWidth = if (isSystemHandled) originalSize.width else placeable.width
        val layoutHeight = if (isSystemHandled) originalSize.height else placeable.height
        val layoutDepth = if (isSystemHandled) originalSize.depth else placeable.depth

        // We use the original size of the component here for system handled policies to maintain
        // the same size in the parent layout.
        return layout(layoutWidth, layoutHeight, layoutDepth) { placeable.place(Pose.Identity) }
    }

    private companion object {
        val MainExecutor: Executor = Dispatchers.Main.asExecutor()
    }
}

private class DeprecatedResizableElement(
    private val enabled: Boolean,
    private val minimumSize: DpVolumeSize,
    private val maximumSize: DpVolumeSize,
    private val maintainAspectRatio: Boolean,
    private val onResizeStart: ((IntVolumeSize) -> Unit),
    private val onResizeUpdate: ((IntVolumeSize) -> Unit),
    private val onResizeEnd: ((IntVolumeSize) -> Boolean),
) : SubspaceModifierNodeElement<DeprecatedResizableNode>() {

    init {
        require(
            minimumSize.depth <= maximumSize.depth &&
                minimumSize.height <= maximumSize.height &&
                minimumSize.width <= maximumSize.width
        ) {
            "minimumSize must be less than or equal to maximumSize"
        }
    }

    override fun create(): DeprecatedResizableNode =
        DeprecatedResizableNode(
            enabled = enabled,
            minimumSize = minimumSize,
            maximumSize = maximumSize,
            maintainAspectRatio = maintainAspectRatio,
            onResizeStart = onResizeStart,
            onResizeUpdate = onResizeUpdate,
            onResizeEnd = onResizeEnd,
        )

    override fun update(node: DeprecatedResizableNode) {
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

        other as DeprecatedResizableElement

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

internal class DeprecatedResizableNode(
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
