/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed -> in writing, software
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

/** JSON rewrite of the LinearRegression DSL demo using the new compact Tag-Key shorthand syntax. */
@Suppress("RestrictedApiAndroidX")
@JvmOverloads
fun rcJsonLinearRegression(
    xData: FloatArray,
    yData: FloatArray,
    platform: RcPlatformServices = AndroidxRcPlatformServices(),
): RemoteComposeContext {
    val xDataJson = xData.joinToString(", ")
    val yDataJson = yData.joinToString(", ")
    val json =
        """
{
  "header": {
    "width": 500,
    "height": 500,
    "contentDescription": "Linear Regression Demo",
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
            "modifiers": [ "fillMaxSize", { "background": "#F8F8F8" } ],
            "commands": [
              { "variable": { "name": "w", "value": "width", "commit": true } },
              { "variable": { "name": "h", "value": "height", "commit": true } },
              { "variable": { "name": "density", "value": "density" } },

              { "resources": {
                "floatArrays": {
                  "rx": { "value": [ ${xDataJson} ], "export": false },
                  "ry": { "value": [ ${yDataJson} ], "export": false }
                }
              } },

              { "variable": { "name": "sumX", "value": "arraySum(@vars.rx)", "flush": true } },
              { "variable": { "name": "sumY", "value": "arraySum(@vars.ry)", "flush": true } },
              { "variable": { "name": "slope", "value": "(arrayLength(@vars.rx) * arraySumXY(@vars.rx, @vars.ry) - @vars.sumX * @vars.sumY) / (arrayLength(@vars.rx) * arraySumSqr(@vars.rx) - @vars.sumX * @vars.sumX)", "flush": true } },
              { "variable": { "name": "intercept", "value": "(@vars.sumY - @vars.slope * @vars.sumX) / arrayLength(@vars.rx)", "flush": true } },

              { "variable": { "name": "xRange", "value": "50.0 - 0.0", "flush": true } },
              { "variable": { "name": "yRange", "value": "40.0 - 0.0", "flush": true } },
              { "variable": { "name": "graphMax", "value": "40.0 + 0.05 * (40.0 - 0.0)", "flush": true } },
              { "variable": { "name": "graphMin", "value": "0.0 - 0.05 * (40.0 - 0.0)", "flush": true } },

              { "variable": { "name": "marginL", "value": "20.0 * @vars.density + 2.0 * @vars.density" } },
              { "variable": { "name": "marginT", "value": "20.0 * @vars.density + 2.0 * @vars.density" } },

              { "save": {} },
              { "translate": { "dx": "@vars.marginL", "dy": "@vars.marginT" } },

              { "variable": { "name": "majorStepX_pow", "value": "pow(10.0, floor(log(@vars.xRange / 3.0)))", "flush": true } },

              { "variable": { "name": "scaleX", "value": "(@vars.w - 20.0 * @vars.density - 2.0 * @vars.density - @vars.marginL - 30.0 * @vars.density - 10.0 * @vars.density) / @vars.xRange", "flush": true } },
              { "variable": { "name": "offsetX", "value": "30.0 * @vars.density - 0.0 * @vars.scaleX", "flush": true } },
              { "variable": { "name": "scaleY", "value": "((10.0 * @vars.density + 50.0 * @vars.density) - (@vars.h - 20.0 * @vars.density - 2.0 * @vars.density - @vars.marginT)) / @vars.yRange", "flush": true } },
              { "variable": { "name": "offsetY", "value": "(@vars.h - 20.0 * @vars.density - 2.0 * @vars.density - @vars.marginT - 50.0 * @vars.density) - @vars.scaleY * 0.0", "flush": true } },

              { "paint": { "color": "#0000FF", "textSize": "@vars.density * 16.0" } },
              { "paint": { "color": "#444444", "width": 2.0 } },

              { "variable": { "name": "minorStepX_pow", "value": "pow(10.0, floor(log(@vars.xRange / 20.0)))", "flush": true } },

              { "variable": { "name": "y1", "value": "0.0 * @vars.scaleY + @vars.offsetY", "flush": true } },
              { "variable": { "name": "y2", "value": "40.0 * @vars.scaleY + @vars.offsetY", "flush": true } },

              { "variable": { "name": "startX", "value": "(0.0)", "flush": true } },
              { "variable": { "name": "minorStepX", "value": [1073741824, 1092616192, -8388560, 1101004800, -5177340, -5177329, -5177330, -5177336, -8388548, 1073741824, -5177341, -8388560, 1101004800, -5177340, -8388548, -5177340, 1073741824, -5177342, -5177318, -8388548, 1084227584, -5177341, -8388560, 1101004800, -5177340, -8388548, -5177340, 1084227584, -5177342, -5177318, -5177337], "flush": true } },
              { "variable": { "name": "endX", "value": "(50.0)", "flush": true } },

              {
                "loop": {
                  "from": "@vars.startX", "step": "@vars.minorStepX", "until": "@vars.endX",
                  "index": "index", "noIndexText": true,
                  "commands": [
                    { "variable": { "name": "plotX1", "value": "@vars.index * @vars.scaleX + @vars.offsetX" } },
                    { "drawLine": {
                      "x1": "@vars.plotX1",
                      "y1": "@vars.y1",
                      "x2": "@vars.plotX1",
                      "y2": "@vars.y2"
                    } }
                  ]
                }
              },

              { "paint": { "color": "#444444", "width": 2.0 } },

              { "variable": { "name": "minorStepY_pow", "value": "pow(10.0, floor(log(@vars.yRange / 20.0)))", "flush": true } },

              { "variable": { "name": "startX_scaled", "value": "@vars.startX * @vars.scaleX + @vars.offsetX", "flush": true } },
              { "variable": { "name": "endX_scaled", "value": "@vars.endX * @vars.scaleX + @vars.offsetX", "flush": true } },

              { "variable": { "name": "startY", "value": "(0.0)", "flush": true } },

              { "variable": { "name": "minorStepY", "value": [1092616192, -8388559, 1101004800, -5177340, -5177329, -5177330, -5177336, -8388540, 1073741824, -5177341, -8388559, 1101004800, -5177340, -8388540, -5177340, 1073741824, -5177342, -5177318, -8388540, 1084227584, -5177341, -8388559, 1101004800, -5177340, -8388540, -5177340, 1084227584, -5177342, -5177318], "flush": true } },

              {
                "loop": {
                  "from": "@vars.startY", "step": "@vars.minorStepY", "until": "40.0 + 0.01",
                  "index": "index", "noIndexText": true,
                  "commands": [
                    { "variable": { "name": "plotY2", "value": "@vars.index * @vars.scaleY + @vars.offsetY" } },
                    { "drawLine": {
                      "x1": "@vars.startX_scaled",
                      "y1": "@vars.plotY2",
                      "x2": "@vars.endX_scaled",
                      "y2": "@vars.plotY2"
                    } }
                  ]
                }
              },

              { "paint": { "color": "#888888", "width": 4.0 } },

              { "variable": { "name": "majorStepX", "value": [1073741824, 1092616192, -8388560, 1077936128, -5177340, -5177329, -5177330, -5177336, -8388554, 1073741824, -5177341, -8388560, 1077936128, -5177340, -8388554, -5177340, 1073741824, -5177342, -5177318, -8388554, 1084227584, -5177341, -8388560, 1077936128, -5177340, -8388554, -5177340, 1084227584, -5177342, -5177318, -5177337], "flush": true } },

              {
                "loop": {
                  "from": "@vars.startX", "step": "@vars.majorStepX", "until": "@vars.endX + 0.01",
                  "index": "index", "noIndexText": true,
                  "commands": [
                    { "variable": { "name": "plotX3", "value": "@vars.index * @vars.scaleX + @vars.offsetX" } },
                    { "variable": { "name": "lblId", "value": { "type": "textFromFloat", "value": "(@vars.index)", "whole": 5, "decimal": 1, "flags": 7 } } },
                    { "drawTextAnchored": {
                      "text": "@vars.lblId",
                      "x": "@vars.plotX3",
                      "y": "@vars.y1",
                      "panX": 0.0, "panY": 1.5, "flags": 0
                    } },
                    { "drawLine": {
                      "x1": "@vars.plotX3",
                      "y1": "@vars.y1",
                      "x2": "@vars.plotX3",
                      "y2": "@vars.y2"
                    } }
                  ]
                }
              },

              { "paint": { "color": "#888888", "width": 4.0 } },

              { "variable": { "name": "majorStepY_pow", "value": "pow(10.0, floor(log(@vars.yRange / 5.0)))", "flush": true } },
              { "variable": { "name": "end_scaled", "value": "@vars.startX * @vars.scaleX + @vars.offsetX", "flush": true } },
              { "variable": { "name": "majorStepY", "value": [1092616192, -8388559, 1084227584, -5177340, -5177329, -5177330, -5177336, -8388526, 1073741824, -5177341, -8388559, 1084227584, -5177340, -8388526, -5177340, 1073741824, -5177342, -5177318, -8388526, 1084227584, -5177341, -8388559, 1084227584, -5177340, -8388526, -5177340, 1084227584, -5177342, -5177318], "flush": true } },

              {
                "loop": {
                  "from": "@vars.startY", "step": "@vars.majorStepY", "until": "40.0 + 0.01",
                  "index": "index", "noIndexText": true,
                  "commands": [
                    { "variable": { "name": "plotY4", "value": "@vars.index * @vars.scaleY + @vars.offsetY" } },
                    { "variable": { "name": "lblIdY", "value": { "type": "textFromFloat", "value": "(@vars.index)", "whole": 5, "decimal": 1, "flags": 7 } } },
                    { "drawTextAnchored": {
                      "text": "@vars.lblIdY",
                      "x": "30.0 * @vars.density",
                      "y": "@vars.plotY4",
                      "panX": 1.5, "panY": 0.0, "flags": 0
                    } },
                    { "drawLine": {
                      "x1": "@vars.startX_scaled",
                      "y1": "@vars.plotY4",
                      "x2": "@vars.endX_scaled",
                      "y2": "@vars.plotY4"
                    } }
                  ]
                }
              },

              { "paint": { "color": "#FFFF00", "width": 4.0 } },
              { "drawLine": {
                "x1": "@vars.offsetX",
                "y1": "@vars.y1",
                "x2": "@vars.offsetX",
                "y2": "@vars.y2"
              } },
              { "paint": { "color": "#FFFF00", "width": 4.0 } },
              { "drawLine": {
                "x1": "@vars.startX_scaled",
                "y1": "@vars.offsetY",
                "x2": "@vars.endX_scaled",
                "y2": "@vars.offsetY"
              } },

              { "paint": { "color": "#000000", "style": "fill" } },
              {
                "loop": {
                  "from": 0.0, "step": 1.0, "until": "arrayLength(@vars.rx)",
                  "index": "index", "noIndexText": true,
                  "commands": [
                    { "drawCircle": {
                      "cx": "arrayGet(@vars.rx, @vars.index) * @vars.scaleX + @vars.offsetX",
                      "cy": "arrayGet(@vars.ry, @vars.index) * @vars.scaleY + @vars.offsetY",
                      "radius": "3.0 * @vars.density"
                    } }
                  ]
                }
              },

              { "paint": {
                "ops": [
                  { "shader": 0 },
                  { "color": "#FF0000" },
                  { "style": "stroke" },
                  { "width": "2.0 * @vars.density" },
                  { "color": "#0000FF" }
                ]
              } },
              { "paint": {
                "ops": [
                  { "shader": 0 },
                  { "color": "#FF0000" },
                  { "style": "stroke" },
                  { "width": "2.0 * @vars.density" }
                ]
              } },
              { "pathExpression": {
                "id": "regressionPath",
                "expressionX": "a[0] * @vars.scaleX + @vars.offsetX",
                "expressionY": "(a[0] * @vars.slope + @vars.intercept) * @vars.scaleY + @vars.offsetY",
                "start": "@vars.startX",
                "end": "@vars.endX",
                "count": 128.0,
                "flags": 4
              } },
              { "drawPath": { "path": "regressionPath" } },

              { "restore": {} },

              { "paint": { "color": "#000000", "textSize": "16.0 * @vars.density" } },
              { "variable": { "name": "slopeText", "value": { "type": "textFromFloat", "value": "@vars.slope", "whole": 5, "decimal": 2, "flags": 0 }, "commit": true } },
              { "variable": { "name": "interceptText", "value": { "type": "textFromFloat", "value": "@vars.intercept", "whole": 5, "decimal": 2, "flags": 0 }, "commit": true } },
              { "variable": { "name": "fullFormula", "value": { "type": "textMerge", "id1": "y = ", "id2": { "type": "textMerge", "id1": "@vars.slopeText", "id2": { "type": "textMerge", "id1": "x + ", "id2": "@vars.interceptText" } } } } },

              { "drawTextAnchored": {
                "text": "@vars.fullFormula",
                "x": "50.0 * @vars.density",
                "y": "50.0 * @vars.density",
                "panX": 0.0, "panY": 0.0, "flags": 0
              } }
            ]
          }
        }
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
// Force recompilation marker 57
