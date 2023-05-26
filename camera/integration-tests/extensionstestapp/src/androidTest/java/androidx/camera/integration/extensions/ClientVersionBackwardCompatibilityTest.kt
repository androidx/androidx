/*
 * Copyright 2023 The Android Open Source Project
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
import android.hardware.camera2.CaptureResult
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SessionProcessor.CaptureCallback
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
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

/**
 * These tests ensures OEM Extensions implementation can work with earlier Extensions-Interface
 * clients. For example, OEM implements v1.4.0 Extensions-Interface, these tests ensure it works
 * well not only on CameraX app with v1.4.0 Extensions-Interface but also on apps with v1.3.0 and
 * prior versions.
 *
 * There are two types of tests:
 *   - Client supplied callback being invoked properly: Apps with older version of
 *   Extensions-Interface might lack some of the methods in these client supplied callback such as
 *   [SessionProcessorImpl#CaptureCallback]. So it's important that OEM extensions doesn't
 *   invoke these new methods on the client that uses older version.
 *   - Variants of the APIs should work properly: some methods such as
 *   [androidx.camera.extensions.imp.CaptureProcessorImpl#process]has two overloaded methods.
 *   While client with latest version will invoke the newer version,
 *   the older one will invoke another version. So it's important to have both version working as
 *   expected.
 *
 *  This class tests these compatibility issues by verifying some high-level functions on top of
 *  CameraX full Extensions implementations because it's difficult and wasted to create a full
 *  functional fake implementation and difficult to monitor the call to the low level
 *  Extensions-Interface instances.
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ClientVersionBackwardCompatibilityTest(private val config: CameraIdExtensionModePair) {
    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "config = {0}")
        val parameters: Collection<CameraIdExtensionModePair>
            get() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var lifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp(): Unit = runBlocking(Dispatchers.Main) {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()
        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(config.cameraId)
    }

    @After
    fun tearDown() = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdown()[10, TimeUnit.SECONDS]
            }
        }

        if (::extensionsManager.isInitialized) {
            withContext(Dispatchers.Main) {
                extensionsManager.shutdown()[10, TimeUnit.SECONDS]
            }
        }
    }

    private suspend fun startCameraAndGetSessionProcessor(
        clientVersion: String,
        minRuntimeVersion: Version? = null
    ): SessionProcessor {
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider,
            clientVersion
        )[10000, TimeUnit.MILLISECONDS]
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, config.extensionMode))
        minRuntimeVersion?.let {
            assumeTrue(ExtensionVersion.isMinimumCompatibleVersion(minRuntimeVersion))
        }
        extensionCameraSelector = extensionsManager
            .getExtensionEnabledCameraSelector(baseCameraSelector, config.extensionMode)

        val previewFrameLatch = CountDownLatch(1)
        val camera = withContext(Dispatchers.Main) {
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()

            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                    previewFrameLatch.countDown()
                }
            )
            cameraProvider.bindToLifecycle(
                lifecycleOwner, extensionCameraSelector,
                preview, imageCapture
            )
        }
        assertThat(previewFrameLatch.await(3, TimeUnit.SECONDS)).isTrue()
        return camera.extendedConfig.sessionProcessor
    }

    private suspend fun verifyOnCaptureCompletedInvoked_stillCapture(
        clientVersion: String,
        shouldInvoke: Boolean
    ) {
        val sessionProcessor = startCameraAndGetSessionProcessor(
            clientVersion = clientVersion,
            minRuntimeVersion = Version.VERSION_1_3
        )

        val latchSequenceCompleted = CountDownLatch(1)
        var isOnCaptureCompletedInvoked = false
        sessionProcessor.startCapture(object : CaptureCallback {
            override fun onCaptureSequenceCompleted(captureSequenceId: Int) {
                latchSequenceCompleted.countDown()
            }

            override fun onCaptureCompleted(
                timestamp: Long,
                captureSequenceId: Int,
                result: MutableMap<CaptureResult.Key<Any>, Any>
            ) {
                isOnCaptureCompletedInvoked = true
            }
        })

        assertThat(latchSequenceCompleted.await(3, TimeUnit.SECONDS)).isTrue()
        assertThat(isOnCaptureCompletedInvoked).isEqualTo(shouldInvoke)
    }

    private suspend fun verifyOnCaptureCompletedInvoked_repeating(
        clientVersion: String,
        shouldInvoke: Boolean
    ) {
        val sessionProcessor = startCameraAndGetSessionProcessor(
            clientVersion = clientVersion,
            minRuntimeVersion = Version.VERSION_1_3
        )

        val latchSequenceCompleted = CountDownLatch(1)
        var isOnCaptureCompletedInvoked = false

        sessionProcessor.startRepeating(object : CaptureCallback {
            override fun onCaptureSequenceCompleted(captureSequenceId: Int) {
                latchSequenceCompleted.countDown()
            }

            override fun onCaptureCompleted(
                timestamp: Long,
                captureSequenceId: Int,
                result: MutableMap<CaptureResult.Key<Any>, Any>
            ) {
                isOnCaptureCompletedInvoked = true
            }
        })

        assertThat(latchSequenceCompleted.await(3, TimeUnit.SECONDS)).isTrue()
        assertThat(isOnCaptureCompletedInvoked).isEqualTo(shouldInvoke)
    }

    /**
     * For Advanced Extender, SessionProcessor.onCaptureCompleted is invoked when
     * SessionProcessorImpl.onCaptureCompleted is invoked. So it's effective to verify just
     * SessionProcessor.onCaptureCompleted.
     *
     * For Basic Extender, CaptureProcessorImpl#process(..) and
     * CaptureProcessorImpl#process(.. ProcessResultImpl) will be invoked depending on the client and
     * OEM versions. And only the process() with ProcessResultImpl version will trigger the
     * SessionProcessor.onCaptureCompleted call. So we can verify if
     * SessionProcessor.onCaptureCompleted is invoked or not to see if the correct version of the
     * process() is invoked.
     */
    @Test
    fun stillCapture_onCaptureCompletedInvoked_1_3_0() = runBlocking {
        verifyOnCaptureCompletedInvoked_stillCapture(clientVersion = "1.3.0", shouldInvoke = true)
    }

    @Test
    fun stillCapture_onCaptureCompletedInvoked_1_3_0_above() = runBlocking {
        // use a significantly large version to see if the OEM appropriately implements the version
        // comparison.
        verifyOnCaptureCompletedInvoked_stillCapture(clientVersion = "1.7.0", shouldInvoke = true)
    }

    @Test
    fun stillCapture_onCaptureCompletedNotInvoked_1_3_0_below() = runBlocking {
        verifyOnCaptureCompletedInvoked_stillCapture(clientVersion = "1.2.0", shouldInvoke = false)
    }

    @Test
    fun repeating_onCaptureCompletedInvoked_1_3_0() = runBlocking {
        verifyOnCaptureCompletedInvoked_repeating(clientVersion = "1.3.0", shouldInvoke = true)
    }

    @Test
    fun repeating_onCaptureCompletedInvoked_1_3_0_above() = runBlocking {
        // use a significantly large version to see if the OEM appropriately implements the version
        // comparison.
        verifyOnCaptureCompletedInvoked_repeating(clientVersion = "1.7.0", shouldInvoke = true)
    }

    @Test
    fun repeating_onCaptureCompletedNotInvoked_1_3_0_below() = runBlocking {
        verifyOnCaptureCompletedInvoked_repeating(clientVersion = "1.2.0", shouldInvoke = false)
    }
}