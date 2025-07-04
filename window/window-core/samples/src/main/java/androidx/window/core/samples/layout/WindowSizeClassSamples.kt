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

package androidx.window.core.samples.layout

import androidx.annotation.Sampled
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.computeWindowSizeClass

@Sampled
fun calculateWindowSizeClass(widthDp: Int, heightDp: Int): WindowSizeClass {
    return WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(widthDp, heightDp)
}

@Sampled
fun processWindowSizeClassWidthOnly(sizeClass: WindowSizeClass): String {
    return when {
        sizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            // return UI for EXPANDED width size class
            FakeUiValues.EXPANDED
        }
        sizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            // return UI for MEDIUM width size class
            FakeUiValues.MEDIUM
        }
        else -> {
            // return UI for COMPACT width size class
            FakeUiValues.COMPACT
        }
    }
}

object FakeUiValues {
    val COMPACT = "COMPACT"
    val MEDIUM = "MEDIUM"
    val EXPANDED = "EXPANDED"
}
