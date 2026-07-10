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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.ExperimentalGlanceApi
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
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * Internal copy of a Canonical Layout for testing. See the github repo for the official samples.
 * https://github.com/android/platform-samples/tree/main/samples/user-interface/appwidgets/src/main/java/com/example/platform/ui/appwidgets/glance
 */
class ClActionListAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = FakeClActionListDataRepository.getActionListDataRepo(id)

        val initialItems = withContext(Dispatchers.Default) { repo.load() }

        provideContent {
            val items by repo.items().collectAsState(initial = initialItems)
            val checkedItems by repo.checkedItems().collectAsState(initial = emptyList())

            GlanceTheme {
                WidgetContent(
                    items = items,
                    checkedItems = checkedItems,
                    checkItemAction = { key -> repo.checkItem(key) },
                )
            }
        }
    }

    @Composable
    fun WidgetContent(
        items: List<ClActionListItem>,
        checkedItems: List<String>,
        checkItemAction: (String) -> Unit,
    ) {
        ClActionListLayout(
            title = "Action List",
            titleIconRes = CLIcons.clSampleHomeIcon,
            titleBarActionIconRes = CLIcons.clSamplePowerSettingsIcon,
            titleBarActionIconContentDescription = "Settings",
            titleBarAction = ActionUtils.actionStartDemoActivity("Power settings title bar action"),
            items = items,
            checkedItems = checkedItems,
            actionButtonClick = checkItemAction,
        )
    }
}

class ClActionListAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ClActionListAppWidget()
}

@Composable
fun ClActionListLayout(
    title: String,
    @DrawableRes titleIconRes: Int,
    @DrawableRes titleBarActionIconRes: Int,
    titleBarActionIconContentDescription: String,
    titleBarAction: Action,
    items: List<ClActionListItem>,
    checkedItems: List<String>,
    actionButtonClick: (String) -> Unit,
) {
    fun titleBar(): @Composable (() -> Unit) = {
        TitleBar(
            startIcon = ImageProvider(titleIconRes),
            title =
                title.takeIf {
                    ClActionListLayoutSize.fromLocalSize() != ClActionListLayoutSize.Small
                } ?: "",
            iconColor = GlanceTheme.colors.primary,
            textColor = GlanceTheme.colors.onSurface,
            actions = {
                CircleIconButton(
                    imageProvider = ImageProvider(titleBarActionIconRes),
                    contentDescription = titleBarActionIconContentDescription,
                    contentColor = GlanceTheme.colors.secondary,
                    backgroundColor = null, // transparent
                    onClick = titleBarAction,
                )
            },
        )
    }

    val scaffoldTopPadding =
        if (ClActionListLayoutSize.showTitleBar()) {
            0.dp
        } else {
            ClActionListLayoutDimensions.widgetPadding
        }

    Scaffold(
        backgroundColor = GlanceTheme.colors.widgetBackground,
        modifier = GlanceModifier.padding(top = scaffoldTopPadding),
        titleBar =
            if (ClActionListLayoutSize.showTitleBar()) {
                titleBar()
            } else {
                null
            },
    ) {
        Content(items = items, checkedItems = checkedItems, actionButtonOnClick = actionButtonClick)
    }
}

@Composable
private fun Content(
    items: List<ClActionListItem>,
    checkedItems: List<String>,
    actionButtonOnClick: (String) -> Unit,
) {
    Box(modifier = GlanceModifier.padding(bottom = ClActionListLayoutDimensions.widgetPadding)) {
        if (items.isEmpty()) {
            ClEmptyListContent()
        } else {
            ClListView(
                items = items,
                checkedItems = checkedItems,
                actionButtonOnClick = actionButtonOnClick,
            )
        }
    }
}

@Composable
private fun ClListView(
    items: List<ClActionListItem>,
    checkedItems: List<String>,
    actionButtonOnClick: (String) -> Unit,
) {
    ClRoundedScrollingLazyColumn(
        modifier =
            GlanceModifier.fillMaxSize()
                .widgetInnerCornerRadius(ClActionListLayoutDimensions.widgetPadding),
        items = items,
        verticalItemsSpacing = ClActionListLayoutDimensions.verticalSpacing,
        itemContentProvider = { item ->
            FilledClActionListItem(
                item = item,
                isChecked = checkedItems.contains(item.key),
                actionButtonClick = actionButtonOnClick,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        },
    )
}

@OptIn(ExperimentalGlanceApi::class)
@Composable
private fun FilledClActionListItem(
    item: ClActionListItem,
    actionButtonClick: (String) -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    isChecked: Boolean,
) {
    @Composable
    fun Title() {
        Text(
            text = item.title,
            style = ClActionListLayoutTextStyles.titleText(isChecked),
            maxLines = 1,
            modifier = GlanceModifier.semantics { contentDescription = "" },
        )
    }

    @Composable
    fun SupportingText() {
        Text(
            text =
                if (isChecked) {
                    item.onSupportingText
                } else {
                    item.offSupportingText
                },
            style = ClActionListLayoutTextStyles.supportingText(isChecked),
            maxLines = 2,
            modifier = GlanceModifier.semantics { contentDescription = "" },
        )
    }

    @Composable
    fun StateIndicatorIcon() {
        Box(
            GlanceModifier.size(ClActionListLayoutDimensions.stateIconBackgroundSize)
                .cornerRadius(ClActionListLayoutDimensions.circularCornerRadius),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(item.stateIconRes),
                modifier = modifier.size(ClActionListLayoutDimensions.stateIconSize),
                contentDescription = null,
                colorFilter =
                    ColorFilter.tint(
                        if (isChecked) {
                            GlanceTheme.colors.onPrimary
                        } else {
                            GlanceTheme.colors.onSurfaceVariant
                        }
                    ),
            )
        }
    }

    @Composable
    fun AdditionalActionButton(@DrawableRes resId: Int, contentDescription: String?) {
        CircleIconButton(
            imageProvider = ImageProvider(resId),
            contentDescription = contentDescription,
            onClick = ActionUtils.actionStartDemoActivity("$contentDescription ${item.key}"),
            backgroundColor = null,
            contentColor =
                if (isChecked) {
                    GlanceTheme.colors.onPrimary
                } else {
                    GlanceTheme.colors.onSurface
                },
        )
    }

    fun combinedContentDescription(): String {
        val contentDescriptionBuilder = StringBuilder()
        contentDescriptionBuilder.append(item.title)
        contentDescriptionBuilder.append(" ")
        contentDescriptionBuilder.append(
            if (isChecked) {
                item.onSupportingText
            } else {
                item.offSupportingText
            }
        )
        contentDescriptionBuilder.append(" ")
        contentDescriptionBuilder.append(
            if (isChecked) {
                item.onStateActionContentDescription
            } else {
                item.offStartActionContentDescription
            }
        )
        return contentDescriptionBuilder.toString()
    }

    ClListItem(
        modifier =
            modifier
                .semantics { contentDescription = combinedContentDescription() }
                .filledContainer(isChecked)
                .clickable(key = "${LocalSize.current} ${item.key}") {
                    actionButtonClick(item.key)
                },
        contentSpacing = ClActionListLayoutDimensions.itemContentSpacing,
        leadingContent =
            takeComposableIf(
                ClActionListLayoutSize.fromLocalSize() != ClActionListLayoutSize.Small
            ) {
                StateIndicatorIcon()
            },
        headlineContent = { Title() },
        supportingContent = { SupportingText() },
        trailingContent =
            if (item.trailingIconButtonRes != null) {
                {
                    AdditionalActionButton(
                        item.trailingIconButtonRes,
                        item.trailingIconButtonContentDescription,
                    )
                }
            } else null,
    )
}

@Composable
private fun GlanceModifier.filledContainer(isChecked: Boolean): GlanceModifier {
    return widgetInnerCornerRadius(ClActionListLayoutDimensions.widgetPadding)
        .padding(ClActionListLayoutDimensions.filledItemPadding)
        .background(
            if (isChecked) {
                GlanceTheme.colors.primary
            } else {
                GlanceTheme.colors.secondaryContainer
            }
        )
}

data class ClActionListItem(
    val key: String,
    val title: String,
    val onSupportingText: String,
    val offSupportingText: String,
    @DrawableRes val stateIconRes: Int,
    val onStateActionContentDescription: String,
    val offStartActionContentDescription: String,
    @DrawableRes val trailingIconButtonRes: Int? = null,
    val trailingIconButtonContentDescription: String? = null,
)

private enum class ClActionListLayoutSize(val maxWidth: Dp) {
    Small(maxWidth = 260.dp),
    Medium(maxWidth = 439.dp),
    Large(maxWidth = 644.dp);

    companion object {
        @Composable
        fun fromLocalSize(): ClActionListLayoutSize {
            val width = LocalSize.current.width

            return if (width >= Medium.maxWidth) {
                Large
            } else if (width >= Small.maxWidth) {
                Medium
            } else {
                Small
            }
        }

        @Composable
        fun showTitleBar(): Boolean {
            return LocalSize.current.height >= 180.dp
        }
    }
}

private object ClActionListLayoutTextStyles {
    @Composable
    fun titleText(checked: Boolean): TextStyle =
        TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize =
                if (ClActionListLayoutSize.fromLocalSize() == ClActionListLayoutSize.Small) {
                    14.sp
                } else {
                    16.sp
                },
            color =
                if (checked) {
                    GlanceTheme.colors.onPrimary
                } else {
                    GlanceTheme.colors.onSurface
                },
        )

    @Composable
    fun supportingText(isChecked: Boolean): TextStyle =
        TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color =
                if (isChecked) {
                    GlanceTheme.colors.onPrimary
                } else {
                    GlanceTheme.colors.onSurfaceVariant
                },
        )
}

private object ClActionListLayoutDimensions {
    const val gridCells = 2
    val widgetPadding = 12.dp
    val filledItemPadding = 12.dp
    val verticalSpacing = 4.dp
    val itemContentSpacing = 4.dp
    val stateIconBackgroundSize = 48.dp
    val stateIconSize = 24.dp
    val circularCornerRadius = 200.dp
}

class FakeClActionListDataRepository {
    private val items = MutableStateFlow<List<ClActionListItem>>(listOf())
    private val checkedItems = MutableStateFlow<List<String>>(listOf())

    fun items(): Flow<List<ClActionListItem>> = items

    fun checkedItems(): Flow<List<String>> = checkedItems

    fun checkItem(key: String) {
        if (checkedItems.value.contains(key)) {
            checkedItems.value = checkedItems.value.toMutableList().apply { remove(key) }
        } else {
            checkedItems.value = checkedItems.value.toMutableList().apply { add(key) }
        }
    }

    fun load(): List<ClActionListItem> {
        items.value = demoData
        checkedItems.value = listOf()
        return items.value
    }

    companion object {
        private val repositories = mutableMapOf<GlanceId, FakeClActionListDataRepository>()

        val demoData =
            listOf(
                ClActionListItem(
                    key = "0",
                    title = "Living room light",
                    onSupportingText = "ON",
                    offSupportingText = "OFF",
                    stateIconRes = CLIcons.clSampleBulbIcon,
                    onStateActionContentDescription = "",
                    offStartActionContentDescription = "",
                ),
                ClActionListItem(
                    key = "1",
                    title = "Thermostat",
                    onSupportingText = "ON (74°F)",
                    offSupportingText = "OFF",
                    stateIconRes = CLIcons.clSampleThermostatIcon,
                    onStateActionContentDescription = "",
                    offStartActionContentDescription = "",
                    trailingIconButtonRes = CLIcons.clSampleArrowRightIcon,
                    trailingIconButtonContentDescription = "Edit temperature",
                ),
                ClActionListItem(
                    key = "2",
                    title = "A/C",
                    onSupportingText = "ON",
                    offSupportingText = "OFF",
                    stateIconRes = CLIcons.clSampleAcIcon,
                    onStateActionContentDescription = "",
                    offStartActionContentDescription = "",
                ),
                ClActionListItem(
                    key = "3",
                    title = "Front door",
                    onSupportingText = "Open",
                    offSupportingText = "Closed",
                    stateIconRes = CLIcons.clSampleDoorIcon,
                    onStateActionContentDescription = "",
                    offStartActionContentDescription = "",
                ),
                ClActionListItem(
                    key = "4",
                    title = "Bedroom light",
                    onSupportingText = "ON",
                    offSupportingText = "OFF",
                    stateIconRes = CLIcons.clSampleBulbIcon,
                    onStateActionContentDescription = "",
                    offStartActionContentDescription = "",
                ),
                ClActionListItem(
                    key = "5",
                    title = "Hallway light",
                    onSupportingText = "ON",
                    offSupportingText = "OFF",
                    stateIconRes = CLIcons.clSampleBulbIcon,
                    onStateActionContentDescription = "",
                    offStartActionContentDescription = "",
                ),
            )

        fun getActionListDataRepo(glanceId: GlanceId): FakeClActionListDataRepository {
            return synchronized(repositories) {
                repositories.computeIfAbsent(glanceId) { FakeClActionListDataRepository() }
            }
        }

        fun cleanUp(glanceId: GlanceId) {
            synchronized(repositories) { repositories.remove(glanceId) }
        }
    }
}
