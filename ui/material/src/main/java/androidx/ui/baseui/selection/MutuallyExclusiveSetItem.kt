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

package androidx.ui.baseui.selection

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Semantics
import androidx.ui.core.gesture.PressReleasedGestureDetector

/**
 * Component for representing one option out of many
 * in mutually exclusion set, e.g [androidx.ui.material.RadioGroup]
 *
 * Provides click handling as well as [Semantics] for accessibility
 *
 * @param selected whether or not this item is selected in mutually exclusion set
 * @param onSelected callback to invoke when this item is clicked
 */
@Composable
fun MutuallyExclusiveSetItem(
    selected: Boolean,
    onSelected: () -> Unit,
    @Children children: @Composable() () -> Unit
) {
    PressReleasedGestureDetector(
        onRelease = onSelected,
        consumeDownOnStart = false
    ) {
        Semantics(
            inMutuallyExclusiveGroup = true,
            selected = selected
        ) {
            children()
        }
    }
}