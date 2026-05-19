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

package androidx.xr.scenecore.testrule

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class TestRuleGltfModelTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session

    @Before
    fun setup() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(
                context = activity,
                coroutineContext = testDispatcher,
                lifecycleOwner = activity as LifecycleOwner,
            )

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @SdkSuppress(minSdkVersion = 27)
    @Test
    fun createGltfByAssetNameTest() =
        runTest(testDispatcher) {
            val gltfModel = GltfModel.create(session, Paths.get("FakeAsset.glb"))
            val gltfModelTester = testRule.createTester(gltfModel)

            assertThat(gltfModelTester.assetPath).isEqualTo("FakeAsset.glb")
        }

    @Test
    fun createGltfByByteArrayTest() =
        runTest(testDispatcher) {
            val gltfModel = GltfModel.create(session, byteArrayOf(1, 2, 3), "FakeAsset.zip")
            val gltfModelTester = testRule.createTester(gltfModel)

            assertThat(gltfModelTester).isNotNull()
            assertThat(gltfModelTester.assetData).isEqualTo(byteArrayOf(1, 2, 3))
            assertThat(gltfModelTester.assetKey).isEqualTo("FakeAsset.zip")
        }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun createGltfByInvalidPath_fails() =
        runTest(testDispatcher) {
            val hardcodedPathString = "/data/data/com.example.myapp/myfolder/myfile.txt"
            val absolutePath: Path? = Paths.get(hardcodedPathString)

            val exception =
                assertFailsWith<IllegalArgumentException> {
                    GltfModel.create(session, absolutePath!!)
                }
            assertThat(exception)
                .hasMessageThat()
                .contains(
                    "GltfModel.create() expects a path relative to `assets/`, received absolute path"
                )
        }
}
