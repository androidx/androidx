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

package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy

// TODO use ReusableContentHost directly after we fix b/309821523
@Composable
fun SubcompositionReusableContentHost(active: Boolean, content: @Composable () -> Unit) {
    SubcomposeLayout(remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) }) { constraints
        ->
        val placeable =
            if (active) {
                subcompose(null, content).map { it.measure(constraints) }
            } else {
                emptyList()
            }
        layout(
            placeable.maxOfOrNull { it.width } ?: 0,
            placeable.maxOfOrNull { it.height } ?: 0,
        ) {
            placeable.forEach { it.place(0, 0) }
        }
    }
}
