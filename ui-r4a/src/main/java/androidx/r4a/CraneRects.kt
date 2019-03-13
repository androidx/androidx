/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.ui.core.Constraints
import androidx.ui.core.CraneWrapper
import androidx.ui.core.IntPx
import androidx.ui.core.MeasureBox
import androidx.ui.core.PxPosition
import androidx.ui.core.adapter.Draw
import androidx.ui.core.adapter.PressGestureDetector
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.core.min
import androidx.ui.core.px
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.Offset
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Padding
import androidx.ui.painting.Color
import androidx.ui.painting.Image
import androidx.ui.painting.Paint
import com.google.r4a.Composable
import com.google.r4a.Recompose
import com.google.r4a.composer
import kotlin.math.min

@Composable
fun FourQuadrants() {
    val resources = composer.composer.context.resources
    val image = BitmapFactory.decodeResource(resources, androidx.ui.port.R.drawable.four_quadrants)
    <MeasureBox> constraints ->
        collect {
            <DrawImage bitmap=image />
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
        }
    </MeasureBox>
}

@Composable
fun DrawImage(bitmap: Bitmap) {
    val paint = Paint()
    <Draw> canvas, parentSize ->
        canvas.save()
        val width = parentSize.width
        val height = parentSize.height
        val scale = min(width / bitmap.width.px, height / bitmap.height.px)
        canvas.scale(scale, scale)
        canvas.drawImage(Image(bitmap), Offset(0.0f, 0.0f), paint)
        canvas.restore()
    </Draw>
}

@Composable
fun DrawRectangle(color: Color) {
    val paint = Paint()
    paint.color = color
    <Draw> canvas, parentSize ->
        canvas.drawRect(parentSize.toRect(), paint)
    </Draw>
}

@Composable
fun Rectangle(color: Color) {
    <MeasureBox> constraints ->
        collect {
            <DrawRectangle color />
        }
        layout(constraints.maxWidth, constraints.maxHeight) {}
    </MeasureBox>
}

@Composable
fun Rectangles() {
    <MeasureBox> constraints ->
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

        val measurables = collect {
            val green = Color(0xFF00FF00.toInt())
            <Rectangle color=green />
            val red = Color(0xFFFF0000.toInt())
            <Rectangle color=red />
            val blue = Color(0xFF0000FF.toInt())
            <Rectangle color=blue />
            <FourQuadrants />
        }

        val size = min(width, height)
        val rectSize = size / 2
        layout(size, size) {
            val placeables = measurables.map {
                it.measure(Constraints.tightConstraints(rectSize, rectSize))
            }
            placeables[0].place(IntPx.Zero, IntPx.Zero)
            placeables[1].place(rectSize, IntPx.Zero)
            placeables[2].place(IntPx.Zero, rectSize)
            placeables[3].place(rectSize, rectSize)
        }
    </MeasureBox>
}

var small = false
var pressed = false

@Composable
fun CraneRects() {
    <CraneWrapper>
        <Recompose> recompose ->
            val onPress: (PxPosition) -> Unit = {
                pressed = true
                recompose()
            }

            val onRelease = {
                small = !small
                pressed = false
                recompose()
            }

            val onCancel = {
                pressed = false
                recompose()
            }

            val padding = if (pressed) 36.dp else if (small) 48.dp else 96.dp

            <Padding padding=EdgeInsets(padding)>
                <PressGestureDetector onPress onRelease onCancel>
                    <Padding padding=EdgeInsets(0.dp)>
                        <Rectangles />
                    </Padding>
                </PressGestureDetector>
            </Padding>
        </Recompose>
    </CraneWrapper>
}
