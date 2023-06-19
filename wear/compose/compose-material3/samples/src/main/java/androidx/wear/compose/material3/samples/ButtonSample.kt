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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun SimpleButtonSample() {
    Button(
        onClick = { /* Do something */ },
        label = { Text("Button") }
    )
}

@Sampled
@Composable
fun ButtonSample() {
    Button(
        onClick = { /* Do something */ },
        label = { Text("Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        }
    )
}

@Sampled
@Composable
fun SimpleFilledTonalButtonSample() {
    FilledTonalButton(
        onClick = { /* Do something */ },
        label = { Text("Filled Tonal Button") }
    )
}

@Sampled
@Composable
fun FilledTonalButtonSample() {
    FilledTonalButton(
        onClick = { /* Do something */ },
        label = { Text("Filled Tonal Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        }
    )
}

@Sampled
@Composable
fun SimpleOutlinedButtonSample() {
    OutlinedButton(
        onClick = { /* Do something */ },
        label = { Text("Outlined Button") }
    )
}

@Sampled
@Composable
fun OutlinedButtonSample() {
    OutlinedButton(
        onClick = { /* Do something */ },
        label = { Text("Outlined Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        }
    )
}

@Sampled
@Composable
fun SimpleChildButtonSample() {
    ChildButton(
        onClick = { /* Do something */ },
        label = { Text("Child Button") }
    )
}

@Sampled
@Composable
fun ChildButtonSample() {
    ChildButton(
        onClick = { /* Do something */ },
        label = { Text("Child Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        }
    )
}
