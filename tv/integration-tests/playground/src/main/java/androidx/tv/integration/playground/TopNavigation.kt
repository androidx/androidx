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

package androidx.tv.integration.playground

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

enum class Navigation(val displayName: String, val action: @Composable () -> Unit) {
    StandardNavigationDrawer("Standard Navigation Drawer", { StandardNavigationDrawer() }),
    ModalNavigationDrawer("Modal Navigation Drawer", { ModalNavigationDrawer() }),
    LazyRowsAndColumns("Lazy Rows and Columns", { LazyRowsAndColumns() }),
    FeaturedCarousel("Featured Carousel", { FeaturedCarouselContent() }),
    ImmersiveList("Immersive List", { ImmersiveListContent() }),
    TextField("Text Field", { TextFieldContent() }),
    StickyHeader("Sticky Header", { StickyHeaderContent() });

    fun toRouteValue(): String {
        return "/${displayName.lowercase().replace(' ', '-')}";
    }
}

@Composable
internal fun TopNavigation(
    initialSelectedTab: Navigation,
    updateSelectedTab: (Navigation) -> Unit = {},
) {
    val tabs = Navigation.values().map { it.displayName }
    var selectedTabIndex by remember {
        mutableStateOf(Navigation.values().indexOf(initialSelectedTab))
    }

    // Pill indicator
    PillIndicatorTabRow(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        updateSelectedTab = { selectedTabIndex = it }
    )

    // Underlined indicator
//    UnderlinedIndicatorTabRow(
//        tabs = tabs,
//        selectedTabIndex = selectedTabIndex,
//        updateSelectedTab = { selectedTabIndex = it }
//    )

    LaunchedEffect(selectedTabIndex) {
        // Only update the tab after 250 milliseconds to avoid loading intermediate tabs while
        // fast scrolling in the TabRow
        delay(250)
        updateSelectedTab(Navigation.values()[selectedTabIndex])
    }
}

/**
 * Pill indicator tab row for reference
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PillIndicatorTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    updateSelectedTab: (Int) -> Unit
) {
    val focusRestorerModifiers = createCustomInitialFocusRestorerModifiers()

    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier
            .then(focusRestorerModifiers.parentModifier)
    ) {
        tabs.forEachIndexed { index, tab ->
            key(index) {
                Tab(
                    selected = index == selectedTabIndex,
                    onFocus = { updateSelectedTab(index) },
                    modifier = Modifier
                        .ifElse(index == 0, focusRestorerModifiers.childModifier)
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Underlined indicator tab row for reference
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UnderlinedIndicatorTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    updateSelectedTab: (Int) -> Unit
) {
    val focusRestorerModifiers = createCustomInitialFocusRestorerModifiers()

    TabRow(
        selectedTabIndex = selectedTabIndex,
        separator = { Spacer(modifier = Modifier.width(12.dp)) },
        indicator = { tabPositions, doesTabRowHaveFocus ->
            TabRowDefaults.UnderlinedIndicator(
                currentTabPosition = tabPositions[selectedTabIndex],
                doesTabRowHaveFocus = doesTabRowHaveFocus,
            )
        },
        modifier = Modifier
            .then(focusRestorerModifiers.parentModifier),
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedTabIndex,
                onFocus = { updateSelectedTab(index) },
                modifier = Modifier
                    .ifElse(index == 0, focusRestorerModifiers.childModifier),
                colors = TabDefaults.underlinedIndicatorTabColors(),
            ) {
                Text(
                    text = tab,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}
