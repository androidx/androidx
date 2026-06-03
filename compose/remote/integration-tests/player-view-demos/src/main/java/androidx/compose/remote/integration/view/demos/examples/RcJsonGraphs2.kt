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

@Suppress("RestrictedApiAndroidX")
@JvmOverloads
fun rcJsonGraphs2(
    platform: RcPlatformServices = AndroidxRcPlatformServices()
): RemoteComposeContext {
    val json =
        """
        {
          "header": {
            "width": 500,
            "height": 500,
            "contentDescription": "Simple Timer",
            "apiLevel": 7,
            "profiles": 512
          },
          "root": {
            "box": {
              "modifiers": [ "fillMaxSize" ],
              "horizontalAlignment": "start",
              "verticalAlignment": "start",
              "children": [
                {
                  "canvas": {
                    "modifiers": [ "fillMaxSize", { "background": "#112244" } ],
                    "commands": [
                      { "variable": { "name": "w", "value": "componentWidth", "commit": true } },
                      { "variable": { "name": "h", "value": "componentHeight", "commit": true } },
                      { "variable": { "name": "density", "value": "density" } },

                      { "variable": { "name": "scale", "value": [ -8388607, -5177326, 1069547520, -5177343, 1092616192, -5177341, -5177334 ], "flush": true } },
                      { "variable": { "name": "xRange", "value": [ 1092616192, -1054867456, -5177342 ], "flush": true } },
                      { "variable": { "name": "yRange", "value": [ -8388564, -1082130432, -8388607, -5177326, 1069547520, -5177343, 1092616192, -5177341, -5177334, -5177341, -5177342 ], "flush": true } },
                      { "variable": { "name": "graphMax", "value": [ -8388564, 1028443341, -8388564, -1082130432, -8388607, -5177326, 1069547520, -5177343, 1092616192, -5177341, -5177334, -5177341, -5177342, -5177341, -5177343 ], "flush": true } },
                      { "variable": { "name": "graphMin", "value": [ -1082130432, -8388607, -5177326, 1069547520, -5177343, 1092616192, -5177341, -5177334, -5177341, 1028443341, -8388564, -1082130432, -8388607, -5177326, 1069547520, -5177343, 1092616192, -5177341, -5177334, -5177341, -5177342, -5177341, -5177342 ], "flush": true } },

                      { "save": {} },

                      { "variable": { "name": "transX", "value": [ 1092616192, -8388581, -5177341, 1073741824, -8388581, -5177341, -5177343 ], "flush": true } },
                      { "variable": { "name": "transY", "value": [ 1092616192, -8388581, -5177341, 1073741824, -8388581, -5177341, -5177343 ], "flush": true } },

                      { "translate": { "dx": "@vars.transX", "dy": "@vars.transY" } },

                      { "variable": { "name": "majorStepX_pow", "value": [ 1092616192, -8388563, 1077936128, -5177340, -5177329, -5177330, -5177336 ], "flush": true } },
                      { "variable": { "name": "scaleX", "value": [ -8388566, 1073741824, -8388581, -5177341, -5177342, -8388559, -5177342, 1106247680, -8388581, -5177341, -5177342, 1092616192, -8388581, -5177341, -5177342, -8388563, -5177340 ], "flush": true } },
                      { "variable": { "name": "offsetX", "value": [ 1106247680, -8388581, -5177341, -1054867456, -8388556, -5177341, -5177342 ], "flush": true } },
                      { "variable": { "name": "scaleY", "value": [ 1092616192, -8388581, -5177341, 1112014848, -8388581, -5177341, -5177343, -8388565, 1073741824, -8388581, -5177341, -5177342, -8388558, -5177342, -5177342, -8388562, -5177340 ], "flush": true } },
                      { "variable": { "name": "offsetY", "value": [ -8388565, 1073741824, -8388581, -5177341, -5177342, -8388558, -5177342, 1112014848, -8388581, -5177341, -5177342, -8388554, -1082130432, -8388607, -5177326, 1069547520, -5177343, 1092616192, -5177341, -5177334, -5177341, -5177341, -5177342 ], "flush": true } },

                      { "variable": { "name": "textSize", "value": [ -8388581, 1098907648, -5177341 ], "flush": true } },
                      { "paint": { "color": "#0000FF", "textSize": "@vars.textSize" } },
                      { "paint": { "color": "#444444", "width": 2.0 } },

                      { "variable": { "name": "minorStepX_pow", "value": [ 1092616192, -8388563, 1101004800, -5177340, -5177329, -5177330, -5177336 ], "flush": true } },
                      { "variable": { "name": "minY_scaled", "value": [ -1082130432, -8388607, -5177326, 1069547520, -5177343, 1092616192, -5177341, -5177334, -5177341, -8388554, -5177341, -8388553, -5177343 ], "flush": true } },
                      { "variable": { "name": "maxY_scaled", "value": [ -8388564, -8388554, -5177341, -8388553, -5177343 ], "flush": true } },

                      { "variable": { "name": "startX", "value": [ -1054867456 ], "flush": true } },
                      { "variable": { "name": "minorStepX", "value": [ 1073741824, 1092616192, -8388563, 1101004800, -5177340, -5177329, -5177330, -5177336, -8388551, 1073741824, -5177341, -8388563, 1101004800, -5177340, -8388551, -5177340, 1073741824, -5177342, -5177318, -8388551, 1084227584, -5177341, -8388563, 1101004800, -5177340, -8388551, -5177340, 1084227584, -5177342, -5177318, -5177337 ], "flush": true } },
                      { "variable": { "name": "endX", "value": [ 1092616192 ], "flush": true } },

                      {
                        "loop": {
                          "from": "@vars.startX", "step": "@vars.minorStepX", "until": "@vars.endX",
                          "index": "x1", "noIndexText": true,
                          "commands": [
                            { "variable": { "name": "sx1", "value": [ -8388545, -8388556, -5177341, -8388555, -5177343 ], "flush": true } },
                            { "drawLine": { "x1": "@vars.sx1", "y1": "@vars.minY_scaled", "x2": "@vars.sx1", "y2": "@vars.maxY_scaled" } }
                          ]
                        }
                      },

                      { "paint": { "color": "#444444", "width": 2.0 } },

                      { "variable": { "name": "minorStepY_pow", "value": [ 1092616192, -8388562, 1101004800, -5177340, -5177329, -5177330, -5177336 ], "flush": true } },
                      { "variable": { "name": "x1", "value": [ -8388548, -8388556, -5177341, -8388555, -5177343 ], "flush": true } },
                      { "variable": { "name": "x2", "value": [ -8388546, -8388556, -5177341, -8388555, -5177343 ], "flush": true } },
                      { "variable": { "name": "minY", "value": [ -1082130432, -8388607, -5177326, 1069547520, -5177343, 1092616192, -5177341, -5177334, -5177341 ], "flush": true } },
                      { "variable": { "name": "minorStepY", "value": [ 1092616192, -8388562, 1101004800, -5177340, -5177329, -5177330, -5177336, -8388543, 1073741824, -5177341, -8388562, 1101004800, -5177340, -8388543, -5177340, 1073741824, -5177342, -5177318, -8388543, 1084227584, -5177341, -8388562, 1101004800, -5177340, -8388543, -5177340, 1084227584, -5177342, -5177318 ], "flush": true } },
                      { "variable": { "name": "endY_plus", "value": [ -8388564, 1008981770, -5177343 ], "flush": true } },

                      {
                        "loop": {
                          "from": "@vars.minY", "step": "@vars.minorStepY", "until": "@vars.endY_plus",
                          "index": "y1", "noIndexText": true,
                          "commands": [
                            { "variable": { "name": "sy1", "value": [ -8388537, -8388554, -5177341, -8388553, -5177343 ], "flush": true } },
                            { "drawLine": { "x1": "@vars.x1", "y1": "@vars.sy1", "x2": "@vars.x2", "y2": "@vars.sy1" } }
                          ]
                        }
                      },

                      { "paint": { "color": "#888888", "width": 4.0 } },

                      { "variable": { "name": "majorStepX", "value": [ 1073741824, 1092616192, -8388563, 1077936128, -5177340, -5177329, -5177330, -5177336, -8388557, 1073741824, -5177341, -8388563, 1077936128, -5177340, -8388557, -5177340, 1073741824, -5177342, -5177318, -8388557, 1084227584, -5177341, -8388563, 1077936128, -5177340, -8388557, -5177340, 1084227584, -5177342, -5177318, -5177337 ], "flush": true } },
                      { "variable": { "name": "endX_plus", "value": [ -8388546, 1008981770, -5177343 ], "flush": true } },

                      {
                        "loop": {
                          "from": "@vars.startX", "step": "@vars.majorStepX", "until": "@vars.endX_plus",
                          "index": "x2", "noIndexText": true,
                          "commands": [
                            { "variable": { "name": "valX", "value": [ -8388533 ], "flush": true } },
                            { "variable": { "name": "textX", "value": { "type": "textFromFloat", "value": "@vars.valX", "whole": 5, "decimal": 1, "flags": 7 }, "commit": true } },
                            { "variable": { "name": "posX", "value": [ -8388533, -8388556, -5177341, -8388555, -5177343 ], "flush": true } },
                            { "drawTextAnchored": { "text": "@vars.textX", "x": "@vars.posX", "y": "@vars.minY_scaled", "panX": 0.0, "panY": 1.5, "flags": 0 } },
                            { "drawLine": { "x1": "@vars.posX", "y1": "@vars.minY_scaled", "x2": "@vars.posX", "y2": "@vars.maxY_scaled" } }
                          ]
                        }
                      },

                      { "paint": { "color": "#888888", "width": 4.0 } },

                      { "variable": { "name": "majorStepY_pow", "value": [ 1092616192, -8388562, 1084227584, -5177340, -5177329, -5177330, -5177336 ], "flush": true } },
                      { "variable": { "name": "end", "value": [ -8388548, -8388556, -5177341, -8388555, -5177343 ], "flush": true } },
                      { "variable": { "name": "majorStepY", "value": [ 1092616192, -8388562, 1084227584, -5177340, -5177329, -5177330, -5177336, -8388529, 1073741824, -5177341, -8388562, 1084227584, -5177340, -8388529, -5177340, 1073741824, -5177342, -5177318, -8388529, 1084227584, -5177341, -8388562, 1084227584, -5177340, -8388529, -5177340, 1084227584, -5177342, -5177318 ], "flush": true } },
                      { "variable": { "name": "endY_plus2", "value": [ -8388564, 1008981770, -5177343 ], "flush": true } },

                      {
                        "loop": {
                          "from": "@vars.minY", "step": "@vars.majorStepY", "until": "@vars.endY_plus2",
                          "index": "y2", "noIndexText": true,
                          "commands": [
                            { "variable": { "name": "valY", "value": [ -8388525 ], "flush": true } },
                            { "variable": { "name": "textY", "value": { "type": "textFromFloat", "value": "@vars.valY", "whole": 5, "decimal": 1, "flags": 7 }, "commit": true } },
                            { "variable": { "name": "labelX", "value": [ 1106247680, -8388581, -5177341 ], "flush": true } },
                            { "variable": { "name": "posY", "value": [ -8388525, -8388554, -5177341, -8388553, -5177343 ], "flush": true } },
                            { "drawTextAnchored": { "text": "@vars.textY", "x": "@vars.labelX", "y": "@vars.posY", "panX": 1.5, "panY": 0.0, "flags": 0 } },
                            { "drawLine": { "x1": "@vars.x1", "y1": "@vars.posY", "x2": "@vars.x2", "y2": "@vars.posY" } }
                          ]
                        }
                      },

                      { "paint": { "color": "#FFFF00", "width": 4.0 } },
                      { "drawLine": { "x1": "@vars.offsetX", "y1": "@vars.minY_scaled", "x2": "@vars.offsetX", "y2": "@vars.maxY_scaled" } },
                      { "paint": { "color": "#FFFF00", "width": 4.0 } },
                      { "drawLine": { "x1": "@vars.x1", "y1": "@vars.offsetY", "x2": "@vars.x2", "y2": "@vars.offsetY" } },

                      { "variable": { "name": "strokeWidth", "value": [ 1073741824, -8388581, -5177341 ], "flush": true } },
                      { "paint": {
                        "ops": [
                          { "shader": 0 },
                          { "color": "#FF0000" },
                          { "style": "stroke" },
                          { "width": "@vars.strokeWidth" }
                        ]
                      } },

                      { "pathExpression": {
                        "id": "graphPath",
                        "expressionX": "a[0] * @vars.scaleX + @vars.offsetX",
                        "expressionY": "min((abs((sin(time) + 1.5) * 10.0)), 15.0) * sin(a[0] * 0.3 + time) * sin(a[0] * 7.0) * @vars.scaleY + @vars.offsetY",
                        "start": "@vars.startX",
                        "end": "@vars.endX",
                        "count": 128.0,
                        "flags": 4
                      } },
                      { "drawPath": { "path": "graphPath" } },

                      { "restore": {} }
                    ]
                  }
                }
              ]
            }
          }
        }
        """
            .trimIndent()

    val actualTags = RemoteComposeJsonParser.parseHeaderOnly(json)
    val actualApiLevel = RemoteComposeJsonParser.parseApiLevel(json)
    val writer = RemoteComposeWriter(platform, actualApiLevel, *actualTags!!)
    val parser = RemoteComposeJsonParser(writer)
    parser.parse(json)
    return RemoteComposeContext(writer)
}
