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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.ui.platform.DesktopOwner
import androidx.compose.ui.platform.DesktopOwners
import androidx.compose.ui.platform.setContent
import org.jetbrains.skija.Canvas
import java.awt.event.InputMethodEvent
import java.awt.im.InputMethodRequests

fun Window.setContent(content: @Composable () -> Unit): Composition {
    val owners = DesktopOwners(glCanvas, glCanvas::display)
    val owner = DesktopOwner(owners)
    val composition = owner.setContent(content)

    this.renderer = SkiaRenderer(owners)

    parent.onDismissEvents.add(owner::dispose)

    return composition
}

fun Dialog.setContent(content: @Composable () -> Unit): Composition {
    val owners = DesktopOwners(glCanvas, glCanvas::display)
    val owner = DesktopOwner(owners)
    val composition = owner.setContent(content)

    this.renderer = SkiaRenderer(owners)

    parent.onDismissEvents.add(owner::dispose)

    return composition
}

fun SkiaRenderer(owners: DesktopOwners) = object : SkiaRenderer {
    override fun onInit() {
    }

    override fun onDispose() {
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int) {
        owners.onRender(canvas, width, height)
    }

    override fun onReshape(canvas: Canvas, width: Int, height: Int) {
    }

    override fun onMouseClicked(x: Int, y: Int, modifiers: Int) {}

    override fun onMousePressed(x: Int, y: Int, modifiers: Int) {
        owners.onMousePressed(x, y)
    }

    override fun onMouseReleased(x: Int, y: Int, modifiers: Int) {
        owners.onMouseReleased(x, y)
    }

    override fun onMouseDragged(x: Int, y: Int, modifiers: Int) {
        owners.onMouseDragged(x, y)
    }

    override fun onKeyPressed(code: Int, char: Char) {
        owners.onKeyPressed(code, char)
    }

    override fun onKeyReleased(code: Int, char: Char) {
        owners.onKeyReleased(code, char)
    }

    override fun inputMethodRequests(): InputMethodRequests? {
        return owners.getInputMethodRequests()
    }

    override fun onInputMethodTextChanged(event: InputMethodEvent) {
        owners.onInputMethodTextChanged(event)
    }

    override fun onKeyTyped(char: Char) {
        owners.onKeyTyped(char)
    }
}