/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.integration.extensions

import android.content.Context
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.impl.CameraEventCallback
import androidx.camera.camera2.impl.CameraEventCallbacks
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.ExtensionsTestUtil
import androidx.camera.integration.extensions.util.ExtensionsTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.integration.extensions.util.ExtensionsTestUtil.STRESS_TEST_REPEAT_COUNT
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.RepeatRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
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
@SdkSuppress(minSdkVersion = 21)
class OpenCloseCaptureSessionStressTest(
    private val cameraId: String,
    private val extensionMode: Int
) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    @get:Rule
    val repeatRule = RepeatRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var camera: Camera
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: ImageAnalysis
    private var isImageAnalysisSupported = false
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private val cameraEventMonitor = CameraEventMonitor()

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(ExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )

        camera = withContext(Dispatchers.Main) {
            lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            cameraProvider.bindToLifecycle(lifecycleOwner, extensionCameraSelector)
        }

        preview = Preview.Builder().build()
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
        }
        imageCapture = ImageCapture.Builder().build()
        imageAnalysis = ImageAnalysis.Builder().build()

        isImageAnalysisSupported =
            camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis)
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
                cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
            }
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewImageCapture(): Unit = runBlocking {
        bindUseCase_unbindAll_toCheckCameraEvent_repeatedly(preview, imageCapture)
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewImageCaptureImageAnalysis(): Unit =
        runBlocking {
            val imageAnalysis = ImageAnalysis.Builder().build()
            assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
            bindUseCase_unbindAll_toCheckCameraEvent_repeatedly(
                preview,
                imageCapture,
                imageAnalysis
            )
        }

    /**
     * Repeatedly binds use cases, unbind all to check whether the capture session can be opened
     * and closed successfully by monitoring the CameraEvent callbacks.
     */
    private fun bindUseCase_unbindAll_toCheckCameraEvent_repeatedly(
        vararg useCases: UseCase,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT
    ): Unit = runBlocking {
        for (i in 1..repeatCount) {
            // Arrange: resets the camera event monitor
            cameraEventMonitor.reset()

            withContext(Dispatchers.Main) {
                // Arrange: retrieves the camera selector which allows to monitor camera event
                // callbacks
                val extensionEnabledCameraEventMonitorCameraSelector =
                    getExtensionsCameraEventMonitorCameraSelector(
                        extensionsManager,
                        extensionMode,
                        baseCameraSelector
                    )

                // Act: binds use cases
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    extensionEnabledCameraEventMonitorCameraSelector,
                    *useCases
                )
            }

            // Assert: checks the CameraEvent#onEnableSession callback function is called
            cameraEventMonitor.awaitSessionEnabledAndAssert()

            // Act: unbinds all use cases
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
            }

            // Assert: checks the CameraEvent#onSessionDisabled callback function is called
            cameraEventMonitor.awaitSessionDisabledAndAssert()
        }
    }

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        val parameters: Collection<Array<Any>>
            get() = ExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()

        /**
         * Retrieves the default extended camera config provider id string
         */
        private fun getExtendedCameraConfigProviderId(@ExtensionMode.Mode mode: Int): String =
            when (mode) {
                ExtensionMode.BOKEH -> "EXTENSION_MODE_BOKEH"
                ExtensionMode.HDR -> "EXTENSION_MODE_HDR"
                ExtensionMode.NIGHT -> "EXTENSION_MODE_NIGHT"
                ExtensionMode.FACE_RETOUCH -> "EXTENSION_MODE_FACE_RETOUCH"
                ExtensionMode.AUTO -> "EXTENSION_MODE_AUTO"
                else -> throw IllegalArgumentException("Invalid extension mode!")
            }.let {
                return ":camera:camera-extensions-$it"
            }

        /**
         * Retrieves the camera event monitor extended camera config provider id string
         */
        private fun getCameraEventMonitorCameraConfigProviderId(
            @ExtensionMode.Mode mode: Int
        ): String = "${getExtendedCameraConfigProviderId(mode)}-camera-event-monitor"
    }

    /**
     * Gets the camera selector which allows to monitor the camera event callbacks
     */
    private fun getExtensionsCameraEventMonitorCameraSelector(
        extensionsManager: ExtensionsManager,
        extensionMode: Int,
        baseCameraSelector: CameraSelector
    ): CameraSelector {
        // Injects the ExtensionsCameraEventMonitorUseCaseConfigFactory which allows to monitor and
        // verify the camera event callbacks
        injectExtensionsCameraEventMonitorUseCaseConfigFactory(
            extensionsManager,
            extensionMode,
            baseCameraSelector
        )

        val builder = CameraSelector.Builder.fromSelector(baseCameraSelector)
        // Add an ExtensionCameraEventMonitorCameraFilter which includes the CameraFilter to check
        // whether the camera is supported for the extension mode or not and also includes the
        // identifier to find the extended camera config provider from
        // ExtendedCameraConfigProviderStore
        builder.addCameraFilter(
            ExtensionsCameraEventMonitorCameraFilter(
                extensionsManager,
                extensionMode
            )
        )
        return builder.build()
    }

    /**
     * Injects the ExtensionsCameraEventMonitorUseCaseConfigFactory which allows to monitor and
     * verify the camera event callbacks
     */
    private fun injectExtensionsCameraEventMonitorUseCaseConfigFactory(
        extensionsManager: ExtensionsManager,
        extensionMode: Int,
        baseCameraSelector: CameraSelector
    ): Unit = runBlocking {
        val defaultConfigProviderId =
            Identifier.create(getExtendedCameraConfigProviderId(extensionMode))
        val cameraEventConfigProviderId =
            Identifier.create(getCameraEventMonitorCameraConfigProviderId(extensionMode))

        // Calls the ExtensionsManager#getExtensionEnabledCameraSelector() function to add the
        // default extended camera config provider to ExtendedCameraConfigProviderStore
        extensionsManager.getExtensionEnabledCameraSelector(baseCameraSelector, extensionMode)

        // Injects the new camera config provider which will keep the original extensions needed
        // configs and also add additional CameraEventMonitor to monitor the camera event callbacks.
        ExtendedCameraConfigProviderStore.addConfig(cameraEventConfigProviderId) {
                cameraInfo: CameraInfo, context: Context ->
            // Retrieves the default extended camera config provider and
            // ExtensionsUseCaseConfigFactory
            val defaultCameraConfigProvider =
                ExtendedCameraConfigProviderStore.getConfigProvider(defaultConfigProviderId)
            val defaultCameraConfig = defaultCameraConfigProvider.getConfig(cameraInfo, context)!!
            val defaultExtensionsUseCaseConfigFactory =
                defaultCameraConfig.retrieveOption(CameraConfig.OPTION_USECASE_CONFIG_FACTORY, null)

            // Creates a new ExtensionsCameraEventMonitorUseCaseConfigFactory on top of the default
            // ExtensionsCameraEventMonitorUseCaseConfigFactory to monitor the capture session
            // callbacks
            val extensionsCameraEventMonitorUseCaseConfigFactory =
                ExtensionsCameraEventMonitorUseCaseConfigFactory(
                    defaultExtensionsUseCaseConfigFactory,
                    cameraEventMonitor
                )

            // Creates the config from the original config and replaces its use case config factory
            // with the ExtensionsCameraEventMonitorUseCaseConfigFactory
            val mutableOptionsBundle = MutableOptionsBundle.from(defaultCameraConfig)
            mutableOptionsBundle.insertOption(
                CameraConfig.OPTION_USECASE_CONFIG_FACTORY,
                extensionsCameraEventMonitorUseCaseConfigFactory
            )

            // Returns a CameraConfig implemented with the updated config
            object : CameraConfig {
                val config = OptionsBundle.from(mutableOptionsBundle)

                override fun getConfig(): Config {
                    return config
                }

                override fun getCompatibilityId(): Identifier {
                    return config.retrieveOption(CameraConfig.OPTION_COMPATIBILITY_ID)!!
                }
            }
        }
    }

    /**
     * A ExtensionsCameraEventMonitorCameraFilter which includes the CameraFilter to check whether
     * the camera is supported for the extension mode or not and also includes the identifier to
     * find the extended camera config provider from ExtendedCameraConfigProviderStore.
     */
    private class ExtensionsCameraEventMonitorCameraFilter constructor(
        private val extensionManager: ExtensionsManager,
        @ExtensionMode.Mode private val mode: Int
    ) : CameraFilter {
        override fun getIdentifier(): Identifier {
            return Identifier.create(getCameraEventMonitorCameraConfigProviderId(mode))
        }

        override fun filter(cameraInfos: MutableList<CameraInfo>): MutableList<CameraInfo> =
            cameraInfos.mapNotNull { cameraInfo ->
                val cameraId = Camera2CameraInfo.from(cameraInfo).cameraId
                val cameraIdCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
                if (extensionManager.isExtensionAvailable(cameraIdCameraSelector, mode)) {
                    cameraInfo
                } else {
                    null
                }
            }.toMutableList()
    }

    /**
     * A UseCaseConfigFactory implemented on top of the default ExtensionsUseCaseConfigFactory to
     * monitor the camera event callbacks
     */
    private class ExtensionsCameraEventMonitorUseCaseConfigFactory constructor(
        private val useCaseConfigFactory: UseCaseConfigFactory?,
        private val cameraEventMonitor: CameraEventMonitor
    ) :
        UseCaseConfigFactory {
        override fun getConfig(
            captureType: UseCaseConfigFactory.CaptureType,
            captureMode: Int
        ): Config {
            // Retrieves the config from the default ExtensionsUseCaseConfigFactory
            val mutableOptionsBundle = useCaseConfigFactory?.getConfig(
                captureType, captureMode
            )?.let {
                MutableOptionsBundle.from(it)
            } ?: MutableOptionsBundle.create()

            // Adds the CameraEventMonitor to the original CameraEventCallbacks of ImageCapture to
            // monitor the camera event callbacks
            if (captureType.equals(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE)) {
                var cameraEventCallbacks = mutableOptionsBundle.retrieveOption(
                    Camera2ImplConfig.CAMERA_EVENT_CALLBACK_OPTION,
                    null
                )

                if (cameraEventCallbacks != null) {
                    cameraEventCallbacks.addAll(
                        mutableListOf<CameraEventCallback>(
                            cameraEventMonitor
                        )
                    )
                } else {
                    cameraEventCallbacks = CameraEventCallbacks(cameraEventMonitor)
                }

                mutableOptionsBundle.insertOption(
                    Camera2ImplConfig.CAMERA_EVENT_CALLBACK_OPTION,
                    cameraEventCallbacks
                )
            }

            return OptionsBundle.from(mutableOptionsBundle)
        }
    }

    /**
     * An implementation of CameraEventCallback to monitor whether the camera event callbacks are
     * called properly or not.
     */
    private class CameraEventMonitor : CameraEventCallback() {
        private var sessionEnabledLatch = CountDownLatch(1)
        private var sessionDisabledLatch = CountDownLatch(1)

        override fun onEnableSession(): CaptureConfig? {
            sessionEnabledLatch.countDown()
            return super.onEnableSession()
        }

        override fun onDisableSession(): CaptureConfig? {
            sessionDisabledLatch.countDown()
            return super.onDisableSession()
        }

        fun reset() {
            sessionEnabledLatch = CountDownLatch(1)
            sessionDisabledLatch = CountDownLatch(1)
        }

        fun awaitSessionEnabledAndAssert() {
            assertThat(sessionEnabledLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun awaitSessionDisabledAndAssert() {
            assertThat(sessionDisabledLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }
}
