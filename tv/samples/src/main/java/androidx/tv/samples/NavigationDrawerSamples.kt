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

package androidx.tv.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Sampled
@Composable
fun SampleNavigationDrawer() {
    var selectedIndex by remember { mutableIntStateOf(0) }

    val items = listOf(
        "Home" to Icons.Default.Home,
        "Settings" to Icons.Default.Settings,
        "Favourites" to Icons.Default.Favorite,
    )

    NavigationDrawer(
        drawerContent = {
            Column(
                Modifier
                    .background(Color.Gray)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .selectableGroup(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEachIndexed { index, item ->
                    val (text, icon) = item

                    NavigationDrawerItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        leadingContent = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                            )
                        }
                    ) {
                        Text(text)
                    }
                }
            }
        }
    ) {
        Button(modifier = Modifier.height(100.dp).fillMaxWidth(), onClick = {}) {
            Text("BUTTON")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Sampled
@Composable
fun SampleModalNavigationDrawerWithSolidScrim() {
    var selectedIndex by remember { mutableIntStateOf(0) }

    val items = listOf(
        "Home" to Icons.Default.Home,
        "Settings" to Icons.Default.Settings,
        "Favourites" to Icons.Default.Favorite,
    )

    val closeDrawerWidth = 80.dp
    val backgroundContentPadding = 10.dp
    ModalNavigationDrawer(
        drawerContent = {
            Column(
                Modifier
                    .background(Color.Gray)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .selectableGroup(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEachIndexed { index, item ->
                    val (text, icon) = item

                    NavigationDrawerItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        leadingContent = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                            )
                        }
                    ) {
                        Text(text)
                    }
                }
            }
        }
    ) {
        Button(
            modifier = Modifier
                .padding(closeDrawerWidth + backgroundContentPadding)
                .height(100.dp)
                .fillMaxWidth(),
            onClick = {}
        ) {
            Text("BUTTON")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Sampled
@Composable
fun SampleModalNavigationDrawerWithGradientScrim() {
    var selectedIndex by remember { mutableIntStateOf(0) }

    val items = listOf(
        "Home" to Icons.Default.Home,
        "Settings" to Icons.Default.Settings,
        "Favourites" to Icons.Default.Favorite,
    )

    val closeDrawerWidth = 80.dp
    val backgroundContentPadding = 10.dp

    ModalNavigationDrawer(
        drawerContent = {
            Column(
                Modifier
                    .background(Color.Gray)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .selectableGroup(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEachIndexed { index, item ->
                    val (text, icon) = item

                    NavigationDrawerItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        leadingContent = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                            )
                        }
                    ) {
                        Text(text)
                    }
                }
            }
        },
        scrimBrush = Brush.horizontalGradient(listOf(Color.DarkGray, Color.Transparent))
    ) {
        Button(
            modifier = Modifier
                .padding(closeDrawerWidth + backgroundContentPadding)
                .height(100.dp)
                .fillMaxWidth(),
            onClick = {}
        ) {
            Text("BUTTON")
        }
    }
}
