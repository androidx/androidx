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

import android.graphics.Paint
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.rem
import androidx.compose.remote.creation.rf
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders the current weekday name, day-of-month and HH:MM:SS as live text into an offscreen
 * bitmap, then texture-maps that bitmap onto a 3D sphere via an AGSL shader. The sphere rotates
 * once per 24h so the label sweeps around the front face.
 */
@Suppress("RestrictedApiAndroidX")
fun timeSphere(): RemoteComposeContext {
    val tw = 800f
    val th = 800f
    val texW = 1024
    val texH = 384

    return RemoteComposeContextAndroid(
        tw.toInt(),
        th.toInt(),
        "Time Sphere",
        apiLevel = 7,
        profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        platform = AndroidxRcPlatformServices(),
    ) {
        root {
            box(Modifier.fillMaxSize()) {
                canvas(Modifier.fillMaxSize()) {
                    val texId = writer.createBitmap(texW, texH)

                    // Day-of-week 1..7 (1 = Monday) → string-list lookup with 0..6 index.
                    val dayList = addStringList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val dayIdx = (rf(RemoteContext.FLOAT_WEEK_DAY) - 1f).toFloat()
                    val dayName = textLookup(dayList, dayIdx)

                    // Day-of-month: 1..31, no padding.
                    val dayOfMonth =
                        createTextFromFloat(
                            RemoteContext.FLOAT_DAY_OF_MONTH,
                            2,
                            0,
                            TextFromFloat.PAD_AFTER_NONE or TextFromFloat.PAD_PRE_NONE,
                        )

                    // HH:MM:SS — minute and second need %60 since the system values are
                    // minutes-from-midnight (0..1439) and seconds-of-hour (0..3599).
                    val hourId =
                        createTextFromFloat(
                            RemoteContext.FLOAT_TIME_IN_HR,
                            2,
                            0,
                            TextFromFloat.PAD_AFTER_NONE or TextFromFloat.PAD_PRE_ZERO,
                        )
                    val minuteVal = (rf(RemoteContext.FLOAT_TIME_IN_MIN) % 60f).toFloat()
                    val minId =
                        createTextFromFloat(
                            minuteVal,
                            2,
                            0,
                            TextFromFloat.PAD_AFTER_NONE or TextFromFloat.PAD_PRE_ZERO,
                        )
                    val secondVal = (rf(RemoteContext.FLOAT_TIME_IN_SEC) % 60f).toFloat()
                    val secId =
                        createTextFromFloat(
                            secondVal,
                            2,
                            0,
                            TextFromFloat.PAD_AFTER_NONE or TextFromFloat.PAD_PRE_ZERO,
                        )
                    val colon = textCreateId(":")
                    val timeText =
                        textMerge(
                            hourId,
                            textMerge(colon, textMerge(minId, textMerge(colon, secId))),
                        )

                    // Render text into the offscreen bitmap.
                    writer.drawOnBitmap(texId, 0, 0xFF181028.toInt())
                    painter
                        .setColor(0xFFE8DCFF.toInt())
                        .setStyle(Paint.Style.FILL)
                        .setTextSize(72f)
                        .setTypeface(3, 700, false)
                        .commit()
                    drawTextAnchored(dayName, texW / 2f, 70f, 0f, 0f, 0)

                    painter.setColor(0xFFFFC85A.toInt()).setTextSize(160f).commit()
                    drawTextAnchored(dayOfMonth, texW / 2f, 200f, 0f, 0f, 0)

                    painter.setColor(0xFFAEE6FF.toInt()).setTextSize(72f).commit()
                    drawTextAnchored(timeText, texW / 2f, 340f, 0f, 0f, 0)
                    writer.drawOnBitmap(0)

                    // Build the sphere shader and sample the text bitmap from it.
                    painter.setShader(0).setColor(0xFF000000.toInt()).commit()
                    val sid =
                        createShader(TIME_SPHERE_SHADER)
                            .setFloatUniform("iResolution", tw, th)
                            .setFloatUniform("iTexResolution", texW.toFloat(), texH.toFloat())
                            .setFloatUniform("iTime", RemoteContext.FLOAT_CONTINUOUS_SEC)
                            .setFloatUniform("iHour", RemoteContext.FLOAT_TIME_IN_HR)
                            .setFloatUniform("iMinute", RemoteContext.FLOAT_TIME_IN_MIN)
                            .setFloatUniform("iSecond", RemoteContext.FLOAT_TIME_IN_SEC)
                            .setFloatUniform("iDayOfWeek", RemoteContext.FLOAT_WEEK_DAY)
                            .setBitmapUniform("textTex", texId)
                            .commit()
                    painter.setShader(sid).commit()
                    drawRect(0f, 0f, tw, th)
                }
            }
        }
    }
}

@Preview @Composable private fun TimeSpherePreview() = RemoteDocumentPreview(timeSphere())

private const val TIME_SPHERE_SHADER =
    """
uniform shader textTex;
uniform float2 iResolution;
uniform float2 iTexResolution;
uniform float iTime;
uniform float iHour;
uniform float iMinute;
uniform float iSecond;
uniform float iDayOfWeek;

const float PI = 3.14159265359;

mat3 rotY(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat3(c, 0.0, -s, 0.0, 1.0, 0.0, s, 0.0, c);
}

mat3 rotX(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

vec3 dayHue(int d) {
    if (d == 0) return vec3(0.95, 0.30, 0.35);
    if (d == 1) return vec3(0.95, 0.60, 0.20);
    if (d == 2) return vec3(0.95, 0.90, 0.30);
    if (d == 3) return vec3(0.40, 0.85, 0.40);
    if (d == 4) return vec3(0.30, 0.70, 0.95);
    if (d == 5) return vec3(0.55, 0.45, 0.95);
    return vec3(0.90, 0.45, 0.85);
}

half4 main(vec2 fragcoord) {
    vec2 center = iResolution.xy * 0.5;
    float radius = min(iResolution.x, iResolution.y) * 0.45;
    vec2 p = (fragcoord - center) / radius;
    p.y = -p.y;
    float r2 = dot(p, p);

    if (r2 > 1.0) {
        float d = sqrt(r2) - 1.0;
        float fall = exp(-d * 4.0);
        vec3 bg = mix(vec3(0.02, 0.02, 0.06), vec3(0.08, 0.05, 0.18), fall);
        return vec4(bg, 1.0);
    }

    float z = sqrt(1.0 - r2);
    vec3 nView = vec3(p.x, p.y, z);

    float subSec = fract(iTime);
    float smoothHour = iHour + iMinute / 60.0 + (iSecond + subSec) / 3600.0;

    float yaw = smoothHour / 24.0 * 2.0 * PI;
    float tilt = (iDayOfWeek - 4.0) * 0.1 + 0.4;
    vec3 surf = rotX(tilt) * rotY(yaw) * nView;

    float lat = asin(clamp(surf.y, -1.0, 1.0));
    float lon = atan(surf.z, surf.x);
    float u = lon / (2.0 * PI) + 0.5;
    float v = lat / PI + 0.5;

    // Base globe colour and grid lines.
    int dayIdx = int(clamp(iDayOfWeek - 1.0, 0.0, 6.0));
    vec3 base = dayHue(dayIdx) * (0.75 + 0.25 * cos(lat * 6.0));
    float lonGrid = abs(fract(lon / (PI / 12.0)) - 0.5);
    float latGrid = abs(fract((lat + PI * 0.5) / (PI / 18.0)) - 0.5);
    float grid = smoothstep(0.45, 0.49, max(lonGrid, latGrid));

    // Sample the text bitmap. Coordinates are in texture pixels.
    vec2 texUv = vec2(u * iTexResolution.x, v * iTexResolution.y);
    vec4 texSample = textTex.eval(texUv);

    // Phong lighting in view space.
    vec3 lightDir = normalize(vec3(-0.5, 0.7, 0.6));
    float diff = max(0.0, dot(nView, lightDir));
    vec3 viewDir = vec3(0.0, 0.0, 1.0);
    vec3 halfDir = normalize(lightDir + viewDir);
    float spec = pow(max(0.0, dot(nView, halfDir)), 28.0);

    vec3 color = base * (0.30 + 0.70 * diff);
    color += vec3(0.55, 0.55, 0.65) * spec * 0.55;
    color = mix(color, vec3(1.0), grid * 0.30);

    // Blend the text overlay using its perceived luminance as the alpha — the bitmap is on a
    // dark background, so this lets only the glyphs come through and keeps them legible against
    // the lit sphere.
    float textAlpha = clamp(dot(texSample.rgb, vec3(0.299, 0.587, 0.114)) - 0.15, 0.0, 1.0);
    textAlpha *= 1.2;
    textAlpha = clamp(textAlpha, 0.0, 1.0);
    color = mix(color, texSample.rgb, textAlpha * 0.85);

    // Edge darkening for sphere depth.
    color *= 0.55 + 0.45 * z;

    return vec4(color, 1.0);
}
"""
