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

package androidx.wear.compose.integration.demos.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/*
 * Component that show its content as it is on a watch and wraps it in a SizedContainer on
 * bigger screens
 */
@Composable
fun AdaptiveScreen(
    screenSizeDp: Int = 220,
    zoomLevel: Float = 1.5f,
    content: @Composable BoxScope.() -> Unit
) {
    if (LocalConfiguration.current.screenWidthDp < 300) {
        Box(Modifier.fillMaxSize(), content = content)
    } else {
        SizedContainer(
            screenSize = screenSizeDp,
            zoomLevel = zoomLevel,
            roundScreen = true,
            modifier = Modifier.fillMaxSize().padding(10.dp).wrapContentSize(Alignment.TopCenter),
            content = content
        )
    }
}

/*
 * Component to show a watch UI on bigger screens (Phone/tablet).
 *
 * Composes the `content` inside a circle/square of the given size, also changing the configuration
 * in `LocalConfiguration.current` to make the inner composable believe the screen is that size &
 * shape.
 * Works with almost all Composables, the only know exception is Dialog, that is designed to be
 * full-screen.
 * Also applies a zoom effect.
 */
@Composable
fun SizedContainer(
    screenSize: Int,
    roundScreen: Boolean,
    modifier: Modifier = Modifier,
    zoomLevel: Float = 1.5f,
    content: @Composable BoxScope.() -> Unit,
) {
    val currentConfig = LocalConfiguration.current
    val config by
        remember(screenSize, roundScreen) {
            derivedStateOf {
                Configuration().apply {
                    setTo(currentConfig)
                    screenWidthDp = screenSize
                    screenHeightDp = screenSize
                    densityDpi = (320 * zoomLevel).roundToInt()
                    // Set the screen to round.
                    screenLayout =
                        (screenLayout and Configuration.SCREENLAYOUT_ROUND_MASK.inv()) or
                            if (roundScreen) {
                                Configuration.SCREENLAYOUT_ROUND_YES
                            } else {
                                Configuration.SCREENLAYOUT_ROUND_NO
                            }
                }
            }
        }
    val currentDensity = LocalDensity.current
    val density =
        object : Density {
            override val density: Float
                get() = currentDensity.density * zoomLevel

            override val fontScale: Float
                get() = currentDensity.fontScale
        }

    CompositionLocalProvider(LocalConfiguration provides config, LocalDensity provides density) {
        val shape = if (roundScreen) CircleShape else RectangleShape

        Box(
            modifier =
                modifier
                    .border(2.dp, Color.DarkGray, shape)
                    .padding(2.dp)
                    .clip(shape)
                    .size(screenSize.dp)
                    .background(Color.Black),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
