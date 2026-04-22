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
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.IconToggleButton
import androidx.xr.glimmer.IconToggleButtonDefaults

@Sampled
@Composable
fun IconToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    IconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        Icon(
            imageVector = if (checked) FavoriteIcon else OutlinedFavoriteIcon,
            contentDescription = "Localized description",
        )
    }
}

@Sampled
@Composable
fun IconToggleButtonWithCustomShapeAndColorSample() {
    var checked by remember { mutableStateOf(false) }
    IconToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        shape =
            IconToggleButtonDefaults.shape(
                checked = checked,
                checkedShape = RectangleShape,
                uncheckedShape = CircleShape,
            ),
        colors =
            IconToggleButtonDefaults.colors(
                backgroundColor = Color.Black,
                backgroundCheckedColor = Color.LightGray,
                contentColor = Color.White,
                contentCheckedColor = Color.DarkGray,
            ),
    ) {
        Icon(
            imageVector = if (checked) FavoriteIcon else OutlinedFavoriteIcon,
            contentDescription = "Localized description",
        )
    }
}

@Sampled
@Composable
fun IconToggleButtonWithAnimatableCustomShapeSample() {
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
                        topStartPercent = 35,
                        topEndPercent = 0,
                        bottomEndPercent = 35,
                        bottomStartPercent = 0,
                    )

                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline {
                    // Depending on the specific shapes, the approach to calculating
                    // intermediate shapes might vary significantly.
                    val currentShape =
                        uncheckedShape.lerp(checkedShape, checkedProgress.value)
                            as RoundedCornerShape
                    return currentShape.createOutline(size, layoutDirection, density)
                }
            }
        }

    IconToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        shape = animatableShape,
    ) {
        Icon(
            imageVector = if (checked) FavoriteIcon else OutlinedFavoriteIcon,
            contentDescription = "Localized description",
        )
    }
}

@Preview
@Composable
private fun IconToggleButtonPreview() {
    GlimmerTheme { IconToggleButtonSample() }
}

@Preview
@Composable
private fun IconToggleButtonWithCustomShapeAndColorPreview() {
    GlimmerTheme { IconToggleButtonWithCustomShapeAndColorSample() }
}

@Preview
@Composable
private fun IconToggleButtonWithAnimatableCustomShapePreview() {
    GlimmerTheme { IconToggleButtonWithAnimatableCustomShapeSample() }
}
