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
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.DesktopPlatformInput
import androidx.ui.desktop.view.LayoutScope
import javax.swing.SwingUtilities
import org.jetbrains.skija.Canvas

fun Window.setContent(content: @Composable () -> Unit) {
    SwingUtilities.invokeLater {
        val mainLayout = LayoutScope(glCanvas)
        mainLayout.setContent(content)
        this.renderer = Renderer(
            mainLayout.context,
            mainLayout.platformInputService)
    }
}

fun Dialog.setContent(content: @Composable () -> Unit) {
    SwingUtilities.invokeLater {
        val mainLayout = LayoutScope(glCanvas)
        mainLayout.setContent(content)

        this.renderer = Renderer(
            mainLayout.context,
            mainLayout.platformInputService)
    }
}

private class Renderer(
    val context: Context,
    val platformInputService: DesktopPlatformInput
) : SkiaRenderer {

    private val canvases = mutableMapOf<LayoutScope, Canvas?>()

    fun getCanvas(layout: LayoutScope, canvas: Canvas): Canvas? {
        if (!canvases.containsKey(layout)) {
            canvases[layout] = canvas
        }
        return canvases[layout]
    }

    fun clearCanvases() {
        canvases.clear()
    }

    override fun onInit() {
    }

    override fun onDispose() {
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        for (layout in LayoutScopeGlobal.getLayoutScopes(context)) {
            // layout.updateLayout(width, height)
            layout.draw(getCanvas(layout, canvas)!!, width, height)
        }
    }

    override fun onReshape(canvas: Canvas, width: Int, height: Int) {
        clearCanvases()
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int) {
        draw(canvas, width, height)
    }

    override fun onMouseClicked(x: Int, y: Int, modifiers: Int) {}

    override fun onMousePressed(x: Int, y: Int, modifiers: Int) {
        getCurrentLayoutScope()?.dispatchTouchEvent(
            applyLayoutScopeOffset(x, y, MotionEvent.ACTION_DOWN))
    }

    override fun onMouseReleased(x: Int, y: Int, modifiers: Int) {
        getCurrentLayoutScope()?.dispatchTouchEvent(
            applyLayoutScopeOffset(x, y, MotionEvent.ACTION_UP))
    }

    override fun onMouseDragged(x: Int, y: Int, modifiers: Int) {
        getCurrentLayoutScope()?.dispatchTouchEvent(
            applyLayoutScopeOffset(x, y, MotionEvent.ACTION_MOVE))
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

    private fun getCurrentLayoutScope(): LayoutScope? {
        return LayoutScopeGlobal.getLayoutScopes(context).lastOrNull()
    }

    private fun applyLayoutScopeOffset(x: Int, y: Int, state: Int): MotionEvent {
        val currentLayoutScope = getCurrentLayoutScope()

        if (currentLayoutScope == null) {
            return MotionEvent(x, y, state)
        }

        var offsetX = currentLayoutScope.x
        var offsetY = currentLayoutScope.y

        return MotionEvent(x - offsetX, y - offsetY, state)
    }
}
