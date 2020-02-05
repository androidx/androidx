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

package androidx.ui.core

import androidx.ui.graphics.Canvas
import androidx.ui.unit.Density
import androidx.ui.unit.PxSize

/**
 * A [Modifier.Element] that draws into the space of the layout.
 */
interface DrawModifier : Modifier.Element {
    fun draw(
        density: Density,
        drawContent: () -> Unit,
        canvas: Canvas,
        size: PxSize
    )
}

/**
 * Creates a [DrawModifier] that calls [onDraw] before the contents of the layout.
 */
fun draw(
    onDraw: Density.(canvas: Canvas, size: PxSize) -> Unit
): DrawModifier = object : DrawModifier, Density {
    private var _density: Density? = null
    override val density: Float get() = _density!!.density
    override val fontScale: Float get() = _density!!.fontScale

    override fun draw(
        density: Density,
        drawContent: () -> Unit,
        canvas: Canvas,
        size: PxSize
    ) {
        _density = density
        try {
            this.onDraw(canvas, size)
        } finally {
            _density = null
            drawContent()
        }
    }
}

/**
 * Creates a [DrawModifier] that allows the developer to draw before or after the layout's
 * contents. It also allows the modifier to adjust the layout's canvas.
 */
// TODO(mount): DrawReceiver should accept a Canvas for drawChildren()
// TODO(mount): drawChildren() should be drawContent()
fun drawWithContent(
    onDraw: DrawReceiver.(canvas: Canvas, size: PxSize) -> Unit
): DrawModifier = object : DrawModifier, DrawReceiver {
    private var _density: Density? = null
    override val density: Float get() = _density!!.density
    override val fontScale: Float get() = _density!!.fontScale
    private var drawContentFunction: (() -> Unit)? = null

    override fun draw(
        density: Density,
        drawContent: () -> Unit,
        canvas: Canvas,
        size: PxSize
    ) {
        drawContentFunction = drawContent
        _density = density
        try {
            this.onDraw(canvas, size)
        } finally {
            _density = null
            drawContentFunction = null
        }
    }

    override fun drawChildren() {
        drawContentFunction!!.invoke()
    }
}
