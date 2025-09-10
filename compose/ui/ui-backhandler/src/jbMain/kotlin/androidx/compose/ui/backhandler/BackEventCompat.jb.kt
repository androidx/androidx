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

package androidx.compose.ui.backhandler

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.ui.ExperimentalComposeUiApi

@Deprecated("Use NavigationEvent instead")
@ExperimentalComposeUiApi
actual class BackEventCompat(
    actual val touchX: Float,
    actual val touchY: Float,
    @get:FloatRange(from = 0.0, to = 1.0)
    actual val progress: Float,
    @get:IntRange(from = 0, to = 1)
    actual val swipeEdge: Int
) {

    actual companion object {
        actual const val EDGE_LEFT: Int = 0
        actual const val EDGE_RIGHT: Int = 1
    }
}