/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalTvMaterial3Api::class)
@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class NonInteractiveSurfaceScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    private val containerModifier = Modifier.size(150.dp)

    private val surfaceModifier: @Composable BoxScope.() -> Modifier = {
        Modifier
            .size(100.dp)
            .align(Alignment.Center)
    }

    private val wrapperTestTag = "NonInteractiveSurfaceWrapper"

    @Test
    fun nonInteractiveSurface_noCustomizations() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(containerModifier.testTag(wrapperTestTag)) {
                Surface(surfaceModifier().align(Alignment.Center)) {}
            }
        }
        assertAgainstGolden("non_interactive_surface_${scheme.name}_noCustomizations")
    }

    @Test
    fun nonInteractiveSurface_nonZero_tonalElevation() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(containerModifier.testTag(wrapperTestTag)) {
                Surface(surfaceModifier(), tonalElevation = 2.dp) {}
            }
        }
        assertAgainstGolden("non_interactive_surface_${scheme.name}_nonZero_tonalElevation")
    }

    @Test
    fun nonInteractiveSurface_circleShape() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(containerModifier.testTag(wrapperTestTag)) {
                Surface(surfaceModifier(), shape = CircleShape) {}
            }
        }
        assertAgainstGolden("non_interactive_surface_${scheme.name}_circleShape")
    }

    @Test
    fun nonInteractiveSurface_containerColor() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(containerModifier.testTag(wrapperTestTag)) {
                Surface(surfaceModifier(), color = Color.Green) {}
            }
        }
        assertAgainstGolden("non_interactive_surface_${scheme.name}_containerColor")
    }

    @Test
    fun nonInteractiveSurface_contentColor() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(containerModifier.testTag(wrapperTestTag)) {
                Surface(surfaceModifier(), contentColor = Color.Red) {}
            }
        }
        assertAgainstGolden("non_interactive_surface_${scheme.name}_contentColor")
    }

    @Test
    fun nonInteractiveSurface_borderApplied() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(containerModifier.testTag(wrapperTestTag)) {
                Surface(
                    surfaceModifier(),
                    border = Border(
                        border = BorderStroke(2.dp, Color.Red),
                        inset = 4.dp,
                    ),
                ) {}
            }
        }
        assertAgainstGolden("non_interactive_surface_${scheme.name}_borderApplied")
    }

    @Test
    fun nonInteractiveSurface_glowApplied() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(containerModifier.testTag(wrapperTestTag)) {
                Surface(
                    surfaceModifier(),
                    glow = Glow(elevationColor = Color.Red, elevation = 2.dp)
                ) {}
            }
        }
        assertAgainstGolden("non_interactive_surface_${scheme.name}_glowApplied")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @OptIn(ExperimentalTvMaterial3Api::class)
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            ColorSchemeWrapper(
                "lightTheme", lightColorScheme(
                    surface = Color(0xFFFF0090)
                )
            ),
            ColorSchemeWrapper("darkTheme", darkColorScheme()),
        )
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    class ColorSchemeWrapper constructor(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}