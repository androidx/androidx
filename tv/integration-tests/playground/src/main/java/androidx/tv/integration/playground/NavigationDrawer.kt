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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StandardNavigationDrawer() {
    val direction = remember { mutableStateOf(LayoutDirection.Ltr) }

    CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
        Row(Modifier.fillMaxSize()) {
            Box(modifier = Modifier.height(400.dp)) {
                NavigationDrawer(
                    drawerContent = { drawerValue ->
                        Sidebar(
                            drawerValue = drawerValue,
                            direction = direction,
                        )
                    }
                ) {
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
                    drawerContent = { drawerValue ->
                        Sidebar(
                            drawerValue = drawerValue,
                            direction = direction,
                        )
                    }
                ) {
                    CommonBackground()
                }
            }
        }
    }
}

@Composable
private fun CommonBackground() {
    Row(modifier = Modifier.padding(start = 10.dp)) {
        Card(backgroundColor = Color.Red)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Sidebar(
    drawerValue: DrawerValue,
    direction: MutableState<LayoutDirection>,
) {
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
            .background(pageColor)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NavigationItem(
            imageVector = Icons.Default.KeyboardArrowRight,
            text = "LTR",
            drawerValue = drawerValue,
            selectedIndex = selectedIndex,
            index = 0
        )
        NavigationItem(
            imageVector = Icons.Default.KeyboardArrowLeft,
            text = "RTL",
            drawerValue = drawerValue,
            selectedIndex = selectedIndex,
            index = 1
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationItem(
    imageVector: ImageVector,
    text: String,
    drawerValue: DrawerValue,
    selectedIndex: MutableState<Int>,
    index: Int,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .background(if (isFocused) Color.White else Color.Transparent)
            .semantics(mergeDescendants = true) {
                selected = selectedIndex.value == index
            }
            .clickable {
                selectedIndex.value = index
            }
    ) {
        Box(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = imageVector,
                    tint = if (isFocused) pageColor else Color.White,
                    contentDescription = null,
                )
                AnimatedVisibility(visible = drawerValue == DrawerValue.Open) {
                    Text(
                        text = text,
                        modifier = Modifier,
                        softWrap = false,
                        color = if (isFocused) pageColor else Color.White,
                    )
                }
            }
            if (selectedIndex.value == index) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(3.dp)
                        .offset(y = 5.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Red)
                        .zIndex(10f)
                )
            }
        }
    }
}
