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

package androidx.camera.testing.impl.fakes

import androidx.annotation.VisibleForTesting
import androidx.camera.core.imagecapture.ImagePipeline
import androidx.camera.core.imagecapture.RequestWithCallback
import androidx.camera.core.imagecapture.TakePictureManager
import androidx.camera.core.imagecapture.TakePictureRequest

private const val TAG = "FakeTakePictureManager"

internal class FakeTakePictureManagerImpl : TakePictureManager {
    override fun setImagePipeline(imagePipeline: ImagePipeline) {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun offerRequest(takePictureRequest: TakePictureRequest) {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun pause() {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun resume() {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun abortRequests() {
        throw UnsupportedOperationException("Not implemented yet")
    }

    @VisibleForTesting
    override fun hasCapturingRequest(): Boolean {
        throw UnsupportedOperationException("Not implemented yet")
    }

    @VisibleForTesting
    override fun getCapturingRequest(): RequestWithCallback? {
        throw UnsupportedOperationException("Not implemented yet")
    }

    @VisibleForTesting
    override fun getIncompleteRequests(): List<RequestWithCallback> {
        throw UnsupportedOperationException("Not implemented yet")
    }

    @VisibleForTesting
    override fun getImagePipeline(): ImagePipeline {
        throw UnsupportedOperationException("Not implemented yet")
    }
}
