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

import androidx.activity.ComponentActivity
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.ActivitySpace as RtActivitySpace
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialCapabilities
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SpatialWindowTest {
    private val fakePerceptionRuntimeFactory = FakePerceptionRuntimeFactory()
    private val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
    private val activity = activityController.create().start().get()
    private val mockSceneRuntime = mock<SceneRuntime>()

    private lateinit var session: Session

    @Before
    fun setUp() {
        // A minimal setup is needed to create a Session instance.
        // The session needs access to the mockSceneRuntime.
        val mockActivitySpace = mock<RtActivitySpace>()
        whenever(mockSceneRuntime.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockSceneRuntime.mainPanelEntity).thenReturn(mock())
        whenever(mockSceneRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockSceneRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.spatialCapabilities).thenReturn(SpatialCapabilities(0))
        session =
            Session(
                activity,
                runtimes =
                    listOf(fakePerceptionRuntimeFactory.createRuntime(activity), mockSceneRuntime),
                lifecycleOwner = activity,
            )
    }

    @Test
    fun setPreferredAspectRatio_callsPlatformAdapter() {
        val ratio = 1.23f

        SpatialWindow.setPreferredAspectRatio(session, activity, ratio)

        verify(mockSceneRuntime).setPreferredAspectRatio(activity, ratio)
    }
}
