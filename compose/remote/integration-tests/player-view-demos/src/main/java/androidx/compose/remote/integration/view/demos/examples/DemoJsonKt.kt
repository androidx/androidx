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

import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.json.RemoteComposeJsonParser
import androidx.compose.remote.integration.view.demos.utils.RCDoc
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.view.platform.AndroidRcPlatformServices
import java.io.ByteArrayInputStream

@Suppress("RestrictedApiAndroidX")
private fun createRCDoc(writer: RemoteComposeWriter, name: String): RCDoc {
    return object : RCDoc {
        private var remoteComposeDocument: RemoteDocument? = null
        private var buildTime: Float = 0f

        override fun run() {}

        override fun getDoc(): RemoteDocument? {
            if (remoteComposeDocument == null) {
                val start = System.nanoTime()
                remoteComposeDocument =
                    RemoteDocument(ByteArrayInputStream(writer.encodeToByteArray()))
                buildTime = (System.nanoTime() - start) * 1E-6f
            }
            return remoteComposeDocument
        }

        override fun getColor(): Int = 0xFF0047AB.toInt()

        override fun getBuildTime(): Float = buildTime

        override fun size(): Int = writer.encodeToByteArray().size

        override fun zipSize(): Int = writer.encodeToByteArray().size // Approximation

        override fun toString(): String = name
    }
}

@Suppress("RestrictedApiAndroidX")
fun demoJson1(): RCDoc {
    val json =
        """
        {
          "resources": {
            "colors": {
              "brand": "#0047AB",
              "accent": "#FF5722"
            }
          },
          "root": {
            "type": "column",
            "modifiers": [
              { "fillMaxSize": 1.0 },
              { "background": "#F5F5F5" },
              { "padding": 20 }
            ],
            "children": [
              {
                "type": "text",
                "value": "JSON RemoteCompose",
                "color": "${'$'}colors.brand",
                "fontSize": 24
              },
              {
                "type": "box",
                "modifiers": [
                  { "height": 100 },
                  { "fillMaxWidth": 1.0 },
                  { "background": "#FFFFFF" },
                  { "padding": 10 }
                ],
                "children": [
                  {
                    "type": "text",
                    "value": "Card Content",
                    "color": "#333333"
                  }
                ]
              },
              {
                "type": "canvas",
                "modifiers": [
                  { "height": 200 },
                  { "fillMaxWidth": 1.0 }
                ],
                "commands": [
                  { "type": "drawRect", "left": 0, "top": 0, "right": "width", "bottom": "height", "color": "#EEEEEE" },
                  { 
                    "type": "drawCircle", 
                    "cx": "width / 2 + cos(time) * 50", 
                    "cy": "height / 2 + sin(time) * 30", 
                    "radius": "40 + sin(time * 2) * 10", 
                    "color": "${'$'}colors.accent" 
                  }
                ]
              }
            ]
          }
        }
        """
            .trimIndent()

    val writer = RemoteComposeWriter(AndroidRcPlatformServices(), 7)
    val parser = RemoteComposeJsonParser(writer)
    parser.parse(json)
    return createRCDoc(writer, "JSON Demo 1")
}

@Suppress("RestrictedApiAndroidX")
fun demoJson2(): RCDoc {
    val json =
        """
        {
          "root": {
            "type": "box",
            "modifiers": [
              { "fillMaxSize": 1.0 },
              { "background": "#000000" }
            ],
            "children": [
              {
                "type": "canvas",
                "modifiers": [{ "fillMaxSize": 1.0 }],
                "commands": [
                  { 
                    "type": "drawCircle", 
                    "cx": "width / 2", 
                    "cy": "height / 2", 
                    "radius": "min(width, height) / 3 * (0.8 + 0.2 * sin(time * 3))", 
                    "color": "#00FF00" 
                  },
                  {
                    "type": "drawRoundRect",
                    "left": "width / 4",
                    "top": "height / 4",
                    "right": "width * 3 / 4",
                    "bottom": "height * 3 / 4",
                    "rx": 20,
                    "ry": 20,
                    "color": "#55FFFFFF"
                  }
                ]
              },
              {
                "type": "text",
                "value": "Dynamic Visuals",
                "color": "#FFFFFF",
                "fontSize": 30,
                "modifiers": [
                  { "padding": { "top": 50 } }
                ]
              }
            ]
          }
        }
        """
            .trimIndent()

    val writer = RemoteComposeWriter(AndroidRcPlatformServices(), 7)
    val parser = RemoteComposeJsonParser(writer)
    parser.parse(json)
    return createRCDoc(writer, "JSON Demo 2")
}

@Suppress("RestrictedApiAndroidX")
fun demoJson3(): RCDoc {
    val json =
        """
        {
          "resources": {
            "variables": {
              "fov": 400,
              "dist": 4,
              "rotX": "time * 0.7",
              "rotY": "time * 1.1",
              
              "cosX": "cos(${'$'}vars.rotX)", "sinX": "sin(${'$'}vars.rotX)",
              "cosY": "cos(${'$'}vars.rotY)", "sinY": "sin(${'$'}vars.rotY)",

              "cx": "width / 2",
              "cy": "height / 2",

              "v0x": "(-1 * ${'$'}vars.cosY + -1 * ${'$'}vars.sinY)",
              "v0y": "(-1 * ${'$'}vars.cosX - (-1 * -1 * ${'$'}vars.sinY + -1 * ${'$'}vars.cosY) * ${'$'}vars.sinX)",
              "v0z": "(-1 * ${'$'}vars.sinX + (-1 * -1 * ${'$'}vars.sinY + -1 * ${'$'}vars.cosY) * ${'$'}vars.cosX) + ${'$'}vars.dist",
              "v0px": "${'$'}vars.v0x * ${'$'}vars.fov / ${'$'}vars.v0z + ${'$'}vars.cx",
              "v0py": "${'$'}vars.v0y * ${'$'}vars.fov / ${'$'}vars.v0z + ${'$'}vars.cy",

              "v1x": "(1 * ${'$'}vars.cosY + -1 * ${'$'}vars.sinY)",
              "v1y": "(-1 * ${'$'}vars.cosX - (1 * -1 * ${'$'}vars.sinY + -1 * ${'$'}vars.cosY) * ${'$'}vars.sinX)",
              "v1z": "(-1 * ${'$'}vars.sinX + (1 * -1 * ${'$'}vars.sinY + -1 * ${'$'}vars.cosY) * ${'$'}vars.cosX) + ${'$'}vars.dist",
              "v1px": "${'$'}vars.v1x * ${'$'}vars.fov / ${'$'}vars.v1z + ${'$'}vars.cx",
              "v1py": "${'$'}vars.v1y * ${'$'}vars.fov / ${'$'}vars.v1z + ${'$'}vars.cy",

              "v2x": "(1 * ${'$'}vars.cosY + 1 * ${'$'}vars.sinY)",
              "v2y": "(1 * ${'$'}vars.cosX - (1 * 1 * ${'$'}vars.sinY + 1 * ${'$'}vars.cosY) * ${'$'}vars.sinX)",
              "v2z": "(1 * ${'$'}vars.sinX + (1 * 1 * ${'$'}vars.sinY + 1 * ${'$'}vars.cosY) * ${'$'}vars.cosX) + ${'$'}vars.dist",
              "v2px": "${'$'}vars.v2x * ${'$'}vars.fov / ${'$'}vars.v2z + ${'$'}vars.cx",
              "v2py": "${'$'}vars.v2y * ${'$'}vars.fov / ${'$'}vars.v2z + ${'$'}vars.cy",

              "v3x": "(-1 * ${'$'}vars.cosY + 1 * ${'$'}vars.sinY)",
              "v3y": "(1 * ${'$'}vars.cosX - (-1 * 1 * ${'$'}vars.sinY + 1 * ${'$'}vars.cosY) * ${'$'}vars.sinX)",
              "v3z": "(1 * ${'$'}vars.sinX + (-1 * 1 * ${'$'}vars.sinY + 1 * ${'$'}vars.cosY) * ${'$'}vars.cosX) + ${'$'}vars.dist",
              "v3px": "${'$'}vars.v3x * ${'$'}vars.fov / ${'$'}vars.v3z + ${'$'}vars.cx",
              "v3py": "${'$'}vars.v3y * ${'$'}vars.fov / ${'$'}vars.v3z + ${'$'}vars.cy",

              "v4x": "(-1 * ${'$'}vars.cosY + -1 * ${'$'}vars.sinY)",
              "v4y": "(-1 * ${'$'}vars.cosX - (-1 * -1 * ${'$'}vars.sinY + 1 * ${'$'}vars.cosY) * ${'$'}vars.sinX)",
              "v4z": "(-1 * ${'$'}vars.sinX + (-1 * -1 * ${'$'}vars.sinY + 1 * ${'$'}vars.cosY) * ${'$'}vars.cosX) + ${'$'}vars.dist",
              "v4px": "${'$'}vars.v4x * ${'$'}vars.fov / ${'$'}vars.v4z + ${'$'}vars.cx",
              "v4py": "${'$'}vars.v4y * ${'$'}vars.fov / ${'$'}vars.v4z + ${'$'}vars.cy",

              "v5x": "(1 * ${'$'}vars.cosY + -1 * ${'$'}vars.sinY)",
              "v5y": "(-1 * ${'$'}vars.cosX - (1 * -1 * ${'$'}vars.sinY + 1 * ${'$'}vars.cosY) * ${'$'}vars.sinX)",
              "v5z": "(-1 * ${'$'}vars.sinX + (1 * -1 * ${'$'}vars.sinY + 1 * ${'$'}vars.cosY) * ${'$'}vars.cosX) + ${'$'}vars.dist",
              "v5px": "${'$'}vars.v5x * ${'$'}vars.fov / ${'$'}vars.v5z + ${'$'}vars.cx",
              "v5py": "${'$'}vars.v5y * ${'$'}vars.fov / ${'$'}vars.v5z + ${'$'}vars.cy",

              "v6x": "(1 * ${'$'}vars.cosY + 1 * ${'$'}vars.sinY)",
              "v6y": "(1 * ${'$'}vars.cosX - (1 * 1 * ${'$'}vars.sinY + -1 * ${'$'}vars.cosY) * ${'$'}vars.sinX)",
              "v6z": "(1 * ${'$'}vars.sinX + (1 * 1 * ${'$'}vars.sinY + -1 * ${'$'}vars.cosY) * ${'$'}vars.cosX) + ${'$'}vars.dist",
              "v6px": "${'$'}vars.v6x * ${'$'}vars.fov / ${'$'}vars.v6z + ${'$'}vars.cx",
              "v6py": "${'$'}vars.v6y * ${'$'}vars.fov / ${'$'}vars.v6z + ${'$'}vars.cy",

              "v7x": "(-1 * ${'$'}vars.cosY + 1 * ${'$'}vars.sinY)",
              "v7y": "(1 * ${'$'}vars.cosX - (-1 * 1 * ${'$'}vars.sinY + -1 * ${'$'}vars.cosY) * ${'$'}vars.sinX)",
              "v7z": "(1 * ${'$'}vars.sinX + (-1 * 1 * ${'$'}vars.sinY + -1 * ${'$'}vars.cosY) * ${'$'}vars.cosX) + ${'$'}vars.dist",
              "v7px": "${'$'}vars.v7x * ${'$'}vars.fov / ${'$'}vars.v7z + ${'$'}vars.cx",
              "v7py": "${'$'}vars.v7y * ${'$'}vars.fov / ${'$'}vars.v7z + ${'$'}vars.cy"
            }
          },
          "root": {
            "type": "canvas",
            "modifiers": [{ "fillMaxSize": 1.0 }],
            "commands": [
              { "type": "setColor", "color": "#00FF00" },
              { "type": "setStrokeWidth", "width": 2 },
              { "type": "drawLine", "x1": "${'$'}vars.v0px", "y1": "${'$'}vars.v0py", "x2": "${'$'}vars.v1px", "y2": "${'$'}vars.v1py" },
              { "type": "drawLine", "x1": "${'$'}vars.v1px", "y1": "${'$'}vars.v1py", "x2": "${'$'}vars.v2px", "y2": "${'$'}vars.v2py" },
              { "type": "drawLine", "x1": "${'$'}vars.v2px", "y1": "${'$'}vars.v2py", "x2": "${'$'}vars.v3px", "y2": "${'$'}vars.v3py" },
              { "type": "drawLine", "x1": "${'$'}vars.v3px", "y1": "${'$'}vars.v3py", "x2": "${'$'}vars.v0px", "y2": "${'$'}vars.v0py" },
              
              { "type": "drawLine", "x1": "${'$'}vars.v4px", "y1": "${'$'}vars.v4py", "x2": "${'$'}vars.v5px", "y2": "${'$'}vars.v5py" },
              { "type": "drawLine", "x1": "${'$'}vars.v5px", "y1": "${'$'}vars.v5py", "x2": "${'$'}vars.v6px", "y2": "${'$'}vars.v6py" },
              { "type": "drawLine", "x1": "${'$'}vars.v6px", "y1": "${'$'}vars.v6py", "x2": "${'$'}vars.v7px", "y2": "${'$'}vars.v7py" },
              { "type": "drawLine", "x1": "${'$'}vars.v7px", "y1": "${'$'}vars.v7py", "x2": "${'$'}vars.v4px", "y2": "${'$'}vars.v4py" },
              
              { "type": "drawLine", "x1": "${'$'}vars.v0px", "y1": "${'$'}vars.v0py", "x2": "${'$'}vars.v4px", "y2": "${'$'}vars.v4py" },
              { "type": "drawLine", "x1": "${'$'}vars.v1px", "y1": "${'$'}vars.v1py", "x2": "${'$'}vars.v5px", "y2": "${'$'}vars.v5py" },
              { "type": "drawLine", "x1": "${'$'}vars.v2px", "y1": "${'$'}vars.v2py", "x2": "${'$'}vars.v6px", "y2": "${'$'}vars.v6py" },
              { "type": "drawLine", "x1": "${'$'}vars.v3px", "y1": "${'$'}vars.v3py", "x2": "${'$'}vars.v7px", "y2": "${'$'}vars.v7py" }
            ]
          }
        }
        """
            .trimIndent()

    val writer = RemoteComposeWriter(AndroidRcPlatformServices(), 7)
    val parser = RemoteComposeJsonParser(writer)
    parser.parse(json)
    return createRCDoc(writer, "JSON Cube 3D")
}
