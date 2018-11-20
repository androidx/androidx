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

package com.example.ui.port

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.view.ViewConfiguration
import androidx.ui.CraneView
import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.Key
import androidx.ui.gestures2.Direction
import androidx.ui.gestures2.GestureDetector2
import androidx.ui.gestures2.MetaData
import androidx.ui.gestures2.DragGestureRecognizerCallback
import androidx.ui.gestures2.ScaleGestureRecognizerCallback
import androidx.ui.painting.Image
import androidx.ui.painting.alignment.Alignment
import androidx.ui.widgets.basic.Align
import androidx.ui.widgets.basic.RawImage
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget
import androidx.ui.widgets.framework.Widget

class WoodPeckerDemo : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val metaData = MetaData(ViewConfiguration.get(this).scaledTouchSlop)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test)
        val widget = Gestures2App(Key.createKey("jetpack image widget!"), bitmap, metaData)
        setContentView(CraneView(this, widget))
    }

    class Gestures2App(key: Key, private val bitmap: Bitmap, private val metaData: MetaData) :
        StatefulWidget(key) {
        override fun createState() =
            Gestures2AppState(this, bitmap, metaData) as State<StatefulWidget>
    }

    class Gestures2AppState(
        widget: Gestures2App,
        private val bitmap: Bitmap,
        private val metaData: MetaData
    ) : State<Gestures2App>(widget) {

        var xTranslation = 0.0
        var yTranslation = 0.0
        var scale = 1.0

        override fun build(context: BuildContext): Widget {
            val bitmap: Bitmap =
                bitmap.mod(xTranslation, yTranslation, scale)

            return GestureDetector2(
                child = Align(
                    key = Key.createKey("alignWidget"),
                    alignment = Alignment.center,
                    child = RawImage(
                        image = Image(bitmap),
                        key = Key.createKey("jetpack image")
                    )
                ),
                dragCallback = object : DragGestureRecognizerCallback {
                    override fun canDrag(direction: Direction) = true
                    override fun drag(dx: Double, dy: Double): Offset {
                        setState {
                            this@Gestures2AppState.xTranslation += dx
                            this@Gestures2AppState.yTranslation += dy
                        }
                        return Offset(dx, dy)
                    }
                },
                onTap = {
                    setState {
                        this@Gestures2AppState.scale = 1.0
                        this@Gestures2AppState.xTranslation = 0.0
                        this@Gestures2AppState.yTranslation = 0.0
                    }
                },
                scaleCallback = object : ScaleGestureRecognizerCallback {
                    override fun onScaleRatio(ratio: Double): Double {
                        setState {
                            this@Gestures2AppState.scale *= ratio
                        }
                        return ratio
                    }

                    override fun onAveragePointMove(offset: Offset): Offset {
                        setState {
                            this@Gestures2AppState.xTranslation += offset.dx
                            this@Gestures2AppState.yTranslation += offset.dy
                        }
                        return offset
                    }
                },
                metaData = metaData
            )
        }

        private fun Bitmap.mod(dx: Double, dy: Double, scaleRatio: Double): Bitmap {
            val m = Matrix().apply {
                preTranslate(-dx.toFloat(), -dy.toFloat())
                preScale(
                    scaleRatio.toFloat(),
                    scaleRatio.toFloat(),
                    width.toFloat() / 2,
                    height.toFloat() / 2)
            }
            val bitmap = Bitmap.createBitmap(width, height, config)
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(this, m, Paint())
            return bitmap
        }
    }
}
