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

package androidx.ui.layout.demos

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.ltr
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.layout.rtl
import androidx.ui.unit.dp

@Composable
fun ComplexLayoutDemo() {
    Stack(Modifier.rtl
        .drawBackground(Color.Magenta)
        .padding(start = 10.dp)
        .ltr
        .padding(start = 10.dp)
        .preferredSize(150.dp)
        .rtl
        .drawBackground(Color.Gray)
        .padding(start = 10.dp)
        .ltr
        .padding(start = 10.dp)
        .drawBackground(Color.Blue)
        .rtl
    ) {
        Stack(Modifier
            .padding(start = 10.dp)
            .ltr
            .padding(start = 10.dp)
            .fillMaxSize().drawBackground(Color.Green)) {}
    }
}
