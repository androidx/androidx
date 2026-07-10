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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeActivitySpace
import androidx.xr.scenecore.testing.FakeSceneRuntimeFactory
import androidx.xr.scenecore.testing.MemoryUtils
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.lang.ref.WeakReference
import java.util.function.Consumer
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class ActivitySpaceTest {
    private val entityRegistry = EntityRegistry()
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private lateinit var fakeRuntime: SceneRuntime

    @Before
    fun setUp() {
        val fakeRuntimeFactory = FakeSceneRuntimeFactory()
        fakeRuntime = fakeRuntimeFactory.create(activity)
    }

    @Test
    fun getBounds_callsImplGetBounds() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)

        assertThat(activitySpace.bounds).isNotNull()

        val bounds = activitySpace.bounds

        assertThat(bounds.width).isEqualTo(Float.POSITIVE_INFINITY)
        assertThat(bounds.height).isEqualTo(Float.POSITIVE_INFINITY)
        assertThat(bounds.depth).isEqualTo(Float.POSITIVE_INFINITY)
    }

    @Test
    fun addBoundsChangedListener_receivesBoundsChangedCallback() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        val rtActivitySpace = activitySpace.rtEntity as FakeActivitySpace
        val boundsChangedListener =
            Consumer<FloatSize3d> { newBounds ->
                assertThat(newBounds.width).isEqualTo(0.3f)
                assertThat(newBounds.height).isEqualTo(0.2f)
                assertThat(newBounds.depth).isEqualTo(0.1f)
            }

        activitySpace.addBoundsChangedListener(directExecutor(), boundsChangedListener)

        // Already one listener by default.
        assertThat((activitySpace.rtEntity as FakeActivitySpace).onBoundsChangedListeners)
            .hasSize(2)

        // Simulates a runtime callback.
        rtActivitySpace.onBoundsChanged(Dimensions(0.3f, 0.2f, 0.1f))

        activitySpace.removeBoundsChangedListener(boundsChangedListener)

        assertThat((activitySpace.rtEntity as FakeActivitySpace).onBoundsChangedListeners)
            .hasSize(1)
    }

    @Test
    fun addOriginChangedListener_receivesRuntimeSetOnOriginChangedListenerCallbacks() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        val rtActivitySpace = activitySpace.rtEntity as FakeActivitySpace

        var listenerCalled = false
        activitySpace.addOriginChangedListener(directExecutor()) { listenerCalled = true }
        // Simulates a runtime callback.
        rtActivitySpace.onOriginChanged()

        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun removeOriginChangedListener_callsRuntimeSetOnOriginChangedListener() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        val rtActivitySpace = activitySpace.rtEntity as FakeActivitySpace

        var listenCount = 0
        val listener = Runnable { listenCount++ }
        activitySpace.addOriginChangedListener(listener)
        // Simulates a runtime callback.
        rtActivitySpace.onOriginChanged()

        assertThat(listenCount).isEqualTo(1)

        activitySpace.removeOriginChangedListener(listener)
        // Simulates a runtime callback.
        rtActivitySpace.onOriginChanged()

        assertThat(listenCount).isEqualTo(1)
    }

    @Test
    fun recommendedContentBoxInFullSpace_returnsCorrectBoundingBox() {
        val expectedResult: BoundingBox =
            BoundingBox.fromMinMax(
                min = Vector3(-1.73f / 2, -1.61f / 2, -0.5f / 2),
                max = Vector3(1.73f / 2, 1.61f / 2, 0.5f / 2),
            )
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        val recommendedContentBoxInFullSpace = activitySpace.recommendedContentBoxInFullSpace

        assertThat(recommendedContentBoxInFullSpace.min).isEqualTo(expectedResult.min)
        assertThat(recommendedContentBoxInFullSpace.max).isEqualTo(expectedResult.max)
    }

    @Test
    fun getParentSpacePose_throwsIllegalArgumentException() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        assertThrows(IllegalArgumentException::class.java) { activitySpace.getPose(Space.PARENT) }
    }

    @Test
    fun getActivitySpacePose_returnsIdentity() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        val pose = activitySpace.getPose(Space.ACTIVITY)
        assertThat(pose.translation).isEqualTo(Vector3.Zero)
        assertThat(pose.rotation).isEqualTo(Quaternion.Identity)
    }

    @Test
    fun getRealWorldSpacePose_returnsPerceptionSpacePose() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
        val pose = activitySpace.getPose(Space.REAL_WORLD)
        assertThat(pose.translation).isEqualTo(Vector3.Zero)
        assertThat(pose.rotation).isEqualTo(Quaternion.Identity)
    }

    @Test
    fun setPose_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
            activitySpace.setPose(Pose(Vector3.Zero, Quaternion.Identity))
        }
    }

    @Test
    fun getActivitySpaceScale_returnsIdentity() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        val scale = activitySpace.getScale(Space.ACTIVITY)
        assertThat(scale).isEqualTo(1f)
    }

    fun getParentSpaceNonUniformScale_throwsIllegalArgumentException() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        assertThrows(IllegalArgumentException::class.java) {
            activitySpace.getNonUniformScale(Space.PARENT)
        }
    }

    @Test
    fun getActivityNonUniformSpaceScale_returnsIdentity() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        val scale = activitySpace.getNonUniformScale(Space.ACTIVITY)
        assertThat(scale).isEqualTo(Vector3.One)
    }

    @Test
    fun getParentSpaceScale_throwsIllegalArgumentException() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        assertThrows(IllegalArgumentException::class.java) { activitySpace.getScale(Space.PARENT) }
    }

    @Test
    fun getRealWorldSpaceScale_returnsIdentity() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
        val scale = activitySpace.getScale(Space.REAL_WORLD)
        assertThat(scale).isEqualTo(1f)
    }

    @Test
    fun getRealWorldSpaceNonUniformScale_returnsIdentity() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
        val scale = activitySpace.getNonUniformScale(Space.REAL_WORLD)
        assertThat(scale).isEqualTo(Vector3.One)
    }

    @Test
    fun setScale_float_throwsUnsupportedOperationException() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setScale(1f, Space.PARENT)
        }
    }

    @Test
    fun setScale_vector_throwsUnsupportedOperationException() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setScale(Vector3.One, Space.PARENT)
        }
    }

    @Test
    fun disposeInternal_removesBoundsChangedListeners() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        val rtActivitySpace = activitySpace.rtEntity as FakeActivitySpace
        val listener = Consumer<FloatSize3d> {}

        activitySpace.addBoundsChangedListener(listener)

        // Already one listener by default.
        assertThat(rtActivitySpace.onBoundsChangedListeners).hasSize(2)

        activitySpace.disposeInternal()

        assertThat(rtActivitySpace.onBoundsChangedListeners).hasSize(1)
    }

    @Test
    fun disposeInternal_removesOriginChangedListeners() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        val rtActivitySpace = activitySpace.rtEntity as FakeActivitySpace
        var listenCount = 0
        val listener = Runnable { listenCount++ }
        activitySpace.addOriginChangedListener(listener)
        // Simulates a runtime callback.
        rtActivitySpace.onOriginChanged()

        assertThat(listenCount).isEqualTo(1) // 0 -> 1

        activitySpace.disposeInternal()
        // Simulates a runtime callback.
        rtActivitySpace.onOriginChanged()

        assertThat(listenCount).isEqualTo(1)
    }

    @Test
    fun disposeInternal_callingTwiceDoesNotCrash() {
        val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
        activitySpace.disposeInternal()
        activitySpace.disposeInternal()
    }

    @Test
    fun garbageCollection_disposesEntity() {
        fun createActivitySpace(): WeakReference<ActivitySpace> {
            val activitySpace = ActivitySpace.create(fakeRuntime, entityRegistry)
            return WeakReference(activitySpace)
        }

        val activitySpaceRef = createActivitySpace()
        assertThat(activitySpaceRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(activitySpaceRef)
    }
}
