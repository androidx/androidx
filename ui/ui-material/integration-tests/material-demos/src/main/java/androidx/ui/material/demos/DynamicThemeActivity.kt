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

package androidx.ui.material.demos

import android.app.Activity
import android.os.Bundle
import androidx.animation.FastOutSlowInEasing
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.remember
import androidx.ui.core.Text
import androidx.ui.core.setContent
import androidx.ui.foundation.ScrollerPosition
import androidx.ui.foundation.VerticalScroller
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.graphics.toArgb
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutWidth
import androidx.ui.material.BottomAppBar
import androidx.ui.material.ColorPalette
import androidx.ui.material.FloatingActionButton
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar
import androidx.ui.material.lightColorPalette
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import kotlin.math.round

/**
 * Demo activity that animates the primary, secondary, and background colours in the [MaterialTheme]
 * as the user scrolls. This has the effect of going from a 'light' theme to a 'dark' theme.
 */
class DynamicThemeActivity : Activity() {
    private val scrollFraction = ScrollFraction()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val palette = interpolateTheme(scrollFraction.fraction)
            val darkenedPrimary = palette.darkenedPrimary
            window.statusBarColor = darkenedPrimary
            window.navigationBarColor = darkenedPrimary

            DynamicThemeApp(scrollFraction, palette)
        }
    }

    private val ColorPalette.darkenedPrimary: Int
        get() {
            return with(primary) {
                copy(
                    red = red * 0.75f,
                    green = green * 0.75f,
                    blue = blue * 0.75f
                )
            }.toArgb()
        }
}

@Model
private class ScrollFraction(var fraction: Float = 0f)

@Composable
private fun DynamicThemeApp(scrollFraction: ScrollFraction, palette: ColorPalette) {
    MaterialTheme(palette) {
        val scrollerPosition = ScrollerPosition()
        val fraction =
            round((scrollerPosition.value / scrollerPosition.maxPosition) * 100) / 100
        remember(fraction) { scrollFraction.fraction = fraction }
        Scaffold(
            topAppBar = { TopAppBar({ Text("Scroll down!") }) },
            bottomAppBar = { BottomAppBar(fabConfiguration = it, cutoutShape = CircleShape) {} },
            floatingActionButton = { Fab(scrollFraction) },
            floatingActionButtonPosition = Scaffold.FabPosition.CenterDocked,
            bodyContent = { modifier ->
                VerticalScroller(scrollerPosition) {
                    Column(modifier) {
                        repeat(20) { index ->
                            Card(index)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun Fab(scrollFraction: ScrollFraction) {
    val secondary = MaterialTheme.colors().secondary
    val fabText = emojiForScrollFraction(scrollFraction.fraction)
    FloatingActionButton(
        text = fabText,
        textStyle = MaterialTheme.typography().h5,
        color = secondary,
        onClick = {}
    )
}

@Composable
private fun Card(index: Int) {
    val shapeColor = lerp(Color(0xFF303030), Color.White, index / 19f)
    val textColor = lerp(Color.White, Color(0xFF303030), index / 19f)
    Container(LayoutPadding(25.dp) + LayoutWidth.Fill + LayoutHeight(150.dp)) {
        // TODO: ideally this would be a Card but currently Surface consumes every
        // colour from the Material theme to work out text colour, so we end up doing a
        // large amount of work here when the top level theme changes
        DrawShape(RoundedCornerShape(10.dp), shapeColor)
        Text("Card ${index + 1}", style = TextStyle(color = textColor))
    }
}

private fun interpolateTheme(fraction: Float): ColorPalette {
    val interpolatedFraction = FastOutSlowInEasing(fraction)

    val primary = lerp(Color(0xFF6200EE), Color(0xFF303030), interpolatedFraction)
    val secondary = lerp(Color(0xFF03DAC6), Color(0xFFBB86FC), interpolatedFraction)
    val background = lerp(Color.White, Color(0xFF121212), interpolatedFraction)

    return lightColorPalette(
        primary = primary,
        secondary = secondary,
        background = background
    )
}

/**
 * 'Animate' the emoji in the FAB from 'sun' to 'moon' as we darken the theme
 */
private fun emojiForScrollFraction(fraction: Float): String {
    return when {
        // Sun
        fraction < 1 / 7f -> "\u2600"
        // Sun behind small cloud
        fraction < 2 / 7f -> "\uD83C\uDF24"
        // Sun behind cloud
        fraction < 3 / 7f -> "\uD83C\uDF25"
        // Cloud
        fraction < 4 / 7f -> "\u2601"
        // Cloud with rain
        fraction < 5 / 7f -> "\uD83C\uDF27"
        // Cloud with lightning
        fraction < 6 / 7f -> "\uD83C\uDF29"
        // Moon
        else -> "\uD83C\uDF15"
    }
}
