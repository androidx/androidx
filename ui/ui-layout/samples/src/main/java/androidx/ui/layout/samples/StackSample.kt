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
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.unit.dp

@Sampled
@Composable
fun SimpleStack() {
    Stack {
        Box(Modifier.fillMaxSize(), backgroundColor = Color.Cyan)
        Box(
            Modifier.matchParentSize().padding(top = 20.dp, bottom = 20.dp),
            backgroundColor = Color.Yellow
        )
        Box(Modifier.matchParentSize().padding(40.dp), backgroundColor = Color.Magenta)
        Box(
            Modifier.gravity(Alignment.Center).preferredSize(300.dp, 300.dp),
            backgroundColor = Color.Green
        )
        Box(
            Modifier.gravity(Alignment.TopStart).preferredSize(150.dp, 150.dp),
            backgroundColor = Color.Red
        )
        Box(
            Modifier.gravity(Alignment.BottomEnd).preferredSize(150.dp, 150.dp),
            backgroundColor = Color.Blue
        )
    }
}