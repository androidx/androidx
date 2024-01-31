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
package androidx.compose.material3

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ColorSchemeScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun colorScheme() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ColorSchemeDemo()
            }
        }
        assertToggeableAgainstGolden("color_scheme_${scheme.name}")
    }

    private fun assertToggeableAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        private val LightCustomColorScheme = lightColorScheme(
            primary = Color(0xFF006E2C),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF43B55F),
            onPrimaryContainer = Color(0xFF004117),
            inversePrimary = Color(0xFF6DDD81),
            secondary = Color(0xFF3F6743),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFC2F0C2),
            onSecondaryContainer = Color(0xFF466F4A),
            tertiary = Color(0xFF005EB3),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF5EA1FF),
            onTertiaryContainer = Color(0xFF00376C),
            background = Color(0xFFF5FBF0),
            onBackground = Color(0xFF171D17),
            surface = Color(0xFFF5FBF0),
            onSurface = Color(0xFF171D17),
            surfaceVariant = Color(0xFFD9E6D6),
            onSurfaceVariant = Color(0xFF3E4A3E),
            inverseSurface = Color(0xFF2C322B),
            inverseOnSurface = Color(0xFFECF3E8),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = Color(0xFF6C786A),
            outlineVariant = Color(0xFFBDCABA),
            scrim = Color(0xFF000000),
            surfaceTint = Color(0xFF006E2C),
            surfaceContainerHighest = Color(0xFFDEE4DA),
            surfaceContainerHigh = Color(0xFFE4EADF),
            surfaceContainer = Color(0xFFE9F0E5),
            surfaceContainerLow = Color(0xFFEFF6EB),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceBright = Color(0xFFF5FBF0),
            surfaceDim = Color(0xFFD5DCD1)
        )

        private val DarkCustomColorScheme = darkColorScheme(
            primary = Color(0xFF6DDD81),
            onPrimary = Color(0xFF003914),
            primaryContainer = Color(0xFF008738),
            onPrimaryContainer = Color(0xFFF7FFF2),
            inversePrimary = Color(0xFF006E2C),
            secondary = Color(0xFFA5D2A6),
            onSecondary = Color(0xFF0F3819),
            secondaryContainer = Color(0xFF1D4524),
            onSecondaryContainer = Color(0xFF87B389),
            tertiary = Color(0xFFA7C8FF),
            onTertiary = Color(0xFF003061),
            tertiaryContainer = Color(0xFF0774D9),
            onTertiaryContainer = Color(0xFFFDFCFF),
            background = Color(0xFF0F150F),
            onBackground = Color(0xFFDEE4DA),
            surface = Color(0xFF0F150F),
            onSurface = Color(0xFFDEE4DA),
            surfaceVariant = Color(0xFF3E4A3E),
            onSurfaceVariant = Color(0xFFBDCABA),
            inverseSurface = Color(0xFFDEE4DA),
            inverseOnSurface = Color(0xFF2C322B),
            error = Color(0xFFFFB4A9),
            onError = Color(0xFF680003),
            errorContainer = Color(0xFF930006),
            onErrorContainer = Color(0xFFFFDAD4),
            outline = Color(0xFF6C786A),
            outlineVariant = Color(0xFF3E4A3E),
            scrim = Color(0xFF000000),
            surfaceTint = Color(0xFF6DDD81),
            surfaceContainerHighest = Color(0xFF30362F),
            surfaceContainerHigh = Color(0xFF252C25),
            surfaceContainer = Color(0xFF1B211B),
            surfaceContainerLow = Color(0xFF171D17),
            surfaceContainerLowest = Color(0xFF0A100A),
            surfaceBright = Color(0xFF343B34),
            surfaceDim = Color(0xFF0F150F)
        )

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            ColorSchemeWrapper("light", lightColorScheme()),
            ColorSchemeWrapper("light_dynamic", LightCustomColorScheme),
            ColorSchemeWrapper("dark", darkColorScheme()),
            ColorSchemeWrapper("dark_dynamic", DarkCustomColorScheme),
        )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }

    private val Tag = "ColorScheme"
}

@Composable
private fun ColorSchemeDemo() {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(8.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())) {
            Text("Surfaces", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            SurfaceColorSwatch(
                surface = colorScheme.surface,
                surfaceText = "Surface",
                onSurface = colorScheme.onSurface,
                onSurfaceText = "On Surface"
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Surface Bright",
                        color = colorScheme.surfaceBright,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Surface Dim",
                        color = colorScheme.surfaceDim,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Surface Container",
                        color = colorScheme.surfaceContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Surface",
                        color = colorScheme.surface,
                    )
                },
            )
            Text("Surface Container Variants", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "High Emphasis",
                        color = colorScheme.surfaceContainerHigh,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Highest Emphasis",
                        color = colorScheme.surfaceContainerHighest,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Low Emphasis",
                        color = colorScheme.surfaceContainerLow,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Lowest Emphasis",
                        color = colorScheme.surfaceContainerLowest,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            SurfaceColorSwatch(
                surface = colorScheme.surfaceVariant,
                surfaceText = "Surface Variant",
                onSurface = colorScheme.onSurfaceVariant,
                onSurfaceText = "On Surface Variant"
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Inverse Surface",
                        color = colorScheme.inverseSurface,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Inverse On Surface",
                        color = colorScheme.inverseOnSurface,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Inverse Primary",
                        color = colorScheme.inversePrimary,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Surface Tint",
                        color = colorScheme.surfaceTint,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())) {
            Text("Content", style = MaterialTheme.typography.bodyLarge)
            ContentColorSwatch(
                color = colorScheme.primary,
                colorText = "Primary",
                onColor = colorScheme.onPrimary,
                onColorText = "On Primary",
                colorContainer = colorScheme.primaryContainer,
                colorContainerText = "Primary Container",
                onColorContainer = colorScheme.onPrimaryContainer,
                onColorContainerText = "On Primary Container")
            Spacer(modifier = Modifier.height(16.dp))
            ContentColorSwatch(
                color = colorScheme.secondary,
                colorText = "Secondary",
                onColor = colorScheme.onSecondary,
                onColorText = "On Secondary",
                colorContainer = colorScheme.secondaryContainer,
                colorContainerText = "Secondary Container",
                onColorContainer = colorScheme.onSecondaryContainer,
                onColorContainerText = "On Secondary Container")
            Spacer(modifier = Modifier.height(16.dp))
            ContentColorSwatch(
                color = colorScheme.tertiary,
                colorText = "Tertiary",
                onColor = colorScheme.onTertiary,
                onColorText = "On Tertiary",
                colorContainer = colorScheme.tertiaryContainer,
                colorContainerText = "Tertiary Container",
                onColorContainer = colorScheme.onTertiaryContainer,
                onColorContainerText = "On Tertiary Container")
            Spacer(modifier = Modifier.height(16.dp))
            ContentColorSwatch(
                color = colorScheme.error,
                colorText = "Error",
                onColor = colorScheme.onError,
                onColorText = "On Error",
                colorContainer = colorScheme.errorContainer,
                colorContainerText = "Error Container",
                onColorContainer = colorScheme.onErrorContainer,
                onColorContainerText = "On Error Container")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Utility", style = MaterialTheme.typography.bodyLarge)
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Outline",
                        color = colorScheme.outline,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Outline Variant",
                        color = colorScheme.outlineVariant,
                    )
                }
            )
        }
    }
}

@Composable
private fun SurfaceColorSwatch(
    surface: Color,
    surfaceText: String,
    onSurface: Color,
    onSurfaceText: String
) {
    ColorTile(
        text = surfaceText,
        color = surface,
    )
    ColorTile(
        text = onSurfaceText,
        color = onSurface,
    )
}

@Composable
private fun ContentColorSwatch(
    color: Color,
    colorText: String,
    onColor: Color,
    onColorText: String,
    colorContainer: Color,
    colorContainerText: String,
    onColorContainer: Color,
    onColorContainerText: String,
) {
    DoubleTile(
        leftTile = {
            ColorTile(
                text = colorText,
                color = color
            )
        },
        rightTile = {
            ColorTile(
                text = onColorText,
                color = onColor,
            )
        },
    )
    DoubleTile(
        leftTile = {
            ColorTile(
                text = colorContainerText,
                color = colorContainer,
            )
        },
        rightTile = {
            ColorTile(
                text = onColorContainerText,
                color = onColorContainer,
            )
        },
    )
}

@Composable
private fun DoubleTile(leftTile: @Composable () -> Unit, rightTile: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) { leftTile() }
        Box(modifier = Modifier.weight(1f)) { rightTile() }
    }
}

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalTextApi::class)
@Composable
private fun ColorTile(text: String, color: Color) {
    var borderColor = Color.Transparent
    if (color == Color.Black) {
        borderColor = Color.White
    } else if (color == Color.White) borderColor = Color.Black

    Surface(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth(),
        color = color,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text,
            Modifier.padding(4.dp),
            style =
            MaterialTheme.typography.bodyMedium.copy(
                if (color.luminance() < .25) Color.White else Color.Black
            )
        )
    }
}
