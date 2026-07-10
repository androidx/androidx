/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.util

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.SparseArray
import android.view.WindowManager

/**
 * Compares two [SparseArray] instances for structural equality.
 *
 * This is a dependency-free, backward-compatible implementation of `SparseArray.contentEquals`,
 * which is only available on newer SDK versions. It ensures both arrays have the same keys in the
 * same order and that their corresponding values are equal according to `==`.
 *
 * This utility is useful when working with SparseArrays in environments where
 * `SparseArray.contentEquals` is not available (pre-API 24).
 *
 * @param other The [SparseArray] to compare with.
 * @return `true` if both arrays contain the same keys and associated values, `false` otherwise.
 */
internal fun <T> SparseArray<T>.compatContentEquals(other: SparseArray<T>): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return this.contentEquals(other)

    if (this.size() != other.size()) {
        return false
    }
    for (i in 0 until this.size()) {
        val key = this.keyAt(i)
        val value = this.valueAt(i)

        if (other.indexOfKey(key) < 0 || value != other.get(key)) {
            return false
        }
    }

    return true
}

/**
 * Returns the display size in pixels for the given context.
 *
 * This method uses the default display from the provided [Context]'s [WindowManager] and populates
 * the provided [Point] with the real size of the screen (width and height in pixels).
 *
 * Useful when calculating screen-dependent UI behavior or layout logic, especially in contexts
 * where you cannot access activity or fragment directly.
 *
 * @param context The [Context] used to access system services.
 * @param Point The [Point] object that will be updated with the display width and height.
 */
@Suppress("deprecation")
internal fun getDisplaySize(context: Context): Point {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val display = context.display
        Point(display.width, display.height)
    } else {
        val display =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size = Point()
        display.getSize(size)
        size
    }
}
