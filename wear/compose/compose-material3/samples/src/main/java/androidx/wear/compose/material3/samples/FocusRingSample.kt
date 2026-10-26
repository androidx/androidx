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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.LocalRippleConfiguration
import androidx.wear.compose.material3.LocalRippleThemeConfiguration
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RippleConfiguration
import androidx.wear.compose.material3.RippleDefaults
import androidx.wear.compose.material3.RippleThemeConfiguration
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ripple

@Sampled
@Composable
fun EnableInsetFocusRingSample() {
    // Enable inset focus rings through the theme configuration.
    CompositionLocalProvider(
        LocalRippleThemeConfiguration provides RippleDefaults.InsetFocusRingThemeConfiguration
    ) {
        // Button is a standard Wear Material3 component that supports focus rings internally.
        Button(onClick = { /* Do something */ }, label = { Text("Button") })
    }
}

@Sampled
@Composable
fun CustomizeInsetFocusRingSample() {
    // Customize the focus ring stroke sizes through the theme configuration.
    CompositionLocalProvider(
        LocalRippleThemeConfiguration provides
            RippleThemeConfiguration(
                RippleThemeConfiguration.Focus.InsetRing(
                    outerStrokeInset = 0.dp,
                    outerStrokeWidth = 3.dp,
                    innerStrokeInset = 0.dp,
                    innerStrokeWidth = 6.dp,
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Button is a standard Wear Material3 component that supports focus rings internally.
            Button(onClick = { /* Do something */ }, label = { Text("Button") })

            // SwitchButton is also a standard component that supports focus rings internally.
            // We override its focus ring colors here.
            CompositionLocalProvider(
                LocalRippleConfiguration provides
                    RippleConfiguration(
                        focus =
                            RippleConfiguration.Focus.InsetRing(
                                outerStrokeColor = MaterialTheme.colorScheme.tertiary,
                                innerStrokeColor = MaterialTheme.colorScheme.onTertiary,
                            )
                    )
            ) {
                SwitchButton(
                    checked = true,
                    onCheckedChange = { /* Do something */ },
                    label = { Text("SwitchButton") },
                )
            }
        }
    }
}

@Sampled
@Composable
fun ClickableWithFocusRingShapeSample() {
    CompositionLocalProvider(
        LocalRippleThemeConfiguration provides RippleDefaults.InsetFocusRingThemeConfiguration
    ) {
        // Pass ripple() explicitly to use the same shape for the component and its focus ring.
        val shape = MaterialTheme.shapes.large
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(
                        interactionSource = null,
                        indication = ripple(focusRingShape = shape),
                        onClick = { /* Do something */ },
                    )
                    .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Clickable box")
        }
    }
}

@Sampled
@Composable
fun FocusableWithFocusRingShapeSample() {
    CompositionLocalProvider(
        LocalRippleThemeConfiguration provides RippleDefaults.InsetFocusRingThemeConfiguration
    ) {
        val shape = MaterialTheme.shapes.large
        val interactionSource = remember { MutableInteractionSource() }
        // indication draws but does not emit interactions; focusable (or clickable) emits them.
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .indication(interactionSource, ripple(focusRingShape = shape))
                    .focusable(interactionSource = interactionSource)
                    .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Focusable box")
        }
    }
}

@Sampled
@Composable
fun BorderedClickableWithFocusRingShapeSample() {
    CompositionLocalProvider(
        LocalRippleThemeConfiguration provides RippleDefaults.InsetFocusRingThemeConfiguration
    ) {
        val shape = MaterialTheme.shapes.large
        val hasInsetFocusRing =
            LocalRippleThemeConfiguration.current.focus is RippleThemeConfiguration.Focus.InsetRing
        val interactionSource =
            if (hasInsetFocusRing) remember { MutableInteractionSource() } else null
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    // Draw the focus ring before the border, so that it is drawn on top of it.
                    .then(
                        if (interactionSource != null) {
                            Modifier.indication(
                                interactionSource = interactionSource,
                                indication =
                                    ripple(
                                        focusRingShape = shape,
                                        enablePressIndication = false,
                                        enableHoverIndication = false,
                                        enableDragIndication = false,
                                    ),
                            )
                        } else {
                            Modifier
                        }
                    )
                    .border(2.dp, MaterialTheme.colorScheme.outline, shape)
                    .clip(shape)
                    // Pass ripple() explicitly to skip focus indication; the ring is drawn above.
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(enableFocusIndication = !hasInsetFocusRing),
                        onClick = { /* Do something */ },
                    )
                    .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Bordered clickable box")
        }
    }
}

@Sampled
@Composable
fun ClickableWithDynamicFocusRingShapeSample() {
    CompositionLocalProvider(
        LocalRippleThemeConfiguration provides RippleDefaults.InsetFocusRingThemeConfiguration
    ) {
        var isSelected by remember { mutableStateOf(false) }
        val primaryColor = MaterialTheme.colorScheme.primary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        // ColorProducer reads state during draw, avoiding recomposition when the ripple color
        // changes, and preserving the existing ripple state instead of recreating the ripple.
        val colorProducer =
            remember(primaryColor, tertiaryColor) {
                ColorProducer { if (isSelected) primaryColor else tertiaryColor }
            }
        val shape = if (isSelected) MaterialTheme.shapes.large else MaterialTheme.shapes.small
        // Delegate from a single Shape instance to the changing shape to preserve the ripple node.
        val focusRingShape = remember {
            object : Shape {
                var currentShape: Shape = shape

                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline = currentShape.createOutline(size, layoutDirection, density)
            }
        }
        focusRingShape.currentShape = shape
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(
                        interactionSource = null,
                        indication = ripple(color = colorProducer, focusRingShape = focusRingShape),
                        onClick = { isSelected = !isSelected },
                    )
                    .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isSelected) "Selected" else "Unselected")
        }
    }
}
