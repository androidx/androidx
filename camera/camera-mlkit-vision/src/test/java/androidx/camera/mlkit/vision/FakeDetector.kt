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

package androidx.camera.mlkit.vision

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import androidx.annotation.VisibleForTesting
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.interfaces.Detector
import java.nio.ByteBuffer

/**
 * Fake [Detector] with [String] as the result type.
 */
class FakeDetector(
    private val result: String?,
    private val detectorType: Int,
) : Detector<String> {

    @VisibleForTesting
    var latestMatrix: Matrix? = null
    @VisibleForTesting
    var latestRotationDegrees: Int = -1

    private var closed = false
    var taskException: Exception? = null
    var taskCanceled: Boolean = false

    override fun process(image: Image, rotationDegrees: Int, matrix: Matrix): Task<String> {
        if (closed) {
            throw Exception("Closed")
        }
        latestMatrix = matrix
        latestRotationDegrees = rotationDegrees
        return FakeTask(result, taskException, taskCanceled)
    }

    override fun getDetectorType(): Int {
        return detectorType
    }

    override fun close() {
        closed = true
    }

    override fun process(p0: Bitmap, p1: Int): Task<String> {
        TODO("Not yet implemented")
    }

    override fun process(p0: Image, p1: Int): Task<String> {
        TODO("Not yet implemented")
    }

    override fun process(p0: ByteBuffer, p1: Int, p2: Int, p3: Int, p4: Int): Task<String> {
        TODO("Not yet implemented")
    }
}
