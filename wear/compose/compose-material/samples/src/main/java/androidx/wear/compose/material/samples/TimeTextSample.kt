/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.BasicCurvedText
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults

@OptIn(ExperimentalWearMaterialApi::class)
@Sampled
@Composable
fun TimeTextWithCustomSeparator() {
    TimeText(
        leadingLinearContent = {
            Text(
                text = "Leading content",
                style = TimeTextDefaults.timeTextStyle(color = Color.Green)
            )
        },
        leadingCurvedContent = {
            BasicCurvedText(
                text = "Leading content",
                style = TimeTextDefaults.timeCurvedTextStyle(color = Color.Green)
            )
        },
        trailingLinearContent = {
            Text(
                text = "Trailing content",
                style = TimeTextDefaults.timeTextStyle(color = Color.Yellow)
            )
        },
        trailingCurvedContent = {
            BasicCurvedText(
                text = "Trailing content",
                style = TimeTextDefaults.timeCurvedTextStyle(color = Color.Yellow)
            )
        },
        textLinearSeparator = {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = "***",
                style = TimeTextDefaults.timeTextStyle(color = Color.Magenta)
            )
        },
        textCurvedSeparator = {
            BasicCurvedText(
                text = "***",
                style = TimeTextDefaults.timeCurvedTextStyle(color = Color.Magenta),
                contentArcPadding = ArcPaddingValues(angular = 8.dp)
            )
        }
    )
}

@OptIn(ExperimentalWearMaterialApi::class)
@Sampled
@Composable
fun TimeTextWithFullDateAndTimeFormat() {
    TimeText(timeSource = TimeTextDefaults.timeSource("yyyy-MM-dd hh:mm"))
}