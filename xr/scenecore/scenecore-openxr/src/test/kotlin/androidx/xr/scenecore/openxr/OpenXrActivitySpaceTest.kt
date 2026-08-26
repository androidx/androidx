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

package androidx.xr.scenecore.openxr

import android.app.Activity
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.openxr.testing.FakeSceneCoreOpenXrNative
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class OpenXrActivitySpaceTest {

    private lateinit var activity: Activity
    private lateinit var fakeNative: FakeSceneCoreOpenXrNative
    private lateinit var nodeRegistry: OpenXrSceneNodeRegistry
    private lateinit var executor: ScheduledExecutorService
    private lateinit var activitySpace: OpenXrActivitySpace

    @Before
    fun setUp() {
        activity = mock()
        fakeNative = FakeSceneCoreOpenXrNative()
        fakeNative.init(100L, 200L, 300L)
        fakeNative.createSpatialContainer()
        nodeRegistry = OpenXrSceneNodeRegistry()
        executor = Executors.newSingleThreadScheduledExecutor()

        activitySpace =
            OpenXrActivitySpace(
                activity,
                fakeNative.fakeRootEntityHandle,
                fakeNative,
                nodeRegistry,
                executor,
            )
        nodeRegistry.addSystemSpaceScenePose(activitySpace)
        nodeRegistry.addSystemSpaceScenePose(PerceptionSpaceScenePoseImpl(activitySpace))
    }

    @Test
    fun activitySpacePose_returnsIdentity() {
        assertThat(activitySpace.activitySpacePose).isEqualTo(Pose.Identity)
    }

    @Test
    fun activitySpaceScale_returnsOne() {
        assertThat(activitySpace.activitySpaceScale).isEqualTo(Vector3.One)
    }

    @Test
    fun getPose_activitySpace_returnsIdentity() {
        assertThat(activitySpace.getPose(Space.ACTIVITY)).isEqualTo(Pose.Identity)
    }

    @Test
    fun getPose_parentSpace_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.getPose(Space.PARENT)
        }
    }

    @Test
    fun getPose_realWorldSpace_returnsPerceptionPose() {
        assertThat(activitySpace.getPose(Space.REAL_WORLD)).isEqualTo(Pose.Identity)
    }

    @Test
    fun getPose_invalidSpace_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) { activitySpace.getPose(999) }
    }

    @Test
    fun setPose_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setPose(Pose.Identity, Space.PARENT)
        }
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setPose(Pose.Identity, Space.ACTIVITY)
        }
    }

    @Test
    fun getScale_activitySpace_returnsOne() {
        assertThat(activitySpace.getScale(Space.ACTIVITY)).isEqualTo(Vector3.One)
    }

    @Test
    fun getScale_parentSpace_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.getScale(Space.PARENT)
        }
    }

    @Test
    fun getScale_realWorldSpace_returnsWorldScale() {
        assertThat(activitySpace.getScale(Space.REAL_WORLD)).isEqualTo(Vector3.One)
    }

    @Test
    fun getScale_invalidSpace_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) { activitySpace.getScale(999) }
    }

    @Test
    fun setScale_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setScale(Vector3.One, Space.PARENT)
        }
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setScale(Vector3.One, Space.ACTIVITY)
        }
    }

    @Test
    fun setParent_throwsUnsupportedOperationException() {
        val otherEntity =
            OpenXrActivitySpace(
                activity,
                fakeNative.createSceneEntity(),
                fakeNative,
                nodeRegistry,
                executor,
            )
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.parent = otherEntity
        }
    }

    @Test
    fun parent_returnsNull() {
        assertThat(activitySpace.parent).isNull()
    }

    @Test
    fun bounds_uninitialized_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) { activitySpace.bounds }
    }

    @Test
    fun onBoundsChanged_updatesBoundsAndNotifiesListeners() {
        var notifiedBounds: Dimensions? = null
        val listener =
            ActivitySpace.OnBoundsChangedListener { dimensions -> notifiedBounds = dimensions }
        activitySpace.addOnBoundsChangedListener(listener)

        val newBounds = Dimensions(2f, 3f, 4f)
        activitySpace.onBoundsChanged(newBounds)

        assertThat(activitySpace.bounds).isEqualTo(newBounds)
        assertThat(notifiedBounds).isEqualTo(newBounds)

        activitySpace.removeOnBoundsChangedListener(listener)
        activitySpace.onBoundsChanged(Dimensions(5f, 6f, 7f))
        assertThat(notifiedBounds).isEqualTo(newBounds) // Should not receive second update
    }

    @Test
    fun setOnOriginChangedListener_notifiesListenerOnOriginChange() {
        var originChangeCount = 0
        val directExecutor = java.util.concurrent.Executor { it.run() }
        activitySpace.setOnOriginChangedListener({ originChangeCount++ }, directExecutor)

        activitySpace.onOriginChanged()

        assertThat(originChangeCount).isEqualTo(1)
    }
}
