/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.graphics

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

/*
     The default implementation (used for all but web) based on `canvas.drawImage` call is slow for web:
     First call: 44ms; Second call: 14ms; Third call: 16ms; (for 256x256 PNG image)
     The profiler showed such calls tree (can be obtained using debug skiko):

     org_jetbrains_skia_Canvas__1nDrawImageRect - 33.21ms / 0ms
       - SkCodec::getPixels - 6.92ms / 0.13ms
       - SkDraw::drawBitmap - 22.29ms / 0.2ms
        - SkRasterPipeline::compile - 17.92 ms / 0ms
            - portable::start_pipeline - 17.92ms / 0 ms
                - portable::load_8888 ...

       The most top functions from a Bottom-Up view in Chrome profiler:
       Self time / Total time  ::: function name
       5.5ms 15.9% / 6.8ms 19.3% ::: portable::srcover_rgba_8888(...)
       2.6ms 7.5% / 14.1ms 40.5% ::: portable::clamp_01(...)
       2.5ms 7.2% / 17ms 48.7% ::: portable::load_8888(...)
       1.9ms 5.5% / 9.0ms 25.8% ::: portable::swap_rb(...)
       1.5ms 4.3% / 1.5ms 4.3% ::: portable::RGBA_to_rgbA_portable(...)
       1.2ms 3.5% / 1.3ms 3.8% ::: baseline::exec_ops(...)
       ___

       Therefore, we can use an alternative implementation - `Bitmap.makeFromImage`:
       First call: 9.92ms; Second call: 1.88ms; Third call: 1.63ms; (for 256x256 PNG image)
       We don't use it as a default, because it's slower for non-web targets.

       Note: The default implementation creates a Bitmap-backed canvas.
       Using an actual (on-screen) canvas is better on web,
       but it doesn't serve the purpose of this function.
 */
internal actual fun Image.toBitmap(): Bitmap {
    val bitmap = Bitmap.makeFromImage(this)
    bitmap.setImmutable()
    return bitmap
}