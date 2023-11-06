/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun InlineSliderButton(
    enabled: Boolean,
    onClick: () -> Unit,
    contentAlignment: Alignment,
    buttonControlSize: Dp,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .width(buttonControlSize)
            .fillMaxHeight()
            .repeatableClickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            )
            .then(modifier),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Modifier.drawProgressBar(
    selectedBarColor: State<Color>,
    unselectedBarColor: State<Color>,
    barSeparatorColor: State<Color>,
    visibleSegments: Int,
    valueRatio: Float,
    direction: LayoutDirection,
    drawSelectedProgressBar:
        (color: Color, valueRatio: Float, direction: LayoutDirection, drawScope: DrawScope) -> Unit,
    drawUnselectedProgressBar:
        (color: Color, valueRatio: Float, direction: LayoutDirection, drawScope: DrawScope) -> Unit,
    drawProgressBarSeparator: (color: Color, position: Float, drawScope: DrawScope) -> Unit,
): Modifier = drawWithContent {
    drawUnselectedProgressBar(unselectedBarColor.value, valueRatio, direction, this)

    drawSelectedProgressBar(selectedBarColor.value, valueRatio, direction, this)

    for (separator in 1 until visibleSegments) {
        val x = separator * size.width / visibleSegments
        drawProgressBarSeparator(barSeparatorColor.value, x, this)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> directedValue(layoutDirection: LayoutDirection, ltrValue: T, rtlValue: T): T =
    if (layoutDirection == LayoutDirection.Ltr) ltrValue else rtlValue
