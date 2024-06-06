/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import androidx.camera.core.CameraInfo
import androidx.camera.extensions.impl.PreviewExtenderImpl
import androidx.camera.extensions.internal.fake.FakeCaptureProcessorImpl
import androidx.camera.extensions.internal.fake.FakeImageCaptureExtenderImpl
import androidx.camera.extensions.internal.fake.FakePreviewExtenderImpl
import androidx.camera.extensions.internal.util.ExtensionsTestUtil
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class BasicVendorExtenderTest {
    val yuvSize = Size(1, 1)
    val jpegSize = Size(2, 2)
    val privateSize = Size(3, 3)
    val streamConfigurationPrivateSize = Size(4, 4)
    val streamConfigurationJpegSize = Size(5, 5)
    val streamConfigurationYuvSize = Size(6, 6)

    @Before
    fun setUp() {
        ExtensionsTestUtil.resetSingleton(ExtensionVersion::class.java, "sExtensionVersion")
    }

    @Test
    fun captureOutputResolutions_returnYuvSizesFromOem_WhenCaptureProcessorSupported() {
        val fakeImageCaptureExtenderImpl =
            FakeImageCaptureExtenderImpl(
                supportedSizes =
                    listOf(
                        android.util.Pair(ImageFormat.YUV_420_888, arrayOf(yuvSize)),
                        android.util.Pair(ImageFormat.JPEG, arrayOf(jpegSize))
                    ),
                captureProcessorImpl = FakeCaptureProcessorImpl()
            )
        ExtensionsTestUtil.setTestApiVersion("1.1.0")
        val basicVendorExtender =
            BasicVendorExtender(fakeImageCaptureExtenderImpl, FakePreviewExtenderImpl())
        basicVendorExtender.init(getCameraInfoWithStreamConfigurations("0"))
        val supportedSizes = basicVendorExtender.supportedCaptureOutputResolutions
        assertContainsOnlySizes(supportedSizes, ImageFormat.YUV_420_888, arrayOf(yuvSize))
    }

    @Test
    fun captureOutputResolutions_replaceJpegSizesAsYuvFromOem_WhenCaptureProcessorSupported() {
        val fakeImageCaptureExtenderImpl =
            FakeImageCaptureExtenderImpl(
                supportedSizes = listOf(android.util.Pair(ImageFormat.JPEG, arrayOf(jpegSize))),
                captureProcessorImpl = FakeCaptureProcessorImpl()
            )
        ExtensionsTestUtil.setTestApiVersion("1.1.0")
        val basicVendorExtender =
            BasicVendorExtender(fakeImageCaptureExtenderImpl, FakePreviewExtenderImpl())
        basicVendorExtender.init(getCameraInfoWithStreamConfigurations("0"))
        val supportedSizes = basicVendorExtender.supportedCaptureOutputResolutions
        assertContainsOnlySizes(supportedSizes, ImageFormat.YUV_420_888, arrayOf(jpegSize))
    }

    @Test
    fun captureOutputResolutions_returnYuvSizesFromStreamConfiguration_priorTo1_1_0() {
        val fakeImageCaptureExtenderImpl =
            FakeImageCaptureExtenderImpl(
                supportedSizes =
                    listOf(
                        android.util.Pair(ImageFormat.YUV_420_888, arrayOf(yuvSize)),
                        android.util.Pair(ImageFormat.JPEG, arrayOf(jpegSize))
                    ),
                captureProcessorImpl = FakeCaptureProcessorImpl()
            )
        ExtensionsTestUtil.setTestApiVersion("1.0.0")
        val basicVendorExtender =
            BasicVendorExtender(fakeImageCaptureExtenderImpl, FakePreviewExtenderImpl())
        basicVendorExtender.init(getCameraInfoWithStreamConfigurations("0"))
        val supportedSizes = basicVendorExtender.supportedCaptureOutputResolutions
        assertContainsOnlySizes(
            supportedSizes,
            ImageFormat.YUV_420_888,
            arrayOf(streamConfigurationYuvSize)
        )
    }

    @Test
    fun captureOutputResolutions_returnAllSizesFromOEMs_whenNoCaptureProcessor() {
        val fakeImageCaptureExtenderImpl =
            FakeImageCaptureExtenderImpl(
                supportedSizes =
                    listOf(
                        android.util.Pair(ImageFormat.YUV_420_888, arrayOf(yuvSize)),
                        android.util.Pair(ImageFormat.JPEG, arrayOf(jpegSize))
                    ),
            )
        ExtensionsTestUtil.setTestApiVersion("1.1.0")
        val basicVendorExtender =
            BasicVendorExtender(fakeImageCaptureExtenderImpl, FakePreviewExtenderImpl())
        basicVendorExtender.init(getCameraInfoWithStreamConfigurations("0"))
        val supportedSizes = basicVendorExtender.supportedCaptureOutputResolutions
        assertThat(supportedSizes).isEqualTo(fakeImageCaptureExtenderImpl.supportedResolutions)
    }

    @Test
    fun captureOutputResolutions_returnYuvSizesFromStreamConfiguration_whenOemReturnsNull() {
        val fakeImageCaptureExtenderImpl =
            FakeImageCaptureExtenderImpl(
                supportedSizes = null,
                captureProcessorImpl = FakeCaptureProcessorImpl()
            )
        ExtensionsTestUtil.setTestApiVersion("1.1.0")
        val basicVendorExtender =
            BasicVendorExtender(fakeImageCaptureExtenderImpl, FakePreviewExtenderImpl())
        basicVendorExtender.init(getCameraInfoWithStreamConfigurations("0"))
        val supportedSizes = basicVendorExtender.supportedCaptureOutputResolutions
        assertContainsOnlySizes(
            supportedSizes,
            ImageFormat.YUV_420_888,
            arrayOf(streamConfigurationYuvSize)
        )
    }

    @Test
    fun captureOutputResolutions_returnJpegSizesFromStreamConfiguration_whenOemReturnsNull() {
        val fakeImageCaptureExtenderImpl =
            FakeImageCaptureExtenderImpl(
                supportedSizes = null,
            )
        ExtensionsTestUtil.setTestApiVersion("1.1.0")
        val basicVendorExtender =
            BasicVendorExtender(fakeImageCaptureExtenderImpl, FakePreviewExtenderImpl())
        basicVendorExtender.init(getCameraInfoWithStreamConfigurations("0"))
        val supportedSizes = basicVendorExtender.supportedCaptureOutputResolutions
        assertContainsOnlySizes(
            supportedSizes,
            ImageFormat.JPEG,
            arrayOf(streamConfigurationJpegSize)
        )
    }

    @Test
    fun previewOutputResolutions_returnPrivSizesFromOem() {
        val fakePreviewExtenderImpl =
            FakePreviewExtenderImpl(
                supportedSizes =
                    listOf(
                        android.util.Pair(ImageFormat.YUV_420_888, arrayOf(yuvSize)),
                        android.util.Pair(ImageFormat.PRIVATE, arrayOf(privateSize))
                    ),
            )
        ExtensionsTestUtil.setTestApiVersion("1.1.0")
        val basicVendorExtender =
            BasicVendorExtender(FakeImageCaptureExtenderImpl(), fakePreviewExtenderImpl)
        basicVendorExtender.init(getCameraInfoWithStreamConfigurations("0"))
        val supportedSizes = basicVendorExtender.supportedPreviewOutputResolutions
        assertContainsOnlySizes(supportedSizes, ImageFormat.PRIVATE, arrayOf(privateSize))
    }

    @Test
    fun previewOutputResolutions_replaceYuvAsPrivSizesFromOem() {
        val fakePreviewExtenderImpl =
            FakePreviewExtenderImpl(
                supportedSizes =
                    listOf(android.util.Pair(ImageFormat.YUV_420_888, arrayOf(yuvSize)))
            )
        ExtensionsTestUtil.setTestApiVersion("1.1.0")
        val basicVendorExtender =
            BasicVendorExtender(FakeImageCaptureExtenderImpl(), fakePreviewExtenderImpl)
        basicVendorExtender.init(getCameraInfoWithStreamConfigurations("0"))
        val supportedSizes = basicVendorExtender.supportedPreviewOutputResolutions
        assertContainsOnlySizes(supportedSizes, ImageFormat.PRIVATE, arrayOf(yuvSize))
    }

    @Test
    fun previewOutputResolutions_returnPrivSizesFromStreamConfiguration_whenOemReturnsNull() {
        val fakePreviewExtenderImpl = FakePreviewExtenderImpl(supportedSizes = null)
        ExtensionsTestUtil.setTestApiVersion("1.1.0")
        val basicVendorExtender =
            BasicVendorExtender(FakeImageCaptureExtenderImpl(), fakePreviewExtenderImpl)
        basicVendorExtender.init(getCameraInfoWithStreamConfigurations("0"))
        val supportedSizes = basicVendorExtender.supportedPreviewOutputResolutions
        assertContainsOnlySizes(
            supportedSizes,
            ImageFormat.PRIVATE,
            arrayOf(streamConfigurationPrivateSize)
        )
    }

    @Test
    fun previewOutputResolutions_returnYuvSizesFromStreamConfiguration_whenOemReturnsNull() {
        val fakePreviewExtenderImpl =
            FakePreviewExtenderImpl(
                supportedSizes = null,
                processorType = PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR
            )
        ExtensionsTestUtil.setTestApiVersion("1.1.0")
        val basicVendorExtender =
            BasicVendorExtender(FakeImageCaptureExtenderImpl(), fakePreviewExtenderImpl)
        basicVendorExtender.init(getCameraInfoWithStreamConfigurations("0"))
        val supportedSizes = basicVendorExtender.supportedPreviewOutputResolutions
        assertContainsOnlySizes(
            supportedSizes,
            ImageFormat.PRIVATE,
            arrayOf(streamConfigurationYuvSize)
        )
    }

    private fun getCameraInfoWithStreamConfigurations(cameraId: String): CameraInfo {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)

        val mockMap: StreamConfigurationMap = mock(StreamConfigurationMap::class.java)
        `when`<Array<Size>>(mockMap.getOutputSizes(ImageFormat.YUV_420_888))
            .thenReturn(arrayOf(streamConfigurationYuvSize))
        `when`<Array<Size>>(mockMap.getOutputSizes(ImageFormat.JPEG))
            .thenReturn(arrayOf(streamConfigurationJpegSize))
        `when`<Array<Size>>(mockMap.getOutputSizes(ImageFormat.PRIVATE))
            .thenReturn(arrayOf(streamConfigurationPrivateSize))

        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, mockMap)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Shadow.extract<ShadowCameraManager>(cameraManager).addCamera(cameraId, characteristics)
        return FakeCameraInfoInternal(cameraId, context)
    }

    private fun assertContainsOnlySizes(
        list: List<android.util.Pair<Int, Array<Size>>>,
        format: Int,
        sizes: Array<Size>
    ) {
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0].first).isEqualTo(format)
        assertThat(list[0].second).isEqualTo(sizes)
    }
}
