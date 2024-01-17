/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.shapes.test

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.core.graphics.scaleMatrix
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.min

class ShapeView(context: Context, shape: RoundedPolygon) : View(context) {
    val paint = Paint()
    val shape = shape.normalized()
    val path = Path()

    init {
        paint.setColor(Color.WHITE)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val scale = min(w, h).toFloat()
        shape.toPath(path)
        path.transform(scaleMatrix(scale, scale))
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }
}
