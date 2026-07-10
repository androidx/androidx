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

package androidx.compose.remote.integration.view.demos.examples

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

@SuppressLint("RestrictedApiAndroidX")
fun info(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 500),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Pressure Gauge"),
            RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
        ) {
            root {
                var density = rf(Rc.System.DENSITY) // DENSITY = 1 here
                addDebugMessage("  density = ", density.toFloat())

                // 2.6*18
                column(RecordingModifier().verticalScroll().fillMaxWidth()) {
                    canvas(
                        RecordingModifier()
                            .fillMaxWidth()
                            .height((density * 18f * 35f).toFloat())
                            .background(0xFFAABBCC.toInt())
                    ) {
                        addDebugMessage("  density = ", density.toFloat())

                        val centerX = windowWidth() / 2f + 100f
                        val cx = centerX.toFloat()
                        var line = rf(Rc.System.DENSITY) * 18f // DENSITY = 3 here

                        // var density = rf(Rc.System.DENSITY)
                        val font_size = rf(Rc.System.FONT_SIZE)
                        val fontScale = font_size / density / 14f

                        painter.setColor(Color.BLACK).setTextSize(line.toFloat()).commit()
                        var lineNo = 4f

                        printVar("System", (line * (lineNo++)).toFloat(), cx)
                        printVar(
                            "fontScale : ",
                            fontScale.toFloat(),
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar("DENSITY : ", Rc.System.DENSITY, (line * (lineNo++)).toFloat(), cx)
                        printVar(
                            "FONT_SIZE : ",
                            Rc.System.FONT_SIZE,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "API_LEVEL : ",
                            Rc.System.API_LEVEL,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "WINDOW_HEIGHT : ",
                            Rc.System.WINDOW_HEIGHT,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "WINDOW_HEIGHT : ",
                            Rc.System.WINDOW_WIDTH,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar("Time", (line * (lineNo++)).toFloat(), cx)

                        printVar(
                            "TIME_IN_SEC : ",
                            Rc.Time.TIME_IN_SEC,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "TIME_IN_MIN : ",
                            Rc.Time.TIME_IN_MIN,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "TIME_IN_HR : ",
                            Rc.Time.TIME_IN_HR,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "DAY_OF_YEAR : ",
                            Rc.Time.DAY_OF_YEAR,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "CALENDAR_MONTH : ",
                            Rc.Time.CALENDAR_MONTH,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar("WEEK_DAY : ", Rc.Time.WEEK_DAY, (line * (lineNo++)).toFloat(), cx)
                        printVar("YEAR : ", Rc.Time.YEAR, (line * (lineNo++)).toFloat(), cx)
                        printVar(
                            "OFFSET_TO_UTC : ",
                            Rc.Time.OFFSET_TO_UTC,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "CONTINUOUS_SEC : ",
                            Rc.Time.CONTINUOUS_SEC,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "ANIMATION_TIME : ",
                            Rc.Time.ANIMATION_TIME,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "ANIMATION_DELTA_TIME : ",
                            Rc.Time.ANIMATION_DELTA_TIME,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar("Sensors", (line * (lineNo++)).toFloat(), cx)
                        printVar(
                            "ACCELERATION_X : ",
                            Rc.Sensor.ACCELERATION_X,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "ACCELERATION_Y : ",
                            Rc.Sensor.ACCELERATION_Y,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "ACCELERATION_Z : ",
                            Rc.Sensor.ACCELERATION_Z,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "GYRO_ROT_X : ",
                            Rc.Sensor.GYRO_ROT_X,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "GYRO_ROT_Y : ",
                            Rc.Sensor.GYRO_ROT_Y,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "GYRO_ROT_Z : ",
                            Rc.Sensor.GYRO_ROT_Z,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "MAGNETIC_X : ",
                            Rc.Sensor.MAGNETIC_X,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "MAGNETIC_Y : ",
                            Rc.Sensor.MAGNETIC_Y,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar(
                            "MAGNETIC_Z : ",
                            Rc.Sensor.MAGNETIC_Z,
                            (line * (lineNo++)).toFloat(),
                            cx,
                        )
                        printVar("LIGHT : ", Rc.Sensor.LIGHT, (line * (lineNo++)).toFloat(), cx)
                    }
                }
            }
        }

    return rc.writer
}

@SuppressLint("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.printVar(
    varName: String,
    value: Float,
    line: Float,
    cx: Float,
) {
    val varValue = createTextFromFloat(value, 5, 5, Rc.TextFromFloat.PAD_PRE_NONE)
    val flags = Rc.TextAnchorMask.BASELINE_RELATIVE or Rc.TextAnchorMask.MONOSPACE_MEASURE
    drawTextAnchored(varName, cx, line, 1, 0, flags)
    drawTextAnchored(varValue, cx, line, -1, 0, flags)
}

@SuppressLint("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.printVar(varName: String, line: Float, cx: Float) {
    val flags = Rc.TextAnchorMask.BASELINE_RELATIVE or Rc.TextAnchorMask.MONOSPACE_MEASURE
    drawTextAnchored(varName, cx, line, 0, 0, flags)
}
