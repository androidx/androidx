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

package androidx.camera.core.imagecapture

import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Fake [TakePictureCallback] for getting the results asynchronously.
 */
class FakeTakePictureCallback : TakePictureCallback {

    private lateinit var inMemoryResultCont: Continuation<ImageProxy>
    private lateinit var onDiskResultCont: Continuation<OutputFileResults>

    override fun onImageCaptured() {
        TODO("Not yet implemented")
    }

    override fun onFinalResult(outputFileResults: OutputFileResults) {
        onDiskResultCont.resume(outputFileResults)
    }

    override fun onFinalResult(imageProxy: ImageProxy) {
        inMemoryResultCont.resume(imageProxy)
    }

    override fun onCaptureFailure(imageCaptureException: ImageCaptureException) {
        TODO("Not yet implemented")
    }

    override fun onProcessFailure(imageCaptureException: ImageCaptureException) {
        TODO("Not yet implemented")
    }

    internal suspend fun getInMemoryResult() = suspendCoroutine { cont ->
        inMemoryResultCont = cont
    }

    internal suspend fun getOnDiskResult() = suspendCoroutine { cont ->
        onDiskResultCont = cont
    }
}