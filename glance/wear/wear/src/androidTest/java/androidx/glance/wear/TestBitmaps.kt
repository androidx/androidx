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

package androidx.glance.wear

import android.graphics.Bitmap
import kotlin.random.Random

/**
 * Creates an opaque bitmap filled with random noise.
 *
 * Random noise doesn't compress, so the resulting bitmap is embedded in a Remote Compose document
 * at roughly `width * height * 3` bytes.
 */
internal fun createNoiseBitmap(width: Int, height: Int, seed: Int = 0): Bitmap {
    val random = Random(seed)
    val pixels = IntArray(width * height) { random.nextInt() or 0xFF000000.toInt() }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}
