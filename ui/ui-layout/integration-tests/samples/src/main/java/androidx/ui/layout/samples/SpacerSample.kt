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

package androidx.ui.layout.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.LayoutWidth

@Sampled
@Composable
fun SpacerExample() {
    Row {
        SizedRectangle(color = Color.Red, width = 100.dp, height = 100.dp)
        Spacer(modifier = LayoutWidth(20.dp))
        SizedRectangle(color = Color.Magenta, width = 100.dp, height = 100.dp)
        Spacer(modifier = LayoutFlexible(1f))
        SizedRectangle(color = Color.Black, width = 100.dp, height = 100.dp)
    }
}