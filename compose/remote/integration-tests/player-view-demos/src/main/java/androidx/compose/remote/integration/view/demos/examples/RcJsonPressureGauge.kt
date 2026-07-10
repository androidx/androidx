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

import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.json.RemoteComposeJsonParser
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

/** JSON rewrite of the PressureGauge DSL demo using the new compact Tag-Key shorthand syntax. */
@Suppress("RestrictedApiAndroidX")
@JvmOverloads
fun rcJsonPressureGauge(
    platform: RcPlatformServices = AndroidxRcPlatformServices()
): RemoteComposeContext {
    val json =
        """
{
  "header": {
    "width": 500,
    "height": 500,
    "contentDescription": "Pressure Gauge",
    "apiLevel": 8,
    "profiles": 513
  },
  "root": {
    "canvas": {
      "modifiers": [ "fillMaxSize", { "background": "#4A6DA7" } ],
      "commands": [
        { "variable": { "name": "w", "value": "width", "commit": true } },
        { "variable": { "name": "h", "value": "height", "commit": true } },
        { "variable": { "name": "cx", "value": "@w / 2.0" } },
        { "variable": { "name": "density", "value": "density" } },
        { "variable": { "name": "deltaPressure", "value": "cos(time)" } },

        { "paint": { "color": "#6A8DC7", "textSize": "@density * 24.0" } },
        { "drawTextAnchored": { "text": "PRESSURE", "x": 50.0, "y": 40.0, "panX": -1.0, "panY": 1.0, "flags": 0 } },

        { "paint": { "style": "stroke", "width": "@density * 6.0" } },
        { "variable": { "name": "dialY", "value": "@h / 2.0 + (min(@w, @h) / 2.0) * 0.1" } },

        {
          "save": {
            "commands": [
              { "rotate": { "angle": -135.0, "pivotX": "@cx", "pivotY": "@dialY" } },
              {
                "loop": {
                  "from": 0.0, "step": 5.0, "until": 265.0,
                  "index": "index",
                  "commands": [
                    { "rotate": { "angle": 5.0, "pivotX": "@cx", "pivotY": "@dialY" } },
                    { "drawLine": {
                      "x1": "@cx",
                      "y1": "@dialY - (min(@w, @h) / 2.0) * 0.8 + (min(@w, @h) / 2.0) * 0.2",
                      "x2": "@cx",
                      "y2": "@dialY - (min(@w, @h) / 2.0) * 0.8"
                    } }
                  ]
                }
              }
            ]
          }
        },

        { "variable": { "name": "tickLength", "value": "(min(@w, @h) / 2.0) * 0.2" } },

        { "paint": {
            "ops": [
              {
                "sweepGradient": {
                  "centerX": "@cx",
                  "centerY": "@dialY",
                  "colors": [ "#00FFFFFF", "#99FFFFFF", "#00FFFFFF" ],
                  "stops": [ 0.01, 0.05, 0.09 ]
                }
              },
              { "style": "stroke" },
              { "width": "@tickLength" },
              { "strokeCap": "butt" }
            ]
        } },

        {
          "save": {
            "commands": [
              { "rotate": { "angle": "((sin(time) * 50.0 + 750.0) - 700.0) / 100.0 * 270.0 - 135.0 - 90.0 - 18.0", "pivotX": "@cx", "pivotY": "@dialY" } },
              { "drawArc": {
                "left": "@cx - ((min(@w, @h) / 2.0) * 0.8 - @tickLength / 2.0)",
                "top": "@dialY - ((min(@w, @h) / 2.0) * 0.8 - @tickLength / 2.0)",
                "right": "@cx + ((min(@w, @h) / 2.0) * 0.8 - @tickLength / 2.0)",
                "bottom": "@dialY + ((min(@w, @h) / 2.0) * 0.8 - @tickLength / 2.0)",
                "startAngle": 18.0,
                "sweepAngle": "cos(time) * -12.0"
              } }
            ]
          }
        },

        {
          "paint": {
            "shader": 0,
            "color": "#FFFFFF",
            "style": "stroke",
            "width": "@density * 8.0",
            "strokeCap": "round"
          }
        },
        {
          "save": {
            "commands": [
              { "rotate": { "angle": "((sin(time) * 50.0 + 750.0) - 700.0) / 100.0 * 270.0 - 135.0", "pivotX": "@cx", "pivotY": "@dialY" } },
              { "drawLine": {
                "x1": "@cx",
                "y1": "@dialY - (min(@w, @h) / 2.0) * 0.8 + @tickLength",
                "x2": "@cx",
                "y2": "@dialY - (min(@w, @h) / 2.0) * 0.8"
              } }
            ]
          }
        },

        {
          "paint": {
            "ops": [
              { "shader": 0 },
              { "textSize": "@density * 64.0" },
              { "color": "#FFFFFF" },
              { "style": "fill" },
              { "width": 0.0 }
            ]
          }
        },

        {
          "conditionalOperations": {
            "condition": "lt",
            "v1": 0.0,
            "v2": "@deltaPressure",
            "commands": [
              { "drawTextAnchored": { "text": "↑", "x": "@cx", "y": "@dialY", "panX": 0.0, "panY": -3.0, "flags": 0 } }
            ]
          }
        },
        {
          "conditionalOperations": {
            "condition": "ge",
            "v1": 0.0,
            "v2": "@deltaPressure",
            "commands": [
              { "drawTextAnchored": { "text": "↓", "x": "@cx", "y": "@dialY", "panX": 0.0, "panY": -3.0, "flags": 0 } }
            ]
          }
        },

        { "variable": { "name": "pressure", "value": "sin(time) * 50.0 + 750.0" } },
        { "variable": { "name": "textId", "value": { "type": "textFromFloat", "value": "@pressure", "whole": 3, "decimal": 0, "flags": 3 } } },
        { "drawTextAnchored": { "text": "@textId", "x": "@cx", "y": "@dialY", "panX": 0.0, "panY": 0.0, "flags": 0 } },

        { "paint": { "textSize": "@density * 32.0" } },
        { "drawTextAnchored": { "text": "mmHg", "x": "@cx", "y": "@dialY", "panX": 0.0, "panY": 4.0, "flags": 0 } },
        { "drawTextAnchored": { "text": "Low", "x": "@cx - ((min(@w, @h) / 2.0) * 0.8) * 0.6", "y": "@dialY + ((min(@w, @h) / 2.0) * 0.8) * 0.9", "panX": 0.0, "panY": 0.0, "flags": 0 } },
        { "drawTextAnchored": { "text": "High", "x": "@cx + ((min(@w, @h) / 2.0) * 0.8) * 0.6", "y": "@dialY + ((min(@w, @h) / 2.0) * 0.8) * 0.9", "panX": 0.0, "panY": 0.0, "flags": 0 } }
      ]
    }
  }
}
    """

    val actualTags = RemoteComposeJsonParser.parseHeaderOnly(json)
    val actualApiLevel = RemoteComposeJsonParser.parseApiLevel(json)
    val writer = RemoteComposeWriter(platform, actualApiLevel, *actualTags!!)
    val parser = RemoteComposeJsonParser(writer)
    parser.parse(json)
    return RemoteComposeContext(writer)
}
