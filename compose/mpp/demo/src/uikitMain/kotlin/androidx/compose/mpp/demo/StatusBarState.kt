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

package androidx.compose.mpp.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.unit.dp
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleDefault
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIView

private var statusBarStyleIndex by mutableStateOf(0)
val preferredStatusBarStyleValue by derivedStateOf { styleValues[statusBarStyleIndex].second }

private var statusBarHiddenIndex by mutableStateOf(0)
val prefersStatusBarHiddenValue by derivedStateOf { hiddenValues[statusBarHiddenIndex].second }

private var statusBarAnimationIndex by mutableStateOf(0)
val preferredStatysBarAnimationValue by derivedStateOf { animationValues[statusBarAnimationIndex].second }

private val hiddenValues = listOf(
    "null" to null,
    "True" to true,
    "False" to false
)

private val animationValues = listOf(
    "null" to null,
    "UIStatusBarAnimationFade" to UIStatusBarAnimation.UIStatusBarAnimationFade,
    "UIStatusBarAnimationSlide" to UIStatusBarAnimation.UIStatusBarAnimationSlide
)

private val styleValues = listOf(
    "null" to null,
    "UIStatusBarStyleDefault" to UIStatusBarStyleDefault,
    "UIStatusBarStyleLightContent" to UIStatusBarStyleLightContent,
    "UIStatusBarStyleDarkContent" to UIStatusBarStyleDarkContent
)

val StatusBarStateExample = Screen.Example("StatusBarState") {
    Column(modifier = Modifier.fillMaxSize()) {
        Dropdown("preferredStatusBarStyle", styleValues[statusBarStyleIndex].first, styleValues.map { it.first }) {
            statusBarStyleIndex = it
        }

        Dropdown("prefersStatusBarHidden", hiddenValues[statusBarHiddenIndex].first, hiddenValues.map { it.first }) {
            statusBarHiddenIndex = it
        }

        Dropdown("preferredStatysBarAnimation", animationValues[statusBarAnimationIndex].first, animationValues.map { it.first }) {
            statusBarAnimationIndex = it
        }
    }

    val viewController = LocalUIViewController.current
    LaunchedEffect(statusBarStyleIndex, statusBarHiddenIndex, statusBarAnimationIndex) {
        UIView.animateWithDuration(0.3) {
            viewController.setNeedsStatusBarAppearanceUpdate()
        }
    }
}

@Composable
private fun Dropdown(
    name: String,
    current: String,
    all: List<String>,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()
        .clickable { expanded = true }) {
        Text("$name: $current")

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }) {
            all.forEachIndexed { index, item ->
                DropdownMenuItem(
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }) {
                    Text(item)
                }
            }
        }
    }
}