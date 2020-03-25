/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.core.globalPosition
import androidx.ui.core.onChildPositioned
import androidx.ui.core.onPositioned
import androidx.ui.core.positionInRoot
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.preferredSize
import androidx.ui.unit.dp

@Sampled
@Composable
fun OnPositionedSample() {
    Column(Modifier.onPositioned { coordinates ->
        // This will be the size of the Column.
        coordinates.size
        // The position of the Column relative to the application window.
        coordinates.globalPosition
        // The position of the Column relative to the Compose root.
        coordinates.positionInRoot
        // These will be the alignment lines provided to the layout (empty here for Column).
        coordinates.providedAlignmentLines
        // This will a LayoutCoordinates instance corresponding to the parent of Column.
        coordinates.parentCoordinates
    }) {
        Box(Modifier.preferredSize(20.dp), backgroundColor = Color.Green)
        Box(Modifier.preferredSize(20.dp), backgroundColor = Color.Blue)
    }
}

@Sampled
@Composable
fun OnChildPositionedSample() {
    Column(Modifier.onChildPositioned { coordinates ->
        // This will be the size of the child SizedRectangle.
        coordinates.size
        // The position of the SizedRectangle relative to the application window.
        coordinates.globalPosition
        // The position of the SizedRectangle relative to the Compose root.
        coordinates.positionInRoot
        // These will be the alignment lines provided to the layout (empty for SizedRectangle)
        coordinates.providedAlignmentLines
        // This will a LayoutCoordinates instance corresponding to the Column.
        coordinates.parentCoordinates
    }) {
        Box(Modifier.preferredSize(20.dp), backgroundColor = Color.Green)
    }
}
