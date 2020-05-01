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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.DrawScope
import androidx.ui.core.drawBehind
import androidx.ui.graphics.painter.CanvasScope
import androidx.ui.layout.ColumnScope
import androidx.ui.layout.Spacer

/**
 * Component that allow you to specify an area on the screen and perform canvas drawing on this
 * area. You MUST specify size with modifier, whether with exact sizes via [LayoutSize] modifier,
 * or relative to parent, via [LayoutSize.Fill], [ColumnScope.LayoutWeight], etc. If parent
 * wraps this child, only exact sizes must be specified.
 *
 * @sample androidx.ui.foundation.samples.CanvasSample
 *
 * @param modifier mandatory modifier to specify size strategy for this composable
 * @param onCanvas lambda that will be called to perform drawing. Note that this lambda will be
 * called during draw stage, you have no access to composition scope, meaning that [Composable]
 * function invocation inside it will result to runtime exception
 */

// TODO b/154962350 to handle deprecation and updating of all compose APIs to Canvas2
// @Deprecated("Favor usage of Canvas2 which leverages the CanvasScope API. CanvasScope" +
//        " introduces a stateless, declarative API surface without the requirement of maintaining" +
//        "a Paint object",
//    ReplaceWith("PaintBox"))
@Composable
fun Canvas(modifier: Modifier, onCanvas: DrawScope.() -> Unit) {
    Spacer(
        modifier.drawBehind(onCanvas)
    )
}

/**
 * Creates a composable that lays out and draws content within a [CanvasScope].
 * The size of this [Canvas2] is purely dictated by the given modifier chain.
 *
 * @sample androidx.ui.foundation.samples.Canvas2Sample
 *
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param block Lambda callback invoked on the created [CanvasScope] to issue drawing commands
 */
@Composable
fun Canvas2(modifier: Modifier, block: CanvasScope.() -> Unit) {
    val canvasScope = remember { CanvasScope() }
    Spacer(
        modifier.drawBehind {
            canvasScope.draw(this, size, block)
        }
    )
}