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
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.arcore.testing.TestPlane
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AnchorTest {
    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session
    private lateinit var xrResourcesManager: XrResourcesManager

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        shadowOf(activity.application).grantPermissions(SCENE_UNDERSTANDING_COARSE)

        activityController.create().start().resume()

        session =
            (Session.create(context = activity, coroutineContext = testDispatcher)
                    as SessionCreateSuccess)
                .session
        session.configure(
            Config.Builder()
                .setAnchorPersistence(AnchorPersistenceMode.LOCAL)
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .build()
        )
        xrResourcesManager =
            session.stateExtenders
                .filterIsInstance<PerceptionStateExtender>()
                .first()
                .xrResourcesManager
    }

    @After
    fun cleanUp() {
        arCoreTestRule.deviceTester.isCameraTracking = true
    }

    @Test
    fun create_anchorLimitReached_returnsAnchorResourcesExhausted() {
        arCoreTestRule.anchorResourceLimit = 6
        repeat(arCoreTestRule.anchorResourceLimit) { Anchor.create(session, Pose()) }

        assertThat(Anchor.create(session, Pose()))
            .isInstanceOf(AnchorCreateResourcesExhausted::class.java)
    }

    @Test
    fun create_notTracking_returnsAnchorNotTracking() =
        runTest(testDispatcher) {
            arCoreTestRule.deviceTester.isCameraTracking = false
            advanceUntilIdle()

            assertThat(Anchor.create(session, Pose()))
                .isInstanceOf(AnchorCreateTrackingUnavailable::class.java)
        }

    @Test
    fun detach_removeAnchorFromActiveAnchorManager() =
        runTest(testDispatcher) {
            val result = Anchor.create(session, Pose())
            check(result is AnchorCreateSuccess)

            val underTest = result.anchor
            assertThat(xrResourcesManager.updatables).contains(underTest)
            underTest.detach()

            assertThat(xrResourcesManager.updatables).isEmpty()
        }

    @Test
    @Suppress("DEPRECATION")
    fun update_poseMatchesRuntimePose() =
        runTest(testDispatcher) {
            // Create a trackable for purposes of attaching, and later moving, the anchor
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(testPlane)
            advanceUntilIdle()
            var trackable: Plane? = null
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { trackable = it.first() }
            }
            advanceUntilIdle()
            check(trackable != null)

            // Create the anchor on the trackable at initialPose
            val initialPose = Pose()
            val anchorResult = trackable.createAnchor(initialPose)
            check(anchorResult is AnchorCreateSuccess)
            val underTest = anchorResult.anchor
            assertThat(underTest.state.value.pose).isEqualTo(initialPose)

            // Move the trackable to a new location
            testPlane.centerPose = Pose(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f))
            advanceUntilIdle()

            // Anchor has moved in perception space along with the trackable
            val expectedNewPose = testPlane.centerPose.compose(initialPose)
            assertThat(underTest.state.value.pose).isEqualTo(expectedNewPose)
        }

    @Test
    fun persist_runtimeAnchorIsPersisted() =
        runTest(testDispatcher) {
            val underTest = (Anchor.create(session, Pose()) as AnchorCreateSuccess).anchor
            val uuid: UUID = underTest.persist()
            advanceUntilIdle()

            assertThat(uuid).isNotNull()
            assertThat(underTest.runtimeAnchor.persistenceState)
                .isEqualTo(androidx.xr.arcore.runtime.Anchor.PersistenceState.PERSISTED)
        }

    @Test
    fun persist_anchorPersistenceDisabled_throwsIllegalStateException() {
        val anchorResult = Anchor.create(session, Pose())
        check(anchorResult is AnchorCreateSuccess)

        val underTest = anchorResult.anchor
        session.configure(
            Config.Builder().setAnchorPersistence(AnchorPersistenceMode.DISABLED).build()
        )

        runTest(testDispatcher) { assertFailsWith<IllegalStateException> { underTest.persist() } }
    }

    @Test
    fun getPersistedAnchorUuids_previouslyPersistedAnchor_returnsPersistedAnchorUuid() =
        runTest(testDispatcher) {
            val anchorResult = Anchor.create(session, Pose())
            check(anchorResult is AnchorCreateSuccess)

            val underTest = anchorResult.anchor
            val uuid: UUID = underTest.persist()
            advanceUntilIdle()

            assertThat(Anchor.getPersistedAnchorUuids(session)).containsExactly(uuid)
        }

    @Test
    fun getPersistedAnchorUuids_noPreviouslyPersistedAnchors_returnsEmptyList() {
        assertThat(Anchor.getPersistedAnchorUuids(session)).isEmpty()
    }

    @Test
    fun getPersistedAnchorUuids_anchorPersistenceDisabled_throwsIllegalStateException() {
        session.configure(
            Config.Builder().setAnchorPersistence(AnchorPersistenceMode.DISABLED).build()
        )

        assertFailsWith<IllegalStateException> { Anchor.getPersistedAnchorUuids(session) }
    }

    @Test
    fun load_previouslyPersistedAnchor_returnsAnchorCreateSuccess() =
        runTest(testDispatcher) {
            val uuid = arCoreTestRule.persistAnchor(Pose())
            advanceUntilIdle()

            val underTest = Anchor.load(session, uuid)

            assertThat(underTest).isInstanceOf(AnchorCreateSuccess::class.java)
        }

    @Test
    fun load_invalidUuid_throwsInvalidUuidException() {
        assertFailsWith<AnchorInvalidUuidException> { Anchor.load(session, UUID.randomUUID()) }
        assertFailsWith<AnchorInvalidUuidException> { Anchor.load(session, UUID(0L, 0L)) }
    }

    @Test
    fun load_anchorLimitReached_returnsAnchorResourcesExhausted() =
        runTest(testDispatcher) {
            val anchorResult = Anchor.create(session, Pose())
            check(anchorResult is AnchorCreateSuccess)

            val anchor = anchorResult.anchor
            val uuid: UUID = anchor.persist()
            advanceUntilIdle()

            arCoreTestRule.anchorResourceLimit = 6
            repeat(arCoreTestRule.anchorResourceLimit - 1) { Anchor.load(session, uuid) }

            assertThat(Anchor.load(session, uuid))
                .isInstanceOf(AnchorCreateResourcesExhausted::class.java)
        }

    @Test
    fun load_anchorPersistenceDisabled_throwsIllegalStateException() {
        session.configure(
            Config.Builder().setAnchorPersistence(AnchorPersistenceMode.DISABLED).build()
        )

        assertFailsWith<IllegalStateException> { Anchor.load(session, UUID.randomUUID()) }
    }

    @Test
    fun unpersist_removesAnchorFromStorage() =
        runTest(testDispatcher) {
            val underTest = (Anchor.create(session, Pose()) as AnchorCreateSuccess).anchor
            val uuid: UUID = underTest.persist()
            advanceUntilIdle()

            Anchor.unpersist(session, uuid)
            assertThat(Anchor.getPersistedAnchorUuids(session)).doesNotContain(uuid)
        }

    @Test
    fun unpersist_anchorPersistenceDisabled_throwsIllegalStateException() {
        session.configure(
            Config.Builder().setAnchorPersistence(AnchorPersistenceMode.DISABLED).build()
        )

        assertFailsWith<IllegalStateException> { Anchor.unpersist(session, UUID.randomUUID()) }
    }

    @Test
    fun unpersist_invalidUuid_throwsAnchorInvalidUuidException() {
        assertFailsWith<AnchorInvalidUuidException> { Anchor.unpersist(session, UUID.randomUUID()) }
        assertFailsWith<AnchorInvalidUuidException> { Anchor.unpersist(session, UUID(0L, 0L)) }
    }

    @Test
    fun equals_sameObject_returnsTrue() {
        val anchorResult = Anchor.create(session, Pose())
        check(anchorResult is AnchorCreateSuccess)

        val underTest = anchorResult.anchor
        assertThat(underTest).isEqualTo(underTest)
    }

    @Test
    fun equals_differentObjectsSameValues_returnsFalse() {
        val firstAnchorResult = Anchor.create(session, Pose())
        val secondAnchorResult = Anchor.create(session, Pose())
        check(firstAnchorResult is AnchorCreateSuccess && secondAnchorResult is AnchorCreateSuccess)

        val underTest1 = firstAnchorResult.anchor
        val underTest2 = secondAnchorResult.anchor

        assertThat(underTest1).isNotEqualTo(underTest2)
    }

    @Test
    fun equals_differentObjectsDifferentValues_returnsFalse() {
        val firstAnchorResult = Anchor.create(session, Pose(Vector3.Right, Quaternion.Identity))
        val secondAnchorResult = Anchor.create(session, Pose(Vector3.Forward, Quaternion.Identity))
        check(firstAnchorResult is AnchorCreateSuccess && secondAnchorResult is AnchorCreateSuccess)

        val underTest1 = firstAnchorResult.anchor
        val underTest2 = secondAnchorResult.anchor

        assertThat(underTest1).isNotEqualTo(underTest2)
    }

    @Test
    fun hashCode_differentObjectsSameValues_returnsDifferentHashCodes() {
        val firstAnchorResult = Anchor.create(session, Pose())
        val secondAnchorResult = Anchor.create(session, Pose())
        check(firstAnchorResult is AnchorCreateSuccess && secondAnchorResult is AnchorCreateSuccess)

        val underTest1 = firstAnchorResult.anchor
        val underTest2 = secondAnchorResult.anchor

        assertThat(underTest1.hashCode()).isNotEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCode_differentObjectsDifferentValues_returnsDifferentHashCodes() {
        val firstAnchorResult = Anchor.create(session, Pose(Vector3.Right, Quaternion.Identity))
        val secondAnchorResult = Anchor.create(session, Pose(Vector3.Forward, Quaternion.Identity))
        check(firstAnchorResult is AnchorCreateSuccess && secondAnchorResult is AnchorCreateSuccess)

        val underTest1 = firstAnchorResult.anchor
        val underTest2 = secondAnchorResult.anchor

        assertThat(underTest1.hashCode()).isNotEqualTo(underTest2.hashCode())
    }
}
