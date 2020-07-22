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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.drawBorder
import androidx.compose.foundation.gestures.rememberZoomableController
import androidx.compose.foundation.gestures.zoomable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.compose.structuralEqualityPolicy
import androidx.ui.core.Modifier
import androidx.ui.core.clipToBounds
import androidx.ui.core.drawLayer
import androidx.compose.ui.graphics.Color
import androidx.ui.unit.dp
import androidx.ui.unit.sp

@Sampled
@Composable
fun ZoomableSample() {
    Box(
        Modifier.preferredSize(300.dp).clipToBounds(),
        backgroundColor = Color.LightGray
    ) {
        var scale by state(structuralEqualityPolicy()) { 1f }
        val zoomableController = rememberZoomableController { scale *= it }
        Box(
            Modifier
                .zoomable(zoomableController)
                .clickable(
                    indication = null,
                    onDoubleClick = { zoomableController.smoothScaleBy(4f) },
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