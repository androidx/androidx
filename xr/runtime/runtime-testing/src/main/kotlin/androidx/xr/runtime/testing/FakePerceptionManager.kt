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

import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.Hand
import androidx.xr.runtime.internal.HitResult
import androidx.xr.runtime.internal.PerceptionManager
import androidx.xr.runtime.internal.Trackable
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import java.util.UUID

/** Test-only implementation of [PerceptionManager] used to validate state transitions. */
public class FakePerceptionManager : PerceptionManager, AnchorHolder {

    public val anchors: MutableList<Anchor> = mutableListOf<Anchor>()
    override val trackables: MutableList<Trackable> = mutableListOf<Trackable>()

    override val leftHand: Hand? = FakeRuntimeHand()
    override val rightHand: Hand? = FakeRuntimeHand()

    private val hitResults = mutableListOf<HitResult>()
    private val anchorUuids = mutableListOf<UUID>()

    override fun createAnchor(pose: Pose): Anchor {
        // TODO: b/349862231 - Modify it once detach is implemented.
        val anchor = FakeRuntimeAnchor(pose, this)
        anchors.add(anchor)
        return anchor
    }

    override fun hitTest(ray: Ray): MutableList<HitResult> = hitResults

    override fun getPersistedAnchorUuids(): List<UUID> = anchorUuids

    override fun loadAnchor(uuid: UUID): Anchor {
        check(anchorUuids.contains(uuid)) { "Anchor is not persisted." }
        return FakeRuntimeAnchor(Pose(), this)
    }

    override fun unpersistAnchor(uuid: UUID) {
        anchorUuids.remove(uuid)
    }

    override fun persistAnchor(anchor: Anchor) {
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
