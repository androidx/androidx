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
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun MovingContentDemo() {
    if (isArrEnabled) {
        val context = LocalContext.current
        val activity: Activity? = findOwner(context)
        DisposableEffect(activity) {
            activity?.window?.frameRateBoostOnTouchEnabled = false
            onDispose { activity?.window?.frameRateBoostOnTouchEnabled = true }
        }
    }

    val shortText = "Change position"
    var moved by remember { mutableStateOf(false) }
    // Animate Dp values (in this case, the offset)
    val offset by
        animateIntAsState(
            targetValue = if (moved) 100 else 0,
            animationSpec = tween(durationMillis = 5000),
            label = "offset",
        )

    Column(
        Modifier.height(250.dp).padding(20.dp).background(Color.Gray).fillMaxWidth().padding(20.dp)
    ) {
        Button(
            onClick = { moved = !moved },
            modifier = Modifier.width(500.dp).testTag("frameRateTag"),
        ) {
            Text(
                shortText,
                modifier = Modifier.preferredFrameRate(30f).offset { IntOffset(x = offset, y = 0) },
            )
        }
    }
}
