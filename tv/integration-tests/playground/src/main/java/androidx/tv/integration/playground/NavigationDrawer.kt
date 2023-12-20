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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StandardNavigationDrawer() {
    val direction = remember { mutableStateOf(LayoutDirection.Ltr) }

    CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
        Row(Modifier.fillMaxSize()) {
            Box(modifier = Modifier.height(400.dp)) {
                NavigationDrawer(drawerContent = { Sidebar(direction = direction) }) {
                    CommonBackground()
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ModalNavigationDrawer() {
    val direction = remember { mutableStateOf(LayoutDirection.Ltr) }

    CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
        Row(Modifier.fillMaxSize()) {
            Box(modifier = Modifier.height(400.dp)) {
                androidx.tv.material3.ModalNavigationDrawer(
                    drawerContent = { Sidebar(direction = direction) },
                    scrimBrush = Brush.verticalGradient(
                        listOf(
                            Color.DarkGray.copy(alpha = 0.2f),
                            Color.LightGray.copy(alpha = 0.2f)
                        )
                    )
                ) {
                    CommonBackground(startPadding = 90.dp)
                }
            }
        }
    }
}

@Composable
private fun CommonBackground(startPadding: Dp = 10.dp) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Blue.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(start = startPadding)) {
            Card(backgroundColor = Color.Red)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerScope.Sidebar(direction: MutableState<LayoutDirection>) {
    val selectedIndex = remember { mutableStateOf(0) }

    LaunchedEffect(selectedIndex.value) {
        direction.value = when (selectedIndex.value) {
            0 -> LayoutDirection.Ltr
            else -> LayoutDirection.Rtl
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(pageColor.copy(alpha = 0.5f))
            .padding(12.dp)
            .selectableGroup(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NavigationDrawerItem(
            selected = true,
            onClick = { },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                )
            },
            supportingContent = {
                Text("Switch account")
            },
            trailingContent = {
                NavigationDrawerItemDefaults.TrailingBadge("NEW")
            }
        ) {
            Text(text = "Hi there")
        }
        NavigationDrawerItem(
            selected = selectedIndex.value == 0,
            onClick = { selectedIndex.value = 0 },
            leadingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null,
                )
            },
        ) {
            Text(text = "Left to right")
        }
        NavigationDrawerItem(
            selected = selectedIndex.value == 1,
            onClick = { selectedIndex.value = 1 },
            leadingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                    contentDescription = null,
                )
            },
        ) {
            Text(text = "Right to left")
        }
    }
}
