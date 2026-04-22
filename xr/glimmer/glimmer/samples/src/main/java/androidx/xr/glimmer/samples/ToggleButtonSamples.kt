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

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.ToggleButton
import androidx.xr.glimmer.ToggleButtonDefaults

@Sampled
@Composable
fun ToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val text = if (checked) "Toggle on" else "Toggle off"
    ToggleButton(checked = checked, onCheckedChange = { checked = it }) { Text(text) }
}

@Sampled
@Composable
fun LargeToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val text = if (checked) "Toggle on" else "Toggle off"
    ToggleButton(
        checked = checked,
        buttonSize = ButtonSize.Large,
        onCheckedChange = { checked = it },
    ) {
        Text(text)
    }
}

@Sampled
@Composable
fun ToggleButtonWithLeadingIconSample() {
    var checked by remember { mutableStateOf(false) }
    val text = if (checked) "Toggle on" else "Toggle off"
    ToggleButton(
        checked = checked,
        leadingIcon = {
            Icon(if (checked) FavoriteIcon else OutlinedFavoriteIcon, "Localized description")
        },
        onCheckedChange = { checked = it },
    ) {
        Text(text)
    }
}

@Sampled
@Composable
fun ToggleButtonWithTrailingIconSample() {
    var checked by remember { mutableStateOf(false) }
    val text = if (checked) "Toggle on" else "Toggle off"
    ToggleButton(
        checked = checked,
        trailingIcon = {
            Icon(if (checked) FavoriteIcon else OutlinedFavoriteIcon, "Localized description")
        },
        onCheckedChange = { checked = it },
    ) {
        Text(text)
    }
}

@Sampled
@Composable
fun ToggleButtonWithCustomShapeAndColorSample() {
    var checked by remember { mutableStateOf(false) }
    ToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        shape =
            ToggleButtonDefaults.shape(
                checked = checked,
                uncheckedShape = CircleShape,
                checkedShape = RectangleShape,
            ),
        colors =
            ToggleButtonDefaults.colors(
                backgroundColor = Color.Black,
                backgroundCheckedColor = Color.LightGray,
                contentColor = Color.White,
                contentCheckedColor = Color.DarkGray,
            ),
    ) {
        Text("Custom shapes and colors")
    }
}

@Sampled
@Composable
fun ToggleButtonWithAnimatableCustomShapeSample() {
    var checked by remember { mutableStateOf(false) }

    // Defer reading the animation progress state as far as possible to avoid recomposition.
    // In this case, the state is read inside createOutline, which is called during the draw phase.
    val checkedProgress = animateFloatAsState(if (checked) 1f else 0f)
    val animatableShape =
        remember(checkedProgress) {
            object : Shape {
                private val checkedShape =
                    RoundedCornerShape(
                        topStartPercent = 0,
                        topEndPercent = 50,
                        bottomEndPercent = 0,
                        bottomStartPercent = 50,
                    )
                private val uncheckedShape =
                    RoundedCornerShape(
                        topStartPercent = 75,
                        topEndPercent = 0,
                        bottomEndPercent = 75,
                        bottomStartPercent = 0,
                    )

                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline {
                    // Depending on the specifics of your shapes, you might want to choose
                    // a different way of calculating the intermediate shape during the transition.
                    val currentShape =
                        uncheckedShape.lerp(checkedShape, checkedProgress.value)
                            as RoundedCornerShape
                    return currentShape.createOutline(size, layoutDirection, density)
                }
            }
        }

    ToggleButton(checked = checked, onCheckedChange = { checked = it }, shape = animatableShape) {
        Text("Custom animatable shape")
    }
}

@Preview
@Composable
private fun ToggleButtonPreview() {
    GlimmerTheme { ToggleButtonSample() }
}

@Preview
@Composable
private fun LargeToggleButtonPreview() {
    GlimmerTheme { LargeToggleButtonSample() }
}

@Preview
@Composable
private fun ToggleButtonWithLeadingIconPreview() {
    GlimmerTheme { ToggleButtonWithLeadingIconSample() }
}

@Preview
@Composable
private fun ToggleButtonWithTrailingIconPreview() {
    GlimmerTheme { ToggleButtonWithTrailingIconSample() }
}

@Preview
@Composable
private fun ToggleButtonWithCustomShapeAndColorPreview() {
    GlimmerTheme { ToggleButtonWithCustomShapeAndColorSample() }
}

@Preview
@Composable
private fun ToggleButtonWithAnimatableCustomShapePreview() {
    GlimmerTheme { ToggleButtonWithAnimatableCustomShapeSample() }
}
