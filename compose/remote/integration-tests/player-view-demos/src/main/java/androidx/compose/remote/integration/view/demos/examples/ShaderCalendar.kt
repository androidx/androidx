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

import android.graphics.Color
import android.graphics.Typeface
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.RootContentBehavior
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.Calendar

@Suppress("RestrictedApiAndroidX")
fun ShaderCalendar(): RemoteComposeContext {

    val font_size = 48f
    val line_size = font_size * 1.5f
    val block = (line_size) * 8.6f
    val tw = 1000.0f
    val th = (block * 11) - line_size
    val month = Calendar.getInstance().get(Calendar.MONTH)
    val doc =
        RemoteComposeContextAndroid(
            1000,
            th.toInt(),
            "Demo",
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_DEPRECATED,
            AndroidxRcPlatformServices(),
        ) {
            setRootContentBehavior(
                RootContentBehavior.SCROLL_VERTICAL,
                RootContentBehavior.ALIGNMENT_START + RootContentBehavior.ALIGNMENT_TOP,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FIT,
            )
            val id =
                createShader(WAVE_SHADER)
                    .setFloatUniform("iTime", TIME_IN_SEC)
                    .setFloatUniform("iResolution", 1000f, block)
                    .setIntUniform("iMonth", month)
                    .commit()
            painter.setShader(id).commit()
            drawRect(0f, 0f, tw, th * 2)
            painter.setShader(0).setColor(0x63FFFFFF).commit()

            for (i in 0..12) {
                val top = i * block + font_size / 2
                val bottom = top + block - font_size / 2
                drawRoundRect(0f, top, tw, bottom, 60f, 60f)
            }
            val xOffset = (tw - 2 * 7 * font_size) / 2
            for (i in 0..12) {
                painter
                    .setTypeface(3, 700, false)
                    .setColor(Color.BLACK)
                    .setTextSize(font_size)
                    .commit()

                var k = block * i + font_size * 2
                val lines = month(i)
                for (j in lines.indices) {
                    val line = lines[j]
                    if (j == 2) {

                        painter.setTypeface(Typeface.MONOSPACE).commit()
                    }
                    drawTextRun(line, 0, line.length, 0, line.length, xOffset, k, false)
                    k += line_size
                }
            }
        }
    println("   size ========== " + doc.buffer.buffer.size)
    return doc
}

fun month(monthOffset: Int): ArrayList<String> {
    val ret = ArrayList<String>()
    val gap = " "
    var line = ""
    val cal: Calendar = Calendar.getInstance()
    val t = cal.timeInMillis

    val fd = SimpleDateFormat("LLLL yyyy")
    // cal.add(Calendar.MONTH, monthOffset)
    cal.set(Calendar.MONTH, monthOffset)
    val calDate = fd.format(cal.time)
    val sp = (7 * (gap.length + 2) - calDate.length) / 2
    val pad = "                          ".substring(0, sp)
    ret.add(pad + calDate)
    val days = "SMTWTFS"
    for (i in days.indices) {
        line += gap + " " + days[i]
    }

    for (pos in 0..41) {
        val col = pos / 7
        val row = pos % 7
        if (row == 0) {
            ret.add(line)
            line = ""
        }
        cal.timeInMillis = t
        cal.add(Calendar.MONTH, monthOffset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val offset = cal.get(Calendar.DAY_OF_WEEK) - 1
        val lastDay: Int = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        line +=
            if (offset > pos || pos - offset >= lastDay) {
                "$gap  "
            } else {
                val num = pos - offset + 1
                gap + (if (num > 9) "" else " ") + num
            }
    }
    cal.timeInMillis = t
    ret.add(line)
    return ret
}

val COLOR_SHADER_SRC =
    "        uniform float2 iResolution;\n" +
        "        uniform float iTime;\n" +
        "        half4 main(vec2 fragcoord) { \n" +
        "            vec2 uv = (.53 - fragcoord.xy / iResolution.y) * 16;\n" +
        "            vec3 color = vec3(0.9, 0.9, 0.9);\n" +
        "\n" +
        "            float size =  0.3 * sin(length(floor(uv) * 0.5) - iTime);\n" +
        "            color.rgb += smoothstep(0.1, 0.0, length(fract(uv) - 0.5) - size);\n" +
        "\t        color.g *= 0.9;\n" +
        "            color.rg *= 0.9;\n" +
        "            color.b *= 0.8;\n" +
        "            return vec4(color, 1.0);\n" +
        "        }"

val WAVE_SHADER =
    """
// modified version of https://www.shadertoy.com/view/ltGSWD

uniform float2 iResolution;
uniform float iTime;
uniform int iMonth;
                    
float gradient(float p)
{
    vec2 pt0 = vec2(0.00,0.0);
    vec2 pt1 = vec2(0.86,0.1);
    vec2 pt2 = vec2(0.955,0.40);
    vec2 pt3 = vec2(0.99,1.0);
    vec2 pt4 = vec2(1.00,0.0);
    if (p < pt0.x) return pt0.y;
    if (p < pt1.x) return mix(pt0.y, pt1.y, (p-pt0.x) / (pt1.x-pt0.x));
    if (p < pt2.x) return mix(pt1.y, pt2.y, (p-pt1.x) / (pt2.x-pt1.x));
    if (p < pt3.x) return mix(pt2.y, pt3.y, (p-pt2.x) / (pt3.x-pt2.x));
    if (p < pt4.x) return mix(pt3.y, pt4.y, (p-pt3.x) / (pt4.x-pt3.x));
    return pt4.y;
}

float waveN(vec2 uv, vec2 s12, vec2 t12, vec2 f12, vec2 h12)
{
    vec2 x12 = sin((iTime * s12 + t12 + uv.x) * f12) * h12;
    float g = gradient(uv.y / (0.5 + x12.x + x12.y));
	return g * 0.27;
}

float wave1(vec2 uv)
{
    return waveN(vec2(uv.x,uv.y-0.25), vec2(0.03,0.06), vec2(0.00,0.02), vec2(8.0,3.7), vec2(0.06,0.05));
}

float wave2(vec2 uv)
{
    return waveN(vec2(uv.x,uv.y-0.25), vec2(0.04,0.07), vec2(0.16,-0.37), vec2(6.7,2.89), vec2(0.06,0.05));
}

float wave3(vec2 uv)
{
    return waveN(vec2(uv.x,0.75-uv.y), vec2(0.035,0.055), vec2(-0.09,0.27), vec2(7.4,2.51), vec2(0.06,0.05));
}

float wave4(vec2 uv)
{
    return waveN(vec2(uv.x,0.75-uv.y), vec2(0.032,0.09), vec2(0.08,-0.22), vec2(6.5,3.89), vec2(0.06,0.05));
}

half4 main(vec2 fragCoord) { 
    vec2 uv = fragCoord.xy / iResolution.xy;
    float month = float(iMonth);
    float pos  = (uv.y - month) * 10 ;
    uv.y = mod(uv.y, 1);
    float waves = wave1(uv) + wave2(uv) + wave3(uv) + wave4(uv);
    
	float x = uv.x;
	float y = abs(uv.y*2-1.0);
    y = 1 - y * y;
    float con = - pos /(1 + abs(pos));
    con = 0.5 * con * con * con;
    float sat = 0.3;
    vec3 base = vec3( sat+con,sat, sat-con);
    vec3 bg = mix(vec3(0.05, 0.05, 0.3), base, (x + y) * 0.55);
    vec3 ac = bg + vec3(1.0, 1.0, 1.0) * waves;

    return vec4(ac, 1.0);
} 
"""

@Preview @Composable fun ShaderCalendarPreview() = RemoteDocPreview(ShaderCalendar())
