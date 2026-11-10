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

@Suppress("RestrictedApiAndroidX")
public fun RcPaint.setStyle(style: Paint.Style) {
    when (style) {
        Paint.Style.FILL -> this.setStyle(0)
        Paint.Style.FILL_AND_STROKE -> this.setStyle(2)
        Paint.Style.STROKE -> this.setStyle(1)
    }
}

@Suppress("RestrictedApiAndroidX")
public fun RcPaint.setStrokeCap(cap: Paint.Cap) {

    when (cap) {
        Paint.Cap.SQUARE -> this.setStrokeCap(2)
        Paint.Cap.BUTT -> this.setStrokeCap(0)
        Paint.Cap.ROUND -> this.setStrokeCap(1)
    }
}

@Suppress("RestrictedApiAndroidX")
public fun RcPaint.setStrokeJoin(join: Paint.Join) {

    when (join) {
        Paint.Join.ROUND -> this.setStrokeJoin(1)
        Paint.Join.BEVEL -> this.setStrokeJoin(2)
        Paint.Join.MITER -> this.setStrokeJoin(0)
    }
}
