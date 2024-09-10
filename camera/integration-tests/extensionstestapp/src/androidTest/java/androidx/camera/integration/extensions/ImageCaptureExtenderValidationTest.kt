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
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Rational
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.integration.extensions.CameraExtensionsActivity.CAMERA_PIPE_IMPLEMENTATION_OPTION
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.CameraXExtensionTestParams
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.getImageCaptureSupportedResolutions
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
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

@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ImageCaptureExtenderValidationTest(private val config: CameraXExtensionTestParams) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = config.implName == CAMERA_PIPE_IMPLEMENTATION_OPTION)

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(config.cameraXConfig)
        )

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraCharacteristics: CameraCharacteristics
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        assumeTrue(!ExtensionVersion.isAdvancedExtenderSupported())

        val (_, cameraXConfig, cameraId, extensionMode) = config
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]

        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        extensionCameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(baseCameraSelector, extensionMode)

        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }

        cameraCharacteristics =
            (camera.cameraInfo as CameraInfoInternal).cameraCharacteristics as CameraCharacteristics
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        val cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        withContext(Dispatchers.Main) { cameraProvider.shutdownAsync() }

        val extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]
        extensionsManager.shutdown()
    }

    companion object {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @JvmStatic
        @get:Parameterized.Parameters(name = "config = {0}")
        val parameters: Collection<CameraXExtensionTestParams>
            get() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @Test
    fun getSupportedResolutionsImplementationTest() {
        // getSupportedResolutions supported since version 1.1
        val version = ExtensionVersion.getRuntimeVersion()
        assumeTrue(version != null && version.compareTo(Version.VERSION_1_1) >= 0)

        // Creates the ImageCaptureExtenderImpl to retrieve the target format/resolutions pair list
        // from vendor library for the target effect mode.
        val impl =
            CameraXExtensionsTestUtil.createImageCaptureExtenderImpl(
                config.extensionMode,
                config.cameraId,
                cameraCharacteristics
            )

        // NoSuchMethodError will be thrown if getSupportedResolutions is not implemented in
        // vendor library, and then the test will fail.
        impl.supportedResolutions
    }

    @Test
    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    fun returnsNullFromOnPresetSession_whenAPILevelOlderThan28() {
        // Creates the ImageCaptureExtenderImpl to check that onPresetSession() returns null when
        // API level is older than 28.
        val impl =
            CameraXExtensionsTestUtil.createImageCaptureExtenderImpl(
                config.extensionMode,
                config.cameraId,
                cameraCharacteristics
            )
        assertThat(impl.onPresetSession()).isNull()
    }

    @Test
    fun getEstimatedCaptureLatencyRangeSameAsImplClass_sinceVersion_1_2(): Unit = runBlocking {
        assumeTrue(ExtensionVersion.getRuntimeVersion()!!.compareTo(Version.VERSION_1_2) >= 0)

        // This call should not cause any exception even if the vendor library doesn't implement
        // the getEstimatedCaptureLatencyRange function.
        val latencyInfo =
            extensionsManager.getEstimatedCaptureLatencyRange(
                baseCameraSelector,
                config.extensionMode
            )

        // Creates ImageCaptureExtenderImpl directly to retrieve the capture latency range info
        val impl =
            CameraXExtensionsTestUtil.createImageCaptureExtenderImpl(
                config.extensionMode,
                config.cameraId,
                cameraCharacteristics
            )
        val expectedLatencyInfo = impl.getEstimatedCaptureLatencyRange(null)

        // Compares the values obtained from ExtensionsManager and ImageCaptureExtenderImpl are
        // the same.
        assertThat(latencyInfo).isEqualTo(expectedLatencyInfo)
    }

    /**
     * The following 1.4 interface methods are validated by this test.
     * <ol>
     * <li>ImageCaptureExtenderImpl#isPostviewAvailable()
     * <li>ImageCaptureExtenderImpl#getSupportedPostviewResolutions()
     * </ol>
     */
    @Test
    fun validatePostviewSupport_sinceVersion_1_4() {
        // Runs the test only when the vendor library implementation is 1.4 or above
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_4)

        val impl =
            CameraXExtensionsTestUtil.createImageCaptureExtenderImpl(
                config.extensionMode,
                config.cameraId,
                cameraCharacteristics
            )

        // Runs the test only when postview is available
        assumeTrue(impl.isPostviewAvailable)

        var anyPostViewSupported = false

        getImageCaptureSupportedResolutions(impl, cameraCharacteristics).forEach { captureSize ->
            anyPostViewSupported = true
            var captureSizeSupported = false
            var yuvFormatSupported = false
            impl.getSupportedPostviewResolutions(captureSize)?.forEach {
                captureSizeSupported = true
                if (it.first == ImageFormat.YUV_420_888) {
                    yuvFormatSupported = true
                }

                it.second.forEach { postviewSize ->
                    // The postview size be smaller than or equal to the provided capture size.
                    assertThat(SizeUtil.getArea(postviewSize))
                        .isAtMost(SizeUtil.getArea(captureSize))
                    // The postview size must have the same aspect ratio as the given capture size.
                    assertThat(
                            AspectRatioUtil.hasMatchingAspectRatio(
                                postviewSize,
                                Rational(captureSize.width, captureSize.height)
                            )
                        )
                        .isTrue()
                }
            }
            // When postview is supported for the capture size, as the javadoc description,
            // YUV_420_888 format must be supported.
            if (captureSizeSupported) {
                assertThat(yuvFormatSupported).isTrue()
            }
        }

        // At least one postview size must be supported when isPostviewAvailable returns true.
        assertThat(anyPostViewSupported).isTrue()
    }

    @Test
    fun validateSessionTypeSupport_sinceVersion_1_4() {
        // Runs the test only when the vendor library implementation is 1.4 or above
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_4)

        val imageCaptureExtenderImpl =
            CameraXExtensionsTestUtil.createImageCaptureExtenderImpl(
                config.extensionMode,
                config.cameraId,
                cameraCharacteristics
            )

        // onSessionType is allowed to return any OEM customized session type, therefore, we can
        // only try to invoke this method to make sure that this method correctly exists in the
        // vendor library implementation and checks the returned.
        val imageCaptureSessionType = imageCaptureExtenderImpl.onSessionType()

        val previewExtenderImpl =
            CameraXExtensionsTestUtil.createPreviewExtenderImpl(
                config.extensionMode,
                config.cameraId,
                cameraCharacteristics
            )

        val previewSessionType = previewExtenderImpl.onSessionType()

        // Checks the session type values retrieved from ImageCaptureExtenderImpl and
        // PreviewExtenderImpl are the same.
        assertThat(imageCaptureSessionType).isEqualTo(previewSessionType)
    }

    @Test
    fun validateProcessProgressSupport_sinceVersion_1_4() {
        // Runs the test only when the vendor library implementation is 1.4 or above
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_4)

        val imageCaptureExtenderImpl =
            CameraXExtensionsTestUtil.createImageCaptureExtenderImpl(
                config.extensionMode,
                config.cameraId,
                cameraCharacteristics
            )

        // Makes sure isCaptureProcessProgressAvailable API can be called without any exception
        // occurring when the vendor library is 1.4 or above
        imageCaptureExtenderImpl.isCaptureProcessProgressAvailable
    }
}
