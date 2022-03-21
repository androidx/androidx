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
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.ExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.GLUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.camera.testing.TimestampCaptureProcessor
import androidx.camera.testing.TimestampCaptureProcessor.TimestampListener
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import junit.framework.AssertionFailedError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class PreviewProcessorTimestampTest(
    private val cameraId: String,
    private val extensionMode: Int
) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    private val inputTimestampsLatch = CountDownLatch(1)
    private val outputTimestampsLatch = CountDownLatch(1)
    private val surfaceTextureLatch = CountDownLatch(1)
    private val inputTimestamps = hashSetOf<Long>()
    private val outputTimestamps = hashSetOf<Long>()

    private val timestampListener = object : TimestampListener {
        private var complete = false

        override fun onTimestampAvailable(timestamp: Long) {
            if (complete) {
                return
            }

            inputTimestamps.add(timestamp)
            if (inputTimestamps.size >= 10) {
                inputTimestampsLatch.countDown()
                complete = true
            }
        }
    }

    private var isSurfaceTextureReleased = false
    private val isSurfaceTextureReleasedLock = Any()

    private val onFrameAvailableListener = object : OnFrameAvailableListener {
        private var complete = false

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture): Unit = runBlocking {
            if (complete) {
                return@runBlocking
            }

            withContext(Dispatchers.Main) {
                synchronized(isSurfaceTextureReleasedLock) {
                    if (!isSurfaceTextureReleased) {
                        surfaceTexture.updateTexImage()
                    }
                }
            }

            outputTimestamps.add(surfaceTexture.timestamp)

            if (outputTimestamps.size >= 10) {
                outputTimestampsLatch.countDown()
                complete = true
            }
        }
    }

    private val processingHandler: Handler
    private val processingHandlerThread = HandlerThread("Processing").also {
        it.start()
        processingHandler = Handler(it.looper)
    }

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(ExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        assumeFalse(Build.BRAND.equals("Samsung", ignoreCase = true))
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
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

    @Test
    fun timestampIsCorrect(): Unit = runBlocking {
        withContext(Dispatchers.Main) {
            val preview = Preview.Builder().build()

            preview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider(createSurfaceTextureCallback())
            )

            // Retrieves the camera selector which a timestamp capture processor is applied
            val timestampExtensionEnabledCameraSelector =
                getTimestampExtensionEnabledCameraSelector(
                    extensionsManager,
                    extensionMode,
                    baseCameraSelector
                )

            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                timestampExtensionEnabledCameraSelector,
                preview
            )
        }

        // Waits for the surface texture being ready
        assertThat(surfaceTextureLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        // Waits for 10 input and output frame timestamps are collected
        assertThat(inputTimestampsLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(outputTimestampsLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        // Verifies that the input and output frame timestamps are the same
        assertThat(outputTimestamps).containsExactlyElementsIn(inputTimestamps)
    }

    private fun createSurfaceTextureCallback(): SurfaceTextureCallback =
        object : SurfaceTextureCallback {
            override fun onSurfaceTextureReady(
                surfaceTexture: SurfaceTexture,
                resolution: Size
            ) {
                surfaceTexture.attachToGLContext(GLUtil.getTexIdFromGLContext())
                surfaceTexture.setOnFrameAvailableListener(
                    onFrameAvailableListener, processingHandler
                )
                surfaceTextureLatch.countDown()
            }

            override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                synchronized(isSurfaceTextureReleasedLock) {
                    isSurfaceTextureReleased = true
                    surfaceTexture.release()
                }
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
                ExtensionMode.NONE -> "EXTENSION_MODE_NONE"
                else -> throw IllegalArgumentException("Invalid extension mode!")
            }.let {
                return ":camera:camera-extensions-$it"
            }

        /**
         * Retrieves the timestamp extended camera config provider id string
         */
        private fun getTimestampCameraConfigProviderId(@ExtensionMode.Mode mode: Int): String =
            "${getExtendedCameraConfigProviderId(mode)}-timestamp"
    }

    /**
     * Gets the camera selector which a timestamp capture processor is applied
     */
    private fun getTimestampExtensionEnabledCameraSelector(
        extensionsManager: ExtensionsManager,
        extensionMode: Int,
        baseCameraSelector: CameraSelector
    ): CameraSelector {
        // Injects the TimestampExtensionsUseCaseConfigFactory which allows to monitor and verify
        // the frames' timestamps
        injectTimestampExtensionsUseCaseConfigFactory(
            extensionsManager,
            extensionMode,
            baseCameraSelector
        )

        val builder = CameraSelector.Builder.fromSelector(baseCameraSelector)
        // Add a TimestampExtensionCameraFilter which includes the CameraFilter to check whether
        // the camera is supported for the extension mode or not and also includes the identifier
        // to find the extended camera config provider from ExtendedCameraConfigProviderStore
        builder.addCameraFilter(TimestampExtensionCameraFilter(extensionsManager, extensionMode))
        return builder.build()
    }

    /**
     * Injects the TimestampExtensionsUseCaseConfigFactory which allows to monitor and verify the
     * frames' timestamps
     */
    private fun injectTimestampExtensionsUseCaseConfigFactory(
        extensionsManager: ExtensionsManager,
        extensionMode: Int,
        baseCameraSelector: CameraSelector
    ): Unit = runBlocking {
        val timestampConfigProviderId =
            Identifier.create(getTimestampCameraConfigProviderId(extensionMode))

        // Calling the ExtensionsManager#getExtensionEnabledCameraSelector() function to add the
        // default extended camera config provider to ExtendedCameraConfigProviderStore
        extensionsManager.getExtensionEnabledCameraSelector(baseCameraSelector, extensionMode)

        ExtendedCameraConfigProviderStore.addConfig(timestampConfigProviderId) {
                cameraInfo: CameraInfo, context: Context ->

            // Retrieves the default extended camera config provider and
            // ExtensionsUseCaseConfigFactory
            val defaultConfigProviderId =
                Identifier.create(getExtendedCameraConfigProviderId(extensionMode))
            val defaultCameraConfigProvider =
                ExtendedCameraConfigProviderStore.getConfigProvider(defaultConfigProviderId)
            val defaultCameraConfig = defaultCameraConfigProvider.getConfig(cameraInfo, context)!!
            val defaultExtensionsUseCaseConfigFactory =
                defaultCameraConfig.retrieveOption(CameraConfig.OPTION_USECASE_CONFIG_FACTORY)

            // Creates a new TimestampExtensionsUseCaseConfigFactory on top of the default
            // ExtensionsUseCaseConfigFactory to monitor the frames' timestamps
            val timestampExtensionsUseCaseConfigFactory = TimestampExtensionsUseCaseConfigFactory(
                defaultExtensionsUseCaseConfigFactory!!,
                timestampListener
            )

            // Creates the config from the original config and replaces its use case config factory
            // with the TimestampExtensionsUseCaseConfigFactory
            val mutableOptionsBundle = MutableOptionsBundle.from(defaultCameraConfig)
            mutableOptionsBundle.insertOption(
                CameraConfig.OPTION_USECASE_CONFIG_FACTORY,
                timestampExtensionsUseCaseConfigFactory
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
     * A TimestampExtensionCameraFilter which includes the CameraFilter to check whether the camera
     * is supported for the extension mode or not and also includes the identifier to find the
     * extended camera config provider from ExtendedCameraConfigProviderStore.
     */
    private class TimestampExtensionCameraFilter constructor(
        private val extensionManager: ExtensionsManager,
        @ExtensionMode.Mode private val mode: Int
    ) : CameraFilter {
        override fun getIdentifier(): Identifier {
            return Identifier.create(getTimestampCameraConfigProviderId(mode))
        }

        override fun filter(cameraInfos: MutableList<CameraInfo>): MutableList<CameraInfo> {
            val resultInfos = mutableListOf<CameraInfo>()

            cameraInfos.forEach {
                val cameraId = Camera2CameraInfo.from(it).cameraId
                val cameraIdCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
                if (extensionManager.isExtensionAvailable(cameraIdCameraSelector, mode)) {
                    resultInfos.add(it)
                }
            }

            return resultInfos
        }
    }

    /**
     * A UseCaseConfigFactory implemented on top of the default ExtensionsUseCaseConfigFactory to
     * monitor the frames' timestamps
     */
    private class TimestampExtensionsUseCaseConfigFactory constructor(
        private val useCaseConfigFactory: UseCaseConfigFactory,
        private val timestampListener: TimestampListener
    ) :
        UseCaseConfigFactory {
        override fun getConfig(captureType: UseCaseConfigFactory.CaptureType): Config? {
            // Retrieves the config from the default ExtensionsUseCaseConfigFactory
            val mutableOptionsBundle = useCaseConfigFactory.getConfig(captureType)?.let {
                MutableOptionsBundle.from(it)
            } ?: throw AssertionFailedError("Can not retrieve config for capture type $captureType")

            // Replaces the PreviewCaptureProcessor by the TimestampCaptureProcessor to monitor the
            // frames' timestamps
            if (captureType.equals(UseCaseConfigFactory.CaptureType.PREVIEW)) {
                val previewCaptureProcessor = mutableOptionsBundle.retrieveOption(
                    PreviewConfig.OPTION_PREVIEW_CAPTURE_PROCESSOR,
                    null
                )

                assumeNotNull(previewCaptureProcessor)

                mutableOptionsBundle.insertOption(
                    PreviewConfig.OPTION_PREVIEW_CAPTURE_PROCESSOR,
                    TimestampCaptureProcessor(previewCaptureProcessor!!, timestampListener)
                )
            }

            return OptionsBundle.from(mutableOptionsBundle)
        }
    }
}