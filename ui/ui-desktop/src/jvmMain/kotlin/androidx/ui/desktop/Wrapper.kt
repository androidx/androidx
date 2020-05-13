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
package androidx.ui.desktop

import android.content.Context
import android.view.ViewGroup
import android.view.View

import androidx.animation.rootAnimationClockFactory
import androidx.animation.ManualAnimationClock
import androidx.compose.Composable
import androidx.compose.Recomposer
import androidx.ui.core.Modifier
import androidx.ui.core.Owner
import androidx.ui.core.setContent
import androidx.ui.layout.padding
import androidx.ui.unit.dp

import javax.swing.SwingUtilities

import org.jetbrains.skija.Canvas

fun SkiaWindow.setContent(content: @Composable() () -> Unit) {
    SwingUtilities.invokeLater {
        println("start composing!")

        rootAnimationClockFactory = { ManualAnimationClock(0L) }
        val context = object : Context() {}
        val viewGroup = object : ViewGroup(context) {}
        viewGroup.setContent(Recomposer.current(), content)
        val view = viewGroup.getChildAt(0)
        // we need this to override the root drawLayer() - RenderNode are not ported yet
        (view as Owner).root.modifier = Modifier.padding(0.dp)
        view.onAttachedToWindow()

        this.renderer = Renderer(view)
        this.setFps(60)
    }
}

private class Renderer(val view: View) : SkiaRenderer {
    var androidCanvas: android.graphics.Canvas? = null

    override fun onInit() {
    }

    override fun onDispose() {
    }

    override fun onReshape(width: Int, height: Int) {
        androidCanvas = null
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int) {
        if (androidCanvas == null) {
            androidCanvas = android.graphics.Canvas(canvas)
        }
        view.onMeasure(width, height)
        view.dispatchDraw(androidCanvas!!)
    }
}
