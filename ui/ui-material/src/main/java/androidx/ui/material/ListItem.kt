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

package androidx.ui.material

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.Image
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.graphics.ImageAsset
import androidx.ui.layout.Row
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeightIn
import androidx.ui.layout.preferredSizeIn
import androidx.ui.layout.preferredWidthIn
import androidx.ui.material.ripple.RippleIndication
import androidx.ui.text.FirstBaseline
import androidx.ui.text.LastBaseline
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextOverflow
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.util.fastForEachIndexed

/**
 * Material Design implementation of [list items](https://material.io/components/lists).
 *
 * This component can be used to achieve the list item templates existing in the spec. For example:
 * - one-line items
 * @sample androidx.ui.material.samples.OneLineListItems
 * - two-line items
 * @sample androidx.ui.material.samples.TwoLineListItems
 * - three-line items
 * @sample androidx.ui.material.samples.ThreeLineListItems
 *
 * @param text The primary text of the list item
 * @param modifier Modifier to be applied to the list item
 * @param onClick Callback to be invoked when the list item is clicked
 * @param icon The leading supporting visual of the list item
 * @param secondaryText The secondary text of the list item
 * @param singleLineSecondaryText Whether the secondary text is single line
 * @param overlineText The text displayed above the primary text
 * @param metaText The meta text to be displayed in the trailing position
 */
@Composable
fun ListItem(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: ImageAsset? = null,
    secondaryText: String? = null,
    // TODO(popam): find a way to remove this
    singleLineSecondaryText: Boolean = true,
    overlineText: String? = null,
    metaText: String? = null
) {
    val iconComposable: @Composable (() -> Unit)? = icon?.let {
        { Image(it) }
    }
    val textComposable: @Composable () -> Unit = text.let {
        { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
    val secondaryTextComposable: @Composable (() -> Unit)? = secondaryText?.let {
        {
            val maxLines = if (!singleLineSecondaryText && overlineText == null) 2 else 1
            Text(it, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        }
    }
    val overlineTextComposable: @Composable (() -> Unit)? = overlineText?.let {
        { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
    val metaTextComposable: @Composable (() -> Unit)? = metaText?.let {
        { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
    ListItem(
        modifier,
        onClick,
        iconComposable,
        secondaryTextComposable,
        singleLineSecondaryText,
        overlineTextComposable,
        metaTextComposable,
        textComposable
    )
}

/**
 * Material Design implementation of [list items](https://material.io/components/lists).
 *
 * This component can be used to achieve the list item templates existing in the spec. For example:
 * - one-line items
 * @sample androidx.ui.material.samples.OneLineListItems
 * - two-line items
 * @sample androidx.ui.material.samples.TwoLineListItems
 * - three-line items
 * @sample androidx.ui.material.samples.ThreeLineListItems
 *
 * @param modifier Modifier to be applied to the list item
 * @param onClick Callback to be invoked when the list item is clicked
 * @param icon The leading supporting visual of the list item
 * @param secondaryText The secondary text of the list item
 * @param singleLineSecondaryText Whether the secondary text is single line
 * @param overlineText The text displayed above the primary text
 * @param trailing The trailing meta text or meta icon of the list item
 * @param text The primary text of the list item
 */
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    secondaryText: @Composable (() -> Unit)? = null,
    singleLineSecondaryText: Boolean = true,
    overlineText: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)
) {
    val emphasisLevels = EmphasisAmbient.current
    val typography = MaterialTheme.typography

    val styledText = applyTextStyle(typography.subtitle1, emphasisLevels.high, text)!!
    val styledSecondaryText = applyTextStyle(typography.body2, emphasisLevels.medium, secondaryText)
    val styledOverlineText = applyTextStyle(typography.overline, emphasisLevels.high, overlineText)
    val styledTrailing = applyTextStyle(typography.caption, emphasisLevels.high, trailing)

    val item = @Composable {
        if (styledSecondaryText == null && styledOverlineText == null) {
            OneLine.ListItem(modifier, icon, styledText, styledTrailing)
        } else if ((styledOverlineText == null && singleLineSecondaryText) ||
            styledSecondaryText == null
        ) {
            TwoLine.ListItem(
                modifier,
                icon,
                styledText,
                styledSecondaryText,
                styledOverlineText,
                styledTrailing
            )
        } else {
            ThreeLine.ListItem(
                modifier,
                icon,
                styledText,
                styledSecondaryText,
                styledOverlineText,
                styledTrailing
            )
        }
    }

    if (onClick != null) {
        val indication = RippleIndication(
            color = MaterialTheme.colors.onSurface.copy(alpha = RippleOpacity)
        )
        Box(Modifier
            .semantics(mergeAllDescendants = true)
            .clickable(onClick = onClick, indication = indication), children = item)
    } else {
        item()
    }
}

private object OneLine {
    // TODO(popam): support wide icons
    // TODO(popam): convert these to sp
    // List item related constants.
    private val MinHeight = 48.dp
    private val MinHeightWithIcon = 56.dp
    // Icon related constants.
    private val IconMinPaddedWidth = 40.dp
    private val IconLeftPadding = 16.dp
    private val IconVerticalPadding = 8.dp
    // Content related constants.
    private val ContentLeftPadding = 16.dp
    private val ContentRightPadding = 16.dp
    // Trailing related constants.
    private val TrailingRightPadding = 16.dp

    @Composable
    fun ListItem(
        modifier: Modifier = Modifier,
        icon: @Composable (() -> Unit)?,
        text: @Composable (() -> Unit),
        trailing: @Composable (() -> Unit)?
    ) {
        val minHeight = if (icon == null) MinHeight else MinHeightWithIcon
        Row(modifier.preferredHeightIn(minHeight = minHeight)) {
            if (icon != null) {
                Box(
                    Modifier.gravity(Alignment.CenterVertically)
                        .preferredWidthIn(minWidth = IconLeftPadding + IconMinPaddedWidth),
                    gravity = ContentGravity.CenterStart,
                    paddingStart = IconLeftPadding,
                    paddingTop = IconVerticalPadding,
                    paddingBottom = IconVerticalPadding,
                    children = icon
                )
            }
            Box(
                Modifier.weight(1f)
                    .gravity(Alignment.CenterVertically)
                    .padding(start = ContentLeftPadding, end = ContentRightPadding),
                gravity = ContentGravity.CenterStart,
                children = text
            )
            if (trailing != null) {
                Box(
                    Modifier.gravity(Alignment.CenterVertically),
                    paddingEnd = TrailingRightPadding,
                    children = trailing
                )
            }
        }
    }
}

private object TwoLine {
    // List item related constants.
    private val MinHeight = 64.dp
    private val MinHeightWithIcon = 72.dp
    // Icon related constants.
    private val IconMinPaddedWidth = 40.dp
    private val IconLeftPadding = 16.dp
    private val IconVerticalPadding = 16.dp
    // Content related constants.
    private val ContentLeftPadding = 16.dp
    private val ContentRightPadding = 16.dp
    private val OverlineBaselineOffset = 24.dp
    private val OverlineToPrimaryBaselineOffset = 20.dp
    private val PrimaryBaselineOffsetNoIcon = 28.dp
    private val PrimaryBaselineOffsetWithIcon = 32.dp
    private val PrimaryToSecondaryBaselineOffsetNoIcon = 20.dp
    private val PrimaryToSecondaryBaselineOffsetWithIcon = 20.dp
    // Trailing related constants.
    private val TrailingRightPadding = 16.dp

    @Composable
    fun ListItem(
        modifier: Modifier = Modifier,
        icon: @Composable (() -> Unit)?,
        text: @Composable (() -> Unit),
        secondaryText: @Composable (() -> Unit)?,
        overlineText: @Composable (() -> Unit)?,
        trailing: @Composable (() -> Unit)?
    ) {
        val minHeight = if (icon == null) MinHeight else MinHeightWithIcon
        Row(modifier.preferredHeightIn(minHeight = minHeight)) {
            val columnModifier = Modifier.weight(1f)
                .padding(start = ContentLeftPadding, end = ContentRightPadding)

            if (icon != null) {
                Box(
                    Modifier.preferredSizeIn(
                        minWidth = IconLeftPadding + IconMinPaddedWidth,
                        minHeight = minHeight
                    ),
                    gravity = ContentGravity.TopStart,
                    paddingStart = IconLeftPadding,
                    paddingTop = IconVerticalPadding,
                    paddingBottom = IconVerticalPadding,
                    children = icon
                )
            }

            if (overlineText != null) {
                BaselinesOffsetColumn(
                    listOf(OverlineBaselineOffset, OverlineToPrimaryBaselineOffset),
                    columnModifier
                ) {
                    overlineText()
                    text()
                }
            } else {
                BaselinesOffsetColumn(
                    listOf(
                        if (icon != null) {
                            PrimaryBaselineOffsetWithIcon
                        } else {
                            PrimaryBaselineOffsetNoIcon
                        },
                        if (icon != null) {
                            PrimaryToSecondaryBaselineOffsetWithIcon
                        } else {
                            PrimaryToSecondaryBaselineOffsetNoIcon
                        }
                    ),
                    columnModifier
                ) {
                    text()
                    secondaryText!!()
                }
            }
            if (trailing != null) {
                OffsetToBaselineOrCenter(
                    if (icon != null) {
                        PrimaryBaselineOffsetWithIcon
                    } else {
                        PrimaryBaselineOffsetNoIcon
                    }
                ) {
                    Box(
                        // TODO(popam): find way to center and wrap content without minHeight
                        Modifier.preferredHeightIn(minHeight = minHeight)
                            .padding(end = TrailingRightPadding),
                        gravity = ContentGravity.Center,
                        children = trailing
                    )
                }
            }
        }
    }
}

private object ThreeLine {
    // List item related constants.
    private val MinHeight = 88.dp
    // Icon related constants.
    private val IconMinPaddedWidth = 40.dp
    private val IconLeftPadding = 16.dp
    private val IconThreeLineVerticalPadding = 16.dp
    // Content related constants.
    private val ContentLeftPadding = 16.dp
    private val ContentRightPadding = 16.dp
    private val ThreeLineBaselineFirstOffset = 28.dp
    private val ThreeLineBaselineSecondOffset = 20.dp
    private val ThreeLineBaselineThirdOffset = 20.dp
    private val ThreeLineTrailingTopPadding = 16.dp
    // Trailing related constants.
    private val TrailingRightPadding = 16.dp

    @Composable
    fun ListItem(
        modifier: Modifier = Modifier,
        icon: @Composable (() -> Unit)?,
        text: @Composable (() -> Unit),
        secondaryText: @Composable (() -> Unit),
        overlineText: @Composable (() -> Unit)?,
        trailing: @Composable (() -> Unit)?
    ) {
        Row(modifier.preferredHeightIn(minHeight = MinHeight)) {
            if (icon != null) {
                val minSize = IconLeftPadding + IconMinPaddedWidth
                Box(
                    Modifier.preferredSizeIn(minWidth = minSize, minHeight = minSize),
                    gravity = ContentGravity.CenterStart,
                    paddingStart = IconLeftPadding,
                    paddingTop = IconThreeLineVerticalPadding,
                    paddingBottom = IconThreeLineVerticalPadding,
                    children = icon
                )
            }
            BaselinesOffsetColumn(
                listOf(
                    ThreeLineBaselineFirstOffset,
                    ThreeLineBaselineSecondOffset,
                    ThreeLineBaselineThirdOffset
                ),
                Modifier.weight(1f)
                    .padding(start = ContentLeftPadding, end = ContentRightPadding)
            ) {
                if (overlineText != null) overlineText()
                text()
                secondaryText()
            }
            if (trailing != null) {
                OffsetToBaselineOrCenter(
                    ThreeLineBaselineFirstOffset - ThreeLineTrailingTopPadding,
                    Modifier.padding(top = ThreeLineTrailingTopPadding, end = TrailingRightPadding),
                    trailing
                )
            }
        }
    }
}

/**
 * Layout that expects [Text] children, and positions them with specific offsets between the
 * top of the layout and the first text, as well as the last baseline and first baseline
 * for subsequent pairs of texts.
 */
// TODO(popam): consider making this a layout composable in ui-layout.
@Composable
private fun BaselinesOffsetColumn(
    offsets: List<Dp>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content, modifier) { measurables, constraints, _ ->
        val childConstraints = constraints.copy(minHeight = 0.ipx, maxHeight = IntPx.Infinity)
        val placeables = measurables.map { it.measure(childConstraints) }

        val containerWidth = placeables.fold(0.ipx) { maxWidth, placeable ->
            max(maxWidth, placeable.width)
        }
        val y = Array(placeables.size) { 0.ipx }
        var containerHeight = 0.ipx
        placeables.fastForEachIndexed { index, placeable ->
            val toPreviousBaseline = if (index > 0) {
                placeables[index - 1].height - placeables[index - 1][LastBaseline]!!
            } else 0.ipx
            val topPadding = max(
                0.ipx,
                offsets[index].toIntPx() - placeable[FirstBaseline]!! - toPreviousBaseline
            )
            y[index] = topPadding + containerHeight
            containerHeight += topPadding + placeable.height
        }

        layout(containerWidth, containerHeight) {
            placeables.fastForEachIndexed { index, placeable ->
                placeable.place(0.ipx, y[index])
            }
        }
    }
}

/**
 * Layout that takes a child and adds the necessary padding such that the first baseline of the
 * child is at a specific offset from the top of the container. If the child does not have
 * a first baseline, the layout will match the minHeight constraint and will center the
 * child.
 */
// TODO(popam): support fallback alignment in AlignmentLineOffset, and use that here.
@Composable
private fun OffsetToBaselineOrCenter(
    offset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content, modifier) { measurables, constraints, _ ->
        val placeable = measurables[0].measure(constraints.copy(minHeight = 0.ipx))
        val baseline = placeable[FirstBaseline]
        val y: IntPx
        val containerHeight: IntPx
        if (baseline != null) {
            y = offset.toIntPx() - baseline
            containerHeight = max(constraints.minHeight, y + placeable.height)
        } else {
            containerHeight = max(constraints.minHeight, placeable.height)
            y = Alignment.Center
                .align(IntPxSize(0.ipx, containerHeight - placeable.height)).y
        }
        layout(placeable.width, containerHeight) {
            placeable.place(0.ipx, y)
        }
    }
}

private fun applyTextStyle(
    textStyle: TextStyle,
    emphasis: Emphasis,
    icon: @Composable (() -> Unit)?
): @Composable (() -> Unit)? {
    if (icon == null) return null
    return {
        ProvideEmphasis(emphasis) {
            ProvideTextStyle(textStyle, icon)
        }
    }
}

// Material spec values.
private const val RippleOpacity = 0.16f
