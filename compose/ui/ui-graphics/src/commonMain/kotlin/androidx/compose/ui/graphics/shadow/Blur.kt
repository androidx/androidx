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

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle

/**
 * Internal only access to BlurFilter. Because the behavior of BlurFilter changes across Android
 * versions, abstract this away such that higher level objects can provide better performance
 * guarantees by caching results across frames.
 */
internal expect fun BlurFilter(radius: Float): BlurFilter

internal expect class BlurFilter

internal expect fun Paint.setBlurFilter(blur: BlurFilter?)

/**
 * Helper method to overwrite the paint configuration for shadow rendering to ensure that previously
 * configured values are overwritten for the explicit usage
 */
internal fun Paint.configureShadow(
    color: Color = Color.Black,
    blendMode: BlendMode = BlendMode.SrcOver,
    blurFilter: BlurFilter? = null,
    style: PaintingStyle = PaintingStyle.Fill,
): Paint {
    this.color = color
    this.blendMode = blendMode
    this.style = style
    setBlurFilter(blurFilter)
    return this
}
