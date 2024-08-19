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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun ColorSchemeDemos() {
    ScalingLazyDemo {
        item { ListHeader { Text("Primary Colors") } }
        item {
            ButtonWithColor(
                "Primary",
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary
            )
        }
        item {
            ButtonWithColor(
                "Primary Dim",
                MaterialTheme.colorScheme.primaryDim,
                MaterialTheme.colorScheme.onPrimary
            )
        }
        item {
            ButtonWithColor(
                "Primary Container",
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        item {
            ButtonWithColor(
                "On Primary",
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.primary
            )
        }
        item {
            ButtonWithColor(
                "On Primary Container",
                MaterialTheme.colorScheme.onPrimaryContainer,
                MaterialTheme.colorScheme.primaryContainer
            )
        }

        item { ListHeader { Text("Secondary Colors") } }
        item {
            ButtonWithColor(
                "Secondary",
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.onSecondary
            )
        }
        item {
            ButtonWithColor(
                "Secondary Dim",
                MaterialTheme.colorScheme.secondaryDim,
                MaterialTheme.colorScheme.onSecondary
            )
        }
        item {
            ButtonWithColor(
                "Secondary Container",
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        item {
            ButtonWithColor(
                "On Secondary",
                MaterialTheme.colorScheme.onSecondary,
                MaterialTheme.colorScheme.secondary
            )
        }
        item {
            ButtonWithColor(
                "On Secondary Container",
                MaterialTheme.colorScheme.onSecondaryContainer,
                MaterialTheme.colorScheme.secondaryContainer
            )
        }

        item { ListHeader { Text("Tertiary Colors") } }
        item {
            ButtonWithColor(
                "Tertiary",
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onTertiary
            )
        }
        item {
            ButtonWithColor(
                "Tertiary Dim",
                MaterialTheme.colorScheme.tertiaryDim,
                MaterialTheme.colorScheme.onTertiary
            )
        }
        item {
            ButtonWithColor(
                "Tertiary Container",
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        item {
            ButtonWithColor(
                "On Tertiary",
                MaterialTheme.colorScheme.onTertiary,
                MaterialTheme.colorScheme.tertiary
            )
        }
        item {
            ButtonWithColor(
                "On Tertiary Container",
                MaterialTheme.colorScheme.onTertiaryContainer,
                MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        item { ListHeader { Text("Surface Colors") } }
        item {
            ButtonWithColor(
                "Surface Container",
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.onSurface
            )
        }
        item {
            ButtonWithColor(
                "Surface Container Low",
                MaterialTheme.colorScheme.surfaceContainerLow,
                MaterialTheme.colorScheme.onSurface
            )
        }
        item {
            ButtonWithColor(
                "Surface Container High",
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.onSurface
            )
        }
        item {
            ButtonWithColor(
                "On Surface",
                MaterialTheme.colorScheme.onSurface,
                MaterialTheme.colorScheme.surfaceContainer
            )
        }
        item {
            ButtonWithColor(
                "On Surface Variant",
                MaterialTheme.colorScheme.onSurfaceVariant,
                MaterialTheme.colorScheme.surfaceContainer
            )
        }

        item { ListHeader { Text("Background Colors") } }
        item {
            ButtonWithColor(
                "Background",
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.onBackground
            )
        }
        item {
            ButtonWithColor(
                "On Background",
                MaterialTheme.colorScheme.onBackground,
                MaterialTheme.colorScheme.background
            )
        }

        item { ListHeader { Text("Outline Colors") } }
        item {
            ButtonWithColor(
                "Outline",
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.surfaceContainer
            )
        }
        item {
            ButtonWithColor(
                "Outline Variant",
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.colorScheme.surfaceContainer
            )
        }

        item { ListHeader { Text("Error Colors") } }
        item {
            ButtonWithColor(
                "Error",
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.onError
            )
        }
        item {
            ButtonWithColor(
                "Error Container",
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
        }
        item {
            ButtonWithColor(
                "On Error",
                MaterialTheme.colorScheme.onError,
                MaterialTheme.colorScheme.error
            )
        }
        item {
            ButtonWithColor(
                "On Error Container",
                MaterialTheme.colorScheme.onErrorContainer,
                MaterialTheme.colorScheme.errorContainer
            )
        }
    }
}

@Composable
private fun ButtonWithColor(text: String, containerColor: Color, contentColor: Color) {
    Button(
        onClick = {},
        label = { Text(text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        modifier = Modifier.width(150.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            )
    )
}
