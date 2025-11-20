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
import android.os.Build
import android.view.WindowManager
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.Api27Impl.setShowWhenLocked
import androidx.camera.testing.impl.Api27Impl.setTurnScreenOn
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.ParameterizedTestConfigUtil
import androidx.camera.testing.impl.fakes.FakeActivity
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.testrule.CameraTestActivityScenarioRule
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PreviewViewBitmapTest(private val implName: String, private val cameraConfig: CameraXConfig) {
    @get:Rule val activityRule = CameraTestActivityScenarioRule(FakeActivity::class.java)

    @get:Rule
    var useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraSelector: CameraSelector

    @Before
    fun setUp() {
        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        val context = ApplicationProvider.getApplicationContext<Context>()
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
    }

    @After
    fun tearDown() {
        if (cameraProvider != null) {
            cameraProvider!!.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
            cameraProvider = null
        }
    }

    @Test
    fun bitmapIsNull_whenPreviewNotDisplaying_textureView() {
        assertBitmapIsNullWhenPreviewNotDisplaying(PreviewView.ImplementationMode.COMPATIBLE)
    }

    @Test
    fun bitmapIsNull_whenPreviewNotDisplaying_surfaceView() {
        assertBitmapIsNullWhenPreviewNotDisplaying(PreviewView.ImplementationMode.PERFORMANCE)
    }

    private fun assertBitmapIsNullWhenPreviewNotDisplaying(
        implementationMode: PreviewView.ImplementationMode
    ) {
        // Arrange
        val previewView = setUpPreviewView(implementationMode)
        val preview = Preview.Builder().build()
        runOnMainThread {
            val lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()

            // Act.
            preview.setSurfaceProvider(previewView.surfaceProvider)
            cameraProvider!!.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

            // Assert.
            // To assert the status before preview is displaying, we have to do it in the same
            // Runnable to avoid race condition.
            Truth.assertThat(previewView.bitmap).isNull()
        }
    }

    @Test
    fun bitmapNotNull_whenPreviewIsDisplaying_textureView() {
        // Arrange
        val previewView = setUpPreviewView(PreviewView.ImplementationMode.COMPATIBLE)

        // Act
        startPreview(previewView)
        waitForPreviewToStart(previewView)

        // assert
        runOnMainThread {
            val bitmap = previewView.bitmap
            Truth.assertThat(bitmap).isNotNull()
        }
    }

    @Test
    fun bitmapNotNull_whenPreviewIsDisplaying_surfaceView() {
        // Arrange
        val previewView = setUpPreviewView(PreviewView.ImplementationMode.PERFORMANCE)

        // Act
        startPreview(previewView)
        waitForPreviewToStart(previewView)

        // assert
        runOnMainThread {
            val bitmap = previewView.bitmap
            Truth.assertThat(bitmap).isNotNull()
        }
    }

    @Test
    fun bitmapHasSameSizeAsPreviewView_fillStart_textureView() {
        bitmapHasSameSizeAsPreviewView(
            PreviewView.ImplementationMode.COMPATIBLE,
            PreviewView.ScaleType.FILL_START,
        )
    }

    @Test
    fun bitmapHasSameSizeAsPreviewView_fillCenter_textureView() {
        bitmapHasSameSizeAsPreviewView(
            PreviewView.ImplementationMode.COMPATIBLE,
            PreviewView.ScaleType.FILL_CENTER,
        )
    }

    @Test
    fun bitmapHasSameSizeAsPreviewView_fillEnd_textureView() {
        bitmapHasSameSizeAsPreviewView(
            PreviewView.ImplementationMode.COMPATIBLE,
            PreviewView.ScaleType.FILL_END,
        )
    }

    @Test
    fun bitmapHasSameSizeAsPreviewView_fillStart_surfaceView() {
        bitmapHasSameSizeAsPreviewView(
            PreviewView.ImplementationMode.PERFORMANCE,
            PreviewView.ScaleType.FILL_START,
        )
    }

    @Test
    fun bitmapHasSameSizeAsPreviewView_fillCenter_surfaceView() {
        bitmapHasSameSizeAsPreviewView(
            PreviewView.ImplementationMode.PERFORMANCE,
            PreviewView.ScaleType.FILL_CENTER,
        )
    }

    @Test
    fun bitmapHasSameSizeAsPreviewView_fillEnd_surfaceView() {
        bitmapHasSameSizeAsPreviewView(
            PreviewView.ImplementationMode.PERFORMANCE,
            PreviewView.ScaleType.FILL_END,
        )
    }

    private fun bitmapHasSameSizeAsPreviewView(
        mode: PreviewView.ImplementationMode,
        scaleType: PreviewView.ScaleType,
    ) {
        // Arrange
        val previewView = setUpPreviewView(mode, scaleType)

        // Act
        startPreview(previewView)
        waitForPreviewToStart(previewView)

        // assert
        runOnMainThread {
            val bitmap = previewView.bitmap
            Truth.assertThat(bitmap).isNotNull()
            Truth.assertThat(bitmap!!.width).isEqualTo(previewView.width)
            Truth.assertThat(bitmap.height).isEqualTo(previewView.height)
        }
    }

    @Test
    fun bitmapSmallerInSizeThanPreviewView_fitStart_textureView() {
        bitmapSmallerInSizeThanPreviewView(
            PreviewView.ImplementationMode.COMPATIBLE,
            PreviewView.ScaleType.FIT_START,
        )
    }

    @Test
    fun bitmapSmallerInSizeThanPreviewView_fitCenter_textureView() {
        bitmapSmallerInSizeThanPreviewView(
            PreviewView.ImplementationMode.COMPATIBLE,
            PreviewView.ScaleType.FIT_CENTER,
        )
    }

    @Test
    fun bitmapSmallerInSizeThanPreviewView_fitEnd_textureView() {
        bitmapSmallerInSizeThanPreviewView(
            PreviewView.ImplementationMode.COMPATIBLE,
            PreviewView.ScaleType.FIT_END,
        )
    }

    @Test
    fun bitmapSmallerInSizeThanPreviewView_fitStart_surfaceView() {
        bitmapSmallerInSizeThanPreviewView(
            PreviewView.ImplementationMode.PERFORMANCE,
            PreviewView.ScaleType.FIT_START,
        )
    }

    @Test
    fun bitmapSmallerInSizeThanPreviewView_fitCenter_surfaceView() {
        bitmapSmallerInSizeThanPreviewView(
            PreviewView.ImplementationMode.PERFORMANCE,
            PreviewView.ScaleType.FIT_CENTER,
        )
    }

    @Test
    fun bitmapSmallerInSizeThanPreviewView_fitEnd_surfaceView() {
        bitmapSmallerInSizeThanPreviewView(
            PreviewView.ImplementationMode.PERFORMANCE,
            PreviewView.ScaleType.FIT_END,
        )
    }

    private fun bitmapSmallerInSizeThanPreviewView(
        mode: PreviewView.ImplementationMode,
        scaleType: PreviewView.ScaleType,
    ) {
        // Arrange
        val previewView = setUpPreviewView(mode, scaleType)

        // Act
        startPreview(previewView)
        waitForPreviewToStart(previewView)

        // assert
        runOnMainThread {
            val bitmap = previewView.bitmap
            Truth.assertThat(bitmap).isNotNull()
            Truth.assertThat(bitmap!!.width).isAtMost(previewView.width)
            Truth.assertThat(bitmap.height).isAtMost(previewView.height)
            Truth.assertThat(
                    bitmap.width == previewView.width || bitmap.height == previewView.height
                )
                .isTrue()
        }
    }

    private fun setUpPreviewView(mode: PreviewView.ImplementationMode): PreviewView {
        return setUpPreviewView(mode, PreviewView.ScaleType.FILL_CENTER)
    }

    private fun setUpPreviewView(
        mode: PreviewView.ImplementationMode,
        scaleType: PreviewView.ScaleType,
    ): PreviewView {
        val previewViewAtomicReference = AtomicReference<PreviewView>()
        runOnMainThread {
            val previewView = PreviewView(ApplicationProvider.getApplicationContext())
            previewView.implementationMode = mode
            previewView.scaleType = scaleType
            activityRule.scenario.onActivity { activity: FakeActivity ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    activity.setShowWhenLocked()
                    activity.setTurnScreenOn()
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    @Suppress("DEPRECATION")
                    activity.window.addFlags(
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    )
                }
                activity.setContentView(previewView)
            }
            previewViewAtomicReference.set(previewView)
        }
        return previewViewAtomicReference.get()
    }

    private fun startPreview(previewView: PreviewView) {
        val preview = Preview.Builder().build()
        runOnMainThread {
            val lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            cameraProvider!!.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
    }

    private fun waitForPreviewToStart(previewView: PreviewView) {
        val semaphore = Semaphore(0)
        val observer = Observer { streamState: PreviewView.StreamState ->
            if (streamState == PreviewView.StreamState.STREAMING) {
                semaphore.release()
            }
        }
        runOnMainThread { previewView.previewStreamState.observeForever(observer) }
        try {
            Truth.assertThat(semaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        } finally {
            runOnMainThread { previewView.previewStreamState.removeObserver(observer) }
        }
    }

    private fun runOnMainThread(block: Runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    companion object {

        @BeforeClass
        @JvmStatic
        fun classSetUp() {
            CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            ParameterizedTestConfigUtil.generateCameraXConfigParameterizedTestConfigs(
                inLabTestRequired = true
            )
    }
}
