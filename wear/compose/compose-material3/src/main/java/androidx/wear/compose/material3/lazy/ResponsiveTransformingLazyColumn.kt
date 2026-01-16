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

package androidx.wear.compose.material3.lazy

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults

/**
 * A scrolling list that transforms its content items based on their position in the viewport. Items
 * in the list are lazy loaded.
 *
 * This component builds upon [TransformingLazyColumn] by providing responsive behavior,
 * automatically adjusting top and bottom padding based on the screen height and the
 * [ResponsiveItemType] of the first and last items in the list.
 *
 * The padding behavior follows Material Design guidelines for Wear OS, ensuring that content is
 * appropriately spaced from the screen edges and that scrolling interactions feel natural.
 * Specifically, it adjusts padding so the first item is initially well-positioned and the last item
 * can scroll to a comfortable viewing position.
 *
 * Example of a [ResponsiveTransformingLazyColumn] with default parameters:
 *
 * @sample androidx.wear.compose.material3.samples.SimpleResponsiveTransformingLazyColumnSample
 *
 * Example of a [ResponsiveTransformingLazyColumn] that snaps items to the center of the viewport:
 *
 * @sample androidx.wear.compose.material3.samples.ResponsiveTransformingLazyColumnWithSnapSample
 * @param modifier The modifier to be applied to the `ResponsiveTransformingLazyColumn`.
 * @param state The state object that can be used to control and observe the list's scroll position.
 * @param contentPadding Padding around the content. The final top and bottom padding will be the
 *   maximum of the values passed in here and the responsive padding calculated by the component to
 *   follow Material Design guidelines. This allows developers to enforce a minimum padding (e.g.
 *   for global screen insets) while still benefiting from responsive adjustments. Side padding
 *   (start and end) will be respected as passed in. Defaults to 0.dp.
 * @param reverseLayout reverse the direction of scrolling and layout, when `true` items will be
 *   composed from the bottom to the top
 * @param verticalArrangement The vertical arrangement of the items, to be used when there is enough
 *   space to show all the items. Note that only [Arrangement.Top], [Arrangement.Center] and
 *   [Arrangement.Bottom] arrangements (including their spacedBy variants, i.e., using spacedBy with
 *   [Alignment.Top], [Alignment.CenterVertically] and [Alignment.Bottom]) are supported, The
 *   default is [Arrangement.Top] when [reverseLayout] is false and [Arrangement.Bottom] when
 *   [reverseLayout] is true.
 * @param horizontalAlignment The horizontal alignment of the items.
 * @param flingBehavior Logic describing fling behavior for touch scroll. If snapping is required
 *   use [TransformingLazyColumnDefaults.snapFlingBehavior]. Note that when configuring fling or
 *   snap behavior, this flingBehavior parameter and the [rotaryScrollableBehavior] parameter that
 *   controls rotary scroll are expected to produce similar list scrolling. For example, if
 *   [rotaryScrollableBehavior] is set for snap (using [RotaryScrollableDefaults.snapBehavior]),
 *   [flingBehavior] should be set for snap as well (using
 *   [TransformingLazyColumnDefaults.snapFlingBehavior])
 * @param userScrollEnabled Whether the user should be able to scroll the list. This also affects
 *   scrolling with rotary.
 * @param rotaryScrollableBehavior Parameter for changing rotary scrollable behavior. Supports
 *   scroll [RotaryScrollableDefaults.behavior] and snap [RotaryScrollableDefaults.snapBehavior].
 *   Note that when configuring fling or snap behavior, this rotaryBehavior parameter and the
 *   [flingBehavior] parameter that controls touch scroll are expected to produce similar list
 *   scrolling. For example, if [rotaryScrollableBehavior] is set for snap (using
 *   [RotaryScrollableDefaults.snapBehavior]), [flingBehavior] should be set for snap as well (using
 *   [TransformingLazyColumnDefaults.snapFlingBehavior]). Can be null if rotary support is not
 *   required or when it should be handled externally - with a separate [Modifier.rotaryScrollable]
 *   modifier.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param content The DSL block that describes the content of the list using
 *   [ResponsiveTransformingLazyColumnScope].
 */
@Composable
public fun ResponsiveTransformingLazyColumn(
    modifier: Modifier = Modifier,
    state: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(
            space = 4.dp,
            alignment = if (!reverseLayout) Alignment.Top else Alignment.Bottom,
        ),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    rotaryScrollableBehavior: RotaryScrollableBehavior? = RotaryScrollableDefaults.behavior(state),
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: ResponsiveTransformingLazyColumnScope.() -> Unit,
) {
    val currentContent by rememberUpdatedState(content)

    val scope by remember {
        derivedStateOf { ResponsiveTransformingLazyColumnScopeImpl().apply(currentContent) }
    }

    val responsivePadding =
        rememberResponsiveContentPadding(
            firstItemTypeProvider = { scope.firstItemType },
            lastItemTypeProvider = { scope.lastItemType },
            minimumContentPadding = contentPadding,
        )

    TransformingLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = responsivePadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        rotaryScrollableBehavior = rotaryScrollableBehavior,
        overscrollEffect = overscrollEffect,
    ) {
        scope.content(this)
    }
}
