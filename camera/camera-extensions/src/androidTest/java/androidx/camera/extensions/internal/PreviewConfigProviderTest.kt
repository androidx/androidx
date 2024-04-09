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

package androidx.camera.extensions.internal

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.util.Pair
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.impl.PreviewExtenderImpl
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23) // BasicVendorExtender requires API level 23
class PreviewConfigProviderTest {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Tests in this class majorly use mock objects to run the test. No matter which extension
    // mode is use, it should not affect the test results.
    @ExtensionMode.Mode
    private val extensionMode = ExtensionMode.NONE
    private var cameraSelector = CameraSelector.Builder().build()
    private val fakeLifecycleOwner = FakeLifecycleOwner()

    private lateinit var cameraProvider: ProcessCameraProvider

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        fakeLifecycleOwner.startAndResume()
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    @MediumTest
    @Ignore("b/331617278")
    fun canSetSupportedResolutionsToConfigTest(): Unit = runBlocking {
        assumeTrue(CameraUtil.deviceHasCamera())

        // getSupportedResolutions supported since version 1.1
        val version = ExtensionVersion.getRuntimeVersion()
        assumeTrue(version != null && version >= Version.VERSION_1_1)

        val mockPreviewExtenderImpl = mock(PreviewExtenderImpl::class.java)

        Mockito.`when`(mockPreviewExtenderImpl.isExtensionAvailable(any(), any())).thenReturn(true)
        Mockito.`when`(mockPreviewExtenderImpl.processorType).thenReturn(
            PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_NONE
        )

        val targetFormatResolutionsPairList = generatePreviewSupportedResolutions()
        Mockito.`when`(mockPreviewExtenderImpl.supportedResolutions).thenReturn(
            targetFormatResolutionsPairList
        )

        val mockVendorExtender = BasicVendorExtender(null, mockPreviewExtenderImpl)

        val preview = createPreviewWithExtenderImpl(mockVendorExtender)

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, preview)
        }

        val resultFormatResolutionsPairList = (preview.currentConfig as ImageOutputConfig)
            .supportedResolutions

        // Checks the result and target pair lists are the same
        for (resultPair in resultFormatResolutionsPairList) {
            val firstTargetSizes = targetFormatResolutionsPairList.filter {
                it.first == resultPair.first
            }.map {
                it.second
            }.first()

            assertThat(mutableListOf(resultPair.second)).containsExactly(firstTargetSizes)
        }
    }

    private suspend fun createPreviewWithExtenderImpl(
        basicVendorExtender: BasicVendorExtender
    ): Preview {
        withContext(Dispatchers.Main) {
            val camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector)
            basicVendorExtender.init(camera.cameraInfo)
        }
        return Preview.Builder().also {
            PreviewConfigProvider(basicVendorExtender).apply {
                updateBuilderConfig(it, basicVendorExtender)
            }
        }.build()
    }

    private fun generatePreviewSupportedResolutions(): List<Pair<Int, Array<Size>>> {
        val formatResolutionsPairList = mutableListOf<Pair<Int, Array<Size>>>()
        val cameraInfo = cameraProvider.availableCameraInfos[0] as CameraInfoInternal
        val characteristics = cameraInfo.cameraCharacteristics as CameraCharacteristics
        val map = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]

        // Retrieves originally supported resolutions from CameraCharacteristics for PRIVATE
        // format to return.
        map?.getOutputSizes(ImageFormat.PRIVATE)?.let {
            formatResolutionsPairList.add(Pair.create(ImageFormat.PRIVATE, it))
        }

        return formatResolutionsPairList
    }
}
