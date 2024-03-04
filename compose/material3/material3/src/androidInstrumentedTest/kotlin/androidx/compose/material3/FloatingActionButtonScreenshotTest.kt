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

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class FloatingActionButtonScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After
    fun resetTouchMode() =
        with(InstrumentationRegistry.getInstrumentation()) {
            if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
        }

    @Test
    fun icon_primary_light_color_scheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_primary_light_color_scheme")
    }

    @Test
    fun lower_elevation_icon_primary_light_color_scheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButton(
                onClick = {},
                elevation = FloatingActionButtonDefaults.loweredElevation(),
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_primary_lower_elevation_light_color_scheme")
    }

    @Test
    fun icon_primary_dark_color_scheme() {
        rule.setMaterialContent(darkColorScheme()) {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_primary_dark_color_scheme")
    }

    @Test
    fun lower_elevation_icon_primary_dark_color_scheme() {
        rule.setMaterialContent(darkColorScheme()) {
            FloatingActionButton(
                onClick = {},
                elevation = FloatingActionButtonDefaults.loweredElevation(),
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_primary_lower_elevation_dark_color_scheme")
    }

    @Test
    fun icon_secondary_light_color_scheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_secondary_light_color_scheme")
    }

    @Test
    fun icon_secondary_dark_color_scheme() {
        rule.setMaterialContent(darkColorScheme()) {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_secondary_dark_color_scheme")
    }

    @Test
    fun icon_tertiary_light_color_scheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_tertiary_light_color_scheme")
    }

    @Test
    fun icon_tertiary_dark_color_scheme() {
        rule.setMaterialContent(darkColorScheme()) {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_tertiary_dark_color_scheme")
    }

    @Test
    fun icon_surface_light_color_scheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_surface_light_color_scheme")
    }

    @Test
    fun icon_surface_dark_color_scheme() {
        rule.setMaterialContent(darkColorScheme()) {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_surface_dark_color_scheme")
    }

    @Test
    fun smallIcon() {
        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_small")
    }

    @Test
    fun largeIcon() {
        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_large")
    }

    @Test
    fun smallFab() {
        rule.setMaterialContent(lightColorScheme()) {
            SmallFloatingActionButton(onClick = {}) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }

        assertClickableAgainstGolden("fab_small_size")
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun mediumFab() {
        rule.setMaterialContent(lightColorScheme()) {
            MediumFloatingActionButton(onClick = {}) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize),
                )
            }
        }

        assertClickableAgainstGolden("fab_medium_size")
    }

    @Test
    fun largeFab() {
        rule.setMaterialContent(lightColorScheme()) {
            LargeFloatingActionButton(onClick = {}) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                )
            }
        }

        assertClickableAgainstGolden("fab_large_size")
    }

    @Test
    fun text() {
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                onClick = {},
                content = { Text("EXTENDED") },
            )
        }

        assertClickableAgainstGolden("fab_extended_text")
    }

    @Test
    fun textAndIcon() {
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                text = { Text("EXTENDED") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                onClick = {}
            )
        }

        assertClickableAgainstGolden("fab_extended_text_and_icon")
    }

    @Test
    fun ripple() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.requiredSize(100.dp, 100.dp).wrapContentSize()) {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Filled.Favorite, contentDescription = null)
                }
            }
        }

        // Start ripple
        rule.onNode(hasClickAction()).performTouchInput { down(center) }

        rule.waitForIdle()
        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't
        // properly wait for synchronization. Instead just wait until after the ripples are
        // finished animating.
        Thread.sleep(300)

        assertRootAgainstGolden("fab_pressed")
    }

    @Test
    fun hover() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.requiredSize(100.dp, 100.dp).wrapContentSize()) {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Filled.Favorite, contentDescription = null)
                }
            }
        }

        rule.onNode(hasClickAction()).performMouseInput { enter(center) }

        rule.waitForIdle()

        assertRootAgainstGolden("fab_hover")
    }

    @Test
    fun focus() {
        val focusRequester = FocusRequester()
        var localInputModeManager: InputModeManager? = null

        rule.setMaterialContent(lightColorScheme()) {
            localInputModeManager = LocalInputModeManager.current
            Box(Modifier.requiredSize(100.dp, 100.dp).wrapContentSize()) {
                FloatingActionButton(
                    onClick = {},
                    modifier = Modifier.focusRequester(focusRequester)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null)
                }
            }
        }

        rule.runOnIdle {
            localInputModeManager!!.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }

        rule.waitForIdle()

        assertRootAgainstGolden("fab_focus")
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun extended_fab_half_way_animation() {
        rule.mainClock.autoAdvance = false

        var expanded by mutableStateOf(true)
        lateinit var motionSpec: FiniteAnimationSpec<Float>
        rule.setMaterialContent(lightColorScheme()) {
            // Loads the same FiniteAnimationSpec that is used by the ExtendedFloatingActionButton
            motionSpec = MotionSchemeKeyTokens.FastSpatial.value()
            ExtendedFloatingActionButton(
                expanded = expanded,
                onClick = {},
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = "Extended FAB") },
            )
        }

        rule.runOnIdle { expanded = false }

        // Calculate the time it should take the current motion to run and advance the clock to
        // 50% of it.
        val duration =
            motionSpec
                .vectorize(Float.VectorConverter)
                .getDurationNanos(
                    initialValue = AnimationVector1D(0f),
                    targetValue = AnimationVector1D(1f),
                    initialVelocity = AnimationVector1D(0f)
                ) / 1_000_000.0

        rule.mainClock.advanceTimeBy(duration.toLong() / 2)

        assertRootAgainstGolden("fab_extended_animation")
    }

    private fun assertClickableAgainstGolden(goldenName: String) {
        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    private fun assertRootAgainstGolden(goldenName: String) {
        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
