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

import android.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

/** Get a [StrokeCap] from a [Paint.Cap] */
internal fun Paint.Cap.toStrokeCap() =
    when (this) {
        Paint.Cap.BUTT -> StrokeCap.Butt
        Paint.Cap.ROUND -> StrokeCap.Round
        Paint.Cap.SQUARE -> StrokeCap.Square
    }

/** Get a [PaintingStyle] from a [Paint.Style] */
internal fun Paint.Style.toPaintingStyle() =
    when (this) {
        Paint.Style.FILL -> PaintingStyle.Fill
        Paint.Style.STROKE -> PaintingStyle.Stroke
        Paint.Style.FILL_AND_STROKE ->
            // No equivalent in PaintingStyle, defaulting it to Fill
            PaintingStyle.Fill
    }

/** Get a [StrokeJoin] from a [Paint.Join] */
internal fun Paint.Join.toStrokeJoin() =
    when (this) {
        Paint.Join.MITER -> StrokeJoin.Miter
        Paint.Join.ROUND -> StrokeJoin.Round
        Paint.Join.BEVEL -> StrokeJoin.Bevel
    }
