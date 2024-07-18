/*
 * Copyright 2023 The Android Open Source Project
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

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import kotlin.math.max
import kotlin.math.min

class MorphView(context: Context, morph: Morph) : View(context) {
    val paint = Paint()
    val path = Path()
    private var pathBounds = RectF()
    val overshooter = OvershootInterpolator()

    var morph = morph
        set(value) {
            field = value
            val animator = ObjectAnimator.ofFloat(this, "progress", 0f, 1f)
            animator.duration = 500
            animator.interpolator = overshooter
            animator.start()
        }

    var progress: Float = 0f
        set(value) {
            field = value
            setupPath(value)
            invalidate()
        }

    init {
        paint.setColor(Color.WHITE)
    }

    private fun setupPath(progress: Float) {
        morph.toPath(progress, path)
        path.computeBounds(pathBounds, false)
        val viewportSize = min(width, height).toFloat()
        val pathSize = max(pathBounds.width(), pathBounds.height())
        val scaleFactor = viewportSize / pathSize
        val pathCenterX = pathBounds.left + pathBounds.width() / 2
        val pathCenterY = pathBounds.top + pathBounds.height() / 2
        val matrix = Matrix()
        matrix.setScale(scaleFactor, scaleFactor)
        matrix.preTranslate(-pathCenterX, -pathCenterY)
        matrix.postTranslate(width / 2f, height / 2f)
        path.transform(matrix)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        setupPath(progress)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }
}
