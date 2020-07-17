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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.compose.foundation.Icon
import androidx.compose.foundation.Text
import androidx.ui.layout.Spacer
import androidx.ui.layout.size
import androidx.ui.material.Button
import androidx.ui.material.ButtonConstants
import androidx.ui.material.OutlinedButton
import androidx.ui.material.TextButton
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite

@Sampled
@Composable
fun ButtonSample() {
    Button(onClick = { /* Do something! */ }) {
        Text("Button")
    }
}

@Sampled
@Composable
fun OutlinedButtonSample() {
    OutlinedButton(onClick = { /* Do something! */ }) {
        Text("Outlined Button")
    }
}

@Sampled
@Composable
fun TextButtonSample() {
    TextButton(onClick = { /* Do something! */ }) {
        Text("Text Button")
    }
}

@Sampled
@Composable
fun ButtonWithIconSample() {
    Button(onClick = { /* Do something! */ }) {
        Icon(Icons.Filled.Favorite, Modifier.size(ButtonConstants.DefaultIconSize))
        Spacer(Modifier.size(ButtonConstants.DefaultIconSpacing))
        Text("Like")
    }
}
