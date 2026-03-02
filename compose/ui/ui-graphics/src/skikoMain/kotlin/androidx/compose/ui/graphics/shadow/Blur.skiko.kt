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

package androidx.compose.ui.graphics.shadow

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.BlurEffect.Companion.convertRadiusToSigma
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaPaint
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

@OptIn(InternalComposeUiApi::class)
internal actual fun BlurFilter(radius: Float): BlurFilter {
    val sigma = convertRadiusToSigma(radius);
    return MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma)
}

// TODO: Do not expose skiko types to common
//  https://youtrack.jetbrains.com/issue/CMP-219
internal actual typealias BlurFilter = MaskFilter // Only the base type is available

internal actual fun Paint.setBlurFilter(blur: BlurFilter?) {
    skiaPaint.maskFilter = blur
}
