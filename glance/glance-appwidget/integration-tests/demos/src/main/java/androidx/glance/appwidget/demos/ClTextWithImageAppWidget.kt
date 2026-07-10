/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Internal copy of a Canonical Layout for testing. See the github repo for the official samples.
 * https://github.com/android/platform-samples/tree/main/samples/user-interface/appwidgets/src/main/java/com/example/platform/ui/appwidgets/glance
 */
class ClTextWithImageAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = FakeTextWithImageRepository.getRepo(id)
        val initialData = withContext(Dispatchers.Default) { repo.load(context) }

        provideContent {
            val data by repo.data().collectAsState(initial = initialData)
            val coroutineScope = rememberCoroutineScope()

            GlanceTheme {
                key(LocalSize.current) {
                    WidgetContent(
                        data = data,
                        refreshDataAction = {
                            coroutineScope.launch {
                                withContext(Dispatchers.Default) { repo.refresh(context) }
                            }
                        },
                    )
                }
            }
        }
    }

    override val previewSizeMode =
        SizeMode.Responsive(
            setOf(DpSize(256.dp, 115.dp), DpSize(256.dp, 180.dp), DpSize(256.dp, 301.dp))
        )

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val repo = FakeTextWithImageRepository()
        val data = repo.load(context)
        provideContent { GlanceTheme { WidgetContent(data = data, refreshDataAction = {}) } }
    }

    @Composable
    internal fun WidgetContent(data: TextWithImageData?, refreshDataAction: () -> Unit) {
        TextWithImageLayout(
            title = "Text and Image", // Inlined: R.string.sample_text_and_image_app_widget_name
            titleIconRes = CLIcons.sampleTextIcon,
            titleBarActionIconRes = CLIcons.sampleRefreshIcon,
            titleBarActionIconContentDescription =
                "Refresh", // Inlined: R.string.sample_refresh_icon_button_label
            titleBarAction = refreshDataAction,
            data = data,
        )
    }
}

@Suppress("RestrictedApiAndroidX")
class ClTextWithImageAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClTextWithImageAppWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { FakeTextWithImageRepository.cleanUp(AppWidgetId(it)) }
        super.onDeleted(context, appWidgetIds)
    }
}

internal data class TextData(
    val key: String,
    val primary: String,
    val secondary: String,
    val caption: String,
)

internal data class ImageData(val bitmap: Bitmap? = null, val contentDescription: String? = null)

internal data class TextWithImageData(val textData: TextData, val imageData: ImageData)

@Composable
internal fun TextWithImageLayout(
    title: String,
    @DrawableRes titleIconRes: Int,
    @DrawableRes titleBarActionIconRes: Int? = null,
    titleBarActionIconContentDescription: String? = null,
    titleBarAction: (() -> Unit)? = null,
    data: TextWithImageData? = null,
) {
    fun titleBar(): @Composable (() -> Unit) = {
        TitleBar(
            startIcon = ImageProvider(titleIconRes),
            title = title.takeIf { LocalSize.current.width >= 230.dp } ?: "",
            iconColor = GlanceTheme.colors.primary,
            textColor = GlanceTheme.colors.onSurface,
            actions = {
                if (titleBarAction != null && titleBarActionIconRes != null) {
                    CircleIconButton(
                        imageProvider = ImageProvider(titleBarActionIconRes),
                        contentDescription = titleBarActionIconContentDescription,
                        contentColor = GlanceTheme.colors.secondary,
                        backgroundColor = null,
                        onClick = titleBarAction,
                    )
                }
            },
        )
    }

    val action = ActionUtils.actionStartDemoActivity("TextWithImage: ${data?.textData?.key}")
    val layoutSize = TextWithImageLayoutSize.fromLocalSize()

    val titleBarComposable = if (layoutSize.showTitleBar()) titleBar() else null
    val scaffoldTopPadding =
        if (layoutSize.showTitleBar()) 0.dp else TextWithImageLayoutDimensions.widgetPadding

    Scaffold(
        titleBar = titleBarComposable,
        backgroundColor = GlanceTheme.colors.widgetBackground,
        horizontalPadding = TextWithImageLayoutDimensions.widgetPadding,
        modifier =
            GlanceModifier.maybeClickable(action)
                .padding(
                    bottom = TextWithImageLayoutDimensions.widgetPadding,
                    top = scaffoldTopPadding,
                ),
    ) {
        when (data) {
            null ->
                ClNoDataContentLayout(
                    noDataText = "No data",
                    noDataIconRes = CLIcons.sampleNoDataIcon,
                    actionButtonText = "Learn more",
                    actionButtonIcon = CLIcons.sampleInfoIcon,
                    actionButtonOnClick =
                        ActionUtils.actionStartDemoActivity(
                            "on-click of info button in no data view"
                        ),
                )
            else ->
                when (layoutSize) {
                    TextWithImageLayoutSize.VerticalSmall ->
                        VerticalContent(data = data, showImage = true, showSecondaryText = false)
                    TextWithImageLayoutSize.VerticalLarge ->
                        VerticalContent(data = data, showImage = true, showSecondaryText = true)
                    TextWithImageLayoutSize.HorizontalSmall ->
                        HorizontalContent(data = data, showImage = false, showSecondaryText = false)
                    TextWithImageLayoutSize.HorizontalLarge ->
                        HorizontalContent(data = data, showImage = true, showSecondaryText = true)
                }
        }
    }
}

@Composable
private fun HorizontalContent(
    data: TextWithImageData,
    showImage: Boolean,
    showSecondaryText: Boolean,
) {
    val contentWidth =
        TextWithImageLayoutDimensions.contentSize.width -
            TextWithImageLayoutDimensions.contentSpacing
    val contentHeight =
        TextWithImageLayoutDimensions.contentSize.height -
            (2 * TextWithImageLayoutDimensions.verticalTextsSpacing)

    Row(
        verticalAlignment = Alignment.Vertical.Bottom,
        horizontalAlignment = Alignment.Horizontal.Start,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        TextStack(
            data = data.textData,
            showSecondaryText = showSecondaryText,
            modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
            availableSize =
                DpSize(
                    width = (if (showImage) 0.4f * contentWidth.value else contentWidth.value).dp,
                    height =
                        (if (showImage) 0.80f * contentHeight.value else contentHeight.value).dp,
                ),
        )
        if (showImage) {
            Spacer(modifier = GlanceModifier.width(TextWithImageLayoutDimensions.contentSpacing))
            Box(modifier = GlanceModifier.fillMaxHeight().defaultWeight()) {
                ImageComposable(
                    data = data.imageData,
                    modifier = GlanceModifier.fillMaxHeight().fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun VerticalContent(
    data: TextWithImageData,
    showImage: Boolean,
    showSecondaryText: Boolean,
) {
    Column(
        verticalAlignment = Alignment.Vertical.Bottom,
        modifier = GlanceModifier.fillMaxHeight(),
    ) {
        val contentWidth = TextWithImageLayoutDimensions.contentSize.width
        val contentHeight =
            TextWithImageLayoutDimensions.contentSize.height -
                (2 * TextWithImageLayoutDimensions.verticalTextsSpacing)

        if (showImage) {
            ImageComposable(
                data = data.imageData,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            )
            Spacer(modifier = GlanceModifier.height(TextWithImageLayoutDimensions.contentSpacing))
        }
        TextStack(
            data = data.textData,
            showSecondaryText = showSecondaryText,
            modifier = GlanceModifier.fillMaxWidth(),
            availableSize =
                DpSize(
                    width = (0.8 * contentWidth.value).dp,
                    height = (if (showImage) 0.4f * contentHeight.value else contentHeight.value).dp,
                ),
        )
    }
}

@Composable
private fun TextStack(
    data: TextData,
    modifier: GlanceModifier,
    availableSize: DpSize,
    showSecondaryText: Boolean,
) {
    val (primaryTextFontSize, primaryTextMaxLines) =
        TextWithImageLayoutTextStyles.primaryTextFontValues(
            text = data.primary,
            availableSize = availableSize,
            showSecondaryText = showSecondaryText,
        )
    Column(verticalAlignment = Alignment.Bottom, modifier = modifier) {
        Text(
            text = data.caption,
            maxLines = 1,
            style = TextWithImageLayoutTextStyles.caption,
            modifier = GlanceModifier.fillMaxWidth(),
        )
        Spacer(modifier = GlanceModifier.height(TextWithImageLayoutDimensions.verticalTextsSpacing))
        Text(
            text = data.primary,
            maxLines = primaryTextMaxLines,
            style = TextWithImageLayoutTextStyles.primary.copy(fontSize = primaryTextFontSize),
        )
        if (showSecondaryText) {
            val (secondaryTextFontSize, secondaryTextMaxLines) =
                TextWithImageLayoutTextStyles.secondaryTextFontValues(
                    text = data.secondary,
                    availableSize = availableSize,
                )
            Spacer(
                modifier = GlanceModifier.height(TextWithImageLayoutDimensions.verticalTextsSpacing)
            )
            Text(
                text = data.secondary,
                maxLines = secondaryTextMaxLines,
                style =
                    TextWithImageLayoutTextStyles.secondary.copy(fontSize = secondaryTextFontSize),
            )
        }
    }
}

@Composable
private fun ImageComposable(data: ImageData, modifier: GlanceModifier) {
    val imageProvider =
        if (data.bitmap != null) ImageProvider(data.bitmap)
        else ImageProvider(CLIcons.samplePlaceholderImage)
    Image(
        provider = imageProvider,
        contentDescription = data.contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.cornerRadius(TextWithImageLayoutDimensions.pictureRadius),
    )
}

private enum class TextWithImageLayoutSize {
    HorizontalSmall,
    VerticalSmall,
    HorizontalLarge,
    VerticalLarge;

    companion object {
        @Composable
        fun fromLocalSize(): TextWithImageLayoutSize {
            val size = LocalSize.current
            val isTall = size.height >= size.width
            return if (isTall && size.height <= 300.dp) VerticalSmall
            else if (isTall) VerticalLarge
            else if (size.width <= 165.dp) HorizontalSmall else HorizontalLarge
        }
    }

    @Composable fun showTitleBar() = LocalSize.current.height >= 180.dp
}

private object TextWithImageLayoutTextStyles {
    val primary: TextStyle
        @Composable
        get() = TextStyle(fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurface)

    val secondary: TextStyle
        @Composable get() = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)

    val caption: TextStyle
        @Composable get() = TextStyle(color = GlanceTheme.colors.secondary)

    @Composable
    fun primaryTextFontValues(
        text: String,
        availableSize: DpSize,
        showSecondaryText: Boolean,
    ): Pair<TextUnit, Int> {
        return 20.sp to 4 // TODO: hardcoded
    }

    @Composable
    fun secondaryTextFontValues(text: String, availableSize: DpSize): Pair<TextUnit, Int> =
        14.sp to 4 // TODO: hardcoded
}

private object TextWithImageLayoutDimensions {
    val widgetPadding = 16.dp
    val pictureRadius = 16.dp
    val contentSpacing = 12.dp
    val verticalTextsSpacing = 4.dp
    private val titleBarHeight: Dp
        @Composable
        get() = if (TextWithImageLayoutSize.fromLocalSize().showTitleBar()) 0.dp else 56.dp

    val contentSize: DpSize
        @Composable
        get() {
            val size = LocalSize.current
            return DpSize(
                width = size.width - (2 * widgetPadding.value).dp,
                height = size.height - titleBarHeight.value.dp - widgetPadding.value.dp,
            )
        }
}

internal class FakeTextWithImageRepository {
    private val dataFlow = MutableStateFlow<TextWithImageData?>(null)
    private var itemIndex = 0
    private var items = Companion.demoItems

    fun data(): Flow<TextWithImageData?> = dataFlow

    suspend fun refresh(context: Context) {
        itemIndex = (itemIndex + 1) % items.size
        load(context)
    }

    suspend fun load(context: Context): TextWithImageData? {
        val item = items[itemIndex]
        val bitmap = fetchLocalImage(context, CLIcons.sampleImage)
        dataFlow.value =
            TextWithImageData(
                textData =
                    TextData(
                        key = "$itemIndex",
                        primary = item.primary,
                        secondary = item.secondary,
                        caption = item.caption,
                    ),
                imageData =
                    ImageData(bitmap = bitmap, contentDescription = item.imageContentDescription),
            )
        return dataFlow.value
    }

    companion object {
        private const val TAG = "SFTWIR_Local"
        private val repositories = mutableMapOf<GlanceId, FakeTextWithImageRepository>()

        fun getRepo(glanceId: GlanceId): FakeTextWithImageRepository =
            synchronized(repositories) {
                repositories.computeIfAbsent(glanceId) { FakeTextWithImageRepository() }
            }

        fun cleanUp(glanceId: GlanceId) =
            synchronized(repositories) { repositories.remove(glanceId) }

        data class DemoData(
            val imageContentDescription: String? = null,
            val primary: String,
            val secondary: String,
            val caption: String,
        )

        val demoItems =
            listOf(
                DemoData(
                    primary = "Davos at sunrise",
                    secondary = "Golden light washes over Davos.",
                    caption = "33,822 views",
                    imageContentDescription = "Davos at sunrise",
                ),
                DemoData(
                    primary = "Wedding Flowers",
                    secondary = "Flowers in a vase by a window.",
                    caption = "33,822 views",
                    imageContentDescription = "Wedding flowers",
                ),
            )
    }
}
