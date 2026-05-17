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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testrule

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.HitTestResult
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.sceneRuntime
import androidx.xr.scenecore.testing.ActivitySpaceTester
import androidx.xr.scenecore.testing.MemoryUtils
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.lang.ref.WeakReference
import java.util.function.Consumer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class TestRuleActivitySpaceTest {

    @get:Rule val scenecoreTestRule = SceneCoreTestRule()
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session
    private lateinit var activitySpace: ActivitySpace
    private lateinit var activitySpaceTester: ActivitySpaceTester

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        activitySpace = session.scene.activitySpace
        activitySpaceTester = scenecoreTestRule.activitySpaceTester
    }

    @Test
    fun getBounds_callsImplGetBounds() {
        assertThat(activitySpace.bounds).isNotNull()

        val bounds = activitySpace.bounds

        assertThat(bounds.width).isEqualTo(Float.POSITIVE_INFINITY)
        assertThat(bounds.height).isEqualTo(Float.POSITIVE_INFINITY)
        assertThat(bounds.depth).isEqualTo(Float.POSITIVE_INFINITY)
    }

    @Test
    fun addBoundsChangedListener_receivesBoundsChangedCallback() {
        var receivedBounds: FloatSize3d? = null
        val boundsChangedListener =
            Consumer<FloatSize3d> { newBounds -> receivedBounds = newBounds }

        activitySpace.addBoundsChangedListener(directExecutor(), boundsChangedListener)

        val newBounds = FloatSize3d(0.3f, 0.2f, 0.1f)
        // Simulates a runtime callback via tester.
        activitySpaceTester.triggerOnBoundsChanged(newBounds)

        assertThat(receivedBounds?.width).isEqualTo(0.3f)
        assertThat(receivedBounds?.height).isEqualTo(0.2f)
        assertThat(receivedBounds?.depth).isEqualTo(0.1f)

        receivedBounds = null
        activitySpace.removeBoundsChangedListener(boundsChangedListener)

        // Simulates another callback, listener should not be called.
        activitySpaceTester.triggerOnBoundsChanged(FloatSize3d(1f, 1f, 1f))
        assertThat(receivedBounds).isNull()
    }

    @Test
    fun addOriginChangedListener_receivesRuntimeSetOnOriginChangedListenerCallbacks() {
        var listenerCalled = false
        activitySpace.addOriginChangedListener(directExecutor()) { listenerCalled = true }

        // Simulates a runtime callback via tester by setting root transform.
        activitySpaceTester.triggerOnOriginChanged()

        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun removeOriginChangedListener_callsRuntimeSetOnOriginChangedListener() {
        var listenCount = 0
        val listener = Runnable { listenCount++ }
        activitySpace.addOriginChangedListener(listener)

        // Simulates a runtime callback.
        activitySpaceTester.triggerOnOriginChanged()

        assertThat(listenCount).isEqualTo(1)

        activitySpace.removeOriginChangedListener(listener)
        // Simulates another callback.
        activitySpaceTester.triggerOnOriginChanged()

        assertThat(listenCount).isEqualTo(1)
    }

    @Test
    fun setRecommendedContentBoxInFullSpace_returnsCorrectBoundingBox() {
        val expectedResult: BoundingBox =
            BoundingBox.fromMinMax(min = Vector3(-1f, -1f, -1f), max = Vector3(1f, 1f, 1f))
        activitySpaceTester.recommendedContentBoxInFullSpace = expectedResult

        val recommendedContentBoxInFullSpace = activitySpace.recommendedContentBoxInFullSpace

        assertThat(recommendedContentBoxInFullSpace.min).isEqualTo(expectedResult.min)
        assertThat(recommendedContentBoxInFullSpace.max).isEqualTo(expectedResult.max)
    }

    @Test
    fun getParentSpacePose_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) { activitySpace.getPose(Space.PARENT) }
    }

    @Test
    fun getActivitySpacePose_returnsIdentity() {
        val pose = activitySpace.getPose(Space.ACTIVITY)
        assertThat(pose.translation).isEqualTo(Vector3.Zero)
        assertThat(pose.rotation).isEqualTo(Quaternion.Identity)
    }

    @Test
    fun getRealWorldSpacePose_returnsPerceptionSpacePose() {
        @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
        val pose = activitySpace.getPose(Space.REAL_WORLD)
        assertThat(pose.translation).isEqualTo(Vector3.Zero)
        assertThat(pose.rotation).isEqualTo(Quaternion.Identity)
    }

    @Test
    fun setPose_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setPose(Pose(Vector3.Zero, Quaternion.Identity))
        }
    }

    @Test
    fun getActivitySpaceScale_returnsIdentity() {
        val scale = activitySpace.getScale(Space.ACTIVITY)
        assertThat(scale).isEqualTo(1f)
    }

    @Test
    fun getParentSpaceNonUniformScale_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            activitySpace.getNonUniformScale(Space.PARENT)
        }
    }

    @Test
    fun getActivityNonUniformSpaceScale_returnsIdentity() {
        val scale = activitySpace.getNonUniformScale(Space.ACTIVITY)
        assertThat(scale).isEqualTo(Vector3.One)
    }

    @Test
    fun getParentSpaceScale_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) { activitySpace.getScale(Space.PARENT) }
    }

    @Test
    fun getRealWorldSpaceScale_returnsIdentity() {
        @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
        val scale = activitySpace.getScale(Space.REAL_WORLD)
        assertThat(scale).isEqualTo(1f)
    }

    @Test
    fun getRealWorldSpaceNonUniformScale_returnsIdentity() {
        @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
        val scale = activitySpace.getNonUniformScale(Space.REAL_WORLD)
        assertThat(scale).isEqualTo(Vector3.One)
    }

    @Test
    fun setScale_float_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setScale(1f, Space.PARENT)
        }
    }

    @Test
    fun setScale_vector_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setScale(Vector3.One, Space.PARENT)
        }
    }

    @Test
    fun disposeInternal_removesBoundsChangedListeners() {
        var callbackCalled = false
        val listener = Consumer<FloatSize3d> { callbackCalled = true }

        activitySpace.addBoundsChangedListener(listener)

        activitySpace.disposeInternal()

        activitySpaceTester.triggerOnBoundsChanged(FloatSize3d(1f, 1f, 1f))
        ShadowLooper.idleMainLooper()

        assertThat(callbackCalled).isFalse()
    }

    @Test
    fun disposeInternal_removesOriginChangedListeners() {
        var listenCount = 0
        val listener = Runnable { listenCount++ }
        activitySpace.addOriginChangedListener(listener)

        // Simulates a runtime callback.
        activitySpaceTester.triggerOnOriginChanged()
        assertThat(listenCount).isEqualTo(1)

        activitySpace.disposeInternal()

        // Simulates another runtime callback, listener should NOT be called.
        activitySpaceTester.triggerOnOriginChanged()
        assertThat(listenCount).isEqualTo(1)
    }

    @Test
    fun disposeInternal_callingTwiceDoesNotCrash() {
        activitySpace.disposeInternal()
        activitySpace.disposeInternal()
    }

    @Test
    fun garbageCollection_disposesEntity() {
        // This test verifies that ActivitySpace can be GCed.
        // We use the rule's session to create a temporary activity space if needed,
        // or just verify the session's one if it's eligible.
        // For this refactor, we'll keep the logic of creating a local one.
        fun createActivitySpace(): WeakReference<ActivitySpace> {
            val localActivitySpace =
                ActivitySpace.create(session.sceneRuntime, session.scene.entityRegistry)
            return WeakReference(localActivitySpace)
        }

        val activitySpaceRef = createActivitySpace()
        assertThat(activitySpaceRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(activitySpaceRef)
    }

    @Test
    fun setupHitTestResult_withNonNull_returnsConfiguredResult() = runTest {
        val expectedResult =
            HitTestResult(
                hitPosition = Vector3(1f, 2f, 3f),
                surfaceNormal = Vector3(0f, 1f, 0f),
                surfaceType = HitTestResult.SurfaceType.PLANE,
                distance = 5f,
            )

        activitySpaceTester.hitTestResult = expectedResult

        val actualResult =
            requireNotNull(
                activitySpace.hitTest(origin = Vector3.Zero, direction = Vector3.Forward)
            )

        assertThat(actualResult.hitPosition).isEqualTo(expectedResult.hitPosition)
        assertThat(actualResult.surfaceNormal).isEqualTo(expectedResult.surfaceNormal)
        assertThat(actualResult.distance).isEqualTo(expectedResult.distance)
        assertThat(actualResult.surfaceType).isEqualTo(expectedResult.surfaceType)
    }

    @Test
    fun setupHitTestResult_withNull_returnsDefaultNoHitResult() = runTest {
        activitySpaceTester.hitTestResult = null

        val actualResult = activitySpace.hitTest(origin = Vector3.Zero, direction = Vector3.Forward)

        assertThat(actualResult).isNull()
    }
}
