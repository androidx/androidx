/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalRippleThemeConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Sampled
@Composable
fun RippleSample() {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier.size(100.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary),
                    onClick = {},
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text("Custom Ripple")
    }
}

@Preview
@Sampled
@Composable
fun DynamicColorRippleSample() {
    var isSelected by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    // ColorProducer reads state during draw, avoiding recomposition when the ripple color changes
    val colorProducer =
        remember(primaryColor, secondaryColor) {
            ColorProducer { if (isSelected) primaryColor else secondaryColor }
        }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            Modifier.size(100.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = colorProducer),
                    onClick = { isSelected = !isSelected },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (isSelected) "Selected" else "Unselected")
    }
}

@Preview
@Sampled
@Composable
fun RippleConfigurationOpacitySample() {
    Column {
        // Custom ripple color provided to a subtree via LocalRippleConfiguration
        val customRippleConfiguration = RippleConfiguration(color = Color.Magenta)
        CompositionLocalProvider(LocalRippleConfiguration provides customRippleConfiguration) {
            Button(onClick = {}) { Text("Magenta Ripple Button") }
        }

        Spacer(Modifier.height(16.dp))

        // Disabling ripples entirely for a subtree by providing null
        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            Button(onClick = {}) { Text("No Ripple Button") }
        }
    }
}

@Preview
@Sampled
@Composable
fun InsetFocusRingRippleSample() {
    // Enable inset focus rings at theme level
    CompositionLocalProvider(
        LocalRippleThemeConfiguration provides RippleDefaults.InsetFocusRingThemeConfiguration
    ) {
        // Components in this tree will render inset focus rings using theme colors when focused
        Button(onClick = {}) { Text("Button with Inset Focus Ring") }
    }
}

@Preview
@Sampled
@Composable
fun RippleConfigurationInsetFocusRingSample() {
    // Enable inset focus rings at theme level
    CompositionLocalProvider(
        LocalRippleThemeConfiguration provides RippleDefaults.InsetFocusRingThemeConfiguration
    ) {
        // Custom focus ring colors provided to a subtree via LocalRippleConfiguration
        val customFocusRingConfiguration =
            RippleConfiguration(
                focus =
                    RippleConfiguration.Focus.InsetRing(
                        outerStrokeColor = MaterialTheme.colorScheme.primary,
                        innerStrokeColor = MaterialTheme.colorScheme.onPrimary,
                    )
            )
        CompositionLocalProvider(LocalRippleConfiguration provides customFocusRingConfiguration) {
            val interactionSource = remember { MutableInteractionSource() }
            // Custom clickable element using inset focus ring with custom focusRingShape
            Box(
                modifier =
                    Modifier.size(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(focusRingShape = RoundedCornerShape(12.dp)),
                            onClick = {},
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text("Custom Focus Ring")
            }
        }
    }
}
