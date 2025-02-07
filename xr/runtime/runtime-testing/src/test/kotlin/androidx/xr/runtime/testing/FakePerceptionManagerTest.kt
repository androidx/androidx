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
import androidx.xr.runtime.internal.HitResult
import androidx.xr.runtime.internal.Trackable
import androidx.xr.runtime.internal.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakePerceptionManagerTest {

    lateinit var underTest: FakePerceptionManager

    @Before
    fun setUp() {
        underTest = FakePerceptionManager()
        FakeRuntimeAnchor.anchorsCreated = 0
    }

    @Test
    fun createAnchor_addsAnchorToAnchors() {
        val anchor = underTest.createAnchor(Pose())

        assertThat(underTest.anchors).containsExactly(anchor)
    }

    @Test
    fun detachAnchor_removesAnchorFromAnchors() {
        val anchor = underTest.createAnchor(Pose())
        check(underTest.anchors.contains(anchor))

        anchor.detach()

        assertThat(underTest.anchors).isEmpty()
    }

    @Test
    fun createAnchor_returnsAnchorWithTheGivenPose() {
        val pose = Pose(translation = Vector3(1f, 2f, 3f))

        val anchor = underTest.createAnchor(pose)

        assertThat(anchor.pose).isEqualTo(pose)
    }

    @Test
    fun createAnchor_returnsAnchorWithTrackingStateTracking() {
        val anchor = underTest.createAnchor(Pose())

        assertThat(anchor.trackingState).isEqualTo(TrackingState.Tracking)
    }

    @Test
    fun detach_removesAnchorFromAnchors() {
        val anchor = underTest.createAnchor(Pose())
        check(underTest.anchors.contains(anchor))

        anchor.detach()

        assertThat(underTest.anchors).isEmpty()
    }

    @Test
    fun hitTest_returnsAddedHitResult() {
        val ray = Ray(Vector3(1f, 2f, 3f), Vector3(4f, 5f, 6f))
        val hitResult = HitResult(distance = 1f, Pose(), createStubTrackable())
        underTest.addHitResult(hitResult)

        assertThat(underTest.hitTest(ray)).containsExactly(hitResult)
    }

    @Test
    fun clearHitResults_removesAllHitResults() {
        val ray = Ray(Vector3(1f, 2f, 3f), Vector3(4f, 5f, 6f))
        val hitResult = HitResult(distance = 1f, Pose(), createStubTrackable())
        underTest.addHitResult(hitResult)

        underTest.clearHitResults()

        assertThat(underTest.hitTest(ray)).isEmpty()
    }

    @Test
    fun addTrackable_addsTrackableToTrackables() {
        val trackable = createStubTrackable()

        underTest.addTrackable(trackable)

        assertThat(underTest.trackables).containsExactly(trackable)
    }

    @Test
    fun clearTrackables_removesAllTrackables() {
        val trackable = createStubTrackable()
        underTest.addTrackable(trackable)

        underTest.clearTrackables()

        assertThat(underTest.trackables).isEmpty()
    }

    @Test
    fun getPersistedAnchorUuids_returnsOneUuid() {
        val anchor = underTest.createAnchor(Pose())
        anchor.persist()

        assertThat(underTest.getPersistedAnchorUuids()).containsExactly(anchor.uuid!!)
    }

    @Test
    fun loadAnchor_returnsNewAnchor() {
        val persistedAnchor = underTest.createAnchor(Pose())
        persistedAnchor.persist()

        val loadedAnchor = underTest.loadAnchor(persistedAnchor.uuid!!)

        assertThat(loadedAnchor.pose).isEqualTo(Pose())
    }

    @Test
    fun unpersistAnchor_removesAnchorFromAnchorUuids() {
        val anchor = underTest.createAnchor(Pose())
        anchor.persist()
        check(underTest.getPersistedAnchorUuids().contains(anchor.uuid!!))

        underTest.unpersistAnchor(anchor.uuid!!)

        assertThat(underTest.getPersistedAnchorUuids()).isEmpty()
    }

    private fun createStubTrackable() =
        object : Trackable, AnchorHolder {
            override fun createAnchor(pose: Pose): Anchor = underTest.createAnchor(pose)

            override fun detachAnchor(anchor: Anchor) {
                underTest.detachAnchor(anchor)
            }

            override fun persistAnchor(anchor: Anchor) {
                underTest.persistAnchor(anchor)
            }

            override val trackingState = TrackingState.Tracking
        }
}
