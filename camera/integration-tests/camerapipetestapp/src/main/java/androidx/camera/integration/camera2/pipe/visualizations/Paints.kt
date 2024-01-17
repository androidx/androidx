/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.camera2.pipe.visualizations

import android.content.Context
import android.graphics.Paint
import android.text.TextPaint
import androidx.camera.integration.camera2.pipe.R
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

/** Uniform paints to be used in all graphs */
class Paints(private val context: Context) {

    val whiteFillPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.graphDataColor)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 1f
    }

    val missingDataPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.missingDataColor)
        style = Paint.Style.STROKE
        strokeWidth = ResourcesCompat.getFloat(context.resources, R.dimen.mediumStrokeWidth)
    }

    val latencyDataPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.latencyDataColor)
        style = Paint.Style.STROKE
        strokeWidth = ResourcesCompat.getFloat(context.resources, R.dimen.mediumStrokeWidth)
    }

    val graphDataPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.graphDataColor)
        style = Paint.Style.STROKE
        strokeWidth = ResourcesCompat.getFloat(context.resources, R.dimen.thickStrokeWidth)
    }

    val dividerLinePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.whiteTransparent)
        style = Paint.Style.STROKE
        strokeWidth = ResourcesCompat.getFloat(context.resources, R.dimen.dividerStrokeWidth)
    }

    val keyValueValuePaint = TextPaint().apply {
        color = ContextCompat.getColor(context, R.color.graphDataColor)
        textSize = ResourcesCompat.getFloat(context.resources, R.dimen.keyValueValueTextSize)
        textAlign = Paint.Align.RIGHT
    }
}
