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

import android.text.format.DateFormat
import androidx.annotation.Sampled
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material.CurvedText
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import java.util.Locale

@OptIn(ExperimentalWearMaterialApi::class)
@Sampled
@Composable
fun TimeTextWithCustomSeparator() {
    val leadingTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Green)
    val trailingTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Yellow)
    val separatorTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Magenta)
    TimeText(
        leadingLinearContent = {
            Text(
                text = "Leading content",
                style = leadingTextStyle
            )
        },
        leadingCurvedContent = {
            CurvedText(
                text = "Leading content",
                style = CurvedTextStyle(leadingTextStyle)
            )
        },
        trailingLinearContent = {
            Text(
                text = "Trailing content",
                style = trailingTextStyle
            )
        },
        trailingCurvedContent = {
            CurvedText(
                text = "Trailing content",
                style = CurvedTextStyle(trailingTextStyle)
            )
        },
        textLinearSeparator = {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = "***",
                style = separatorTextStyle
            )
        },
        textCurvedSeparator = {
            CurvedText(
                text = "***",
                style = CurvedTextStyle(separatorTextStyle),
                contentArcPadding = ArcPaddingValues(angular = 8.dp)
            )
        }
    )
}

@OptIn(ExperimentalWearMaterialApi::class)
@Sampled
@Composable
fun TimeTextWithFullDateAndTimeFormat() {
    TimeText(
        timeSource = TimeTextDefaults.timeSource(
            DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MM-dd hh:mm")
        )
    )
}