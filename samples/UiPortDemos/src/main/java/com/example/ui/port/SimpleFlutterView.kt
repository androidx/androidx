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

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.ui.compositing.Scene
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.window.Window
import androidx.ui.engine.window.WindowPadding
import androidx.ui.flow.CompositorContext
import androidx.ui.painting.Canvas
import androidx.ui.skia.SkMatrix
import androidx.ui.updateWindowMetrics
import androidx.ui.vectormath64.Matrix4
import androidx.ui.widgets.binding.WidgetsFlutterBinding
import androidx.ui.widgets.binding.runApp
import androidx.ui.widgets.framework.Widget

@SuppressLint("ViewConstructor")
class SimpleFlutterView(
    context: Context,
    private val widget: Widget
) : View(context) {

    private var scene: Scene? = null
    private var initialized: Boolean = false
    private val window = Window()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        updateMetrics()
        if (!initialized) {
            initialized = true
            window.renderDelegate = { newScene ->
                scene = newScene
                invalidate()
            }
            runApp(widget, WidgetsFlutterBinding.create(window))
        }
    }

    private fun updateMetrics() {
        val devicePixelRatio = resources.displayMetrics.density.toDouble()
        val size = Size(measuredWidth.toDouble(), measuredHeight.toDouble())
        val padding = WindowPadding(paddingLeft.toDouble(), paddingTop.toDouble(),
                paddingRight.toDouble(), paddingBottom.toDouble())
        if (window.devicePixelRatio != devicePixelRatio ||
                window.physicalSize != size ||
                window.padding != padding) {
            window.updateWindowMetrics(
                    devicePixelRatio = devicePixelRatio,
                    width = size.width,
                    height = size.height,
                    paddingTop = padding.top,
                    paddingRight = padding.right,
                    paddingBottom = padding.bottom,
                    paddingLeft = padding.left,
                    viewInsetTop = 0.0,
                    viewInsetRight = 0.0,
                    viewInsetBottom = 0.0,
                    viewInsetLeft = 0.0
            )
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(frameworkCanvas: android.graphics.Canvas) {
        scene?.let { scene ->
            val canvas = Canvas(frameworkCanvas)
            val surfaceTransformation = SkMatrix(Matrix4.identity())
            val frame = CompositorContext().AcquireFrame(canvas, surfaceTransformation)
            frame.Raster(scene.takeLayerTree())
            frame.destructor()
        }
    }
}