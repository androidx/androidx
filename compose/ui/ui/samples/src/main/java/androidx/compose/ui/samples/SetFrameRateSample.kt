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

package androidx.compose.ui.samples

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.Sampled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.dp

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

@Sampled
@Composable
fun SetFrameRateSample() {
    var targetAlpha by remember { mutableFloatStateOf(1f) }
    val context = LocalContext.current
    val activity: Activity? = findOwner(context)
    DisposableEffect(activity) {
        activity?.window?.frameRateBoostOnTouchEnabled = false
        onDispose { activity?.window?.frameRateBoostOnTouchEnabled = true }
    }

    val alpha by
        animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(durationMillis = 5000))

    Column(modifier = Modifier.size(300.dp)) {
        Button(
            onClick = { targetAlpha = if (targetAlpha == 1f) 0.2f else 1f },
            modifier =
                Modifier.testTag("frameRateTag")
                    .background(LocalContentColor.current.copy(alpha = alpha)),
        ) {
            Text(
                text = "Click Me for alpha change with 30 Hz frame rate",
                color = LocalContentColor.current.copy(alpha = alpha), // Adjust text alpha
                modifier = Modifier.preferredFrameRate(30f),
            )
        }
    }
}
