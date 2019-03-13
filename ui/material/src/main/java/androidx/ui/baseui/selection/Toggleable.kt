/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.ui.core.PxPosition
import androidx.ui.core.adapter.PressGestureDetector
import androidx.ui.core.adapter.Semantics
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

@Composable
fun Toggleable(
    value: ToggleableState = ToggleableState.Checked,
    onToggle: (() -> Unit)? = null,
    testTag: String? = null,
    @Children children: () -> Unit
) {
    val onPress: (PxPosition) -> Unit = {
        onToggle?.invoke()
    }

    <PressGestureDetector onPress>
        // TODO(pavlis): Semantics currently doesn't support 3 states (only checked / unchecked).
        <Semantics checked=(value == ToggleableState.Checked) testTag>
            <children />
        </Semantics>
    </PressGestureDetector>
}