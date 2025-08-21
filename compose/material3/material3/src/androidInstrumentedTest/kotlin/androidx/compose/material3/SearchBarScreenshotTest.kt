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

import androidx.activity.BackEventCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMaterial3Api::class)
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class SearchBarScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val testTag = "SearchBar"

    @Test
    fun searchBar_notExpanded() {
        rule.setMaterialContent(scheme.colorScheme) {
            val expanded = false
            val onExpandedChange: (Boolean) -> Unit = {}
            SearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState(),
                        onSearch = {},
                        expanded = expanded,
                        onExpandedChange = onExpandedChange,
                        placeholder = { Text("Hint") },
                    )
                },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                content = {},
            )
        }
        assertAgainstGolden("searchBar_inactive_${scheme.name}")
    }

    @Test
    fun searchBar_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            val expanded = false
            val onExpandedChange: (Boolean) -> Unit = {}
            SearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState(),
                        onSearch = {},
                        expanded = expanded,
                        onExpandedChange = onExpandedChange,
                        enabled = false,
                        placeholder = { Text("Hint") },
                    )
                },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                content = {},
            )
        }
        assertAgainstGolden("searchBar_disabled_${scheme.name}")
    }

    @Test
    fun searchBar_expanded() {
        rule.setMaterialContent(scheme.colorScheme) {
            val expanded = true
            val onExpandedChange: (Boolean) -> Unit = {}
            SearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState("Query"),
                        onSearch = {},
                        expanded = expanded,
                        onExpandedChange = onExpandedChange,
                    )
                },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_active_${scheme.name}")
    }

    @Test
    fun searchBar_expanded_withIcons() {
        rule.setMaterialContent(scheme.colorScheme) {
            val expanded = true
            val onExpandedChange: (Boolean) -> Unit = {}
            SearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState("Query"),
                        onSearch = {},
                        expanded = expanded,
                        onExpandedChange = onExpandedChange,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    )
                },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_active_withIcons_${scheme.name}")
    }

    @Test
    fun searchBar_expanded_customColors() {
        rule.setMaterialContent(lightColorScheme()) {
            val expanded = true
            val onExpandedChange: (Boolean) -> Unit = {}
            SearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState("Query"),
                        onSearch = {},
                        expanded = expanded,
                        onExpandedChange = onExpandedChange,
                    )
                },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                colors =
                    SearchBarDefaults.colors(
                        containerColor = Color.Yellow,
                        dividerColor = Color.Green,
                    ),
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_active_customColors")
    }

    @Test
    fun searchBar_shadow_notExpanded() {
        rule.setMaterialContent(lightColorScheme()) {
            val expanded = false
            val onExpandedChange: (Boolean) -> Unit = {}
            SearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState(),
                        onSearch = {},
                        expanded = expanded,
                        onExpandedChange = onExpandedChange,
                        placeholder = { Text("Hint") },
                    )
                },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                shadowElevation = 6.dp,
                content = {},
            )
        }
        assertAgainstGolden("searchBar_shadow_inactive")
    }

    @Test
    fun searchBar_shadow_expanded() {
        rule.setMaterialContent(lightColorScheme()) {
            val expanded = true
            val onExpandedChange: (Boolean) -> Unit = {}
            SearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState("Query"),
                        onSearch = {},
                        expanded = expanded,
                        onExpandedChange = onExpandedChange,
                        placeholder = { Text("Hint") },
                    )
                },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                shadowElevation = 6.dp,
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_shadow_active")
    }

    @Test
    fun searchBar_predictiveBack_progress0() {
        rule.setMaterialContent(lightColorScheme()) { SearchBarPredictiveBack(progress = 0f) }
        assertAgainstGolden("searchBar_predictiveBack_progress0")
    }

    @Test
    fun searchBar_predictiveBack_progress25() {
        rule.setMaterialContent(lightColorScheme()) { SearchBarPredictiveBack(progress = 0.25f) }
        assertAgainstGolden("searchBar_predictiveBack_progress25")
    }

    @Test
    fun searchBar_predictiveBack_progress50() {
        rule.setMaterialContent(lightColorScheme()) { SearchBarPredictiveBack(progress = 0.50f) }
        assertAgainstGolden("searchBar_predictiveBack_progress50")
    }

    @Test
    fun searchBar_predictiveBack_progress75() {
        rule.setMaterialContent(lightColorScheme()) { SearchBarPredictiveBack(progress = 0.75f) }
        assertAgainstGolden("searchBar_predictiveBack_progress75")
    }

    @Test
    fun searchBar_predictiveBack_progress100() {
        rule.setMaterialContent(lightColorScheme()) { SearchBarPredictiveBack(progress = 1f) }
        assertAgainstGolden("searchBar_predictiveBack_progress100")
    }

    @Test
    fun dockedSearchBar_notExpanded() {
        rule.setMaterialContent(scheme.colorScheme) {
            DockedSearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState(),
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text("Hint") },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                content = {},
            )
        }
        assertAgainstGolden("dockedSearchBar_inactive_${scheme.name}")
    }

    @Test
    fun dockedSearchBar_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            DockedSearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState(),
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        enabled = false,
                        placeholder = { Text("Hint") },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                content = {},
            )
        }
        assertAgainstGolden("dockedSearchBar_disabled_${scheme.name}")
    }

    @Test
    fun dockedSearchBar_expanded() {
        rule.setMaterialContent(scheme.colorScheme) {
            DockedSearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState("Query"),
                        onSearch = {},
                        expanded = true,
                        onExpandedChange = {},
                    )
                },
                expanded = true,
                onExpandedChange = {},
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("dockedSearchBar_active_${scheme.name}")
    }

    @Test
    fun dockedSearchBar_expanded_withIcons() {
        rule.setMaterialContent(scheme.colorScheme) {
            DockedSearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState("Query"),
                        onSearch = {},
                        expanded = true,
                        onExpandedChange = {},
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    )
                },
                expanded = true,
                onExpandedChange = {},
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("dockedSearchBar_active_withIcons_${scheme.name}")
    }

    @Test
    fun dockedSearchBar_expanded_customShape() {
        rule.setMaterialContent(lightColorScheme()) {
            DockedSearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState("Query"),
                        onSearch = {},
                        expanded = true,
                        onExpandedChange = {},
                    )
                },
                expanded = true,
                onExpandedChange = {},
                shape = CutCornerShape(24.dp),
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("dockedSearchBar_active_customShape")
    }

    @Test
    fun dockedSearchBar_expanded_customColors() {
        rule.setMaterialContent(lightColorScheme()) {
            DockedSearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState("Query"),
                        onSearch = {},
                        expanded = true,
                        onExpandedChange = {},
                    )
                },
                expanded = true,
                onExpandedChange = {},
                colors =
                    SearchBarDefaults.colors(
                        containerColor = Color.Yellow,
                        dividerColor = Color.Green,
                    ),
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("dockedSearchBar_active_customColors")
    }

    @Test
    fun dockedSearchBar_shadow_notExpanded() {
        rule.setMaterialContent(lightColorScheme()) {
            DockedSearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState(),
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text("Hint") },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                shadowElevation = 6.dp,
                content = {},
            )
        }
        assertAgainstGolden("dockedSearchBar_shadow_inactive")
    }

    @Test
    fun dockedSearchBar_shadow_expanded() {
        rule.setMaterialContent(lightColorScheme()) {
            DockedSearchBar(
                modifier = Modifier.testTag(testTag),
                inputField = {
                    SearchBarDefaults.InputField(
                        state = rememberTextFieldState("Query"),
                        onSearch = {},
                        expanded = true,
                        onExpandedChange = {},
                    )
                },
                expanded = true,
                onExpandedChange = {},
                shadowElevation = 6.dp,
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("dockedSearchBar_shadow_active")
    }

    @Test
    fun newSearchBar_collapsed() {
        rule.setMaterialContent(scheme.colorScheme) {
            val state = rememberSearchBarState()
            SearchBar(
                modifier = Modifier.testTag(testTag),
                state = state,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = state,
                        textFieldState = rememberTextFieldState(),
                        onSearch = {},
                        placeholder = { Text("Hint") },
                    )
                },
            )
        }
        assertAgainstGolden("searchBar_collapsed_${scheme.name}")
    }

    @Test
    fun newSearchBar_collapsed_shadow() {
        rule.setMaterialContent(lightColorScheme()) {
            val state = rememberSearchBarState()
            SearchBar(
                modifier = Modifier.testTag(testTag),
                state = state,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = state,
                        textFieldState = rememberTextFieldState(),
                        onSearch = {},
                        placeholder = { Text("Hint") },
                    )
                },
                shadowElevation = 6.dp,
            )
        }
        assertAgainstGolden("searchBar_collapsed_shadow")
    }

    @Test
    fun newSearchBar_collapsed_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            val state = rememberSearchBarState()
            SearchBar(
                modifier = Modifier.testTag(testTag),
                state = state,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = state,
                        textFieldState = rememberTextFieldState(),
                        onSearch = {},
                        enabled = false,
                        placeholder = { Text("Hint") },
                    )
                },
            )
        }
        assertAgainstGolden("searchBar_collapsed_disabled_${scheme.name}")
    }

    @Test
    fun newSearchBar_fullScreen_expanded() {
        rule.setMaterialContent(scheme.colorScheme) {
            val state = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
            ExpandedFullScreenSearchBar(
                modifier = Modifier.testTag(testTag),
                state = state,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = state,
                        textFieldState = rememberTextFieldState("Query"),
                        onSearch = {},
                    )
                },
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_fullScreen_${scheme.name}")
    }

    @Test
    fun newSearchBar_fullScreen_expanded_withIcons() {
        rule.setMaterialContent(scheme.colorScheme) {
            val state = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
            ExpandedFullScreenSearchBar(
                modifier = Modifier.testTag(testTag),
                state = state,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = state,
                        textFieldState = rememberTextFieldState("Query"),
                        onSearch = {},
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    )
                },
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_fullScreen_withIcons_${scheme.name}")
    }

    @Test
    fun newSearchBar_fullScreen_expanded_customColors() {
        rule.setMaterialContent(lightColorScheme()) {
            val state = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
            val colors =
                SearchBarDefaults.colors(containerColor = Color.Yellow, dividerColor = Color.Green)
            ExpandedFullScreenSearchBar(
                modifier = Modifier.testTag(testTag),
                state = state,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = state,
                        textFieldState = rememberTextFieldState("Query"),
                        onSearch = {},
                        colors = colors.inputFieldColors,
                    )
                },
                colors = colors,
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_fullScreen_customColors")
    }

    @Test
    fun newSearchBar_docked_expanded() {
        rule.setMaterialContent(scheme.colorScheme) {
            val state = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
            ExpandedDockedSearchBar(
                modifier = Modifier.testTag(testTag),
                state = state,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = state,
                        textFieldState = rememberTextFieldState("Query"),
                        onSearch = {},
                    )
                },
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_docked_${scheme.name}")
    }

    @Test
    fun newSearchBar_docked_expanded_withIcons() {
        rule.setMaterialContent(scheme.colorScheme) {
            val state = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
            ExpandedDockedSearchBar(
                modifier = Modifier.testTag(testTag),
                state = state,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = state,
                        textFieldState = rememberTextFieldState("Query"),
                        onSearch = {},
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    )
                },
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_docked_withIcons_${scheme.name}")
    }

    @Test
    fun newSearchBar_docked_expanded_customShape() {
        rule.setMaterialContent(lightColorScheme()) {
            val state = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
            ExpandedDockedSearchBar(
                modifier = Modifier.testTag(testTag),
                state = state,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = state,
                        textFieldState = rememberTextFieldState("Query"),
                        onSearch = {},
                    )
                },
                shape = CutCornerShape(24.dp),
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_docked_customShape")
    }

    @Test
    fun newSearchBar_docked_expanded_customColors() {
        rule.setMaterialContent(lightColorScheme()) {
            val state = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
            val colors =
                SearchBarDefaults.colors(containerColor = Color.Yellow, dividerColor = Color.Green)
            ExpandedDockedSearchBar(
                modifier = Modifier.testTag(testTag),
                state = state,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = state,
                        textFieldState = rememberTextFieldState("Query"),
                        onSearch = {},
                        colors = colors.inputFieldColors,
                    )
                },
                colors = colors,
                content = { Text("Content") },
            )
        }
        assertAgainstGolden("searchBar_docked_customColors")
    }

    @Test
    fun appBarWithSearch_withNavigationIconAndActions() {
        rule.setMaterialContent(scheme.colorScheme) {
            val searchBarState = rememberSearchBarState()
            AppBarWithSearch(
                modifier = Modifier.testTag(testTag),
                state = searchBarState,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = searchBarState,
                        textFieldState = rememberTextFieldState(),
                        onSearch = {},
                        placeholder = { Text("Hint") },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account",
                        )
                    }
                },
            )
        }
        assertAgainstGolden("appBarWithSearch_withNavigationIconAndActions_${scheme.name}")
    }

    @Test
    fun appBarWithSearch_withoutNavigationIconAndActions() {
        rule.setMaterialContent(scheme.colorScheme) {
            val searchBarState = rememberSearchBarState()
            AppBarWithSearch(
                modifier = Modifier.testTag(testTag),
                state = searchBarState,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = searchBarState,
                        textFieldState = rememberTextFieldState(),
                        onSearch = {},
                        placeholder = { Text("Hint") },
                    )
                },
            )
        }
        assertAgainstGolden("appBarWithSearch_withoutNavigationIconAndActions_${scheme.name}")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(testTag).captureToImage().assertAgainstGolden(screenshotRule, goldenName)
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("lightTheme", lightColorScheme()),
                ColorSchemeWrapper("darkTheme", darkColorScheme()),
            )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }

    @Composable
    private fun SearchBarPredictiveBack(progress: Float) {
        val animationProgress = remember { Animatable(initialValue = 1 - progress) }
        val finalBackProgress = remember { mutableFloatStateOf(Float.NaN) }
        val firstBackEvent = remember {
            mutableStateOf<BackEventCompat?>(
                BackEventCompat(
                    touchX = 0f,
                    touchY = 0f,
                    progress = 0f,
                    swipeEdge = BackEventCompat.EDGE_LEFT,
                )
            )
        }
        val currentBackEvent = remember {
            mutableStateOf<BackEventCompat?>(
                BackEventCompat(
                    touchX = 0f,
                    touchY = 0f,
                    progress = progress,
                    swipeEdge = BackEventCompat.EDGE_LEFT,
                )
            )
        }

        SearchBarImpl(
            animationProgress = animationProgress,
            finalBackProgress = finalBackProgress,
            firstBackEvent = firstBackEvent,
            currentBackEvent = currentBackEvent,
            modifier = Modifier.testTag(testTag),
            inputField = {
                SearchBarDefaults.InputField(
                    state = rememberTextFieldState("Query"),
                    onSearch = {},
                    expanded = true,
                    onExpandedChange = {},
                )
            },
            content = { Text("Content") },
        )
    }
}
