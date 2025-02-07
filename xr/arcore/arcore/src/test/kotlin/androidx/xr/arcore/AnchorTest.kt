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

package androidx.xr.arcore

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.internal.Anchor as RuntimeAnchor
import androidx.xr.runtime.internal.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakePerceptionManager
import androidx.xr.runtime.testing.FakeRuntimeAnchor
import androidx.xr.runtime.testing.FakeRuntimePlane
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnchorTest {

    private lateinit var xrResourcesManager: XrResourcesManager
    private lateinit var session: Session

    @get:Rule
    val grantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.SCENE_UNDERSTANDING",
            "android.permission.HAND_TRACKING",
        )

    @Before
    fun setUp() {
        xrResourcesManager = XrResourcesManager()
        FakeRuntimeAnchor.anchorsCreated = 0
    }

    @After
    fun tearDown() {
        xrResourcesManager.clear()
    }

    @Test
    fun create_anchorLimitReached_returnsAnchorResourcesExhausted() = createTestSessionAndRunTest {
        repeat(FakeRuntimeAnchor.ANCHOR_RESOURCE_LIMIT) { Anchor.create(session, Pose()) }

        assertThat(Anchor.create(session, Pose()))
            .isInstanceOf(AnchorCreateResourcesExhausted::class.java)
    }

    @Test
    fun detach_removeAnchorFromActiveAnchorManager() {
        val runtimeAnchor = FakeRuntimeAnchor(Pose())
        val underTest = Anchor(runtimeAnchor, xrResourcesManager)
        xrResourcesManager.addUpdatable(underTest)
        check(xrResourcesManager.updatables.contains(underTest))
        check(xrResourcesManager.updatables.size == 1)

        underTest.detach()

        assertThat(xrResourcesManager.updatables).isEmpty()
    }

    @Test
    fun detach_stopsUpdateAndQueuesAnchorToBeDetached() {
        val runtimeAnchor = FakeRuntimePlane().createAnchor(Pose()) as FakeRuntimeAnchor
        check(runtimeAnchor.isAttached)
        val underTest = Anchor(runtimeAnchor, xrResourcesManager)
        xrResourcesManager.addUpdatable(underTest)
        check(xrResourcesManager.updatables.contains(underTest))
        check(xrResourcesManager.updatables.size == 1)

        underTest.detach()

        assertThat(xrResourcesManager.updatables).isEmpty()
        assertThat(xrResourcesManager.anchorsToDetachQueue.toList()).containsExactly(underTest)
    }

    @Test
    fun update_trackingStateMatchesRuntimeTrackingState() = runBlocking {
        val runtimeAnchor = FakeRuntimeAnchor(Pose())
        runtimeAnchor.trackingState = TrackingState.Paused
        val underTest = Anchor(runtimeAnchor, xrResourcesManager)
        check(underTest.state.value.trackingState.equals(TrackingState.Paused))
        runtimeAnchor.trackingState = TrackingState.Tracking

        underTest.update()

        assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.Tracking)
    }

    @Test
    fun update_poseMatchesRuntimePose() = runBlocking {
        val runtimeAnchor = FakeRuntimeAnchor(Pose())
        val underTest = Anchor(runtimeAnchor, xrResourcesManager)
        check(
            underTest.state.value.pose.equals(
                Pose(Vector3(0f, 0f, 0f), Quaternion(0f, 0f, 0f, 1.0f))
            )
        )
        val newPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        runtimeAnchor.pose = newPose

        underTest.update()

        assertThat(underTest.state.value.pose).isEqualTo(newPose)
    }

    @Test
    fun persist_runtimeAnchorIsPersisted() = runTest {
        val runtimeAnchor = FakeRuntimeAnchor(Pose())
        val underTest = Anchor(runtimeAnchor, xrResourcesManager)
        check(runtimeAnchor.persistenceState == RuntimeAnchor.PersistenceState.NotPersisted)

        var uuid: UUID? = null
        val persistJob = launch { uuid = underTest.persist() }
        val updateJob = launch { underTest.update() }
        updateJob.join()
        persistJob.join()

        assertThat(uuid).isNotNull()
        assertThat(runtimeAnchor.persistenceState)
            .isEqualTo(RuntimeAnchor.PersistenceState.Persisted)
    }

    @Test
    fun getPersistedAnchorUuids_previouslyPersistedAnchor_returnsPersistedAnchorUuid() =
        createTestSessionAndRunTest {
            runTest {
                val runtimeAnchor =
                    FakeRuntimeAnchor(
                        Pose(),
                        session.runtime.perceptionManager as FakePerceptionManager
                    )
                val underTest = Anchor(runtimeAnchor, xrResourcesManager)
                var uuid: UUID? = null
                val persistJob = launch { uuid = underTest.persist() }
                val updateJob = launch { underTest.update() }
                updateJob.join()
                persistJob.join()

                assertThat(Anchor.getPersistedAnchorUuids(session)).containsExactly(uuid)
            }
        }

    @Test
    fun getPersistedAnchorUuids_noPreviouslyPersistedAnchors_returnsEmptyList() =
        createTestSessionAndRunTest {
            assertThat(Anchor.getPersistedAnchorUuids(session)).isEmpty()
        }

    @Test
    fun load_previouslyPersistedAnchor_returnsAnchorCreateSuccess() = createTestSessionAndRunTest {
        runTest {
            val runtimeAnchor =
                FakeRuntimeAnchor(
                    Pose(),
                    session.runtime.perceptionManager as FakePerceptionManager
                )
            val underTest = Anchor(runtimeAnchor, xrResourcesManager)
            var uuid: UUID? = null
            val persistJob = launch { uuid = underTest.persist() }
            val updateJob = launch { underTest.update() }
            updateJob.join()
            persistJob.join()

            assertThat(Anchor.load(session, uuid!!)).isInstanceOf(AnchorCreateSuccess::class.java)
        }
    }

    @Test
    fun load_anchorLimitReached_returnsAnchorResourcesExhausted() = createTestSessionAndRunTest {
        runTest {
            val anchor = (Anchor.create(session, Pose()) as AnchorCreateSuccess).anchor
            var uuid: UUID? = null
            val persistJob = launch { uuid = anchor.persist() }
            val updateJob = launch { anchor.update() }
            updateJob.join()
            persistJob.join()
            repeat(FakeRuntimeAnchor.ANCHOR_RESOURCE_LIMIT - 1) { Anchor.load(session, uuid!!) }

            assertThat(Anchor.load(session, uuid!!))
                .isInstanceOf(AnchorCreateResourcesExhausted::class.java)
        }
    }

    @Test
    fun loadFromNativePointer_returnsAnchorCreateSuccess() = createTestSessionAndRunTest {
        assertThat(Anchor.loadFromNativePointer(session, 123L)).isNotNull()
    }

    @Test
    fun unpersist_removesAnchorFromStorage() = createTestSessionAndRunTest {
        runTest {
            val runtimeAnchor =
                FakeRuntimeAnchor(
                    Pose(),
                    session.runtime.perceptionManager as FakePerceptionManager
                )
            val underTest = Anchor(runtimeAnchor, xrResourcesManager)
            var uuid: UUID? = null
            val persistJob = launch { uuid = underTest.persist() }
            val updateJob = launch { underTest.update() }
            updateJob.join()
            persistJob.join()

            Anchor.unpersist(session, uuid!!)

            assertThat(Anchor.getPersistedAnchorUuids(session)).doesNotContain(uuid)
        }
    }

    @Test
    fun equals_sameObject_returnsTrue() {
        val underTest = Anchor(FakeRuntimeAnchor(Pose()), xrResourcesManager)

        assertThat(underTest.equals(underTest)).isTrue()
    }

    @Test
    fun equals_differentObjectsSameValues_returnsTrue() {
        val runtimeAnchor = FakeRuntimeAnchor(Pose())
        val underTest1 = Anchor(runtimeAnchor, xrResourcesManager)
        val underTest2 = Anchor(runtimeAnchor, xrResourcesManager)

        assertThat(underTest1.equals(underTest2)).isTrue()
    }

    @Test
    fun equals_differentObjectsDifferentValues_returnsFalse() {
        val underTest1 =
            Anchor(FakeRuntimeAnchor(Pose(Vector3.Up, Quaternion.Identity)), xrResourcesManager)
        val underTest2 =
            Anchor(FakeRuntimeAnchor(Pose(Vector3.Down, Quaternion.Identity)), xrResourcesManager)

        assertThat(underTest1.equals(underTest2)).isFalse()
    }

    @Test
    fun hashCode_differentObjectsSameValues_returnsSameHashCode() {
        val runtimeAnchor = FakeRuntimeAnchor(Pose())
        val underTest1 = Anchor(runtimeAnchor, xrResourcesManager)
        val underTest2 = Anchor(runtimeAnchor, xrResourcesManager)

        assertThat(underTest1.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCode_differentObjectsDifferentValues_returnsDifferentHashCodes() {
        val underTest1 =
            Anchor(FakeRuntimeAnchor(Pose(Vector3.Up, Quaternion.Identity)), xrResourcesManager)
        val underTest2 =
            Anchor(FakeRuntimeAnchor(Pose(Vector3.Down, Quaternion.Identity)), xrResourcesManager)

        assertThat(underTest1.hashCode()).isNotEqualTo(underTest2.hashCode())
    }

    private fun createTestSessionAndRunTest(testBody: () -> Unit) {
        ActivityScenario.launch(Activity::class.java).use {
            it.onActivity { activity ->
                session =
                    (Session.create(activity, StandardTestDispatcher()) as SessionCreateSuccess)
                        .session

                testBody()
            }
        }
    }
}
