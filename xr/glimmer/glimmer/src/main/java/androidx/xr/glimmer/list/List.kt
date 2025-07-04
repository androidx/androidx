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

package androidx.xr.glimmer.list

import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/**
 * This is a scrolling list component that only composes and lays out the currently visible items.
 * It is based on [androidx.compose.foundation.lazy.LazyColumn], but with extra functionality and
 * customized behavior required for Glimmer. Glimmer applications should always use VerticalList
 * instead of LazyColumn to ensure correct behavior.
 *
 * The [content] block defines a DSL which allows you to emit items of different types. For example
 * you can use [ListScope.item] to add a single item and [ListScope.items] to add a list of items.
 *
 * @sample androidx.xr.glimmer.samples.VerticalListSample
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the list's state.
 * @param contentPadding a padding around the whole content. This will add padding for the. content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first item or after the last one.
 * @param userScrollEnabled If user gestures are enabled.
 * @param reverseLayout reverses the direction of scrolling and layout.
 * @param horizontalAlignment aligns items horizontally.
 * @param verticalArrangement is arrangement for items. This only applies if the content is smaller
 *   than the viewport.
 * @param content a block which describes the content. Inside this block you can use methods like
 *   [ListScope.item] to add a single item or [ListScope.items] to add a list of items.
 */
@Composable
public fun VerticalList(
    modifier: Modifier = Modifier,
    state: ListState = rememberListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: ListScope.() -> Unit,
): Unit =
    List(
        orientation = Orientation.Vertical,
        modifier = modifier,
        state = state,
        reverseLayout = reverseLayout,
        contentPadding = contentPadding,
        userScrollEnabled = userScrollEnabled,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        verticalAlignment = null,
        horizontalArrangement = null,
        content = content,
    )

/**
 * The scrolling List list that only composes and lays out the currently visible items. The
 * [content] block defines a DSL which allows you to emit items of different types. For example you
 * can use [ListScope.item] to add a single item and [ListScope.items] to add a list of items.
 *
 * @param orientation The orientation in which to layout items in this list.
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the list's state.
 * @param contentPadding a padding around the whole content. This will add padding for the. content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first item or after the last one.
 * @param userScrollEnabled If user gestures are enabled.
 * @param reverseLayout reverses the direction of scrolling and layout.
 * @param horizontalAlignment aligns items horizontally. It's required and used only if
 *   [orientation] is [Orientation.Vertical].
 * @param verticalArrangement is arrangement for items. This only applies if the content is smaller
 *   than the viewport. It's required and used only if [orientation] is [Orientation.Vertical].
 * @param verticalAlignment aligns items vertically. It's required and used only if [orientation] is
 *   [Orientation.Horizontal].
 * @param horizontalArrangement is arrangement for items. This only applies if the content is
 *   smaller than the viewport. It's required and used only if [orientation] is
 *   [Orientation.Vertical].
 * @param content a block which describes the content. Inside this block you can use methods like
 *   [ListScope.item] to add a single item or [ListScope.items] to add a list of items.
 */
@Composable
internal fun List(
    orientation: Orientation,
    modifier: Modifier = Modifier,
    state: ListState = rememberListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    horizontalAlignment: Alignment.Horizontal? = null,
    verticalArrangement: Arrangement.Vertical? = null,
    verticalAlignment: Alignment.Vertical? = null,
    horizontalArrangement: Arrangement.Horizontal? = null,
    content: ListScope.() -> Unit,
) {
    val itemProvider = rememberGlimmerListItemProviderLambda(state, content)

    val semanticState = rememberGlimmerListSemanticState(state, orientation)

    val measurePolicy =
        rememberGlimmerListMeasurePolicy(
            itemProviderLambda = itemProvider,
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            orientation = orientation,
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            verticalAlignment = verticalAlignment,
            horizontalArrangement = horizontalArrangement,
        )

    val beyondBoundsModifier =
        if (userScrollEnabled) {
            Modifier.lazyLayoutBeyondBoundsModifier(
                state = rememberGlimmerListBeyondBoundsState(state),
                beyondBoundsInfo = state.beyondBoundsInfo,
                reverseLayout = reverseLayout,
                orientation = orientation,
            )
        } else {
            Modifier
        }

    LazyLayout(
        modifier =
            modifier
                .then(state.remeasurementModifier)
                .then(state.awaitLayoutModifier)
                .lazyLayoutSemantics(
                    itemProviderLambda = itemProvider,
                    state = semanticState,
                    orientation = orientation,
                    userScrollEnabled = userScrollEnabled,
                    reverseScrolling = reverseLayout,
                )
                .then(beyondBoundsModifier)
                .clipScrollableContainer(orientation)
                .scrollable(
                    state = state,
                    orientation = orientation,
                    enabled = userScrollEnabled,
                    reverseDirection =
                        ScrollableDefaults.reverseDirection(
                            layoutDirection = LocalLayoutDirection.current,
                            orientation = orientation,
                            reverseScrolling = reverseLayout,
                        ),
                    interactionSource = state.internalInteractionSource,
                ),
        itemProvider = itemProvider,
        measurePolicy = measurePolicy,
    )
}
