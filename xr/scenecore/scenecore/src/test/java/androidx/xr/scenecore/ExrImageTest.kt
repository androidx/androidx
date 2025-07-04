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

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.ExrImageResource as RtExrImage
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PanelEntity as RtPanelEntity
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
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
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class ExrImageTest {

    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private val mockPlatformAdapter = mock<JxrPlatformAdapter>()
    private val mockActivitySpace = mock<RtActivitySpace>()
    private val mockPanelEntityImpl = mock<RtPanelEntity>()
    private val activity: Activity =
        Robolectric.buildActivity(Activity::class.java).create().start().get()

    @Before
    fun setUp() {
        mockPlatformAdapter.stub {
            on { activitySpace }.thenReturn(mockActivitySpace)
            on { activitySpaceRootImpl }.thenReturn(mockActivitySpace)
            on { perceptionSpaceActivityPose }.thenReturn(mock())
            on { spatialCapabilities }.thenReturn(RtSpatialCapabilities(0))
            on { mainPanelEntity }.thenReturn(mockPanelEntityImpl)
        }
    }

    @Test
    fun exrImage_createFromZip_failsForExrFile() {
        val mockRtExrImage = mock<RtExrImage>()
        mockPlatformAdapter.stub {
            on { loadExrImageByAssetName("test.exr") }
                .thenReturn(Futures.immediateFuture(mockRtExrImage))
        }
        val session =
            Session(activity, fakeRuntimeFactory.createRuntime(activity), mockPlatformAdapter)

        runBlocking {
            @Suppress("UNUSED_VARIABLE", "NewApi")
            val exception =
                assertFailsWith<IllegalArgumentException> {
                    val unusedExrImage: ExrImage =
                        ExrImage.createFromZip(session, Paths.get("test.exr"))
                }

            assertThat(exception)
                .hasMessageThat()
                .contains("Only preprocessed skybox files with the .zip extension are supported.")
        }
        verify(mockPlatformAdapter, never()).loadExrImageByAssetName("test.exr")
    }

    @Test
    fun exrImage_createFromZip_withZipExtension_passes() {
        val mockRtExrImage = mock<RtExrImage>()
        mockPlatformAdapter.stub {
            on { loadExrImageByAssetName("test.zip") }
                .thenReturn(Futures.immediateFuture(mockRtExrImage))
        }
        val session =
            Session(activity, fakeRuntimeFactory.createRuntime(activity), mockPlatformAdapter)

        runBlocking {
            @Suppress("UNUSED_VARIABLE", "NewApi")
            val exrImage: ExrImage = ExrImage.createFromZip(session, Paths.get("test.zip"))

            assertIs<ExrImage>(exrImage)
        }
        verify(mockPlatformAdapter).loadExrImageByAssetName("test.zip")
    }
}
