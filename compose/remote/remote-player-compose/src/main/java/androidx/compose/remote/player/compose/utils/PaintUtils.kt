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
package androidx.compose.remote.player.compose.utils

import androidx.compose.ui.graphics.Paint

/**
 * Make a copy of [Paint]. This is not a deep copy, [Paint.shader], [Paint.colorFilter] and
 * [Paint.pathEffect] hold the same reference as the original.
 */
internal fun Paint.copy(): Paint =
    Paint().also { copy ->
        copy.alpha = this.alpha
        copy.isAntiAlias = this.isAntiAlias
        copy.color = this.color
        copy.blendMode = this.blendMode
        copy.style = this.style
        copy.strokeWidth = this.strokeWidth
        copy.strokeCap = this.strokeCap
        copy.strokeJoin = this.strokeJoin
        copy.strokeMiterLimit = this.strokeMiterLimit
        copy.filterQuality = this.filterQuality

        // same references:
        copy.shader = this.shader
        copy.colorFilter = this.colorFilter
        copy.pathEffect = this.pathEffect
    }
