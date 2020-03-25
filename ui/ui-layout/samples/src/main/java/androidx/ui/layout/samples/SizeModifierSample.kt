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
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.ContentGravity
import androidx.ui.graphics.Color
import androidx.ui.layout.Stack
import androidx.ui.layout.aspectRatio
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.unit.dp

@Sampled
@Composable
fun SimpleSizeModifier() {
    Stack {
        Box(Modifier.preferredSize(100.dp, 100.dp), backgroundColor = Color.Red)
    }
}

@Sampled
@Composable
fun SimpleWidthModifier() {
    Stack {
        Box(Modifier.preferredWidth(100.dp).aspectRatio(1f), backgroundColor = Color.Magenta)
    }
}

@Sampled
@Composable
fun SimpleHeightModifier() {
    Stack {
        Box(Modifier.preferredHeight(100.dp).aspectRatio(1f), backgroundColor = Color.Blue)
    }
}

@Sampled
@Composable
fun SimpleFillWidthModifier() {
    Box(Modifier.fillMaxWidth(), backgroundColor = Color.Red, gravity = ContentGravity.Center) {
        ColoredRect(color = Color.Magenta, width = 100.dp, height = 100.dp)
    }
}

@Sampled
@Composable
fun SimpleFillHeightModifier() {
    Box(Modifier.fillMaxHeight(), backgroundColor = Color.Red, gravity = ContentGravity.Center) {
        ColoredRect(color = Color.Magenta, width = 100.dp, height = 100.dp)
    }
}

@Sampled
@Composable
fun SimpleFillModifier() {
    Box(Modifier.fillMaxSize(), backgroundColor = Color.Red, gravity = ContentGravity.Center) {
        ColoredRect(color = Color.Magenta, width = 100.dp, height = 100.dp)
    }
}
