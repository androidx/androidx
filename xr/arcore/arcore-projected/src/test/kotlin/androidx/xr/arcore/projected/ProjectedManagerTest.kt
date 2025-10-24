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
package androidx.xr.arcore.projected

import android.app.Activity
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProjectedManagerTest {
    @Mock private lateinit var mockActivity: Activity
    @Mock private lateinit var mockPerceptionService: IProjectedPerceptionService.Stub
    @Captor
    private lateinit var vpsAvailabilityCallbackCaptor: ArgumentCaptor<IVpsAvailabilityCallback>
    private lateinit var perceptionManager: ProjectedPerceptionManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(mockPerceptionService.asBinder()).thenReturn(mockPerceptionService)
        `when`(mockPerceptionService.queryLocalInterface(anyString()))
            .thenReturn(mockPerceptionService)
        perceptionManager = ProjectedPerceptionManager(ProjectedTimeSource())
    }

    @Test
    fun create_initializesPerceptionManager() = runTest {
        val manager =
            ProjectedManager(
                mockActivity,
                perceptionManager,
                ProjectedTimeSource(),
                Dispatchers.IO,
                testPerceptionService = mockPerceptionService,
            )

        manager.create()
        launch { perceptionManager.checkVpsAvailability(1.0, 2.0) }
        runCurrent()

        verify(mockPerceptionService)
            .checkVpsAvailability(eq(1.0), eq(2.0), vpsAvailabilityCallbackCaptor.capture())
        vpsAvailabilityCallbackCaptor.value.onVpsAvailabilityChanged(0)
        advanceUntilIdle()
    }

    @Test
    fun update_updatesPerceptionManager() = runTest {
        val projectedPose =
            ProjectedPose().apply {
                vector =
                    ProjectedVector3().apply {
                        x = 1.0f
                        y = 2.0f
                        z = 3.0f
                    }
                q =
                    ProjectedQuarternion().apply {
                        x = 1.0f
                        y = 2.0f
                        z = 3.0f
                        w = 4.0f
                    }
            }
        val expectedPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        val expectedUpdateResult = ProjectedUpdateResult()
        expectedUpdateResult.deviceTrackingState = ProjectedTrackingState.TRACKING
        expectedUpdateResult.earthTrackingState = ProjectedTrackingState.STOPPED
        expectedUpdateResult.devicePose = projectedPose
        `when`(mockPerceptionService.update()).thenReturn(expectedUpdateResult)
        val manager =
            ProjectedManager(
                mockActivity,
                perceptionManager,
                ProjectedTimeSource(),
                Dispatchers.IO,
                testPerceptionService = mockPerceptionService,
            )
        manager.create()

        manager.update()
        assertThat(perceptionManager.xrResources.deviceTrackingState)
            .isEqualTo(TrackingState.TRACKING)
        assertThat(perceptionManager.xrResources.earthTrackingState)
            .isEqualTo(TrackingState.STOPPED)
        assertThat(perceptionManager.arDevice.devicePose).isEqualTo(expectedPose)
    }
}
