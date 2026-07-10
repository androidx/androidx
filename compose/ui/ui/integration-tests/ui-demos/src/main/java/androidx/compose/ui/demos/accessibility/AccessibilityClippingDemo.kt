/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.demos.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AccessibilityClippingDemo() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "The blue box is the size of the gray box, but then there is a box in-between that" +
                " is half the size and clips the box underneath it, so we expect it to take up" +
                " the top left quarter of the gray box. The goal is to use the a11y inspector" +
                " on the visible blue box and find that the boundsInScreen is half the size," +
                " but the shape is the full size."
        )

        ClippedBoxDemo(400.dp)
    }
}

@Composable
private fun ClippedBoxDemo(sideLength: Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${sideLength.value}px")
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            Box(Modifier.size(sideLength).background(Color.Black.copy(alpha = 0.1f))) {
                Box(Modifier.requiredSize(sideLength / 2).clipToBounds()) {
                    Box(
                        Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(sideLength)
                            .clip(RectangleShape)
                            .background(Color.Blue)
                    )
                }
            }
        }
    }
}
