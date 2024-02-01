/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.tokens.BadgeTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import kotlin.math.roundToInt

/**
 * Material Design badge box.
 *
 * A badge represents dynamic information such as a number of pending requests in a navigation bar.
 *
 * Badges can be icon only or contain short text.
 *
 * ![Badge image](https://developer.android.com/images/reference/androidx/compose/material3/badge.png)
 *
 * A common use case is to display a badge with navigation bar items.
 * For more information, see [Navigation Bar](https://m3.material.io/components/navigation-bar/overview)
 *
 * A simple icon with badge example looks like:
 * @sample androidx.compose.material3.samples.NavigationBarItemWithBadge
 *
 * @param badge the badge to be displayed - typically a [Badge]
 * @param modifier the [Modifier] to be applied to this BadgedBox
 * @param content the anchor to which this badge will be positioned
 *
 */
@Composable
fun BadgedBox(
    badge: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var layoutAbsoluteLeft by remember { mutableFloatStateOf(0f) }
    var layoutAbsoluteTop by remember { mutableFloatStateOf(0f) }
    // We use Float.POSITIVE_INFINITY and Float.NEGATIVE_INFINITY to represent the case
    // when there isn't a great grand parent layout.
    var greatGrandParentAbsoluteRight by remember { mutableFloatStateOf(Float.POSITIVE_INFINITY) }
    var greatGrandParentAbsoluteTop by remember { mutableFloatStateOf(Float.NEGATIVE_INFINITY) }

    Layout(
        {
            Box(
                modifier = Modifier.layoutId("anchor"),
                contentAlignment = Alignment.Center,
                content = content
            )
            Box(
                modifier = Modifier.layoutId("badge"),
                content = badge
            )
        },
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                layoutAbsoluteLeft = coordinates.boundsInWindow().left
                layoutAbsoluteTop = coordinates.boundsInWindow().top
                val layoutGreatGrandParent =
                    coordinates.parentLayoutCoordinates?.parentLayoutCoordinates?.parentCoordinates
                layoutGreatGrandParent?.let {
                    greatGrandParentAbsoluteRight = it.boundsInWindow().right
                    greatGrandParentAbsoluteTop = it.boundsInWindow().top
                }
            }
    ) { measurables, constraints ->

        val badgePlaceable = measurables.fastFirst { it.layoutId == "badge" }.measure(
            // Measure with loose constraints for height as we don't want the text to take up more
            // space than it needs.
            constraints.copy(minHeight = 0)
        )

        val anchorPlaceable = measurables.fastFirst { it.layoutId == "anchor" }.measure(constraints)

        val firstBaseline = anchorPlaceable[FirstBaseline]
        val lastBaseline = anchorPlaceable[LastBaseline]
        val totalWidth = anchorPlaceable.width
        val totalHeight = anchorPlaceable.height

        layout(
            totalWidth,
            totalHeight,
            // Provide custom baselines based only on the anchor content to avoid default baseline
            // calculations from including by any badge content.
            mapOf(
                FirstBaseline to firstBaseline,
                LastBaseline to lastBaseline
            )
        ) {
            // Use the width of the badge to infer whether it has any content (based on radius used
            // in [Badge]) and determine its horizontal offset.
            val hasContent = badgePlaceable.width > (BadgeTokens.Size.roundToPx())
            val badgeHorizontalOffset =
                if (hasContent) BadgeWithContentHorizontalOffset else BadgeOffset
            val badgeVerticalOffset =
                if (hasContent) BadgeWithContentVerticalOffset else BadgeOffset

            anchorPlaceable.placeRelative(0, 0)

            // Desired Badge placement
            var badgeX = anchorPlaceable.width + badgeHorizontalOffset.roundToPx()
            var badgeY = -badgePlaceable.height / 2 + badgeVerticalOffset.roundToPx()
            // Badge correction logic if the badge will be cut off by the grandparent bounds.
            val badgeAbsoluteTop = layoutAbsoluteTop + badgeY
            val badgeAbsoluteRight = layoutAbsoluteLeft + badgeX + badgePlaceable.width.toFloat()
            val badgeGreatGrandParentHorizontalDiff =
                greatGrandParentAbsoluteRight - badgeAbsoluteRight
            val badgeGreatGrandParentVerticalDiff =
                badgeAbsoluteTop - greatGrandParentAbsoluteTop
            // Adjust badgeX and badgeY if the desired placement would cause it to clip.
            if (badgeGreatGrandParentHorizontalDiff < 0) {
                badgeX += badgeGreatGrandParentHorizontalDiff.roundToInt()
            }
            if (badgeGreatGrandParentVerticalDiff < 0) {
                badgeY -= badgeGreatGrandParentVerticalDiff.roundToInt()
            }

            badgePlaceable.placeRelative(badgeX, badgeY)
        }
    }
}

/**
 * A badge represents dynamic information such as a number of pending requests in a navigation bar.
 *
 * Badges can be icon only or contain short text.
 *
 * ![Badge image](https://developer.android.com/images/reference/androidx/compose/material3/badge.png)
 *
 * See [BadgedBox] for a top level layout that will properly place the badge relative to content
 * such as text or an icon.
 *
 * @param modifier the [Modifier] to be applied to this badge
 * @param containerColor the color used for the background of this badge
 * @param contentColor the preferred color for content inside this badge. Defaults to either the
 * matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param content optional content to be rendered inside this badge
 */
@Composable
fun Badge(
    modifier: Modifier = Modifier,
    containerColor: Color = BadgeDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    val size = if (content != null) BadgeTokens.LargeSize else BadgeTokens.Size
    val shape = if (content != null) {
        BadgeTokens.LargeShape.value
    } else {
        BadgeTokens.Shape.value
    }

    // Draw badge container.
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = size, minHeight = size)
            .background(
                color = containerColor,
                shape = shape
            )
            .clip(shape)
            .then(
                if (content != null)
                    Modifier.padding(horizontal = BadgeWithContentHorizontalPadding) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (content != null) {
            // Not using Surface composable because it blocks touch propagation behind it.
            val style = MaterialTheme.typography.fromToken(BadgeTokens.LargeLabelTextFont)
            ProvideContentColorTextStyle(
                contentColor = contentColor,
                textStyle = style,
                content = { content() }
            )
        }
    }
}

/** Default values used for [Badge] implementations. */
object BadgeDefaults {
    /** Default container color for a badge. */
    val containerColor: Color @Composable get() = BadgeTokens.Color.value
}

/*@VisibleForTesting*/
// Leading and trailing text padding when a badge is displaying text that is too long to fit in
// a circular badge, e.g. if badge number is greater than 9.
internal val BadgeWithContentHorizontalPadding = 4.dp

/*@VisibleForTesting*/
// Horizontally align start/end of text badge 6dp from the top end corner of its anchor
internal val BadgeWithContentHorizontalOffset = -6.dp
internal val BadgeWithContentVerticalOffset = 6.dp

/*@VisibleForTesting*/
// Horizontally align start/end of icon only badge 0.dp from the end/start edge of anchor
internal val BadgeOffset = 0.dp
