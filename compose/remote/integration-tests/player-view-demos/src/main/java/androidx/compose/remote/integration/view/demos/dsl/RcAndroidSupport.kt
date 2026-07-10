/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.dsl

import android.graphics.*
import androidx.compose.remote.creation.RcPaint
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcStrokeJoin
import androidx.compose.remote.creation.dsl.setStrokeCap
import androidx.compose.remote.creation.dsl.setStrokeJoin
import androidx.compose.remote.creation.dsl.setStyle

@Suppress("RestrictedApiAndroidX")
public fun RcPaint.setStyle(style: Paint.Style) {
    when (style) {
        Paint.Style.FILL -> this.setStyle(RcPaintStyle.Fill)
        Paint.Style.FILL_AND_STROKE -> this.setStyle(RcPaintStyle.FillAndStroke)
        Paint.Style.STROKE -> this.setStyle(RcPaintStyle.Stroke)
    }
}

@Suppress("RestrictedApiAndroidX")
public fun RcPaint.setStrokeCap(cap: Paint.Cap) {

    when (cap) {
        Paint.Cap.SQUARE -> this.setStrokeCap(RcStrokeCap.Square)
        Paint.Cap.BUTT -> this.setStrokeCap(RcStrokeCap.Butt)
        Paint.Cap.ROUND -> this.setStrokeCap(RcStrokeCap.Round)
    }
}

@Suppress("RestrictedApiAndroidX")
public fun RcPaint.setStrokeJoin(join: Paint.Join) {

    when (join) {
        Paint.Join.ROUND -> this.setStrokeJoin(RcStrokeJoin.Round)
        Paint.Join.BEVEL -> this.setStrokeJoin(RcStrokeJoin.Bevel)
        Paint.Join.MITER -> this.setStrokeJoin(RcStrokeJoin.Miter)
    }
}
