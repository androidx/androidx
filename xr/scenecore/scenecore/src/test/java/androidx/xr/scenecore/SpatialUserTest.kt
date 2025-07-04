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

package androidx.xr.scenecore

import androidx.activity.ComponentActivity
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyInt
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpatialUserTest {

    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private val mockRuntime = mock<JxrPlatformAdapter>()
    private val mockActivitySpace = mock<RtActivitySpace>()
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session
    lateinit var spatialUser: SpatialUser

    @Before
    fun setUp() {
        whenever(mockRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockRuntime.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockRuntime.activitySpaceRootImpl).thenReturn(mockActivitySpace)
        whenever(mockRuntime.headActivityPose).thenReturn(mock())
        whenever(mockRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockRuntime.mainPanelEntity).thenReturn(mock())
        whenever(mockRuntime.headActivityPose).thenReturn(mock())
        whenever(mockRuntime.getCameraViewActivityPose(anyInt())).thenReturn(mock())
        whenever(mockRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockRuntime.spatialCapabilities).thenReturn(RtSpatialCapabilities(0))
        session = Session(activity, fakeRuntimeFactory.createRuntime(activity), mockRuntime)
        session.configure(Config(headTracking = HeadTrackingMode.LAST_KNOWN))
        spatialUser = SpatialUser.create(session.runtime.lifecycleManager, mockRuntime)
    }

    @Test
    fun getHeadActivityPose_returnsNullIfNoRtActivityPose() {
        whenever(mockRuntime.headActivityPose).thenReturn(null)
        val head = spatialUser.head
        assertThat(head).isNull()
    }

    @Test
    fun getHeadActivityPose_returnsNullThenHeadWhenAvailable() {
        whenever(mockRuntime.headActivityPose).thenReturn(null)
        var head = spatialUser.head
        assertThat(head).isNull()

        whenever(mockRuntime.headActivityPose).thenReturn(mock())
        head = spatialUser.head
        assertThat(head).isNotNull()
    }

    @Test
    fun getHeadActivityPose_returnsHeadActivityPose() {
        val head = spatialUser.head
        assertThat(head).isNotNull()
    }

    @Test
    fun getHeadActivityPoseTwice_returnsSameHeadActivityPose() {
        val head1 = spatialUser.head
        val head2 = spatialUser.head

        assertThat(head1).isEqualTo(head2)
    }

    @Test
    fun getNullCameraViews_returnsNullCameraViews() {
        whenever(mockRuntime.getCameraViewActivityPose(anyInt())).thenReturn(null)
        val leftView = spatialUser.cameraViews[CameraView.CameraType.LEFT_EYE]
        val rightView = spatialUser.cameraViews[CameraView.CameraType.RIGHT_EYE]

        assertThat(leftView).isNull()
        assertThat(rightView).isNull()
    }

    @Test
    fun getCameraViews_returnsNullThenCameraViewsWhenAvailable() {
        whenever(mockRuntime.getCameraViewActivityPose(anyInt())).thenReturn(null)
        var leftView = spatialUser.cameraViews[CameraView.CameraType.LEFT_EYE]
        var rightView = spatialUser.cameraViews[CameraView.CameraType.RIGHT_EYE]

        assertThat(spatialUser.cameraViews).isEmpty()
        assertThat(leftView).isNull()
        assertThat(rightView).isNull()

        whenever(mockRuntime.getCameraViewActivityPose(anyInt())).thenReturn(mock())
        leftView = spatialUser.cameraViews[CameraView.CameraType.LEFT_EYE]
        rightView = spatialUser.cameraViews[CameraView.CameraType.RIGHT_EYE]

        assertThat(spatialUser.cameraViews).isNotEmpty()
        assertThat(leftView).isNotNull()
        assertThat(rightView).isNotNull()
    }

    @Test
    fun getCameraViews_returnsCameraView() {
        val leftView = spatialUser.cameraViews[CameraView.CameraType.LEFT_EYE]
        val rightView = spatialUser.cameraViews[CameraView.CameraType.RIGHT_EYE]

        assertThat(spatialUser.cameraViews).isNotEmpty()
        assertThat(leftView).isNotNull()
        assertThat(rightView).isNotNull()
    }

    @Test
    fun getCameraViewsTwice_returnsSameCameraView() {
        val leftView1 = spatialUser.cameraViews[CameraView.CameraType.LEFT_EYE]
        val leftView2 = spatialUser.cameraViews[CameraView.CameraType.LEFT_EYE]
        val rightView1 = spatialUser.cameraViews[CameraView.CameraType.RIGHT_EYE]
        val rightView2 = spatialUser.cameraViews[CameraView.CameraType.RIGHT_EYE]

        assertThat(leftView1).isEqualTo(leftView2)
        assertThat(rightView1).isEqualTo(rightView2)
    }

    @Test
    fun getCameraViews_returnsEmptyMapIfNullCamera() {
        val mockRuntimeNoCamera = mock<JxrPlatformAdapter>()
        whenever(mockRuntimeNoCamera.headActivityPose).thenReturn(mock())
        whenever(mockRuntimeNoCamera.getCameraViewActivityPose(anyInt())).thenReturn(null)
        val spatialUserNoCamera =
            SpatialUser.create(session.runtime.lifecycleManager, mockRuntimeNoCamera)

        val cameraViews = spatialUserNoCamera.cameraViews

        assertThat(cameraViews).isEmpty()
    }
}
