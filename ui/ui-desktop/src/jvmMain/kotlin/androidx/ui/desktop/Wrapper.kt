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
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.View

import androidx.animation.rootAnimationClockFactory
import androidx.animation.ManualAnimationClock
import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.Recomposer
import androidx.ui.core.setContent
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.input.TextInputService

import javax.swing.SwingUtilities

import org.jetbrains.skija.Canvas

fun SkiaWindow.setContent(content: @Composable () -> Unit) {
    SwingUtilities.invokeLater {
        val fps = 60
        val clocks = mutableListOf<ManualAnimationClock>()
        rootAnimationClockFactory = {
            ManualAnimationClock(0L).also {
                clocks.add(it)
            }
        }

        val context = object : Context() {}
        val viewGroup = object : ViewGroup(context) {}
        val platformInputService = DesktopPlatformInput()
        viewGroup.setContent(Recomposer.current(), @Composable {
            Providers(TextInputServiceAmbient provides TextInputService(
                platformInputService), children = content)
        })
        val view = viewGroup.getChildAt(0)
        view.onAttachedToWindow()

        this.renderer = Renderer(view, clocks, fps, platformInputService)
        this.setFps(fps)
    }
}

private class Renderer(
    val view: View,
    val clocks: List<ManualAnimationClock>,
    val fps: Int,
    val platformInputService: DesktopPlatformInput
) : SkiaRenderer {
    var androidCanvas: android.graphics.Canvas? = null

    override fun onInit() {
    }

    override fun onDispose() {
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        if (androidCanvas == null) {
            androidCanvas = android.graphics.Canvas(canvas)
        }
        view.onMeasure(width, height)
        view.onLayout(true, 0, 0, width, height)
        view.dispatchDraw(androidCanvas!!)
    }

    override fun onReshape(canvas: Canvas, width: Int, height: Int) {
        androidCanvas = null
        draw(canvas, width, height)
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int) {
        clocks.forEach {
            it.clockTimeMillis += 1000 / fps
        }
        draw(canvas, width, height)
    }

    override fun onMouseClicked(x: Int, y: Int, modifiers: Int) {}

    override fun onMousePressed(x: Int, y: Int, awtModifiers: Int) {
        view.dispatchTouchEvent(
            MotionEvent(x, y, MotionEvent.ACTION_DOWN or modifiers(awtModifiers)))
    }

    override fun onMouseReleased(x: Int, y: Int, awtModifiers: Int) {
        view.dispatchTouchEvent(MotionEvent(x, y, MotionEvent.ACTION_UP or modifiers(awtModifiers)))
    }

    override fun onMouseDragged(x: Int, y: Int, awtModifiers: Int) {
        view.dispatchTouchEvent(MotionEvent(x, y,
            MotionEvent.ACTION_MOVE or modifiers(awtModifiers)))
    }

    private fun modifiers(awtModifiers: Int): Int {
        return 0
    }

    override fun onKeyPressed(code: Int, char: Char) {
        platformInputService.onKeyPressed(code, char)
    }

    override fun onKeyReleased(code: Int, char: Char) {
        platformInputService.onKeyReleased(code, char)
    }

    override fun onKeyTyped(char: Char) {
        platformInputService.onKeyTyped(char)
    }
}
