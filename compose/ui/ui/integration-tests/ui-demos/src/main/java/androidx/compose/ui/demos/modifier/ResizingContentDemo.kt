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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResizingContentDemo() {
    if (isArrEnabled) {
        val context = LocalContext.current
        val activity: Activity? = findOwner(context)
        DisposableEffect(activity) {
            activity?.window?.frameRateBoostOnTouchEnabled = false
            onDispose { activity?.window?.frameRateBoostOnTouchEnabled = true }
        }
    }

    Column(
        Modifier.height(250.dp).padding(20.dp).background(Color.Gray).fillMaxWidth().padding(20.dp)
    ) {
        Text("Text - text change animation", fontSize = 20.sp, color = Color.White)
        Spacer(Modifier.requiredHeight(20.dp))
        ResizingButtons(30f)
    }
}

@Composable
fun ResizingButtons(frameRate: Float) {
    var expanded by remember { mutableStateOf(false) }
    val size by
        animateDpAsState(
            targetValue = if (expanded) 300.dp else 200.dp,
            animationSpec = tween(durationMillis = 5000),
        )

    Button(
        onClick = { expanded = !expanded },
        modifier = Modifier.testTag("ContentResizing").preferredFrameRate(frameRate).width(size),
    ) {
        Text("Click Me for size change")
    }
}
