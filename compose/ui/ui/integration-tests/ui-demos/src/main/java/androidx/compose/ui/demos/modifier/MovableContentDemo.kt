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
import android.content.Context
import android.content.ContextWrapper
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.Button
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ComposeUiFlags.isAdaptiveRefreshRateEnabled
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.dp

internal val isArrEnabled =
    @OptIn(ExperimentalComposeUiApi::class) isAdaptiveRefreshRateEnabled &&
        SDK_INT >= VANILLA_ICE_CREAM

@RequiresApi(VANILLA_ICE_CREAM)
internal inline fun <reified T> findOwner(context: Context): T? {
    var innerContext = context
    while (innerContext is ContextWrapper) {
        if (innerContext is T) {
            return innerContext
        }
        innerContext = innerContext.baseContext
    }
    return null
}

@Composable
fun MovableContentDemo() {
    if (isArrEnabled) {
        val context = LocalContext.current
        val activity: Activity? = findOwner(context)
        DisposableEffect(activity) {
            activity?.window?.frameRateBoostOnTouchEnabled = false
            onDispose { activity?.window?.frameRateBoostOnTouchEnabled = true }
        }
    }

    var isRow by remember { mutableStateOf(true) }

    val buttons = remember {
        movableContentOf {
            AlphaButton(30f)
            Spacer(Modifier.requiredSize(20.dp))
            AlphaButton(60f)
        }
    }

    Column(
        Modifier.height(300.dp).padding(20.dp).background(Color.Gray).fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = { isRow = !isRow }) { Text("toggle") }

        if (isRow) {
            Row(verticalAlignment = Alignment.CenterVertically) { buttons() }
        } else {
            Column(verticalArrangement = Arrangement.Center) { buttons() }
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
            text = "Click Me for $frameRate",
            color = LocalContentColor.current.copy(alpha = alpha), // Adjust text alpha
            modifier = Modifier.preferredFrameRate(frameRate),
        )
    }
}
