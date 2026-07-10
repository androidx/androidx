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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.UserAction
import com.android.compose.animation.scene.UserActionResult
import com.android.compose.animation.scene.ValueKey
import com.android.compose.animation.scene.animateElementColorAsState

object Camera {
    fun userActions(lockscreenScene: SceneKey): Map<UserAction, UserActionResult> =
        mapOf(Back to lockscreenScene)

    object Elements {
        val Background = ElementKey("CameraBackground")
        val Button = ElementKey("CameraButton")
        val ButtonIcon = ElementKey("CameraButtonBackground")
    }

    object Values {
        val ButtonColor = ValueKey("CameraButtonColor")
        val ButtonIconColor = ValueKey("CameraButtonIconColor")
    }
}

@Composable
fun ContentScope.Camera(modifier: Modifier = Modifier) {
    Box(modifier) {
        Box(Modifier.element(Camera.Elements.Background).fillMaxSize().background(Color.Black))
        CameraButton(
            backgroundColor = Color.White,
            iconColor = Color.Black,
            onClick = {},
            Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp).size(70.dp),
        )
    }
}

@Composable
fun ContentScope.CameraButton(
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElementWithValues(Camera.Elements.Button, modifier) {
        val backgroundColor by
            animateElementColorAsState(backgroundColor, Camera.Values.ButtonColor)
        val iconColor by
            animateElementColorAsState(iconColor, Camera.Values.ButtonIconColor)
                // TODO(b/231674463): We should not read iconColor during composition.
                .unsafeCompositionState(initialValue = iconColor)

        content {
            Box(
                Modifier.fillMaxSize().clip(CircleShape).clickable(onClick = onClick).drawBehind {
                    drawRect(backgroundColor)
                }
            ) {
                CameraButtonIcon(
                    iconColor = iconColor,
                    Modifier.element(Camera.Elements.ButtonIcon).size(24.dp).align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun CameraButtonIcon(iconColor: Color, modifier: Modifier = Modifier) {
    // Note: In practice, we should somehow defer the icon color read to the drawing phase but
    // there is no overload of Icon taking a `tint: () -> Color`.
    Icon(
        Icons.Default.PhotoCamera,
        contentDescription = null,
        tint = iconColor,
        modifier = modifier,
    )
}
