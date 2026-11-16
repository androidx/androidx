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

package androidx.camera.extensions.internal

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Pair
import android.util.Range
import android.util.Size
import androidx.camera.core.impl.AdapterCameraControl
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraExtensionCapabilities
import androidx.camera.extensions.ExtensionMode
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeCameraExtensionCapabilities
import androidx.camera.testing.impl.fakes.FakeCameraExtensionCapabilities.CaptureConfig
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.TIRAMISU)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class Camera2ExtensionsVendorExtenderTest {

    private val jpegSizes = setOf(Size(1920, 1080))
    private val yuvSizes = setOf(Size(1280, 720))
    private val jpegRSizes = setOf(Size(3840, 2160))
    private val captureOutputSizes =
        mapOf(
            ImageFormat.JPEG to jpegSizes,
            ImageFormat.YUV_420_888 to yuvSizes,
            ImageFormat.JPEG_R to jpegRSizes,
        )
    private val postviewCaptureSize = Size(4000, 3000)
    private val postviewSizes =
        mapOf(
            CaptureConfig(postviewCaptureSize, ImageFormat.JPEG) to jpegSizes,
            CaptureConfig(postviewCaptureSize, ImageFormat.YUV_420_888) to yuvSizes,
            CaptureConfig(postviewCaptureSize, ImageFormat.JPEG_R) to jpegRSizes,
        )

    @After
    fun tearDown() {
        ShadowBuild.reset()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.TIRAMISU, maxSdk = Build.VERSION_CODES.TIRAMISU)
    fun getSupportedCaptureOutputResolutions_api33_doesNotQueryJpegR() {
        val extender = createInitializedExtender(outputSizesFormat = captureOutputSizes)

        val formats = extender.supportedCaptureOutputResolutions.map { it.first }

        assertThat(formats).containsExactly(ImageFormat.JPEG, ImageFormat.YUV_420_888)
    }

    @Test
    @Config(
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    )
    fun getSupportedCaptureOutputResolutions_api34_doesNotQueryJpegR() {
        // The API 34 framework throws "Unsupported format: 4101" when JPEG_R is queried.
        val extender = createInitializedExtender(outputSizesFormat = captureOutputSizes)

        val formats = extender.supportedCaptureOutputResolutions.map { it.first }

        assertThat(formats).containsExactly(ImageFormat.JPEG, ImageFormat.YUV_420_888)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getSupportedCaptureOutputResolutions_api35_queriesJpegR() {
        val extender = createInitializedExtender(outputSizesFormat = captureOutputSizes)

        val formats = extender.supportedCaptureOutputResolutions.map { it.first }

        assertThat(formats)
            .containsExactly(ImageFormat.JPEG, ImageFormat.YUV_420_888, ImageFormat.JPEG_R)
    }

    @Test
    @Config(
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    )
    fun getSupportedPostviewResolutions_api34_doesNotQueryJpegR() {
        val extender = createInitializedExtender(postviewSizes = postviewSizes)

        val formats = extender.getSupportedPostviewResolutions(postviewCaptureSize).keys

        assertThat(formats).containsExactly(ImageFormat.JPEG, ImageFormat.YUV_420_888)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getSupportedPostviewResolutions_api35_queriesJpegR() {
        val extender = createInitializedExtender(postviewSizes = postviewSizes)

        val formats = extender.getSupportedPostviewResolutions(postviewCaptureSize).keys

        assertThat(formats)
            .containsExactly(ImageFormat.JPEG, ImageFormat.YUV_420_888, ImageFormat.JPEG_R)
    }

    @Suppress("DEPRECATION") // EXTENSION_BEAUTY
    @Test
    fun isExtensionAvailable_returnsFalseWhenDisabledByQuirk() {
        ShadowBuild.setBrand("Samsung")
        ShadowBuild.setModel("SM-A528B")
        val camera0 =
            FakeCameraInfoInternal("0").apply {
                setSupportedExtensions(
                    setOf(
                        CameraExtensionCharacteristics.EXTENSION_BOKEH,
                        CameraExtensionCharacteristics.EXTENSION_BEAUTY,
                        CameraExtensionCharacteristics.EXTENSION_HDR,
                        CameraExtensionCharacteristics.EXTENSION_AUTOMATIC,
                    )
                )
            }
        val camera1 =
            FakeCameraInfoInternal("1").apply {
                setSupportedExtensions(setOf(CameraExtensionCharacteristics.EXTENSION_BOKEH))
            }

        assertThat(
                Camera2ExtensionsVendorExtender(ExtensionMode.BOKEH).isExtensionAvailable(camera0)
            )
            .isFalse()
        assertThat(
                Camera2ExtensionsVendorExtender(ExtensionMode.FACE_RETOUCH)
                    .isExtensionAvailable(camera0)
            )
            .isFalse()
        assertThat(Camera2ExtensionsVendorExtender(ExtensionMode.HDR).isExtensionAvailable(camera0))
            .isFalse()
        assertThat(
                Camera2ExtensionsVendorExtender(ExtensionMode.AUTO).isExtensionAvailable(camera0)
            )
            .isTrue()
        assertThat(
                Camera2ExtensionsVendorExtender(ExtensionMode.BOKEH).isExtensionAvailable(camera1)
            )
            .isTrue()
    }

    @Test
    fun init_doesNotEagerlyQueryCharacteristics() {
        val (extender, capabilities, _) =
            createCountingExtender(
                characteristicsKeys =
                    mapOf(
                        CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE to Range(1.0f, 5.0f),
                        CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to
                            intArrayOf(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON),
                    )
            )

        extender.createSessionProcessor(ApplicationProvider.getApplicationContext())

        assertThat(capabilities.queriedKeys).isEmpty()
        assertThat(capabilities.getAvailableCharacteristicsKeyValuesCallCount).isEqualTo(0)
    }

    @Test
    fun getExtensionZoomRange_queriesSingleKeyOnceAndCachesResult() {
        val expectedRange = Range(1.0f, 5.0f)
        val (extender, capabilities, _) =
            createCountingExtender(
                characteristicsKeys =
                    mapOf(
                        CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE to expectedRange,
                        CameraCharacteristics.SENSOR_ORIENTATION to 90,
                    )
            )

        assertThat(extender.extensionZoomRange).isEqualTo(expectedRange)
        assertThat(extender.extensionZoomRange).isEqualTo(expectedRange)
        assertThat(extender.extensionZoomRange).isEqualTo(expectedRange)

        assertThat(capabilities.queriedKeys)
            .containsExactly(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        assertThat(capabilities.getAvailableCharacteristicsKeyValuesCallCount).isEqualTo(0)
    }

    @Test
    fun getExtensionZoomRange_whenKeyAbsent_cachesNullResult() {
        val (extender, capabilities, _) =
            createCountingExtender(
                characteristicsKeys = mapOf(CameraCharacteristics.SENSOR_ORIENTATION to 90)
            )

        assertThat(extender.extensionZoomRange).isNull()
        assertThat(extender.extensionZoomRange).isNull()

        assertThat(capabilities.queriedKeys)
            .containsExactly(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        assertThat(capabilities.getAvailableCharacteristicsKeyValuesCallCount).isEqualTo(0)
    }

    @Test
    fun getExtensionZoomRangeAndStabilizationModes_whenCapabilitiesThrows_returnsAndCachesNull() {
        listOf<Throwable>(IllegalArgumentException("bad key"), AssertionError("missing keys"))
            .forEach { failure ->
                val (extender, capabilities, _) = createCountingExtender(onGet = { throw failure })

                assertThat(extender.extensionZoomRange).isNull()
                assertThat(extender.extensionZoomRange).isNull()
                assertThat(extender.extensionAvailableStabilizationModes).isNull()
                assertThat(extender.extensionAvailableStabilizationModes).isNull()

                assertThat(capabilities.queriedKeys)
                    .containsExactly(
                        CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE,
                        CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES,
                    )
            }
    }

    @Test
    fun getExtensionAvailableStabilizationModes_cachesAndReturnsDefensiveCopy() {
        val modes = intArrayOf(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)
        val (extender, capabilities, _) =
            createCountingExtender(
                characteristicsKeys =
                    mapOf(
                        CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to modes,
                        CameraCharacteristics.SENSOR_ORIENTATION to 90,
                    )
            )

        val firstCall = requireNotNull(extender.extensionAvailableStabilizationModes)
        assertThat(firstCall.asList())
            .containsExactly(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)
        firstCall[0] = -1

        val secondCall = requireNotNull(extender.extensionAvailableStabilizationModes)
        assertThat(secondCall.asList())
            .containsExactly(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)

        assertThat(capabilities.queriedKeys)
            .containsExactly(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
        assertThat(capabilities.getAvailableCharacteristicsKeyValuesCallCount).isEqualTo(0)
    }

    @Test
    fun getExtensionZoomRangeAndStabilizationModes_beforeInit_throwIllegalStateException() {
        val extender = Camera2ExtensionsVendorExtender(ExtensionMode.NIGHT)

        assertThrows(IllegalStateException::class.java) { extender.extensionZoomRange }
        assertThrows(IllegalStateException::class.java) {
            extender.extensionAvailableStabilizationModes
        }
    }

    @Test
    fun sessionProcessor_sharesCachedValuesAcrossAdapterCameraInfoAndControl() {
        val expectedRange = Range(1.0f, 5.0f)
        val (extender, capabilities, cameraInfo) =
            createCountingExtender(
                availableCaptureRequestKeys = setOf(CaptureRequest.CONTROL_ZOOM_RATIO),
                characteristicsKeys =
                    mapOf(
                        CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE to expectedRange,
                        CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to
                            intArrayOf(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON),
                        CameraCharacteristics.SENSOR_ORIENTATION to 90,
                    ),
            )
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val sessionProcessor1 = requireNotNull(extender.createSessionProcessor(context))
        val sessionProcessor2 = requireNotNull(extender.createSessionProcessor(context))
        val adapterInfo1 =
            AdapterCameraInfo(cameraInfo, FakeCameraConfig(sessionProcessor = sessionProcessor1))
        val adapterInfo2 =
            AdapterCameraInfo(cameraInfo, FakeCameraConfig(sessionProcessor = sessionProcessor2))
        val fakeControl1 = FakeCameraControl()
        val fakeControl2 = FakeCameraControl()
        val adapterControl1 = AdapterCameraControl(fakeControl1, sessionProcessor1)
        val adapterControl2 = AdapterCameraControl(fakeControl2, sessionProcessor2)

        repeat(3) {
            val state1 = requireNotNull(adapterInfo1.zoomState.value)
            val state2 = requireNotNull(adapterInfo2.zoomState.value)
            assertThat(state1.minZoomRatio).isEqualTo(expectedRange.lower)
            assertThat(state1.maxZoomRatio).isEqualTo(expectedRange.upper)
            assertThat(state2.minZoomRatio).isEqualTo(expectedRange.lower)
            assertThat(state2.maxZoomRatio).isEqualTo(expectedRange.upper)
            adapterControl1.setZoomRatio(2.0f).get()
            adapterControl2.setZoomRatio(3.0f).get()
            adapterControl1.setLinearZoom(0.5f).get()
            adapterControl2.setLinearZoom(1.0f).get()
            assertThat(adapterInfo1.isVideoStabilizationSupported).isTrue()
            assertThat(adapterInfo2.isPreviewStabilizationSupported).isFalse()
        }

        assertThat(fakeControl1.zoomRatio)
            .isEqualTo(AdapterCameraInfo.getZoomRatioByPercentage(0.5f, 1.0f, 5.0f))
        assertThat(fakeControl2.zoomRatio)
            .isEqualTo(AdapterCameraInfo.getZoomRatioByPercentage(1.0f, 1.0f, 5.0f))
        assertThat(capabilities.queriedKeys)
            .containsExactly(
                CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE,
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES,
            )
        assertThat(capabilities.getAvailableCharacteristicsKeyValuesCallCount).isEqualTo(0)
    }

    @Test
    fun init_reinitializesAndReplacesCachedCharacteristics() {
        val firstRange = Range(1.0f, 4.0f)
        val firstModes = intArrayOf(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)
        val (extender, firstCapabilities, _) =
            createCountingExtender(
                characteristicsKeys =
                    mapOf(
                        CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE to firstRange,
                        CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to
                            firstModes,
                    )
            )

        assertThat(extender.extensionZoomRange).isEqualTo(firstRange)
        assertThat(extender.extensionAvailableStabilizationModes?.asList())
            .containsExactly(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)

        val secondRange = Range(0.5f, 10.0f)
        val secondModes =
            intArrayOf(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)
        val (_, secondCapabilities, secondCameraInfo) =
            createCountingExtender(
                characteristicsKeys =
                    mapOf(
                        CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE to secondRange,
                        CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to
                            secondModes,
                    )
            )
        extender.init(secondCameraInfo)

        assertThat(extender.extensionZoomRange).isEqualTo(secondRange)
        assertThat(extender.extensionZoomRange).isEqualTo(secondRange)
        assertThat(extender.extensionAvailableStabilizationModes?.asList())
            .containsExactly(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)
        assertThat(firstCapabilities.queriedKeys)
            .containsExactly(
                CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE,
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES,
            )
        assertThat(secondCapabilities.queriedKeys)
            .containsExactly(
                CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE,
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES,
            )
    }

    private fun createCountingExtender(
        availableCaptureRequestKeys: Set<Any> = emptySet(),
        characteristicsKeys: Map<Any, Any?> = emptyMap(),
        outputSizesFormat: Map<Int, Set<Size>> = emptyMap(),
        postviewSizes: Map<CaptureConfig, Set<Size>> = emptyMap(),
        onGet: ((Any) -> Unit)? = null,
    ): Triple<
        Camera2ExtensionsVendorExtender,
        CountingCameraExtensionCapabilities,
        FakeCameraInfoInternal,
    > {
        val capabilities =
            CountingCameraExtensionCapabilities(
                delegate =
                    FakeCameraExtensionCapabilities(
                        outputSizesFormat = outputSizesFormat,
                        postviewSizes = postviewSizes,
                        availableCaptureRequestKeys = availableCaptureRequestKeys,
                        characteristicsKeys = characteristicsKeys,
                    ),
                onGet = onGet,
            )
        val cameraInfo =
            FakeCameraInfoInternal("0").apply {
                setSupportedExtensions(setOf(CameraExtensionCharacteristics.EXTENSION_NIGHT))
                setCameraExtensionCapabilities(
                    CameraExtensionCharacteristics.EXTENSION_NIGHT,
                    capabilities,
                )
            }
        val extender =
            Camera2ExtensionsVendorExtender(ExtensionMode.NIGHT).apply { init(cameraInfo) }
        return Triple(extender, capabilities, cameraInfo)
    }

    private fun createInitializedExtender(
        outputSizesFormat: Map<Int, Set<Size>> = emptyMap(),
        postviewSizes: Map<CaptureConfig, Set<Size>> = emptyMap(),
    ): Camera2ExtensionsVendorExtender =
        createCountingExtender(
                outputSizesFormat = outputSizesFormat,
                postviewSizes = postviewSizes,
            )
            .first

    private class CountingCameraExtensionCapabilities(
        private val delegate: CameraExtensionCapabilities,
        private val onGet: ((Any) -> Unit)? = null,
    ) : CameraExtensionCapabilities by delegate {
        val queriedKeys = mutableListOf<Any>()
        var getAvailableCharacteristicsKeyValuesCallCount = 0
            private set

        override fun <T> get(key: Any): T? {
            queriedKeys.add(key)
            onGet?.invoke(key)
            return delegate.get(key)
        }

        override fun getAvailableCharacteristicsKeyValues(): List<Pair<Any, Any>> {
            getAvailableCharacteristicsKeyValuesCallCount++
            return delegate.getAvailableCharacteristicsKeyValues()
        }
    }
}
