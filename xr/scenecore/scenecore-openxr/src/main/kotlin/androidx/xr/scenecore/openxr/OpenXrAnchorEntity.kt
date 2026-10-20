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

package androidx.xr.scenecore.openxr

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.CleanupAction
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpaceValue
import androidx.xr.scenecore.runtime.impl.PlatformReferenceScenePoseHelper
import java.lang.ref.WeakReference
import java.util.concurrent.ScheduledExecutorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Implementation of SceneCore's [AnchorEntity] for OpenXR.
 *
 * [setAnchor] parents this entity's native scene entity to the root and attaches it to the anchor's
 * `XrSpace`, in one transaction. From then on, the OpenXR runtime draws the entity, and so its
 * children, at the anchor's pose every frame.
 *
 * The runtime doesn't report where it draws the entity, so this entity's Kotlin pose and [state]
 * come from the anchor's [Anchor.state]. They can trail the drawn pose by up to one ARCore update.
 * Like spatial-core, this entity is ANCHORED as soon as the runtime has the attachment, before
 * ARCore first tracks the anchor. Until then, its pose is the identity pose, including after
 * [setAnchor] replaces an anchor.
 *
 * Don't call [Anchor.detach] on the anchor while this entity uses it. Dispose this entity first.
 */
// If the runtime can't locate the anchor, it keeps the entity at the last transform it had. On
// Vitreous, that transform is relative to the user's view.
// TODO: b/575789326 - Move to ERROR when the anchor is detached, once ARCore reports that as
// STOPPED.
internal class OpenXrAnchorEntity(
    context: Context?,
    entityHandle: Long,
    nativeWrapper: SceneCoreOpenXrNative,
    sceneNodeRegistry: OpenXrSceneNodeRegistry,
    executor: ScheduledExecutorService,
    private val activitySpace: OpenXrActivitySpace,
) :
    OpenXrSystemSpaceEntity(context, entityHandle, nativeWrapper, sceneNodeRegistry, executor),
    AnchorEntity {

    /** One update of an anchor's tracking state, and its pose in the platform reference space. */
    internal data class AnchorSample(val trackingState: TrackingState, val pose: Pose)

    private val platformReferenceScenePoseHelper = PlatformReferenceScenePoseHelper(activitySpace)

    // Collects anchor samples on the SceneCore executor.
    private val scope = CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher())

    private val lock = Any()

    @GuardedBy("lock") private var _state: @AnchorEntity.State Int = AnchorEntity.State.UNANCHORED

    @GuardedBy("lock")
    private var onStateChangedListener: AnchorEntity.OnStateChangedListener? = null

    // The XrSpace that the native entity is attached to, or INVALID_HANDLE.
    @GuardedBy("lock") private var attachedSpaceHandle: Long = INVALID_HANDLE

    // Whether the current anchor has reported a TRACKING pose.
    @GuardedBy("lock") private var hasTracked = false

    // Collects the current anchor's samples. Null before attach(), and in ERROR.
    @GuardedBy("lock") private var collectorJob: Job? = null

    init {
        // Stops the collector when this entity is disposed or garbage collected.
        registerCleanup(executor, ScopeCleanupAction(scope))
    }

    // Captures only the scope, so the cleanup doesn't keep this entity reachable.
    private class ScopeCleanupAction(scope: CoroutineScope) : CleanupAction({ scope.cancel() })

    override val state: @AnchorEntity.State Int
        get() = synchronized(lock) { _state }

    override fun setOnStateChangedListener(
        onStateChangedListener: AnchorEntity.OnStateChangedListener?
    ) {
        synchronized(lock) {
            this.onStateChangedListener = onStateChangedListener
            // Reports the current state, unless it changes before the report runs.
            if (onStateChangedListener != null) postStateLocked(onStateChangedListener, _state)
        }
    }

    override fun setAnchor(anchor: Anchor): Boolean {
        // TODO: b/575789578 - Fail without SCENE_UNDERSTANDING_COARSE, as AnchorSpace documents,
        // if Vitreous has that permission.
        return attach(
            anchor.nativeSpaceHandle,
            anchor.state.map { AnchorSample(it.trackingState, it.pose) },
        )
    }

    /**
     * Attaches this entity to the `XrSpace` [spaceHandle], and follows [samples] for its Kotlin
     * pose and [state]. Calling this again replaces the earlier anchor.
     *
     * @return true if the attachment was committed. Otherwise moves to ERROR, which is final, so an
     *   earlier anchor is dropped too, and returns false.
     */
    @VisibleForTesting
    internal fun attach(spaceHandle: Long?, samples: Flow<AnchorSample>): Boolean {
        synchronized(lock) {
            if (_state == AnchorEntity.State.ERROR) return false
            val rootHandle = activitySpace.entityHandle
            if (
                spaceHandle == null ||
                    spaceHandle == INVALID_HANDLE ||
                    entityHandle == INVALID_HANDLE ||
                    rootHandle == INVALID_HANDLE ||
                    nativeWrapper.nativeScenecore == INVALID_HANDLE
            ) {
                // TODO: b/537445115 - Record a warning event saying which handle is missing.
                failLocked()
                return false
            }
            // TODO: b/575789122 - Retry instead of moving to ERROR when no scene transaction is
            // free, for example while the runtime links another anchor.
            val isCommitted =
                try {
                    nativeWrapper.openTransaction().use { tx ->
                        tx.isAvailable &&
                            tx.setParent(entityHandle, rootHandle)
                                .setSpaceAttachment(entityHandle, spaceHandle)
                                .commit()
                    }
                } catch (_: UnsatisfiedLinkError) {
                    // TODO: b/575789269 - Remove once the prebuilt has them.
                    false
                }
            if (!isCommitted) {
                // TODO: b/537445115 - Record a warning event that the attachment failed.
                failLocked()
                return false
            }
            attachedSpaceHandle = spaceHandle
            // Only now that the new attachment is committed, stop following the earlier anchor.
            // Its pose no longer applies: the pose is identity until the new anchor is tracked.
            collectorJob?.cancel()
            if (hasTracked) setPlatformReferenceSpacePose(Pose.Identity)
            hasTracked = false
            // A weak reference, so the anchor's flow doesn't keep this entity reachable.
            val entity = WeakReference(this)
            collectorJob = scope.launch {
                val collector = coroutineContext.job
                samples.collect { sample -> entity.get()?.onSample(collector, sample) }
            }
            updateStateLocked(AnchorEntity.State.ANCHORED)
            return true
        }
    }

    private fun onSample(collector: Job, sample: AnchorSample) {
        synchronized(lock) {
            // Drops samples from a replaced anchor, and samples after ERROR or dispose().
            if (collector !== collectorJob) return
            applySampleLocked(sample)
        }
    }

    @GuardedBy("lock")
    private fun applySampleLocked(sample: AnchorSample) {
        when (sample.trackingState) {
            TrackingState.TRACKING -> {
                setPlatformReferenceSpacePose(sample.pose)
                hasTracked = true
                updateStateLocked(AnchorEntity.State.ANCHORED)
            }
            // A new anchor is PAUSED until it's first tracked. Stay ANCHORED until then, so the
            // state doesn't flicker right after attach(). Keep the last pose either way.
            TrackingState.PAUSED -> if (hasTracked) updateStateLocked(AnchorEntity.State.UNANCHORED)
            TrackingState.STOPPED -> failLocked()
        }
    }

    @GuardedBy("lock")
    private fun updateStateLocked(newState: @AnchorEntity.State Int) {
        if (newState == _state) return
        _state = newState
        onStateChangedListener?.let { postStateLocked(it, newState) }
    }

    @GuardedBy("lock")
    private fun postStateLocked(
        listener: AnchorEntity.OnStateChangedListener,
        state: @AnchorEntity.State Int,
    ) {
        // Report on the executor, outside the lock, so the listener can call back into this
        // entity. Posting while locked to the single-threaded executor keeps reports in order.
        executor.execute {
            // Skip if the listener or the state changed since (a new state posts its own report),
            // so a late report can't undo a state that AnchorSpace set itself, such as ERROR. A
            // quick A, B, A thus skips B and reports A again, which AnchorSpace ignores.
            val isCurrent =
                synchronized(lock) { onStateChangedListener === listener && _state == state }
            if (isCurrent) listener.onStateChanged(state)
        }
    }

    /**
     * Stops following the anchor and moves to ERROR, which is final.
     *
     * First removes the attachment, since the ERROR listener may destroy the anchor's `XrSpace`,
     * and unparents the native entity so its content stops drawing. Both commit before the report
     * is posted. If no scene transaction is free, the attachment stays until [dispose].
     */
    // TODO: b/517189353 - Vitreous re-roots unparented entities under the activity window instead
    // of removing them as XR_EXTX1_system_renderer specifies, so their content stays visible.
    @GuardedBy("lock")
    private fun failLocked() {
        collectorJob?.cancel()
        collectorJob = null
        if (attachedSpaceHandle != INVALID_HANDLE) {
            detachNativeEntityLocked()
            attachedSpaceHandle = INVALID_HANDLE
        }
        updateStateLocked(AnchorEntity.State.ERROR)
    }

    @GuardedBy("lock")
    private fun detachNativeEntityLocked() {
        if (entityHandle == INVALID_HANDLE || nativeWrapper.nativeScenecore == INVALID_HANDLE) {
            return
        }
        // Best effort: this only runs on the way to ERROR, so there's no caller to report to.
        // TODO: b/537445115 - Record a warning event if the attachment isn't removed.
        // TODO: b/575789122 - Retry the removal when no scene transaction is free, before
        // reporting ERROR.
        nativeWrapper.openTransaction().use { tx ->
            if (tx.isAvailable) {
                tx.removeSpaceAttachment(entityHandle)
                    .setParent(entityHandle, INVALID_HANDLE)
                    .commit()
            }
        }
    }

    /**
     * The anchor's pose in the ActivitySpace.
     *
     * This is the identity pose before the anchor is first tracked, and in ERROR. While tracking is
     * lost, it's the last tracked pose.
     */
    override val activitySpacePose: Pose
        get() =
            synchronized(lock) {
                if (!hasTracked || _state == AnchorEntity.State.ERROR) {
                    Pose()
                } else {
                    platformReferenceScenePoseHelper.getActivitySpacePose(
                        poseInPlatformReferenceSpace
                    )
                }
            }

    override val activitySpaceScale: Vector3
        get() = platformReferenceScenePoseHelper.getActivitySpaceScale(worldSpaceScale)

    override var parent: Entity?
        get() = null
        set(_) = throw UnsupportedOperationException("Cannot set 'parent' on an AnchorEntity.")

    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        return when (relativeTo) {
            Space.PARENT ->
                throw UnsupportedOperationException(
                    "AnchorEntity is a root space and does not have a parent."
                )
            Space.ACTIVITY -> activitySpacePose
            Space.REAL_WORLD -> poseInPerceptionSpace
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        throw UnsupportedOperationException("Cannot set 'pose' on an AnchorEntity.")
    }

    override fun getScale(@SpaceValue relativeTo: Int): Vector3 {
        return when (relativeTo) {
            Space.PARENT ->
                throw UnsupportedOperationException(
                    "AnchorEntity is a root space and does not have a parent."
                )
            Space.ACTIVITY -> activitySpaceScale
            Space.REAL_WORLD -> worldSpaceScale
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setScale(scale: Vector3, @SpaceValue relativeTo: Int) {
        throw UnsupportedOperationException("Cannot set 'scale' on an AnchorEntity.")
    }

    /**
     * Removes the attachment, moves to ERROR, then destroys the native entity. The listener gets
     * the ERROR report afterwards, on the executor.
     *
     * This destroys the native entity even if already in ERROR.
     */
    override fun dispose() {
        synchronized(lock) {
            scope.cancel()
            failLocked()
        }
        super.dispose()
    }
}
