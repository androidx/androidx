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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.core.locationbutton.R

/**
 * Renders a standard Material 3 Button locally on platforms before
 * [Build.VERSION_CODES.CINNAMON_BUN].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    BoxWithConstraints(modifier = modifier) {
        val layoutDirection = LocalLayoutDirection.current

        val startPadding =
            clickablePadding.calculateStartPadding(layoutDirection).coerceIn(MinPadding, MaxPadding)
        val endPadding =
            clickablePadding.calculateEndPadding(layoutDirection).coerceIn(MinPadding, MaxPadding)
        val topPadding = clickablePadding.calculateTopPadding().coerceIn(MinPadding, MaxPadding)
        val bottomPadding =
            clickablePadding.calculateBottomPadding().coerceIn(MinPadding, MaxPadding)

        val totalWidth = if (maxWidth != Dp.Infinity) maxWidth else ButtonDefaults.MinWidth
        val totalHeight = if (maxHeight != Dp.Infinity) maxHeight else ButtonDefaults.MinHeight

        val visualWidth = maxOf(0.dp, totalWidth - startPadding - endPadding)
        val visualHeight = maxOf(0.dp, totalHeight - topPadding - bottomPadding)

        val referenceHeight = minOf(visualWidth, visualHeight)

        val textResId = getTextResId(textType)

        val defaultShapes = ButtonDefaults.shapesFor(referenceHeight)
        val shape =
            if (cornerRadius.isSpecified) {
                RoundedCornerShape(cornerRadius)
            } else {
                defaultShapes.shape
            }

        val pressedShape =
            if (pressedCornerRadius.isSpecified) {
                RoundedCornerShape(pressedCornerRadius)
            } else {
                defaultShapes.pressedShape
            }

        val contentPadding =
            if (textResId != null) {
                val defaultPadding = ButtonDefaults.contentPaddingFor(referenceHeight)
                PaddingValues(
                    start = defaultPadding.calculateStartPadding(layoutDirection),
                    top = 0.dp,
                    end = defaultPadding.calculateEndPadding(layoutDirection),
                    bottom = 0.dp,
                )
            } else {
                PaddingValues()
            }

        val border =
            if (strokeWidth.isSpecified && strokeWidth > 0.dp && strokeColor.isSpecified) {
                BorderStroke(strokeWidth, strokeColor)
            } else {
                null
            }

        Button(
            onClick = onClick,
            modifier =
                Modifier.padding(
                        start = startPadding,
                        top = topPadding,
                        end = endPadding,
                        bottom = bottomPadding,
                    )
                    .size(visualWidth, visualHeight),
            shapes = ButtonShapes(shape = shape, pressedShape = pressedShape),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = backgroundColor,
                    contentColor = textColor,
                ),
            border = border,
            contentPadding = contentPadding,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_location_button_my_location),
                contentDescription = null,
                modifier = Modifier.requiredSize(ButtonDefaults.iconSizeFor(referenceHeight)),
                tint = iconTint,
            )
            textResId?.let { resId ->
                Text(
                    text = stringResource(resId),
                    modifier =
                        Modifier.padding(start = ButtonDefaults.iconSpacingFor(referenceHeight)),
                    style = ButtonDefaults.textStyleFor(referenceHeight),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private fun getTextResId(textType: Int): Int? =
    when (textType) {
        LocationButtonTextType.PreciseLocation.value -> R.string.location_button_precise_location
        LocationButtonTextType.UsePreciseLocation.value ->
            R.string.location_button_use_precise_location
        LocationButtonTextType.SharePreciseLocation.value ->
            R.string.location_button_share_precise_location
        LocationButtonTextType.NearMyPreciseLocation.value ->
            R.string.location_button_near_my_precise_location
        LocationButtonTextType.NearYourPreciseLocation.value ->
            R.string.location_button_near_your_precise_location
        else -> null
    }
