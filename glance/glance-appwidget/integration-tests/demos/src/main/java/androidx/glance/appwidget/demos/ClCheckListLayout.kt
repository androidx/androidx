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
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.demos.ClCheckListLayoutSize.Companion.isWiderThan
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Internal copy of a Canonical Layout for testing. See the github repo for the official samples.
 * https://github.com/android/platform-samples/tree/main/samples/user-interface/appwidgets/src/main/java/com/example/platform/ui/appwidgets/glance
 */
class ClCheckListAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = FakeClCheckListDataRepository.getCheckListDataRepo(id)

        val initialItems = withContext(Dispatchers.Default) { repo.load() }

        provideContent {
            val items by repo.items().collectAsState(initial = initialItems)
            val checkedItems by repo.checkedItems().collectAsState(initial = emptyList())

            GlanceTheme {
                WidgetContent(
                    items = items,
                    checkedItems = checkedItems,
                    checkItemAction = { key: String -> repo.checkItem(key) },
                )
            }
        }
    }

    @Composable
    fun WidgetContent(
        items: List<ClCheckListItem>,
        checkedItems: List<String>,
        checkItemAction: (String) -> Unit,
    ) {
        ClCheckListLayout(
            title = "Checklist",
            titleIconRes = CLIcons.clSamplePinIcon,
            titleBarActionIconRes = CLIcons.sampleAddIcon,
            titleBarActionIconContentDescription = "Add",
            titleBarAction = ActionUtils.actionStartDemoActivity("Add icon in title bar"),
            items = items,
            checkedItems = checkedItems,
            checkButtonContentDescription = "Mark as done",
            checkedIconRes = CLIcons.clSampleCheckedCircleIcon,
            unCheckedIconRes = CLIcons.clSampleCircleIcon,
            onCheck = checkItemAction,
        )
    }
}

class ClCheckListAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ClCheckListAppWidget()
}

@Composable
fun ClCheckListLayout(
    title: String,
    @DrawableRes titleIconRes: Int,
    @DrawableRes titleBarActionIconRes: Int,
    titleBarActionIconContentDescription: String,
    titleBarAction: Action,
    items: List<ClCheckListItem>,
    checkedItems: List<String>,
    @DrawableRes checkedIconRes: Int,
    @DrawableRes unCheckedIconRes: Int,
    checkButtonContentDescription: String,
    onCheck: (String) -> Unit,
) {
    val checkListLayoutSize = ClCheckListLayoutSize.fromLocalSize()

    fun titleBar(): @Composable (() -> Unit) = {
        TitleBar(
            startIcon = ImageProvider(titleIconRes),
            title = title.takeIf { checkListLayoutSize != ClCheckListLayoutSize.Small } ?: "",
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
        if (ClCheckListLayoutSize.showTitleBar()) {
            0.dp
        } else {
            ClCheckListLayoutDimensions.widgetPadding
        }

    Scaffold(
        backgroundColor = GlanceTheme.colors.widgetBackground,
        horizontalPadding = ClCheckListLayoutDimensions.scaffoldHorizontalPadding,
        modifier =
            GlanceModifier.padding(
                top = scaffoldTopPadding,
                bottom = ClCheckListLayoutDimensions.widgetPadding,
            ),
        titleBar =
            if (ClCheckListLayoutSize.showTitleBar()) {
                titleBar()
            } else {
                null
            },
    ) {
        if (items.isEmpty()) {
            ClEmptyListContent()
        } else {
            Content(
                items = items,
                checkedItems = checkedItems,
                onCheck = onCheck,
                checkedIconRes = checkedIconRes,
                unCheckedIconRes = unCheckedIconRes,
                checkButtonContentDescription = checkButtonContentDescription,
            )
        }
    }
}

@Composable
private fun Content(
    items: List<ClCheckListItem>,
    checkedItems: List<String>,
    onCheck: (String) -> Unit,
    @DrawableRes checkedIconRes: Int,
    @DrawableRes unCheckedIconRes: Int,
    checkButtonContentDescription: String,
) {
    ClRoundedScrollingLazyColumn(
        modifier = GlanceModifier.fillMaxSize(),
        items = items,
        verticalItemsSpacing = ClCheckListLayoutDimensions.verticalListItemSpacing,
        itemContentProvider = { item ->
            ClCheckListItem(
                item = item,
                isChecked = checkedItems.contains(item.key),
                onCheck = onCheck,
                checkedIconRes = checkedIconRes,
                unCheckedIconRes = unCheckedIconRes,
                checkButtonContentDescription = checkButtonContentDescription,
            )
        },
    )
}

@Composable
private fun ClCheckListItem(
    item: ClCheckListItem,
    @DrawableRes checkedIconRes: Int,
    @DrawableRes unCheckedIconRes: Int,
    checkButtonContentDescription: String,
    onCheck: (String) -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    isChecked: Boolean,
) {
    @Composable
    fun CheckButton() {
        CircleIconButton(
            imageProvider =
                if (isChecked) {
                    ImageProvider(checkedIconRes)
                } else {
                    ImageProvider(unCheckedIconRes)
                },
            backgroundColor = null, // to show transparent background
            contentColor = GlanceTheme.colors.secondary,
            contentDescription = checkButtonContentDescription,
            enabled = !isChecked,
            onClick = { onCheck(item.key) },
            key = "${LocalSize.current} ${item.key}",
        )
    }

    @Composable
    fun Title() {
        Text(text = item.title, style = ClCheckListLayoutTextStyles.titleText, maxLines = 2)
    }

    @Composable
    fun SupportingText() {
        Text(
            text = item.supportingText,
            style = ClCheckListLayoutTextStyles.supportingText,
            maxLines = 2,
        )
    }

    @Composable
    fun TrailingActions() {
        TrailingIconButtonSet(
            leadingButtonRes = CLIcons.clSampleEditIcon,
            leadingButtonContentDescription = "Edit",
            leadingButtonOnClick =
                ActionUtils.actionStartDemoActivity(message = "Edit click on item: ${item.key}"),
            middleButtonRes = CLIcons.clSampleSnoozeIcon,
            middleButtonContentDescription = "Snooze",
            middleButtonOnClick =
                ActionUtils.actionStartDemoActivity(message = "Snooze click on item: ${item.key}"),
            trailingButtonRes = CLIcons.clSampleDeleteIcon,
            trailingButtonContentDescription = "Delete",
            trailingButtonOnClick =
                ActionUtils.actionStartDemoActivity(message = "Delete click on item: ${item.key}"),
        )
    }

    val rowEndPadding =
        if (item.hasTrailingIcons) {
            0.dp
        } else {
            ClCheckListLayoutDimensions.checkListRowEndPadding
        }

    ClListItem(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = ClCheckListLayoutDimensions.checkListRowStartPadding,
                    end = rowEndPadding,
                ),
        contentSpacing = 0.dp,
        leadingContent = { CheckButton() },
        headlineContent = { Title() },
        supportingContent = { SupportingText() },
        trailingContent = takeComposableIf(item.hasTrailingIcons) { TrailingActions() },
    )
}

@Composable
private fun TrailingIconButtonSet(
    @DrawableRes leadingButtonRes: Int,
    leadingButtonContentDescription: String,
    leadingButtonOnClick: Action,
    @DrawableRes middleButtonRes: Int,
    middleButtonContentDescription: String,
    middleButtonOnClick: Action,
    @DrawableRes trailingButtonRes: Int,
    trailingButtonContentDescription: String,
    trailingButtonOnClick: Action,
) {
    val checkListLayoutSize = ClCheckListLayoutSize.fromLocalSize()

    if (checkListLayoutSize.isWiderThan(ClCheckListLayoutSize.Medium)) {
        CircleIconButton(
            imageProvider = ImageProvider(leadingButtonRes),
            backgroundColor = null,
            contentColor = GlanceTheme.colors.secondary,
            contentDescription = leadingButtonContentDescription,
            onClick = leadingButtonOnClick,
        )
        if (checkListLayoutSize.isWiderThan(ClCheckListLayoutSize.Large)) {
            CircleIconButton(
                imageProvider = ImageProvider(middleButtonRes),
                backgroundColor = null,
                contentColor = GlanceTheme.colors.secondary,
                contentDescription = middleButtonContentDescription,
                onClick = middleButtonOnClick,
            )
        }
        if (checkListLayoutSize.isWiderThan(ClCheckListLayoutSize.XLarge)) {
            CircleIconButton(
                imageProvider = ImageProvider(trailingButtonRes),
                backgroundColor = null,
                contentColor = GlanceTheme.colors.secondary,
                contentDescription = trailingButtonContentDescription,
                onClick = trailingButtonOnClick,
            )
        }
    }
}

data class ClCheckListItem(
    val key: String,
    val title: String,
    val supportingText: String,
    val hasTrailingIcons: Boolean = false,
)

private enum class ClCheckListLayoutSize(val maxWidth: Dp) {
    Small(maxWidth = 260.dp),
    Medium(maxWidth = 304.dp),
    Large(maxWidth = 348.dp),
    XLarge(maxWidth = 396.dp),
    XXLarge(maxWidth = Dp.Infinity);

    companion object {
        @Composable
        fun fromLocalSize(): ClCheckListLayoutSize {
            val size = LocalSize.current

            ClCheckListLayoutSize.values().forEach {
                if (size.width < it.maxWidth) {
                    return it
                }
            }
            throw IllegalStateException("No mapped size ")
        }

        fun ClCheckListLayoutSize.isWiderThan(checkListLayoutSize: ClCheckListLayoutSize): Boolean {
            return this.maxWidth > checkListLayoutSize.maxWidth
        }

        @Composable
        fun showTitleBar(): Boolean {
            return LocalSize.current.height >= 180.dp
        }
    }
}

private object ClCheckListLayoutTextStyles {
    val titleText: TextStyle
        @Composable
        get() =
            TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize =
                    if (ClCheckListLayoutSize.fromLocalSize() == ClCheckListLayoutSize.Small) {
                        14.sp
                    } else {
                        16.sp
                    },
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

private object ClCheckListLayoutDimensions {
    val widgetPadding = 12.dp
    val verticalListItemSpacing = 4.dp
    val scaffoldHorizontalPadding = 0.dp
    val checkListRowStartPadding = 2.dp
    val checkListRowEndPadding = widgetPadding
}

class FakeClCheckListDataRepository {
    private val items = MutableStateFlow<List<ClCheckListItem>>(listOf())
    private val checkedItems = MutableStateFlow<List<String>>(listOf())

    fun items(): Flow<List<ClCheckListItem>> = items

    fun checkedItems(): Flow<List<String>> = checkedItems

    @OptIn(DelicateCoroutinesApi::class)
    fun checkItem(key: String) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                checkedItems.value = checkedItems.value.toMutableList().apply { add(key) }
                delay(500)

                items.value =
                    items.value.toMutableList().apply { removeIf { item -> item.key == key } }
                checkedItems.value = checkedItems.value.toMutableList().apply { remove(key) }
            }
        }
    }

    fun load(): List<ClCheckListItem> {
        items.value = demoData
        checkedItems.value = listOf()

        return items.value
    }

    companion object {
        private val repositories = mutableMapOf<GlanceId, FakeClCheckListDataRepository>()

        val demoData =
            listOf(
                ClCheckListItem(
                    key = "0",
                    title = "Pay electricity bill",
                    supportingText = "Due in 10 days",
                ),
                ClCheckListItem(
                    key = "1",
                    title = "Prepare for the meeting",
                    supportingText = "Due tomorrow",
                ),
                ClCheckListItem(
                    key = "2",
                    title = "Renew lease",
                    supportingText = "Due in 1 month",
                ),
                ClCheckListItem(
                    key = "3",
                    title = "Plan the trip",
                    supportingText = "Due tomorrow",
                ),
                ClCheckListItem(key = "4", title = "Call plumber", supportingText = "Due today"),
                ClCheckListItem(
                    key = "5",
                    title = "Dentist appointment",
                    supportingText = "Due in 1 week",
                ),
                ClCheckListItem(
                    key = "6",
                    title = "Eye appointment",
                    supportingText = "Due in 1 month",
                ),
            )

        fun getCheckListDataRepo(glanceId: GlanceId): FakeClCheckListDataRepository {
            return synchronized(repositories) {
                repositories.computeIfAbsent(glanceId) { FakeClCheckListDataRepository() }
            }
        }

        fun cleanUp(glanceId: GlanceId) {
            synchronized(repositories) { repositories.remove(glanceId) }
        }
    }
}
