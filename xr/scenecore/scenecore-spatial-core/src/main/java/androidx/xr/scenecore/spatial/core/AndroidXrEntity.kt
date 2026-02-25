/*
 * Copyright 2025 The Android Open Source Project
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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore.spatial.core

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.PointerCaptureComponent
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpaceValue
import androidx.xr.scenecore.runtime.impl.BaseEntity
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.function.Consumer
import com.android.extensions.xr.node.InputEvent
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.ReformEvent
import com.android.extensions.xr.node.ReformOptions
import java.lang.ref.WeakReference
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService

/**
 * Implementation of a JXR SceneCore Entity that wraps an android XR extension Node.
 *
 * <p>This should not be created on its own but should be inherited by objects that need to wrap an
 * Android extension node.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(b/452961674): Review RestrictTo annotations in SceneCore.
public abstract class AndroidXrEntity(
    context: Context?,
    // Returns the underlying extension Node for the Entity.
    @JvmField internal val mNode: Node,
    @JvmField protected val mExtensions: XrExtensions,
    @JvmField protected val mEntityManager: EntityManager,
    @JvmField protected val mExecutor: ScheduledExecutorService,
) : BaseEntity(context), Entity {

    // Visible for testing
    @VisibleForTesting
    internal val inputEventListenerMap: MutableMap<InputEventListener, Executor> =
        ConcurrentHashMap()
    internal var pointerCaptureInputEventListener: Optional<InputEventListener> = Optional.empty()
    internal var pointerCaptureExecutor: Optional<Executor> = Optional.empty()
    public val reformEventConsumerMap: MutableMap<Consumer<ReformEvent>, Executor> =
        ConcurrentHashMap()
    private var reformOptions: ReformOptions? = null

    init {
        mEntityManager.setEntityForNode(mNode, this)
    }

    override var parent: Entity?
        get() = super.parent
        set(newParent) {
            if (newParent != null && newParent !is AndroidXrEntity) {
                return
            }
            super.parent = newParent

            mExtensions.createNodeTransaction().use { transaction ->
                if (newParent == null) {
                    @Suppress("UNUSED_VARIABLE")
                    val unused = transaction.setVisibility(mNode, false).setParent(mNode, null)
                } else {
                    @Suppress("UNUSED_VARIABLE")
                    val unused = transaction.setParent(mNode, newParent.mNode)
                }
                transaction.apply()
            }
        }

    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        return when (relativeTo) {
            Space.PARENT -> super<BaseEntity>.getPose(relativeTo)
            Space.ACTIVITY -> poseInActivitySpace
            Space.REAL_WORLD -> poseInPerceptionSpace
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        val localPose: Pose =
            when (relativeTo) {
                Space.PARENT -> pose
                Space.ACTIVITY -> getLocalPoseForActivitySpacePose(pose)
                Space.REAL_WORLD -> getLocalPoseForPerceptionSpacePose(pose)
                else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
            }
        super<BaseEntity>.setPose(localPose, Space.PARENT)

        mExtensions.createNodeTransaction().use { transaction ->
            transaction
                .setPosition(
                    mNode,
                    localPose.translation.x,
                    localPose.translation.y,
                    localPose.translation.z,
                )
                .setOrientation(
                    mNode,
                    localPose.rotation.x,
                    localPose.rotation.y,
                    localPose.rotation.z,
                    localPose.rotation.w,
                )
                .apply()
        }
    }

    override fun setScale(scale: Vector3, @SpaceValue relativeTo: Int) {
        super<BaseEntity>.setScale(scale, relativeTo)
        val localScale = super<BaseEntity>.getScale(Space.PARENT)
        mExtensions.createNodeTransaction().use { transaction ->
            transaction.setScale(mNode, localScale.x, localScale.y, localScale.z).apply()
        }
    }

    /** Returns the pose for this entity, relative to the activity space root. */
    override val poseInActivitySpace: Pose
        get() {
            // This code might produce unexpected results when non-uniform scale
            // is involved in the parent-child entity hierarchy.

            // Any parentless "space" entities (such as the root and anchor entities) are expected
            // to
            // override this method non-recursively so that this error is never thrown.
            if (parent !is AndroidXrEntity) {
                throw IllegalStateException(
                    "Cannot get pose in Activity Space with a non-AndroidXrEntity parent"
                )
            }
            val xrParent = parent as AndroidXrEntity
            return xrParent.poseInActivitySpace.compose(
                Pose(
                    getPose(Space.PARENT).translation.scale(xrParent.activitySpaceScale),
                    getPose(Space.PARENT).rotation,
                )
            )
        }

    private val poseInPerceptionSpace: Pose
        get() {
            val perceptionSpaceScenePose =
                mEntityManager
                    .getSystemSpaceActivityPoseOfType(PerceptionSpaceScenePose::class.java)[0]
            return transformPoseTo(Pose(), perceptionSpaceScenePose)
        }

    private fun getLocalPoseForActivitySpacePose(pose: Pose): Pose {
        if (parent !is AndroidXrEntity) {
            throw IllegalStateException(
                "Cannot get pose in Activity Space with a non-AndroidXrEntity parent"
            )
        }
        val xrParent = parent as AndroidXrEntity
        val activitySpace =
            mEntityManager.getSystemSpaceActivityPoseOfType(ActivitySpace::class.java)[0]
        return activitySpace.transformPoseTo(pose, xrParent)
    }

    private fun getLocalPoseForPerceptionSpacePose(pose: Pose): Pose {
        if (parent !is AndroidXrEntity) {
            throw IllegalStateException(
                "Cannot get pose in Activity Space with a non-AndroidXrEntity parent"
            )
        }
        val xrParent = parent as AndroidXrEntity
        val perceptionSpaceScenePose =
            mEntityManager.getSystemSpaceActivityPoseOfType(PerceptionSpaceScenePose::class.java)[0]
        return perceptionSpaceScenePose.transformPoseTo(pose, xrParent)
    }

    override fun setAlpha(alpha: Float) {
        super<BaseEntity>.setAlpha(alpha)

        mExtensions.createNodeTransaction().use { transaction ->
            transaction.setAlpha(mNode, super<BaseEntity>.getAlpha()).apply()
        }
    }

    override fun setHidden(hidden: Boolean) {
        super.setHidden(hidden)

        mExtensions.createNodeTransaction().use { transaction ->
            if (reformOptions != null) {
                if (hidden) {
                    // Since this entity is being hidden, disable reform and the highlights around
                    // the node.
                    @Suppress("UNUSED_VARIABLE") val unused = transaction.disableReform(mNode)
                } else {
                    // Enables reform and the highlights around the node.
                    @Suppress("UNUSED_VARIABLE")
                    val unused = transaction.enableReform(mNode, reformOptions)
                }
            }
            transaction.setVisibility(mNode, !hidden).apply()
        }
    }

    override fun addInputEventListener(executor: Executor?, listener: InputEventListener) {
        maybeSetupInputListeners()
        inputEventListenerMap[listener] = executor ?: mExecutor
    }

    /**
     * Request pointer capture for this Entity, using the given interfaces to propagate state and
     * captured input.
     *
     * <p>Returns true if a new pointer capture session was requested. Returns false if there is
     * already a previously existing pointer capture session as only one can be supported at a given
     * time.
     */
    public fun requestPointerCapture(
        executor: Executor,
        eventListener: InputEventListener,
        stateListener: PointerCaptureComponent.StateListener,
    ): Boolean {
        if (pointerCaptureInputEventListener.isPresent) {
            return false
        }
        mNode.requestPointerCapture(executor) { pcState ->
            when (pcState) {
                Node.POINTER_CAPTURE_STATE_PAUSED ->
                    stateListener.onStateChanged(
                        PointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_PAUSED
                    )
                Node.POINTER_CAPTURE_STATE_ACTIVE ->
                    stateListener.onStateChanged(
                        PointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE
                    )
                Node.POINTER_CAPTURE_STATE_STOPPED ->
                    stateListener.onStateChanged(
                        PointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_STOPPED
                    )
                else -> throw IllegalStateException("Invalid state received for pointer capture")
            }
        }

        addPointerCaptureInputListener(executor, eventListener)
        return true
    }

    private fun addPointerCaptureInputListener(
        executor: Executor,
        eventListener: InputEventListener,
    ) {
        maybeSetupInputListeners()
        pointerCaptureInputEventListener = Optional.of(eventListener)
        pointerCaptureExecutor = Optional.ofNullable(executor)
    }

    private fun maybeSetupInputListeners() {
        // Only set up the listener if it doesn't already exist.
        if (inputEventListenerMap.isEmpty() && pointerCaptureInputEventListener.isEmpty) {
            mNode.listenForInput(mExecutor, this::handleInputEvent)
        }
    }

    /** Handles an incoming input event from the underlying node and dispatches it appropriately. */
    public fun handleInputEvent(xrInputEvent: InputEvent) {
        if (xrInputEvent.dispatchFlags == InputEvent.DISPATCH_FLAG_CAPTURED_POINTER) {
            dispatchCapturedPointerEvent(xrInputEvent)
        } else {
            dispatchStandardEvent(xrInputEvent)
        }
    }

    /** Dispatches an event to the active pointer capture listener. */
    private fun dispatchCapturedPointerEvent(xrInputEvent: InputEvent) {
        pointerCaptureInputEventListener.ifPresent { listener ->
            val executor = pointerCaptureExecutor.orElse(mExecutor)
            executor.execute {
                listener.onInputEvent(RuntimeUtils.getInputEvent(xrInputEvent, mEntityManager))
            }
        }
    }

    /** Dispatches an event to all standard input listeners. */
    private fun dispatchStandardEvent(xrInputEvent: InputEvent) {
        inputEventListenerMap.forEach { (listener, executor) ->
            executor.execute {
                listener.onInputEvent(RuntimeUtils.getInputEvent(xrInputEvent, mEntityManager))
            }
        }
    }

    override fun removeInputEventListener(listener: InputEventListener) {
        inputEventListenerMap.remove(listener)
        maybeStopListeningForInput()
    }

    /** Stop any pointer capture requests on this Entity. */
    public fun stopPointerCapture() {
        mNode.stopPointerCapture()
        pointerCaptureInputEventListener = Optional.empty()
        pointerCaptureExecutor = Optional.empty()
        maybeStopListeningForInput()
    }

    private fun maybeStopListeningForInput() {
        if (inputEventListenerMap.isEmpty() && pointerCaptureInputEventListener.isEmpty) {
            mNode.stopListeningForInput()
        }
    }

    override fun dispose() {
        inputEventListenerMap.clear()
        mNode.stopListeningForInput()
        reformEventConsumerMap.clear()
        mExtensions.createNodeTransaction().use { transaction ->
            @Suppress("UNUSED_VARIABLE") val unused = transaction.disableReform(mNode)
            transaction.apply()
        }

        // SystemSpaceEntityImpls (Anchors, ActivitySpace, etc) should have null parents.
        if (parent != null) {
            parent = null
        }
        mEntityManager.removeEntityForNode(mNode)
        super.dispose()
    }

    /**
     * Handles the logic for a reform event. This is in a separate method to be called from a weak
     * reference to avoid memory leaks.
     */
    private fun handleReformEvent(reformEvent: ReformEvent) {
        val currentReformOptions = reformOptions
        if (
            currentReformOptions != null &&
                (currentReformOptions.enabledReform and ReformOptions.ALLOW_MOVE) != 0 &&
                (currentReformOptions.flags and ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT) != 0
        ) {
            // Update the cached pose of the entity.
            super<BaseEntity>.setPose(
                Pose(
                    Vector3(
                        reformEvent.proposedPosition.x,
                        reformEvent.proposedPosition.y,
                        reformEvent.proposedPosition.z,
                    ),
                    Quaternion(
                        reformEvent.proposedOrientation.x,
                        reformEvent.proposedOrientation.y,
                        reformEvent.proposedOrientation.z,
                        reformEvent.proposedOrientation.w,
                    ),
                ),
                Space.PARENT,
            )
            // Update the cached scale of the entity.
            super.setScaleInternal(
                Vector3(
                    reformEvent.proposedScale.x,
                    reformEvent.proposedScale.y,
                    reformEvent.proposedScale.z,
                )
            )
        }
        reformEventConsumerMap.forEach { (eventConsumer, consumerExecutor) ->
            consumerExecutor.execute { eventConsumer.accept(reformEvent) }
        }
    }

    /**
     * Gets the reform options for this entity.
     *
     * @return The reform options for this entity.
     */
    public fun getReformOptions(): ReformOptions {
        if (reformOptions == null) {
            val weakThis = WeakReference(this)
            val reformEventConsumer = Consumer { reformEvent: ReformEvent ->
                val entity = weakThis.get()
                entity?.handleReformEvent(reformEvent)
            }
            reformOptions = mExtensions.createReformOptions(mExecutor, reformEventConsumer)
        }
        return reformOptions!!
    }

    /**
     * Updates the reform options for this entity. Uses the same instance of [ReformOptions]
     * provided by {@link #getReformOptions()}.
     */
    @SuppressLint("BanSynchronizedMethods")
    @Synchronized
    public fun updateReformOptions() {
        mExtensions.createNodeTransaction().use { transaction ->
            if (reformOptions!!.enabledReform == 0) {
                // Disables reform and the highlights around the node.
                @Suppress("UNUSED_VARIABLE") val unused = transaction.disableReform(mNode)
            } else {
                // Enables reform and the highlights around the node.
                @Suppress("UNUSED_VARIABLE")
                val unused = transaction.enableReform(mNode, reformOptions)
            }
            transaction.apply()
        }
    }

    public fun addReformEventConsumer(
        reformEventConsumer: Consumer<ReformEvent>,
        executor: Executor?,
    ) {
        val finalExecutor = executor ?: this.mExecutor
        reformEventConsumerMap[reformEventConsumer] = finalExecutor
    }

    public fun removeReformEventConsumer(reformEventConsumer: Consumer<ReformEvent>) {
        reformEventConsumerMap.remove(reformEventConsumer)
    }

    override suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @ScenePose.HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult {
        // Hit tests need to be issued in the activity space then converted to the entity's space.
        val activitySpace =
            mEntityManager.getSystemSpaceActivityPoseOfType(ActivitySpace::class.java)[0]
                ?: throw IllegalStateException("ActivitySpace is null")
        return activitySpace.hitTestRelativeToActivityPose(origin, direction, hitTestFilter, this)
    }

    public open fun getNode(): Node = mNode
}
