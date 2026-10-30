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
import androidx.xr.scenecore.openxr.OpenXrSceneCommitter.CommitCallback
import androidx.xr.scenecore.openxr.OpenXrSceneCommitter.CommitResult
import androidx.xr.scenecore.openxr.OpenXrSceneCommitter.SpaceLease
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.CleanupAction
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpaceValue
import androidx.xr.scenecore.runtime.impl.PlatformReferenceScenePoseHelper
import java.lang.ref.WeakReference
import java.util.concurrent.RejectedExecutionException
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
 * [setAnchor] has [sceneCommitter] parent this entity's native scene entity to the root and attach
 * it to the anchor's `XrSpace`, in one transaction. From then on, the OpenXR runtime draws the
 * entity, and so its children, at the anchor's pose every frame.
 *
 * The runtime doesn't report where it draws the entity, so this entity's Kotlin pose and [state]
 * come from the anchor's [Anchor.state]. They can trail the drawn pose by up to one ARCore update.
 * Like spatial-core, this entity is ANCHORED as soon as the runtime has the attachment, before
 * ARCore first tracks the anchor. Until then, its pose is the identity pose, including after
 * [setAnchor] replaces an anchor.
 *
 * Don't call [Anchor.detach] on the anchor while this entity uses it: wait until the state listener
 * gets ERROR, or dispose this entity first. [state] is ERROR as soon as this entity fails, but the
 * listener only gets ERROR once the runtime has committed the removal of the attachment, or refused
 * it, since a failed entity stays in the scene, and detaching the anchor earlier could leave it
 * attached to a destroyed space. Once [dispose] returns, this entity never passes the space to the
 * runtime again: the runtime only reads the space when [sceneCommitter] stages the attachment. It
 * can keep the attachment until the destruction of the native entity commits, shortly after.
 */
// If the runtime can't locate the anchor, it keeps the entity at the last transform it had. On
// Vitreous, that transform is relative to the user's view.
// TODO: b/575789326 - Move to ERROR when the anchor is detached, once ARCore reports that as
// STOPPED.
internal class OpenXrAnchorEntity(
    context: Context?,
    entityHandle: Long,
    nativeWrapper: SceneCoreOpenXrNative,
    sceneCommitter: OpenXrSceneCommitter,
    sceneNodeRegistry: OpenXrSceneNodeRegistry,
    executor: ScheduledExecutorService,
    private val activitySpace: OpenXrActivitySpace,
) :
    OpenXrSystemSpaceEntity(
        context,
        entityHandle,
        nativeWrapper,
        sceneCommitter,
        sceneNodeRegistry,
        executor,
    ),
    AnchorEntity {

    /** One update of an anchor's tracking state, and its pose in the platform reference space. */
    internal data class AnchorSample(val trackingState: TrackingState, val pose: Pose)

    /**
     * An attachment that [attach] asked for, which lets [sceneCommitter] stage it while [lease]
     * isn't revoked, and follows [samples] once it commits.
     */
    private class PendingAttachment(val lease: SpaceLease, val samples: Flow<AnchorSample>)

    private val platformReferenceScenePoseHelper = PlatformReferenceScenePoseHelper(activitySpace)

    // Collects anchor samples on the SceneCore executor.
    private val scope = CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher())

    private val lock = Any()

    @GuardedBy("lock") private var _state: @AnchorEntity.State Int = AnchorEntity.State.UNANCHORED

    @GuardedBy("lock")
    private var onStateChangedListener: AnchorEntity.OnStateChangedListener? = null

    // Whether the current anchor has reported a TRACKING pose.
    @GuardedBy("lock") private var hasTracked = false

    // Collects the current anchor's samples. Null before the first attachment commits, and in
    // ERROR.
    @GuardedBy("lock") private var collectorJob: Job? = null

    // The latest attachment that attach() asked for, until its result arrives. Its lease is revoked
    // once it's replaced, and when this entity fails or is disposed.
    @GuardedBy("lock") private var pendingAttachment: PendingAttachment? = null

    // Whether the runtime has an attachment from this entity, or may have once a transaction
    // commits, that sceneCommitter wasn't asked to remove.
    @GuardedBy("lock") private var isAttached = false

    // Whether the ERROR report waits for the removal of the attachment to commit, or for the
    // runtime to refuse it.
    @GuardedBy("lock") private var isRemovingAttachment = false

    init {
        // Stops the collector when this entity is disposed or garbage collected.
        registerCleanup(executor, ScopeCleanupAction(scope))
    }

    // Captures only the scope, so the cleanup doesn't keep this entity reachable.
    private class ScopeCleanupAction(scope: CoroutineScope) : CleanupAction({ scope.cancel() })

    // A weak reference, so that an attachment waiting to commit doesn't keep this entity reachable.
    private class AttachmentCallback(
        private val entity: WeakReference<OpenXrAnchorEntity>,
        private val attachment: PendingAttachment,
    ) : CommitCallback {
        override fun onResult(result: CommitResult) {
            entity.get()?.onAttachmentResult(attachment, result)
        }
    }

    // A weak reference, so that a removal waiting to commit doesn't keep this entity reachable.
    private class RemovalCallback(private val entity: WeakReference<OpenXrAnchorEntity>) :
        CommitCallback {
        override fun onResult(result: CommitResult) {
            entity.get()?.onRemovalResult()
        }
    }

    override val state: @AnchorEntity.State Int
        get() = synchronized(lock) { _state }

    override fun setOnStateChangedListener(
        onStateChangedListener: AnchorEntity.OnStateChangedListener?
    ) {
        synchronized(lock) {
            this.onStateChangedListener = onStateChangedListener
            // Reports the current state, unless it changes before the report runs, or it's an
            // ERROR that waits for the removal of the attachment.
            if (onStateChangedListener != null && !isRemovingAttachment) {
                postStateLocked(onStateChangedListener, _state)
            }
        }
    }

    /**
     * Has [sceneCommitter] attach this entity to [anchor]'s `XrSpace`, and follows [anchor] for its
     * Kotlin pose and [state], as [attach] describes.
     *
     * @return true once the attachment is requested: this entity moves to ANCHORED when it commits,
     *   or to ERROR if the runtime rejects it. False if the attachment failed right away, for
     *   example after [dispose] or if [anchor] has no `XrSpace`. This entity then moves to ERROR.
     */
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
     * [sceneCommitter] commits the attachment shortly after, once a scene transaction is free, for
     * example after the runtime links another anchor, and the runtime reads the space then. Until
     * then, this entity keeps its state and keeps following the earlier anchor, if any. If the
     * earlier anchor stops meanwhile, this entity stops following it instead of failing, and waits
     * for the new attachment. It moves to ANCHORED once the attachment commits, and to ERROR if the
     * runtime rejects it.
     *
     * @return false if the attachment failed right away. This entity then moves to ERROR, and has
     *   [sceneCommitter] remove the earlier attachment, if any. Otherwise true.
     */
    @VisibleForTesting
    internal fun attach(spaceHandle: Long?, samples: Flow<AnchorSample>): Boolean {
        synchronized(lock) {
            if (_state == AnchorEntity.State.ERROR) return false
            val handle = entityHandle
            val rootHandle = activitySpace.entityHandle
            if (
                spaceHandle == null ||
                    spaceHandle == INVALID_HANDLE ||
                    handle == INVALID_HANDLE ||
                    rootHandle == INVALID_HANDLE ||
                    nativeWrapper.nativeScenecore == INVALID_HANDLE
            ) {
                // TODO: b/537445115 - Record a warning event saying which handle is missing.
                failLocked()
                return false
            }
            // The new attachment replaces the earlier one that hasn't committed, if any.
            revokeLocked(pendingAttachment)
            val attachment = PendingAttachment(SpaceLease(spaceHandle), samples)
            pendingAttachment = attachment
            val callback = AttachmentCallback(WeakReference(this), attachment)
            val isRecorded = sceneCommitter.edit {
                setSpaceAttachment(handle, attachment.lease, callback)
                setParent(handle, rootHandle)
            }
            // The committer stopped, with the runtime.
            if (!isRecorded) failLocked()
            return isRecorded
        }
    }

    private fun onAttachmentResult(attachment: PendingAttachment, result: CommitResult) {
        synchronized(lock) {
            // attach(), failLocked() or dispose() revoked it since, and revokeLocked() recorded
            // then whether the runtime has it.
            if (attachment !== pendingAttachment) return
            if (result == CommitResult.COMMITTED) {
                pendingAttachment = null
                isAttached = true
                followLocked(attachment.samples)
                updateStateLocked(AnchorEntity.State.ANCHORED)
            } else {
                // TODO: b/537445115 - Record a warning event that the attachment failed.
                failLocked()
            }
        }
    }

    /** Stops following the earlier anchor's samples, if any, and follows [samples] instead. */
    @GuardedBy("lock")
    private fun followLocked(samples: Flow<AnchorSample>) {
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
            TrackingState.STOPPED ->
                if (pendingAttachment == null) {
                    failLocked()
                } else {
                    // The earlier anchor stopped while its replacement waits to commit. Stop
                    // following it, and let the replacement's commit or rejection decide this
                    // entity's state.
                    collectorJob?.cancel()
                    collectorJob = null
                }
        }
    }

    @GuardedBy("lock")
    private fun updateStateLocked(newState: @AnchorEntity.State Int) {
        if (newState == _state) return
        _state = newState
        // reportDeferredErrorLocked() reports an ERROR that waits for the removal.
        if (!isRemovingAttachment) onStateChangedListener?.let { postStateLocked(it, newState) }
    }

    @GuardedBy("lock")
    private fun postStateLocked(
        listener: AnchorEntity.OnStateChangedListener,
        state: @AnchorEntity.State Int,
    ) {
        // Report on the executor, outside the lock, so the listener can call back into this
        // entity. Posting while locked to the single-threaded executor keeps reports in order.
        try {
            executor.execute {
                // Skip if the listener or the state changed since (a new state posts its own
                // report), so a late report can't undo a state that AnchorSpace set itself, such
                // as ERROR. A quick A, B, A thus skips B and reports A again, which AnchorSpace
                // ignores.
                val isCurrent =
                    synchronized(lock) { onStateChangedListener === listener && _state == state }
                if (isCurrent) listener.onStateChanged(state)
            }
        } catch (_: RejectedExecutionException) {
            // The executor shut down with the runtime, so nothing can run the listener.
        }
    }

    /**
     * Stops following the anchor and stops [sceneCommitter] from staging the attachment that hasn't
     * committed, has it remove the attachment that the runtime has, if any, and moves to ERROR,
     * which is final.
     *
     * The listener gets ERROR once that removal commits or the runtime refuses it, or right away if
     * there's nothing to remove, since the ERROR listener may destroy the anchor's `XrSpace`, and
     * unlike after [dispose], this entity stays in the scene, so its attachment should be removed
     * first.
     */
    @GuardedBy("lock")
    private fun failLocked() {
        revokeLocked(pendingAttachment)
        pendingAttachment = null
        collectorJob?.cancel()
        collectorJob = null
        if (removeAttachmentLocked()) isRemovingAttachment = true
        updateStateLocked(AnchorEntity.State.ERROR)
    }

    /**
     * Revokes the lease of [attachment], if any, so that [sceneCommitter] doesn't stage it anymore,
     * and records whether the runtime accepted it. If [sceneCommitter] is staging it, waits for it
     * to finish first.
     */
    @GuardedBy("lock")
    private fun revokeLocked(attachment: PendingAttachment?) {
        if (attachment?.lease?.revoke() == true) isAttached = true
    }

    /**
     * Has [sceneCommitter] remove the attachment that the runtime has, if any, and unparent the
     * native entity so its content stops drawing. Leaves alone an entity that was never attached,
     * and so never parented.
     *
     * @return whether [onRemovalResult] will get the result of a removal.
     */
    // TODO: b/517189353 - Vitreous re-roots unparented entities under the activity window instead
    // of removing them as XR_EXTX1_system_renderer specifies, so their content stays visible.
    @GuardedBy("lock")
    private fun removeAttachmentLocked(): Boolean {
        val handle = entityHandle
        if (!isAttached || handle == INVALID_HANDLE) return false
        isAttached = false
        val callback = RemovalCallback(WeakReference(this))
        // False if the committer stopped, with the runtime.
        return sceneCommitter.edit {
            removeSpaceAttachment(handle, callback)
            setParent(handle, INVALID_HANDLE)
        }
    }

    private fun onRemovalResult() {
        synchronized(lock) { reportDeferredErrorLocked() }
    }

    /** Reports the ERROR that waits for the removal of the attachment, if any. */
    @GuardedBy("lock")
    private fun reportDeferredErrorLocked() {
        if (!isRemovingAttachment) return
        isRemovingAttachment = false
        onStateChangedListener?.let { postStateLocked(it, _state) }
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
     * Moves to ERROR, then has [sceneCommitter] destroy the native entity, which removes its
     * attachment too. The listener gets ERROR afterwards, on the executor, without waiting for the
     * destruction to commit. Unlike after a failure, the entity leaves the scene, so nothing is
     * gained by waiting.
     *
     * Once this returns, this entity never passes the anchor's `XrSpace` to the runtime again: it
     * revokes the lease of the attachment that hasn't committed, if any, which waits for
     * [sceneCommitter] to finish staging it, if it is.
     *
     * The runtime reads the space only when the attachment is staged, but keeps the attachment
     * until the destruction commits. If the anchor is detached meanwhile, the entity stays attached
     * to the destroyed space until then.
     *
     * This destroys the native entity even if already in ERROR.
     */
    override fun dispose() {
        synchronized(lock) {
            scope.cancel()
            revokeLocked(pendingAttachment)
            pendingAttachment = null
            // Destroying the native entity removes its attachment, so failLocked() doesn't need to.
            isAttached = false
            failLocked()
            // Destroying the native entity drops a removal that's waiting, so report ERROR now.
            reportDeferredErrorLocked()
        }
        super.dispose()
    }
}
