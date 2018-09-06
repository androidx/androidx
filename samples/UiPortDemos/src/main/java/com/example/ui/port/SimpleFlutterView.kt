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
import androidx.ui.engine.window.Window
import androidx.ui.flow.CompositorContext
import androidx.ui.painting.Canvas
import androidx.ui.skia.SkMatrix
import androidx.ui.vectormath64.Matrix4
import androidx.ui.widgets.binding.runApp
import androidx.ui.widgets.framework.Widget

@SuppressLint("ViewConstructor")
class SimpleFlutterView(
    context: Context,
    widget: Widget
) : View(context) {

    private var scene: Scene? = null

    init {
        runApp(widget)
        Window.renderDelegate = { newScene ->
            scene = newScene
            invalidate()
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