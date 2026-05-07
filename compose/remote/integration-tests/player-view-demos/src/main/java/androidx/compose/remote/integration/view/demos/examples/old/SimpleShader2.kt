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
package androidx.compose.remote.integration.view.demos.examples.old

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

@SuppressLint("RestrictedApiAndroidX")
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

    // remove color = myImage.eval(fragcoord).rgb;  to see it work
    val tw = 1024.0f
    val th = 1024.0f
    val doc =
        RemoteComposeContextAndroid(
            1024,
            1024,
            "Demo",
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX,
            platform = AndroidxRcPlatformServices(),
        ) {
            val bId = addBitmap(personImage)
            val id =
                createShader(COLOR_SHADER_SRC)
                    .setFloatUniform("iTime", RemoteContext.FLOAT_CONTINUOUS_SEC)
                    .setFloatUniform("iResolution", 600f, 600f)
                    .setBitmapUniform("myImage", bId)
                    .commit()
            painter.setShader(id).commit()
            drawOval(0f, 0f, tw, th)
        }

    return doc
}
