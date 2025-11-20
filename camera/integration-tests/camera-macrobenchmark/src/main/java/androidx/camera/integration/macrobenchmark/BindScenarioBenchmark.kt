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

package androidx.camera.integration.macrobenchmark

import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.integration.macrobenchmark.CameraBenchmarkUtils.SESSION_CONFIG_BIND_ACTIVITY
import androidx.camera.integration.macrobenchmark.CameraBenchmarkUtils.grantCameraPermission
import androidx.camera.integration.macrobenchmark.CameraBenchmarkUtils.measureStartupDefault
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class BindScenarioBenchmark(private val cameraXConfigName: String, private val lens: Int) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = cameraXConfigName == CameraPipeConfig::class.simpleName)

    @Before
    fun setUp() {
        grantCameraPermission()
    }

    @Test
    fun measureStartupForBindingDefaultSessionConfigOfPVIUseCases() =
        benchmarkRule.measureStartupDefault(
            setupIntent = {
                action = SESSION_CONFIG_BIND_ACTIVITY
                putExtra("camerax_config", cameraXConfigName)
                putExtra("lens", lens)
            }
        )

    @Test
    fun measureStartupForBindingSessionConfigOfPVIUseCasesAndAllFeaturesPreferred() =
        benchmarkRule.measureStartupDefault(
            setupIntent = {
                action = SESSION_CONFIG_BIND_ACTIVITY
                putExtra("camerax_config", cameraXConfigName)
                putExtra("lens", lens)
                putExtra("prefer_all_features", true)
            }
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "config={0} lensFacing={1}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lens = selector.lensFacing
                    add(arrayOf(Camera2Config::class.simpleName, lens))
                    add(arrayOf(CameraPipeConfig::class.simpleName, lens))
                }
            }
    }
}
