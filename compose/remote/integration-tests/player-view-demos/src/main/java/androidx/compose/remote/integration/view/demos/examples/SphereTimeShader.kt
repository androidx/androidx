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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Generates a 512x256 bitmap acting as our offscreen buffer, containing the current Day, Date, and
 * Time formatted beautifully inside a futuristic hud-style card.
 */
fun createTimeBitmap(): Bitmap {
    val width = 512
    val height = 256
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Clear with a deep dark indigo space background
    canvas.drawColor(Color.rgb(15, 15, 35))

    val paint =
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

    // Fetch actual current day, date, time
    val calendar = Calendar.getInstance()
    val dayFormat = SimpleDateFormat("EEEE", Locale.US)
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    val dayStr = dayFormat.format(calendar.time)
    val dateStr = dateFormat.format(calendar.time)
    val timeStr = timeFormat.format(calendar.time)

    // Draw border and separation grid/lines in futuristic neon-cyan
    paint.color = Color.argb(80, 0, 255, 200)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawRect(10f, 10f, width - 10f, height - 10f, paint)

    canvas.drawLine(10f, height * 0.33f, width - 10f, height * 0.33f, paint)
    canvas.drawLine(10f, height * 0.66f, width - 10f, height * 0.66f, paint)

    // Draw Day (Top Section)
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(0, 255, 255) // Cyan
    paint.textSize = 36f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    canvas.drawText(dayStr.uppercase(Locale.US), width / 2f, height * 0.22f, paint)

    // Draw Date (Middle Section)
    paint.color = Color.rgb(255, 255, 255) // White
    paint.textSize = 28f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    canvas.drawText(dateStr, width / 2f, height * 0.53f, paint)

    // Draw Time (Bottom Section)
    paint.color = Color.rgb(255, 200, 0) // Gold
    paint.textSize = 42f
    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    canvas.drawText(timeStr, width / 2f, height * 0.86f, paint)

    return bitmap
}

val SPHERE_TIME_SHADER_SRC =
    """
    uniform shader myImage;
    uniform float2 iResolution;
    uniform float iTime;

    half4 main(vec2 fragcoord) {
        // Center UV coordinates around the center, scaled by resolution height
        vec2 uv = (fragcoord.xy - 0.5 * iResolution.xy) / iResolution.y;
        
        // Ray definition
        vec3 ro = vec3(0.0, 0.0, -2.2);
        vec3 rd = normalize(vec3(uv, 1.0));
        
        // Ray-sphere intersection
        float R = 0.85;
        float b = dot(ro, rd);
        float c = dot(ro, ro) - R * R;
        float h = b * b - c;
        
        vec3 color;
        
        if (h >= 0.0) {
            float t = -b - sqrt(h);
            vec3 p = ro + t * rd;
            vec3 N = normalize(p);
            
            // Spin sphere on the Y axis
            float angle = iTime * 0.35;
            float cosA = cos(angle);
            float sinA = sin(angle);
            vec3 rotatedN = vec3(
                N.x * cosA - N.z * sinA,
                N.y,
                N.x * sinA + N.z * cosA
            );
            
            // Equirectangular mapping to spherical coordinates
            float u = 0.5 + atan(rotatedN.z, rotatedN.x) / (2.0 * 3.14159265);
            float v = 0.5 + asin(rotatedN.y) / 3.14159265;
            
            // texture coordinates based on the 512x256 size
            vec2 texCoord = vec2(u * 512.0, v * 256.0);
            vec4 texColor = myImage.eval(texCoord);
            
            // Sleek studio lighting setup
            vec3 L = normalize(vec3(1.0, 1.0, -1.5));
            float diff = max(0.0, dot(N, L));
            
            // Specular reflection highlight
            vec3 V = -rd;
            vec3 H = normalize(L + V);
            float spec = pow(max(0.0, dot(N, H)), 24.0);
            
            // Soft edge fresnel glow
            float fresnel = pow(1.0 - max(0.0, dot(N, V)), 4.0);
            
            color = texColor.rgb * (diff * 0.8 + 0.35) + vec3(0.4) * spec + vec3(0.0, 0.6, 0.6) * fresnel * 0.4;
        } else {
            // Animated cosmos background with stellar waves
            vec2 bgUv = fragcoord.xy / iResolution.xy;
            vec3 bgGradient = mix(vec3(0.01, 0.01, 0.03), vec3(0.04, 0.06, 0.12), bgUv.y);
            
            float wave = sin(uv.x * 4.0 + iTime * 0.15) * cos(uv.y * 4.0 - iTime * 0.1);
            bgGradient += vec3(0.0, 0.015, 0.03) * (wave + 1.0);
            
            color = bgGradient;
        }
        
        return vec4(color, 1.0);
    }
    """
        .trimIndent()

@Suppress("RestrictedApiAndroidX")
fun sphereTimeShader(): RemoteComposeContext {
    val tw = 800f
    val th = 800f
    return RemoteComposeContextAndroid(
        800,
        800,
        "SphereTimeShader",
        apiLevel = 7,
        profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        platform = AndroidxRcPlatformServices(),
    ) {
        val w = windowWidth()
        val h = windowHeight()
        val bitmap = createTimeBitmap()
        val bId = addBitmap(bitmap)
        val id =
            createShader(SPHERE_TIME_SHADER_SRC)
                .setFloatUniform("iTime", RemoteContext.FLOAT_CONTINUOUS_SEC)
                .setFloatUniform("iResolution", w.toFloat(), h.toFloat())
                .setBitmapUniform("myImage", bId)
                .commit()
        painter.setShader(id).commit()
        drawRect(0f, 0f, w, h)
    }
}

@Preview
@Composable
private fun SphereTimeShaderPreview() = RemoteDocumentPreview(sphereTimeShader())
