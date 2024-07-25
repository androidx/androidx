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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class WideNavigationRailScreenshotTest(private val scheme: TestWrapper) {

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun wideNavigationRail() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setMaterialContent(scheme.colorScheme) {
            scope = rememberCoroutineScope()
            DefaultWideNavigationRail(interactionSource, expanded = scheme.expanded)
        }

        assertWideNavigationRailMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "wideNavigationRail_${scheme.name}"
        )
    }

    @Test
    @Ignore("b/355413615")
    fun wideNavigationRail_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setMaterialContent(scheme.colorScheme) {
            scope = rememberCoroutineScope()
            DefaultWideNavigationRail(interactionSource, expanded = scheme.expanded)
        }

        assertWideNavigationRailMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "wideNavigationRail_${scheme.name}_pressed"
        )
    }

    @Test
    fun wideNavigationRail_disabled() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setMaterialContent(scheme.colorScheme) {
            scope = rememberCoroutineScope()
            DefaultWideNavigationRail(
                interactionSource = interactionSource,
                expanded = scheme.expanded,
                setUnselectedItemsAsDisabled = true
            )
        }

        assertWideNavigationRailMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "wideNavigationRail_${scheme.name}_disabled"
        )
    }

    @Test
    fun wideNavigationRail_withHeader() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultWideNavigationRail(
                interactionSource,
                expanded = scheme.expanded,
                withHeader = true,
            )
        }

        val expanded = if (scheme.expanded) "expanded" else "collapsed"
        assertWideNavigationRailMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "wideNavigationRail_${expanded}_lightTheme_defaultColors_withHeader"
        )
    }

    @Test
    fun wideNavigationRail_centeredArrangement() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultWideNavigationRail(
                interactionSource,
                expanded = scheme.expanded,
                arrangement = NavigationRailArrangement.Center
            )
        }

        val expanded = if (scheme.expanded) "expanded" else "collapsed"
        assertWideNavigationRailMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier =
                "wideNavigationRail_${expanded}_lightTheme_defaultColors_centeredArrangement"
        )
    }

    @Test
    fun wideNavigationRail_bottomArrangement() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            DefaultWideNavigationRail(
                interactionSource,
                expanded = scheme.expanded,
                arrangement = NavigationRailArrangement.Bottom
            )
        }

        val expanded = if (scheme.expanded) "expanded" else "collapsed"
        assertWideNavigationRailMatches(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier =
                "wideNavigationRail_${expanded}_lightTheme_defaultColors_bottomArrangement"
        )
    }

    /**
     * Asserts that the WideNavigationRail matches the screenshot with identifier
     * [goldenIdentifier].
     *
     * @param scope [CoroutineScope] used to interact with [MutableInteractionSource]
     * @param interactionSource the [MutableInteractionSource] used for the first NavigationRailItem
     * @param interaction the [Interaction] to assert for, or `null` if no [Interaction].
     * @param goldenIdentifier the identifier for the corresponding screenshot
     */
    private fun assertWideNavigationRailMatches(
        scope: CoroutineScope,
        interactionSource: MutableInteractionSource,
        interaction: Interaction? = null,
        goldenIdentifier: String
    ) {
        if (interaction != null) {
            composeTestRule.runOnIdle {
                // Start ripple.
                scope.launch { interactionSource.emit(interaction) }
            }

            composeTestRule.waitForIdle()
            // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't properly
            // wait for synchronization. Instead just advance until after the ripples are finished
            // animating.
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.mainClock.advanceTimeBy(300)
        }

        // Capture and compare screenshots.
        composeTestRule
            .onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }

    // Provide the ColorScheme, expanded value and their name parameter in a TestWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @Parameterized.Parameters(name = "expanded={0} name={1}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                TestWrapper(
                    expanded = false,
                    "collapsed_lightTheme_defaultColors",
                    lightColorScheme()
                ),
                TestWrapper(
                    expanded = false,
                    "collapsed_darkTheme_defaultColors",
                    darkColorScheme()
                ),
                TestWrapper(
                    expanded = true,
                    "expanded_lightTheme_defaultColors",
                    lightColorScheme()
                ),
                TestWrapper(expanded = true, "expanded_darkTheme_defaultColors", darkColorScheme()),
            )
    }

    class TestWrapper(val expanded: Boolean, val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}

/**
 * Default colored [WideNavigationRail] with three [WideNavigationRailItem]s. The first is selected,
 * and the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [WideNavigationRailItem],
 *   to control its visual state
 * @param expanded whether the rail is expanded
 * @param arrangement the [NavigationRailArrangement] of the rail
 * @param withHeader when true, shows a [FloatingActionButton] as the header
 * @param setUnselectedItemsAsDisabled when true, marks unselected items as disabled
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultWideNavigationRail(
    interactionSource: MutableInteractionSource,
    expanded: Boolean = false,
    arrangement: NavigationRailArrangement = NavigationRailArrangement.Top,
    withHeader: Boolean = false,
    setUnselectedItemsAsDisabled: Boolean = false,
) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        WideNavigationRail(
            expanded = expanded,
            arrangement = arrangement,
            header =
                if (withHeader) {
                    { Header() }
                } else {
                    null
                }
        ) {
            WideNavigationRailItem(
                railExpanded = expanded,
                icon = { Icon(Icons.Filled.Favorite, null) },
                label = { Text("Favorites") },
                selected = true,
                onClick = {},
                interactionSource = interactionSource
            )
            WideNavigationRailItem(
                railExpanded = expanded,
                icon = { Icon(Icons.Filled.Home, null) },
                label = { Text("Home") },
                selected = false,
                enabled = !setUnselectedItemsAsDisabled,
                onClick = {}
            )
            WideNavigationRailItem(
                railExpanded = expanded,
                icon = { Icon(Icons.Filled.Search, null) },
                label = { Text("Search") },
                selected = false,
                enabled = !setUnselectedItemsAsDisabled,
                onClick = {}
            )
        }
    }
}

/**
 * Default Menu header button to be used along with the [DefaultWideNavigationRail] when the
 * withHeader flag is true.
 */
@Composable
private fun Header() {
    Column {
        IconButton(modifier = Modifier.padding(start = 24.dp), onClick = {}) {
            Icon(Icons.Filled.Menu, "Menu")
        }
    }
}

private const val Tag = "WideNavigationRail"
