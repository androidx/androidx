/*
 * Copyright 2026 The Android Open Source Project
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
 * When the resizable modifier is present and enabled, UI controls will be shown that allow the user
 * to resize the element in 3D space. The final transform of the attached Composable will be set by
 * the system. The resize affordance will not curve and thus adding it to a Composable with
 * curvature like [SpatialCurvedRow] is not recommended.
 *
 * @param enabled Whether resizing is enabled for this object. If `false`, the object cannot be
 *   resized. Changing the [enabled] state of the modifier does not clear the user resize state;
 *   whereas, removing the modifier will reset the user resize state, causing the object to revert
 *   to its layout size.
 * @param minimumSize The minimum allowable size for the object, represented by a [DpVolumeSize].
 *   The object cannot be scaled down beyond these dimensions. Defaults to [DpVolumeSize.Zero].
 * @param maximumSize The maximum allowable size for the object, represented by a [DpVolumeSize].
 *   The object cannot be scaled up beyond these dimensions. Defaults to a [DpVolumeSize] with all
 *   dimensions set to [Dp.Infinity], meaning no upper limit by default.
 * @param maintainAspectRatio If `true`, the object's aspect ratio (proportions) will be preserved
 *   during resizing. If `false`, individual dimensions can be changed independently.
 * @param onResize Optional observer callback invoked during the manipulation to observe resize
 *   events through [SpatialResizeEvent]. Since the system automatically applies the resize, this
 *   callback is strictly for monitoring changes and does not control the size.
 * @sample androidx.xr.compose.samples.BasicTransformingResizableSample
 * @see resizable for implementing custom resize behaviors.
 */
public fun SubspaceModifier.transformingResizable(
    enabled: Boolean = true,
    minimumSize: DpVolumeSize = DpVolumeSize.Zero,
    maximumSize: DpVolumeSize = DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity),
    maintainAspectRatio: Boolean = false,
    onResize: (SpatialResizeEvent) -> Unit = {},
): SubspaceModifier =
    this.then(
        TransformingResizableElement(
            enabled = enabled,
            minimumSize = minimumSize,
            maximumSize = maximumSize,
            maintainAspectRatio = maintainAspectRatio,
            onResize = onResize,
        )
    )

private class TransformingResizableElement(
    private val enabled: Boolean,
    private val minimumSize: DpVolumeSize,
    private val maximumSize: DpVolumeSize,
    private val maintainAspectRatio: Boolean,
    private val onResize: (SpatialResizeEvent) -> Unit,
) : SubspaceModifierNodeElement<TransformingResizableNode>() {

    init {
        require(
            minimumSize.depth <= maximumSize.depth &&
                minimumSize.height <= maximumSize.height &&
                minimumSize.width <= maximumSize.width
        ) {
            "minimumSize must be less than or equal to maximumSize"
        }
    }

    override fun create(): TransformingResizableNode =
        TransformingResizableNode(
            enabled = enabled,
            minimumSize = minimumSize,
            maximumSize = maximumSize,
            maintainAspectRatio = maintainAspectRatio,
            onResize = onResize,
        )

    override fun update(node: TransformingResizableNode) {
        node.enabled = enabled
        node.minimumSize = minimumSize
        node.maximumSize = maximumSize
        node.maintainAspectRatio = maintainAspectRatio
        node.onResize = onResize
        node.updateState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransformingResizableElement

        if (enabled != other.enabled) return false
        if (minimumSize != other.minimumSize) return false
        if (maximumSize != other.maximumSize) return false
        if (maintainAspectRatio != other.maintainAspectRatio) return false
        if (onResize !== other.onResize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + minimumSize.hashCode()
        result = 31 * result + maximumSize.hashCode()
        result = 31 * result + maintainAspectRatio.hashCode()
        result = 31 * result + onResize.hashCode()
        return result
    }
}

private class TransformingResizableNode(
    var enabled: Boolean,
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

    /** Size based on user adjustments from ResizeEvents from SceneCore. */
    private var userSize: IntVolumeSize? = null

    /** Size based on measurement of the content without user adjustments. */
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
        userSize = null
    }

    private fun handleResizeEvent(resizeEvent: ResizeEvent) {
        val eventType =
            when (resizeEvent.resizeState) {
                ResizeEvent.ResizeState.START -> {
                    component?.isFixedAspectRatioEnabled = maintainAspectRatio
                    SpatialResizeEventType.Start
                }
                ResizeEvent.ResizeState.ONGOING -> SpatialResizeEventType.Resizing
                ResizeEvent.ResizeState.END -> {
                    userSize = resizeEvent.newSize.toIntVolumeSize(density)
                    invalidateMeasurement()
                    SpatialResizeEventType.End
                }
                else -> return
            }
        onResize.invoke(SpatialResizeEvent(eventType, resizeEvent.newSize.toIntVolumeSize(density)))
    }

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val userSize = userSize
        val placeable =
            if (userSize == null) {
                measurable.measure(constraints).also {
                    originalSize = IntVolumeSize(it.width, it.height, it.depth)
                }
            } else {
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
                .toDimensionsInMeters(Density(density))

        return layout(originalSize.width, originalSize.height, originalSize.depth) {
            placeable.place(Pose.Identity)
        }
    }

    private companion object {
        val MainExecutor: Executor = Dispatchers.Main.asExecutor()
    }
}
