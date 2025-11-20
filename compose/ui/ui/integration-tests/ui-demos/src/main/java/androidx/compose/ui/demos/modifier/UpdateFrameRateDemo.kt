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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UpdateFrameRateDemo() {
    if (isArrEnabled) {
        val context = LocalContext.current
        val activity: Activity? = findOwner(context)
        DisposableEffect(activity) {
            activity?.window?.frameRateBoostOnTouchEnabled = false
            onDispose { activity?.window?.frameRateBoostOnTouchEnabled = true }
        }
    }

    var box1Color by remember { mutableStateOf(Color.Blue) }
    var box2Color by remember { mutableStateOf(Color.Blue) }
    var box1FrameRate by remember { mutableFloatStateOf(60f) }
    var box1Alpha by remember { mutableFloatStateOf(0.5f) } // State for box 1 alpha
    var box2Alpha by remember { mutableFloatStateOf(0.5f) } // State for box 2 alpha
    var isBox2frameRateEnabled by remember {
        mutableStateOf(true)
    } // State to track if alpha is applied

    val animatedBox1Color by
        animateColorAsState(
            targetValue = box1Color,
            animationSpec = tween(5000), // 5 seconds animation
        )

    val animatedBox2Color by
        animateColorAsState(
            targetValue = box2Color,
            animationSpec = tween(5000), // 5 seconds animation
        )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    box1Alpha = if (box1Alpha == 0.5f) 1.0f else 0.5f // Toggle alpha for box 1
                    box1FrameRate = if (box1FrameRate == 120f) 60f else 120f
                    box1Color = if (box1Color == Color.Blue) Color.Red else Color.Blue
                },
                modifier = Modifier.testTag("button1"),
            ) {
                Text("Box 1")
            }
            Button(
                onClick = {
                    box2Alpha = if (box2Alpha == 0.5f) 1.0f else 0.5f // Toggle alpha for box 2
                    isBox2frameRateEnabled = if (isBox2frameRateEnabled) false else true
                    box2Color = if (box2Color == Color.Blue) Color.Red else Color.Blue
                },
                modifier = Modifier.testTag("button2"),
            ) {
                Text("Box 2")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier =
                Modifier.alpha(box1Alpha) // Use the state variable for alpha
                    .preferredFrameRate(box1FrameRate) // Use the state variable for frame rate
                    .width(250.dp)
                    .height(200.dp)
                    .background(animatedBox1Color)
        ) {
            Text("frame rate: $box1FrameRate", fontSize = 30.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier =
                Modifier.alpha(box2Alpha) // Use the state variable for alpha
                    .then(
                        if (isBox2frameRateEnabled) Modifier.preferredFrameRate(120f) else Modifier
                    )
                    .width(250.dp)
                    .height(220.dp)
                    .background(animatedBox2Color)
        ) {
            Text(
                "frame rate: ${if (isBox2frameRateEnabled) 120f else 60f}",
                fontSize = 30.sp,
                color = Color.White,
            )
        }
    }
}
