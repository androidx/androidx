/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.glance.appwidget.demos

import android.content.Context
import android.os.Build.VERSION
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import kotlin.math.floor
import kotlin.math.min

/**
 * Internal copy of a Canonical Layout for testing. See the github repo for the official samples.
 * https://github.com/android/platform-samples/tree/main/samples/user-interface/appwidgets/src/main/java/com/example/platform/ui/appwidgets/glance
 */
class ClQuickActionAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { WidgetContent() } }
    }

    @Composable
    fun WidgetContent() {
        QuickActionsLayout(
            titleIconRes = CLIcons.sampleNoteIcon,
            primaryButton =
                QuickActionIconButton(
                    iconRes = CLIcons.sampleSearchIcon,
                    contentDescription = "Search notes",
                    onClick = ActionUtils.actionStartDemoActivity("search notes button"),
                ),
            secondaryButtons =
                listOf(
                    QuickActionIconButton(
                        iconRes = CLIcons.sampleVideocamIcon,
                        contentDescription = "video note",
                        onClick = ActionUtils.actionStartDemoActivity("video note button"),
                    ),
                    QuickActionIconButton(
                        iconRes = CLIcons.sampleDrawIcon,
                        contentDescription = "drawing",
                        onClick = ActionUtils.actionStartDemoActivity("drawing note button"),
                    ),
                    QuickActionIconButton(
                        iconRes = CLIcons.sampleImageIcon,
                        contentDescription = "image",
                        onClick = ActionUtils.actionStartDemoActivity("image button"),
                    ),
                    QuickActionIconButton(
                        iconRes = CLIcons.sampleMicIcon,
                        contentDescription = "audio",
                        onClick = ActionUtils.actionStartDemoActivity("audio button"),
                    ),
                ),
        )
    }
}

class ClQuickActionAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClQuickActionAppWidget()
}

@Composable
fun QuickActionsLayout(
    @DrawableRes titleIconRes: Int,
    primaryButton: QuickActionIconButton,
    secondaryButtons: List<QuickActionIconButton>,
) {
    when (QuickActionsLayoutSize.fromLocalSize()) {
        QuickActionsLayoutSize.MultiRow ->
            MultiRowLayout(
                titleIconRes = titleIconRes,
                primaryButton = primaryButton,
                secondaryButtons = secondaryButtons,
            )

        QuickActionsLayoutSize.SingleRow ->
            SingleRowLayout(
                primaryButton = primaryButton,
                secondaryButtons = secondaryButtons,
                iconButtonStyle = IconButtonStyle.FULL,
            )

        QuickActionsLayoutSize.SingleRowCompact ->
            SingleRowLayout(
                primaryButton = primaryButton,
                secondaryButtons = secondaryButtons,
                iconButtonStyle = IconButtonStyle.COMPACT,
            )
    }
}

data class QuickActionIconButton(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
    val onClick: Action,
)

@Composable
private fun SingleRowLayout(
    primaryButton: QuickActionIconButton,
    secondaryButtons: List<QuickActionIconButton>,
    iconButtonStyle: IconButtonStyle,
) {
    val normalizedWidth: Dp =
        QuickActionsLayoutDimensions.widgetContentWidth +
            QuickActionsLayoutDimensions.contentSpacing
    val normalizedButtonWidth: Dp =
        QuickActionsLayoutDimensions.minIconButtonSize + QuickActionsLayoutDimensions.contentSpacing
    val rowButtonsCount = ((normalizedWidth / normalizedButtonWidth)).toInt()
    val secondaryButtonsCount = (rowButtonsCount - 1).coerceAtLeast(0)
    val secondaryButtonsToShow: List<QuickActionIconButton> =
        secondaryButtons.take(secondaryButtonsCount)

    Scaffold(
        horizontalPadding = QuickActionsLayoutDimensions.widgetPadding,
        modifier =
            GlanceModifier.padding(
                top = QuickActionsLayoutDimensions.widgetPadding,
                bottom = QuickActionsLayoutDimensions.widgetPadding,
            ),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryIconButton(
                primaryButton = primaryButton,
                style = iconButtonStyle,
                modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
            )

            if (secondaryButtonsToShow.isNotEmpty()) {
                HorizontalContentSpacer()

                secondaryButtonsToShow.forEachIndexed { index, quickActionIconButton ->
                    SecondaryIconButton(
                        quickActionIconButton = quickActionIconButton,
                        style = iconButtonStyle,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    )
                    if (
                        secondaryButtonsToShow.size > 1 && index != secondaryButtonsToShow.lastIndex
                    ) {
                        HorizontalContentSpacer()
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiRowLayout(
    @DrawableRes titleIconRes: Int,
    primaryButton: QuickActionIconButton,
    secondaryButtons: List<QuickActionIconButton>,
) {
    val bottomRows: List<List<QuickActionIconButton>> =
        splitSecondaryButtonsInToRows(buttons = secondaryButtons)
    val rowHeight: Dp = calculateRowHeight(numberOfRows = bottomRows.size + 1)

    @Composable
    fun TopRow() {
        val singleRowSize = bottomRows[0].size
        val showAppIcon = singleRowSize > 1
        val buttonWidth: Dp = calculateButtonWidth(numberOfColumns = singleRowSize)
        val primaryButtonWidth: Dp =
            if (showAppIcon) {
                QuickActionsLayoutDimensions.widgetContentWidth -
                    buttonWidth -
                    QuickActionsLayoutDimensions.contentSpacing
            } else {
                buttonWidth
            }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().height(rowHeight),
        ) {
            if (showAppIcon) {
                AppIcon(iconRes = titleIconRes, modifier = GlanceModifier.defaultWeight())
                HorizontalContentSpacer()
            }
            PrimaryIconButton(
                primaryButton = primaryButton,
                style = IconButtonStyle.FULL,
                modifier =
                    GlanceModifier.fillMaxHeight().then(GlanceModifier.width(primaryButtonWidth)),
            )
        }
    }

    @Composable
    fun BottomRow(buttons: List<QuickActionIconButton>) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(rowHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buttons.forEachIndexed { index, quickActionIconButton ->
                SecondaryIconButton(
                    quickActionIconButton = quickActionIconButton,
                    style = IconButtonStyle.FULL,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
                MayHorizontalContentSpacer(show = buttons.size > 1 && index != buttons.lastIndex)
            }
        }
    }

    Scaffold(
        horizontalPadding = QuickActionsLayoutDimensions.widgetPadding,
        modifier =
            GlanceModifier.padding(
                top = QuickActionsLayoutDimensions.widgetPadding,
                bottom = QuickActionsLayoutDimensions.widgetPadding,
            ),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            TopRow()
            VerticalContentSpacer()
            for (i in 0..bottomRows.lastIndex) {
                BottomRow(bottomRows[i])
                MaybeVerticalContentSpacer(show = bottomRows.size > 1 && i != bottomRows.lastIndex)
            }
        }
    }
}

@Composable
private fun VerticalContentSpacer() {
    Spacer(modifier = GlanceModifier.height(QuickActionsLayoutDimensions.contentSpacing))
}

@Composable
private fun MaybeVerticalContentSpacer(show: Boolean) {
    if (show) {
        VerticalContentSpacer()
    }
}

@Composable
private fun HorizontalContentSpacer() {
    Spacer(modifier = GlanceModifier.width(QuickActionsLayoutDimensions.contentSpacing))
}

@Composable
private fun MayHorizontalContentSpacer(show: Boolean) {
    if (show) {
        HorizontalContentSpacer()
    }
}

@Composable
private fun PrimaryIconButton(
    primaryButton: QuickActionIconButton,
    style: IconButtonStyle,
    modifier: GlanceModifier,
) {
    RectangularIconButton(
        imageProvider = ImageProvider(primaryButton.iconRes),
        contentDescription = primaryButton.contentDescription,
        iconSize = QuickActionsLayoutDimensions.iconSize,
        roundedCornerShape = RoundedCornerShape.FULL,
        backgroundColor =
            when (style) {
                IconButtonStyle.FULL -> GlanceTheme.colors.primary
                IconButtonStyle.COMPACT -> ColorProvider(Color.Transparent)
            },
        contentColor =
            when (style) {
                IconButtonStyle.FULL -> GlanceTheme.colors.onPrimary
                IconButtonStyle.COMPACT -> GlanceTheme.colors.primary
            },
        onClick = primaryButton.onClick,
        modifier = modifier,
    )
}

@Composable
private fun SecondaryIconButton(
    quickActionIconButton: QuickActionIconButton,
    style: IconButtonStyle,
    modifier: GlanceModifier,
) {
    RectangularIconButton(
        imageProvider = ImageProvider(quickActionIconButton.iconRes),
        contentDescription = quickActionIconButton.contentDescription,
        iconSize = QuickActionsLayoutDimensions.iconSize,
        roundedCornerShape = RoundedCornerShape.MEDIUM,
        contentColor = GlanceTheme.colors.primary,
        backgroundColor =
            when (style) {
                IconButtonStyle.FULL -> GlanceTheme.colors.secondaryContainer
                IconButtonStyle.COMPACT -> ColorProvider(Color.Transparent)
            },
        onClick = quickActionIconButton.onClick,
        modifier = modifier,
    )
}

@Composable
private fun AppIcon(iconRes: Int, modifier: GlanceModifier) {
    Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.then(modifier)) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(QuickActionsLayoutDimensions.iconSize),
        )
    }
}

@Composable
private fun calculateButtonWidth(numberOfColumns: Int): Dp {
    val widthUsedBySpacing = (numberOfColumns - 1) * QuickActionsLayoutDimensions.contentSpacing
    return (QuickActionsLayoutDimensions.widgetContentWidth - widthUsedBySpacing) / numberOfColumns
}

@Composable
private fun calculateRowHeight(numberOfRows: Int): Dp {
    val heightUsedBySpacing = (numberOfRows - 1) * QuickActionsLayoutDimensions.contentSpacing
    return (QuickActionsLayoutDimensions.widgetContentHeight - heightUsedBySpacing) / numberOfRows
}

@Composable
private fun splitSecondaryButtonsInToRows(
    buttons: List<QuickActionIconButton>
): List<List<QuickActionIconButton>> {
    val rowHeight = 82.dp
    val normalizedWidth: Dp =
        QuickActionsLayoutDimensions.widgetContentWidth +
            QuickActionsLayoutDimensions.contentSpacing
    val normalizedHeight: Dp =
        QuickActionsLayoutDimensions.widgetContentHeight +
            QuickActionsLayoutDimensions.contentSpacing
    val normalizedMinButtonWidth: Dp =
        QuickActionsLayoutDimensions.minIconButtonSize + QuickActionsLayoutDimensions.contentSpacing
    val normalizedRowHeight: Dp = rowHeight + QuickActionsLayoutDimensions.contentSpacing
    val maxButtonsPerRow = floor(normalizedWidth / normalizedMinButtonWidth).toInt()
    val maxRows = floor(normalizedHeight / normalizedRowHeight).toInt() - 1
    var rowsToUse = min(buttons.size, maxRows).coerceAtLeast(1)
    while ((buttons.size % rowsToUse) != 0 && rowsToUse > 1) {
        rowsToUse--
    }

    val finalButtonsPerRow = min((buttons.size / rowsToUse), maxButtonsPerRow).coerceAtLeast(1)

    return buttons.chunked(finalButtonsPerRow).take(rowsToUse)
}

@Composable
private fun GlanceModifier.widgetContainer(): GlanceModifier {
    var modifier =
        GlanceModifier.fillMaxSize()
            .padding(QuickActionsLayoutDimensions.widgetPadding)
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)

    if (VERSION.SDK_INT >= 31) {
        modifier = modifier.widgetCornerRadius()
    }

    return this.then(modifier)
}

@Composable
@RequiresApi(31)
private fun GlanceModifier.widgetCornerRadius(): GlanceModifier {
    return this.then(
        GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    )
}

private enum class IconButtonStyle {
    FULL,
    COMPACT,
}

private enum class QuickActionsLayoutSize {
    MultiRow,
    SingleRow,
    SingleRowCompact;

    companion object {
        @Composable
        fun fromLocalSize(): QuickActionsLayoutSize {
            val height = LocalSize.current.height

            return if (height >= 178.dp) {
                MultiRow
            } else if (height >= 82.dp) {
                SingleRow
            } else {
                SingleRowCompact
            }
        }
    }
}

private object QuickActionsLayoutDimensions {
    val secondaryButtonCornerRadius = 16.dp
    val primaryButtonCornerRadius = 100.dp
    val iconSize = 24.dp

    val widgetPadding: Dp
        @Composable
        get() =
            when (QuickActionsLayoutSize.fromLocalSize()) {
                QuickActionsLayoutSize.SingleRowCompact -> 0.dp
                else -> 12.dp
            }

    val contentSpacing = 8.dp
    val minIconButtonSize = 48.dp

    val widgetContentWidth: Dp
        @Composable get() = LocalSize.current.width - (2 * widgetPadding)

    val widgetContentHeight: Dp
        @Composable get() = LocalSize.current.height - (2 * widgetPadding)
}
