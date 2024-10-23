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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen
import androidx.wear.compose.material3.macrobenchmark.common.scrollDown

val ColorSchemeScreen =
    object : MacrobenchmarkScreen {
        override val content: @Composable BoxScope.() -> Unit
            get() = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ButtonWithColor(
                        "Primary",
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary
                    )
                    ButtonWithColor(
                        "Primary Dim",
                        MaterialTheme.colorScheme.primaryDim,
                        MaterialTheme.colorScheme.onPrimary
                    )
                    ButtonWithColor(
                        "Primary Container",
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    ButtonWithColor(
                        "On Primary",
                        MaterialTheme.colorScheme.onPrimary,
                        MaterialTheme.colorScheme.primary
                    )
                    ButtonWithColor(
                        "On Primary Container",
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                    ButtonWithColor(
                        "Secondary",
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.onSecondary
                    )
                    ButtonWithColor(
                        "Secondary Dim",
                        MaterialTheme.colorScheme.secondaryDim,
                        MaterialTheme.colorScheme.onSecondary
                    )
                    ButtonWithColor(
                        "Secondary Container",
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    ButtonWithColor(
                        "On Secondary",
                        MaterialTheme.colorScheme.onSecondary,
                        MaterialTheme.colorScheme.secondary
                    )
                    ButtonWithColor(
                        "On Secondary Container",
                        MaterialTheme.colorScheme.onSecondaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                    ButtonWithColor(
                        "Tertiary",
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.onTertiary
                    )
                    ButtonWithColor(
                        "Tertiary Dim",
                        MaterialTheme.colorScheme.tertiaryDim,
                        MaterialTheme.colorScheme.onTertiary
                    )
                    ButtonWithColor(
                        "Tertiary Container",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    ButtonWithColor(
                        "On Tertiary",
                        MaterialTheme.colorScheme.onTertiary,
                        MaterialTheme.colorScheme.tertiary
                    )
                    ButtonWithColor(
                        "On Tertiary Container",
                        MaterialTheme.colorScheme.onTertiaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer
                    )
                    ButtonWithColor(
                        "Surface Container",
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.onSurface
                    )
                    ButtonWithColor(
                        "Surface Container Low",
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.onSurface
                    )
                    ButtonWithColor(
                        "Surface Container High",
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.onSurface
                    )
                    ButtonWithColor(
                        "On Surface",
                        MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                    ButtonWithColor(
                        "On Surface Variant",
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                    ButtonWithColor(
                        "Background",
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.onBackground
                    )
                    ButtonWithColor(
                        "On Background",
                        MaterialTheme.colorScheme.onBackground,
                        MaterialTheme.colorScheme.background
                    )
                    ButtonWithColor(
                        "Outline",
                        MaterialTheme.colorScheme.outline,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                    ButtonWithColor(
                        "Outline Variant",
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                    ButtonWithColor(
                        "Error",
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.onError
                    )
                    ButtonWithColor(
                        "Error Container",
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer
                    )
                    ButtonWithColor(
                        "On Error",
                        MaterialTheme.colorScheme.onError,
                        MaterialTheme.colorScheme.error
                    )
                    ButtonWithColor(
                        "On Error Container",
                        MaterialTheme.colorScheme.onErrorContainer,
                        MaterialTheme.colorScheme.errorContainer
                    )
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                repeat(4) {
                    device.scrollDown()
                    device.waitForIdle()
                }
            }
    }

@Composable
private fun ButtonWithColor(text: String, containerColor: Color, contentColor: Color) {
    Button(
        onClick = {},
        label = { Text(text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        modifier = Modifier.fillMaxWidth(),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            )
    )
}
