/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.testapp.animation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.alpha
import androidx.xr.compose.subspace.layout.padding

/*
 * Advanced animation example - Concurrent example
 * Panels fade out at the same time
 *
 */
@Composable
@SubspaceComposable
fun ConcurrentAnimationExample1(modifier: SubspaceModifier = SubspaceModifier) {
    var isVisible by remember { mutableStateOf(true) }
    val alpha: Float by
        animateFloatAsState(targetValue = if (isVisible) 1f else 0f, animationSpec = tween(2000))
    SpatialPanel {
        Button(onClick = { isVisible = !isVisible }) {
            Text(if (isVisible) "Fade out" else "Fade in")
        }
    }
    SpatialCurvedRow(modifier = modifier) {
        repeat(5) { index ->
            SpatialPanel(modifier = SubspaceModifier.alpha(alpha).padding(20.dp)) {
                Box(
                    modifier = Modifier.background(Color.LightGray).width(150.dp).height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Panel $index")
                }
            }
        }
    }
}
