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

package androidx.compose.remote.integration.view.demos.examples

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Suppress("RestrictedApiAndroidX")
@Composable
fun CanvasCalendarMonth(modifier: RemoteModifier = RemoteModifier, month: Int = 0) {

    val numDays = arrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val startDays = arrayOf(31, 28, 25, 31, 28, 26, 30, 28, 1, 29, 27, 1)

    val days = numDays[month]
    val start = startDays[month]
    val lastDays = numDays[(month + 11) % 12]
    val monthNames =
        arrayOf(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December",
        )
    val dayNames = arrayOf("S", "M", "T", "W", "T", "F", "S")
    val daysValue = IntArray(7 * 6)
    for (i in 0 until 42) {
        var number = i + start
        if (start > 1) {
            if (number > lastDays + days) {
                number -= lastDays + days
            } else if (number > lastDays) {
                number -= lastDays
            }
        } else if (number > days) {
            number -= days
        }
        daysValue[i] = number
    }
    RemoteColumn(
        modifier =
            modifier.clip(RemoteRoundedCornerShape(18.rdp)).background(Color(3, 169, 244, 173)),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
    ) {
        RemoteText(
            monthNames[month],
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 32.rsp,
            modifier = RemoteModifier.padding(bottom = 24.dp),
        )
        RemoteRow(modifier = RemoteModifier.height(IntrinsicSize.Min)) {
            var done = false
            for (j in 0 until 7) {
                if (j == 1 || j == 6) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxHeight().width(1.rdp).background(Color.DarkGray)
                    )
                }
                var modifier = RemoteModifier.padding(left = 8.rf, right = 8.rf)
                RemoteColumn(modifier = modifier, horizontalAlignment = RemoteAlignment.End) {
                    RemoteCanvas(modifier = RemoteModifier.size(20.rdp)) {
                        drawAnchoredText(
                            dayNames[j].rs,
                            40f.rf,
                            20f.rf,
                            1f.rf,
                            0f.rf,
                            paint =
                                RemotePaint().apply {
                                    color = Color.White.rc
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                },
                        )
                    }
                    for (i in 0 until 6) {
                        val index = j + i * 7
                        var number = index + start
                        var stage = 1
                        if (start > 1) {
                            stage = 0
                            if (number > lastDays + days) {
                                number -= lastDays + days
                                stage = 2
                            } else if (number > lastDays) {
                                number -= lastDays
                                stage = 1
                            }
                        } else if (number > days) {
                            number -= days
                            stage = 2
                        }
                        if (stage == 2 && i == 5 && j == 0) {
                            done = true
                        }
                        if (done && i == 5) {
                            continue
                        }
                        if (stage == 0 || stage == 2) {
                            RemoteCanvas(modifier = RemoteModifier.size(20.rdp)) {
                                drawAnchoredText(
                                    "$number".rs,
                                    40f.rf,
                                    20f.rf,
                                    1f.rf,
                                    0f.rf,
                                    paint = RemotePaint().apply { color = Color.White.rc },
                                )
                            }
                        } else {
                            RemoteCanvas(modifier = RemoteModifier.size(20.rdp)) {
                                drawAnchoredText(
                                    "$number".rs,
                                    40f.rf,
                                    20f.rf,
                                    1f.rf,
                                    0f.rf,
                                    paint = RemotePaint().apply { color = Color.Black.rc },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun ScrollViewDemo() {
    val numElements = 12
    val scrollState = rememberRemoteScrollState(evenNotches = numElements)
    val dimensionCard = 280.rdp
    RemoteBox(modifier = RemoteModifier, contentAlignment = RemoteAlignment.BottomEnd) {
        val height = dimensionCard.toPx()
        val h2 = 280.rdp
        RemoteColumn(
            modifier =
                RemoteModifier.fillMaxWidth()
                    .height(h2)
                    .clip(RemoteRectangleShape)
                    // .background(Color.LightGray)
                    .verticalScroll(scrollState),
            verticalArrangement = RemoteArrangement.Center,
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            for (i in 0 until numElements) {
                val scale =
                    0.8f.rf +
                        (1.rf - abs(scrollState.positionState - (height * i.toFloat())) / height) *
                            0.2f
                val rotation =
                    (abs(scrollState.positionState - (height * i.toFloat())) / height) * 40f
                //                Box(horizontalAlignment = Alignment.End) {
                CanvasCalendarMonth(
                    modifier =
                        RemoteModifier.graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                rotationX = rotation,
                            )
                            .width(h2)
                            .height(h2)
                            .padding(0.dp)
                    //    .background(Color.White)
                    ,
                    i,
                )
                //                    val value = rememberRemoteString(scrollState.position.rf)
                //                    RemoteText(value, fontSize = 34.sp, color = Color.Blue)
                //                }
            }
        }
        val debug = false
        if (debug) {
            RemoteColumn(
                verticalArrangement = RemoteArrangement.Center,
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                val blue = RemoteColor(Color.Blue.toArgb())
                RemoteText(
                    scrollState.positionState.toRemoteString(5),
                    fontSize = 34.rsp,
                    color = blue,
                )
                RemoteText(height.toRemoteString(5), fontSize = 34.rsp, color = blue)
            }
        }
        //            val value = rememberRemoteString(RemoteFloat(scrollState.position))
        //            RemoteText(value, fontSize = 34.sp, color = Color.Blue)
    }
}

@Preview
@Composable
private fun CanvasCalendarMonthPreview() = RemotePreview { CanvasCalendarMonth() }

@Preview @Composable private fun ScrollViewDemoPreview() = RemotePreview { ScrollViewDemo() }
