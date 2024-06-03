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

package androidx.window.layout.util

import android.graphics.Point
import android.view.Display

internal object DisplayHelper {

    /**
     * Returns the full (real) size of the display, in pixels, without subtracting any window decor
     * or applying any compatibility scale factors.
     *
     * The size is adjusted based on the current rotation of the display.
     *
     * @return a point representing the real display size in pixels.
     * @see Display.getRealSize
     */
    @Suppress("DEPRECATION")
    fun getRealSizeForDisplay(display: Display): Point {
        val size = Point()
        display.getRealSize(size)
        return size
    }
}
