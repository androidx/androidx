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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.UserAction
import com.android.compose.animation.scene.UserActionResult
import com.android.compose.grid.VerticalGrid

object Launcher {
    fun userActions(
        shadeScene: SceneKey,
        configuration: DemoConfiguration,
    ): Map<UserAction, UserActionResult> {
        return buildList {
                if (configuration.enableOverlays) {
                    add(
                        Swipe.Down(fromSource = SceneContainerArea.StartHalf) to
                            UserActionResult.ShowOverlay(Overlays.Notifications)
                    )
                    add(
                        Swipe.Down(fromSource = SceneContainerArea.EndHalf) to
                            UserActionResult.ShowOverlay(Overlays.QuickSettings)
                    )
                } else {
                    add(Swipe.Down to shadeScene)
                    add(Swipe.Down(pointerCount = 2) to Scenes.QuickSettings)
                }
            }
            .toMap()
    }

    object Elements {
        val Scene = ElementKey("LauncherScene")
        val SmartSpace = ElementKey("SmartSpace")
        val IconsGrid = ElementKey("LauncherIconsGrid")
    }
}

@Composable
fun ContentScope.Launcher(columnsCount: Int, modifier: Modifier = Modifier) {
    Column(modifier.element(Launcher.Elements.Scene)) {
        SmartSpace(
            MaterialTheme.colorScheme.onBackground,
            Modifier.element(Launcher.Elements.SmartSpace).padding(top = 40.dp, start = 40.dp),
        )

        VerticalGrid(columnsCount, Modifier.element(Launcher.Elements.IconsGrid).padding(16.dp)) {
            val iconColor = MaterialTheme.colorScheme.tertiary
            repeat(columnsCount * 5) {
                Box(Modifier.fillMaxSize()) { AppIcon(iconColor, Modifier.align(Alignment.Center)) }
            }
        }
    }
}

@Composable
private fun AppIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(60.dp)) { drawCircle(color) }
}
