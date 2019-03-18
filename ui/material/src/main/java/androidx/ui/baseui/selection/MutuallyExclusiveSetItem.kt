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

import androidx.ui.core.adapter.PressGestureDetector
import androidx.ui.core.adapter.Semantics
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * Component for representing one option out of many
 * in mutually exclusion set, e.g [androidx.ui.material.RadioGroup]
 *
 * Provides click handling as well as [Semantics] with testTag equals to [key] value
 *
 * @param key key for handling clicks as well as for testTag for Semantics
 * @param selected whether or not this item is selected in mutually exclusion set
 * @param onSelected callback to invoke when this item is selected,
 * does nothing if item is already selected
 * @param children composable content of this item
 */
@Composable
fun <K> MutuallyExclusiveSetItem(
    key: K,
    selected: Boolean,
    onSelected: (K) -> Unit,
    @Children children: () -> Unit
) {
    <PressGestureDetector onPress={ if (!selected) onSelected(key) }>
        <Semantics
            inMutuallyExclusiveGroup=true
            testTag=key.toString()
            selected=selected>
            <children />
        </Semantics>
    </PressGestureDetector>
}