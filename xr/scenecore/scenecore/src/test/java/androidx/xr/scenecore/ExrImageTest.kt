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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.ActivitySpace as RtActivitySpace
import androidx.xr.scenecore.runtime.ExrImageResource as RtExrImage
import androidx.xr.scenecore.runtime.PanelEntity as RtPanelEntity
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialCapabilities as RtSpatialCapabilities
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class ExrImageTest {

    private val mFakePerceptionRuntimeFactory = FakePerceptionRuntimeFactory()
    private val mockSceneRuntime = mock<SceneRuntime>()
    private val mockRenderingRuntime = mock<RenderingRuntime>()

    private val mockActivitySpace = mock<RtActivitySpace>()
    private val mockPanelEntityImpl = mock<RtPanelEntity>()
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()

    @Before
    fun setUp() {
        mockSceneRuntime.stub {
            on { activitySpace }.thenReturn(mockActivitySpace)
            on { perceptionSpaceActivityPose }.thenReturn(mock())
            on { spatialCapabilities }.thenReturn(RtSpatialCapabilities(0))
            on { mainPanelEntity }.thenReturn(mockPanelEntityImpl)
        }
    }

    @Test
    fun exrImage_createFromZip_failsForExrFile() {
        runBlocking {
            val mockRtExrImage = mock<RtExrImage>()
            whenever(mockRenderingRuntime.loadExrImageByAssetNameAsync("test.exr"))
                .thenReturn(mockRtExrImage)
            val session =
                Session(
                    activity,
                    runtimes =
                        listOf(
                            mFakePerceptionRuntimeFactory.createRuntime(activity),
                            mockSceneRuntime,
                            mockRenderingRuntime,
                        ),
                )

            @Suppress("UNUSED_VARIABLE", "NewApi")
            val exception =
                assertFailsWith<IllegalArgumentException> {
                    val unusedExrImage: ExrImage =
                        ExrImage.createFromZip(session, Paths.get("test.exr"))
                }

            assertThat(exception)
                .hasMessageThat()
                .contains("Only preprocessed skybox files with the .zip extension are supported.")
            verify(mockRenderingRuntime, never()).loadExrImageByAssetNameAsync("test.exr")
        }
    }

    @Test
    fun exrImage_createFromZip_withZipExtension_passes() {
        runBlocking {
            val mockRtExrImage = mock<RtExrImage>()
            whenever(mockRenderingRuntime.loadExrImageByAssetNameAsync("test.zip"))
                .thenReturn(mockRtExrImage)
            val session =
                Session(
                    activity,
                    runtimes =
                        listOf(
                            mFakePerceptionRuntimeFactory.createRuntime(activity),
                            mockSceneRuntime,
                            mockRenderingRuntime,
                        ),
                )

            @Suppress("UNUSED_VARIABLE", "NewApi")
            val exrImage: ExrImage = ExrImage.createFromZip(session, Paths.get("test.zip"))

            assertIs<ExrImage>(exrImage)
            verify(mockRenderingRuntime).loadExrImageByAssetNameAsync("test.zip")
        }
    }
}
