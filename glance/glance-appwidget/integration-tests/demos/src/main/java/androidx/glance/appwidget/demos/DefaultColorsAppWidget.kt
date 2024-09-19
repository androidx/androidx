/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget.demos

import android.content.Context
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.RadioButton
import androidx.glance.appwidget.Switch
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.material.ColorProviders
import androidx.glance.material3.ColorProviders
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * A demo showing how to construct a widget with [GlanceTheme]. It will use Material 3 colors and
 * when supported, it will use the dynamic color theme.
 */
class DefaultColorsAppWidget : GlanceAppWidget() {
    enum class Scheme {
        SystemM3,
        CustomM3,
        CustomM2
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        var currentScheme by remember { mutableStateOf(Scheme.SystemM3) }
        val colors =
            when (currentScheme) {
                Scheme.SystemM3 -> GlanceTheme.colors
                Scheme.CustomM3 ->
                    ColorProviders(
                        light = DemoColorScheme.LightColors,
                        dark = DemoColorScheme.DarkColors
                    )
                Scheme.CustomM2 ->
                    ColorProviders(
                        light = DemoColorScheme.SampleM2ColorsLight,
                        dark = DemoColorScheme.SampleM2ColorsDark
                    )
            }

        Content(colors, currentScheme)
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content(GlanceTheme.colors, Scheme.SystemM3)
    }

    @Composable
    private fun Content(colors: ColorProviders, currentScheme: Scheme) {
        var currentScheme1 = currentScheme
        GlanceTheme(colors) {
            Column(
                GlanceModifier.fillMaxSize()
                    .padding(16.dp)
                    .background(GlanceTheme.colors.widgetBackground)
            ) {
                Button(
                    text = "Theme: $currentScheme1",
                    onClick = {
                        currentScheme1 =
                            when (currentScheme1) {
                                Scheme.SystemM3 -> Scheme.CustomM3
                                Scheme.CustomM3 -> Scheme.CustomM2
                                Scheme.CustomM2 -> Scheme.SystemM3
                            }
                    },
                    modifier = GlanceModifier.padding(8.dp)
                )
                Row(GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                    CheckBox(
                        modifier = GlanceModifier.defaultWeight(),
                        checked = false,
                        onCheckedChange = doNothingAction,
                        text = "Unchecked"
                    )
                    CheckBox(
                        modifier = GlanceModifier.defaultWeight(),
                        checked = true,
                        onCheckedChange = doNothingAction,
                        text = "Checked"
                    )
                }

                Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    RadioButton(
                        modifier = GlanceModifier.defaultWeight(),
                        checked = false,
                        onClick = doNothingAction,
                        text = "Unchecked"
                    )
                    RadioButton(
                        modifier = GlanceModifier.defaultWeight(),
                        checked = true,
                        onClick = doNothingAction,
                        text = "Checked"
                    )
                }

                Row(modifier = GlanceModifier.padding(bottom = 8.dp)) {
                    Switch(checked = false, onCheckedChange = doNothingAction, text = "Off")
                    Switch(checked = true, onCheckedChange = doNothingAction, text = "On")
                }
                ColorDebug()
            }
        }
    }
}

@Composable
private fun ColorDebug() {
    @Composable
    fun Text(text: String, fg: ColorProvider, bg: ColorProvider) =
        Text(
            text = text,
            style = TextStyle(color = fg),
            modifier = GlanceModifier.fillMaxWidth().background(bg).padding(6.dp)
        )
    Column(modifier = GlanceModifier.cornerRadius(8.dp)) {
        with(GlanceTheme.colors) {
            // Using  nested column because Glance uses statically generated layouts. Our
            // Rows/Columns can only support a fixed number of children, so nesting is a workaround.
            // The usual perf caveats for nested views still apply.
            Column(modifier = GlanceModifier.background(Color.Transparent)) {
                Text(text = "Primary / OnPrimary", fg = onPrimary, bg = primary)
                Text(
                    text = "PrimaryContainer / OnPrimaryContainer",
                    fg = onPrimaryContainer,
                    bg = primaryContainer
                )
                Text(text = "Secondary / OnSecondary", fg = onSecondary, bg = secondary)
                Text(
                    text = "SecondaryContainer / OnSecondaryContainer",
                    fg = onSecondaryContainer,
                    bg = secondaryContainer
                )
                Text(text = "Tertiary / OnTertiary", fg = onTertiary, bg = tertiary)
            }
            Column {
                Text(
                    text = "TertiaryContainer / OnTertiaryContainer",
                    fg = onTertiaryContainer,
                    bg = tertiaryContainer
                )
                Text(text = "Surface / OnSurface", fg = onSurface, bg = surface)
                Text(
                    text = "SurfaceVariant / OnSurfaceVariant",
                    fg = onSurfaceVariant,
                    bg = surfaceVariant
                )
                Text(
                    text = "InverseOnSurface / InverseSurface",
                    fg = inverseOnSurface,
                    bg = inverseSurface
                )
                Text(text = "Background / OnBackground", fg = onBackground, bg = background)
                Text(text = "Error / OnError", fg = onError, bg = error)
            }
        }
    }
}

private val doNothingAction = null

class DefaultColorsAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DefaultColorsAppWidget()
}

/** Color scheme generated using https://m3.material.io/theme-builder#/custom */
object DemoColorScheme {

    val md_theme_light_primary = Color(0xFF026E00)
    val md_theme_light_onPrimary = Color(0xFFFFFFFF)
    val md_theme_light_primaryContainer = Color(0xFF77FF61)
    val md_theme_light_onPrimaryContainer = Color(0xFF002200)
    val md_theme_light_secondary = Color(0xFFA900A9)
    val md_theme_light_onSecondary = Color(0xFFFFFFFF)
    val md_theme_light_secondaryContainer = Color(0xFFFFD7F5)
    val md_theme_light_onSecondaryContainer = Color(0xFF380038)
    val md_theme_light_tertiary = Color(0xFF006A6A)
    val md_theme_light_onTertiary = Color(0xFFFFFFFF)
    val md_theme_light_tertiaryContainer = Color(0xFF00FBFB)
    val md_theme_light_onTertiaryContainer = Color(0xFF002020)
    val md_theme_light_error = Color(0xFFBA1A1A)
    val md_theme_light_errorContainer = Color(0xFFFFDAD6)
    val md_theme_light_onError = Color(0xFFFFFFFF)
    val md_theme_light_onErrorContainer = Color(0xFF410002)
    val md_theme_light_background = Color(0xFFFFFBFF)
    val md_theme_light_onBackground = Color(0xFF1E1C00)
    val md_theme_light_surface = Color(0xFFFFFBFF)
    val md_theme_light_onSurface = Color(0xFF1E1C00)
    val md_theme_light_surfaceVariant = Color(0xFFDFE4D7)
    val md_theme_light_onSurfaceVariant = Color(0xFF43483F)
    val md_theme_light_outline = Color(0xFF73796E)
    val md_theme_light_inverseOnSurface = Color(0xFFFFF565)
    val md_theme_light_inverseSurface = Color(0xFF353200)
    val md_theme_light_inversePrimary = Color(0xFF02E600)
    val md_theme_light_shadow = Color(0xFF000000)
    val md_theme_light_surfaceTint = Color(0xFF026E00)

    val md_theme_dark_primary = Color(0xFF02E600)
    val md_theme_dark_onPrimary = Color(0xFF013A00)
    val md_theme_dark_primaryContainer = Color(0xFF015300)
    val md_theme_dark_onPrimaryContainer = Color(0xFF77FF61)
    val md_theme_dark_secondary = Color(0xFFFFABF3)
    val md_theme_dark_onSecondary = Color(0xFF5B005B)
    val md_theme_dark_secondaryContainer = Color(0xFF810081)
    val md_theme_dark_onSecondaryContainer = Color(0xFFFFD7F5)
    val md_theme_dark_tertiary = Color(0xFF00DDDD)
    val md_theme_dark_onTertiary = Color(0xFF003737)
    val md_theme_dark_tertiaryContainer = Color(0xFF004F4F)
    val md_theme_dark_onTertiaryContainer = Color(0xFF00FBFB)
    val md_theme_dark_error = Color(0xFFFFB4AB)
    val md_theme_dark_errorContainer = Color(0xFF93000A)
    val md_theme_dark_onError = Color(0xFF690005)
    val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
    val md_theme_dark_background = Color(0xFF1E1C00)
    val md_theme_dark_onBackground = Color(0xFFF2E720)
    val md_theme_dark_surface = Color(0xFF1E1C00)
    val md_theme_dark_onSurface = Color(0xFFF2E720)
    val md_theme_dark_surfaceVariant = Color(0xFF43483F)
    val md_theme_dark_onSurfaceVariant = Color(0xFFC3C8BC)
    val md_theme_dark_outline = Color(0xFF8D9387)
    val md_theme_dark_inverseOnSurface = Color(0xFF1E1C00)
    val md_theme_dark_inverseSurface = Color(0xFFF2E720)
    val md_theme_dark_inversePrimary = Color(0xFF026E00)
    val md_theme_dark_shadow = Color(0xFF000000)
    val md_theme_dark_surfaceTint = Color(0xFF02E600)

    val seed = Color(0xFF00FF00)

    val LightColors =
        lightColorScheme(
            primary = md_theme_light_primary,
            onPrimary = md_theme_light_onPrimary,
            primaryContainer = md_theme_light_primaryContainer,
            onPrimaryContainer = md_theme_light_onPrimaryContainer,
            secondary = md_theme_light_secondary,
            onSecondary = md_theme_light_onSecondary,
            secondaryContainer = md_theme_light_secondaryContainer,
            onSecondaryContainer = md_theme_light_onSecondaryContainer,
            tertiary = md_theme_light_tertiary,
            onTertiary = md_theme_light_onTertiary,
            tertiaryContainer = md_theme_light_tertiaryContainer,
            onTertiaryContainer = md_theme_light_onTertiaryContainer,
            error = md_theme_light_error,
            onError = md_theme_light_onError,
            errorContainer = md_theme_light_errorContainer,
            onErrorContainer = md_theme_light_onErrorContainer,
            background = md_theme_light_background,
            onBackground = md_theme_light_onBackground,
            surface = md_theme_light_surface,
            onSurface = md_theme_light_onSurface,
            surfaceVariant = md_theme_light_surfaceVariant,
            onSurfaceVariant = md_theme_light_onSurfaceVariant,
            outline = md_theme_light_outline,
            inverseSurface = md_theme_light_inverseSurface,
            inverseOnSurface = md_theme_light_inverseOnSurface,
            inversePrimary = md_theme_light_inversePrimary,
            surfaceTint = md_theme_light_surfaceTint,
        )

    val DarkColors =
        darkColorScheme(
            primary = md_theme_dark_primary,
            onPrimary = md_theme_dark_onPrimary,
            primaryContainer = md_theme_dark_primaryContainer,
            onPrimaryContainer = md_theme_dark_onPrimaryContainer,
            secondary = md_theme_dark_secondary,
            onSecondary = md_theme_dark_onSecondary,
            secondaryContainer = md_theme_dark_secondaryContainer,
            onSecondaryContainer = md_theme_dark_onSecondaryContainer,
            tertiary = md_theme_dark_tertiary,
            onTertiary = md_theme_dark_onTertiary,
            tertiaryContainer = md_theme_dark_tertiaryContainer,
            onTertiaryContainer = md_theme_dark_onTertiaryContainer,
            error = md_theme_dark_error,
            onError = md_theme_dark_onError,
            errorContainer = md_theme_dark_errorContainer,
            onErrorContainer = md_theme_dark_onErrorContainer,
            background = md_theme_dark_background,
            onBackground = md_theme_dark_onBackground,
            surface = md_theme_dark_surface,
            onSurface = md_theme_dark_onSurface,
            surfaceVariant = md_theme_dark_surfaceVariant,
            onSurfaceVariant = md_theme_dark_onSurfaceVariant,
            outline = md_theme_dark_outline,
            inverseSurface = md_theme_dark_inverseSurface,
            inverseOnSurface = md_theme_dark_inverseOnSurface,
            inversePrimary = md_theme_dark_inversePrimary,
            surfaceTint = md_theme_dark_surfaceTint,
        )

    // Palette based on Jetchat
    private val Yellow400 = Color(0xFFF6E547)
    private val Yellow700 = Color(0xFFF3B711)
    private val Yellow800 = Color(0xFFF29F05)
    private val Blue200 = Color(0xFF9DA3FA)
    private val Blue400 = Color(0xFF4860F7)
    private val Blue500 = Color(0xFF0540F2)
    private val Blue800 = Color(0xFF001CCF)
    private val Red300 = Color(0xFFEA6D7E)
    private val Red800 = Color(0xFFD00036)

    val SampleM2ColorsDark =
        darkColors(
            primary = Blue200,
            primaryVariant = Blue400,
            onPrimary = Color.Black,
            secondary = Yellow400,
            onSecondary = Color.Black,
            onSurface = Color.White,
            onBackground = Color.White,
            error = Red300,
            onError = Color.Black
        )
    val SampleM2ColorsLight =
        lightColors(
            primary = Blue500,
            primaryVariant = Blue800,
            onPrimary = Color.White,
            secondary = Yellow700,
            secondaryVariant = Yellow800,
            onSecondary = Color.Black,
            onSurface = Color.Black,
            onBackground = Color.Black,
            error = Red800,
            onError = Color.White
        )
}
