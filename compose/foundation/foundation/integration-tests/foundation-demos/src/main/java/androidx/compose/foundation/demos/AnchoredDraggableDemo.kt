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

package androidx.compose.foundation.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.samples.AnchoredDraggableAnchorsFromCompositionSample
import androidx.compose.foundation.samples.AnchoredDraggableCustomAnchoredSample
import androidx.compose.foundation.samples.AnchoredDraggableLayoutDependentAnchorsSample
import androidx.compose.foundation.samples.AnchoredDraggableProgressSample
import androidx.compose.foundation.samples.AnchoredDraggableWithOverscrollSample
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun AnchoredDraggableDemo() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        AnchoredDraggableAnchorsFromCompositionSample()
        Spacer(Modifier.height(50.dp))
        AnchoredDraggableLayoutDependentAnchorsSample()
        Spacer(Modifier.height(50.dp))
        Spacer(Modifier.height(50.dp))
        AnchoredDraggableCustomAnchoredSample()
        Spacer(Modifier.height(50.dp))
        AnchoredDraggableWithOverscrollSample()
        Spacer(Modifier.height(50.dp))
        AnchoredDraggableProgressSample()
    }
}
