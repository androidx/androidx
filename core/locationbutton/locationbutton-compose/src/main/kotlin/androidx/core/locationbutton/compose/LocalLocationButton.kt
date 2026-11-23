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

package androidx.core.locationbutton.compose

import android.os.Build
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders a standard Material 3 Button locally on platforms before
 * [Build.VERSION_CODES.CINNAMON_BUN].
 */
@Composable
internal fun LocalLocationButton(
    onClick: () -> Unit,
    modifier: Modifier,
    backgroundColor: Color,
    strokeColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp,
    pressedCornerRadius: Dp,
    iconTint: Color,
    textType: Int,
    textColor: Color,
    clickablePadding: PaddingValues,
) {
    // BoxWithConstraints is deliberate here. LocationButtonContent selects its Material 3 tokens
    // during composition from referenceHeight, so the composition itself depends on the incoming
    // constraints, which is what subcomposition is for. RemoteLocationButton has no such
    // dependency, since the button is composed by the system, and uses a plain Layout instead.
    BoxWithConstraints(modifier = modifier) {
        val layoutDirection = LocalLayoutDirection.current

        val startPadding =
            clickablePadding.calculateStartPadding(layoutDirection).coerceIn(MinPadding, MaxPadding)
        val endPadding =
            clickablePadding.calculateEndPadding(layoutDirection).coerceIn(MinPadding, MaxPadding)
        val topPadding = clickablePadding.calculateTopPadding().coerceIn(MinPadding, MaxPadding)
        val bottomPadding =
            clickablePadding.calculateBottomPadding().coerceIn(MinPadding, MaxPadding)

        // Only a fixed constraint means the caller asked for a specific size. A bounded constraint,
        // which is what an ordinary Column, Box or Scaffold child receives, is an upper limit, and
        // treating it as a required size is what made the button fill its parent.
        val hasFixedWidth = constraints.hasFixedWidth
        val hasFixedHeight = constraints.hasFixedHeight

        val visualWidth = maxOf(0.dp, maxWidth - startPadding - endPadding)
        val visualHeight = maxOf(0.dp, maxHeight - topPadding - bottomPadding)

        // The system picks the tokens from the smaller of the two content dimensions. The width
        // can't exceed its maximum, so a bounded width limits the tokens even when it isn't fixed.
        val referenceHeight =
            when {
                !hasFixedHeight -> ButtonDefaults.MinHeight
                constraints.hasBoundedWidth -> minOf(visualWidth, visualHeight)
                else -> visualHeight
            }

        val sizeModifier =
            when {
                hasFixedWidth && hasFixedHeight -> Modifier.size(visualWidth, visualHeight)
                hasFixedWidth -> Modifier.width(visualWidth)
                hasFixedHeight -> Modifier.height(visualHeight)
                else -> Modifier
            }

        LocationButtonContent(
            onClick = onClick,
            modifier =
                Modifier.padding(
                        start = startPadding,
                        top = topPadding,
                        end = endPadding,
                        bottom = bottomPadding,
                    )
                    .then(sizeModifier),
            referenceHeight = referenceHeight,
            backgroundColor = backgroundColor,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            cornerRadius = cornerRadius,
            pressedCornerRadius = pressedCornerRadius,
            iconTint = iconTint,
            textType = textType,
            textColor = textColor,
        )
    }
}
