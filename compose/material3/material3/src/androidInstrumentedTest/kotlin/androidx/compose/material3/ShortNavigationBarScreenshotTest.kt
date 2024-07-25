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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ShortNavigationBarScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun equalWeightArrangement_lightTheme() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(interactionSource)
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "shortNavigationBar_equalWeightArrangement_lightTheme"
        )
    }

    @Test
    @Ignore("b/355413615")
    fun equalWeightArrangement_lightTheme_pressed() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(interactionSource)
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "shortNavigationBar_equalWeightArrangement_lightTheme_pressed"
        )
    }

    @Test
    fun equalWeightArrangement_lightTheme_disabled() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(interactionSource, setUnselectedItemsAsDisabled = true)
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "shortNavigationBar_equalWeightArrangement_lightTheme_disabled"
        )
    }

    @Test
    fun equalWeightArrangement_lightTheme_twoLinesLabel() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ShortNavigationBar {
                    ShortNavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text("Label\nLabel") },
                    )
                    ShortNavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text("Label") },
                    )
                    ShortNavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text("Label") },
                    )
                }
            }
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "shortNavigationBar_equalWeightArrangement_lightTheme_twoLinesLabel"
        )
    }

    @Test
    fun equalWeightArrangement_darkTheme() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setMaterialContent(darkColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(interactionSource)
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "shortNavigationBar_equalWeightArrangement_darkTheme"
        )
    }

    @Test
    @Ignore("b/355413615")
    fun equalWeightArrangement_darkTheme_pressed() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setMaterialContent(darkColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(interactionSource)
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "shortNavigationBar_equalWeightArrangement_darkTheme_pressed"
        )
    }

    @Test
    fun equalWeightArrangement_darkTheme_disabled() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setMaterialContent(darkColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(interactionSource, setUnselectedItemsAsDisabled = true)
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "shortNavigationBar_equalWeightArrangement_darkTheme_disabled"
        )
    }

    @Test
    fun centeredArrangement_lightTheme() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setContentWithSimulatedSize(600.dp, 100.dp, lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(
                interactionSource = interactionSource,
                arrangement = NavigationBarArrangement.Centered,
                iconPosition = NavigationItemIconPosition.Start
            )
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "shortNavigationBar_centeredArrangement_lightTheme"
        )
    }

    @Test
    @Ignore("b/355413615")
    fun centeredArrangement_lightTheme_pressed() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setContentWithSimulatedSize(600.dp, 100.dp, lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(
                interactionSource = interactionSource,
                arrangement = NavigationBarArrangement.Centered,
                iconPosition = NavigationItemIconPosition.Start
            )
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(140f, 10f)),
            goldenIdentifier = "shortNavigationBar_centeredArrangement_lightTheme_pressed"
        )
    }

    @Test
    fun centeredArrangement_lightTheme_disabled() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setContentWithSimulatedSize(600.dp, 100.dp, lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(
                interactionSource = interactionSource,
                setUnselectedItemsAsDisabled = true,
                arrangement = NavigationBarArrangement.Centered,
                iconPosition = NavigationItemIconPosition.Start
            )
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "shortNavigationBar_centeredArrangement_lightTheme_disabled"
        )
    }

    @Test
    fun centeredArrangement_lightTheme_oneWiderItem() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setContentWithSimulatedSize(600.dp, 100.dp, lightColorScheme()) {
            scope = rememberCoroutineScope()
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ShortNavigationBar(arrangement = NavigationBarArrangement.Centered) {
                    ShortNavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        iconPosition = NavigationItemIconPosition.Start,
                        label = { Text("Really looooong label") },
                    )
                    ShortNavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        iconPosition = NavigationItemIconPosition.Start,
                        label = { Text("Label") },
                    )
                    ShortNavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        iconPosition = NavigationItemIconPosition.Start,
                        label = { Text("Label") },
                    )
                }
            }
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "shortNavigationBar_centeredArrangement_lightTheme_oneWiderItem"
        )
    }

    @Test
    fun centeredArrangement_darkTheme() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setContentWithSimulatedSize(600.dp, 100.dp, darkColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(
                interactionSource = interactionSource,
                arrangement = NavigationBarArrangement.Centered,
                iconPosition = NavigationItemIconPosition.Start
            )
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "shortNavigationBar_centeredArrangement_darkTheme"
        )
    }

    @Test
    @Ignore("b/355413615")
    fun centeredArrangement_darkTheme_pressed() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setContentWithSimulatedSize(600.dp, 100.dp, darkColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(
                interactionSource = interactionSource,
                arrangement = NavigationBarArrangement.Centered,
                iconPosition = NavigationItemIconPosition.Start
            )
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(140f, 10f)),
            goldenIdentifier = "shortNavigationBar_centeredArrangement_darkTheme_pressed"
        )
    }

    @Test
    fun centeredArrangement_darkTheme_disabled() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope

        composeTestRule.setContentWithSimulatedSize(600.dp, 100.dp, darkColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultShortNavigationBar(
                interactionSource = interactionSource,
                setUnselectedItemsAsDisabled = true,
                arrangement = NavigationBarArrangement.Centered,
                iconPosition = NavigationItemIconPosition.Start
            )
        }

        assertShortNavigationBarMatches(
            scope = scope,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "shortNavigationBar_centeredArrangement_darkTheme_disabled"
        )
    }

    /**
     * Asserts that the [ShortNavigationBar] matches the screenshot with identifier
     * [goldenIdentifier].
     *
     * @param scope [CoroutineScope] used to interact with [MutableInteractionSource]
     * @param interactionSource the [MutableInteractionSource] used for the first
     *   [ShortNavigationBarItem]
     * @param interaction the [Interaction] to assert for, or `null` if no [Interaction].
     * @param goldenIdentifier the identifier for the corresponding screenshot
     */
    private fun assertShortNavigationBarMatches(
        scope: CoroutineScope,
        interactionSource: MutableInteractionSource,
        interaction: Interaction? = null,
        goldenIdentifier: String
    ) {
        if (interaction != null) {
            composeTestRule.runOnIdle {
                // Start ripple
                scope.launch { interactionSource.emit(interaction) }
            }

            composeTestRule.waitForIdle()
            // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't properly
            // wait for synchronization. Instead just advance until after the ripples are finished
            // animating.
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.mainClock.advanceTimeBy(300)
        }

        // Capture and compare screenshots
        composeTestRule
            .onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}

/**
 * Default colored [ShortNavigationBar] with three [ShortNavigationBarItem]s. The first item is
 * selected, and the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [ShortNavigationBarItem],
 *   to control its visual state.
 * @param modifier the [Modifier] applied to the navigation bar
 * @param setUnselectedItemsAsDisabled when true, marks unselected items as disabled
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultShortNavigationBar(
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    setUnselectedItemsAsDisabled: Boolean = false,
    arrangement: NavigationBarArrangement = NavigationBarArrangement.EqualWeight,
    iconPosition: NavigationItemIconPosition = NavigationItemIconPosition.Top
) {
    Box(modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        ShortNavigationBar(arrangement = arrangement) {
            ShortNavigationBarItem(
                icon = { Icon(Icons.Filled.Favorite, null) },
                iconPosition = iconPosition,
                selected = true,
                label = { Text("Label") },
                onClick = {},
                interactionSource = interactionSource
            )
            ShortNavigationBarItem(
                icon = { Icon(Icons.Filled.Favorite, null) },
                iconPosition = iconPosition,
                selected = false,
                label = { Text("Label") },
                enabled = !setUnselectedItemsAsDisabled,
                onClick = {}
            )
            ShortNavigationBarItem(
                icon = { Icon(Icons.Filled.Favorite, null) },
                iconPosition = iconPosition,
                selected = false,
                label = { Text("Label") },
                enabled = !setUnselectedItemsAsDisabled,
                onClick = {}
            )
        }
    }
}

private fun ComposeContentTestRule.setContentWithSimulatedSize(
    simulatedWidth: Dp,
    simulatedHeight: Dp,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier,
    composable: @Composable () -> Unit
) {
    setContent {
        val currentDensity = LocalDensity.current
        val currentConfiguration = LocalConfiguration.current
        val simulatedDensity =
            Density(
                currentDensity.density * (currentConfiguration.screenWidthDp.dp / simulatedWidth)
            )
        MaterialTheme(colorScheme = colorScheme) {
            Surface(modifier = modifier) {
                CompositionLocalProvider(LocalDensity provides simulatedDensity) {
                    Box(
                        Modifier.fillMaxWidth().height(simulatedHeight),
                    ) {
                        composable()
                    }
                }
            }
        }
    }
}

private const val Tag = "ShortNavigationBar"
