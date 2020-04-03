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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.animation.animate
import androidx.ui.foundation.Icon
import androidx.ui.graphics.Color
import androidx.ui.material.IconButton
import androidx.ui.material.IconToggleButton
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite

@Sampled
@Composable
fun IconButtonSample() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Favorite)
    }
}

@Sampled
@Composable
fun IconToggleButtonSample() {
    var checked by state { false }

    IconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        val tint = animate(if (checked) Color(0xFFEC407A) else Color(0xFFB0BEC5))
        Icon(Icons.Filled.Favorite, tint = tint)
    }
}
