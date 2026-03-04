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

package androidx.xr.scenecore

import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.runtime.Plane
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionManager
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.arcore.testing.FakeRuntimePlane
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.testing.FakeAnchorEntity
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.TestTimeSource
import kotlin.time.toDuration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class AnchorEntityTest {
    private val fakeAnchorEntity = FakeAnchorEntity()
    private lateinit var entityManager: EntityManager
    private lateinit var session: Session
    private lateinit var anchor: Anchor
    private lateinit var mFakeRuntime: FakePerceptionRuntime
    private lateinit var mFakeLifecycleManager: FakeLifecycleManager
    private lateinit var mFakePerceptionManager: FakePerceptionManager
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var timeSource: TestTimeSource
    private var mCurrentTimeMillis: Long = 1000000000L

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        createSession()
        mFakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
        mFakeLifecycleManager = mFakeRuntime.lifecycleManager
        mFakePerceptionManager = mFakeRuntime.perceptionManager
        timeSource = mFakeLifecycleManager.timeSource
        SystemClock.setCurrentTimeMillis(mCurrentTimeMillis)
    }

    @After
    fun tearDown() {
        anchor.runtimeAnchor.detach()
        if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            activityController.destroy()
        }
    }

    @Test
    fun createViaAnchor_returnsAnchoredEntity() {
        val anchorEntity = AnchorEntity.create(session, anchor)

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
    }

    @Test
    fun createViaSemantic_noPlanes_returnsUnanchoredEntity() {
        val anchorEntity =
            AnchorEntity.create(
                session,
                FloatSize2d(1.0f, 1.0f),
                PlaneOrientation.ANY,
                PlaneSemanticType.ANY,
                timeout = 0.toDuration(DurationUnit.SECONDS).toJavaDuration(),
            )

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
    }

    @Test
    fun createViaSemantic_noViablePlanes_returnsUnanchoredEntity() {
        val plane = FakeRuntimePlane(type = Plane.Type.VERTICAL, label = Plane.Label.WALL)
        mFakePerceptionManager.addTrackable(plane)
        val anchorEntity =
            AnchorEntity.create(
                session,
                FloatSize2d(1.0f, 1.0f),
                PlaneOrientation.HORIZONTAL,
                PlaneSemanticType.CEILING,
                timeout = 0.toDuration(DurationUnit.SECONDS).toJavaDuration(),
            )

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createViaSemantic_withinTimeout_returnAnchoredEntity() {
        runTest(testDispatcher) {
            activityController.create().start().resume()
            val plane =
                FakeRuntimePlane(
                    type = Plane.Type.HORIZONTAL_DOWNWARD_FACING,
                    label = Plane.Label.CEILING,
                    extents = FloatSize2d(1.0f, 1.0f),
                )
            mFakePerceptionManager.addTrackable(plane)
            val anchorEntity =
                AnchorEntity.create(
                    session,
                    FloatSize2d(1.0f, 1.0f),
                    PlaneOrientation.HORIZONTAL,
                    PlaneSemanticType.CEILING,
                    timeout = 0.toDuration(DurationUnit.SECONDS).toJavaDuration(),
                )
            advanceUntilIdle()

            assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createViaSemantic_twice_doesNotReanchor() {
        runTest(testDispatcher) {
            activityController.create().start().resume()
            val plane1 =
                FakeRuntimePlane(
                    type = Plane.Type.HORIZONTAL_DOWNWARD_FACING,
                    label = Plane.Label.CEILING,
                    extents = FloatSize2d(1.0f, 1.0f),
                )
            mFakePerceptionManager.addTrackable(plane1)

            val anchorEntity =
                AnchorEntity.create(
                    session,
                    FloatSize2d(1.0f, 1.0f),
                    PlaneOrientation.HORIZONTAL,
                    PlaneSemanticType.CEILING,
                    timeout = 0.toDuration(DurationUnit.SECONDS).toJavaDuration(),
                )
            advanceUntilIdle()

            assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
            val anchor1 = anchorEntity.getAnchor()
            assertThat(anchor1).isNotNull()

            // Add another matching plane
            val plane2 =
                FakeRuntimePlane(
                    type = Plane.Type.HORIZONTAL_DOWNWARD_FACING,
                    label = Plane.Label.CEILING,
                    extents = FloatSize2d(1.0f, 1.0f),
                )
            mFakePerceptionManager.addTrackable(plane2)
            advanceUntilIdle()

            // Should still be anchored to the first one
            assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
            assertThat(anchorEntity.getAnchor()).isEqualTo(anchor1)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createViaSemantic_pastTimeout_returnsTimedOutAnchorEntity() {
        runTest(testDispatcher) {
            activityController.create().start().resume()
            val plane =
                FakeRuntimePlane(
                    type = Plane.Type.HORIZONTAL_DOWNWARD_FACING,
                    label = Plane.Label.CEILING,
                    extents = FloatSize2d(1.0f, 1.0f),
                )

            val anchorEntity =
                AnchorEntity.create(
                    session,
                    FloatSize2d(1.0f, 1.0f),
                    PlaneOrientation.HORIZONTAL,
                    PlaneSemanticType.CEILING,
                    timeout = 5.seconds.toJavaDuration(),
                )
            advanceUntilIdle()

            assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
            advanceClock(6.seconds)
            mFakePerceptionManager.addTrackable(plane)

            mFakeLifecycleManager.allowOneMoreCallToUpdate()
            advanceUntilIdle()

            assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.TIMEDOUT)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createViaSemantic_zeroTimeout_keepsSearching() {
        runTest(testDispatcher) {
            val anchorAttempts = 100
            activityController.create().start().resume()
            val anchorEntity =
                AnchorEntity.create(
                    session,
                    FloatSize2d(1.0f, 1.0f),
                    PlaneOrientation.HORIZONTAL,
                    PlaneSemanticType.CEILING,
                    timeout = 0.toDuration(DurationUnit.SECONDS).toJavaDuration(),
                )
            advanceUntilIdle()

            // Check once every 5 seconds up to 500 seconds
            for (i in 0 until anchorAttempts) {
                advanceClock(5.seconds)
                mFakeLifecycleManager.allowOneMoreCallToUpdate()
                advanceUntilIdle()
                assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
            }
        }
    }

    @Test
    fun setOnStateChangedListener_receivesStateChangedCallback() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        val stateChangedListener =
            Consumer<AnchorEntity.State> { newState ->
                assertThat(newState).isEqualTo(AnchorEntity.State.ANCHORED)
            }

        anchorEntity.setOnStateChangedListener(directExecutor(), stateChangedListener)
    }

    @Test
    fun setOnOriginChangedListener_withNullParams_callsRuntimeSetOnOriginChangedListener() {
        val anchorEntity = AnchorEntity.create(fakeAnchorEntity, entityManager)
        anchorEntity.setOnOriginChangedListener(null)
        assertThat(fakeAnchorEntity.onOriginChangedListener).isNull()
    }

    @Test
    fun setOnOriginChangedListener_receivesRuntimeSetOnOriginChangedListenerCallbacks() {
        var listenerCalled = false
        val anchorEntity = AnchorEntity.create(fakeAnchorEntity, entityManager)
        anchorEntity.setOnOriginChangedListener(directExecutor()) { listenerCalled = true }

        assertThat(fakeAnchorEntity.onOriginChangedListener).isNotNull()
        assertThat(listenerCalled).isFalse()

        // Simulates a runtime callback.
        fakeAnchorEntity.onOriginChanged()

        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun getParentSpacePose_throwsIllegalArgumentException() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        assertThrows(IllegalArgumentException::class.java) { anchorEntity.getPose(Space.PARENT) }
    }

    @Test
    fun getActivitySpacePose_returnsIdentity() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        val pose = anchorEntity.getPose(Space.ACTIVITY)
        assertThat(pose.translation).isEqualTo(anchor.runtimeAnchor.pose.translation)
        assertThat(pose.rotation).isEqualTo(anchor.runtimeAnchor.pose.rotation)
    }

    @Test
    fun getRealWorldSpacePose_returnsPerceptionSpacePose() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        val pose = anchorEntity.getPose(Space.REAL_WORLD)
        assertThat(pose.translation).isEqualTo(anchor.runtimeAnchor.pose.translation)
        assertThat(pose.rotation).isEqualTo(anchor.runtimeAnchor.pose.rotation)
    }

    @Test
    fun setPose_throwsUnsupportedOperationException() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setPose(Pose(Vector3.Zero, Quaternion.Identity))
        }
    }

    @Test
    fun getParentNonUniformSpaceScale_throwsIllegalArgumentException() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        assertThrows(IllegalArgumentException::class.java) {
            anchorEntity.getNonUniformScale(Space.PARENT)
        }
    }

    @Test
    fun getActivityNonUniformSpaceScale_returnsIdentity() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        val scale = anchorEntity.getNonUniformScale(Space.ACTIVITY)
        assertThat(scale).isEqualTo(Vector3.One)
    }

    @Test
    fun getParentSpaceScale_throwsIllegalArgumentException() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        assertThrows(IllegalArgumentException::class.java) { anchorEntity.getScale(Space.PARENT) }
    }

    @Test
    fun getActivitySpaceScale_returnsIdentity() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        val scale = anchorEntity.getScale(Space.ACTIVITY)
        assertThat(scale).isEqualTo(1f)
    }

    @Test
    fun getRealWorldSpaceScale_returnsIdentity() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        val scale = anchorEntity.getScale(Space.REAL_WORLD)
        assertThat(scale).isEqualTo(1f)
    }

    @Test
    fun getRealWorldSpaceNonUniformScale_returnsIdentity() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        val scale = anchorEntity.getNonUniformScale(Space.REAL_WORLD)
        assertThat(scale).isEqualTo(Vector3.One)
    }

    @Test
    fun setScale_float_throwsUnsupportedOperationException() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setScale(1f, Space.PARENT)
        }
    }

    @Test
    fun setScale_vector_throwsUnsupportedOperationException() {
        val anchorEntity = AnchorEntity.create(session, anchor)
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setScale(Vector3.One, Space.PARENT)
        }
    }

    @Test
    fun dispose_clearsListeners() {
        val anchorEntity = AnchorEntity.create(fakeAnchorEntity, entityManager)

        anchorEntity.setOnStateChangedListener(directExecutor(), {})
        anchorEntity.setOnOriginChangedListener(directExecutor(), {})

        assertThat(fakeAnchorEntity.onOriginChangedListener).isNotNull()
        assertThat(anchorEntity.onStateChangedListener).isNotNull()

        anchorEntity.dispose()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(fakeAnchorEntity.onOriginChangedListener).isNull()
        assertThat(anchorEntity.onStateChangedListener).isNull()
    }

    @Test
    fun dispose_callingTwiceDoesNotCrash() {
        val anchorEntity = AnchorEntity.create(fakeAnchorEntity, entityManager)
        anchorEntity.dispose()
        anchorEntity.dispose()
    }

    private fun createSession(coroutineDispatcher: CoroutineDispatcher = testDispatcher) {
        val result = Session.create(activity, coroutineDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        val anchorPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion.Identity)
        anchor = (Anchor.create(session, anchorPose) as AnchorCreateSuccess).anchor
        entityManager = session.scene.entityManager
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun advanceClock(duration: Duration) {
        mCurrentTimeMillis += duration.toJavaDuration().toMillis()
        SystemClock.setCurrentTimeMillis(mCurrentTimeMillis)
        timeSource += duration
        delay(duration)
    }
}
