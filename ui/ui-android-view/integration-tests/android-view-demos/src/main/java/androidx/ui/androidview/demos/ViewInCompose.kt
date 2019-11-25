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

package androidx.ui.androidview.demos

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.Providers
import androidx.compose.state
import androidx.ui.androidview.AndroidView
import androidx.ui.androidview.adapters.Ref
import androidx.ui.androidview.adapters.setRef
import androidx.ui.core.ContextAmbient
import androidx.ui.core.setContent
import androidx.ui.graphics.Color
import androidx.ui.graphics.toArgb
import androidx.ui.layout.Column
import androidx.ui.material.Button

class ViewInCompose : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                // Include Android View.
                TextView(text = "This is a text in a TextView")

                // Inflate AndroidView from XML.
                AndroidView(R.layout.test_layout)

                // Inflate AndroidView from XML with style set.
                val context = ContextAmbient.current
                Providers(
                    ContextAmbient provides ContextThemeWrapper(context, R.style.TestLayoutStyle)
                ) {
                    AndroidView(R.layout.test_layout)
                }

                // Compose custom Android View and do remeasurements and invalidates.
                val squareRef = Ref<ColoredSquareView>()
                FrameLayout {
                    ColoredSquareView(size = 200, color = Color.Cyan, ref = squareRef)
                }
                Button(
                    text = "Increase size of Android view",
                    onClick = { squareRef.value!!.size += 50 }
                )
                val colorIndex = state { 0 }
                Button(text = "Change color of Android view", onClick = {
                    colorIndex.value = (colorIndex.value + 1) % 4
                    squareRef.value!!.color = arrayOf(
                        Color.Blue, Color.LightGray, Color.Yellow, Color.Cyan
                    )[colorIndex.value]
                })

                // Inflate AndroidView from XML and change it in callback post inflation.
                AndroidView(R.layout.test_layout) { view ->
                    view as RelativeLayout
                    view.setVerticalGravity(Gravity.BOTTOM)
                    view.layoutParams.width = 700
                    view.setBackgroundColor(android.graphics.Color.BLUE)
                }
            }
        }
    }
}

class ColoredSquareView(context: Context) : View(context) {
    var size: Int = 100
        set(value) {
            if (value != field) {
                field = value
                requestLayout()
            }
        }

    var color: Color = Color.Blue
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(size, size)
    }
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(color.toArgb())
    }
}
