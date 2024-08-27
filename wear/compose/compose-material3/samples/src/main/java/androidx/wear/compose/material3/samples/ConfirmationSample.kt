/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Confirmation
import androidx.wear.compose.material3.ConfirmationDefaults
import androidx.wear.compose.material3.FailureConfirmation
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SuccessConfirmation
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun ConfirmationSample() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") }
        )
    }

    // Has an icon and a short curved text content, which will be displayed along the bottom edge of
    // the screen.
    Confirmation(
        show = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        curvedText = ConfirmationDefaults.curvedText("Confirmed")
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(ConfirmationDefaults.IconSize),
        )
    }
}

@Sampled
@Composable
fun LongTextConfirmationSample() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") }
        )
    }

    // Has an icon and a text content. Text will be displayed in the center of the screen below the
    // icon.
    Confirmation(
        show = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        text = { Text(text = "Your message has been sent") },
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(ConfirmationDefaults.SmallIconSize),
        )
    }
}

@Sampled
@Composable
fun FailureConfirmationSample() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") }
        )
    }

    FailureConfirmation(show = showConfirmation, onDismissRequest = { showConfirmation = false })
}

@Sampled
@Composable
fun SuccessConfirmationSample() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") }
        )
    }

    SuccessConfirmation(show = showConfirmation, onDismissRequest = { showConfirmation = false })
}
