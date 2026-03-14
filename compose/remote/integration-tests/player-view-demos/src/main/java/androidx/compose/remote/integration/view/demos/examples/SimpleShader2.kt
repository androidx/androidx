/*
 * Copyright (C) 2024 The Android Open Source Project
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
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Suppress("RestrictedApiAndroidX")
fun createShaderDoc4(personImage: Bitmap): RemoteComposeContext {
    val COLOR_SHADER_SRC =
        """
        uniform shader myImage;
        uniform float2 iResolution;
        uniform float iTime;
        half4 main(vec2 fragcoord) { 
          float2 scale = vec2(224.,233.) / iResolution.xy;
          vec3 color = vec3(0.5, 0.2, 0.4);
          color.g = fragcoord.y/iResolution.y;
          color.r = 0.5;
          vec2 uv = (.53 - fragcoord.xy / iResolution.y) * 16;
          float wave =  0.3 * sin(length( uv * 0.5) - 2*iTime);
          color.b  = fragcoord.x/iResolution.x;
          color = myImage.eval(fragcoord*(0.5+sin(iTime*10+uv)*0.003)).rgb; 
         
          color.b -= wave - 0.3;
          color.rg *= 0.4;
         //  color.b  *= fragcoord.x/iResolution.x;
          return vec4(color, 1.0);
        }
        """
            .trimIndent()
    val shader = COLOR_SHADER_SRC
    // remove color = myImage.eval(fragcoord).rgb;  to see it work
    val tw = 1024.0f
    val th = 1024.0f
    val doc =
        RemoteComposeContextAndroid(
            1024,
            1024,
            "Demo",
            7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            val bId = addBitmap(personImage)
            val id =
                createShader(shader)
                    .setFloatUniform("iTime", RemoteContext.FLOAT_CONTINUOUS_SEC)
                    .setFloatUniform(
                        "iResolution",
                        RemoteContext.FLOAT_WINDOW_WIDTH,
                        RemoteContext.FLOAT_WINDOW_HEIGHT,
                    )
                    .setBitmapUniform("myImage", bId)
                    .commit()
            painter.setShader(id).commit()
            drawOval(0f, 0f, tw, th)
        }

    return doc
}

@Suppress("RestrictedApiAndroidX")
fun createShaderDoc3(personImage: Bitmap): RemoteComposeContext {
    val COLOR_SHADER_SRC =
        """
        uniform shader myImage;
        uniform float2 iResolution;
        uniform float iTime;
        half4 main(vec2 fragcoord) { 
          float2 scale = vec2(224.,233.) / iResolution.xy;
          vec3 color = vec3(0.5, 0.2, 0.4);
          color.g = fragcoord.y/iResolution.y;
          color.r = 0.5;
          vec2 uv = (.53 - fragcoord.xy / iResolution.y) * 16;
          float wave =  0.3 * sin(length( uv * 0.5) - 2*iTime);
          color.b  = fragcoord.x/iResolution.x;
          color = myImage.eval(fragcoord*(0.5+sin(iTime*10+uv)*0.003)).rgb; 
         
          color.b -= wave - 0.3;
          color.rg *= 0.4;
         //  color.b  *= fragcoord.x/iResolution.x;
          return vec4(color, 1.0);
        }
        """
            .trimIndent()
    val shader =
        """
        //        uniform shader myImage;
                 uniform float2 iResolution;
                 uniform float iTime;
                 half4 main(vec2 fragcoord) {
                   vec3 color = vec3(0.5, 0.2, 0.4);
                   return vec4(color , 1.0);
                 }
        """
            .trimIndent()

    return RemoteComposeContextAndroid(
        800,
        800,
        "Clock",
        7,
        profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        platform = AndroidxRcPlatformServices(),
    ) {
        root {
            box(Modifier.fillMaxSize()) {
                canvas(Modifier.fillMaxSize()) {
                    val width = ComponentWidth()
                    val height = ComponentHeight()
                    val bId = addBitmap(personImage)
                    painter.setColor(Color.Green.toArgb()).commit()

                    val id =
                        createShader(COLOR_SHADER_SRC)
                            .setFloatUniform("iTime", RemoteContext.FLOAT_CONTINUOUS_SEC)
                            .setFloatUniform("iResolution", 500f, 500f)
                            .setBitmapUniform("myImage", bId)
                            .commit()
                    val id2 =
                        createShader(COLOR_SHADER_SRC)
                            .setFloatUniform("iTime", RemoteContext.FLOAT_CONTINUOUS_SEC)
                            .setFloatUniform("iResolution", 500f, 500f)
                            .setBitmapUniform("myImage", bId)
                            .commit()

                    println(">>>>>>>>>> shader id $id2")
                    painter.setShader(id2).commit()
                    drawOval(0f, 0f, width.toFloat(), height.toFloat())
                    //                    painter.setColor(Color.Red.toArgb()).commit()
                    //                    drawLine(0f, 0f, width.toFloat(), height.toFloat())
                    //                    drawLine(0f, height.toFloat(), width.toFloat(), 0f)
                }
            }
        }
    }
}
