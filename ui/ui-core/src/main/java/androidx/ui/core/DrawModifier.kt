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
import androidx.ui.graphics.drawscope.DrawScope

/**
 * A [Modifier.Element] that draws into the space of the layout.
 */
interface DrawModifier : Modifier.Element {
    fun ContentDrawScope.draw()
}

/**
 * Draw into a [Canvas] behind the modified content.
 */
fun Modifier.drawBehind(
    onDraw: DrawScope.() -> Unit
) = this + DrawBackgroundModifier(onDraw)

/**
 * Creates a [DrawModifier] that calls [onDraw] before the contents of the layout.
 */
@Deprecated(
    "Replaced by Modifier.drawBehind",
    replaceWith = ReplaceWith(
        "Modifier.drawBehind(onDraw)",
        "androidx.ui.core.drawBehind",
        "androidx.ui.core.Modifier"
    )
)
fun draw(
    onDraw: DrawScope.() -> Unit
): DrawModifier = DrawBackgroundModifier(onDraw)

private class DrawBackgroundModifier(
    val onDraw: DrawScope.() -> Unit
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        onDraw()
        drawContent()
    }
}

/**
 * Creates a [DrawModifier] that allows the developer to draw before or after the layout's
 * contents. It also allows the modifier to adjust the layout's canvas.
 */
// TODO: Inline this function -- it breaks with current compiler
/*inline*/ fun Modifier.drawWithContent(
    onDraw: ContentDrawScope.() -> Unit
): Modifier = this + object : DrawModifier {
    override fun ContentDrawScope.draw() {
        onDraw()
    }
}

/**
 * Creates a [DrawModifier] that allows the developer to draw before or after the layout's
 * contents. It also allows the modifier to adjust the layout's canvas.
 */
@Deprecated(
    "Replaced by Modifier.drawWithContent",
    ReplaceWith(
        "Modifier.drawWithContent(onDraw)",
        "androidx.ui.core.Modifier"
    )
)
/*inline*/ fun drawWithContent(
    onDraw: ContentDrawScope.() -> Unit
): DrawModifier = Modifier.drawWithContent(onDraw) as DrawModifier
