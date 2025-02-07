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

import android.content.res.Resources
import android.util.Log
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Density
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceLayoutNode
import androidx.xr.compose.subspace.node.coordinator
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.toDimensionsInMeters
import androidx.xr.compose.unit.toIntVolumeSize
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.scenecore.BasePanelEntity
import androidx.xr.scenecore.ContentlessEntity
import androidx.xr.scenecore.Dimensions
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.MoveListener
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PixelDimensions
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeListener
import androidx.xr.scenecore.Session
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Wrapper class for Entities from SceneCore to provide convenience methods for working with
 * Entities from SceneCore.
 */
internal sealed class CoreEntity(public val entity: Entity) : SubspaceLayoutCoordinates {

    internal var movable: Movable? = null
    internal var resizable: Resizable? = null

    internal var layout: SubspaceLayoutNode.MeasurableLayout? = null
        set(value) {
            field = value
            updateEntityPose()
        }

    internal fun updateEntityPose() {
        // Compose XR uses pixels, SceneCore uses meters.
        val corePose =
            (layout?.poseInParentEntity ?: Pose.Identity).convertPixelsToMeters(DEFAULT_DENSITY)
        if (entity.getPose() != corePose) {
            entity.setPose(corePose)
        }
    }

    public open fun dispose() {
        entity.dispose()
    }

    override val pose: Pose
        get() = movable?.userPose ?: Pose.Identity

    override val poseInRoot: Pose
        get() = pose.translate(sourcePoseInRoot.translation).rotate(sourcePoseInRoot.rotation)

    private val sourcePoseInRoot: Pose
        get() = coordinatesInRoot?.poseInRoot ?: Pose.Identity

    private val coordinatesInRoot: SubspaceLayoutCoordinates?
        get() =
            layout
                ?.tail
                ?.traverseSelfThenAncestors()
                ?.findInstance<SubspaceLayoutModifierNode>()
                ?.coordinator ?: layout?.parentCoordinatesInRoot

    override val poseInParentEntity: Pose
        get() =
            pose
                .translate(sourcePoseInParentEntity.translation)
                .rotate(sourcePoseInParentEntity.rotation)

    private val sourcePoseInParentEntity: Pose
        get() = coordinatesInParentEntity?.poseInParentEntity ?: Pose.Identity

    private val coordinatesInParentEntity: SubspaceLayoutCoordinates?
        get() =
            layout
                ?.tail
                ?.traverseSelfThenAncestors()
                ?.findInstance<SubspaceLayoutModifierNode>()
                ?.coordinator ?: layout?.parentCoordinatesInParentEntity

    protected val _size = mutableStateOf(IntVolumeSize.Zero)

    override var size: IntVolumeSize
        get() = _size.value
        set(value) {
            val proposedSize = resizable?.userSize ?: value
            if (_size.value == proposedSize) {
                return
            }

            setEntitySize(proposedSize)
            _size.value = proposedSize

            movable?.setComponentSize(proposedSize)
            resizable?.setComponentSize(proposedSize)
        }

    protected open fun setEntitySize(size: IntVolumeSize) {
        entity.setSize(
            Dimensions(size.width.toFloat(), size.height.toFloat(), size.depth.toFloat())
        )
    }

    public var parent: CoreEntity? = null
        set(value) {
            field = value

            // Leave SceneCore's parent as-is if we're trying to clear it out. SceneCore
            // parents all
            // newly-created non-Anchor entities under a world space point of reference for the
            // activity
            // space, but we don't have access to it. To maintain this parent-is-not-null property,
            // we use
            // this hack to keep the original parent, even if it's not technically correct when
            // we're
            // trying to reparent a node. The correct parent will be set on the "set" part of the
            // reparent.
            //
            // TODO(b/356952297): Remove this hack once we can save and restore the original parent.
            if (value == null) return

            entity.setParent(value.entity)
        }

    /**
     * The scale of this entity relative to its parent. This value will affect the rendering of this
     * Entity's children. As the scale increases, this will uniformly stretch the content of the
     * Entity. This does not affect layout and other content will be laid out according to the
     * original scale of the entity.
     */
    public var scale: Float = 1.0F
        set(value) {
            field = value
            entity.setScale(value * scaleFromMovement)
        }

    /**
     * The scale of this entity when it is moved. This value is only used to be multiplied by the
     * scale of the entity, to preserve the original scale of the entity and determine the final
     * scale of the entity.
     */
    var scaleFromMovement: Float = 1.0F

    /**
     * Sets the opacity of this entity (and its children) to a value between [0..1]. An alpha value
     * of 0.0f means fully transparent while the value of 1.0f means fully opaque.
     *
     * @param alpha The opacity of this entity.
     */
    public fun setAlpha(alpha: Float) {
        entity.setAlpha(alpha)
    }

    public companion object {
        protected val LocalExecutor: Executor = Executors.newSingleThreadExecutor()

        // TODO(djeu): Figure out if there's a better way here where there is no context.
        private val DEFAULT_DENSITY: Density =
            Density(
                density = Resources.getSystem().displayMetrics.density,
                fontScale = Resources.getSystem().configuration.fontScale,
            )
    }

    internal inner class Movable(private val session: Session) {
        /**
         * The node should be movable.
         *
         * Right now this only affects the initial attachment of the component to the Core entity;
         * however, this may change to be more reactive.
         *
         * TODO(b/358678496): Revisit this now that Movable cannot by disabled, except via the
         *   'enabled' bit in the Node.
         */
        public var isEnabled: Boolean = true
        /** Pose based on user adjustments from MoveEvents from SceneCore. */
        public var userPose: Pose? = null
            set(value) {
                field = value
                updateEntityPose()
            }

        private var initialOffset: Pose = Pose.Identity

        /** Sets the size of the SysUI movable affordance. */
        public fun setComponentSize(size: IntVolumeSize) {
            if (isAttached) {
                component.size = size.toDimensionsInMeters(DEFAULT_DENSITY)
            }
        }

        /** All Compose XR params for the Movable modifier for this CoreEntity. */
        private var movableNode: MovableNode? = null

        /** Whether the movableComponent is attached to the entity. */
        private var isAttached: Boolean = false

        private val component: MovableComponent by lazy {
            // Here we create the component and pass in false to systemMovable since Compose is
            // going to
            // handle the move events.
            MovableComponent.create(session, systemMovable = false)
        }

        /**
         * Updates the movable state of this CoreEntity. Only update movable state if [Movable] is
         * enabled.
         *
         * @param node The Movable modifier for this CoreEntity.
         */
        internal fun updateState(node: MovableNode?) {
            if (!isEnabled) {
                if (node != null && node.enabled) logEnabledCheck()
                return
            }

            movableNode = node
            if (node != null && node.enabled) {
                enableComponent()
            } else {
                disableComponent()
            }
        }

        /** Enables the MovableComponent for this CoreEntity. */
        private fun enableComponent() {
            if (!isAttached) {
                check(entity.addComponent(component)) {
                    "Could not add MovableComponent to Core Entity"
                }
                component.addMoveListener(
                    LocalExecutor,
                    object : MoveListener {
                        override fun onMoveStart(
                            entity: Entity,
                            initialInputRay: Ray,
                            initialPose: Pose,
                            initialScale: Float,
                            initialParent: Entity,
                        ) {
                            // updatePoseOnMove() not called because initialPose should be the same
                            // as the current
                            // pose.
                            initialOffset = sourcePoseInParentEntity
                        }

                        override fun onMoveUpdate(
                            entity: Entity,
                            currentInputRay: Ray,
                            currentPose: Pose,
                            currentScale: Float,
                        ) {
                            updatePoseOnMove(
                                currentPose,
                                currentScale,
                                entity.getSize().toIntVolumeSize(DEFAULT_DENSITY),
                            )
                        }

                        override fun onMoveEnd(
                            entity: Entity,
                            finalInputRay: Ray,
                            finalPose: Pose,
                            finalScale: Float,
                            updatedParent: Entity?,
                        ) {
                            updatePoseOnMove(
                                finalPose,
                                finalScale,
                                entity.getSize().toIntVolumeSize(DEFAULT_DENSITY),
                            )
                            initialOffset = Pose.Identity
                        }
                    },
                )
                // Ensure size is correct, since we do not update the size
                // when the component is detached.
                setComponentSize(size)
                isAttached = true
            }

            // If the MovableComponent gets more internal state, copy it over
            // from the modifier node here.
        }

        /**
         * Disables the MovableComponent for this CoreEntity. Takes care of life cycle tasks for the
         * underlying component in SceneCore.
         */
        private fun disableComponent() {
            if (isAttached) {
                entity.removeComponent(component)
                isAttached = false
                if (movableNode?.stickyPose != true) {
                    userPose = null
                }
            }
        }

        /** Called every time there is a MoveEvent in SceneCore, if this CoreEntity is movable. */
        private fun updatePoseOnMove(pose: Pose, scaleWithDistance: Float, size: IntVolumeSize) {

            if (movableNode?.enabled == false) {
                return
            }
            val node = movableNode ?: return

            // SceneCore uses meters, Compose XR uses pixels.
            val corePose = pose.convertMetersToPixels(DEFAULT_DENSITY)

            // Find the delta from when the move event started.
            val coreDeltaPose =
                Pose(
                    corePose.translation - initialOffset.translation,
                    initialOffset.rotation.inverse * corePose.rotation,
                )

            if (node.onPoseChange(PoseChangeEvent(corePose, scaleWithDistance, size))) {
                // We're done, the user app will handle the event.
                return
            }
            userPose = coreDeltaPose
            if (movableNode?.scaleWithDistance!!) {
                scaleFromMovement = scaleWithDistance
                entity.setScale(scale * scaleFromMovement)
            }
        }

        /** Flag to enforce single logging of Entity Component update error. */
        private var shouldLogEnabledCheck: Boolean = true

        /** Log enabled check error if first time occurring. */
        private fun logEnabledCheck() {
            if (shouldLogEnabledCheck) {
                Log.i(
                    "CoreEntity",
                    "Not attempting to update Components, functionality is not enabled."
                )
                shouldLogEnabledCheck = false
            }
        }
    }

    internal inner class Resizable(private val session: Session) {
        /**
         * The node should be resizable.
         *
         * Right now this only affects the initial attachment of the component to the Core entity;
         * however, this may change to be more reactive.
         *
         * TODO(b/358678496): Revisit this now that Resizable cannot by disabled, except via the
         *   'enabled' bit in the Node.
         */
        public var isEnabled: Boolean = true

        /** Size based on user adjustments from ResizeEvents from SceneCore. */
        public var userSize: IntVolumeSize? = null
            private set(value) {
                field = value
                if (value != null) {
                    // The user size takes priority. Set the current size to the user provided size.
                    size = value
                }
            }

        /** Sets the size of the SysUI resizable affordance. */
        public fun setComponentSize(size: IntVolumeSize) {
            if (isAttached) {
                component.size = size.toDimensionsInMeters(DEFAULT_DENSITY)
            }
        }

        /** All Compose XR params for the Resizable modifier for this CoreEntity. */
        private var resizableNode: ResizableNode? = null

        /** Whether the resizableComponent is attached to the entity. */
        private var isAttached: Boolean = false

        private val component: ResizableComponent by lazy {
            ResizableComponent.create(session).apply {
                addResizeListener(
                    LocalExecutor,
                    object : ResizeListener {
                        override fun onResizeStart(entity: Entity, originalSize: Dimensions) {
                            resizeListener(originalSize)
                        }

                        // Compose does not need to handle the onResizeUpdate event since Core is
                        // handling the
                        // UI affordance change and adding the update would make it so we update the
                        // size twice.
                        override fun onResizeEnd(entity: Entity, finalSize: Dimensions) {
                            resizeListener(finalSize)
                        }
                    },
                )
            }
        }

        /**
         * Updates the resizable state of this CoreEntity. Only update resizable state if
         * [Resizable] is enabled.
         *
         * @param node The Resizable modifier for this CoreEntity.
         */
        internal fun updateState(node: ResizableNode?) {
            if (!isEnabled) {
                if (node != null && node.enabled) logEnabledCheck()
                return
            }

            resizableNode = node
            if (node != null && node.enabled) {
                enableAndUpdateComponent(node)
            } else {
                disableComponent()
            }
        }

        /** Flag to enforce single logging of Entity Component update error. */
        private var shouldLogEnabledCheck: Boolean = true

        /** Log enabled check error if first time occurring. */
        private fun logEnabledCheck() {
            if (shouldLogEnabledCheck) {
                Log.i(
                    "CoreEntity",
                    "Not attempting to update Components, entity type is not interactive."
                )
                shouldLogEnabledCheck = false
            }
        }

        /**
         * Enables the ResizableComponent for this CoreEntity. Also, updates the component using
         * [node]'s values.
         *
         * @param node The Resizable modifier for this CoreEntity.
         */
        private fun enableAndUpdateComponent(node: ResizableNode) {
            if (!isAttached) {
                check(entity.addComponent(component)) {
                    "Could not add ResizableComponent to Core Entity"
                }
                // Ensure size is correct, since we do not update the size
                // when the component is detached.
                setComponentSize(size)
                isAttached = true
            }

            component.minimumSize = node.minimumSize.toDimensionsInMeters()
            component.maximumSize = node.maximumSize.toDimensionsInMeters()

            component.fixedAspectRatio = if (node.maintainAspectRatio) getAspectRatioY() else 0.0f
        }

        /** Returns 0.0f if the aspect ratio of x to y is not well defined. */
        private fun getAspectRatioY(): Float {
            if (size.width == 0 || size.height == 0) return 0.0f
            return size.width.toFloat() / size.height.toFloat()
        }

        /**
         * Disables the ResizableComponent for this CoreEntity. Takes care of life cycle tasks for
         * the underlying component in SceneCore.
         */
        private fun disableComponent() {
            if (isAttached) {
                entity.removeComponent(component)
                isAttached = false
            }
        }

        /**
         * Called every time there is an onResizeEnd event in SceneCore, if this CoreEntity is
         * resizable.
         */
        private fun resizeListener(newSize: Dimensions) {
            val node = resizableNode ?: return
            if (node.onSizeChange(newSize.toIntVolumeSize(DEFAULT_DENSITY))) {
                // We're done, the user app will handle the event.
                return
            }
            userSize = newSize.toIntVolumeSize(DEFAULT_DENSITY)
        }
    }
}

/**
 * A [CoreEntityNode] will apply itself to the entity in question as part of the [CoreEntity]'s
 * application of modifiers. A [CoreEntityNode] should be applicable to all Entity types.
 *
 * TODO(b/374774812)
 */
internal interface CoreEntityNode {
    public fun modifyCoreEntity(coreEntity: CoreEntity)
}

/** Wrapper class for contentless entities from SceneCore. */
internal class CoreContentlessEntity(entity: Entity) : CoreEntity(entity) {
    init {
        require(entity is ContentlessEntity) {
            "Entity passed to CoreContentlessEntity should be a ContentlessEntity."
        }
    }
}

/**
 * Wrapper class for [BasePanelEntity] to provide convenience methods for working with panel
 * entities from SceneCore.
 */
internal sealed class CoreBasePanelEntity(
    session: Session,
    private val panelEntity: BasePanelEntity<*>,
) : CoreEntity(panelEntity) {

    init {
        movable = Movable(session)
        resizable = Resizable(session)
    }

    override fun setEntitySize(size: IntVolumeSize) {
        panelEntity.setPixelDimensions(PixelDimensions(size.width, size.height))
    }

    /**
     * Sets a corner radius on all four corners of this PanelEntity.
     *
     * @param radius The radius of the corners, in pixels.
     * @param density The panel pixel density.
     * @throws IllegalArgumentException if radius is <= 0.0f.
     */
    internal fun setCornerRadius(radius: Float, density: Density) {
        panelEntity.setCornerRadius(Meter.fromPixel(radius, density).value)
    }

    /**
     * Returns the corner radius of this PanelEntity, in pixels.
     *
     * @param density The panel pixel density.
     */
    internal fun getCornerRadius(density: Density): Float {
        return Meter(panelEntity.getCornerRadius()).toPx(density)
    }
}

/**
 * Wrapper class for [PanelEntity] to provide convenience methods for working with panel entities
 * from SceneCore.
 */
internal class CorePanelEntity(session: Session, entity: PanelEntity) :
    CoreBasePanelEntity(session, entity)

/**
 * Wrapper class for [Session.mainPanelEntity] to provide convenience methods for working with the
 * main panel from SceneCore.
 */
internal class CoreMainPanelEntity(session: Session) :
    CoreBasePanelEntity(session, session.mainPanelEntity) {
    private val mainView = session.activity.window.decorView
    private val listener =
        View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            _size.value =
                session.mainPanelEntity.getPixelDimensions().run { IntVolumeSize(width, height, 0) }
        }

    init {
        mainView.addOnLayoutChangeListener(listener)
    }

    override fun dispose() {
        mainView.removeOnLayoutChangeListener(listener)
        super.dispose()
    }
}
