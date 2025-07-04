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

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.GltfModelResource as RtGltfModel
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PanelEntity as RtPanelEntity
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class GltfModelTest {

    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private val mockPlatformAdapter = mock<JxrPlatformAdapter>()
    private val mockActivitySpace = mock<RtActivitySpace>()
    private val mockPanelEntityImpl = mock<RtPanelEntity>()
    private val activity: Activity =
        Robolectric.buildActivity(Activity::class.java).create().start().get()

    @Before
    fun setup() {
        mockPlatformAdapter.stub {
            on { activitySpace }.thenReturn(mockActivitySpace)
            on { activitySpaceRootImpl }.thenReturn(mockActivitySpace)
            on { perceptionSpaceActivityPose }.thenReturn(mock())
            on { spatialCapabilities }.thenReturn(RtSpatialCapabilities(0))
            on { mainPanelEntity }.thenReturn(mockPanelEntityImpl)
        }
    }

    @SdkSuppress(minSdkVersion = 27)
    @Test
    fun createGltfByAssetNameTest() = runTest {
        val mockRtGltfModel = mock<RtGltfModel>()
        mockPlatformAdapter.stub {
            on { loadGltfByAssetName("FakeAsset.glb") }
                .thenReturn(Futures.immediateFuture(mockRtGltfModel))
        }
        val session =
            Session(activity, fakeRuntimeFactory.createRuntime(activity), mockPlatformAdapter)

        val gltfModel: GltfModel = GltfModel.create(session, Paths.get("FakeAsset.glb"))

        verify(mockPlatformAdapter).loadGltfByAssetName("FakeAsset.glb")
    }

    @Test
    fun createGltfByByteArrayTest() = runTest {
        val mockRtGltfModel = mock<RtGltfModel>()
        mockPlatformAdapter.stub {
            on { loadGltfByByteArray(byteArrayOf(1, 2, 3), "FakeAsset.zip") }
                .thenReturn(Futures.immediateFuture(mockRtGltfModel))
        }
        val session =
            Session(activity, fakeRuntimeFactory.createRuntime(activity), mockPlatformAdapter)

        val gltfModel: GltfModel = GltfModel.create(session, byteArrayOf(1, 2, 3), "FakeAsset.zip")

        verify(mockPlatformAdapter).loadGltfByByteArray(byteArrayOf(1, 2, 3), "FakeAsset.zip")
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun gltfModel_createAsync_fails() = runTest {
        val hardcodedPathString = "/data/data/com.example.myapp/myfolder/myfile.txt"
        val absolutePath: Path? = Paths.get(hardcodedPathString)
        val session =
            Session(activity, fakeRuntimeFactory.createRuntime(activity), mockPlatformAdapter)

        val exception =
            assertFailsWith<IllegalArgumentException> { GltfModel.create(session, absolutePath!!) }
        assertThat(exception)
            .hasMessageThat()
            .contains(
                "GltfModel.create() expects a path relative to `assets/`, received absolute path"
            )
    }
}
