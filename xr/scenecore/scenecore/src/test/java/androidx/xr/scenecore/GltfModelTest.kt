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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeGltfModelResource
import androidx.xr.scenecore.testing.FakeRenderingRuntimeFactory
import androidx.xr.scenecore.testing.FakeSceneRuntimeFactory
import com.google.common.truth.Truth.assertThat
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class GltfModelTest {

    private val mFakePerceptionRuntimeFactory = FakePerceptionRuntimeFactory()
    private lateinit var fakeSceneRuntime: SceneRuntime
    private lateinit var fakeRenderingRuntime: RenderingRuntime
    private lateinit var session: Session

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()

    @Before
    fun setup() {
        val runtimes = mutableListOf<JxrRuntime>()
        val fakeRuntimeFactory = FakeSceneRuntimeFactory()
        fakeSceneRuntime = fakeRuntimeFactory.create(activity)
        runtimes.add(fakeSceneRuntime)
        val fakeRenderingRuntimeFactory = FakeRenderingRuntimeFactory()
        fakeRenderingRuntime = fakeRenderingRuntimeFactory.create(runtimes, activity)
        runtimes.add(fakeRenderingRuntime)

        session =
            Session(
                activity,
                runtimes =
                    listOf(
                        mFakePerceptionRuntimeFactory.createRuntime(activity),
                        fakeSceneRuntime,
                        fakeRenderingRuntime,
                    ),
            )
        // Prevent the session's PlaneTrackingMode become DISABLED.
        session.configure(Config(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
    }

    @SdkSuppress(minSdkVersion = 27)
    @Test
    fun createGltfByAssetNameTest() = runTest {
        val gltfModel = GltfModel.create(session, Paths.get("FakeAsset.glb"))

        assertThat((gltfModel.model as FakeGltfModelResource).assetName).isEqualTo("FakeAsset.glb")
    }

    @Test
    fun createGltfByByteArrayTest() = runTest {
        val gltfModel = GltfModel.create(session, byteArrayOf(1, 2, 3), "FakeAsset.zip")

        assertThat((gltfModel.model as FakeGltfModelResource).assetData)
            .isEqualTo(byteArrayOf(1, 2, 3))
        assertThat(gltfModel.model.assetKey).isEqualTo("FakeAsset.zip")
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun gltfModel_createAsync_fails() = runTest {
        val hardcodedPathString = "/data/data/com.example.myapp/myfolder/myfile.txt"
        val absolutePath: Path? = Paths.get(hardcodedPathString)

        val exception =
            assertFailsWith<IllegalArgumentException> { GltfModel.create(session, absolutePath!!) }
        assertThat(exception)
            .hasMessageThat()
            .contains(
                "GltfModel.create() expects a path relative to `assets/`, received absolute path"
            )
    }
}
