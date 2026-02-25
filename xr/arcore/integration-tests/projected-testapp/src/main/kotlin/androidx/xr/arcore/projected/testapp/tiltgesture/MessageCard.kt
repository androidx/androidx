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

package androidx.xr.arcore.projected.testapp.tiltgesture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.xr.arcore.ExperimentalGesturesApi
import androidx.xr.arcore.Tilt
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text

@OptIn(ExperimentalGesturesApi::class)
@Composable
fun MessageCard(modifier: Modifier = Modifier, sender: String, message: String, tilt: Tilt) {
    Card(
        modifier = modifier,
        leadingIcon = { Icon(Icons.Filled.Star, "") },
        title = {
            AnimatedVisibility(visible = tilt != Tilt.DOWN) {
                Text(sender, fontWeight = FontWeight.Bold)
            }
        },
        action = {
            AnimatedVisibility(visible = tilt == Tilt.DOWN) {
                Button(onClick = {}) { Text("Reply") }
            }
        },
    ) {
        AnimatedVisibility(visible = tilt == Tilt.DOWN) { Text(message) }
    }
}
