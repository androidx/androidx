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
package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastForEach

/**
 * An [AppBarRow] arranges its children in a horizontal sequence, and if any children overflow the
 * constraints, an overflow indicator is displayed.
 *
 * This composable lays out its children from left to right in LTR layouts and from right to left in
 * RTL layouts. If the children's combined width exceeds the available width, [overflowIndicator] is
 * displayed at the end of the row, replacing the content that otherwise cannot fit. The items are
 * constructed through a DSL in [AppBarRowScope]. Each item provides a way to render itself in the
 * row layout, and an alternative way, to render inside of a dropdown menu, when there is overflow.
 *
 * A sample with an [TopAppBar], that varies the number of actions shown.
 *
 * @sample androidx.compose.material3.samples.SimpleTopAppBarWithAdaptiveActions
 * @param overflowIndicator A composable that is displayed at the end of the row when the content
 *   overflows. It receives an [AppBarMenuState] instance.
 * @param modifier The modifier to be applied to the row.
 * @param maxItemCount the max amount of items that should render in the row, before starting to use
 *   the overflow menu. Consider that using large items or small constraints, will reduce the
 *   effective maximum. Note: If the number of items supplied is bigger than max, at most max - 1
 *   items will render, since the last one will be dedicated to the overflow composable.
 * @param content The content to be arranged in the row, defined using a dsl with [AppBarRowScope].
 */
@Composable
@Suppress("ComposableLambdaParameterPosition", "KotlinDefaultParameterOrder")
fun AppBarRow(
    overflowIndicator: @Composable (AppBarMenuState) -> Unit,
    modifier: Modifier = Modifier,
    maxItemCount: Int = Int.MAX_VALUE,
    content: AppBarRowScope.() -> Unit,
) {
    val latestContent = rememberUpdatedState(content)
    val scope by remember {
        derivedStateOf { AppBarRowScopeImpl(AppBarScopeImpl()).apply(latestContent.value) }
    }
    val menuState = remember { AppBarMenuState() }
    val overflowState = rememberAppBarOverflowState()
    val measurePolicy =
        remember(overflowState, maxItemCount) {
            OverflowMeasurePolicy(overflowState, maxItemCount, isVertical = false)
        }

    Layout(
        contents =
            listOf(
                { scope.items.fastForEach { it.AppbarContent() } },
                {
                    Box {
                        overflowIndicator(menuState)
                        DropdownMenu(
                            expanded = menuState.isExpanded,
                            onDismissRequest = { menuState.dismiss() },
                        ) {
                            scope.items
                                .subList(
                                    overflowState.visibleItemCount,
                                    overflowState.totalItemCount,
                                )
                                .fastForEach { item -> item.MenuContent(menuState) }
                        }
                    }
                },
            ),
        modifier = modifier,
        measurePolicy = measurePolicy,
    )
}

/** DSL scope for building the content of an [AppBarRow]. */
interface AppBarRowScope : AppBarScope

private class AppBarRowScopeImpl(val impl: AppBarScopeImpl) :
    AppBarRowScope, AppBarScope by impl, AppBarItemProvider by impl
