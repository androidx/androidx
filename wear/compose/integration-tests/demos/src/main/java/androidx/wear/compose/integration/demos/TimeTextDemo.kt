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

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import java.util.Locale

@Composable
fun TimeTextClockOnly() {
    TimeText()
}

@Composable
fun TimeTextWithLeadingText() {
    val textStyle = TimeTextDefaults.timeTextStyle(color = AlternatePrimaryColor1)
    TimeText(
        startLinearContent = { Text(text = "ETA 12:48", style = textStyle) },
        startCurvedContent = {
            basicCurvedText(text = "ETA 12:48", style = CurvedTextStyle(textStyle))
        }
    )
}

@Composable
fun TimeTextWithShadow() {
    val textStyle = TimeTextDefaults.timeTextStyle(color = AlternatePrimaryColor1)
    val radiusCoeff = 0.8f
    val linearGradientHeight = 30.dp
    val timeTextModifier =
        if (LocalConfiguration.current.isScreenRound) {
            Modifier.drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0.8f to Color.Transparent,
                        1.0f to Color.Black,
                        center = Offset(size.width / 2, size.height * radiusCoeff),
                        radius = size.height * radiusCoeff
                    )
                )
            }
        } else {
            Modifier.drawBehind {
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        start = Offset(x = size.width / 2, y = 0f),
                        end = Offset(x = size.width / 2, y = linearGradientHeight.toPx())
                    )
                )
            }
        }

    Box(modifier = Modifier.background(Color.Red)) {
        TimeText(
            modifier = timeTextModifier,
            startLinearContent = { Text(text = "ETA 12:48", style = textStyle) },
            startCurvedContent = {
                basicCurvedText(text = "ETA 12:48", style = CurvedTextStyle(textStyle))
            }
        )
    }
}

@Composable
fun TimeTextWithLocalisedFormat() {
    TimeText(
        timeSource =
            TimeTextDefaults.timeSource(
                DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy.MM.dd HH:mm")
            )
    )
}
