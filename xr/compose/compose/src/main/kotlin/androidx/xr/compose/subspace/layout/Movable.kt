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
import androidx.xr.compose.unit.toIntVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * When the movable modifier is present and enabled, draggable UI controls will be shown that allow
 * the user to move the element in 3D space.
 *
 * There are some limitations that should be considered when using this modifier: 1) the draggable
 * UI controls of nested composables using the movable modifier may conflict with each other, 2)
 * when attaching multiple movable modifiers that handle movement internally, the movement effect
 * will be compounded.
 *
 * @param enabled true if this composable should be movable.
 * @param stickyPose if enabled, the user specified position will be retained when the modifier is
 *   disabled or removed.
 * @param scaleWithDistance true if this composable should scale in size when moved in depth. When
 *   this scaleWithDistance is enabled, the subspace element moved will grow or shrink. It will also
 *   maintain any explicit scale that it had before movement.
 * @param onMoveStart a callback to process the start of a move event. This will only be called if
 *   [enabled] is true.
 * @param onMoveEnd a callback to process the end of a move event. This will only be called if
 *   [enabled] is true.
 * @param onMove a callback to process the pose change during movement, with translation in pixels.
 *   This will only be called if [enabled] is true. If the callback returns false the default
 *   behavior of moving this composable's subspace hierarchy will be executed. If it returns true,
 *   it is the responsibility of the callback to process the event.
 * @sample androidx.xr.compose.samples.BasicMovableSample
 * @sample androidx.xr.compose.samples.CustomMovableSample
 * @see [SpatialMoveEvent].
 */
public fun SubspaceModifier.movable(
    enabled: Boolean = true,
    stickyPose: Boolean = false,
    scaleWithDistance: Boolean = true,
    onMoveStart: ((SpatialMoveEvent) -> Unit)? = null,
    onMoveEnd: ((SpatialMoveEvent) -> Unit)? = null,
    onMove: ((SpatialMoveEvent) -> Boolean)? = null,
): SubspaceModifier =
    this.then(
        MovableElement(
            enabled = enabled,
            onMoveStart = onMoveStart,
            onMoveEnd = onMoveEnd,
            onMove = onMove,
            stickyPose = stickyPose,
            scaleWithDistance = scaleWithDistance,
        )
    )

private class MovableElement(
    private val enabled: Boolean,
    private val onMoveStart: ((SpatialMoveEvent) -> Unit)?,
    private val onMoveEnd: ((SpatialMoveEvent) -> Unit)?,
    private val onMove: ((SpatialMoveEvent) -> Boolean)?,
    private val stickyPose: Boolean,
    private val scaleWithDistance: Boolean,
) : SubspaceModifierNodeElement<MovableNode>() {
    override fun create(): MovableNode =
        MovableNode(
            enabled = enabled,
            stickyPose = stickyPose,
            onMoveStart = onMoveStart,
            onMoveEnd = onMoveEnd,
            onMove = onMove,
            scaleWithDistance = scaleWithDistance,
        )

    override fun update(node: MovableNode) {
        val componentUpdateNeeded = node.scaleWithDistance != scaleWithDistance

        node.enabled = enabled
        node.onMoveStart = onMoveStart
        node.onMoveEnd = onMoveEnd
        node.onMove = onMove
        node.stickyPose = stickyPose
        node.scaleWithDistance = scaleWithDistance

        if (componentUpdateNeeded) {
            node.updateComponent()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MovableElement) return false
        if (enabled != other.enabled) return false
        if (onMoveStart !== other.onMoveStart) return false
        if (onMoveEnd !== other.onMoveEnd) return false
        if (onMove !== other.onMove) return false
        if (stickyPose != other.stickyPose) return false
        if (scaleWithDistance != other.scaleWithDistance) return false
        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + onMoveStart.hashCode()
        result = 31 * result + onMoveEnd.hashCode()
        result = 31 * result + onMove.hashCode()
        result = 31 * result + stickyPose.hashCode()
        result = 31 * result + scaleWithDistance.hashCode()
        return result
    }
}

internal class MovableNode(
    var enabled: Boolean,
    var stickyPose: Boolean,
    var scaleWithDistance: Boolean,
    var onMoveStart: ((SpatialMoveEvent) -> Unit)?,
    var onMoveEnd: ((SpatialMoveEvent) -> Unit)?,
    var onMove: ((SpatialMoveEvent) -> Boolean)?,
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

    /** The previous pose of this entity from the last MoveEvent. */
    private var previousPose: Pose = Pose.Identity
    /** Pose based on user adjustments from MoveEvents from SceneCore. */
    private var userPose: Pose = Pose.Identity
    /** The scale of this entity when it is moved. */
    private var scaleFromMovement: Float = 1.0F
    private var component: MovableComponent? = null

    override fun CoreEntityScope.modifyCoreEntity() {
        setOrAppendScale(scaleFromMovement)
    }

    override fun onDetach() {
        if (component != null) {
            disableComponent()
        }
    }

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        // modifyCoreEntity happens during placement, so we need to update the component state here
        // before measurement
        updateState()
        val placeable = measurable.measure(constraints)
        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            placeable.place(userPose)
        }
    }

    override fun onPlaced(coordinates: SubspaceLayoutCoordinates) {
        // Update the size of the component to match the final size of the layout.
        component?.size = coordinates.size.toDimensionsInMeters(density)
    }

    /** Updates the movable state of this CoreEntity. */
    private fun updateState() {
        // Enabled is on the Node. It means "should be enabled" for the Component.
        if (enabled && component == null) {
            enableComponent()
        } else if (!enabled && component != null) {
            disableComponent()
        }
    }

    /** Enables the MovableComponent and anchorPlacement for this CoreEntity. */
    private fun enableComponent() {
        check(component == null) { "MovableComponent already enabled." }
        component =
            MovableComponent.createCustomMovable(
                session = session,
                scaleInZ = scaleWithDistance,
                executor = MainExecutor,
                entityMoveListener = this,
            )

        coreEntity.onEntityAttached {
            check(component?.let { coreEntity.addComponent(it) } == true) {
                "Could not add MovableComponent to Core Entity."
            }
        }
    }

    /**
     * Disables the MovableComponent for this CoreEntity. Takes care of life cycle tasks for the
     * underlying component in SceneCore.
     *
     * @param keepUserPose When `true`, the current [userPose] is retained. When `false`the decision
     *   to retain the pose is determined by the [stickyPose] parameter configured in the modifier.
     */
    private fun disableComponent(keepUserPose: Boolean = false) {
        check(component != null) { "MovableComponent already disabled." }
        val preservePose = keepUserPose || stickyPose
        component?.removeMoveListener(this)
        component?.let { coreEntity.removeComponent(it) }
        component = null
        if (!preservePose) {
            userPose = Pose.Identity
        }
    }

    /**
     * Recreates the underlying [MovableComponent] with updated settings.
     *
     * This is necessary when a parameter that cannot be changed dynamically on the existing
     * component, such as [scaleWithDistance], is updated. It temporarily removes and then re-adds
     * the component with the new configuration.
     */
    internal fun updateComponent() {
        disableComponent(keepUserPose = true)
        enableComponent()
    }

    override fun onMoveStart(
        entity: Entity,
        initialInputRay: Ray,
        initialPose: Pose,
        initialScale: Float,
        initialParent: Entity,
    ) {
        previousPose = initialPose
        val initialSize: IntVolumeSize =
            when (entity) {
                is PanelEntity -> entity.size.to3d().toIntVolumeSize(density)
                else -> IntVolumeSize.Zero
            }
        val event =
            SpatialMoveEvent(
                pose = initialPose.convertMetersToPixels(density),
                scale = initialScale,
                size = initialSize,
            )
        onMoveStart?.invoke(event)
    }

    override fun onMoveUpdate(
        entity: Entity,
        currentInputRay: Ray,
        currentPose: Pose,
        currentScale: Float,
    ) {
        updatePoseOnMove(
            previousPose,
            currentPose,
            currentScale,
            when (entity) {
                is PanelEntity -> entity.size.to3d().toIntVolumeSize(density)
                else -> IntVolumeSize.Zero
            },
        )
        previousPose = currentPose
    }

    override fun onMoveEnd(
        entity: Entity,
        finalInputRay: Ray,
        finalPose: Pose,
        finalScale: Float,
        updatedParent: Entity?,
    ) {
        val finalSize: IntVolumeSize =
            when (entity) {
                is PanelEntity -> entity.size.to3d().toIntVolumeSize(density)
                else -> IntVolumeSize.Zero
            }
        val event =
            SpatialMoveEvent(
                pose = finalPose.convertMetersToPixels(density),
                scale = finalScale,
                size = finalSize,
            )
        onMoveEnd?.invoke(event)
        previousPose = Pose.Identity
    }

    /** Called every time there is a MoveEvent in SceneCore, if this CoreEntity is movable. */
    private fun updatePoseOnMove(
        previousPose: Pose,
        nextPose: Pose,
        scale: Float,
        size: IntVolumeSize,
    ) {
        if (!enabled) {
            return
        }
        // SceneCore uses meters, Compose XR uses pixels.
        val previousCorePose = previousPose.convertMetersToPixels(density)
        val corePose = nextPose.convertMetersToPixels(density)
        val spatialMoveEvent = SpatialMoveEvent(corePose, scale, size)
        if (onMove?.invoke(spatialMoveEvent) == true) {
            // We're done, the user app will handle the event.
            return
        }
        // Find the delta from the previous move event.
        val coreDeltaPose =
            Pose(
                corePose.translation - previousCorePose.translation,
                previousCorePose.rotation.inverse * corePose.rotation,
            )
        userPose =
            Pose(
                userPose.translation + coreDeltaPose.translation,
                userPose.rotation * coreDeltaPose.rotation,
            )
        scaleFromMovement = scale

        invalidatePlacement()
        invalidateCoreEntity()
    }

    companion object {
        private val MainExecutor: Executor = Dispatchers.Main.asExecutor()
    }
}

/**
 * An event representing a change in pose, scale, and size.
 *
 * @property pose The new pose of the composable in the subspace, relative to its parent, with its
 *   translation being expressed in pixels.
 * @property scale The scale of the composable as a result of its motion. This value will change
 *   with the composable's depth when scaleWithDistance is true on the modifier.
 * @property size The [IntVolumeSize] value that includes the width, height and depth of the
 *   composable, factoring in shrinking or stretching due to [scale].
 */
public class SpatialMoveEvent(
    public val pose: Pose,
    public val scale: Float,
    public val size: IntVolumeSize,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialMoveEvent) return false
        if (pose != other.pose) return false
        if (scale != other.scale) return false
        if (size != other.size) return false
        return true
    }

    override fun hashCode(): Int {
        var result = pose.hashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }

    override fun toString(): String {
        return "MoveEvent(pose=$pose, scale=$scale, size=$size)"
    }
}

/**
 * An event representing the start of a move event.
 *
 * This is expected to trigger when the user first starts moving the movable element and should only
 * be called once per move action.
 *
 * @property pose The initial pose of the composable in the subspace, relative to its parent, with
 *   its translation being expressed in pixels.
 * @property scale The initial scale of the composable as a result of its motion. This value will
 *   change with the composable's depth when scaleWithDistance is true on the modifier.
 * @property size The [IntVolumeSize] value that includes the width, height and depth of the
 *
 *   composable, factoring in shrinking or stretching due to [scale].
 */
@Deprecated(
    message = "Use SpatialMoveEvent instead",
    replaceWith = ReplaceWith("SpatialMoveEvent(pose = pose, scale = scale, size = size)"),
)
public typealias SpatialMoveStartEvent = SpatialMoveEvent

/**
 * An event representing the end of a move event.
 *
 * This is expected to trigger when the user finishes moving the movable element and should only be
 * called once per move action.
 *
 * @property pose The final pose of the composable in the subspace, relative to its parent, with its
 *   translation being expressed in pixels.
 * @property scale The final scale of the composable as a result of its motion. This value will
 *   change with the composable's depth when scaleWithDistance is true on the modifier.
 * @property size The [IntVolumeSize] value that includes the width, height and depth of the
 *   composable, factoring in shrinking or stretching due to [scale].
 */
@Deprecated(
    message = "Use SpatialMoveEvent instead",
    replaceWith = ReplaceWith("SpatialMoveEvent(pose = pose, scale = scale, size = size)"),
)
public typealias SpatialMoveEndEvent = SpatialMoveEvent
