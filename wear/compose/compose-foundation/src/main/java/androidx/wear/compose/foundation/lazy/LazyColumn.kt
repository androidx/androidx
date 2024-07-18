/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/**
 * The vertically scrolling list that only composes and lays out the currently visible items. This
 * is a wear specific version of LazyColumn that adds support for scaling and morphing animations.
 *
 * @sample androidx.wear.compose.foundation.samples.LazyColumnLettersSample
 * @param modifier The modifier to be applied to the layout.
 * @param state The state object to be used to control the list and the applied layout.
 * @param verticalArrangement The vertical arrangement of the items.
 * @param horizontalAlignment The horizontal alignment of the items.
 * @param flingBehavior The fling behavior to be used for the list.
 * @param userScrollEnabled Whether the user should be able to scroll the list.
 * @param content The content of the list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyColumnState = rememberLazyColumnState(),
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(
            space = 4.dp,
            // TODO: b/352513793 - Add support for reverseLayout.
            alignment = Alignment.Top
        ),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyColumnScope.() -> Unit
) {
    val latestContent = rememberUpdatedState(newValue = content)

    val itemProviderLambda by
        remember(state) {
            derivedStateOf(referentialEqualityPolicy()) {
                {
                    LazyColumnItemProvider(
                        scope = LazyColumnScopeImpl(latestContent.value),
                        state = state
                    )
                }
            }
        }

    val measurePolicy =
        rememberLazyColumnMeasurePolicy(
            itemProviderLambda = itemProviderLambda,
            state = state,
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
        )
    val reverseDirection =
        ScrollableDefaults.reverseDirection(
            LocalLayoutDirection.current,
            Orientation.Vertical,
            reverseScrolling = false
        )
    LazyLayout(
        itemProvider = itemProviderLambda,
        modifier =
            modifier
                .then(state.remeasurementModifier)
                .scrollable(
                    state = state,
                    reverseDirection = reverseDirection,
                    enabled = userScrollEnabled,
                    orientation = Orientation.Vertical,
                    flingBehavior = flingBehavior,
                ),
        measurePolicy = measurePolicy
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyColumnItemProvider(
    val scope: LazyColumnScopeImpl,
    val state: LazyColumnState,
) : LazyLayoutItemProvider {
    override val itemCount: Int
        get() = scope.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        // TODO: b/352511749 - Use keys to identify items.
        val itemScope = remember(index) { LazyColumnItemScopeImpl(index, state = state) }
        scope.withInterval(index) { localIndex, content -> content.item(itemScope, localIndex) }
    }
}
