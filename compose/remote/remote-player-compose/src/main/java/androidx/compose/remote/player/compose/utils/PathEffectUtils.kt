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

import android.graphics.PathDashPathEffect
import androidx.compose.ui.graphics.StampedPathEffectStyle
import androidx.compose.ui.graphics.StampedPathEffectStyle.Companion.Morph
import androidx.compose.ui.graphics.StampedPathEffectStyle.Companion.Rotate
import androidx.compose.ui.graphics.StampedPathEffectStyle.Companion.Translate

/** Get a [StampedPathEffectStyle] from a [PathDashPathEffect.Style]. */
internal fun PathDashPathEffect.Style.toStampedPathEffectStyle(): StampedPathEffectStyle =
    when (this) {
        PathDashPathEffect.Style.TRANSLATE -> Translate
        PathDashPathEffect.Style.ROTATE -> Rotate
        PathDashPathEffect.Style.MORPH -> Morph
    }
