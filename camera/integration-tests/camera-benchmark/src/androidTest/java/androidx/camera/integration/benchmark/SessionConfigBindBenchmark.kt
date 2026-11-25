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

package androidx.camera.integration.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class SessionConfigBindBenchmark(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraXConfig: CameraXConfig,
) {
    @get:Rule val benchmarkRule = BenchmarkRule()

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        ProcessCameraProvider.configureInstance(cameraXConfig)

        withContext(Dispatchers.Main) {
            // Keep the lifecycle in the CREATED state so binding doesn't attach the use cases to
            // the camera. Otherwise every iteration triggers asynchronous camera (and encoder)
            // reconfiguration in the background, which makes iteration times noisy and prevents
            // warmup from converging within the warmup time limit.
            fakeLifecycleOwner = FakeLifecycleOwner()
        }
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun bindSessionConfig_defaultParamsWithPVIUseCases() {
        cameraProvider =
            ProcessCameraProvider.getInstance(ApplicationProvider.getApplicationContext())[
                    10, TimeUnit.SECONDS]

        val sessionConfig =
            SessionConfig(
                useCases =
                    listOf(
                        Preview.Builder().build(),
                        ImageCapture.Builder().build(),
                        VideoCapture.Builder(Recorder.Builder().build()).build(),
                    )
            )

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        benchmarkRule.measureRepeated {
            instrumentation.runOnMainSync {
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, sessionConfig)
            }
            // Unbind so that each iteration binds from the same unbound state, and drain the main
            // thread so that resources released by unbind (e.g. ImageReaders) don't pile up and
            // trigger GCs during measurement.
            runWithMeasurementDisabled {
                instrumentation.runOnMainSync { cameraProvider.unbindAll() }
                instrumentation.waitForIdleSync()
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lens = selector.lensFacing
                    add(
                        arrayOf(
                            "config=${Camera2Config::class.simpleName} lensFacing={$lens}",
                            selector,
                            Camera2Config::class.simpleName,
                            Camera2Config.defaultConfig(),
                        )
                    )
                }
            }
    }
}
