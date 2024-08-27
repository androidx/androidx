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
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.Confirmation
import androidx.wear.compose.material3.ConfirmationDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.ConfirmationSample
import androidx.wear.compose.material3.samples.FailureConfirmationSample
import androidx.wear.compose.material3.samples.LongTextConfirmationSample
import androidx.wear.compose.material3.samples.SuccessConfirmationSample

val Comfirmations =
    listOf(
        ComposableDemo("Generic confirmation") { ConfirmationSample() },
        ComposableDemo("Long content confirmation") { LongTextConfirmationSample() },
        ComposableDemo("Success confirmation") { SuccessConfirmationSample() },
        ComposableDemo("Failure confirmation") { FailureConfirmationSample() },
        ComposableDemo("Confirmation without text") { ConfirmationWithoutText() },
        ComposableDemo("Confirmation with custom colors") { ConfirmationWithCustomColors() },
    )

@Composable
fun ConfirmationWithoutText() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") }
        )
    }

    Confirmation(
        show = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        curvedText = null
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(ConfirmationDefaults.IconSize).align(Alignment.Center),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ConfirmationWithCustomColors() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") }
        )
    }

    Confirmation(
        show = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        colors =
            ConfirmationDefaults.confirmationColors(
                iconColor = MaterialTheme.colorScheme.tertiary,
                iconContainerColor = MaterialTheme.colorScheme.onTertiary,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
        curvedText = ConfirmationDefaults.curvedText("Custom confirmation")
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(ConfirmationDefaults.IconSize).align(Alignment.Center),
        )
    }
}
