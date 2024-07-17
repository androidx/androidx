/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier

// TODO b/316559454 to remove @Composable from it and make it public
/** Scrolling related information to transform a layout into a "Scrollable Container" */
internal fun Modifier.scrollingContainer(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean,
    reverseDirection: Boolean,
    flingBehavior: FlingBehavior?,
    interactionSource: MutableInteractionSource?,
    bringIntoViewSpec: BringIntoViewSpec? = null,
    overscrollEffect: OverscrollEffect? = null
): Modifier {
    return clipScrollableContainer(orientation)
        .then(if (overscrollEffect == null) Modifier else Modifier.overscroll(overscrollEffect))
        .scrollable(
            orientation = orientation,
            reverseDirection = reverseDirection,
            enabled = enabled,
            interactionSource = interactionSource,
            flingBehavior = flingBehavior,
            state = state,
            overscrollEffect = overscrollEffect,
            bringIntoViewSpec = bringIntoViewSpec
        )
}
