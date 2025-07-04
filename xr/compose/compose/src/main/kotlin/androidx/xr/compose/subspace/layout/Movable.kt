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

import androidx.annotation.RestrictTo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.node.CompositionLocalConsumerSubspaceModifierNode
import androidx.xr.compose.subspace.node.LayoutCoordinatesAwareModifierNode
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.currentValueOf
import androidx.xr.compose.subspace.node.requestRelayout
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.compose.unit.toDimensionsInMeters
import androidx.xr.compose.unit.toIntVolumeSize
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.scenecore.AnchorPlacement
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PlaneOrientation as SceneCorePlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType as SceneCorePlaneSemantic
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * When the movable modifier is present and enabled, draggable UI controls will be shown that allow
 * the user to move the element in 3D space. This feature is only available for
 * [SpatialPanels][androidx.xr.compose.subspace.SpatialPanel] at the moment.
 *
 * @param enabled true if this composable should be movable.
 * @param stickyPose if enabled, the user specified position will be retained when the modifier is
 *   disabled or removed.
 * @param scaleWithDistance true if this composable should scale in size when moved in depth. When
 *   this scaleWithDistance is enabled, the subspace element moved will grow or shrink. It will also
 *   maintain any explicit scale that it had before movement.
 * @param onMoveStart a callback to process the start of a move event. This will only be called if
 *   [enabled] is true. The callback will be called with the [MoveStartEvent] type
 * @param onMoveEnd a callback to process the end of a move event. This will only be called if
 *   [enabled] is true. The callback will be called with the [MoveEndEvent] type
 * @param onMove a callback to process the pose change during movement, with translation in pixels.
 *   This will only be called if [enabled] is true. If the callback returns false the default
 *   behavior of moving this composable's subspace hierarchy will be executed. If it returns true,
 *   it is the responsibility of the callback to process the event. The callback will be called with
 *   the [MoveEvent] type.
 * @see [MoveEvent].
 */
public fun SubspaceModifier.movable(
    enabled: Boolean = true,
    stickyPose: Boolean = false,
    scaleWithDistance: Boolean = true,
    onMoveStart: ((MoveStartEvent) -> Unit)? = null,
    onMoveEnd: ((MoveEndEvent) -> Unit)? = null,
    onMove: ((MoveEvent) -> Boolean)? = null,
): SubspaceModifier =
    this.then(
        MovableElement(
            enabled,
            onMoveStart,
            onMoveEnd,
            onMove,
            stickyPose,
            scaleWithDistance,
            anchorPlaneOrientations = emptySet(),
            anchorPlaneSemantics = emptySet(),
        )
    )

/**
 * When the movable modifier is present and enabled, draggable UI controls will be shown that allow
 * the user to move the element in 3D space. This feature is only available for
 * [SpatialPanels][androidx.xr.compose.subspace.SpatialPanel] at the moment. This overload of the
 * modifier allows the element to be anchored to a plane in the real world.
 *
 * @param enabled true if this composable should be movable.
 * @param anchorPlaneOrientations when supplied, this movable entity can be anchored to Horizontal
 *   or Vertical planes or both (ANY). Can be used without anchorPlaneSemantics being supplied.
 * @param anchorPlaneSemantics when supplied, this movable entity can be anchored to planes which
 *   match one of the supplied list of semantic interpretations, such as a "table" or "floor". Can
 *   be used without anchorPlaneOrientations being supplied.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Suppress("PrimitiveInCollection")
public fun SubspaceModifier.anchorable(
    enabled: Boolean = true,
    anchorPlaneOrientations: Set<PlaneOrientation> = emptySet(),
    anchorPlaneSemantics: Set<PlaneSemantic> = emptySet(),
): SubspaceModifier =
    this.then(
        MovableElement(
            enabled,
            onMoveStart = null,
            onMoveEnd = null,
            onMove = null,
            stickyPose = true,
            scaleWithDistance = true,
            anchorPlaneOrientations,
            anchorPlaneSemantics,
        )
    )

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
public open class MoveEvent(
    public var pose: Pose = Pose.Identity,
    public var scale: Float = 1.0F,
    public var size: IntVolumeSize = IntVolumeSize.Zero,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MoveEvent) return false
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
 *   composable, factoring in shrinking or stretching due to [scale].
 */
public class MoveStartEvent(
    pose: Pose = Pose.Identity,
    scale: Float = 1.0F,
    size: IntVolumeSize = IntVolumeSize.Zero,
) : MoveEvent(pose, scale, size) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MoveStartEvent) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "OnMoveStart(pose=$pose, scale=$scale, size=$size)"
    }
}

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
public class MoveEndEvent(
    pose: Pose = Pose.Identity,
    scale: Float = 1.0F,
    size: IntVolumeSize = IntVolumeSize.Zero,
) : MoveEvent(pose, scale, size) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MoveEndEvent) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "OnMoveEnd(pose=$pose, scale=$scale, size=$size)"
    }
}

@Suppress("PrimitiveInCollection")
private class MovableElement(
    private val enabled: Boolean,
    private val onMoveStart: ((MoveStartEvent) -> Unit)?,
    private val onMoveEnd: ((MoveEndEvent) -> Unit)?,
    private val onMove: ((MoveEvent) -> Boolean)?,
    private val stickyPose: Boolean,
    private val scaleWithDistance: Boolean,
    private val anchorPlaneOrientations: Set<PlaneOrientation> = emptySet(),
    private val anchorPlaneSemantics: Set<PlaneSemantic> = emptySet(),
) : SubspaceModifierNodeElement<MovableNode>() {
    override fun create(): MovableNode =
        MovableNode(
            enabled = enabled,
            stickyPose = stickyPose,
            onMoveStart = onMoveStart,
            onMoveEnd = onMoveEnd,
            onMove = onMove,
            scaleWithDistance = scaleWithDistance,
            anchorPlaneOrientations = anchorPlaneOrientations,
            anchorPlaneSemantics = anchorPlaneSemantics,
        )

    override fun update(node: MovableNode) {
        node.enabled = enabled
        node.onMoveStart = onMoveStart
        node.onMoveEnd = onMoveEnd
        node.onMove = onMove
        node.stickyPose = stickyPose
        node.scaleWithDistance = scaleWithDistance
        node.anchorPlaneOrientations = anchorPlaneOrientations
        node.anchorPlaneSemantics = anchorPlaneSemantics
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
        if (anchorPlaneOrientations != other.anchorPlaneOrientations) return false
        if (anchorPlaneSemantics != other.anchorPlaneSemantics) return false
        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + onMoveStart.hashCode()
        result = 31 * result + onMoveEnd.hashCode()
        result = 31 * result + onMove.hashCode()
        result = 31 * result + stickyPose.hashCode()
        result = 31 * result + scaleWithDistance.hashCode()
        result = 31 * result + anchorPlaneOrientations.hashCode()
        result = 31 * result + anchorPlaneSemantics.hashCode()
        return result
    }
}

@Suppress("PrimitiveInCollection")
internal class MovableNode(
    public var enabled: Boolean,
    public var stickyPose: Boolean,
    public var scaleWithDistance: Boolean,
    public var onMoveStart: ((MoveStartEvent) -> Unit)?,
    public var onMoveEnd: ((MoveEndEvent) -> Unit)?,
    public var onMove: ((MoveEvent) -> Boolean)?,
    public var anchorPlaneOrientations: Set<PlaneOrientation> = emptySet(),
    public var anchorPlaneSemantics: Set<PlaneSemantic> = emptySet(),
) :
    SubspaceModifier.Node(),
    CompositionLocalConsumerSubspaceModifierNode,
    CoreEntityNode,
    LayoutCoordinatesAwareModifierNode,
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

    override fun onLayoutCoordinates(coordinates: SubspaceLayoutCoordinates) {
        // Update the size of the component to match the final size of the layout.
        component?.size = coordinates.size.toDimensionsInMeters(density)
    }

    /** Updates the movable state of this CoreEntity. */
    private fun updateState() {
        if (coreEntity !is MovableCoreEntity) {
            return
        }
        // Enabled is on the Node. It means "should be enabled" for the Component.
        if (enabled && component == null) {
            enableComponent()
        } else if (!enabled && component != null) {
            disableComponent()
        }
    }

    /** Enables the MovableComponent for this CoreEntity. */
    private fun enableComponent() {
        check(component == null) { "MovableComponent already enabled." }
        val anchorPlacement = convertToAnchorPlacement()

        if (!anchorPlacement.isEmpty())
            session.configure(
                Config(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
            )

        // The developer could have used the movable overload which allows them to supply their own
        // onMove logic, or they could have used the overload which provides the anchoring ability
        // (where the movement is managed by the system).
        if (anchorPlacement.isEmpty()) {
            component =
                MovableComponent.createCustomMovable(
                    session = session,
                    scaleInZ = true,
                    executor = MainExecutor,
                    entityMoveListener = this,
                )
        } else {
            component = MovableComponent.createAnchorable(session, anchorPlacement)
            component?.addMoveListener(MainExecutor, this)
        }
        check(component?.let { coreEntity.addComponent(it) } == true) {
            "Could not add MovableComponent to Core Entity."
        }
    }

    /**
     * Takes the param values PlaneOrientation and PlaneSemantic, and returns the SceneCore
     * equivalent, which is bundled in a structure called AnchorPlacement.
     *
     * The lint error was suppressed because the function being called requires a set of ints.
     */
    @Suppress("PrimitiveInCollection")
    private fun convertToAnchorPlacement(): Set<AnchorPlacement> {
        if (anchorPlaneOrientations.isEmpty() && anchorPlaneSemantics.isEmpty())
            return mutableSetOf()

        val planeTypeFilter: MutableSet<Int> = mutableSetOf()
        anchorPlaneOrientations.forEach { planeTypeFilter.add(it.value) }
        if (planeTypeFilter.isEmpty()) planeTypeFilter.add(SceneCorePlaneOrientation.ANY)

        val planeSemanticFilter: MutableSet<Int> = mutableSetOf()
        anchorPlaneSemantics.forEach { planeSemanticFilter.add(it.value) }
        if (planeSemanticFilter.isEmpty()) planeSemanticFilter.add(SceneCorePlaneSemantic.ANY)

        return mutableSetOf(AnchorPlacement.createForPlanes(planeTypeFilter, planeSemanticFilter))
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
        if (!stickyPose) {
            userPose = Pose.Identity
        }
    }

    override fun onMoveStart(
        entity: Entity,
        initialInputRay: Ray,
        initialPose: Pose,
        initialScale: Float,
        initialParent: Entity,
    ) {
        previousPose = initialPose
        updatePoseOnMove(previousPose, initialPose, initialScale, IntVolumeSize.Zero) {
            pose,
            scale,
            size ->
            MoveStartEvent(pose, scale, size).also { onMoveStart?.invoke(it) }
        }
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
            ::MoveEvent,
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
        updatePoseOnMove(previousPose, finalPose, finalScale, finalSize) { pose, scale, size ->
            MoveEndEvent(pose, scale, size).also { onMoveEnd?.invoke(it) }
        }
        previousPose = Pose.Identity
    }

    /** Called every time there is a MoveEvent in SceneCore, if this CoreEntity is movable. */
    private fun updatePoseOnMove(
        previousPose: Pose,
        nextPose: Pose,
        scale: Float,
        size: IntVolumeSize,
        eventCreator: (pose: Pose, scale: Float, size: IntVolumeSize) -> MoveEvent,
    ) {
        if (!enabled) {
            return
        }
        // SceneCore uses meters, Compose XR uses pixels.
        val previousCorePose = previousPose.convertMetersToPixels(density)
        val corePose = nextPose.convertMetersToPixels(density)
        val moveEvent = eventCreator(corePose, scale, size)
        if (onMove?.invoke(moveEvent) == true) {
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
        if (scaleWithDistance) {
            scaleFromMovement = scale
        }
        requestRelayout()
    }

    public companion object {
        private val MainExecutor: Executor = Dispatchers.Main.asExecutor()
    }
}

/** Type of plane based on orientation i.e. Horizontal or Vertical. */
@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public value class PlaneOrientation private constructor(internal val value: Int) {
    public companion object {
        public val Horizontal: PlaneOrientation =
            PlaneOrientation(SceneCorePlaneOrientation.HORIZONTAL)
        public val Vertical: PlaneOrientation = PlaneOrientation(SceneCorePlaneOrientation.VERTICAL)
        public val Any: PlaneOrientation = PlaneOrientation(SceneCorePlaneOrientation.ANY)
    }
}

/** Semantic plane types. */
@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public value class PlaneSemantic private constructor(internal val value: Int) {
    public companion object {
        public val Wall: PlaneSemantic = PlaneSemantic(SceneCorePlaneSemantic.WALL)
        public val Floor: PlaneSemantic = PlaneSemantic(SceneCorePlaneSemantic.FLOOR)
        public val Ceiling: PlaneSemantic = PlaneSemantic(SceneCorePlaneSemantic.CEILING)
        public val Table: PlaneSemantic = PlaneSemantic(SceneCorePlaneSemantic.TABLE)
        public val Any: PlaneSemantic = PlaneSemantic(SceneCorePlaneSemantic.ANY)
    }
}
