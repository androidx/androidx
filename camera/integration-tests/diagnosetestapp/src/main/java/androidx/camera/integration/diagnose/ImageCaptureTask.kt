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

package androidx.camera.integration.diagnose

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.utils.Threads
import androidx.camera.view.CameraController.IMAGE_CAPTURE
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Diagnosis task that utilizes ImageCapture use case
 * TODO: unit tests for this task (have only tested in end-to-end)
 */
class ImageCaptureTask : DiagnosisTask("ImageCaptureTask") {

    /**
     * Collects image captured as JPEG in diagnosis report zip
     */
    @Override
    override suspend fun runDiagnosisTask(
        cameraController: LifecycleCameraController,
        dataStore: DataStore,
        context: Context
    ) {
        // write file/section header
        dataStore.appendTitle(this.getTaskName())

        try {
            withContext(ContextCompat.getMainExecutor(context).asCoroutineDispatcher()) {
                captureImage(cameraController, dataStore, context)
            }?.let {
                dataStore.flushTempFileToImageFile(it, "ImageCaptureTask")
            }
        } catch (exception: ImageCaptureException) {
            Log.d("ImageCaptureTask", "Failed to run ImageCaptureTask: ${exception.message}")
        }
    }

    /**
     * Runs ImageCapture use case and return image captured
     */
    @MainThread
    suspend fun captureImage(
        cameraController: LifecycleCameraController,
        dataStore: DataStore,
        context: Context
    ): File? = suspendCancellableCoroutine { continuation ->
        Threads.checkMainThread()

        // enable ImageCapture use case
        cameraController.setEnabledUseCases(IMAGE_CAPTURE)
        dataStore.appendText("image capture enabled: " +
            "${cameraController.isImageCaptureEnabled}")

        val file = File(context.cacheDir, "temp.jpeg")
        val outputOption = ImageCapture.OutputFileOptions.Builder(file).build()
        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraController.takePicture(outputOption, mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("ImageCaptureTask", "${outputFileResults.savedUri}")
                    continuation.resume(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            })
    }
}
