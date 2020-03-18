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
package androidx.ui.core

import androidx.compose.Composable
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.painter.Painter
import androidx.ui.unit.Density
import androidx.ui.unit.PxSize

/**
 * Use Draw to get a [Canvas] to paint into the parent.
 *
 *  The [onPaint] lambda uses a [Density] receiver scope, to allow easy translation
 *  between [Dp], [Sp], and [Px]. The `parentSize` parameter indicates the layout size of
 *  the parent.
 *
 *  *Deprecated:* Draw composable is a common source of bugs as it's not a layout and takes parent
 *  size, but doesn't tell you that. Therefore, layout strategies, like [androidx.ui.layout.Row] or
 * [androidx.ui.layout.Column] doesn't work with Draw. You should use [draw] modifier if you want
 * to decorate existent layout with custom drawing, use existent drawing modifiers
 * ([androidx.ui.foundation.DrawBackground], [androidx.ui.foundation.DrawBorder],
 * [Painter.toModifier])or use [androidx.ui.foundation.Canvas] to make a layout that takes space
 * and allows custom drawing.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
@Deprecated(
    "Draw composable is a common source of bugs as it's not a layout and takes parent size, but " +
            "doesn't tell you that. Therefore, layout strategies, like Row or Column doesn't work" +
            " with Draw. You should use androidx.ui.core.draw modifier if you want to decorate " +
            "existent layout with custom drawing, use existent drawing modifiers (DrawBackground," +
            " DrawBorder, PainterModifier) or use androidx.ui.foundation.Canvas to make a layout " +
            "that takes space and allows custom drawing",
    ReplaceWith("modifier = draw(onPaint)")
)
inline fun Draw(
    noinline onPaint: Density.(canvas: Canvas, parentSize: PxSize) -> Unit
) {
    DrawNode(onPaint = onPaint)
}

/**
 * A Draw scope that accepts children to allow modifying the canvas for children.
 * The [children] are drawn when [DrawReceiver.drawChildren] is called.
 * If the [onPaint] does not call [DrawReceiver.drawChildren] then it will be called
 * after the lambda.
 *
 * *Deprecated:* Draw composable is a common source of bugs as it's not a layout and takes parent
 * size, but doesn't tell you that. Therefore, layout strategies, like [androidx.ui.layout.Row] or
 * [androidx.ui.layout.Column] doesn't work with Draw. You should use [drawWithContent] modifier
 * if you want to decorate existent layout with custom drawing
 */
@Composable
@Deprecated(
    "Draw composable is a common source of bugs as it's not a layout and takes parent size, but " +
            "doesn't tell you that. Therefore, layout strategies, like Row or Column doesn't work" +
            " with Draw. You should use androidx.ui.core.drawWithContent modifier if you want to " +
            "decorate existent layout with custom drawing",
    ReplaceWith("modifier = drawWithContent(onPaint)")
)
inline fun Draw(
    crossinline children: @Composable() () -> Unit,
    noinline onPaint: DrawReceiver.(canvas: Canvas, parentSize: PxSize) -> Unit
) {
    DrawNode(onPaintWithChildren = onPaint) {
        children()
    }
}
