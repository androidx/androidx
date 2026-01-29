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

package androidx.compose.foundation.layout.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
@Composable
@OptIn(ExperimentalFlexBoxApi::class)
fun SimpleFlexBox() {
    // FlexBox defaults to a Row-like layout (FlexDirection.Row).
    // The children will be laid out horizontally.
    FlexBox(
        modifier = Modifier.fillMaxWidth(),
        config = {
            direction =
                if (constraints.maxWidth < 400.dp.roundToPx()) FlexDirection.Column
                else FlexDirection.Row
        },
    ) {
        // This child has a fixed size and will not flex.
        Box(
            modifier = Modifier.size(80.dp).background(Color.Magenta),
            contentAlignment = Alignment.Center,
        ) {
            Text("Fixed")
        }
        // This child has a grow factor of 1. It will take up 1/3 of the remaining space.
        Box(
            modifier = Modifier.height(80.dp).flex { grow = 1f }.background(Color.Yellow),
            contentAlignment = Alignment.Center,
        ) {
            Text("Grow = 1")
        }
        // This child has a grow factor of 2. It will take up 2/3 of the remaining space.
        Box(
            modifier = Modifier.height(80.dp).flex { grow = 2f }.background(Color.Green),
            contentAlignment = Alignment.Center,
        ) {
            Text("Grow = 2")
        }
    }
}
