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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

class ProgressiveBlurActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var animateProgressiveBlur by remember { mutableStateOf(false) }

            val blurRadiusAnimate by
                animateFloatAsState(
                    targetValue = if (animateProgressiveBlur) 30f else 0f,
                    animationSpec = tween(durationMillis = 500),
                    label = "blurRadius",
                )

            Column(modifier = Modifier.padding(top = 100.dp)) {
                Box(
                    modifier =
                        Modifier.size(100.dp, 50.dp)
                            .background(Color.Gray)
                            .semantics { contentDescription = "toggle-animation" }
                            .clickable { animateProgressiveBlur = !animateProgressiveBlur },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(animateProgressiveBlur.toString(), color = Color.White)
                }

                Box(
                    modifier =
                        Modifier.size(300.dp)
                            .background(Brush.verticalGradient(listOf(Color.Red, Color.Blue)))
                            .blur {
                                radius =
                                    BlurRadiusSpec.verticalGradient(
                                        startRadius = 0.dp,
                                        endRadius = blurRadiusAnimate.dp,
                                    )
                            }
                )
            }
            launchIdlenessTracking()
        }
    }
}
