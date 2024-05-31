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
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.angularSize
import androidx.wear.compose.foundation.curvedColumn
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.sizeIn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import androidx.wear.compose.material.curvedText
import java.util.Locale

@Sampled
@Composable
fun TimeTextWithStatus() {
    val leadingTextStyle = TimeTextDefaults.timeTextStyle(color = MaterialTheme.colors.primary)

    TimeText(
        startLinearContent = { Text(text = "ETA 12:48", style = leadingTextStyle) },
        startCurvedContent = {
            curvedText(text = "ETA 12:48", style = CurvedTextStyle(leadingTextStyle))
        },
    )
}

@Sampled
@Composable
fun TimeTextWithFullDateAndTimeFormat() {
    TimeText(
        timeSource =
            TimeTextDefaults.timeSource(
                DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MM-dd hh:mm")
            )
    )
}

@Sampled
@Composable
fun TimeTextAnimation() {
    var showState by remember { mutableStateOf(false) }
    val showTransition = updateTransition(showState)

    val time = 350
    val animatedColor by
        showTransition.animateColor(
            label = "animatedColor",
            transitionSpec = {
                tween(
                    time,
                    easing =
                        when {
                            false isTransitioningTo true ->
                                // Fade In
                                CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
                            else ->
                                // Fade Out
                                CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
                        }
                )
            }
        ) { state ->
            when (state) {
                true -> Color.White
                false -> Color.Transparent
            }
        }
    val animateSize by
        showTransition.animateFloat(
            label = "animatedSize",
            transitionSpec = { tween(time, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)) }
        ) { state ->
            when (state) {
                true -> 1f
                false -> 0f
            }
        }

    val text = "Long text to animate"

    val curvedSeparatorSweep = 10f
    val curvedTextSweep = 80f
    val curvedAnimatedSweep = animateSize * (curvedSeparatorSweep + curvedTextSweep)
    val curvedSeparatorGap = curvedAnimatedSweep.coerceAtMost(curvedSeparatorSweep) / 2f

    val linearSeparatorSize = 10.dp
    val linearTextSize = 70.dp
    val linearAnimatedSize = animateSize * (linearSeparatorSize + linearTextSize)
    val linearSeparatorGap = linearAnimatedSize.coerceAtMost(linearSeparatorSize) / 2f

    val textStyle =
        TimeTextDefaults.timeTextStyle().copy(fontWeight = FontWeight.Normal, color = animatedColor)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TimeText(
            // Curved
            textCurvedSeparator = {
                curvedColumn(modifier = CurvedModifier.angularSize(curvedSeparatorGap)) {}
                curvedText("·", style = CurvedTextStyle(textStyle))
                curvedColumn(modifier = CurvedModifier.angularSize(curvedSeparatorGap)) {}
            },
            startCurvedContent = {
                curvedRow(
                    modifier =
                        CurvedModifier.sizeIn(
                            maxSweepDegrees =
                                (curvedAnimatedSweep - curvedSeparatorSweep).coerceAtLeast(0f)
                        )
                ) {
                    curvedText(
                        text,
                        CurvedModifier.sizeIn(maxSweepDegrees = curvedTextSweep),
                        style = CurvedTextStyle(textStyle),
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            // Linear
            textLinearSeparator = {
                Spacer(modifier = Modifier.width(linearSeparatorGap))
                Text("·", style = textStyle)
                Spacer(modifier = Modifier.width(linearSeparatorGap))
            },
            startLinearContent = {
                Box(
                    modifier =
                        Modifier.clipToBounds()
                            .widthIn(
                                max = (linearAnimatedSize - linearSeparatorSize).coerceAtLeast(0.dp)
                            )
                ) {
                    Text(
                        text,
                        maxLines = 1,
                        style = textStyle,
                        modifier =
                            Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true)
                                .widthIn(max = linearTextSize),
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
        )
        Button(onClick = { showState = !showState }) { Text("Go!") }
    }
}
