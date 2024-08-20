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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.OpenOnPhoneDialogSample

val OpenOnPhoneDialogDemos =
    listOf(
        ComposableDemo("Default OpenOnPhone Dialog") { OpenOnPhoneDialogSample() },
        ComposableDemo("With custom text") { OpenOnPhoneDialogWithCustomText() },
        ComposableDemo("With custom colors") { OpenOnPhoneDialogWithCustomColors() },
    )

@Composable
fun OpenOnPhoneDialogWithCustomText() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Open on phone") }
        )
    }

    OpenOnPhoneDialog(
        show = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        curvedText = OpenOnPhoneDialogDefaults.curvedText("Custom text")
    )
}

@Composable
fun OpenOnPhoneDialogWithCustomColors() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Open on phone") }
        )
    }

    OpenOnPhoneDialog(
        show = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        colors =
            OpenOnPhoneDialogDefaults.colors(
                iconColor = MaterialTheme.colorScheme.tertiary,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                progressIndicatorColor = MaterialTheme.colorScheme.tertiary,
                progressTrackColor = MaterialTheme.colorScheme.onTertiary,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
    )
}
