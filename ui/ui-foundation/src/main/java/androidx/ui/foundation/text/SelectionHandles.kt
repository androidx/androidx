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

package androidx.ui.foundation.text

import androidx.compose.Composable
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Draw
import androidx.ui.core.dp
import androidx.ui.core.selection.Selection
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.Path
import androidx.ui.layout.Container
import androidx.ui.text.style.TextDirection

internal val HANDLE_WIDTH = 25.dp
internal val HANDLE_HEIGHT = 25.dp
private val HANDLE_COLOR = Color(0xFF2B28F5.toInt())

@Composable
private fun SelectionHandle(left: Boolean) {
    val paint = +memo { Paint().also { it.isAntiAlias = true } }
    paint.color = HANDLE_COLOR
    Container(width = HANDLE_WIDTH, height = HANDLE_HEIGHT) {
        Draw { canvas, _ ->
            var path = Path()
            path.addRect(
                Rect(
                    top = 0f,
                    bottom = 0.5f * HANDLE_HEIGHT.toPx().value,
                    left = if (left) {
                        0.5f * HANDLE_WIDTH.toPx().value
                    } else {
                        0f
                    },
                    right = if (left) {
                        HANDLE_WIDTH.toPx().value
                    } else {
                        0.5f * HANDLE_WIDTH.toPx()
                            .value
                    }
                )
            )
            path.addOval(
                Rect(
                    top = 0f,
                    bottom = HANDLE_HEIGHT.toPx().value,
                    left = 0f,
                    right = HANDLE_WIDTH.toPx().value
                )
            )

            canvas.drawPath(path, paint)
        }
    }
}

@Composable
internal fun StartSelectionHandle(selection: Selection?) {
    selection?.let {
        if (it.start.direction == TextDirection.Ltr) SelectionHandle(left = true)
        else SelectionHandle(left = false)
    }
}

@Composable
internal fun EndSelectionHandle(selection: Selection?) {
    selection?.let {
        if (it.end.direction == TextDirection.Ltr) SelectionHandle(left = false)
        else SelectionHandle(left = true)
    }
}
