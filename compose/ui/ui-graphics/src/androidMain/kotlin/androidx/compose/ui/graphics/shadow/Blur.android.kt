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

package androidx.compose.ui.graphics.shadow

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint

internal actual fun BlurFilter(radius: Float): BlurFilter =
    BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)

internal actual typealias BlurFilter = BlurMaskFilter

internal actual fun Paint.setBlurFilter(blur: BlurFilter?) {
    asFrameworkPaint().setMaskFilter(blur)
}
