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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.animation.FastOutSlowInEasing
import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.ScrollerPosition
import androidx.ui.foundation.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.graphics.toArgb
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.material.BottomAppBar
import androidx.ui.material.ColorPalette
import androidx.ui.material.ExtendedFloatingActionButton
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar
import androidx.ui.material.lightColorPalette
import androidx.ui.unit.dp
import kotlin.math.round

/**
 * Demo activity that animates the primary, secondary, and background colours in the [MaterialTheme]
 * as the user scrolls. This has the effect of going from a 'light' theme to a 'dark' theme.
 */
class DynamicThemeActivity : ComponentActivity() {
    private val scrollFraction = mutableStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val palette = interpolateTheme(scrollFraction.value)
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

private typealias ScrollFraction = MutableState<Float>

@Composable
private fun DynamicThemeApp(scrollFraction: ScrollFraction, palette: ColorPalette) {
    MaterialTheme(palette) {
        val scrollerPosition = ScrollerPosition()
        val fraction =
            round((scrollerPosition.value / scrollerPosition.maxPosition) * 100) / 100
        remember(fraction) { scrollFraction.value = fraction }
        Scaffold(
            topBar = { TopAppBar({ Text("Scroll down!") }) },
            bottomBar = { BottomAppBar(cutoutShape = CircleShape) {} },
            floatingActionButton = { Fab(scrollFraction) },
            floatingActionButtonPosition = Scaffold.FabPosition.Center,
            isFloatingActionButtonDocked = true,
            bodyContent = { innerPadding ->
                VerticalScroller(scrollerPosition) {
                    Column(Modifier.padding(innerPadding)) {
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
    val fabText = emojiForScrollFraction(scrollFraction.value)
    ExtendedFloatingActionButton(
        text = { Text(fabText, style = MaterialTheme.typography.h5) },
        onClick = {}
    )
}

@Composable
private fun Card(index: Int) {
    val shapeColor = lerp(Color(0xFF303030), Color.White, index / 19f)
    val textColor = lerp(Color.White, Color(0xFF303030), index / 19f)
    // TODO: ideally this would be a Card but currently Surface consumes every
    // colour from the Material theme to work out text colour, so we end up doing a
    // large amount of work here when the top level theme changes
    Box(
        Modifier.padding(25.dp).fillMaxWidth().preferredHeight(150.dp),
        shape = RoundedCornerShape(10.dp),
        backgroundColor = shapeColor,
        gravity = ContentGravity.Center
    ) {
        Text("Card ${index + 1}", color = textColor)
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
