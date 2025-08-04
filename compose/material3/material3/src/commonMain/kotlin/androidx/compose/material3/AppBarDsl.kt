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

package androidx.compose.material3

import androidx.compose.material3.internal.subtractConstraintSafely
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import kotlin.math.max

/** DSL scope for building the content of an [AppBarRow] and [AppBarColumn]. */
sealed interface AppBarScope {

    /**
     * Adds a clickable item to the [AppBarRow] or [AppBarColumn].
     *
     * @param onClick The action to perform when the item is clicked.
     * @param icon The composable representing the item's icon.
     * @param enabled Whether the item is enabled.
     * @param label The text label for the item, used in the overflow menu.
     */
    fun clickableItem(
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        label: String,
        enabled: Boolean = true,
    )

    /**
     * Adds a toggleable item to the [AppBarRow] or [AppBarColumn].
     *
     * @param checked Whether the item is currently checked.
     * @param onCheckedChange The action to perform when the item's checked state changes.
     * @param icon The composable representing the item's icon.
     * @param enabled Whether the item is enabled.
     * @param label The text label for the item, used in the overflow menu.
     */
    fun toggleableItem(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        icon: @Composable () -> Unit,
        label: String,
        enabled: Boolean = true,
    )

    /**
     * Adds a custom item to the [AppBarRow] or [AppBarColumn].
     *
     * @param appbarContent The composable to display in the app bar.
     * @param menuContent The composable to display in the overflow menu. It receives an
     *   [AppBarMenuState] instance.
     */
    fun customItem(
        appbarContent: @Composable () -> Unit,
        menuContent: @Composable (AppBarMenuState) -> Unit,
    )
}

internal interface AppBarItemProvider {
    val itemsCount: Int
    val items: MutableList<AppBarItem>
}

internal interface AppBarItem {

    /** Composable function to render the item in the app bar. */
    @Composable fun AppbarContent()

    /**
     * Composable function to render the item in the overflow menu.
     *
     * @param state The [AppBarMenuState] instance.
     */
    @Composable fun MenuContent(state: AppBarMenuState)
}

internal class AppBarScopeImpl() : AppBarScope, AppBarItemProvider {

    override val items = mutableListOf<AppBarItem>()

    override val itemsCount: Int
        get() = items.size

    override fun clickableItem(
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        label: String,
        enabled: Boolean,
    ) {
        items.add(
            ClickableAppBarItem(onClick = onClick, icon = icon, enabled = enabled, label = label)
        )
    }

    override fun toggleableItem(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        icon: @Composable () -> Unit,
        label: String,
        enabled: Boolean,
    ) {
        items.add(
            ToggleableAppBarItem(
                checked = checked,
                onCheckedChange = onCheckedChange,
                icon = icon,
                enabled = enabled,
                label = label,
            )
        )
    }

    override fun customItem(
        appbarContent: @Composable () -> Unit,
        menuContent: @Composable (AppBarMenuState) -> Unit,
    ) {
        items.add(CustomAppBarItem(appbarContent, menuContent))
    }
}

internal class ClickableAppBarItem(
    private val onClick: () -> Unit,
    private val icon: @Composable () -> Unit,
    private val enabled: Boolean,
    private val label: String,
) : AppBarItem {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun AppbarContent() {
        // Icon buttons should have a tooltip associated with them.
        TooltipBox(
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(label) } },
            state = rememberTooltipState(),
        ) {
            IconButton(onClick = onClick, enabled = enabled, content = icon)
        }
    }

    @Composable
    override fun MenuContent(state: AppBarMenuState) {
        DropdownMenuItem(
            enabled = enabled,
            text = { Text(label) },
            onClick = {
                onClick()
                state.dismiss()
            },
        )
    }
}

internal class ToggleableAppBarItem(
    private val checked: Boolean,
    private val onCheckedChange: (Boolean) -> Unit,
    private val icon: @Composable () -> Unit,
    private val enabled: Boolean,
    private val label: String,
) : AppBarItem {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun AppbarContent() {
        // Icon buttons should have a tooltip associated with them.
        TooltipBox(
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(label) } },
            state = rememberTooltipState(),
        ) {
            IconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                content = icon,
            )
        }
    }

    @Composable
    override fun MenuContent(state: AppBarMenuState) {
        DropdownMenuItem(
            enabled = enabled,
            text = { Text(label) },
            onClick = {
                onCheckedChange(!checked)
                state.dismiss()
            },
        )
    }
}

internal class CustomAppBarItem(
    private val appbarContent: @Composable () -> Unit,
    private val menuContent: @Composable (AppBarMenuState) -> Unit,
) : AppBarItem {
    @Composable
    override fun AppbarContent() {
        appbarContent()
    }

    @Composable
    override fun MenuContent(state: AppBarMenuState) {
        menuContent(state)
    }
}

/** State class for the overflow menu in [AppBarRow] and [AppBarColumn]. */
class AppBarMenuState {

    /** Indicates whether the overflow menu is currently expanded. */
    var isExpanded by mutableStateOf(false)
        private set

    /** Closes the overflow menu. */
    fun dismiss() {
        isExpanded = false
    }

    /** Show the overflow menu. */
    fun show() {
        isExpanded = true
    }
}

internal interface AppBarOverflowState {

    var totalItemCount: Int

    var visibleItemCount: Int
}

@Composable
internal fun rememberAppBarOverflowState(): AppBarOverflowState {
    return rememberSaveable(saver = AppBarOverflowStateImpl.Saver) { AppBarOverflowStateImpl() }
}

private class AppBarOverflowStateImpl : AppBarOverflowState {
    override var totalItemCount: Int by mutableIntStateOf(0)
    override var visibleItemCount: Int by mutableIntStateOf(0)

    companion object {
        val Saver: Saver<AppBarOverflowStateImpl, *> =
            Saver(
                save = { listOf(it.totalItemCount, it.visibleItemCount) },
                restore = {
                    AppBarOverflowStateImpl().apply {
                        totalItemCount = it[0]
                        visibleItemCount = it[1]
                    }
                },
            )
    }
}

internal class OverflowMeasurePolicy(
    private val overflowState: AppBarOverflowState,
    val maxItemCount: Int,
    private val isVertical: Boolean = false,
) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val (contentMeasurables, overflowMeasurables) = measurables

        overflowState.totalItemCount = contentMeasurables.size

        // First, reserve space for the overflow indicator
        val reservedOverflowSpace =
            if (isVertical) {
                overflowMeasurables.fastMaxOfOrNull { it.maxIntrinsicHeight(constraints.maxWidth) }
                    ?: 0
            } else {
                overflowMeasurables.fastMaxOfOrNull { it.maxIntrinsicWidth(constraints.maxHeight) }
                    ?: 0
            }
        var remainingSpace = if (isVertical) constraints.maxHeight else constraints.maxWidth
        remainingSpace = remainingSpace.subtractConstraintSafely(reservedOverflowSpace)

        var currentSpace = 0
        val contentPlaceables = mutableListOf<Placeable>()

        // Measure content until it doesn't fit
        for (i in contentMeasurables.indices) {
            val placeable = contentMeasurables[i].measure(looseConstraints)
            val isLastContent = i == contentMeasurables.lastIndex
            if (!isLastContent && (i == maxItemCount - 1)) {
                break
            }
            val placeableSpace = if (isVertical) placeable.height else placeable.width
            val hasSufficientSpace =
                placeableSpace <= remainingSpace ||
                    (isLastContent && placeableSpace <= remainingSpace + reservedOverflowSpace)

            if (hasSufficientSpace) {
                contentPlaceables.add(placeable)
                currentSpace += placeableSpace
                remainingSpace = remainingSpace.subtractConstraintSafely(placeableSpace)
            } else {
                break
            }
        }

        overflowState.visibleItemCount = contentPlaceables.size

        // Measure overflow if needed
        val overflowPlaceables =
            if (contentPlaceables.size != contentMeasurables.size) {
                val overflowConstraints =
                    if (isVertical) {
                        looseConstraints.copy(maxHeight = remainingSpace + reservedOverflowSpace)
                    } else {
                        looseConstraints.copy(maxWidth = remainingSpace + reservedOverflowSpace)
                    }
                overflowMeasurables.fastMap { it.measure(overflowConstraints) }
            } else {
                null
            }

        val overflowSpace =
            if (isVertical) {
                overflowPlaceables?.fastMaxOfOrNull { it.height } ?: 0
            } else {
                overflowPlaceables?.fastMaxOfOrNull { it.width } ?: 0
            }
        currentSpace += overflowSpace

        val childrenMaxSpace =
            if (isVertical) {
                max(
                    contentPlaceables.fastMaxOfOrNull { it.width } ?: 0,
                    overflowPlaceables?.fastMaxOfOrNull { it.width } ?: 0,
                )
            } else {
                max(
                    contentPlaceables.fastMaxOfOrNull { it.height } ?: 0,
                    overflowPlaceables?.fastMaxOfOrNull { it.height } ?: 0,
                )
            }

        var width: Int
        var height: Int
        return if (isVertical) {
            width = constraints.constrainWidth(childrenMaxSpace)
            height = constraints.constrainHeight(currentSpace)

            layout(width, height) {
                var currentY = 0
                contentPlaceables.fastForEach {
                    it.placeRelative(x = 0, y = currentY)
                    currentY += it.height
                }
                overflowPlaceables?.fastForEach { it.placeRelative(x = 0, y = currentY) }
            }
        } else {
            width = constraints.constrainWidth(currentSpace)
            height = constraints.constrainHeight(childrenMaxSpace)

            layout(width, height) {
                var currentX = 0
                contentPlaceables.fastForEach {
                    it.placeRelative(x = currentX, y = 0)
                    currentX += it.width
                }
                overflowPlaceables?.fastForEach { it.placeRelative(x = currentX, y = 0) }
            }
        }
    }
}
