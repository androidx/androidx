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
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Sidebar(
    selectedIndex: Int,
    onIndexChange: (index: Int) -> Unit,
) {
    val fr = remember { FocusRequester() }
    val drawIcon: @Composable (
        imageVector: ImageVector,
        index: Int,
        modifier: Modifier
    ) -> Unit =
        { imageVector, index, modifier ->
            var isFocused by remember { mutableStateOf(false) }
            val isSelected = selectedIndex == index

            IconButton(
                onClick = { },
                modifier = modifier
                    .onFocusChanged {
                        isFocused = it.isFocused
                        if (it.isFocused) {
                            onIndexChange(index)
                        }
                    }
                    .focusRequester(if (index == 0) fr else FocusRequester()),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor =
                    if (isSelected && isFocused)
                        Color.White
                    else
                        Color.Transparent,
                )
            ) {
                Box(modifier = Modifier) {
                    Icon(
                        imageVector = imageVector,
                        tint = if (isSelected && isFocused) pageColor else Color.White,
                        contentDescription = null,
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(3.dp)
                                .offset(y = 4.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }

    val focusRestorerModifiers = createCustomInitialFocusRestorerModifiers()

    Column(
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight()
            .background(pageColor)
            .then(focusRestorerModifiers.parentModifier)
            .focusGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        key(0) {
            drawIcon(
                Icons.Outlined.Home,
                0,
                focusRestorerModifiers.childModifier,
            )
        }
        key(1) {
            drawIcon(
                Icons.Outlined.Movie,
                1,
                Modifier,
            )
        }
        key(2) {
            drawIcon(
                Icons.Outlined.Tv,
                2,
                Modifier,
            )
        }
    }
}
