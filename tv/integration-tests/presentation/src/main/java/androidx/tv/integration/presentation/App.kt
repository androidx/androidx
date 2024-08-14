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

package androidx.tv.integration.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ModalNavigationDrawer

val pageColor = Color(0xff18171a)

enum class Tabs(val displayName: String, val action: @Composable () -> Unit) {
    Home(
        "Home",
        {
            LazyColumn(
                modifier = Modifier.fillMaxSize().focusRequester(Home.fr).background(pageColor)
            ) {
                item {
                    FeaturedCarousel()
                    AppSpacer(height = 50.dp)
                }
                movieCollections.forEach { movieCollection ->
                    item {
                        AppLazyRow(
                            title = movieCollection.label,
                            items = movieCollection.items,
                            drawItem = { movie, _, modifier ->
                                ImageCard(movie, aspectRatio = 2f / 3, modifier = modifier)
                            }
                        )
                        AppSpacer(height = 35.dp)
                    }
                }
            }
        }
    ),
    Shows(
        "Shows",
        {
            LazyColumn(modifier = Modifier.fillMaxSize().background(pageColor)) {
                item { ShowsGrid(Modifier.focusRequester(Shows.fr)) }
            }
        }
    );

    val fr: FocusRequester = FocusRequester()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun App() {
    val tabs = remember { Tabs.values() }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val activeTab = remember(selectedTabIndex) { tabs[selectedTabIndex] }

    val tabRow =
        @Composable {
            AppTabRow(
                tabs = tabs.map { it.displayName },
                selectedTabIndex = selectedTabIndex,
                onSelectedTabIndexChange = { selectedTabIndex = it },
                modifier =
                    Modifier.zIndex(100f).onKeyEvent {
                        if (it.key.nativeKeyCode == Key.DirectionDown.nativeKeyCode) {
                            activeTab.fr.requestFocus()
                            true
                        } else false
                    }
            )
        }

    val activePage: MutableState<(@Composable () -> Unit)> =
        remember(selectedTabIndex) { mutableStateOf(activeTab.action) }

    ModalNavigationDrawer(
        drawerContent = {
            Sidebar(selectedIndex = selectedTabIndex, onIndexChange = { selectedTabIndex = it })
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            activePage.value()
            tabRow()
        }
    }
}
