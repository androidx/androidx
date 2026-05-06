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

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

@Suppress("RestrictedApiAndroidX")
fun sysVar(): RemoteComposeWriter {

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Activity Rings"),
        ) {
            var rad = backgroundRadius()
            var innerRad = innerRadius()
            var fontWeight = fontWeight()

            addDebugMessage("backgroundRadius ", rad)
            addDebugMessage("innerRadius", innerRad)
            addDebugMessage("fontWeight", fontWeight)
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFFAABBCCL.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val cx = w / 2f
                    val cy = h / 2f
                    painter.setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit()
                    drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), rad, rad)
                    painter.setColor(Color.LTGRAY).setStyle(Paint.Style.FILL).commit()
                    drawRoundRect(100f, 100f, (w - 100f).toFloat(), (h - 100f).toFloat(), rad, rad)
                    painter.setColor(Color.BLACK).setTextSize(55f).commit()
                    print(cx, cy, -4f, "backgroundRadius ", rad)
                    print(cx, cy, 0f, "innerRadius ", innerRad)
                    print(cx, cy, 4f, "fontWeight ", fontWeight)
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.print(
    x: RFloat,
    y: RFloat,
    line: Float,
    str: String,
    value: RFloat,
) {
    val v = createTextFromFloat(value, 7, 1, 0)
    val txt = textMerge(addText(str + ": "), v)
    drawTextAnchored(txt, x, y, 0, line, 0)
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.backgroundRadius(): RFloat {
    val v = addNamedFloat("system.system_app_widget_background_radius", 32f)
    return RFloat(writer, v)
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.innerRadius(): RFloat {
    val v = addNamedFloat("system.system_app_widget_inner_radius", 25f)
    return RFloat(writer, v)
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.fontWeight(): RFloat {
    val v = addNamedFloat("system.font_weight", 0f)
    return RFloat(writer, v)
}
