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
@file:JvmName("WindowMetricsTesting")

package androidx.window.testing.layout

import android.graphics.Rect
import androidx.window.layout.WindowMetrics

/**
 * Returns a [WindowMetrics] with default values for testing.
 *
 * @param bounds The window bounds This value cannot be null.
 * @param density The window density.
 * @return [WindowMetrics] object
 */
@Suppress("FunctionName")
@JvmOverloads
@JvmName("createWindowMetrics")
fun TestWindowMetrics(bounds: Rect, density: Float = 1f): WindowMetrics {
    return WindowMetrics(bounds, density = density)
}

/**
 * Returns a [WindowMetrics] with default values for testing.
 *
 * @param left The X coordinate of the left side of window bounds.
 * @param top The Y coordinate of the top side of window bounds.
 * @param right The X coordinate of the right side of window bounds.
 * @param bottom The Y coordinate of the bottom side of window bounds.
 * @param density The window density.
 * @return [WindowMetrics] object
 */
@Suppress("FunctionName")
@JvmOverloads
@JvmName("createWindowMetrics")
fun TestWindowMetrics(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    density: Float = 1f
): WindowMetrics {
    return WindowMetrics(Rect(left, top, right, bottom), density = density)
}
