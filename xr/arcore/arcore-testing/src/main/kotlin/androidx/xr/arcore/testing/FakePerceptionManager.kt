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

package androidx.xr.arcore.testing

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.AnchorInvalidUuidException
import androidx.xr.arcore.runtime.ConversationState
import androidx.xr.arcore.runtime.Depth
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

// TODO b/500091606 Remove when no longer used in G3
/**
 * Fake implementation of [PerceptionManager] used to validate state transitions.
 *
 * @property anchors a [MutableList] of [FakeRuntimeAnchors][FakeRuntimeAnchor] created
 * @property leftHand the left [Hand] as a [FakeRuntimeHand]
 * @property rightHand the right [Hand] as a [FakeRuntimeHand]
 * @property leftDepth the left [Depth] as a [FakeRuntimeDepth]
 * @property rightDepth the right [Depth] as a [FakeRuntimeDepth]
 * @property monoDepth the mono [Depth] as a [FakeRuntimeDepth]
 * @property isTrackingAvailable a flag to represent available tracking state of the camera
 * @deprecated This will be removed in a future release. In order to test androidx.xr.arcore APIs,
 *   use an [ArCoreTestRule] in your tests.
 */
@SuppressWarnings("HiddenSuperclass")
@Suppress("DEPRECATION")
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FakePerceptionManager : PerceptionManager, AnchorHolder {

    public val anchors: MutableList<Anchor> = mutableListOf<Anchor>()
    override val trackables: MutableList<Trackable> = mutableListOf<Trackable>()

    override val leftEye: Eye? = FakeRuntimeEye()

    override val rightEye: Eye? = FakeRuntimeEye()

    override val leftHand: Hand? = FakeRuntimeHand()
    override val rightHand: Hand? = FakeRuntimeHand()

    @get:SuppressLint("HiddenTypeParameter", "UnavailableSymbol")
    override val arDevice: FakeRuntimeArDevice = FakeRuntimeArDevice()

    @get:SuppressLint("HiddenTypeParameter", "UnavailableSymbol")
    override val leftRenderViewpoint: FakeRuntimeRenderViewpoint? =
        FakeRuntimeRenderViewpoint(Pose(Vector3(1f, 0f, 0f), Quaternion.Companion.Identity))

    @get:SuppressLint("HiddenTypeParameter", "UnavailableSymbol")
    override val rightRenderViewpoint: FakeRuntimeRenderViewpoint? =
        FakeRuntimeRenderViewpoint(Pose(Vector3(0f, 1f, 0f), Quaternion.Companion.Identity))

    @get:SuppressLint("HiddenTypeParameter", "UnavailableSymbol")
    override val monoRenderViewpoint: FakeRuntimeRenderViewpoint? =
        FakeRuntimeRenderViewpoint(Pose(Vector3(0f, 0f, 1f), Quaternion.Companion.Identity))

    override val userFace: Face? = FakeRuntimeFace()

    override val geospatial: Geospatial = FakeRuntimeGeospatial()

    override val leftDepth: Depth? = FakeRuntimeDepth()

    override val rightDepth: Depth? = FakeRuntimeDepth()

    override val monoDepth: Depth? = FakeRuntimeDepth()

    override val conversationSceneSignal: ConversationState? = null

    private val hitResults = mutableListOf<HitResult>()
    private val anchorUuids = mutableListOf<UUID>()
    public var isSizeEstimationSupported: Boolean = true
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

    override fun onAnchorPersisted(anchor: Anchor) {
        anchorUuids.add(anchor.uuid!!)
    }

    override fun detachAnchor(anchor: Anchor) {
        anchors.remove(anchor)
        anchor.uuid?.let { anchorUuids.remove(it) }
    }

    override val imageDatabaseMaxLoadedImageCount: Int = 5

    override val isPhysicalSizeEstimationSupported: Boolean = isSizeEstimationSupported

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
