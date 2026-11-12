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

package androidx.compose.remote.integration.view.demos.examples

import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

/**
 * Does a colour-group themed colour resolve on a **cold start**, in each system theme?
 *
 * Three columns, three rows:
 * ```
 *   Light               dark                 theme
 *   system_accent2_50   system_accent2_800   (blank -- it is whichever of the two applies)
 *   [swatch]            [swatch]             [swatch]
 * ```
 *
 * All three swatches are `ColorTheme` colours, which is the only form that emits a `ColorTheme`
 * operation. The outer two pin one resource by passing the same index for light and dark, so they
 * always show that resource whatever the theme; the third is the adaptive one. The expected reading
 * is simply:
 *
 * in light mode the third swatch matches the first in dark mode the third swatch matches the second
 *
 * **The fallbacks are the measuring instrument.** Each is a colour no system palette would produce
 * -- magenta, green, orange, azure -- so "the fallback is showing" cannot be mistaken for "the
 * system colour resolved and happens to look like that". An earlier version of this test used white
 * as the light fallback and was useless, because `system_accent2_50` is itself near-white.
 *
 * A document-authored light/dark pair will *not* exercise this path: those compile to `Theme`
 * sections wrapping two `ColorExpression`s and never create a `ColorTheme`. Only
 * `addThemedColor(group, ...)` does, and JSON cannot reach it -- hence a DSL document.
 *
 * `ColorTheme` is op 196 and is registered only in the AndroidX and Widgets read maps, so the
 * profile below is required; without it the document fails to load outright.
 */
@Suppress("RestrictedApiAndroidX")
fun colorThemeProof(): RemoteComposeWriter {
    // Two system colours far apart in brightness, so light and dark are obvious at a glance.
    val lightIdx = Rc.AndroidColors.SYSTEM_ACCENT2_50
    val darkIdx = Rc.AndroidColors.SYSTEM_ACCENT2_800

    val fbLightCol = 0xFFFF00FF.toInt() // magenta  -- column 1 unresolved
    val fbDarkCol = 0xFF00FF00.toInt() //  green    -- column 2 unresolved
    val fbThemeL = 0xFFFF8000.toInt() //   orange   -- column 3 unresolved, light side
    val fbThemeD = 0xFF0080FF.toInt() //   azure    -- column 3 unresolved, dark side

    val cellW = 200f
    val swatchH = 150f
    val label = 0xFF000000.toInt()

    val rc =
        RemoteComposeContextAndroid(
            width = 620,
            height = 300,
            contentDescription = "colour-group themed colour: light, dark, adaptive",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            // Same index for light and dark: pins the column to one resource regardless of theme.
            val lightOnly =
                writer.addThemedColor(
                    Rc.AndroidColors.GROUP,
                    lightIdx,
                    lightIdx,
                    fbLightCol,
                    fbLightCol,
                )
            val darkOnly =
                writer.addThemedColor(
                    Rc.AndroidColors.GROUP,
                    darkIdx,
                    darkIdx,
                    fbDarkCol,
                    fbDarkCol,
                )
            // The adaptive one under test.
            val adaptive =
                writer.addThemedColor(Rc.AndroidColors.GROUP, lightIdx, darkIdx, fbThemeL, fbThemeD)

            root {
                column(RecordingModifier().background(0xFFB0B0B0.toInt()).fillMaxSize()) {
                    row(RecordingModifier().fillMaxWidth()) {
                        text(
                            "Light",
                            RecordingModifier().width(cellW),
                            color = label,
                            fontSize = 26f,
                        )
                        text(
                            "dark",
                            RecordingModifier().width(cellW),
                            color = label,
                            fontSize = 26f,
                        )
                        text(
                            "theme",
                            RecordingModifier().width(cellW),
                            color = label,
                            fontSize = 26f,
                        )
                    }
                    row(RecordingModifier().fillMaxWidth()) {
                        text(
                            "system_accent2_50",
                            RecordingModifier().width(cellW),
                            color = label,
                            fontSize = 16f,
                        )
                        text(
                            "system_accent2_800",
                            RecordingModifier().width(cellW),
                            color = label,
                            fontSize = 16f,
                        )
                        // deliberately blank: the adaptive colour has no single resource name
                        text("", RecordingModifier().width(cellW), color = label, fontSize = 16f)
                    }
                    row(RecordingModifier().fillMaxWidth()) {
                        box(
                            RecordingModifier()
                                .padding(8, 8, 8, 8)
                                .backgroundId(lightOnly)
                                .width(cellW - 16)
                                .height(swatchH)
                        ) {}
                        box(
                            RecordingModifier()
                                .padding(8, 8, 8, 8)
                                .backgroundId(darkOnly)
                                .width(cellW - 16)
                                .height(swatchH)
                        ) {}
                        box(
                            RecordingModifier()
                                .padding(8, 8, 8, 8)
                                .backgroundId(adaptive)
                                .width(cellW - 16)
                                .height(swatchH)
                        ) {}
                    }
                }
            }
        }
    return rc.writer
}
