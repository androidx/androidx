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

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.AnchorInvalidUuidException
import androidx.xr.arcore.runtime.DepthMap
import androidx.xr.arcore.runtime.Eye
import androidx.xr.arcore.runtime.Face
import androidx.xr.arcore.runtime.Geospatial
import androidx.xr.arcore.runtime.Hand
import androidx.xr.arcore.runtime.HitResult
import androidx.xr.arcore.runtime.PerceptionManager
import androidx.xr.arcore.runtime.Trackable
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import java.util.UUID

/**
 * Fake implementation of [PerceptionManager] used to validate state transitions.
 *
 * @property anchors a [MutableList] of [FakeRuntimeAnchors][FakeRuntimeAnchor] created
 * @property leftHand the left [Hand] as a [FakeRuntimeHand]
 * @property rightHand the right [Hand] as a [FakeRuntimeHand]
 * @property leftDepthMap the left [DepthMap] as a [FakeRuntimeDepthMap]
 * @property rightDepthMap the right [DepthMap] as a [FakeRuntimeDepthMap]
 * @property monoDepthMap the mono [DepthMap] as a [FakeRuntimeDepthMap]
 * @property isTrackingAvailable a flag to represent available tracking state of the camera
 */
@SuppressWarnings("HiddenSuperclass")
public class FakePerceptionManager : PerceptionManager, AnchorHolder {

    public val anchors: MutableList<Anchor> = mutableListOf<Anchor>()
    override val trackables: MutableList<Trackable> = mutableListOf<Trackable>()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val leftEye: Eye? = FakeRuntimeEye()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val rightEye: Eye? = FakeRuntimeEye()

    override val leftHand: Hand? = FakeRuntimeHand()
    override val rightHand: Hand? = FakeRuntimeHand()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val arDevice: FakeRuntimeArDevice = FakeRuntimeArDevice()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val leftRenderViewpoint: FakeRuntimeRenderViewpoint? =
        FakeRuntimeRenderViewpoint(Pose(Vector3(1f, 0f, 0f), Quaternion.Companion.Identity))

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val rightRenderViewpoint: FakeRuntimeRenderViewpoint? =
        FakeRuntimeRenderViewpoint(Pose(Vector3(0f, 1f, 0f), Quaternion.Companion.Identity))

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val monoRenderViewpoint: FakeRuntimeRenderViewpoint? =
        FakeRuntimeRenderViewpoint(Pose(Vector3(0f, 0f, 1f), Quaternion.Companion.Identity))

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val userFace: Face? = FakeRuntimeFace()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val geospatial: Geospatial = FakeRuntimeGeospatial()

    override val leftDepthMap: DepthMap? = FakeRuntimeDepthMap()

    override val rightDepthMap: DepthMap? = FakeRuntimeDepthMap()

    override val monoDepthMap: DepthMap? = FakeRuntimeDepthMap()

    private val hitResults = mutableListOf<HitResult>()
    private val anchorUuids = mutableListOf<UUID>()

    public var isTrackingAvailable: Boolean = true

    override fun createAnchor(pose: Pose): Anchor {
        // TODO: b/349862231 - Modify it once detach is implemented.
        val anchor = FakeRuntimeAnchor(pose, this, isTrackingAvailable)
        anchors.add(anchor)
        return anchor
    }

    override fun hitTest(ray: Ray): MutableList<HitResult> = hitResults

    override fun getPersistedAnchorUuids(): List<UUID> = anchorUuids

    override fun loadAnchor(uuid: UUID): Anchor {
        if (!anchorUuids.contains(uuid)) {
            throw AnchorInvalidUuidException()
        }
        return FakeRuntimeAnchor(Pose(), this)
    }

    override fun unpersistAnchor(uuid: UUID) {
        if (!anchorUuids.contains(uuid)) {
            throw AnchorInvalidUuidException()
        }
        anchorUuids.remove(uuid)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun onAnchorPersisted(anchor: Anchor) {
        anchorUuids.add(anchor.uuid!!)
    }

    override fun detachAnchor(anchor: Anchor) {
        anchors.remove(anchor)
        anchor.uuid?.let { anchorUuids.remove(it) }
    }

    /**
     * Adds a [HitResult] to the list that is returned when calling [hitTest] with any pose.
     *
     * @param hitResult the [HitResult] to add
     */
    public fun addHitResult(hitResult: HitResult) {
        hitResults.add(hitResult)
    }

    /** Removes all [HitResult] instances passed to [addHitResult]. */
    public fun clearHitResults() {
        hitResults.clear()
    }

    /**
     * Adds a [Trackable] to the list that is returned when calling [trackables].
     *
     * @param trackable the [Trackable] to add
     */
    public fun addTrackable(trackable: Trackable) {
        trackables.add(trackable)
    }

    /** Removes all [Trackable] instances passed to [addTrackable]. */
    public fun clearTrackables() {
        trackables.clear()
    }
}
