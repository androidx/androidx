/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.view

import android.content.Context
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.ParameterizedTestConfigUtil
import androidx.camera.testing.impl.fakes.FakeActivity
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.testrule.CameraTestActivityScenarioRule
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PreviewViewStreamStateTest(
    private val implMode: PreviewView.ImplementationMode,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    private lateinit var previewView: PreviewView
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var isSetup = false
    private lateinit var lifecycle: FakeLifecycleOwner
    private lateinit var cameraProvider: ProcessCameraProvider

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule
    val activityRule: CameraTestActivityScenarioRule<FakeActivity> =
        CameraTestActivityScenarioRule(FakeActivity::class.java)

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    private lateinit var defaultCameraSelector: CameraSelector

    @Before
    fun setUp() {
        defaultCameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        CoreAppTestUtil.assumeCompatibleDevice()

        val context = ApplicationProvider.getApplicationContext<Context>()

        instrumentation.runOnMainSync {
            lifecycle = FakeLifecycleOwner()
            activityRule.scenario.onActivity { activity ->
                previewView = PreviewView(context)
                previewView.implementationMode = implMode
                activity.setContentView(previewView)
            }
        }
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        isSetup = true
    }

    @After
    fun tearDown() {
        if (isSetup) {
            instrumentation.runOnMainSync {
                cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
            }
            isSetup = false
        }
    }

    private fun startPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        cameraSelector: CameraSelector,
    ): Preview {
        val preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(previewView.surfaceProvider)
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        }

        return preview
    }

    @Test
    fun streamState_IDLE_TO_STREAMING_startPreview() {
        assertStreamState(PreviewView.StreamState.IDLE)

        startPreview(lifecycle, previewView, defaultCameraSelector)
        instrumentation.runOnMainSync { lifecycle.startAndResume() }

        assertStreamState(PreviewView.StreamState.STREAMING)
    }

    @Test
    fun streamState_STREAMING_TO_IDLE_TO_STREAMING_lifecycleStopAndStart() {
        startPreview(lifecycle, previewView, defaultCameraSelector)
        instrumentation.runOnMainSync { lifecycle.startAndResume() }
        assertStreamState(PreviewView.StreamState.STREAMING)

        instrumentation.runOnMainSync { lifecycle.pauseAndStop() }
        assertStreamState(PreviewView.StreamState.IDLE)

        instrumentation.runOnMainSync { lifecycle.startAndResume() }
        assertStreamState(PreviewView.StreamState.STREAMING)
    }

    @Test
    fun streamState_STREAMING_TO_IDLE_unbindAll() {
        startPreview(lifecycle, previewView, defaultCameraSelector)
        instrumentation.runOnMainSync { lifecycle.startAndResume() }
        assertStreamState(PreviewView.StreamState.STREAMING)

        instrumentation.runOnMainSync { cameraProvider.unbindAll() }
        assertStreamState(PreviewView.StreamState.IDLE)
    }

    @Test
    fun streamState_STREAMING_TO_IDLE_unbindPreviewOnly() {
        val preview = startPreview(lifecycle, previewView, defaultCameraSelector)

        instrumentation.runOnMainSync { lifecycle.startAndResume() }
        assertStreamState(PreviewView.StreamState.STREAMING)

        instrumentation.runOnMainSync { cameraProvider.unbind(preview) }
        assertStreamState(PreviewView.StreamState.IDLE)
    }

    @Test
    @FlakyTest(bugId = 238664500)
    fun streamState_STREAMING_TO_IDLE_TO_STREAMING_switchCamera() {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        Assume.assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)
        val cameraSelector0 = cameraSelectors[0]
        val cameraSelector1 = cameraSelectors[1]

        startPreview(lifecycle, previewView, cameraSelector0)
        instrumentation.runOnMainSync { lifecycle.startAndResume() }
        assertStreamState(PreviewView.StreamState.STREAMING)

        instrumentation.runOnMainSync { cameraProvider.unbindAll() }
        startPreview(lifecycle, previewView, cameraSelector1)

        assertStreamState(PreviewView.StreamState.IDLE)
        assertStreamState(PreviewView.StreamState.STREAMING)
    }

    private fun assertStreamState(expectStreamState: PreviewView.StreamState) {
        val latchForState = CountDownLatch(1)

        val observer =
            Observer<PreviewView.StreamState> { streamState ->
                if (streamState == expectStreamState) {
                    latchForState.countDown()
                }
            }
        instrumentation.runOnMainSync { previewView.previewStreamState.observeForever(observer) }

        try {
            assertThat(latchForState.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        } finally {
            instrumentation.runOnMainSync {
                previewView.previewStreamState.removeObserver(observer)
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0},{1}")
        fun data() =
            ParameterizedTestConfigUtil.generateCameraXConfigParameterizedTestConfigs(
                listOf(
                    arrayOf(PreviewView.ImplementationMode.COMPATIBLE),
                    arrayOf(PreviewView.ImplementationMode.PERFORMANCE),
                ),
                inLabTestRequired = true,
            )

        @BeforeClass
        @JvmStatic
        fun classSetUp() {
            CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        }

        const val TIMEOUT_SECONDS = 10L
    }
}
