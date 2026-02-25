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

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.Size
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.LazyVerticalGridScope
import androidx.glance.appwidget.lazy.itemsIndexed
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
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * This file contains ai generated adaptations of the glance canonical layout samples. This
 * adaptation is intended for internal visual testing.
 */
object CLIcons {
    val sampleTextIcon: Int = R.drawable.sample_text_icon
    val sampleRefreshIcon: Int = R.drawable.sample_refresh_icon
    val sampleSearchIcon: Int = R.drawable.sample_search_icon
    val sampleMicIcon: Int = R.drawable.sample_mic_icon
    val sampleVideocamIcon: Int = R.drawable.sample_videocam_icon
    val sampleCameraIcon: Int = R.drawable.sample_camera_icon
    val sampleShareIcon: Int = R.drawable.sample_share_icon
    val sampleAppLogo: Int = R.drawable.ic_demo_app
    val sampleAddIcon: Int = R.drawable.baseline_add_24
    val sampleNoDataIcon: Int = R.drawable.sample_no_data_icon
    val sampleInfoIcon: Int = R.drawable.sample_info_icon
    val samplePlaceholderImage: Int = R.drawable.sample_placeholder_image
    val sampleImage_tooBig: Int =
        R.drawable.sample_recipe_matcha_mellowness_1x1 // use this to produce memory pressure
    val sampleImage: Int = R.drawable.sample_recipe_matcha_mellowness_1x1_small
    val sampleNoteIcon: Int = R.drawable.sample_note_icon
    val sampleDrawIcon: Int = R.drawable.sample_draw_icon
    val sampleImageIcon: Int = R.drawable.sample_image_icon
    val clSampleHomeIcon: Int = R.drawable.cl_sample_home_icon
    val clSamplePowerSettingsIcon: Int = R.drawable.cl_sample_power_settings_icon
    val clSampleBulbIcon: Int = R.drawable.cl_sample_bulb_icon
    val clSampleThermostatIcon: Int = R.drawable.cl_sample_thermostat_icon
    val clSampleArrowRightIcon: Int = R.drawable.cl_sample_arrow_right_icon
    val clSampleAcIcon: Int = R.drawable.cl_sample_ac_icon
    val clSampleDoorIcon: Int = R.drawable.cl_sample_door_icon
    val clSamplePinIcon: Int = R.drawable.cl_sample_pin_icon
    val clSampleCheckedCircleIcon: Int = R.drawable.cl_sample_checked_circle_icon
    val clSampleCircleIcon: Int = R.drawable.cl_sample_circle_icon
    val clSampleEditIcon: Int = R.drawable.cl_sample_edit_icon
    val clSampleSnoozeIcon: Int = R.drawable.cl_sample_snooze_icon
    val clSampleDeleteIcon: Int = R.drawable.cl_sample_delete_icon
    val clSampleFileUploadIcon: Int = R.drawable.cl_sample_file_upload_icon
    val clFourSideCookieBackground: Int = R.drawable.cl_four_side_cookie_background
    val clSampleGridIcon: Int = R.drawable.cl_sample_grid_icon
}

@Composable
internal fun SpacedColumn(
    items: List<@Composable () -> Unit>,
    spacing: Dp,
    modifier: GlanceModifier = GlanceModifier.fillMaxHeight(),
) {
    val padding = spacing / 2
    Row(modifier = modifier) {
        Column(modifier = GlanceModifier.fillMaxHeight().defaultWeight()) {
            items.forEachIndexed { index, item ->
                val paddingModifier =
                    when (index) {
                        0 -> GlanceModifier.padding(bottom = padding)
                        items.lastIndex -> GlanceModifier.padding(top = padding)
                        else -> GlanceModifier.padding(top = padding, bottom = padding)
                    }
                Box(modifier = paddingModifier.fillMaxWidth().defaultWeight(), content = item)
            }
        }
    }
}

@Composable
internal fun SpacedRow(
    items: List<@Composable () -> Unit>,
    spacing: Dp,
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth(),
) {
    val padding = spacing / 2
    Column(modifier = modifier) {
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            items.forEachIndexed { index, item ->
                val paddingModifier =
                    when (index) {
                        0 -> GlanceModifier.padding(end = padding)
                        items.lastIndex -> GlanceModifier.padding(start = padding)
                        else -> GlanceModifier.padding(start = padding, end = padding)
                    }
                Box(modifier = paddingModifier.fillMaxHeight().defaultWeight(), content = item)
            }
        }
    }
}

@Composable
internal fun TwoRowGrid(
    items: List<@Composable () -> Unit>,
    spacing: Dp,
    modifier: GlanceModifier = GlanceModifier.fillMaxSize(),
) {
    if (items.isEmpty()) return
    val middle = items.size / 2 + items.size % 2 // Ensure first row gets more or equal items
    val rowOneItems = items.subList(0, middle)
    val rowTwoItems = items.subList(middle, items.size)

    Column(modifier = modifier) {
        if (rowOneItems.isNotEmpty()) {
            SpacedRow(
                items = rowOneItems,
                spacing = spacing,
                modifier =
                    GlanceModifier.fillMaxWidth()
                        .defaultWeight()
                        .padding(bottom = if (rowTwoItems.isNotEmpty()) spacing / 2 else 0.dp),
            )
        }
        if (rowTwoItems.isNotEmpty()) {
            SpacedRow(
                items = rowTwoItems,
                spacing = spacing,
                modifier =
                    GlanceModifier.fillMaxWidth()
                        .padding(top = if (rowOneItems.isNotEmpty()) spacing / 2 else 0.dp)
                        .defaultWeight(),
            )
        }
    }
}

@Composable
internal fun SideBarTwoRowGrid(
    sideBarItem: @Composable () -> Unit,
    items: List<@Composable () -> Unit>,
    sideBarWidth: Dp,
    spacing: Dp,
    modifier: GlanceModifier = GlanceModifier.fillMaxSize(),
) {
    Row(modifier = modifier) {
        Box(modifier = GlanceModifier.fillMaxHeight().width(sideBarWidth), content = sideBarItem)
        Spacer(modifier = GlanceModifier.fillMaxHeight().width(spacing))
        TwoRowGrid(
            items = items,
            spacing = spacing,
            modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
        )
    }
}

@Composable
internal fun HeaderTwoRowGrid(
    headerItem: @Composable () -> Unit,
    items: List<@Composable () -> Unit>,
    headerHeight: Dp,
    spacing: Dp,
    modifier: GlanceModifier = GlanceModifier.fillMaxSize(),
) {
    Column(modifier = modifier) {
        Box(modifier = GlanceModifier.height(headerHeight).fillMaxWidth(), content = headerItem)
        Spacer(modifier = GlanceModifier.fillMaxWidth().height(spacing))
        TwoRowGrid(
            items = items,
            spacing = spacing,
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
        )
    }
}

@Composable
internal fun RectangularIconButton(
    imageProvider: ImageProvider,
    onClick: Action,
    roundedCornerShape: RoundedCornerShape,
    contentDescription: String,
    iconSize: Dp,
    modifier: GlanceModifier,
    backgroundColor: ColorProvider = GlanceTheme.colors.primary,
    contentColor: ColorProvider = GlanceTheme.colors.onPrimary,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .cornerRadius(roundedCornerShape.cornerRadius)
                .background(backgroundColor)
                .semantics { this.contentDescription = contentDescription }
                .clickable(onClick),
    ) {
        Image(
            provider = imageProvider,
            contentDescription = null,
            colorFilter = ColorFilter.tint(contentColor),
            modifier = GlanceModifier.size(iconSize),
        )
    }
}

@Composable
internal fun PillShapedButton(
    iconImageProvider: ImageProvider,
    iconSize: Dp,
    backgroundColor: ColorProvider,
    contentColor: ColorProvider,
    contentDescription: String,
    onClick: Action,
    modifier: GlanceModifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .semantics { this.contentDescription = contentDescription }
                .height(48.dp)
                .clickable(onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                GlanceModifier.width(52.dp)
                    .cornerRadius(RoundedCornerShape.FULL.cornerRadius)
                    .height(32.dp)
                    .background(backgroundColor),
        ) {
            Image(
                provider = iconImageProvider,
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = GlanceModifier.size(iconSize),
            )
        }
    }
}

enum class RoundedCornerShape(val cornerRadius: Dp) {
    FULL(100.dp),
    MEDIUM(16.dp),
}

@Composable
fun ClListItem(
    headlineContent: @Composable (() -> Unit),
    modifier: GlanceModifier = GlanceModifier,
    contentSpacing: Dp = 16.dp,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: Action? = null,
    itemContentDescription: String? = null,
) {
    val listItemModifier =
        if (itemContentDescription != null) {
            modifier.semantics { contentDescription = itemContentDescription }
        } else {
            modifier
        }

    Row(
        modifier = listItemModifier.maybeClickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent?.let {
            it()
            Spacer(modifier = GlanceModifier.width(contentSpacing))
        }
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            headlineContent()
            supportingContent?.let { it() }
        }
        trailingContent?.let {
            Spacer(modifier = GlanceModifier.width(contentSpacing))
            it()
        }
    }
}

@Composable
fun ClVerticalListItem(
    titleContent: @Composable (() -> Unit),
    modifier: GlanceModifier = GlanceModifier,
    topContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingBottomContent: @Composable (() -> Unit)? = null,
    onClick: Action? = null,
    itemContentDescription: String? = null,
) {
    val listItemModifier =
        if (itemContentDescription != null) {
            modifier.semantics { contentDescription = itemContentDescription }
        } else {
            modifier
        }

    Column(
        modifier = listItemModifier.maybeClickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        topContent?.let {
            it()
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
        Row {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                titleContent()
                supportingContent?.let { it() }
            }
            trailingBottomContent?.let {
                Spacer(modifier = GlanceModifier.width(8.dp))
                it()
            }
        }
    }
}

@Composable
fun ClRoundedScrollingLazyColumn(
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit,
) {
    Box(modifier = GlanceModifier.fillMaxSize().cornerRadius(16.dp).then(modifier)) {
        LazyColumn(horizontalAlignment = horizontalAlignment, content = content)
    }
}

@Composable
fun <T> ClRoundedScrollingLazyColumn(
    items: List<T>,
    itemContentProvider: @Composable (item: T) -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalItemsSpacing: Dp = 4.dp,
) {
    val lastIndex = items.size - 1

    ClRoundedScrollingLazyColumn(modifier, horizontalAlignment) {
        itemsIndexed(items) { index, item ->
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                itemContentProvider(item)
                if (index != lastIndex) {
                    Spacer(modifier = GlanceModifier.height(verticalItemsSpacing))
                }
            }
        }
    }
}

@Composable
fun ClRoundedScrollingLazyVerticalGrid(
    gridCells: GridCells,
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyVerticalGridScope.() -> Unit,
) {
    Box(modifier = GlanceModifier.cornerRadius(16.dp).then(modifier)) {
        LazyVerticalGrid(
            gridCells = gridCells,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}

@Composable
fun <T> ClRoundedScrollingLazyVerticalGrid(
    gridCells: Int,
    items: List<T>,
    itemContentProvider: @Composable (item: T) -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    cellSpacing: Dp = 12.dp,
) {
    val numRows = ceil(items.size.toDouble() / gridCells).toInt()

    val perCellHorizontalPadding = (cellSpacing * (gridCells - 1)) / gridCells
    val perCellVerticalPadding = (cellSpacing * (numRows - 1)) / numRows

    ClRoundedScrollingLazyVerticalGrid(
        gridCells = GridCells.Fixed(gridCells),
        horizontalAlignment = horizontalAlignment,
        modifier = modifier,
    ) {
        itemsIndexed(items) { index, item ->
            val row = index / gridCells
            val column = index % gridCells

            val cellTopPadding =
                when (row) {
                    0 -> 0.dp
                    numRows - 1 -> perCellVerticalPadding
                    else -> perCellVerticalPadding / 2
                }

            val cellBottomPadding =
                when (row) {
                    0 -> perCellVerticalPadding
                    numRows - 1 -> 0.dp
                    else -> perCellVerticalPadding / 2
                }

            val cellStartPadding =
                when (column) {
                    0 -> 0.dp
                    gridCells - 1 -> perCellHorizontalPadding
                    else -> perCellHorizontalPadding / 2
                }

            val cellEndPadding =
                when (column) {
                    0 -> perCellHorizontalPadding
                    gridCells - 1 -> 0.dp
                    else -> perCellHorizontalPadding / 2
                }

            Box(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(
                            start = cellStartPadding,
                            end = cellEndPadding,
                            top = cellTopPadding,
                            bottom = cellBottomPadding,
                        )
            ) {
                itemContentProvider(item)
            }
        }
    }
}

@Composable
internal inline fun takeComposableIf(
    predicate: Boolean,
    crossinline block: @Composable () -> Unit,
): (@Composable () -> Unit)? {
    return if (predicate) {
        { block() }
    } else null
}

@Composable
internal fun GlanceModifier.widgetInnerCornerRadius(widgetPadding: Dp): GlanceModifier {
    if (android.os.Build.VERSION.SDK_INT < 31) {
        return this
    }

    val resources = LocalContext.current.resources
    val px = resources.getDimension(android.R.dimen.system_app_widget_background_radius)
    val widgetBackgroundRadiusDpValue = px / resources.displayMetrics.density

    if (widgetBackgroundRadiusDpValue < widgetPadding.value) {
        return this
    }

    return this.cornerRadius(Dp(widgetBackgroundRadiusDpValue - widgetPadding.value))
}

fun GlanceModifier.maybeClickable(action: Action?): GlanceModifier =
    if (action != null) this.clickable(action) else this

object ActionUtils {

    @Composable
    fun actionStartDemoActivity(message: String): Action =
        actionStartActivity(
            ComponentName(
                "androidx.glance.appwidget.demos",
                GlanceAppWidgetDemoActivity::class.java.simpleName,
            )
        )
}

object ClImageUtils {
    fun getMaxWidgetMemoryAllowedSizeInBytes(context: Context): Int {
        val size = context.resources.displayMetrics.run { Size(widthPixels, heightPixels) }
        return 6 * size.width * size.height
    }

    fun getMaxPossibleImageSize(aspectRatio: Double, memoryLimitBytes: Int, maxImages: Int): Size {
        val limit = (memoryLimitBytes / 4) / maxImages
        val maxSizeAllowedPerPixel: Int = limit / 4

        val side = sqrt(maxSizeAllowedPerPixel.toDouble()).toInt()
        val width = if (aspectRatio > 1) side else max(1, (side * aspectRatio).roundToInt())
        val height = if (aspectRatio > 1) max(1, (side / aspectRatio).roundToInt()) else side
        return Size(width, height)
    }
}

internal fun fetchLocalImage(context: Context, @DrawableRes resId: Int): Bitmap? =
    try {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, resId, opts).also {
            Log.i(
                "CL fetchLocalImage()",
                "Bitmap decoded w ${it.width}, h ${it.height}, sz ${it.allocationByteCount}, id  = ${context.resources.getResourceName(resId)}",
            )
        }
    } catch (e: Exception) {
        Log.e("rcLayoutTests", "Failed to load local image resource: $resId", e)
        null
    }

@Composable
internal fun ClEmptyListContent() {
    ClNoDataContentLayout(
        noDataText = "No data",
        noDataIconRes = CLIcons.sampleNoDataIcon,
        actionButtonText = "Add",
        actionButtonIcon = CLIcons.sampleAddIcon,
        actionButtonOnClick = ActionUtils.actionStartDemoActivity("on-click of add item button"),
    )
}

@Composable
internal fun ClNoDataContentLayout(
    noDataIconRes: Int,
    noDataText: String,
    actionButtonText: String,
    actionButtonIcon: Int,
    actionButtonOnClick: Action,
) {
    @Composable fun showIcon() = LocalSize.current.height >= 180.dp

    Column(
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        if (showIcon()) {
            Image(provider = ImageProvider(noDataIconRes), contentDescription = null)
            Spacer(modifier = GlanceModifier.height(8.dp))
        }
        Text(
            text = noDataText,
            style =
                TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                ),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        FilledButton(
            text = actionButtonText,
            icon = ImageProvider(actionButtonIcon),
            onClick = actionButtonOnClick,
        )
    }
}
