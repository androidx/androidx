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
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Internal copy of a Canonical Layout for testing. See the github repo for the official samples.
 * https://github.com/android/platform-samples/tree/main/samples/user-interface/appwidgets/src/main/java/com/example/platform/ui/appwidgets/glance
 */
class ClSearchToolBarAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { WidgetContent() } }
    }

    override val previewSizeMode =
        SizeMode.Responsive(
            setOf(DpSize(240.dp, 72.dp), DpSize(128.dp, 128.dp), DpSize(184.dp, 188.dp))
        )

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { GlanceTheme { WidgetContent() } }
    }

    @Composable
    fun WidgetContent() {
        SearchToolBarLayout(
            searchButton =
                SearchToolBarButton(
                    iconRes = CLIcons.sampleSearchIcon,
                    contentDescription = "Search notes",
                    text = "Search",
                    onClick = ActionUtils.actionStartDemoActivity("search notes button"),
                ),
            trailingButtons =
                listOf(
                    SearchToolBarButton(
                        iconRes = CLIcons.sampleMicIcon,
                        contentDescription = "audio",
                        onClick = ActionUtils.actionStartDemoActivity("audio button"),
                    ),
                    SearchToolBarButton(
                        iconRes = CLIcons.sampleVideocamIcon,
                        contentDescription = "video note",
                        onClick = ActionUtils.actionStartDemoActivity("video note button"),
                    ),
                    SearchToolBarButton(
                        iconRes = CLIcons.sampleCameraIcon,
                        contentDescription = "camera",
                        onClick = ActionUtils.actionStartDemoActivity("camera button"),
                    ),
                    SearchToolBarButton(
                        iconRes = CLIcons.sampleShareIcon,
                        contentDescription = "share",
                        onClick = ActionUtils.actionStartDemoActivity("share button"),
                    ),
                ),
        )
    }
}

class ClSearchToolBarAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClSearchToolBarAppWidget()
}

// --- Inlined from SearchToolBarLayout.kt ---
internal data class SearchToolBarButton(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
    val onClick: Action,
    val text: String? = null,
)

@Composable
internal fun SearchToolBarLayout(
    searchButton: SearchToolBarButton,
    trailingButtons: List<SearchToolBarButton>,
) {
    val searchButtonItem: @Composable () -> Unit = {
        if (SearchToolBarLayoutSize.canShowSearchText()) SearchBar(searchButton = searchButton)
        else
            SearchIconButton(
                searchButton = searchButton,
                filled = SearchToolBarLayoutSize.canUseFilledButtons(),
            )
    }
    val trailingButtonItems: List<@Composable () -> Unit> =
        trailingButtons.map {
            { TrailingButton(button = it, filled = SearchToolBarLayoutSize.canUseFilledButtons()) }
        }

    Scaffold(
        modifier = GlanceModifier.padding(vertical = SearchToolBarLayoutDimens.widgetPadding),
        horizontalPadding = SearchToolBarLayoutDimens.widgetPadding,
    ) {
        when (val layoutSize = SearchToolBarLayoutSize.fromLocalSize()) {
            SearchToolBarLayoutSize.HorizontalRow,
            SearchToolBarLayoutSize.VerticalColumn -> {
                val horizontal = (layoutSize == SearchToolBarLayoutSize.HorizontalRow)
                val numberOfItems =
                    SearchToolBarLayoutSize.numberOfItemsThatFit(
                        horizontal = horizontal,
                        minItemSize = SearchToolBarLayoutDimens.minButtonSize,
                        spacing = SearchToolBarLayoutDimens.itemsSpacing,
                    )
                val finalItems =
                    (listOf(searchButtonItem) + trailingButtonItems).take(numberOfItems)
                if (horizontal)
                    SpacedRow(
                        items = finalItems,
                        spacing = SearchToolBarLayoutDimens.itemsSpacing,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
                else
                    SpacedColumn(
                        items = finalItems,
                        spacing = SearchToolBarLayoutDimens.itemsSpacing,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
            }
            SearchToolBarLayoutSize.TwoByTwoGrid ->
                TwoRowGrid(
                    items = listOf(searchButtonItem) + trailingButtonItems.take(3),
                    spacing = SearchToolBarLayoutDimens.itemsSpacing,
                )
            SearchToolBarLayoutSize.SideBarTwoRowGrid ->
                SideBarTwoRowGrid(
                    sideBarItem = searchButtonItem,
                    items = trailingButtonItems.take(4),
                    sideBarWidth = SearchToolBarLayoutDimens.sideBarLeadingItemWidth,
                    spacing = SearchToolBarLayoutDimens.itemsSpacing,
                )
            SearchToolBarLayoutSize.HeaderTwoRowGrid ->
                HeaderTwoRowGrid(
                    headerItem = searchButtonItem,
                    items = trailingButtonItems.take(4),
                    headerHeight = SearchToolBarLayoutDimens.headerItemHeight,
                    spacing = SearchToolBarLayoutDimens.itemsSpacing,
                )
        }
    }
}

@Composable
private fun SearchIconButton(searchButton: SearchToolBarButton, filled: Boolean) =
    RectangularIconButton(
        imageProvider = ImageProvider(searchButton.iconRes),
        contentDescription = searchButton.contentDescription,
        backgroundColor =
            if (filled) GlanceTheme.colors.tertiary else ColorProvider(Color.Transparent),
        contentColor =
            if (filled) GlanceTheme.colors.onTertiary else GlanceTheme.colors.onSecondaryContainer,
        iconSize = SearchToolBarLayoutDimens.iconSize,
        roundedCornerShape = RoundedCornerShape.FULL,
        onClick = searchButton.onClick,
        modifier = GlanceModifier.fillMaxSize(),
    )

@Composable
private fun SearchBar(searchButton: SearchToolBarButton) =
    Row(
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            GlanceModifier.fillMaxSize()
                .cornerRadius(RoundedCornerShape.FULL.cornerRadius)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .semantics { this.contentDescription = searchButton.contentDescription }
                .clickable(searchButton.onClick),
    ) {
        Image(
            provider = ImageProvider(searchButton.iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(SearchToolBarLayoutDimens.iconSize),
        )
        searchButton.text?.let {
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = it,
                maxLines = 1,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }
    }

@Composable
private fun TrailingButton(button: SearchToolBarButton, filled: Boolean = true) =
    RectangularIconButton(
        imageProvider = ImageProvider(button.iconRes),
        contentDescription = button.contentDescription,
        backgroundColor =
            if (filled) GlanceTheme.colors.secondaryContainer else ColorProvider(Color.Transparent),
        contentColor = GlanceTheme.colors.onSecondaryContainer,
        onClick = button.onClick,
        iconSize = SearchToolBarLayoutDimens.iconSize,
        roundedCornerShape = RoundedCornerShape.MEDIUM,
        modifier = GlanceModifier.fillMaxSize(),
    )

private enum class SearchToolBarLayoutSize {
    HorizontalRow,
    VerticalColumn,
    TwoByTwoGrid,
    SideBarTwoRowGrid,
    HeaderTwoRowGrid;

    companion object {
        @Composable
        fun fromLocalSize(): SearchToolBarLayoutSize {
            val (h, w) = LocalSize.current.run { height to width }
            return if (h < 128.dp) HorizontalRow
            else if (w < 128.dp) VerticalColumn
            else if (h < 188.dp && w < 188.dp) TwoByTwoGrid
            else if (h < 188.dp) SideBarTwoRowGrid else HeaderTwoRowGrid
        }

        @Composable
        fun canShowSearchText(): Boolean =
            LocalSize.current.run { width >= 184.dp && height >= 188.dp }

        @Composable
        fun canUseFilledButtons(): Boolean =
            LocalSize.current.run { height >= 72.dp && width >= 72.dp }

        @Composable
        fun numberOfItemsThatFit(horizontal: Boolean, minItemSize: Dp, spacing: Dp): Int {
            val size = if (horizontal) LocalSize.current.width else LocalSize.current.height
            return ((size + spacing) / (minItemSize + spacing)).toInt()
        }
    }
}

private object SearchToolBarLayoutDimens {
    val minButtonSize = 48.dp
    val widgetPadding = 12.dp
    val itemsSpacing = 8.dp
    val iconSize = 24.dp
    val sideBarLeadingItemWidth = 52.dp
    val headerItemHeight = 52.dp
}
