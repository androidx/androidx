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
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.node.CompositionLocalConsumerSubspaceModifierNode
import androidx.xr.compose.subspace.node.SubspaceLayoutAwareModifierNode
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.currentValueOf
import androidx.xr.compose.subspace.node.invalidatePlacement
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.compose.unit.toDimensionsInMeters
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.MovableComponent
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * Configures this subspace element to be interactive and movable, delegating the pose
 * transformation to the system.
 *
 * When this modifier is present and enabled, draggable UI controls will be shown that allow the
 * user to move the element in 3D space. The system intercepts spatial input events, calculates the
 * resulting [Pose] and scale, and automatically applies these transformations to the element's
 * layout. This is the default behavior for standard movable UI elements where a 1:1 transformation
 * is desired, and custom gesture handling or manual state management is not required. Input events
 * used for moving in this way are consumed.
 *
 * There are some limitations that should be considered when using this modifier: 1) the draggable
 * UI controls of nested composables using the [transformingMovable] modifier and [movable] modifier
 * may conflict with each other, 2) attaching multiple [transformingMovable] modifiers to the same
 * element will compound the movement distance, since each modifier independently applies the drag
 * offset upon release. 3) It should not be used with the following composables
 * [androidx.xr.compose.subspace.SpatialExternalSurfaceHemisphere] and
 * [androidx.xr.compose.subspace.SpatialExternalSurfaceSphere] due to their similarity with the
 * system environment and not having any layout size.
 *
 * @param enabled true if this composable should be movable. Setting this to false will remove the
 *   interactable affordance associated with the content. Disabling the modifier after movement
 *   keeps the composable at its last dragged position. Removing the modifier entirely resets the
 *   composable to its original layout position.
 * @param scaleWithDistance true if this composable should scale in size when moved in depth. When
 *   enabled, the subspace element will grow if pushed away from the user or shrink when pulled
 *   toward the user in order to maintain the interact-ability and legibility of the panel. Scaling
 *   with distance respects other transformations applied to this layout.
 * @param onMove Optional observer callback invoked during the manipulation. Since the system
 *   automatically applies the move, this callback is strictly for monitoring changes and should not
 *   control the position. The [onMove] callback values can be used to position other sibling
 *   composables with the offset modifier. This callback reports a [SpatialMoveEvent] which will
 *   contain a [Pose]. The [Pose] contained in this event is the sum of all previous events in the
 *   move gesture.
 * @sample androidx.xr.compose.samples.BasicTransformingMovableSample
 * @sample androidx.xr.compose.samples.TransformingMovableSiblingSample
 * @see movable for implementing custom movement behaviors
 */
public fun SubspaceModifier.transformingMovable(
    enabled: Boolean = true,
    scaleWithDistance: Boolean = true,
    onMove: ((SpatialMoveEvent) -> Unit)? = null,
): SubspaceModifier = this.then(TransformingMovableElement(enabled, scaleWithDistance, onMove))

private class TransformingMovableElement(
    private val enabled: Boolean,
    private val scaleWithDistance: Boolean,
    private val onMove: ((SpatialMoveEvent) -> Unit)?,
) : SubspaceModifierNodeElement<TransformingMovableNode>() {
    override fun create(): TransformingMovableNode =
        TransformingMovableNode(
            enabled = enabled,
            scaleWithDistance = scaleWithDistance,
            onMove = onMove,
        )

    override fun update(node: TransformingMovableNode) {
        val scaleChanged = node.scaleWithDistance != scaleWithDistance
        node.enabled = enabled
        node.scaleWithDistance = scaleWithDistance
        node.onMove = onMove
        node.updateState()
        if (scaleChanged) {
            node.updateComponent()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransformingMovableElement

        if (enabled != other.enabled) return false
        if (scaleWithDistance != other.scaleWithDistance) return false
        if (onMove !== other.onMove) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + scaleWithDistance.hashCode()
        result = 31 * result + (onMove?.hashCode() ?: 0)
        return result
    }
}

private class TransformingMovableNode(
    var enabled: Boolean,
    var scaleWithDistance: Boolean,
    var onMove: ((SpatialMoveEvent) -> Unit)?,
) :
    SubspaceModifier.Node(),
    CompositionLocalConsumerSubspaceModifierNode,
    CoreEntityNode,
    SubspaceLayoutAwareModifierNode,
    EntityMoveListener,
    SubspaceLayoutModifierNode {

    private inline val density: Density
        get() = currentValueOf(LocalDensity)

    private inline val session: Session
        get() = checkNotNull(currentValueOf(LocalSession)) { "Movable requires a Session." }

    private var component: MovableComponent? = null

    /** The scale of this entity when it is moved. */
    private var scaleFromMovement: Float = 1.0F

    /** Pose based on user adjustments from MoveEvents from SceneCore. */
    private var userPose: Pose = Pose.Identity

    /** The current layout size of this entity, captured during placement. */
    private var currentLayoutSize: IntVolumeSize = IntVolumeSize.Zero

    /** The pose of this entity at the very start of the current move gesture. */
    private var initialDragPose: Pose = Pose.Identity

    /** The previous pose of this entity from the last MoveEvent. */
    private var previousPose: Pose = Pose.Identity

    /** The previous scale of this entity from the last MoveEvent. */
    private var previousScale: Float = 1.0F

    override fun CoreEntityScope.modifyCoreEntity() {
        setOrAppendScale(scaleFromMovement)
    }

    override fun onAttach() {
        super.onAttach()
        updateState()
    }

    override fun onDetach() {
        if (component != null) {
            disableComponent()
        }
    }

    override fun onPlaced(coordinates: SubspaceLayoutCoordinates) {
        // Update the size of the component to match the final size of the layout.
        component?.size = coordinates.size.toDimensionsInMeters(density)
        // Update the cached layout size of the composable.
        currentLayoutSize = coordinates.size
    }

    /** Updates the movable state of this CoreEntity. */
    internal fun updateState() {
        // Enabled is on the Node. It means "should be enabled" for the Component.
        if (enabled && component == null) {
            enableComponent()
        } else if (!enabled && component != null) {
            disableComponent()
        }
    }

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            placeable.place(userPose)
        }
    }

    /** Enables the MovableComponent and anchorPlacement for this CoreEntity. */
    private fun enableComponent() {
        check(component == null) { "MovableComponent already enabled." }
        component =
            MovableComponent.createSystemMovable(session = session, scaleInZ = scaleWithDistance)
                .also { it.addMoveListener(MainExecutor, this) }

        coreEntity.onEntityAttached { entity ->
            val currentComponent = component
            if (currentComponent != null) {
                val success = entity.addComponent(currentComponent)
                if (!success) {
                    component = null
                    throw IllegalStateException(
                        "Failed to add MovableComponent to Core Entity. The entity may have been " +
                            "detached or entered an invalid state during composition."
                    )
                }
            }
        }
    }

    /**
     * Disables the MovableComponent for this CoreEntity. Takes care of life cycle tasks for the
     * underlying component in SceneCore.
     */
    private fun disableComponent() {
        check(component != null) { "MovableComponent already disabled." }
        component?.removeMoveListener(this)
        component?.let { coreEntity.removeComponent(it) }
        component = null
    }

    /**
     * Recreates the underlying [MovableComponent] with updated settings.
     *
     * This is necessary when a parameter that cannot be changed dynamically on the existing
     * component, such as [scaleWithDistance], is updated. It temporarily removes and then re-adds
     * the component with the new configuration.
     */
    internal fun updateComponent() {
        disableComponent()
        enableComponent()
    }

    override fun onMoveStart(
        entity: Entity,
        initialInputRay: Ray,
        initialPose: Pose,
        initialScale: Float,
        initialParent: Entity,
    ) {
        val event =
            SpatialMoveEvent(
                type = SpatialMoveEventType.Start,
                pose = initialPose.convertMetersToPixels(density),
                scale = initialScale,
                size = currentLayoutSize,
                previousPose = initialPose.convertMetersToPixels(density),
                previousScale = initialScale,
            )

        initialDragPose = initialPose
        previousPose = initialPose
        previousScale = initialScale

        onMove?.invoke(event)
    }

    override fun onMoveUpdate(
        entity: Entity,
        currentInputRay: Ray,
        currentPose: Pose,
        currentScale: Float,
    ) {
        val event =
            SpatialMoveEvent(
                type = SpatialMoveEventType.Moving,
                pose = currentPose.convertMetersToPixels(density),
                scale = currentScale,
                size = currentLayoutSize,
                previousPose = previousPose.convertMetersToPixels(density),
                previousScale = previousScale,
            )

        previousPose = currentPose
        previousScale = currentScale

        onMove?.invoke(event)
    }

    override fun onMoveEnd(
        entity: Entity,
        finalInputRay: Ray,
        finalPose: Pose,
        finalScale: Float,
        updatedParent: Entity?,
    ) {
        val event =
            SpatialMoveEvent(
                type = SpatialMoveEventType.End,
                pose = finalPose.convertMetersToPixels(density),
                scale = finalScale,
                size = currentLayoutSize,
                previousPose = previousPose.convertMetersToPixels(density),
                previousScale = previousScale,
            )

        updatePoseOnFinalMoveEvent(initialDragPose, finalPose, finalScale)
        onMove?.invoke(event)

        initialDragPose = Pose.Identity
        previousPose = Pose.Identity
        previousScale = 1.0F
    }

    /**
     * Called at the finale of a move event to make sure the pose isn't lost in the layout, if this
     * CoreEntity is movable.
     */
    private fun updatePoseOnFinalMoveEvent(initialPose: Pose, nextPose: Pose, scale: Float) {
        if (!enabled) {
            return
        }
        // SceneCore uses meters, Compose XR uses pixels.
        val initialCorePose = initialPose.convertMetersToPixels(density)
        val corePose = nextPose.convertMetersToPixels(density)
        // Find the delta from the start of the move event.
        val coreDeltaPose =
            if (coreEntity !is CoreModelEntity) {
                Pose(
                    corePose.translation - initialCorePose.translation,
                    initialCorePose.rotation.inverse * corePose.rotation,
                )
            } else {
                Pose.Identity
            }
        userPose =
            Pose(
                userPose.translation + coreDeltaPose.translation,
                userPose.rotation * coreDeltaPose.rotation,
            )
        scaleFromMovement = scale
        // Make sure that the pose isn't lost when using system movement
        invalidatePlacement()
        invalidateCoreEntity()
    }

    companion object {
        private val MainExecutor: Executor = Dispatchers.Main.asExecutor()
    }
}
