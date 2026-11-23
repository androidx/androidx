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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
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
 * The Material 3 button rendered by the location button, shared by both implementations.
 *
 * This is the single definition of what a location button looks like on the client side. It is used
 * by [LocalLocationButton] to render the button on platforms before
 * [android.os.Build.VERSION_CODES.CINNAMON_BUN], and by [RemoteLocationButton] as a hidden
 * measurement twin to determine the wrap content size to request from the system. Keeping both in
 * one place ensures the size the system is asked for matches the size the local fallback renders.
 *
 * All size dependent Material 3 tokens are derived from [referenceHeight]. The system applies the
 * same rule on its side, using the smaller of the two content dimensions, so callers that know
 * their final size should pass that same value to stay visually consistent.
 *
 * Vertical content padding is intentionally zeroed. The content is already centered vertically, and
 * vertical padding clips the text when the button is shorter than the text wants to be.
 *
 * @param onClick called when the button is clicked.
 * @param modifier the [Modifier] applied to the button. Callers supply any sizing here; this
 *   composable never forces a size of its own so that it can wrap its content.
 * @param referenceHeight the height used to select size dependent Material 3 tokens.
 */
@Composable
internal fun LocationButtonContent(
    onClick: () -> Unit,
    modifier: Modifier,
    referenceHeight: Dp,
    backgroundColor: Color,
    strokeColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp,
    pressedCornerRadius: Dp,
    iconTint: Color,
    textType: Int,
    textColor: Color,
) {
    val layoutDirection = LocalLayoutDirection.current
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
        modifier = modifier,
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
                modifier = Modifier.padding(start = ButtonDefaults.iconSpacingFor(referenceHeight)),
                style = ButtonDefaults.textStyleFor(referenceHeight),
                maxLines = 1,
                softWrap = false,
            )
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
