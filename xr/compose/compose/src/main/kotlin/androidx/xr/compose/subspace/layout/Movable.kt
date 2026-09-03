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
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.scenecore.AnchorPlacement
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.MovableComponent
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
 * 2) Attaching multiple [movable] modifiers with auto-applying policies (like [MovePolicy.system])
 *    to the same element will compound the movement distance, since each modifier independently
 *    applies the drag offset upon release.
 *
 * @param enabled true if this composable should be movable. Setting this to false will remove the
 *   interactable affordance associated with the content. Disabling the modifier after movement
 *   keeps the composable at its last dragged position. Removing the modifier entirely resets the
 *   composable to its original layout position.
 * @param movePolicy The [MovePolicy] that dictates how movement transformations are calculated and
 *   applied. Defaults to [MovePolicy.Default].
 * @sample androidx.xr.compose.samples.BasicMovableSample
 * @sample androidx.xr.compose.samples.CustomMovableSample
 */
public fun SubspaceModifier.movable(
    enabled: Boolean = true,
    movePolicy: MovePolicy = MovePolicy.Default,
): SubspaceModifier = this.then(MovableElement(enabled = enabled, movePolicy = movePolicy))

/** Defines the behavior and configuration for movement applied by the [movable] modifier. */
public sealed interface MovePolicy {
    public companion object {

        /** The default system-handled move policy. */
        public val Default: MovePolicy = system()

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
        public fun system(
            scaleWithDistance: Boolean = true,
            onMove: ((SpatialMoveEvent) -> Unit)? = null,
        ): MovePolicy = SystemMovePolicy(scaleWithDistance = scaleWithDistance, onMove = onMove)

        /**
         * A policy that accepts move events and reports the calculated pose updates via a callback,
         * without automatically applying a resulting transformation.
         *
         * This policy enables custom behavior for movement of the content. The system calculates
         * the target [Pose] based on input, but does not automatically apply it to the associated
         * layout. The developer is responsible for consuming the [onMove] event and applying the
         * result (e.g., by updating a state backed by [SubspaceModifier.offset]). Using this policy
         * has higher latency than [system].
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
         * Note: Anchoring is currently only supported for
         * [androidx.xr.compose.subspace.SpatialPanel] and
         * [androidx.xr.compose.subspace.SpatialGltfModel].
         *
         * Note: Once a composable is anchored to an external plane using this policy, it is
         * reparented outside the normal Compose hierarchy. As a result, conventional layout pose
         * calculations and pose-based modifiers (such as `rotate` or `gravityAligned`) are not
         * currently compatible with the anchored composable.
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

internal data class SystemMovePolicy(
    val scaleWithDistance: Boolean,
    val onMove: ((SpatialMoveEvent) -> Unit)?,
) : MovePolicy

internal data class CustomMovePolicy(
    val scaleWithDistance: Boolean,
    val onMove: (SpatialMoveEvent) -> Unit,
) : MovePolicy

@OptIn(ExperimentalMoveAnchorPolicy::class)
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

@OptIn(ExperimentalMoveAnchorPolicy::class)
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
        // Check if the underlying type of the policy changed (e.g., System to Custom)
        val policyTypeChanged = this.movePolicy::class != movePolicy::class

        // Only require a component recreation if the type changed, or if structural properties
        // changed.
        // We explicitly ignore the 'onMove' lambdas here so they can be updated freely without
        // recreation.
        val componentUpdateNeeded =
            policyTypeChanged ||
                when (movePolicy) {
                    is SystemMovePolicy -> {
                        movePolicy.scaleWithDistance !=
                            (this.movePolicy as SystemMovePolicy).scaleWithDistance
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
            is SystemMovePolicy -> {
                component =
                    MovableComponent.createSystemMovable(
                            session = session,
                            scaleInZ = (movePolicy as SystemMovePolicy).scaleWithDistance,
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
            is SystemMovePolicy -> {
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
            is SystemMovePolicy -> {
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
            is SystemMovePolicy -> {
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

@RequiresOptIn("This API is experimental and is likely to change or to be removed in the future.")
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalMoveAnchorPolicy
