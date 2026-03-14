/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.samples

import android.os.IBinder
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Sampled
@Composable
fun PopupSample() {
    Box {
        val popupWidth = 200.dp
        val popupHeight = 50.dp
        val cornerSize = 16.dp

        Popup(alignment = Alignment.Center) {
            // Draw a rectangle shape with rounded corners inside the popup
            Box(
                Modifier.size(popupWidth, popupHeight)
                    .background(Color.White, RoundedCornerShape(cornerSize))
            )
        }
    }
}

@Sampled
@Composable
fun PopupWithPositionProviderSample() {
    val dropdownPopupPositioner = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                // Position the popup below the anchor aligned horizontally with the anchor's
                // center.
                return IntOffset(
                    x = anchorBounds.left + anchorBounds.width / 2,
                    y = anchorBounds.top + anchorBounds.height * 2,
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.background(Color.LightGray).padding(10.dp)) {
            BasicText(text = "Anchor")

            Popup(popupPositionProvider = dropdownPopupPositioner) {
                Box(
                    modifier =
                        Modifier.background(Color.Green, RoundedCornerShape(16.dp)).padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(text = "Popup")
                }
            }
        }
    }
}

@Sampled
@Composable
fun PopupFromServiceSample() {
    // In a real Service scenario, appWindowToken would be received via IPC
    // from the main application process. This sample simulates having the token.
    val appWindowToken: IBinder? = null // Provided via IPC

    var showPopup by remember { mutableStateOf(false) }

    Button(onClick = { showPopup = true }) { Text("Show Popup From Service") }

    if (showPopup) {
        Popup(
            onDismissRequest = { showPopup = false },
            properties =
                PopupProperties(
                    // Pass the application's window token
                    windowToken = appWindowToken
                ),
        ) {
            Box(Modifier.size(200.dp, 100.dp).background(Color.Blue.copy(alpha = 0.8f))) {
                Text("Popup Content (Service)", color = Color.White)
            }
        }
    }
}
