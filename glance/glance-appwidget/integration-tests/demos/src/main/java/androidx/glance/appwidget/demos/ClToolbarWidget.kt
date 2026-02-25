/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("ComposableLambdaParameterNaming", "ComposableLambdaParameterPosition")

package androidx.glance.appwidget.demos

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider

/**
 * Internal copy of a Canonical Layout for testing. See the github repo for the official samples.
 * https://github.com/android/platform-samples/tree/main/samples/user-interface/appwidgets/src/main/java/com/example/platform/ui/appwidgets/glance
 */
class ClToolBarAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClToolBarAppWidget()
}

class ClToolBarAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { WidgetContent() } }
    }

    override val previewSizeMode =
        SizeMode.Responsive(
            setOf(DpSize(245.dp, 56.dp), DpSize(245.dp, 72.dp), DpSize(296.dp, 72.dp))
        )

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { GlanceTheme { WidgetContent() } }
    }

    @Composable
    fun WidgetContent() {
        ToolBarLayout(
            appName = "App name", // Inlined
            appIconRes = CLIcons.sampleAppLogo,
            headerButton =
                ToolBarButton(
                    iconRes = CLIcons.sampleAddIcon,
                    contentDescription = "add", // Inlined
                    onClick = ActionUtils.actionStartDemoActivity("add button"),
                ),
            buttons =
                listOf(
                    ToolBarButton(
                        iconRes = CLIcons.sampleMicIcon,
                        contentDescription = "mic", // Inlined
                        onClick = ActionUtils.actionStartDemoActivity("mic button"),
                    ),
                    ToolBarButton(
                        iconRes = CLIcons.sampleShareIcon,
                        contentDescription = "share", // Inlined
                        onClick = ActionUtils.actionStartDemoActivity("share button"),
                    ),
                    ToolBarButton(
                        iconRes = CLIcons.sampleVideocamIcon,
                        contentDescription = "video", // Inlined
                        onClick = ActionUtils.actionStartDemoActivity("video button"),
                    ),
                    ToolBarButton(
                        iconRes = CLIcons.sampleCameraIcon,
                        contentDescription = "camera", // Inlined
                        onClick = ActionUtils.actionStartDemoActivity("camera button"),
                    ),
                ),
        )
    }
}

// --- Inlined from ToolBarLayout.kt ---
internal data class ToolBarButton(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
    val onClick: Action,
    val text: String? = null,
)

@Composable
internal fun ToolBarLayout(
    appName: String,
    @DrawableRes appIconRes: Int,
    headerButton: ToolBarButton,
    buttons: List<ToolBarButton>,
) {
    val appIconItem: @Composable () -> Unit = { FluidHeaderAppIcon(iconRes = appIconRes) }
    val headerButtonItem: @Composable () -> Unit = { FluidHeaderIconButton(button = headerButton) }
    val header: @Composable () -> Unit = {
        Header(
            appIconRes = appIconRes,
            actionButton = headerButton,
            title = if (ToolBarLayoutSize.canShowHeaderTitle()) appName else "",
        )
    }
    val buttonItems: List<@Composable () -> Unit> =
        buttons.map {
            { FluidContentIconButton(it, filled = ToolBarLayoutSize.canUseFilledButtons()) }
        }

    when (val layoutSize = ToolBarLayoutSize.fromLocalSize()) {
        ToolBarLayoutSize.HorizontalRow,
        ToolBarLayoutSize.VerticalColumn -> {
            Scaffold(
                modifier = GlanceModifier.padding(vertical = ToolBarLayoutDimens.widgetPadding),
                horizontalPadding = ToolBarLayoutDimens.widgetPadding,
            ) {
                val horizontal = layoutSize == ToolBarLayoutSize.HorizontalRow
                val numberOfItems =
                    ToolBarLayoutSize.numberOfItemsThatFit(
                        horizontal = horizontal,
                        minItemSize = ToolBarLayoutDimens.minButtonSize,
                        spacing = ToolBarLayoutDimens.itemsSpacing,
                    )
                val finalItems =
                    (listOf(appIconItem, headerButtonItem) + buttonItems).take(numberOfItems)
                if (horizontal)
                    SpacedRow(
                        items = finalItems,
                        spacing = ToolBarLayoutDimens.itemsSpacing,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
                else
                    SpacedColumn(
                        items = finalItems,
                        spacing = ToolBarLayoutDimens.itemsSpacing,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
            }
        }
        ToolBarLayoutSize.TwoRowGrid -> {
            val contentButtonsToShow =
                buttonItems.take(ToolBarLayoutSize.numberOfContentButtonsInTwoRowGrid())
            Scaffold(
                modifier = GlanceModifier.padding(vertical = ToolBarLayoutDimens.widgetPadding),
                horizontalPadding = ToolBarLayoutDimens.widgetPadding,
            ) {
                TwoRowGrid(
                    items = listOf(appIconItem, headerButtonItem) + contentButtonsToShow,
                    spacing = ToolBarLayoutDimens.itemsSpacing,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            }
        }
        ToolBarLayoutSize.HeaderTwoRowGrid -> {
            Scaffold(
                modifier = GlanceModifier.padding(bottom = ToolBarLayoutDimens.widgetPadding),
                horizontalPadding = ToolBarLayoutDimens.widgetPadding,
                titleBar = header,
            ) {
                TwoRowGrid(
                    items = buttonItems,
                    spacing = ToolBarLayoutDimens.itemsSpacing,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun Header(appIconRes: Int, title: String, actionButton: ToolBarButton) =
    TitleBar(
        startIcon = ImageProvider(appIconRes),
        title = title,
        iconColor = GlanceTheme.colors.primary,
        actions = {
            PillShapedButton(
                iconImageProvider = ImageProvider(actionButton.iconRes),
                contentDescription = actionButton.contentDescription,
                iconSize = ToolBarLayoutDimens.iconSize,
                backgroundColor =
                    if (ToolBarLayoutSize.canUseFilledButtons()) GlanceTheme.colors.tertiary
                    else ColorProvider(Color.Transparent),
                contentColor = GlanceTheme.colors.onTertiary,
                onClick = actionButton.onClick,
                modifier = GlanceModifier.padding(end = ToolBarLayoutDimens.widgetPadding),
            )
        },
    )

@Composable
private fun FluidHeaderAppIcon(@DrawableRes iconRes: Int) =
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(ToolBarLayoutDimens.iconSize),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
        )
    }

@Composable
private fun FluidHeaderIconButton(button: ToolBarButton) =
    RectangularIconButton(
        imageProvider = ImageProvider(button.iconRes),
        contentDescription = button.contentDescription,
        contentColor =
            if (ToolBarLayoutSize.canUseFilledButtons()) GlanceTheme.colors.onTertiary
            else GlanceTheme.colors.onSecondaryContainer,
        backgroundColor =
            if (ToolBarLayoutSize.canUseFilledButtons()) GlanceTheme.colors.tertiary
            else ColorProvider(Color.Transparent),
        roundedCornerShape = RoundedCornerShape.FULL,
        iconSize = ToolBarLayoutDimens.iconSize,
        modifier = GlanceModifier.fillMaxSize(),
        onClick = button.onClick,
    )

@Composable
private fun FluidContentIconButton(button: ToolBarButton, filled: Boolean = true) =
    RectangularIconButton(
        imageProvider = ImageProvider(button.iconRes),
        contentDescription = button.contentDescription,
        iconSize = ToolBarLayoutDimens.iconSize,
        roundedCornerShape = RoundedCornerShape.MEDIUM,
        backgroundColor =
            if (filled) GlanceTheme.colors.secondaryContainer else ColorProvider(Color.Transparent),
        contentColor = GlanceTheme.colors.onSecondaryContainer,
        onClick = button.onClick,
        modifier = GlanceModifier.fillMaxSize(),
    )

private enum class ToolBarLayoutSize {
    HorizontalRow,
    VerticalColumn,
    TwoRowGrid,
    HeaderTwoRowGrid;

    companion object {
        @Composable
        fun fromLocalSize(): ToolBarLayoutSize {
            val (h, w) = LocalSize.current.run { height to width }
            return if (h < 128.dp) HorizontalRow
            else if (w < 128.dp) VerticalColumn
            else if (h < 172.dp) TwoRowGrid else HeaderTwoRowGrid
        }

        @Composable
        fun canUseFilledButtons(): Boolean =
            LocalSize.current.run { height >= 72.dp && width >= 72.dp }

        @Composable
        fun numberOfItemsThatFit(horizontal: Boolean, minItemSize: Dp, spacing: Dp): Int {
            val size = if (horizontal) LocalSize.current.width else LocalSize.current.height
            return ((size + spacing) / (minItemSize + spacing)).toInt()
        }

        @Composable
        fun numberOfContentButtonsInTwoRowGrid() = if (LocalSize.current.width >= 240.dp) 4 else 2

        @Composable
        fun canShowHeaderTitle() = LocalSize.current.run { width >= 240.dp && height >= 172.dp }
    }
}

private object ToolBarLayoutDimens {
    val minButtonSize = 48.dp
    val widgetPadding = 12.dp
    val itemsSpacing = 8.dp
    val iconSize = 24.dp
}
