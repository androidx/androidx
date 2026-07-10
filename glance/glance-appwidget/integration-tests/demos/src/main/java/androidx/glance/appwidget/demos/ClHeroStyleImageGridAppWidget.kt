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
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Internal copy of a Canonical Layout for testing. See the github repo for the official samples.
 * https://github.com/android/platform-samples/tree/main/samples/user-interface/appwidgets/src/main/java/com/example/platform/ui/appwidgets/glance
 */
class ClHeroStyleImageGridAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = FakeClImageGridDataRepository.getImageGridDataRepo(id)

        val initialItems = withContext(Dispatchers.Default) { repo.load(context) }

        provideContent {
            val items by repo.data().collectAsState(initial = initialItems)
            val coroutineScope = rememberCoroutineScope()

            GlanceTheme {
                key(LocalSize.current) {
                    WidgetContent(
                        items = items,
                        refreshAction = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) { repo.refresh(context) }
                            }
                        },
                    )
                }
            }
        }
    }

    @Composable
    fun WidgetContent(items: List<ClImageGridItemData>, refreshAction: () -> Unit) {
        ClHeroStyleImageGridLayout(
            titleIconRes = CLIcons.clSampleGridIcon,
            titleBarActionIconRes = CLIcons.sampleRefreshIcon,
            titleBarActionIconContentDescription = "Refresh",
            titleBarAction = refreshAction,
            items = items,
        )
    }
}

class ClHeroStyleImageGridAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClHeroStyleImageGridAppWidget()
}

@Composable
fun ClHeroStyleImageGridLayout(
    @DrawableRes titleIconRes: Int,
    @DrawableRes titleBarActionIconRes: Int,
    titleBarActionIconContentDescription: String,
    titleBarAction: () -> Unit,
    items: List<ClImageGridItemData>,
) {
    @Composable
    fun TitleBar(filledActionButtons: Boolean = false) {
        val actionButtonBackgroundColor =
            GlanceTheme.colors.inverseOnSurface.takeIf { filledActionButtons }

        TitleBar(
            startIcon = ImageProvider(titleIconRes),
            title = "",
            iconColor = GlanceTheme.colors.primary,
            textColor = GlanceTheme.colors.onSurface,
            actions = {
                CircleIconButton(
                    imageProvider = ImageProvider(titleBarActionIconRes),
                    contentDescription = titleBarActionIconContentDescription,
                    contentColor = GlanceTheme.colors.secondary,
                    backgroundColor = actionButtonBackgroundColor,
                    onClick = titleBarAction,
                )
            },
        )
    }

    @Composable
    fun EmptyListWidgetContent() {
        Scaffold(
            backgroundColor = GlanceTheme.colors.widgetBackground,
            titleBar = { TitleBar(filledActionButtons = false) },
        ) {
            ClEmptyListContent()
        }
    }

    @Composable
    fun HeroGridWidgetContent() {
        val systemCornerRadiusDefined =
            LocalContext.current.resources.getResourceName(
                android.R.dimen.system_app_widget_background_radius
            ) != null
        val cornerRadiusModifier =
            if (android.os.Build.VERSION.SDK_INT >= 31 && systemCornerRadiusDefined) {
                GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
            } else {
                GlanceModifier
            }

        Box(
            GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .then(cornerRadiusModifier)
        ) {
            ScrollableHeroGrid(items = items, titleBar = { TitleBar(filledActionButtons = true) })
        }
    }

    if (items.isEmpty()) {
        EmptyListWidgetContent()
    } else {
        HeroGridWidgetContent()
    }
}

@Composable
private fun ScrollableHeroGrid(items: List<ClImageGridItemData>, titleBar: @Composable () -> Unit) {
    val heroItem = items.first()
    val gridItems = items.subList(1, items.size)
    var gridItemIndex = 0

    @Composable
    fun HeroWithTitleBarOverlay() {
        Box(
            modifier =
                GlanceModifier.fillMaxWidth()
                    .padding(bottom = ClHeroStyleImageGridLayoutDimensions.gridCellSpacing)
        ) {
            GridItem(
                item = heroItem,
                modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
                textStartMargin = ClHeroStyleImageGridLayoutDimensions.contentPadding,
            )
            Box(
                modifier =
                    GlanceModifier.padding(ClHeroStyleImageGridLayoutDimensions.contentPadding),
                content = titleBar,
            )
        }
    }

    @Composable
    fun GridRow(isLastRow: Boolean) {
        Row(
            modifier =
                GlanceModifier.fillMaxWidth()
                    .padding(horizontal = ClHeroStyleImageGridLayoutDimensions.contentPadding)
                    .padding(
                        bottom =
                            ClHeroStyleImageGridLayoutDimensions.gridCellSpacing.takeIf {
                                isLastRow
                            } ?: 0.dp
                    )
        ) {
            GridItem(
                item = gridItems[gridItemIndex++],
                modifier =
                    GlanceModifier.width(ClHeroStyleImageGridLayoutDimensions.itemWidth)
                        .padding(end = ClHeroStyleImageGridLayoutDimensions.gridCellSpacing)
                        .defaultWeight(),
            )
            if (gridItemIndex < gridItems.size) {
                GridItem(
                    item = gridItems[gridItemIndex++],
                    modifier = GlanceModifier.width(ClHeroStyleImageGridLayoutDimensions.itemWidth),
                )
            }
        }
    }

    LazyColumn(
        modifier =
            GlanceModifier.semantics { contentDescription = "Top ${items.size} items for you" }
    ) {
        val numberOfRows = gridItems.size / ClHeroStyleImageGridLayoutDimensions.GRID_CELLS

        item { HeroWithTitleBarOverlay() }
        items(numberOfRows) { rowIndex -> GridRow(isLastRow = rowIndex != numberOfRows - 1) }
    }
}

@Composable
private fun GridItem(
    item: ClImageGridItemData,
    modifier: GlanceModifier,
    textStartMargin: Dp = 4.dp,
) {
    val finalModifier =
        modifier.clickable(
            ActionUtils.actionStartDemoActivity(
                "Grid item: ${item.title ?: item.imageContentDescription}"
            )
        )
    val imageProvider =
        if (item.image != null) {
            ImageProvider(item.image)
        } else {
            ImageProvider(CLIcons.samplePlaceholderImage)
        }

    @Composable
    fun Image() {
        Image(
            provider = imageProvider,
            contentDescription = item.imageContentDescription,
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.fillMaxWidth().wrapContentHeight().cornerRadius(16.dp),
        )
    }

    @Composable
    fun Title(text: String) {
        Text(
            text = text,
            maxLines = 1,
            style = ClHeroStyleImageGridLayoutTextStyles.titleText,
            modifier = GlanceModifier.padding(start = textStartMargin),
        )
    }

    @Composable
    fun SupportingText(text: String) {
        Text(
            text = text,
            maxLines = 1,
            style = ClHeroStyleImageGridLayoutTextStyles.supportingText,
            modifier = GlanceModifier.padding(start = textStartMargin),
        )
    }

    if (item.title != null) {
        ClVerticalListItem(
            modifier = finalModifier,
            topContent = { Image() },
            titleContent = { Title(text = item.title) },
            supportingContent =
                item.supportingText?.let { supportingText ->
                    { SupportingText(text = supportingText) }
                },
        )
    } else {
        Box(finalModifier) { Image() }
    }
}

private object ClHeroStyleImageGridLayoutTextStyles {
    val titleText: TextStyle
        @Composable
        get() =
            TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = GlanceTheme.colors.onSurface,
            )

    val supportingText: TextStyle
        @Composable
        get() =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = GlanceTheme.colors.secondary,
            )
}

private object ClHeroStyleImageGridLayoutDimensions {
    val contentPadding: Dp = 12.dp
    val gridCellSpacing: Dp = 12.dp
    const val GRID_CELLS = 2

    val itemWidth: Dp
        @Composable
        get() {
            val totalContentWidth = LocalSize.current.width - (contentPadding * 2)
            val totalHorizontalCellSpacing = gridCellSpacing * (GRID_CELLS - 1)
            return (totalContentWidth - totalHorizontalCellSpacing) / GRID_CELLS
        }
}

data class ClImageGridItemData(
    val key: String,
    val image: Bitmap?,
    val imageContentDescription: String?,
    val title: String? = null,
    val supportingText: String? = null,
)

class FakeClImageGridDataRepository {
    private val data = MutableStateFlow(listOf<ClImageGridItemData>())
    private var items = demoItems.take(MAX_ITEMS_PER_WIDGET)

    fun data(): Flow<List<ClImageGridItemData>> = data

    suspend fun refresh(context: Context) {
        items = items.shuffled()
        this.load(context)
    }

    suspend fun load(context: Context): List<ClImageGridItemData> {
        data.value = processImagesAndBuildData(context = context, items = items)
        return data.value
    }

    private suspend fun processImagesAndBuildData(
        context: Context,
        items: List<ImageGridItemBackendData>,
    ): List<ClImageGridItemData> {
        val maxAllowedBytes = ClImageUtils.getMaxWidgetMemoryAllowedSizeInBytes(context)
        val maxAllowedBytesPerImage = maxAllowedBytes / items.size
        val imageSizeLimit =
            ClImageUtils.getMaxPossibleImageSize(
                aspectRatio = 16.0 / 9.0,
                memoryLimitBytes = maxAllowedBytesPerImage,
                maxImages = 1,
            )

        val width = IMAGE_SIZE.coerceAtMost(imageSizeLimit.width)
        val height = width * 9 / 16

        val mappedItems = runBlocking {
            items
                .map { item ->
                    async(Dispatchers.IO) {
                        var bitmap: Bitmap? =
                            fetchLocalImage(
                                context,
                                R.drawable.sample_recipe_matcha_mellowness_1x1_small,
                            )

                        return@async ClImageGridItemData(
                            key = item.key,
                            title = item.title,
                            supportingText = item.supportingText,
                            image = bitmap,
                            imageContentDescription = item.imageContentDescription,
                        )
                    }
                }
                .awaitAll()
        }

        return mappedItems
    }

    private data class ImageGridItemBackendData(
        val key: String,
        val imageUrl: String,
        val imageContentDescription: String?,
        val title: String? = null,
        val supportingText: String? = null,
    )

    companion object {
        private val repositories = mutableMapOf<GlanceId, FakeClImageGridDataRepository>()

        private val demoItems =
            listOf(
                ImageGridItemBackendData(
                    key = "1",
                    imageUrl = "https://images.unsplash.com/photo-1531306760863-7fb02a41db12",
                    imageContentDescription = "Flowers at a wedding reception",
                    title = "Flowers at a wedding reception",
                    supportingText = "33,822 views",
                ),
                ImageGridItemBackendData(
                    key = "2",
                    imageUrl = "https://images.unsplash.com/photo-1566964423430-3e52903303a5",
                    imageContentDescription = "An up-close look at a Blushing Bride Protea flower.",
                    title = "An up-close look at a Blushing Bride Protea flower.",
                    supportingText = "31,072 views",
                ),
                ImageGridItemBackendData(
                    key = "3",
                    imageUrl = "https://images.unsplash.com/photo-1685540466252-8c21e7c37624",
                    imageContentDescription =
                        "A single water droplet rests in a budding red pansy.",
                    title = "A single water droplet rests in a budding red pansy.",
                    supportingText = "193 views",
                ),
                ImageGridItemBackendData(
                    key = "4",
                    imageUrl = "https://images.unsplash.com/photo-1582817954171-c3533fffde89",
                    imageContentDescription = "Blossom, petal, flower",
                    title = "Blossom, petal, flower",
                    supportingText = "23,815 views",
                ),
                //            ImageGridItemBackendData(
                //                key = "5",
                //                imageUrl =
                // "https://images.unsplash.com/photo-1565314912546-0d18918fdc8f",
                //                imageContentDescription = "Green plant, sky and flowers",
                //                title = "Green plant, sky and flowers",
                //                supportingText = "99,467 views"
                //            ),
                //            ImageGridItemBackendData(
                //                key = "6",
                //                imageUrl =
                // "https://images.unsplash.com/photo-1671525784444-392a8f8daa3f",
                //                imageContentDescription = "A snow-shoer walking up Strelapass",
                //                title = "A snow-shoer walking up Strelap-ass",
                //                supportingText = "3,033 views",
                //            ),
                //            ImageGridItemBackendData(
                //                key = "7",
                //                imageUrl =
                // "https://images.unsplash.com/photo-1671525737370-1d490286372e",
                //                imageContentDescription = "Davos at sunrise, viewed from
                // Schatzalp",
                //                title = "Davos at sunrise, viewed from Schatzalp",
                //                supportingText = "4,054 views",
                //            ),
                //            ImageGridItemBackendData(
                //                key = "8",
                //                imageUrl =
                // "https://images.unsplash.com/photo-1629027272726-2eed15f90e8e",
                //                imageContentDescription = "Nasturtium plants",
                //                title = "Nasturtium plants",
                //                supportingText = "975 views",
                //            )
            )

        fun getImageGridDataRepo(glanceId: GlanceId): FakeClImageGridDataRepository {
            return synchronized(repositories) {
                repositories.computeIfAbsent(glanceId) { FakeClImageGridDataRepository() }
            }
        }

        fun cleanUp(glanceId: GlanceId) {
            synchronized(repositories) { repositories.remove(glanceId) }
        }

        const val MAX_ITEMS_PER_WIDGET = 8
        const val IMAGE_SIZE = 200
        const val TAG = "FIGDR"
    }
}
