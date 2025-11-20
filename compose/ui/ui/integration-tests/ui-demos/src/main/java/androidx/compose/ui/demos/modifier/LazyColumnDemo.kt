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

package androidx.compose.ui.demos.modifier

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.dp

@Composable
fun LazyColumnDemo() {
    if (isArrEnabled) {
        val context = LocalContext.current
        val activity: Activity? = findOwner(context)
        DisposableEffect(activity) {
            activity?.window?.frameRateBoostOnTouchEnabled = false
            onDispose { activity?.window?.frameRateBoostOnTouchEnabled = true }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(60.dp).background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Add a single item
        items(100) { index ->
            when (index % 4) {
                0 -> AlphaButton(30f)
                1 -> AlphaButton(60f)
                2 -> AlphaButton(80f)
                3 -> AlphaButton(120f)
                else -> {}
            }
        }
    }
}

@Composable
private fun AlphaButton(frameRate: Float) {
    var targetAlpha by remember { mutableFloatStateOf(1f) }
    val alpha by
        animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(durationMillis = 5000))

    Button(onClick = { targetAlpha = if (targetAlpha == 1f) 0.2f else 1f }) {
        Text(
            text = "Click Me for alpha change $frameRate",
            color = LocalContentColor.current.copy(alpha = alpha), // Adjust text alpha
            modifier = Modifier.preferredFrameRate(frameRate),
        )
    }
}
