/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.StructurallyEqual
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.clipToBounds
import androidx.ui.core.drawLayer
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.foundation.drawBorder
import androidx.ui.foundation.gestures.ZoomableState
import androidx.ui.foundation.gestures.zoomable
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.unit.dp
import androidx.ui.unit.sp

@Sampled
@Composable
fun ZoomableSample() {
    Box(
        Modifier.preferredSize(700.dp).clipToBounds(),
        backgroundColor = Color.LightGray
    ) {
        var scale by state(StructurallyEqual) { 1f }
        val zoomableState = ZoomableState { scale *= it }

        Box(
            Modifier
                .zoomable(zoomableState)
                .clickable(
                    indication = null,
                    onDoubleClick = { zoomableState.smoothScaleBy(4f) },
                    onClick = {}
                )
                .fillMaxSize()
                .drawBorder(1.dp, Color.Green),
            gravity = ContentGravity.Center
        ) {
            Text(
                "☠",
                fontSize = 32.sp,
                modifier = Modifier.drawLayer(scaleX = scale, scaleY = scale)
            )
        }
    }
}