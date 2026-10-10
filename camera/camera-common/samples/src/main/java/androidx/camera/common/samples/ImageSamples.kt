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

package androidx.camera.common.samples

import android.graphics.Rect
import android.media.Image
import androidx.annotation.Sampled
import androidx.camera.common.ImagePlane
import androidx.camera.common.ImageWrappers
import androidx.camera.common.MutableImageWrapper

@Sampled
fun wrapImageSample(image: Image) {
    // Wrap a native android.media.Image into a MutableImageWrapper.
    val imageWrapper: MutableImageWrapper = ImageWrappers.wrap(image)

    // Inspect image metadata:
    val width: Int = imageWrapper.width
    val height: Int = imageWrapper.height
    val format: Int = imageWrapper.format
    val timestamp: Long = imageWrapper.timestamp
    val planes: List<ImagePlane> = imageWrapper.imagePlanes

    // Optionally update mutable properties such as cropRect:
    imageWrapper.cropRect = Rect(0, 0, width / 2, height / 2)

    // Always close the wrapper to release native resources:
    imageWrapper.close()
}
