/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.integration.a2ui.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a subtle grid of dots behind the content, creating an elevated canvas feel commonly used in
 * design tools and component preview sandboxes.
 */
internal fun Modifier.drawDottedPattern(
    dotColor: Color,
    spacing: Dp = 16.dp,
    dotRadius: Dp = 1.25.dp,
): Modifier = drawWithCache {
    val spacingPx = spacing.toPx()
    if (spacingPx <= 0f) {
        return@drawWithCache onDrawBehind {}
    }
    val dotRadiusPx = dotRadius.toPx()
    val xCount = (size.width / spacingPx).toInt()
    val yCount = (size.height / spacingPx).toInt()
    val xOffset = (size.width - xCount * spacingPx) / 2f
    val yOffset = (size.height - yCount * spacingPx) / 2f
    onDrawBehind {
        for (i in 0..xCount) {
            for (j in 0..yCount) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadiusPx,
                    center = Offset(xOffset + i * spacingPx, yOffset + j * spacingPx),
                )
            }
        }
    }
}
