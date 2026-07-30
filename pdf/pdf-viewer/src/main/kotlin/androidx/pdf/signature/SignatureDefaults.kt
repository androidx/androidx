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

package androidx.pdf.signature

import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint

internal object SignatureDefaults {

    /** Default paint used for free-hand drawn vector paths. */
    val defaultVectorPaint: Paint
        get() =
            Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

    /** Default paint used for typed typography signatures. */
    val defaultTextPaint: TextPaint
        get() =
            TextPaint().apply {
                color = Color.BLACK
                isAntiAlias = true
            }
}
