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

package androidx.xr.scenecore.testing

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.GltfModel
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class GltfModelTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session

    @Before
    fun setUp() {
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

    @Config(minSdk = 27)
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

            assertThat(gltfModelTester.assetData).isEqualTo(byteArrayOf(1, 2, 3))
            assertThat(gltfModelTester.assetKey).isEqualTo("FakeAsset.zip")
        }
}
