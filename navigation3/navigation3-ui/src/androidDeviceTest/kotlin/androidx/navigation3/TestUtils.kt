/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BlueBox(text: String) {
    Box(
        Modifier.fillMaxSize().background(Color(0.2f, 0.2f, 1.0f, 1.0f)).border(10.dp, Color.Blue),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, Modifier.size(50.dp))
    }
}

@Composable
fun RedBox(text: String) {
    Box(
        Modifier.fillMaxSize().background(Color(1.0f, 0.3f, 0.3f, 1.0f)).border(10.dp, Color.Red),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, Modifier.size(50.dp))
    }
}

@Composable
fun GreenBox(text: String) {
    Box(
        Modifier.fillMaxSize().background(Color(0.2f, 0.9f, 0.7f, 1.0f)).border(10.dp, Color.Green),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, Modifier.size(50.dp))
    }
}

const val first = "first"
const val second = "second"
const val third = "third"
const val fourth = "fourth"
