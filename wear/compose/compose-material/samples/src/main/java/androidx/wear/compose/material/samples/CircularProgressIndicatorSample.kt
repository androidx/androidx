/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ProgressIndicatorDefaults
import androidx.wear.compose.material.Text

@Sampled
@Composable
public fun IndeterminateCircularProgressIndicator() {
    CircularProgressIndicator()
}

@Sampled
@Composable
public fun CircularProgressIndicatorWithAnimation() {
    var progress by remember { mutableStateOf(0.1f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            progress = animatedProgress,
        )
        Spacer(Modifier.requiredHeight(10.dp))
        CompactChip(
            modifier = Modifier.width(90.dp),
            onClick = {
                if (progress < 1f) progress += 0.1f
            },
            label = { Text("Increase") }
        )
    }
}

@Sampled
@Composable
public fun CircularProgressIndicatorFullscreenWithGap() {
    val padding = if (LocalConfiguration.current.isScreenRound) 3.dp else 5.dp
    CircularProgressIndicator(
        modifier = Modifier.fillMaxSize().padding(all = padding),
        startAngle = 295.5f,
        endAngle = 245.5f,
        progress = 0.3f,
        strokeWidth = 5.dp
    )
}
