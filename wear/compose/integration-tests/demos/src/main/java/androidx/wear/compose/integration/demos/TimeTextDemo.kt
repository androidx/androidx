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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.BasicCurvedText
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun TimeTextClockOnly() {
    TimeText()
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun TimeTextWithLeadingText() {
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
        }
    )
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun TimeTextWithTrailingText() {
    TimeText(
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
        }
    )
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun TimeTextWithLeadingAndTrailingText() {
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
        }
    )
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun TimeTextWithPadding() {
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
        contentPadding = PaddingValues(8.dp)
    )
}
