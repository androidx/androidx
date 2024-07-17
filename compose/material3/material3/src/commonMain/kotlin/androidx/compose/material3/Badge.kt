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
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.tokens.BadgeTokens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalRuler
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst

/**
 * Material Design badge box.
 *
 * A badge represents dynamic information such as a number of pending requests in a navigation bar.
 *
 * Badges can be icon only or contain short text.
 *
 * ![Badge
 * image](https://developer.android.com/images/reference/androidx/compose/material3/badge.png)
 *
 * A common use case is to display a badge with navigation bar items. For more information, see
 * [Navigation Bar](https://m3.material.io/components/navigation-bar/overview)
 *
 * A simple icon with badge example looks like:
 *
 * @sample androidx.compose.material3.samples.NavigationBarItemWithBadge
 * @param badge the badge to be displayed - typically a [Badge]
 * @param modifier the [Modifier] to be applied to this BadgedBox
 * @param content the anchor to which this badge will be positioned
 */
@Composable
fun BadgedBox(
    badge: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier.layoutId("anchor"),
                contentAlignment = Alignment.Center,
                content = content
            )
            Box(modifier = Modifier.layoutId("badge"), content = badge)
        },
    ) { measurables, constraints ->
        val badgePlaceable =
            measurables
                .fastFirst { it.layoutId == "badge" }
                .measure(
                    // Measure with loose constraints for height as we don't want the text to take
                    // up more
                    // space than it needs.
                    constraints.copy(minHeight = 0)
                )

        val anchorPlaceable = measurables.fastFirst { it.layoutId == "anchor" }.measure(constraints)

        val firstBaseline = anchorPlaceable[FirstBaseline]
        val lastBaseline = anchorPlaceable[LastBaseline]
        val totalWidth = anchorPlaceable.width
        val totalHeight = anchorPlaceable.height

        layout(
            width = totalWidth,
            height = totalHeight,
            // Provide custom baselines based only on the anchor content to avoid default baseline
            // calculations from including by any badge content.
            alignmentLines = mapOf(FirstBaseline to firstBaseline, LastBaseline to lastBaseline),
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
            val badgeX =
                minOf(
                    anchorPlaceable.width - badgeHorizontalOffset.roundToPx(),
                    BadgeEndRuler.current(Float.POSITIVE_INFINITY).toInt() - badgePlaceable.width
                )

            val badgeY =
                maxOf(
                    -badgePlaceable.height + badgeVerticalOffset.roundToPx(),
                    BadgeTopRuler.current(Float.NEGATIVE_INFINITY).toInt()
                )

            badgePlaceable.placeRelative(badgeX, badgeY)
        }
    }
}

/**
 * A badge represents dynamic information such as a number of pending requests in a navigation bar.
 *
 * Badges can be icon only or contain short text.
 *
 * ![Badge
 * image](https://developer.android.com/images/reference/androidx/compose/material3/badge.png)
 *
 * See [BadgedBox] for a top level layout that will properly place the badge relative to content
 * such as text or an icon.
 *
 * @param modifier the [Modifier] to be applied to this badge
 * @param containerColor the color used for the background of this badge
 * @param contentColor the preferred color for content inside this badge. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
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
    val shape =
        if (content != null) {
            BadgeTokens.LargeShape.value
        } else {
            BadgeTokens.Shape.value
        }

    // Draw badge container.
    Row(
        modifier =
            modifier
                .defaultMinSize(minWidth = size, minHeight = size)
                .background(color = containerColor, shape = shape)
                .then(
                    if (content != null)
                        Modifier.padding(horizontal = BadgeWithContentHorizontalPadding)
                    else Modifier
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (content != null) {
            // Not using Surface composable because it blocks touch propagation behind it.
            val style = BadgeTokens.LargeLabelTextFont.value
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
    val containerColor: Color
        @Composable get() = BadgeTokens.Color.value
}

/*@VisibleForTesting*/
// Leading and trailing text padding when a badge is displaying text that is too long to fit in
// a circular badge, e.g. if badge number is greater than 9.
internal val BadgeWithContentHorizontalPadding = 4.dp

/*@VisibleForTesting*/
// Offsets for badge when there is short or long content
// Horizontally align start/end of text badge 12.dp from the top end corner of its anchor
// Vertical overlap with anchor is 14.dp
internal val BadgeWithContentHorizontalOffset = 12.dp
internal val BadgeWithContentVerticalOffset = 14.dp

/*@VisibleForTesting*/
// Offsets for badge when there is no content
// Horizontally align start/end of icon only badge 6.dp from the end/start edge of anchor
// Vertical overlap with anchor is 6.dp
internal val BadgeOffset = 6.dp

internal val BadgeTopRuler = HorizontalRuler()
internal val BadgeEndRuler = VerticalRuler()

internal fun Modifier.badgeBounds() =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(
            width = placeable.width,
            height = placeable.height,
            rulers = {
                // use provides instead of provideRelative cause we will place relative
                // in the badge code
                BadgeEndRuler provides coordinates.size.width.toFloat()
                BadgeTopRuler provides 0f
            }
        ) {
            placeable.place(0, 0)
        }
    }
