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

package androidx.camera.integration.core

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.LabTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.mlkit.vision.barcode.Barcode.FORMAT_QR_CODE
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// The integration-tests for MLKit vision barcode component with CameraX ImageAnalysis use case.
@LargeTest
@RunWith(Parameterized::class)
class MLKitBarcodeTest(
    private val resolution: Size,
    private val cameraConfig: CameraXConfig
) {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    companion object {
        private const val DETECT_TIMEOUT = 5_000L
        private const val TAG = "MLKitVisionTest"
        private val size480p = Size(640, 480)
        private val size720p = Size(1280, 720)
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(size480p, Camera2Config.defaultConfig()),
            arrayOf(size720p, Camera2Config.defaultConfig()),
            arrayOf(size480p, CameraPipeConfig.defaultConfig()),
            arrayOf(size720p, CameraPipeConfig.defaultConfig())
        )
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var camera: CameraUseCaseAdapter
    // For MK Kit Barcode scanner
    private lateinit var barcodeScanner: BarcodeScanner

    @Before
    fun setup() {
        CameraX.initialize(context, cameraConfig).get(10, TimeUnit.SECONDS)

        barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(FORMAT_QR_CODE).build()
        )
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::camera.isInitialized) {
            // TODO: The removeUseCases() call might be removed after clarifying the
            // abortCaptures() issue in b/162314023
            withContext(Dispatchers.Main) {
                camera.removeUseCases(camera.useCases)
            }
        }
        CameraX.shutdown().get(10, TimeUnit.SECONDS)

        if (::barcodeScanner.isInitialized) {
            barcodeScanner.close()
        }
    }

    @LabTestRule.LabTestFrontCamera
    @Test
    fun barcodeDetectViaFontCamera() {
        val imageAnalysis = initImageAnalysis()

        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            imageAnalysis
        )
        assertBarcodeDetect(imageAnalysis)
    }

    @LabTestRule.LabTestRearCamera
    @Test
    fun barcodeDetectViaRearCamera() {
        val imageAnalysis = initImageAnalysis()

        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.DEFAULT_BACK_CAMERA,
            imageAnalysis
        )
        assertBarcodeDetect(imageAnalysis)
    }

    private fun assertBarcodeDetect(imageAnalysis: ImageAnalysis) {
        val latchForBarcodeDetect = CountDownLatch(4)

        imageAnalysis.setAnalyzer(
            Dispatchers.Main.asExecutor(),
            { imageProxy ->
                barcodeScanner.process(
                    InputImage.fromMediaImage(
                        imageProxy.image!!,
                        imageProxy.imageInfo.rotationDegrees
                    )
                )
                    .addOnSuccessListener { barcodes ->
                        barcodes.forEach {
                            if ("Hi, CamX!" == it.displayValue) {
                                latchForBarcodeDetect.countDown()
                                Log.d(TAG, "barcode display value: {${it.displayValue}} ")
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "processImage onFailure: $exception")
                    }
                    // When the image is from CameraX analysis use case, must call image.close() on
                    // received images when finished using them. Otherwise, new images may not be
                    // received or the camera may stall.
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        )

        // Verify it is the CameraX lab test environment and can detect qr-code.
        assertTrue(latchForBarcodeDetect.await(DETECT_TIMEOUT, TimeUnit.MILLISECONDS))
    }

    private fun initImageAnalysis(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setTargetName("ImageAnalysis")
            .setTargetResolution(resolution)
            .build()
    }
}
