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

@file:JvmName("WindowSizeClassFactory")

package androidx.window.layout.adapter

import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import androidx.window.layout.WindowMetrics

/** A convenience function for computing the [WindowSizeClass] from the [WindowMetrics] */
fun Set<WindowSizeClass>.computeWindowSizeClass(windowMetrics: WindowMetrics): WindowSizeClass {
    val density = windowMetrics.density
    val widthDp = (windowMetrics.bounds.width() * 160) / density
    val heightDp = (windowMetrics.bounds.height() * 160) / density
    return computeWindowSizeClass(widthDp, heightDp)
}
