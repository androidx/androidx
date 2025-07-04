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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.AnchorInvalidUuidException
import androidx.xr.runtime.internal.DepthMap
import androidx.xr.runtime.internal.Earth
import androidx.xr.runtime.internal.Face
import androidx.xr.runtime.internal.Hand
import androidx.xr.runtime.internal.HitResult
import androidx.xr.runtime.internal.PerceptionManager
import androidx.xr.runtime.internal.Trackable
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import java.util.UUID

/** Test-only implementation of [PerceptionManager] used to validate state transitions. */
public class FakePerceptionManager : PerceptionManager, AnchorHolder {

    /** List of anchors created by this [FakePerceptionManager]. */
    public val anchors: MutableList<Anchor> = mutableListOf<Anchor>()
    override val trackables: MutableList<Trackable> = mutableListOf<Trackable>()

    override val leftHand: Hand? = FakeRuntimeHand()
    override val rightHand: Hand? = FakeRuntimeHand()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val arDevice: FakeRuntimeArDevice = FakeRuntimeArDevice()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val viewCameras: List<FakeRuntimeViewCamera> = listOf(FakeRuntimeViewCamera())

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val userFace: Face? = FakeRuntimeFace()

    override val earth: Earth = FakeRuntimeEarth()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val depthMaps: MutableList<DepthMap> = mutableListOf(FakeRuntimeDepthMap())

    private val hitResults = mutableListOf<HitResult>()
    private val anchorUuids = mutableListOf<UUID>()

    /** Flag to represent available tracking state of the camera. */
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

    override fun loadAnchorFromNativePointer(nativePointer: Long): Anchor {
        return FakeRuntimeAnchor(Pose(), this)
    }

    override fun detachAnchor(anchor: Anchor) {
        anchors.remove(anchor)
        anchor.uuid?.let { anchorUuids.remove(it) }
    }

    /** Adds a [HitResult] to the list that is returned when calling [hitTest] with any pose. */
    public fun addHitResult(hitResult: HitResult) {
        hitResults.add(hitResult)
    }

    /** Removes all [HitResult] instances passed to [addHitResult]. */
    public fun clearHitResults() {
        hitResults.clear()
    }

    /** Adds a [Trackable] to the list that is returned when calling [trackables]. */
    public fun addTrackable(trackable: Trackable) {
        trackables.add(trackable)
    }

    /** Removes all [Trackable] instances passed to [addTrackable]. */
    public fun clearTrackables() {
        trackables.clear()
    }
}
