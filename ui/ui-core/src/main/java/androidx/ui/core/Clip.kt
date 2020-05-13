/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.ui.graphics.Shape

/**
 * Clips the content to the bounds of the layer.
 */
@Deprecated(
    "Use Modifier.clipToBounds",
    replaceWith = ReplaceWith(
        "Modifier.clipToBounds()",
        "androidx.ui.core.Modifier",
        "androidx.ui.core.clipToBounds"
    )
)
val DrawClipToBounds: Modifier = Modifier.drawLayer(clipToBounds = true)

/**
 * Clip the content to the bounds of a layer defined at this modifier.
 */
@Suppress("DEPRECATION")
fun Modifier.clipToBounds() = this + DrawClipToBounds

/**
 * Clips the content to [shape].
 */
@Deprecated(
    "Use Modifier.clip",
    replaceWith = ReplaceWith(
        "Modifier.clip(shape)",
        "androidx.ui.core.Modifier",
        "androidx.ui.core.clip"
    )
)
fun drawClip(shape: Shape): Modifier =
    Modifier.drawLayer(clipToBounds = false, clipToOutline = true, outlineShape = shape)

/**
 * Clip the content to [shape].
 */
fun Modifier.clip(shape: Shape) =
    this + Modifier.drawLayer(clipToBounds = false, clipToOutline = true, outlineShape = shape)