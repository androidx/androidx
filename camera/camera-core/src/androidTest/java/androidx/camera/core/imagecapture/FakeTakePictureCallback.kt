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

    private var inMemoryResult: ImageProxy? = null
    private var inMemoryResultCont: Continuation<ImageProxy>? = null
    private var onDiskResult: OutputFileResults? = null
    private var onDiskResultCont: Continuation<OutputFileResults>? = null

    override fun onImageCaptured() {
        TODO("onImageCaptured Not yet implemented")
    }

    override fun onFinalResult(outputFileResults: OutputFileResults) {
        val cont = onDiskResultCont
        if (cont != null) {
            cont.resume(outputFileResults)
            onDiskResultCont = null
        } else {
            onDiskResult = outputFileResults
        }
    }

    override fun onFinalResult(imageProxy: ImageProxy) {
        val cont = inMemoryResultCont
        if (cont != null) {
            cont.resume(imageProxy)
            inMemoryResultCont = null
        } else {
            inMemoryResult = imageProxy
        }
    }

    override fun onCaptureFailure(imageCaptureException: ImageCaptureException) {
        TODO("Not yet implemented")
    }

    override fun onProcessFailure(imageCaptureException: ImageCaptureException) {
        TODO("Not yet implemented")
    }

    override fun isAborted(): Boolean {
        return false
    }

    internal suspend fun getInMemoryResult() = suspendCoroutine { cont ->
        if (inMemoryResult != null) {
            cont.resume(inMemoryResult!!)
        } else {
            inMemoryResultCont = cont
        }
    }

    internal suspend fun getOnDiskResult() = suspendCoroutine { cont ->
        if (onDiskResult != null) {
            cont.resume(onDiskResult!!)
        } else {
            onDiskResultCont = cont
        }
    }
}
