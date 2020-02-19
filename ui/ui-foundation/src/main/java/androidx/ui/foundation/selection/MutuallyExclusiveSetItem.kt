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

import androidx.compose.Composable
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.ui.foundation.Strings
import androidx.ui.semantics.Semantics
import androidx.ui.foundation.semantics.inMutuallyExclusiveGroup
import androidx.ui.foundation.semantics.selected
import androidx.ui.semantics.onClick
import androidx.ui.semantics.accessibilityValue

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
    children: @Composable() () -> Unit
) {
    // TODO: when semantics can be merged, we should make this use Clickable internally rather
    //  than duplicating logic
    Semantics(
        container = true,
        properties = {
            inMutuallyExclusiveGroup = true
            this.selected = selected
            this.accessibilityValue = if (selected) Strings.Selected else Strings.NotSelected
            onClick(action = onClick)
        }) {
        PressReleasedGestureDetector(
            onRelease = onClick,
            consumeDownOnStart = false,
            children = children
        )
    }
}