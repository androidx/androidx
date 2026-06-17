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
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.runtime.Plane
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.testing.AnchorSpaceTester
import androidx.xr.scenecore.testing.MemoryUtils
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.lang.ref.WeakReference
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class AnchorSpaceTest {

    @Rule @JvmField val scenecoreTestRule = SceneCoreTestRule()

    private lateinit var session: Session
    private lateinit var anchor: Anchor
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    @Suppress("DEPRECATION")
    private lateinit var mFakeRuntime: androidx.xr.arcore.testing.FakePerceptionRuntime
    @Suppress("DEPRECATION")
    private lateinit var mFakePerceptionManager: androidx.xr.arcore.testing.FakePerceptionManager
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var timeSource: TestTimeSource
    private var mCurrentTimeMillis: Long = 1000000000L

    @Before
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun setup(): Unit = runBlocking {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        createSession()
        mFakeRuntime =
            session.runtimes
                .filterIsInstance<androidx.xr.arcore.testing.FakePerceptionRuntime>()
                .first()
        mFakePerceptionManager = mFakeRuntime.perceptionManager
        timeSource = mFakeRuntime.timeSource
        SystemClock.setCurrentTimeMillis(mCurrentTimeMillis)
    }

    @After
    fun tearDown() {
        if (::anchor.isInitialized) {
            anchor.runtimeAnchor.detach()
        }
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun createViaAnchor_returnsAnchoredEntity() {
        val anchorSpace = AnchorSpace.create(session, anchor)

        assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.ANCHORED)
    }

    @Test
    fun createViaAnchor_anchor_returnsProvidedAnchor() {
        val anchorSpace = AnchorSpace.create(session, anchor)

        assertThat(anchorSpace.anchor).isEqualTo(anchor)
    }

    @Test
    fun createViaSemantic_noPlanes_returnsUnanchoredEntity() {
        val anchorSpace =
            AnchorSpace.create(
                session,
                FloatSize2d(1.0f, 1.0f),
                PlaneOrientation.ALL,
                PlaneSemanticType.ALL,
                timeout = 0.toDuration(DurationUnit.SECONDS).toJavaDuration(),
            )

        assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.UNANCHORED)
    }

    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createViaSemantic_noViablePlanes_returnsUnanchoredEntity() {
        val plane =
            androidx.xr.arcore.testing.FakeRuntimePlane(
                type = Plane.Type.VERTICAL,
                label = Plane.Label.WALL,
            )
        mFakePerceptionManager.addTrackable(plane)
        val anchorSpace =
            AnchorSpace.create(
                session,
                FloatSize2d(1.0f, 1.0f),
                setOf(PlaneOrientation.HORIZONTAL),
                setOf(PlaneSemanticType.CEILING),
                timeout = 0.toDuration(DurationUnit.SECONDS).toJavaDuration(),
            )

        assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.UNANCHORED)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createViaSemantic_withinTimeout_returnAnchoredEntity() {
        return runTest(testDispatcher) {
            activityController.create().start().resume()
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    type = Plane.Type.HORIZONTAL_DOWNWARD_FACING,
                    label = Plane.Label.CEILING,
                    extents = FloatSize2d(1.0f, 1.0f),
                )
            mFakePerceptionManager.addTrackable(plane)
            val anchorSpace =
                AnchorSpace.create(
                    session,
                    FloatSize2d(1.0f, 1.0f),
                    setOf(PlaneOrientation.HORIZONTAL),
                    setOf(PlaneSemanticType.CEILING),
                    timeout = 0.toDuration(DurationUnit.SECONDS).toJavaDuration(),
                )
            advanceUntilIdle()

            assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.ANCHORED)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createViaSemantic_twice_doesNotReanchor() {
        return runTest(testDispatcher) {
            activityController.create().start().resume()
            val plane1 =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    type = Plane.Type.HORIZONTAL_DOWNWARD_FACING,
                    label = Plane.Label.CEILING,
                    extents = FloatSize2d(1.0f, 1.0f),
                )
            mFakePerceptionManager.addTrackable(plane1)

            val anchorSpace =
                AnchorSpace.create(
                    session,
                    FloatSize2d(1.0f, 1.0f),
                    setOf(PlaneOrientation.HORIZONTAL),
                    setOf(PlaneSemanticType.CEILING),
                    timeout = 0.toDuration(DurationUnit.SECONDS).toJavaDuration(),
                )
            advanceUntilIdle()

            assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.ANCHORED)
            val anchor1 = anchorSpace.anchor
            assertThat(anchor1).isNotNull()

            // Add another matching plane
            val plane2 =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    type = Plane.Type.HORIZONTAL_DOWNWARD_FACING,
                    label = Plane.Label.CEILING,
                    extents = FloatSize2d(1.0f, 1.0f),
                )
            mFakePerceptionManager.addTrackable(plane2)
            advanceUntilIdle()

            // Should still be anchored to the first one
            assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.ANCHORED)
            assertThat(anchorSpace.anchor).isEqualTo(anchor1)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createViaSemantic_pastTimeout_returnsTimedOutAnchorSpace() {
        return runTest(testDispatcher) {
            activityController.create().start().resume()
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    type = Plane.Type.HORIZONTAL_DOWNWARD_FACING,
                    label = Plane.Label.CEILING,
                    extents = FloatSize2d(1.0f, 1.0f),
                )

            val anchorSpace =
                AnchorSpace.create(
                    session,
                    FloatSize2d(1.0f, 1.0f),
                    setOf(PlaneOrientation.HORIZONTAL),
                    setOf(PlaneSemanticType.CEILING),
                    timeout = 5.seconds.toJavaDuration(),
                )
            advanceUntilIdle()

            assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.UNANCHORED)
            advanceClock(6.seconds)
            mFakePerceptionManager.addTrackable(plane)

            mFakeRuntime.allowOneMoreCallToUpdate()
            advanceUntilIdle()

            assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.TIMED_OUT)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    fun createViaSemantic_zeroTimeout_keepsSearching() {
        return runTest(testDispatcher) {
            val anchorAttempts = 100
            activityController.create().start().resume()
            val anchorSpace =
                AnchorSpace.create(
                    session,
                    FloatSize2d(1.0f, 1.0f),
                    setOf(PlaneOrientation.HORIZONTAL),
                    setOf(PlaneSemanticType.CEILING),
                    timeout = 0.toDuration(DurationUnit.SECONDS).toJavaDuration(),
                )
            advanceUntilIdle()

            // Check once every 5 seconds up to 500 seconds
            for (i in 0 until anchorAttempts) {
                advanceClock(5.seconds)
                mFakeRuntime.allowOneMoreCallToUpdate()
                advanceUntilIdle()
                assertThat(anchorSpace.state).isEqualTo(AnchorSpace.State.UNANCHORED)
            }
        }
    }

    @Test
    fun addStateChangedListener_receivesStateChangedCallback() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        var callbackInvoked = false
        val stateChangedListener =
            Consumer<AnchorSpace.State> { newState ->
                callbackInvoked = true
                assertThat(newState).isEqualTo(AnchorSpace.State.ANCHORED)
            }

        anchorSpace.addStateChangedListener(directExecutor(), stateChangedListener)
        assertThat(callbackInvoked).isTrue()
    }

    @Test
    fun addOriginChangedListener_receivesOnOriginChangedListenerCallbacks() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        val tester = scenecoreTestRule.createTester<AnchorSpaceTester>(anchorSpace)
        var listenerCalled = false
        anchorSpace.addOriginChangedListener(directExecutor()) { listenerCalled = true }

        assertThat(listenerCalled).isFalse()

        // Simulates a runtime callback via tester.
        tester.triggerOnOriginChanged()

        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun getParentSpacePose_throwsIllegalArgumentException() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        assertThrows(IllegalArgumentException::class.java) { anchorSpace.getPose(Space.PARENT) }
    }

    @Test
    fun getActivitySpacePose_returnsIdentity() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        val pose = anchorSpace.getPose(Space.ACTIVITY)
        assertThat(pose.translation).isEqualTo(anchor.runtimeAnchor.pose.translation)
        assertThat(pose.rotation).isEqualTo(anchor.runtimeAnchor.pose.rotation)
    }

    @Test
    fun getRealWorldSpacePose_returnsPerceptionSpacePose() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
        val pose = anchorSpace.getPose(Space.REAL_WORLD)
        assertThat(pose.translation).isEqualTo(anchor.runtimeAnchor.pose.translation)
        assertThat(pose.rotation).isEqualTo(anchor.runtimeAnchor.pose.rotation)
    }

    @Test
    fun setPose_throwsUnsupportedOperationException() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        assertThrows(UnsupportedOperationException::class.java) {
            anchorSpace.setPose(Pose(Vector3.Zero, Quaternion.Identity))
        }
    }

    @Test
    fun getParentNonUniformSpaceScale_throwsIllegalArgumentException() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        assertThrows(IllegalArgumentException::class.java) {
            anchorSpace.getNonUniformScale(Space.PARENT)
        }
    }

    @Test
    fun getActivityNonUniformSpaceScale_returnsIdentity() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        val scale = anchorSpace.getNonUniformScale(Space.ACTIVITY)
        assertThat(scale).isEqualTo(Vector3.One)
    }

    @Test
    fun getParentSpaceScale_throwsIllegalArgumentException() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        assertThrows(IllegalArgumentException::class.java) { anchorSpace.getScale(Space.PARENT) }
    }

    @Test
    fun getActivitySpaceScale_returnsIdentity() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        val scale = anchorSpace.getScale(Space.ACTIVITY)
        assertThat(scale).isEqualTo(1f)
    }

    @Test
    fun getRealWorldSpaceScale_returnsIdentity() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
        val scale = anchorSpace.getScale(Space.REAL_WORLD)
        assertThat(scale).isEqualTo(1f)
    }

    @Test
    fun getRealWorldSpaceNonUniformScale_returnsIdentity() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
        val scale = anchorSpace.getNonUniformScale(Space.REAL_WORLD)
        assertThat(scale).isEqualTo(Vector3.One)
    }

    @Test
    fun setScale_float_throwsUnsupportedOperationException() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        assertThrows(UnsupportedOperationException::class.java) {
            anchorSpace.setScale(1f, Space.PARENT)
        }
    }

    @Test
    fun setScale_vector_throwsUnsupportedOperationException() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        assertThrows(UnsupportedOperationException::class.java) {
            anchorSpace.setScale(Vector3.One, Space.PARENT)
        }
    }

    @Test
    fun disposeInternal_clearsListeners() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        val tester = scenecoreTestRule.createTester<AnchorSpaceTester>(anchorSpace)
        var listenerCalledCount = 0
        anchorSpace.addOriginChangedListener(directExecutor()) { listenerCalledCount++ }

        anchorSpace.disposeInternal()

        // After dispose, triggering via tester should not increment the count.
        tester.triggerOnOriginChanged()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalledCount).isEqualTo(0)
    }

    @Test
    fun disposeInternal_callingTwiceDoesNotCrash() {
        val anchorSpace = AnchorSpace.create(session, anchor)
        anchorSpace.disposeInternal()
        anchorSpace.disposeInternal()
    }

    @Test
    fun garbageCollection_disposesEntity() {
        fun createAnchorSpace(): WeakReference<AnchorSpace> {
            val localAnchorSpace = AnchorSpace.create(session, anchor)
            return WeakReference(localAnchorSpace)
        }

        val anchorSpaceRef = createAnchorSpace()
        assertThat(anchorSpaceRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(anchorSpaceRef)
    }

    private suspend fun createSession(coroutineDispatcher: CoroutineDispatcher = testDispatcher) {
        val result =
            Session.create(
                activity,
                coroutineDispatcher,
                lifecycleOwner = activity as LifecycleOwner,
            )
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session
        session.configure(
            Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
        )
        val anchorPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion.Identity)
        anchor = (Anchor.create(session, anchorPose) as AnchorCreateSuccess).anchor
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun advanceClock(duration: Duration) {
        mCurrentTimeMillis += duration.toJavaDuration().toMillis()
        SystemClock.setCurrentTimeMillis(mCurrentTimeMillis)
        timeSource += duration
        delay(duration)
    }
}
