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

import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
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
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.scenecore.AnchorPlacement
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PixelDensity
import androidx.xr.scenecore.PlaneOrientation as SceneCorePlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType as SceneCorePlaneSemantic
import androidx.xr.scenecore.scene
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * Configures this subspace element to be interactive and movable.
 *
 * When this modifier is present and enabled, draggable UI controls will be shown that allow the
 * user to move the element in 3D space. The specific behavior of this movement—such as whether the
 * system automatically applies the transformation, leaves it to the developer, or anchors it to
 * physical surfaces—is defined by the provided [MovePolicy]. Input events used for moving in this
 * way are consumed.
 *
 * There are some limitations that should be considered when using this modifier:
 * 1) the draggable UI controls of nested composables using the [movable] modifier may conflict with
 *    each other.
 * 2) Attaching multiple [movable] modifiers with auto-applying policies (like [MovePolicy.default])
 *    to the same element will compound the movement distance, since each modifier independently
 *    applies the drag offset upon release.
 *
 * @param enabled true if this composable should be movable. Setting this to false will remove the
 *   interactable affordance associated with the content. Disabling the modifier after movement
 *   keeps the composable at its last dragged position. Removing the modifier entirely resets the
 *   composable to its original layout position.
 * @param movePolicy The [MovePolicy] that dictates how movement transformations are calculated and
 *   applied. Defaults to [MovePolicy.default].
 *
 *     @sample androidx.xr.compose.samples.BasicMovableSample
 *     @sample androidx.xr.compose.samples.CustomMovableSample
 */
public fun SubspaceModifier.movable(
    enabled: Boolean = true,
    movePolicy: MovePolicy = MovePolicy.default(),
): SubspaceModifier = this.then(MovableElement(enabled = enabled, movePolicy = movePolicy))

/** Defines the behavior and configuration for movement applied by the [movable] modifier. */
public sealed interface MovePolicy {
    public companion object {

        /**
         * A policy that delegates the pose transformation entirely to the system.
         *
         * The system intercepts spatial input events, calculates the resulting [Pose] and scale,
         * and automatically applies these transformations to the element's layout. This is the
         * standard behavior where a 1:1 transformation is desired, and custom gesture handling or
         * manual state management is not required. This policy will have a better overall
         * performance than the custom policy.
         *
         * @param scaleWithDistance true if this composable should scale in size when moved in
         *   depth. When enabled, the subspace element will grow if pushed away from the user or
         *   shrink when pulled toward the user in order to maintain the interactability and
         *   legibility of the panel. Scaling with distance respects other transformations applied
         *   to this layout.
         * @param onMove Optional observer callback invoked during the manipulation. Since the
         *   system automatically applies the move, this callback is strictly for monitoring changes
         *   and should not control the position. The [onMove] callback values can be used to
         *   position other sibling composables with the offset modifier. This callback reports a
         *   [SpatialMoveEvent] which will contain a [Pose]. The [Pose] contained in this event is
         *   the sum of all previous events in the move gesture.
         */
        public fun default(
            scaleWithDistance: Boolean = true,
            onMove: ((SpatialMoveEvent) -> Unit)? = null,
        ): MovePolicy = DefaultMovePolicy(scaleWithDistance = scaleWithDistance, onMove = onMove)

        /**
         * A policy that accepts move events and reports the calculated pose updates via a callback,
         * without automatically applying a resulting transformation.
         *
         * This policy enables custom behavior for movement of the content. The system calculates
         * the target [Pose] based on input, but does not automatically apply it to the associated
         * layout. The developer is responsible for consuming the [onMove] event and applying the
         * result (e.g., by updating a state backed by [SubspaceModifier.offset]). Using this policy
         * has higher latency than [default].
         *
         * @param scaleWithDistance true if this composable should scale in size when moved in
         *   depth. When enabled, the subspace element will grow if pushed away from the user or
         *   shrink when pulled toward the user in order to maintain the interactability and
         *   legibility of the panel. Scaling with distance respects other transformations applied
         *   to this layout.
         * @param onMove callback invoked continuously during the interaction that receives a
         *   [SpatialMoveEvent] containing the calculated target pose, scale, and size. The pose
         *   contained in this event is the sum of all previous events in the move gesture. The
         *   receiver MUST use this data to update the element's external state to reflect movement.
         */
        public fun custom(
            scaleWithDistance: Boolean = false,
            onMove: ((SpatialMoveEvent) -> Unit),
        ): MovePolicy = CustomMovePolicy(scaleWithDistance = scaleWithDistance, onMove = onMove)

        /**
         * A policy that enables anchoring the movable element to detected real-world planes.
         *
         * Using this policy allows the user to snap the subspace element to physical surfaces (like
         * tables or walls) that match the provided orientations and semantics. If no specific
         * orientations or semantics are provided (i.e., the sets are empty), the element is
         * permitted to anchor to any detected plane.
         *
         * @param anchorPlaneOrientations The set of [PlaneOrientation]s (e.g., Horizontal,
         *   Vertical) that the element is permitted to anchor to. Defaults to an empty set, which
         *   allows all orientations.
         * @param anchorPlaneSemantics The set of [PlaneSemantic]s (e.g., Wall, Floor, Table) that
         *   the element is permitted to anchor to. Defaults to an empty set, which allows all
         *   semantics.
         * @param onMove Optional observer callback invoked during the manipulation to monitor the
         *   movement and anchoring events.
         */
        // TODO (b/522261084) - Optimize anchor policy.
        @OptIn(ExperimentalMoveAnchorPolicy::class)
        @ExperimentalMoveAnchorPolicy
        public fun anchor(
            anchorPlaneOrientations: Set<PlaneOrientation> = emptySet(),
            anchorPlaneSemantics: Set<PlaneSemantic> = emptySet(),
            onMove: ((SpatialMoveEvent) -> Unit)? = null,
        ): MovePolicy =
            Anchor(
                anchorPlaneOrientations = anchorPlaneOrientations,
                anchorPlaneSemantics = anchorPlaneSemantics,
                onMove = onMove,
            )
    }
}

internal data class DefaultMovePolicy(
    val scaleWithDistance: Boolean,
    val onMove: ((SpatialMoveEvent) -> Unit)?,
) : MovePolicy

internal data class CustomMovePolicy(
    val scaleWithDistance: Boolean,
    val onMove: (SpatialMoveEvent) -> Unit,
) : MovePolicy

internal data class Anchor(
    val anchorPlaneOrientations: Set<PlaneOrientation>,
    val anchorPlaneSemantics: Set<PlaneSemantic>,
    val onMove: ((SpatialMoveEvent) -> Unit)?,
) : MovePolicy

private class MovableElement(val enabled: Boolean, val movePolicy: MovePolicy) :
    SubspaceModifierNodeElement<MovableNode>() {

    override fun create(): MovableNode = MovableNode(enabled = enabled, movePolicy = movePolicy)

    override fun update(node: MovableNode) {
        node.updateNode(enabled = enabled, movePolicy = movePolicy)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MovableElement

        if (enabled != other.enabled) return false
        if (movePolicy != other.movePolicy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + movePolicy.hashCode()
        return result
    }

    override fun toString(): String {
        return "MovableElement(enabled=$enabled, movePolicy=$movePolicy)"
    }
}

internal class MovableNode(var enabled: Boolean, var movePolicy: MovePolicy) :
    SubspaceModifier.Node(),
    CompositionLocalConsumerSubspaceModifierNode,
    CoreEntityNode,
    SubspaceLayoutAwareModifierNode,
    SubspaceLayoutModifierNode,
    EntityMoveListener {

    private inline val density: Density
        get() = currentValueOf(LocalDensity)

    private inline val session: Session
        get() = checkNotNull(currentValueOf(LocalSession)) { "Movable requires a Session." }

    private inline val pixelDensity: PixelDensity
        get() = session.scene.virtualPixelDensity

    private var component: MovableComponent? = null

    /** The current layout size of this entity, captured during placement. */
    private var currentLayoutSize: IntVolumeSize = IntVolumeSize.Zero

    /** The previous pose of this entity from the last MoveEvent. */
    private var previousPose: Pose = Pose.Identity

    /** The previous scale of this entity from the last MoveEvent. */
    private var previousScale: Float = 1.0F

    /** The scale of this entity when it is moved. */
    private var scaleFromMovement: Float = 1.0F

    /** Pose based on user adjustments from MoveEvents from SceneCore. */
    private var layoutNodeFromDraggedNodePixels: Pose = Pose.Identity

    override fun CoreEntityScope.modifyCoreEntity() {
        setOrAppendScale(scaleFromMovement)
    }

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            placeable.place(layoutNodeFromDraggedNodePixels)
        }
    }

    internal fun updateNode(enabled: Boolean, movePolicy: MovePolicy) {
        // Check if the underlying type of the policy changed (e.g., Default to Custom)
        val policyTypeChanged = this.movePolicy::class != movePolicy::class

        // Only require a component recreation if the type changed, or if structural properties
        // changed.
        // We explicitly ignore the 'onMove' lambdas here so they can be updated freely without
        // recreation.
        val componentUpdateNeeded =
            policyTypeChanged ||
                when (movePolicy) {
                    is DefaultMovePolicy -> {
                        movePolicy.scaleWithDistance !=
                            (this.movePolicy as DefaultMovePolicy).scaleWithDistance
                    }
                    is CustomMovePolicy -> {
                        movePolicy.scaleWithDistance !=
                            (this.movePolicy as CustomMovePolicy).scaleWithDistance
                    }
                    is Anchor -> {
                        val oldAnchor = this.movePolicy as Anchor
                        movePolicy.anchorPlaneOrientations != oldAnchor.anchorPlaneOrientations ||
                            movePolicy.anchorPlaneSemantics != oldAnchor.anchorPlaneSemantics
                    }
                }
        this.enabled = enabled
        this.movePolicy = movePolicy

        if (componentUpdateNeeded && component != null) {
            disableComponent()
            enableComponent()
        } else {
            updateState() // handles standard enable/disable toggling
        }
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
        component?.size = coordinates.size.toDimensionsInMeters(pixelDensity)
        // Update the cached layout size of the composable.
        currentLayoutSize = coordinates.size
    }

    /** Enables the MovableComponent and anchorPlacement for this CoreEntity. */
    private fun enableComponent() {
        check(component == null) { "MovableComponent already enabled." }

        when (movePolicy) {
            is DefaultMovePolicy -> {
                component =
                    MovableComponent.createSystemMovable(
                            session = session,
                            scaleInZ = (movePolicy as DefaultMovePolicy).scaleWithDistance,
                        )
                        .also { it.addMoveListener(MainExecutor, this) }
            }
            is CustomMovePolicy -> {
                component =
                    MovableComponent.createCustomMovable(
                        session = session,
                        scaleInZ = (movePolicy as CustomMovePolicy).scaleWithDistance,
                        executor = MainExecutor,
                        entityMoveListener = this,
                    )
            }
            is Anchor -> {
                if (session.config.planeTracking == PlaneTrackingMode.DISABLED) {
                    return
                }

                val anchorPlacement =
                    convertToAnchorPlacement(
                        anchorPlaneSemantics = (movePolicy as Anchor).anchorPlaneSemantics,
                        anchorPlaneOrientations = (movePolicy as Anchor).anchorPlaneOrientations,
                    )

                if (
                    ContextCompat.checkSelfPermission(
                        currentValueOf(LocalContext),
                        SCENE_UNDERSTANDING_COARSE,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                component =
                    MovableComponent.createAnchorable(session, anchorPlacement = anchorPlacement)
            }
        }
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
     * Takes the param values PlaneOrientation and PlaneSemantic, and returns the SceneCore
     * equivalent, which is bundled in a structure called AnchorPlacement.
     *
     * The lint error was suppressed because the function being called requires a set of ints.
     */
    @Suppress("PrimitiveInCollection")
    private fun convertToAnchorPlacement(
        anchorPlaneSemantics: Set<PlaneSemantic>,
        anchorPlaneOrientations: Set<androidx.xr.compose.subspace.layout.PlaneOrientation>,
    ): Set<AnchorPlacement> {
        // If no orientations provided, allow ALL. Otherwise, map the provided ones.
        val planeTypeFilter =
            if (anchorPlaneOrientations.isEmpty()) {
                SceneCorePlaneOrientation.ALL
            } else {
                anchorPlaneOrientations.flatMapTo(mutableSetOf()) { it.value }
            }

        // If no semantics provided, allow ALL. Otherwise, map the provided ones.
        val planeSemanticFilter =
            if (anchorPlaneSemantics.isEmpty()) {
                SceneCorePlaneSemantic.ALL
            } else {
                anchorPlaneSemantics.flatMapTo(mutableSetOf()) { it.value }
            }

        return setOf(AnchorPlacement.createForPlanes(planeTypeFilter, planeSemanticFilter))
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
                pose = initialPose.metersToPx(pixelDensity),
                scale = initialScale,
                size = currentLayoutSize,
                previousPose = initialPose.metersToPx(pixelDensity),
                previousScale = initialScale,
            )

        previousPose = initialPose
        previousScale = initialScale
        when (val policy = movePolicy) {
            is DefaultMovePolicy -> {
                layoutNode?.markSystemMoveOngoing(true)
                policy.onMove?.invoke(event)
            }

            is CustomMovePolicy -> {
                policy.onMove.invoke(event)
            }

            is Anchor -> {
                policy.onMove?.invoke(event)
            }
        }
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
                pose = currentPose.metersToPx(pixelDensity),
                scale = currentScale,
                size = currentLayoutSize,
                previousPose = previousPose.metersToPx(pixelDensity),
                previousScale = previousScale,
            )

        when (val policy = movePolicy) {
            is DefaultMovePolicy -> {
                updatePoseOnMoveEvent(
                    parentFromDraggedNodeMeters = currentPose,
                    scale = currentScale,
                )
                previousPose = currentPose
                previousScale = currentScale
                policy.onMove?.invoke(event)
            }
            is CustomMovePolicy -> {
                previousPose = currentPose
                previousScale = currentScale
                policy.onMove.invoke(event)
            }
            is Anchor -> {
                previousPose = currentPose
                previousScale = currentScale
                policy.onMove?.invoke(event)
            }
        }
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
                pose = finalPose.metersToPx(pixelDensity),
                scale = finalScale,
                size = currentLayoutSize,
                previousPose = previousPose.metersToPx(pixelDensity),
                previousScale = previousScale,
            )

        when (val policy = movePolicy) {
            is DefaultMovePolicy -> {
                updatePoseOnMoveEvent(parentFromDraggedNodeMeters = finalPose, scale = finalScale)
                policy.onMove?.invoke(event)
                layoutNode?.markSystemMoveOngoing(false)
                previousPose = Pose.Identity
                previousScale = 1.0F
            }
            is CustomMovePolicy -> {
                previousPose = Pose.Identity
                previousScale = 1.0F
                policy.onMove.invoke(event)
            }
            is Anchor -> {
                previousPose = Pose.Identity
                previousScale = 1.0F
                policy.onMove?.invoke(event)
            }
        }
    }

    /**
     * Called during and at the finale of a move event to make sure the pose isn't lost in the
     * layout, if this CoreEntity is movable.
     */
    private fun updatePoseOnMoveEvent(parentFromDraggedNodeMeters: Pose, scale: Float) {
        if (!enabled) {
            return
        }

        // SceneCore uses meters, Compose XR uses pixels
        val parentFromDraggedNodePixels = parentFromDraggedNodeMeters.metersToPx(pixelDensity)
        val parentFromLayoutNodePixels = node.coordinator?.poseInParent ?: Pose.Identity
        val layoutNodeFromParentPixels = parentFromLayoutNodePixels.inverse
        layoutNodeFromDraggedNodePixels =
            layoutNodeFromParentPixels.compose(parentFromDraggedNodePixels)
        scaleFromMovement = scale

        // Make sure that the pose isn't lost when using system movement
        invalidatePlacement()
        invalidateCoreEntity()
    }

    companion object {
        private val MainExecutor: Executor = Dispatchers.Main.asExecutor()
    }
}

/**
 * Configures this subspace element to accept move events and report the calculated pose updates via
 * a callback, without automatically applying a resulting transformation.
 *
 * When the movable modifier is present and enabled, draggable UI controls will be shown that allow
 * the user to move the element in 3D space. This modifier enables custom behavior for movement of
 * the content. The system calculates the target [Pose] based on input, but does not automatically
 * apply it to the associated layout. The developer is responsible for consuming the [onMove] event
 * and applying the result. (e.g., by updating a state backed by [SubspaceModifier.offset])
 *
 * There are some limitations that should be considered when using this modifier: 1) the draggable
 * UI controls of nested composables using the [transformingMovable] modifier and [movable] modifier
 * may conflict with each other, 2) It cannot be used with the following composables
 * [androidx.xr.compose.subspace.SpatialExternalSurfaceHemisphere] and
 * [androidx.xr.compose.subspace.SpatialExternalSurfaceSphere] due to their similarity with the
 * system environment and not having any layout size, 3) If this element has animations that affect
 * its layout properties (e.g., offset), these animations should be stopped when a move gesture
 * starts (detected via the [onMove] callback with [SpatialMoveEventType.Start]) to prevent
 * rendering jitter, and can be resumed when the gesture ends ([SpatialMoveEventType.End]).
 *
 * @param enabled true if this composable should be movable. Setting this to false will remove the
 *   interactable affordance associated with the content. Disabling the modifier after movement
 *   keeps the composable at its last dragged position. Removing the modifier entirely resets the
 *   composable to its original layout position.
 * @param scaleWithDistance true if this composable should scale in size when moved in depth. When
 *   enabled, the subspace element will grow if pushed away from the user or shrink when pulled
 *   toward the user in order to maintain the interact-ability and legibility of the panel. Scaling
 *   with distance respects other transformations applied to this layout.
 * @param onMove callback invoked continuously during the interaction that receives a
 *   [SpatialMoveEvent] containing the calculated target pose, scale, and size. The pose contained
 *   in this event is the sum of all previous events in the move gesture. The receiver MUST use this
 *   data to update the element's external state to reflect movement.
 * @see transformingMovable for implementing system controlled movement.
 */
@Deprecated(
    message = "This signature is deprecated. Use movable with MovePolicy.custom instead.",
    replaceWith =
        ReplaceWith(
            "movable(enabled = enabled, movePolicy = MovePolicy.custom(scaleWithDistance = scaleWithDistance, onMove = onMove))",
            "androidx.xr.compose.subspace.layout.MovePolicy",
        ),
)
public fun SubspaceModifier.movable(
    enabled: Boolean = true,
    scaleWithDistance: Boolean = true,
    onMove: ((SpatialMoveEvent) -> Unit),
): SubspaceModifier = this.then(DeprecatedCustomMovableElement(enabled, scaleWithDistance, onMove))

private class DeprecatedCustomMovableElement(
    private val enabled: Boolean,
    private val scaleWithDistance: Boolean,
    private val onMove: ((SpatialMoveEvent) -> Unit),
) : SubspaceModifierNodeElement<DeprecatedCustomMovableNode>() {
    override fun create(): DeprecatedCustomMovableNode =
        DeprecatedCustomMovableNode(
            enabled = enabled,
            scaleWithDistance = scaleWithDistance,
            onMove = onMove,
        )

    override fun update(node: DeprecatedCustomMovableNode) {
        node.enabled = enabled
        node.scaleWithDistance = scaleWithDistance
        node.onMove = onMove
        node.updateState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeprecatedCustomMovableElement

        if (enabled != other.enabled) return false
        if (scaleWithDistance != other.scaleWithDistance) return false
        if (onMove !== other.onMove) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + scaleWithDistance.hashCode()
        result = 31 * result + onMove.hashCode()
        return result
    }
}

private class DeprecatedCustomMovableNode(
    var enabled: Boolean,
    var scaleWithDistance: Boolean,
    var onMove: (SpatialMoveEvent) -> Unit,
) :
    SubspaceModifier.Node(),
    CompositionLocalConsumerSubspaceModifierNode,
    CoreEntityNode,
    SubspaceLayoutAwareModifierNode,
    EntityMoveListener {

    private inline val density: Density
        get() = currentValueOf(LocalDensity)

    private inline val session: Session
        get() = checkNotNull(currentValueOf(LocalSession)) { "Movable requires a Session." }

    private inline val pixelDensity: PixelDensity
        get() = session.scene.virtualPixelDensity

    private var component: MovableComponent? = null

    /** The previous pose of this entity from the last MoveEvent. */
    private var previousPose: Pose = Pose.Identity

    /** The previous scale of this entity from the last MoveEvent. */
    private var previousScale: Float = 1.0F
    /** The current layout size of this entity, captured during placement. */
    private var currentLayoutSize: IntVolumeSize = IntVolumeSize.Zero

    override fun onAttach() {
        super.onAttach()
        updateState()
    }

    override fun CoreEntityScope.modifyCoreEntity() {}

    override fun onDetach() {
        if (component != null) {
            disableComponent()
        }
    }

    override fun onPlaced(coordinates: SubspaceLayoutCoordinates) {
        // Update the size of the component to match the final size of the layout.
        component?.size = coordinates.size.toDimensionsInMeters(pixelDensity)
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
                pose = initialPose.metersToPx(pixelDensity),
                previousPose = initialPose.metersToPx(pixelDensity),
                scale = initialScale,
                previousScale = initialScale,
                size = currentLayoutSize,
            )
        previousPose = initialPose
        previousScale = initialScale
        onMove.invoke(event)
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
                pose = currentPose.metersToPx(pixelDensity),
                previousPose = previousPose.metersToPx(pixelDensity),
                scale = currentScale,
                previousScale = previousScale,
                size = currentLayoutSize,
            )
        previousPose = currentPose
        previousScale = currentScale
        onMove.invoke(event)
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
                pose = finalPose.metersToPx(pixelDensity),
                previousPose = previousPose.metersToPx(pixelDensity),
                scale = finalScale,
                previousScale = previousScale,
                size = currentLayoutSize,
            )
        onMove.invoke(event)
        previousPose = Pose.Identity
        previousScale = 1.0F
    }

    companion object {
        private val MainExecutor: Executor = Dispatchers.Main.asExecutor()
    }
}

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
@Deprecated(
    message =
        "This signature is deprecated. The movement behavior is now configured via MovePolicy. " +
            "For default system-handled movement, use movable(movePolicy = MovePolicy.default(...)). " +
            "For custom movement where you manually apply the resulting pose, use movable(movePolicy = MovePolicy.custom(...))."
)
public fun SubspaceModifier.movable(
    enabled: Boolean = true,
    stickyPose: Boolean = false,
    scaleWithDistance: Boolean = true,
    onMoveStart: ((SpatialMoveEvent) -> Unit)? = null,
    onMoveEnd: ((SpatialMoveEvent) -> Unit)? = null,
    onMove: ((SpatialMoveEvent) -> Boolean)? = null,
): SubspaceModifier =
    this.then(
        DeprecatedMovableElement(
            enabled = enabled,
            onMoveStart = onMoveStart,
            onMoveEnd = onMoveEnd,
            onMove = onMove,
            stickyPose = stickyPose,
            scaleWithDistance = scaleWithDistance,
        )
    )

private class DeprecatedMovableElement(
    private val enabled: Boolean,
    private val onMoveStart: ((SpatialMoveEvent) -> Unit)?,
    private val onMoveEnd: ((SpatialMoveEvent) -> Unit)?,
    private val onMove: ((SpatialMoveEvent) -> Boolean)?,
    private val stickyPose: Boolean,
    private val scaleWithDistance: Boolean,
) : SubspaceModifierNodeElement<DeprecatedMovableNode>() {
    override fun create(): DeprecatedMovableNode =
        DeprecatedMovableNode(
            enabled = enabled,
            stickyPose = stickyPose,
            onMoveStart = onMoveStart,
            onMoveEnd = onMoveEnd,
            onMove = onMove,
            scaleWithDistance = scaleWithDistance,
        )

    override fun update(node: DeprecatedMovableNode) {
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
        if (other !is DeprecatedMovableElement) return false
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

internal class DeprecatedMovableNode(
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

    private inline val pixelDensity: PixelDensity
        get() = session.scene.virtualPixelDensity

    /** The previous pose of this entity from the last MoveEvent. */
    private var previousPose: Pose = Pose.Identity

    /** The previous scale of this entity from the last MoveEvent. */
    private var previousScale: Float = 1.0F

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
        return layout(placeable.width, placeable.height, placeable.depth) {
            placeable.place(userPose)
        }
    }

    override fun onPlaced(coordinates: SubspaceLayoutCoordinates) {
        // Update the size of the component to match the final size of the layout.
        component?.size = coordinates.size.toDimensionsInMeters(pixelDensity)
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
        previousScale = initialScale
        val initialSize: IntVolumeSize =
            when (entity) {
                is PanelEntity -> entity.size.to3d().toIntVolumeSize(pixelDensity)
                else -> IntVolumeSize.Zero
            }
        val event =
            SpatialMoveEvent(
                type = SpatialMoveEventType.Start,
                pose = initialPose.metersToPx(pixelDensity),
                previousPose = initialPose.metersToPx(pixelDensity),
                scale = initialScale,
                previousScale = initialScale,
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
            previousScale,
            currentScale,
            when (entity) {
                is PanelEntity -> entity.size.to3d().toIntVolumeSize(pixelDensity)
                else -> IntVolumeSize.Zero
            },
        )
        previousPose = currentPose
        previousScale = currentScale
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
                is PanelEntity -> entity.size.to3d().toIntVolumeSize(pixelDensity)
                else -> IntVolumeSize.Zero
            }
        val event =
            SpatialMoveEvent(
                type = SpatialMoveEventType.End,
                pose = finalPose.metersToPx(pixelDensity),
                previousPose = previousPose.metersToPx(pixelDensity),
                scale = finalScale,
                previousScale = previousScale,
                size = finalSize,
            )
        onMoveEnd?.invoke(event)
        previousPose = Pose.Identity
        previousScale = 1.0F
    }

    /** Called every time there is a MoveEvent in SceneCore, if this CoreEntity is movable. */
    private fun updatePoseOnMove(
        previousPose: Pose,
        nextPose: Pose,
        previousScale: Float,
        scale: Float,
        size: IntVolumeSize,
    ) {
        if (!enabled) {
            return
        }
        // SceneCore uses meters, Compose XR uses pixels.
        val previousCorePose = previousPose.metersToPx(pixelDensity)
        val corePose = nextPose.metersToPx(pixelDensity)
        val spatialMoveEvent =
            SpatialMoveEvent(
                type = SpatialMoveEventType.Moving,
                pose = corePose,
                previousPose = previousCorePose,
                scale = scale,
                previousScale = previousScale,
                size = size,
            )
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
 *   composable, factoring in shrinking or stretching due to [SpatialMoveEvent.scale].
 */
@Deprecated(message = "Use SpatialMoveEvent instead")
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
 *   composable, factoring in shrinking or stretching due to [SpatialMoveEvent.scale].
 */
@Deprecated(message = "Use SpatialMoveEvent instead")
public typealias SpatialMoveEndEvent = SpatialMoveEvent

@RequiresOptIn("This API is experimental and is likely to change or to be removed in the future.")
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalMoveAnchorPolicy
