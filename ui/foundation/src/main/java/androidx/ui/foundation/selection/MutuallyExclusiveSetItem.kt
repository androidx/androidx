/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation.selection

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.ui.core.Semantics
import androidx.ui.core.semantics.SemanticsAction
import androidx.ui.core.semantics.SemanticsActionType

/**
 * Component for representing one option out of many
 * in mutually exclusion set, e.g [androidx.ui.material.RadioGroup]
 *
 * Provides click handling as well as [Semantics] for accessibility
 *
 * @param selected whether or not this item is selected in mutually exclusion set
 * @param onClick callback to invoke when this item is clicked
 */
@Composable
fun MutuallyExclusiveSetItem(
    selected: Boolean,
    onClick: () -> Unit,
    @Children children: @Composable() () -> Unit
) {
    // TODO: when semantics can be merged, we should make this use Clickable internally rather
    // than duplicating logic
    Semantics(
        inMutuallyExclusiveGroup = true,
        selected = selected,
        actions = listOf<SemanticsAction<*>>(SemanticsAction(SemanticsActionType.Tap, onClick))) {
        PressReleasedGestureDetector(
            onRelease = onClick,
            consumeDownOnStart = false
        ) {
            children()
        }
    }
}