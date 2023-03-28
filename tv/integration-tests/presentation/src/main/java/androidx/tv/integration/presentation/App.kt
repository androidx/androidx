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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ModalNavigationDrawer

val pageColor = Color(0xff18171a)

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val tabs = listOf("Home", "Movies", "Shows")
    var selectedTabIndex by remember { mutableStateOf(0) }
    val homePageFr = remember { FocusRequester() }
    val moviesPageFr = remember { FocusRequester() }
    val showsPageFr = remember { FocusRequester() }

    val tabRow = @Composable {
        AppTabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onSelectedTabIndexChange = { selectedTabIndex = it },
            modifier = Modifier
                .zIndex(100f)
                .onKeyEvent {
                    if (it.key.nativeKeyCode == Key.DirectionDown.nativeKeyCode) {
                        val fr = when (selectedTabIndex) {
                            0 -> homePageFr
                            1 -> moviesPageFr
                            2 -> showsPageFr
                            else -> null
                        }
                        fr?.requestFocus()
                        true
                    } else
                    false
                }
        )
    }

    val homepage = @Composable {
        TvLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(homePageFr)
                .background(pageColor)
        ) {
            item {
                FeaturedCarousel(
//                    modifier = Modifier.focusRequester(homePageFr)
                )
                AppSpacer(height = 50.dp)
            }
            movieCollections.forEach { movieCollection ->
                item {
                    AppLazyRow(
                        title = movieCollection.label,
                        items = movieCollection.items,
                        drawItem = { movie, _, modifier ->
                            ImageCard(
                                movie,
                                aspectRatio = 2f / 3,
                                modifier = modifier
                            )
                        }
                    )
                    AppSpacer(height = 35.dp)
                }
            }
        }
    }

    val moviesPage = @Composable {
        TvLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(pageColor)
        ) {
            item {
                AppImmersiveList(Modifier.focusRequester(moviesPageFr))
            }
        }
    }

    val showsPage = @Composable {
        TvLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(pageColor)
        ) {
            item {
                ShowsGrid(Modifier.focusRequester(showsPageFr))
            }
        }
    }

    val activePage: MutableState<(@Composable () -> Unit)> = remember(selectedTabIndex) {
        mutableStateOf(
            when (selectedTabIndex) {
                0 -> homepage
                1 -> moviesPage
                2 -> showsPage
                else -> homepage
            }
        )
    }

    ModalNavigationDrawer(
        drawerContent = {
            Sidebar(
                selectedIndex = selectedTabIndex,
                onIndexChange = { selectedTabIndex = it }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            activePage.value()
            tabRow()
        }
    }
}
