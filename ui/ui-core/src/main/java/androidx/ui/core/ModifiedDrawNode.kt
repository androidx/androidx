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

package androidx.ui.core

import androidx.ui.geometry.Size
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.painter.drawCanvas

internal class ModifiedDrawNode(
    wrapped: LayoutNodeWrapper,
    drawModifier: DrawModifier
) : DelegatingLayoutNodeWrapper<DrawModifier>(wrapped, drawModifier) {
    private val drawScope = DrawScopeImpl()

    // This is not thread safe
    override fun draw(canvas: Canvas) {
        withPositionTranslation(canvas) {
            val size = Size(
                measuredSize.width.value.toFloat(),
                measuredSize.height.value.toFloat()
            )
            drawScope.draw(canvas, size) {
                with(drawScope) {
                    with(modifier) {
                        draw()
                    }
                }
            }
        }
    }

    inner class DrawScopeImpl() : ContentDrawScope() {
        override fun drawContent() {
            drawCanvas { canvas, _ ->
                wrapped.draw(canvas)
            }
        }

        override val density: Float
            get() = layoutNode.requireOwner().density.density

        override val fontScale: Float
            get() = layoutNode.requireOwner().density.fontScale

        override val layoutDirection: LayoutDirection
            get() = this@ModifiedDrawNode.measureScope.layoutDirection
    }
}