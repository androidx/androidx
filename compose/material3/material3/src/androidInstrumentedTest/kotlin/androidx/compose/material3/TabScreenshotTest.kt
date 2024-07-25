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
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TabScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun lightTheme_primary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) { DefaultPrimaryTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_lightTheme_primary"
        )
    }

    @Test
    fun lightTheme_secondary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) { DefaultSecondaryTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_lightTheme_secondary"
        )
    }

    @Test
    @Ignore("b/355413615")
    fun lightTheme_primary_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) { DefaultPrimaryTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_lightTheme_primary_pressed"
        )
    }

    @Test
    @Ignore("b/355413615")
    fun lightTheme_secondary_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) { DefaultSecondaryTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_lightTheme_secondary_pressed"
        )
    }

    @Test
    fun darkTheme_primary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) { DefaultPrimaryTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_darkTheme_primary"
        )
    }

    @Test
    fun darkTheme_secondary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) { DefaultSecondaryTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_darkTheme_secondary"
        )
    }

    @Test
    @Ignore("b/355413615")
    fun darkTheme_primary_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) { DefaultPrimaryTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_darkTheme_primary_pressed"
        )
    }

    @Test
    @Ignore("b/355413615")
    fun darkTheme_secondary_pressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) { DefaultSecondaryTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = PressInteraction.Press(Offset(10f, 10f)),
            goldenIdentifier = "tabs_darkTheme_secondary_pressed"
        )
    }

    @Test
    fun customTabs_lightTheme_primary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) {
                CustomPrimaryTabs(
                    interactionSource,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedContentColor = MaterialTheme.colorScheme.onTertiary,
                    unselectedContentColor = Color.Black
                )
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "customTabs_lightTheme_primary"
        )
    }

    @Test
    fun customTabs_lightTheme_secondary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) {
                CustomSecondaryTabs(
                    interactionSource,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedContentColor = MaterialTheme.colorScheme.onTertiary,
                    unselectedContentColor = Color.Black
                )
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "customTabs_lightTheme_secondary"
        )
    }

    @Test
    fun customTabs_darkTheme_primary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) {
                CustomPrimaryTabs(
                    interactionSource,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedContentColor = MaterialTheme.colorScheme.onTertiary,
                    unselectedContentColor = Color.Black
                )
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "customTabs_darkTheme_primary"
        )
    }

    @Test
    fun customTabs_darkTheme_secondary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) {
                CustomSecondaryTabs(
                    interactionSource,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedContentColor = MaterialTheme.colorScheme.onTertiary,
                    unselectedContentColor = Color.Black
                )
            }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "customTabs_darkTheme_secondary"
        )
    }

    @Test
    fun leadingIconTabs_lightTheme_primary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) { DefaultPrimaryLeadingIconTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "leadingIconTabs_lightTheme_primary"
        )
    }

    @Test
    fun leadingIconTabs_lightTheme_secondary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) { DefaultSecondaryLeadingIconTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "leadingIconTabs_lightTheme_secondary"
        )
    }

    @Test
    fun leadingIconTabs_darkTheme_primary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) { DefaultPrimaryLeadingIconTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "leadingIconTabs_darkTheme_primary"
        )
    }

    @Test
    fun leadingIconTabs_darkTheme_secondary() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) { DefaultSecondaryLeadingIconTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "leadingIconTabs_darkTheme_secondary"
        )
    }

    @Test
    fun lightTheme_primary_scrollable() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) { DefaultPrimaryScrollableTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_lightTheme_primary_scrollable"
        )
    }

    @Test
    fun lightTheme_secondary_scrollable() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(lightColorScheme()) { DefaultSecondaryScrollableTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_lightTheme_secondary_scrollable"
        )
    }

    @Test
    fun darkTheme_primary_scrollable() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) { DefaultPrimaryScrollableTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_darkTheme_primary_scrollable"
        )
    }

    @Test
    fun darkTheme_secondary_scrollable() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme(darkColorScheme()) { DefaultSecondaryScrollableTabs(interactionSource) }
        }

        assertTabsMatch(
            scope = scope!!,
            interactionSource = interactionSource,
            interaction = null,
            goldenIdentifier = "tabs_darkTheme_secondary_scrollable"
        )
    }

    /**
     * Asserts that the tabs match the screenshot with identifier [goldenIdentifier].
     *
     * @param interactionSource the [MutableInteractionSource] used for the first Tab
     * @param interaction the [Interaction] to assert for, or `null` if no [Interaction].
     * @param goldenIdentifier the identifier for the corresponding screenshot
     */
    private fun assertTabsMatch(
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
            // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't
            // properly wait for synchronization. Instead just wait until after the ripples are
            // finished animating.
            Thread.sleep(300)
        }

        // Capture and compare screenshots
        composeTestRule
            .onNodeWithTag(TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}

/**
 * Default primary colored [TabRow] with three [Tab]s. The first [Tab] is selected, and the rest are
 * not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [Tab], to control its
 *   visual state.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DefaultPrimaryTabs(interactionSource: MutableInteractionSource) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TAG)) {
        PrimaryTabRow(selectedTabIndex = 0) {
            Tab(
                selected = true,
                onClick = {},
                text = { Text("TAB") },
                interactionSource = interactionSource
            )
            Tab(selected = false, onClick = {}, text = { Text("TAB") })
            Tab(selected = false, onClick = {}, text = { Text("TAB") })
        }
    }
}

/**
 * Default secondary colored [TabRow] with three [Tab]s. The first [Tab] is selected, and the rest
 * are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [Tab], to control its
 *   visual state.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DefaultSecondaryTabs(interactionSource: MutableInteractionSource) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TAG)) {
        SecondaryTabRow(selectedTabIndex = 0) {
            Tab(
                selected = true,
                onClick = {},
                text = { Text("TAB") },
                interactionSource = interactionSource
            )
            Tab(selected = false, onClick = {}, text = { Text("TAB") })
            Tab(selected = false, onClick = {}, text = { Text("TAB") })
        }
    }
}

/**
 * Custom primary colored [TabRow] with three [Tab]s. The first [Tab] is selected, and the rest are
 * not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [Tab], to control its
 *   visual state.
 * @param containerColor the containerColor of the [TabRow]
 * @param selectedContentColor the content color for a selected [Tab] (first tab)
 * @param unselectedContentColor the content color for an unselected [Tab] (second and third tabs)
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomPrimaryTabs(
    interactionSource: MutableInteractionSource,
    containerColor: Color,
    selectedContentColor: Color,
    unselectedContentColor: Color
) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TAG)) {
        PrimaryTabRow(selectedTabIndex = 0, containerColor = containerColor) {
            Tab(
                selected = true,
                onClick = {},
                text = { Text("TAB") },
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
                interactionSource = interactionSource
            )
            Tab(
                selected = false,
                onClick = {},
                text = { Text("TAB") },
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor
            )
            Tab(
                selected = false,
                onClick = {},
                text = { Text("TAB") },
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor
            )
        }
    }
}

/**
 * Custom secondary colored [TabRow] with three [Tab]s. The first [Tab] is selected, and the rest
 * are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [Tab], to control its
 *   visual state.
 * @param containerColor the containerColor of the [TabRow]
 * @param selectedContentColor the content color for a selected [Tab] (first tab)
 * @param unselectedContentColor the content color for an unselected [Tab] (second and third tabs)
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomSecondaryTabs(
    interactionSource: MutableInteractionSource,
    containerColor: Color,
    selectedContentColor: Color,
    unselectedContentColor: Color
) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TAG)) {
        SecondaryTabRow(
            selectedTabIndex = 0,
            containerColor = containerColor,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(0),
                    color = selectedContentColor
                )
            }
        ) {
            Tab(
                selected = true,
                onClick = {},
                text = { Text("TAB") },
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
                interactionSource = interactionSource
            )
            Tab(
                selected = false,
                onClick = {},
                text = { Text("TAB") },
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor
            )
            Tab(
                selected = false,
                onClick = {},
                text = { Text("TAB") },
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor
            )
        }
    }
}

/**
 * Default primary colored [TabRow] with three [LeadingIconTab]s. The first [LeadingIconTab] is
 * selected, and the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [LeadingIconTab], to
 *   control its visual state.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DefaultPrimaryLeadingIconTabs(interactionSource: MutableInteractionSource) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TAG)) {
        PrimaryTabRow(selectedTabIndex = 0) {
            LeadingIconTab(
                selected = true,
                onClick = {},
                text = { Text("TAB") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") },
                interactionSource = interactionSource
            )
            LeadingIconTab(
                selected = false,
                onClick = {},
                text = { Text("TAB") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") }
            )
            LeadingIconTab(
                selected = false,
                onClick = {},
                text = { Text("TAB") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") }
            )
        }
    }
}

/**
 * Default secondary colored [TabRow] with three [LeadingIconTab]s. The first [LeadingIconTab] is
 * selected, and the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [LeadingIconTab], to
 *   control its visual state.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DefaultSecondaryLeadingIconTabs(interactionSource: MutableInteractionSource) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TAG)) {
        SecondaryTabRow(selectedTabIndex = 0) {
            LeadingIconTab(
                selected = true,
                onClick = {},
                text = { Text("TAB") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") },
                interactionSource = interactionSource
            )
            LeadingIconTab(
                selected = false,
                onClick = {},
                text = { Text("TAB") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") }
            )
            LeadingIconTab(
                selected = false,
                onClick = {},
                text = { Text("TAB") },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") }
            )
        }
    }
}

/**
 * Default primary colored [ScrollableTabRow] with three [Tab]s. The first [Tab] is selected, and
 * the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [Tab], to control its
 *   visual state.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DefaultPrimaryScrollableTabs(interactionSource: MutableInteractionSource) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TAG)) {
        PrimaryScrollableTabRow(
            selectedTabIndex = 0,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(0, matchContentSize = true),
                )
            }
        ) {
            Tab(
                selected = true,
                onClick = {},
                text = { Text("TAB") },
                interactionSource = interactionSource
            )
            Tab(selected = false, onClick = {}, text = { Text("TAB") })
            Tab(selected = false, onClick = {}, text = { Text("TAB") })
        }
    }
}

/**
 * Default secondary colored [ScrollableTabRow] with three [Tab]s. The first [Tab] is selected, and
 * the rest are not.
 *
 * @param interactionSource the [MutableInteractionSource] for the first [Tab], to control its
 *   visual state.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DefaultSecondaryScrollableTabs(interactionSource: MutableInteractionSource) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TAG)) {
        SecondaryScrollableTabRow(selectedTabIndex = 0) {
            Tab(
                selected = true,
                onClick = {},
                text = { Text("TAB") },
                interactionSource = interactionSource
            )
            Tab(selected = false, onClick = {}, text = { Text("TAB") })
            Tab(selected = false, onClick = {}, text = { Text("TAB") })
        }
    }
}

private const val TAG = "Tab"
