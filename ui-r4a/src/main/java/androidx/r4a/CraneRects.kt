/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.r4a

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.ui.core.Constraints
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Dimension
import androidx.ui.core.Draw
import androidx.ui.core.MeasureBox
import androidx.ui.core.PixelSize
import androidx.ui.core.coerceAtLeast
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.core.min
import androidx.ui.core.minus
import androidx.ui.core.plus
import androidx.ui.core.tightConstraints
import androidx.ui.core.times
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Image
import androidx.ui.painting.Paint
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.Recompose
import com.google.r4a.composer
import kotlin.math.min

@Composable
fun Padding(
    left: Dimension = 0.dp,
    top: Dimension = 0.dp,
    right: Dimension = 0.dp,
    bottom: Dimension = 0.dp, @Children children: () -> Unit
) {
    <MeasureBox bust=Math.random()>constraints, measureOperations ->
        val measurables = measureOperations.collect(children)
        val horizontalPadding = (left + right)
        val verticalPadding = (top + bottom)

        val newConstraints = Constraints(
            minWidth = (constraints.minWidth - horizontalPadding).coerceAtLeast(0.dp),
            maxWidth = (constraints.maxWidth - horizontalPadding).coerceAtLeast(0.dp),
            minHeight = (constraints.minHeight - verticalPadding).coerceAtLeast(0.dp),
            maxHeight = (constraints.maxHeight - verticalPadding).coerceAtLeast(0.dp)
        )
        val placeable = measurables.firstOrNull()?.measure(newConstraints)
        val width = (placeable?.width ?: 0.dp) + horizontalPadding
        val height = (placeable?.height ?: 0.dp) + verticalPadding

        measureOperations.layout(width, height) {
            placeable?.place(left, top)
        }
    </MeasureBox>
}

@Composable
fun FourQuadrants(bust: Double) {
    <MeasureBox bust=Math.random() > constraints, measureOperations ->
        val context = composer.composer.context
        val resources = context.resources
        val image =
            BitmapFactory.decodeResource(resources, androidx.ui.port.R.drawable.four_quadrants)
        val maxHeight = constraints.maxHeight
        val maxWidth = constraints.maxWidth
        val aspectRatio = image.width.toFloat()/image.height.toFloat()
        val height = min(maxHeight, maxWidth * aspectRatio)
        val width = min(maxWidth, maxHeight / aspectRatio)

        measureOperations.collect {
            <DrawImage bitmap=image/>
        }

        measureOperations.layout(width, height) {
        }
    </MeasureBox>
}

@Composable
fun DrawImage(bitmap: Bitmap) {
    val onPaint: (Canvas, PixelSize) -> Unit = { canvas, parentSize ->
        val paint = Paint()
        canvas.save()
        val width = parentSize.width
        val height = parentSize.height
        val scale = min(width/bitmap.width.toFloat(), height/bitmap.height.toFloat())
        canvas.scale(scale, scale)
        canvas.drawImage(Image(bitmap), Offset(0.0f, 0.0f), paint)
        canvas.restore()
    }
    <Draw onPaint />
}

@Composable
fun DrawRectangle(color: Color) {
    val paint = Paint()
    paint.color = color
    val onPaint: (Canvas, PixelSize) -> Unit = { canvas, parentSize ->
        val widthPx = parentSize.width
        val heightPx = parentSize.height
        canvas.drawRect(Rect(0.0f, 0.0f, widthPx, heightPx), paint)
    }
    <Draw onPaint />
}

@Composable
fun Rectangle(width: Dimension, height: Dimension, color: Color) {
    <MeasureBox bust=Math.random()> _, measureOperations ->
        measureOperations.collect {
            <DrawRectangle color />
        }
        measureOperations.layout(width, height) {}
    </MeasureBox>
}

@Composable
fun Rectangles(bust: Double) {
    <MeasureBox bust=Math.random()> constraints, measureOperations ->
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            constraints.minWidth
        }

        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            constraints.minHeight
        }

        val size = min(width, height)
        val rectSize = size / 2
        val measurables = measureOperations.collect {
            val green = Color(0xFF00FF00.toInt())
            <Rectangle width=rectSize height=rectSize color=green />
            val red = Color(0xFFFF0000.toInt())
            <Rectangle width=rectSize height=rectSize color=red />
            val blue = Color(0xFF0000FF.toInt())
            <Rectangle width=rectSize height=rectSize color=blue />
            val yellow = Color(0xFFFFFF00.toInt())
            <Rectangle width=rectSize height=rectSize color=yellow />
        }

        measureOperations.layout(size, size) {
            val placeables = measurables.map {
                it.measure(tightConstraints(size, size))
            }
            placeables[0].place(0.dp, 0.dp)
            placeables[1].place(rectSize, 0.dp)
            placeables[2].place(0.dp, rectSize)
            placeables[3].place(rectSize, rectSize)
        }
    </MeasureBox>
}

var iterations = 0
var recomposeFunc : Runnable? = null

@Composable
fun CraneRects() {
    <CraneWrapper>
        <Recompose> recompose ->
            if (recomposeFunc == null) {
                val handler = Handler(Looper.getMainLooper())
                recomposeFunc = object: Runnable {
                    override fun run() {
                        iterations++
                        handler.postDelayed(this, 1000)
                        recompose()
                    }
                }
                handler.postDelayed(recomposeFunc, 1000)
            }
            <Padding left=20.dp top=20.dp right=20.dp bottom=20.dp>
                if (iterations % 2 == 0) {
                    <FourQuadrants bust=Math.random() />
                } else {
                    <Rectangles bust=Math.random()/>
                }
            </Padding>
        </Recompose>
    </CraneWrapper>
}

