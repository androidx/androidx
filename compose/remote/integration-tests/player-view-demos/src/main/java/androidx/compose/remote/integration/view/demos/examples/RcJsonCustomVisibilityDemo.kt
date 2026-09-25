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

/**
 * Simple demo showing component visibility animations driven by custom enter/exit functions defined
 * in JSON via `defineVisibilityAnimation` and referenced in `animationSpec`.
 */
@Suppress("RestrictedApiAndroidX")
@JvmOverloads
fun rcJsonCustomVisibilityDemo(
    platform: RcPlatformServices = AndroidxRcPlatformServices()
): RemoteComposeContext {
    val json =
        """
        {
          "header": {
            "apiLevel": 8,
            "width": 360,
            "height": 520,
            "profiles": 513,
            "densityBehavior": 2
          },
          "root": [
            {
              "variable": { "name": "card1Vis", "vtype": "integer", "value": 1 }
            },
            {
              "variable": { "name": "card2Vis", "vtype": "integer", "value": 1 }
            },
            {
              "variable": { "name": "card3Vis", "vtype": "integer", "value": 1 }
            },
            {
              "variable": { "name": "card4Vis", "vtype": "integer", "value": 1 }
            },
            {
              "defineVisibilityAnimation": {
                "name": "cloudPuffExit",
                "params": ["progress", "w", "h", "x", "y"],
                "commands": [
                  { "variable": { "name": "cx", "value": "@x + @w / 2" } },
                  { "variable": { "name": "cy", "value": "@y + @h / 2" } },
                  { "variable": { "name": "pad", "value": "@w * 0.7" } },
                  { "variable": { "name": "cardAlpha", "value": "max(0, 1 - @progress * 2.8)" } },
                  { "variable": { "name": "squashX", "value": "max(0, 1 + 0.35 * sin(@progress * 18) - @progress * 2.7)" } },
                  { "variable": { "name": "squashY", "value": "max(0, 1 - 0.25 * sin(@progress * 18) - @progress * 2.7)" } },
                  { "variable": { "name": "wobble", "value": "sin(@progress * 28) * 16 * @progress" } },
                  {
                    "save": [
                      { "paint": { "alpha": "@cardAlpha" } },
                      { "rotate": { "angle": "@wobble", "pivotX": "@cx", "pivotY": "@cy" } },
                      { "scale": { "sx": "@squashX", "sy": "@squashY", "pivotX": "@cx", "pivotY": "@cy" } },
                      { "type": "drawComponentContent" }
                    ]
                  },
                  { "variable": { "name": "burst", "value": "1 - pow(1 - @progress, 3)" } },
                  { "variable": { "name": "smokeAlpha", "value": "min(1, @progress * 6.5) * pow(1 - @progress, 1.35)" } },
                  { "variable": { "name": "flashAlpha", "value": "max(0, 1 - @progress * 3.2)" } },
                  { "variable": { "name": "driftY", "value": "-@progress * @h * 0.45" } },
                  { "variable": { "name": "shockR", "value": "(@w * 0.65) * @burst" } },
                  {
                    "save": [
                      {
                        "paint": {
                          "color": "#FFF59D",
                          "style": "fill",
                          "alpha": "@flashAlpha * 0.85"
                        }
                      },
                      { "drawCircle": { "cx": "@cx", "cy": "@cy", "radius": "(@h * 0.75) * (0.3 + @progress)" } },
                      {
                        "paint": {
                          "color": "#FFD54F",
                          "style": "stroke",
                          "strokeWidth": "12 * (1 - @progress)",
                          "alpha": "@flashAlpha"
                        }
                      },
                      { "drawCircle": { "cx": "@cx", "cy": "@cy", "radius": "@shockR" } }
                    ]
                  },
                  {
                    "loop": {
                      "from": 0,
                      "until": 10,
                      "step": 1,
                      "index": "s",
                      "commands": [
                        { "variable": { "name": "sAng", "value": "@s * 0.628318 + 0.2" } },
                        { "variable": { "name": "r1", "value": "(@w * 0.25) + (@w * 0.38) * @burst" } },
                        { "variable": { "name": "r2", "value": "@r1 + (@h * 0.45) * (1 - @progress * 0.5)" } },
                        { "variable": { "name": "lx1", "value": "@cx + cos(@sAng) * @r1" } },
                        { "variable": { "name": "ly1", "value": "@cy + sin(@sAng) * @r1 * 0.75 + @driftY" } },
                        { "variable": { "name": "lx2", "value": "@cx + cos(@sAng) * @r2" } },
                        { "variable": { "name": "ly2", "value": "@cy + sin(@sAng) * @r2 * 0.75 + @driftY" } },
                        {
                          "save": [
                            {
                              "paint": {
                                "color": "#FFFDE7",
                                "style": "stroke",
                                "strokeWidth": 3.5,
                                "alpha": "@smokeAlpha * 0.85"
                              }
                            },
                            { "drawLine": { "x1": "@lx1", "y1": "@ly1", "x2": "@lx2", "y2": "@ly2" } },
                            {
                              "paint": {
                                "color": "#FFB300",
                                "style": "fill",
                                "alpha": "@smokeAlpha"
                              }
                            },
                            {
                              "drawCircle": {
                                "cx": "@lx2",
                                "cy": "@ly2",
                                "radius": "(@h * 0.09) * (1 - @progress)"
                              }
                            }
                          ]
                        }
                      ]
                    }
                  },
                  {
                    "save": [
                      {
                        "paint": {
                          "color": "#546E7A",
                          "style": "fill",
                          "alpha": "@smokeAlpha * 0.85"
                        }
                      },
                      {
                        "drawCircle": {
                          "cx": "@cx",
                          "cy": "@cy + @driftY + 8",
                          "radius": "(@h * 0.62) * (0.5 + 0.6 * @burst) * (1 - @progress * 0.3)"
                        }
                      },
                      {
                        "paint": {
                          "color": "#ECEFF1",
                          "style": "fill",
                          "alpha": "@smokeAlpha * 0.95"
                        }
                      },
                      {
                        "drawCircle": {
                          "cx": "@cx - @w * 0.12 * @burst",
                          "cy": "@cy + @driftY - 4",
                          "radius": "(@h * 0.55) * (0.5 + 0.5 * @burst) * (1 - @progress * 0.35)"
                        }
                      },
                      {
                        "drawCircle": {
                          "cx": "@cx + @w * 0.12 * @burst",
                          "cy": "@cy + @driftY - 6",
                          "radius": "(@h * 0.52) * (0.5 + 0.5 * @burst) * (1 - @progress * 0.35)"
                        }
                      }
                    ]
                  },
                  {
                    "loop": {
                      "from": 0,
                      "until": 12,
                      "step": 1,
                      "index": "i",
                      "commands": [
                        { "variable": { "name": "ang", "value": "@i * 0.523598 + @progress * 0.6" } },
                        { "variable": { "name": "alt", "value": "0.68 + 0.32 * sin(@i * 2.3)" } },
                        { "variable": { "name": "rx", "value": "(@w * 0.54) * @burst * @alt" } },
                        { "variable": { "name": "ry", "value": "(@h * 0.72) * @burst * @alt" } },
                        { "variable": { "name": "px", "value": "@cx + cos(@ang) * @rx" } },
                        { "variable": { "name": "py", "value": "@cy + sin(@ang) * @ry + @driftY" } },
                        { "variable": { "name": "pSize", "value": "(@h * 0.44) * (0.45 + 0.75 * sin(@progress * 2.6)) * (0.78 + 0.25 * cos(@i * 1.7))" } },
                        {
                          "save": [
                            {
                              "paint": {
                                "color": "#455A64",
                                "style": "fill",
                                "alpha": "@smokeAlpha * 0.9"
                              }
                            },
                            {
                              "drawCircle": {
                                "cx": "@px + cos(@ang) * 7",
                                "cy": "@py + 10",
                                "radius": "@pSize * 1.08"
                              }
                            },
                            {
                              "paint": {
                                "color": "#CFD8DC",
                                "style": "fill",
                                "alpha": "@smokeAlpha * 0.96"
                              }
                            },
                            {
                              "drawCircle": {
                                "cx": "@px",
                                "cy": "@py",
                                "radius": "@pSize"
                              }
                            },
                            {
                              "paint": {
                                "color": "#FFFFFF",
                                "style": "fill",
                                "alpha": "@smokeAlpha"
                              }
                            },
                            {
                              "drawCircle": {
                                "cx": "@px - cos(@ang) * 5",
                                "cy": "@py - 6",
                                "radius": "@pSize * 0.72"
                              }
                            }
                          ]
                        }
                      ]
                    }
                  }
                ]
              }
            },
            {
              "defineVisibilityAnimation": {
                "name": "cloudPuffEnter",
                "params": ["progress", "w", "h", "x", "y"],
                "commands": [
                  { "variable": { "name": "cx", "value": "@x + @w / 2" } },
                  { "variable": { "name": "cy", "value": "@y + @h / 2" } },
                  { "variable": { "name": "pad", "value": "@w * 0.7" } },
                  { "variable": { "name": "cardAlpha", "value": "min(1, max(0, (@progress - 0.15) * 1.6))" } },
                  { "variable": { "name": "scaleVal", "value": "min(1, max(0, (@progress - 0.1) * 1.25)) + 0.12 * sin(@progress * 3.14159)" } },
                  {
                    "save": [
                      { "paint": { "alpha": "@cardAlpha" } },
                      { "scale": { "sx": "@scaleVal", "sy": "@scaleVal", "pivotX": "@cx", "pivotY": "@cy" } },
                      { "type": "drawComponentContent" }
                    ]
                  },
                  { "variable": { "name": "burst", "value": "1 - pow(1 - @progress, 2.6)" } },
                  { "variable": { "name": "smokeAlpha", "value": "sin(@progress * 3.14159) * (1 - @progress * 0.25)" } },
                  { "variable": { "name": "driftY", "value": "-@progress * @h * 0.35" } },
                  {
                    "loop": {
                      "from": 0,
                      "until": 10,
                      "step": 1,
                      "index": "i",
                      "commands": [
                        { "variable": { "name": "ang", "value": "@i * 0.628318 - @progress * 0.5" } },
                        { "variable": { "name": "alt", "value": "0.7 + 0.3 * sin(@i * 2.1)" } },
                        { "variable": { "name": "rx", "value": "(@w * 0.52) * @burst * @alt" } },
                        { "variable": { "name": "ry", "value": "(@h * 0.65) * @burst * @alt" } },
                        { "variable": { "name": "px", "value": "@cx + cos(@ang) * @rx" } },
                        { "variable": { "name": "py", "value": "@cy + sin(@ang) * @ry + @driftY" } },
                        { "variable": { "name": "pSize", "value": "(@h * 0.4) * (1 - @progress * 0.45) * (0.8 + 0.2 * cos(@i * 1.5))" } },
                        {
                          "save": [
                            {
                              "paint": {
                                "color": "#546E7A",
                                "style": "fill",
                                "alpha": "@smokeAlpha * 0.85"
                              }
                            },
                            {
                              "drawCircle": {
                                "cx": "@px + 5",
                                "cy": "@py + 8",
                                "radius": "@pSize * 1.05"
                              }
                            },
                            {
                              "paint": {
                                "color": "#ECEFF1",
                                "style": "fill",
                                "alpha": "@smokeAlpha * 0.95"
                              }
                            },
                            {
                              "drawCircle": {
                                "cx": "@px",
                                "cy": "@py",
                                "radius": "@pSize"
                              }
                            },
                            {
                              "paint": {
                                "color": "#FFFFFF",
                                "style": "fill",
                                "alpha": "@smokeAlpha"
                              }
                            },
                            {
                              "drawCircle": {
                                "cx": "@px - 4",
                                "cy": "@py - 5",
                                "radius": "@pSize * 0.7"
                              }
                            }
                          ]
                        }
                      ]
                    }
                  }
                ]
              }
            },
            {
              "defineVisibilityAnimation": {
                "name": "blockShatterExit",
                "params": ["progress", "w", "h", "x", "y"],
                "commands": [
                  { "variable": { "name": "t", "value": "@progress" } },
                  { "variable": { "name": "tileW", "value": "@w / 6" } },
                  { "variable": { "name": "tileH", "value": "@h / 2" } },
                  { "variable": { "name": "bScale", "value": "max(0, 1 - pow(@t, 2.6))" } },
                  { "variable": { "name": "extrude", "value": "(@tileH * 0.38) * min(1, @t * 5.0) * @bScale" } },
                  { "variable": { "name": "edgeAlpha", "value": "min(1, @t * 8.0)" } },
                  {
                    "loop": {
                      "from": 0,
                      "until": 12,
                      "step": 1,
                      "index": "i",
                      "commands": [
                        { "variable": { "name": "row", "value": "max(0, min(1, (@i - 5.5) * 10))" } },
                        { "variable": { "name": "col", "value": "@i - @row * 6" } },
                        { "variable": { "name": "tileLeft", "value": "@x + @col * @tileW" } },
                        { "variable": { "name": "tileTop", "value": "@y + @row * @tileH" } },
                        { "variable": { "name": "tileRight", "value": "@tileLeft + @tileW" } },
                        { "variable": { "name": "tileBottom", "value": "@tileTop + @tileH" } },
                        { "variable": { "name": "startX", "value": "@tileLeft + @tileW * 0.5" } },
                        { "variable": { "name": "startY", "value": "@tileTop + @tileH * 0.5" } },
                        { "variable": { "name": "vx", "value": "(@col - 2.5) * (@w * 0.24) + sin(@i * 2.7) * (@w * 0.06)" } },
                        { "variable": { "name": "vy", "value": "(-@h * 1.45) + @row * (@h * 0.38) - abs(cos(@i * 1.9)) * (@h * 0.52)" } },
                        { "variable": { "name": "gravity", "value": "(@h * 4.4) * @t * @t" } },
                        { "variable": { "name": "bx", "value": "@startX + @vx * @t" } },
                        { "variable": { "name": "by", "value": "@startY + @vy * @t + @gravity" } },
                        { "variable": { "name": "rot", "value": "((@col - 2.5) * 85 + sin(@i * 3.1) * 95) * @t" } },
                        { "variable": { "name": "sx", "value": "@bScale * (1 - 0.18 * sin(@t * 5.0 + @i * 0.7) * min(1, @t * 4))" } },
                        { "variable": { "name": "sy", "value": "@bScale * (1 - 0.18 * sin(@t * 6.0 + @i * 1.1) * min(1, @t * 4))" } },
                        { "variable": { "name": "dx3d", "value": "@extrude * (0.75 * cos(@t * 4.5 + @i * 0.9) + (@col - 2.5) * 0.18)" } },
                        { "variable": { "name": "dy3d", "value": "@extrude * (0.75 * sin(@t * 4.5 + @i * 0.9) + 0.45)" } },
                        {
                          "save": [
                            { "translate": { "dx": "@bx - @startX", "dy": "@by - @startY" } },
                            { "rotate": { "angle": "@rot", "pivotX": "@startX", "pivotY": "@startY" } },
                            { "scale": { "sx": "@sx", "sy": "@sy", "pivotX": "@startX", "pivotY": "@startY" } },
                            {
                              "paint": {
                                "color": "#0D1440",
                                "style": "fill",
                                "alpha": "@edgeAlpha"
                              }
                            },
                            {
                              "drawRoundRect": {
                                "left": "@tileLeft + @dx3d",
                                "top": "@tileTop + @dy3d",
                                "right": "@tileRight + @dx3d",
                                "bottom": "@tileBottom + @dy3d",
                                "rx": 3,
                                "ry": 3
                              }
                            },
                            {
                              "paint": {
                                "color": "#1A237E",
                                "style": "fill",
                                "alpha": "@edgeAlpha"
                              }
                            },
                            {
                              "drawRoundRect": {
                                "left": "@tileLeft + @dx3d * 0.66",
                                "top": "@tileTop + @dy3d * 0.66",
                                "right": "@tileRight + @dx3d * 0.66",
                                "bottom": "@tileBottom + @dy3d * 0.66",
                                "rx": 3,
                                "ry": 3
                              }
                            },
                            {
                              "paint": {
                                "color": "#283593",
                                "style": "fill",
                                "alpha": "@edgeAlpha"
                              }
                            },
                            {
                              "drawRoundRect": {
                                "left": "@tileLeft + @dx3d * 0.33",
                                "top": "@tileTop + @dy3d * 0.33",
                                "right": "@tileRight + @dx3d * 0.33",
                                "bottom": "@tileBottom + @dy3d * 0.33",
                                "rx": 3,
                                "ry": 3
                              }
                            },
                            {
                              "paint": {
                                "color": "#3F51B5",
                                "style": "fill",
                                "alpha": "@edgeAlpha"
                              }
                            },
                            {
                              "drawRect": {
                                "left": "@tileLeft",
                                "top": "@tileTop",
                                "right": "@tileRight",
                                "bottom": "@tileBottom"
                              }
                            },
                            {
                              "save": [
                                { "paint": { "alpha": "@bScale" } },
                                {
                                  "clipRect": {
                                    "left": "@tileLeft",
                                    "top": "@tileTop",
                                    "right": "@tileRight",
                                    "bottom": "@tileBottom"
                                  }
                                },
                                { "type": "drawComponentContent" }
                              ]
                            },
                            {
                              "paint": {
                                "color": "#9FA8DA",
                                "style": "stroke",
                                "strokeWidth": 1.5,
                                "alpha": "@edgeAlpha * 0.85"
                              }
                            },
                            {
                              "drawRect": {
                                "left": "@tileLeft",
                                "top": "@tileTop",
                                "right": "@tileRight",
                                "bottom": "@tileBottom"
                              }
                            }
                          ]
                        }
                      ]
                    }
                  }
                ]
              }
            },
            {
              "defineVisibilityAnimation": {
                "name": "blockShatterEnter",
                "params": ["progress", "w", "h", "x", "y"],
                "commands": [
                  { "variable": { "name": "revT", "value": "pow(max(0, 1 - @progress), 1.65)" } },
                  { "variable": { "name": "tileW", "value": "@w / 6" } },
                  { "variable": { "name": "tileH", "value": "@h / 2" } },
                  { "variable": { "name": "bScale", "value": "min(1, @progress * 3.2)" } },
                  { "variable": { "name": "extrude", "value": "(@tileH * 0.38) * min(1, @revT * 4.0) * @bScale" } },
                  { "variable": { "name": "edgeAlpha", "value": "min(1, @revT * 6.0)" } },
                  {
                    "loop": {
                      "from": 0,
                      "until": 12,
                      "step": 1,
                      "index": "i",
                      "commands": [
                        { "variable": { "name": "row", "value": "max(0, min(1, (@i - 5.5) * 10))" } },
                        { "variable": { "name": "col", "value": "@i - @row * 6" } },
                        { "variable": { "name": "tileLeft", "value": "@x + @col * @tileW" } },
                        { "variable": { "name": "tileTop", "value": "@y + @row * @tileH" } },
                        { "variable": { "name": "tileRight", "value": "@tileLeft + @tileW" } },
                        { "variable": { "name": "tileBottom", "value": "@tileTop + @tileH" } },
                        { "variable": { "name": "startX", "value": "@tileLeft + @tileW * 0.5" } },
                        { "variable": { "name": "startY", "value": "@tileTop + @tileH * 0.5" } },
                        { "variable": { "name": "vx", "value": "(@col - 2.5) * (@w * 0.22)" } },
                        { "variable": { "name": "vy", "value": "(-@h * 1.15) + @row * (@h * 0.35)" } },
                        { "variable": { "name": "gravity", "value": "(@h * 3.4) * @revT * @revT" } },
                        { "variable": { "name": "bx", "value": "@startX + @vx * @revT" } },
                        { "variable": { "name": "by", "value": "@startY + @vy * @revT + @gravity" } },
                        { "variable": { "name": "rot", "value": "((@col - 2.5) * 75) * @revT" } },
                        { "variable": { "name": "sx", "value": "@bScale * (1 - 0.16 * sin(@revT * 5.0 + @i * 0.7) * min(1, @revT * 4))" } },
                        { "variable": { "name": "sy", "value": "@bScale * (1 - 0.16 * sin(@revT * 6.0 + @i * 1.1) * min(1, @revT * 4))" } },
                        { "variable": { "name": "dx3d", "value": "@extrude * (0.75 * cos(@revT * 4.5 + @i * 0.9) + (@col - 2.5) * 0.18)" } },
                        { "variable": { "name": "dy3d", "value": "@extrude * (0.75 * sin(@revT * 4.5 + @i * 0.9) + 0.45)" } },
                        {
                          "save": [
                            { "translate": { "dx": "@bx - @startX", "dy": "@by - @startY" } },
                            { "rotate": { "angle": "@rot", "pivotX": "@startX", "pivotY": "@startY" } },
                            { "scale": { "sx": "@sx", "sy": "@sy", "pivotX": "@startX", "pivotY": "@startY" } },
                            {
                              "paint": {
                                "color": "#0D1440",
                                "style": "fill",
                                "alpha": "@edgeAlpha"
                              }
                            },
                            {
                              "drawRoundRect": {
                                "left": "@tileLeft + @dx3d",
                                "top": "@tileTop + @dy3d",
                                "right": "@tileRight + @dx3d",
                                "bottom": "@tileBottom + @dy3d",
                                "rx": 3,
                                "ry": 3
                              }
                            },
                            {
                              "paint": {
                                "color": "#1A237E",
                                "style": "fill",
                                "alpha": "@edgeAlpha"
                              }
                            },
                            {
                              "drawRoundRect": {
                                "left": "@tileLeft + @dx3d * 0.66",
                                "top": "@tileTop + @dy3d * 0.66",
                                "right": "@tileRight + @dx3d * 0.66",
                                "bottom": "@tileBottom + @dy3d * 0.66",
                                "rx": 3,
                                "ry": 3
                              }
                            },
                            {
                              "paint": {
                                "color": "#283593",
                                "style": "fill",
                                "alpha": "@edgeAlpha"
                              }
                            },
                            {
                              "drawRoundRect": {
                                "left": "@tileLeft + @dx3d * 0.33",
                                "top": "@tileTop + @dy3d * 0.33",
                                "right": "@tileRight + @dx3d * 0.33",
                                "bottom": "@tileBottom + @dy3d * 0.33",
                                "rx": 3,
                                "ry": 3
                              }
                            },
                            {
                              "paint": {
                                "color": "#3F51B5",
                                "style": "fill",
                                "alpha": "@edgeAlpha"
                              }
                            },
                            {
                              "drawRect": {
                                "left": "@tileLeft",
                                "top": "@tileTop",
                                "right": "@tileRight",
                                "bottom": "@tileBottom"
                              }
                            },
                            {
                              "save": [
                                { "paint": { "alpha": "@bScale" } },
                                {
                                  "clipRect": {
                                    "left": "@tileLeft",
                                    "top": "@tileTop",
                                    "right": "@tileRight",
                                    "bottom": "@tileBottom"
                                  }
                                },
                                { "type": "drawComponentContent" }
                              ]
                            },
                            {
                              "paint": {
                                "color": "#9FA8DA",
                                "style": "stroke",
                                "strokeWidth": 1.5,
                                "alpha": "@edgeAlpha * 0.85"
                              }
                            },
                            {
                              "drawRect": {
                                "left": "@tileLeft",
                                "top": "@tileTop",
                                "right": "@tileRight",
                                "bottom": "@tileBottom"
                              }
                            }
                          ]
                        }
                      ]
                    }
                  }
                ]
              }
            },
            {
              "defineVisibilityAnimation": {
                "name": "shaderMeltEnter",
                "params": ["progress", "w", "h", "x", "y", "id"],
                "commands": [
                  { "createOffscreenBitmap": "offscreenBitmap" },
                  { "variable": { "name": "pMelt", "value": "1 - @progress" } },
                  { "drawComponentToBitmap": { "id": "@id", "bitmap": "@offscreenBitmap" } },
                  {
                    "save": [
                      { "translate": { "dx": "@x", "dy": "@y" } },
                      {
                        "paint": {
                          "shader": {
                            "agsl": "uniform shader uTexture; uniform float uProgress; uniform float2 uResolution; half4 sampleClamped(vec2 coord, vec2 res) { if (coord.x < -1.0 || coord.x >= res.x + 2.0 || coord.y < -1.0 || coord.y >= res.y + 2.0) { return half4(0.0); } return uTexture.eval(coord); } half4 main(vec2 fragCoord) { if (uResolution.x <= 0.0 || uResolution.y <= 0.0) { return uTexture.eval(fragCoord); } vec2 uv = fragCoord / uResolution; float env = smoothstep(0.0, 0.16, uProgress); float waveDist = length(uv - vec2(0.5, 0.5)); float wave = sin(waveDist * 20.0 - uProgress * 10.0) * 0.04 * uProgress * env; vec2 distortedCoord = fragCoord + vec2(wave * uResolution.y, wave * uResolution.x); float split = uProgress * 8.0 * env; half4 colR = sampleClamped(distortedCoord + vec2(split, 0.0), uResolution); half4 colG = sampleClamped(distortedCoord, uResolution); half4 colB = sampleClamped(distortedCoord - vec2(split, 0.0), uResolution); half4 color = half4(colR.r, colG.g, colB.b, colG.a); float plasma = sin(uv.x * 16.0 + uProgress * 5.0) * cos(uv.y * 16.0 + uProgress * 3.0) * 0.12 * env; float threshold = uv.y + plasma; float cutoff = mix(1.20, -0.20, uProgress); float edgeWidth = 0.08; float dissolve = smoothstep(cutoff, cutoff + edgeWidth, threshold); float borderDist = abs(threshold - cutoff); float glow = smoothstep(edgeWidth, 0.0, borderDist) * env * (1.0 - smoothstep(0.85, 1.0, uProgress)); vec3 glowCol = vec3(0.0, 0.95, 1.0) * glow * 2.5; float fadeAlpha = 1.0 - smoothstep(0.15, 1.0, uProgress) * 0.8; float finalAlpha = (1.0 - dissolve) * color.a * fadeAlpha; vec3 finalRgb = (color.rgb * (1.0 - dissolve)) + glowCol * color.a; return half4(finalRgb, finalAlpha); }",
                            "uniforms": {
                              "uTexture": "@offscreenBitmap",
                              "uProgress": "@pMelt",
                              "uResolution": ["@w", "@h"]
                            }
                          }
                        }
                      },
                      {
                        "drawRect": {
                          "left": 0,
                          "top": 0,
                          "right": "@w",
                          "bottom": "@h"
                        }
                      },
                      { "paint": { "shader": 0 } }
                    ]
                  }
                ]
              }
            },
            {
              "defineVisibilityAnimation": {
                "name": "shaderMeltExit",
                "params": ["progress", "w", "h", "x", "y", "id"],
                "commands": [
                  { "createOffscreenBitmap": "offscreenBitmap" },
                  { "drawComponentToBitmap": { "id": "@id", "bitmap": "@offscreenBitmap" } },
                  {
                    "save": [
                      { "translate": { "dx": "@x", "dy": "@y" } },
                      {
                        "paint": {
                          "shader": {
                            "agsl": "uniform shader uTexture; uniform float uProgress; uniform float2 uResolution; half4 sampleClamped(vec2 coord, vec2 res) { if (coord.x < -1.0 || coord.x >= res.x + 2.0 || coord.y < -1.0 || coord.y >= res.y + 2.0) { return half4(0.0); } return uTexture.eval(coord); } half4 main(vec2 fragCoord) { if (uResolution.x <= 0.0 || uResolution.y <= 0.0) { return uTexture.eval(fragCoord); } vec2 uv = fragCoord / uResolution; float env = smoothstep(0.0, 0.16, uProgress); float waveDist = length(uv - vec2(0.5, 0.5)); float wave = sin(waveDist * 20.0 - uProgress * 10.0) * 0.04 * uProgress * env; vec2 distortedCoord = fragCoord + vec2(wave * uResolution.y, wave * uResolution.x); float split = uProgress * 8.0 * env; half4 colR = sampleClamped(distortedCoord + vec2(split, 0.0), uResolution); half4 colG = sampleClamped(distortedCoord, uResolution); half4 colB = sampleClamped(distortedCoord - vec2(split, 0.0), uResolution); half4 color = half4(colR.r, colG.g, colB.b, colG.a); float plasma = sin(uv.x * 16.0 + uProgress * 5.0) * cos(uv.y * 16.0 + uProgress * 3.0) * 0.12 * env; float threshold = uv.y + plasma; float cutoff = mix(1.20, -0.20, uProgress); float edgeWidth = 0.08; float dissolve = smoothstep(cutoff, cutoff + edgeWidth, threshold); float borderDist = abs(threshold - cutoff); float glow = smoothstep(edgeWidth, 0.0, borderDist) * env * (1.0 - smoothstep(0.85, 1.0, uProgress)); vec3 glowCol = vec3(0.0, 0.95, 1.0) * glow * 2.5; float fadeAlpha = 1.0 - smoothstep(0.15, 1.0, uProgress) * 0.8; float finalAlpha = (1.0 - dissolve) * color.a * fadeAlpha; vec3 finalRgb = (color.rgb * (1.0 - dissolve)) + glowCol * color.a; return half4(finalRgb, finalAlpha); }",
                            "uniforms": {
                              "uTexture": "@offscreenBitmap",
                              "uProgress": "@progress",
                              "uResolution": ["@w", "@h"]
                            }
                          }
                        }
                      },
                      {
                        "drawRect": {
                          "left": 0,
                          "top": 0,
                          "right": "@w",
                          "bottom": "@h"
                        }
                      },
                      { "paint": { "shader": 0 } }
                    ]
                  }
                ]
              }
            },
            {
              "defineVisibilityAnimation": {
                "name": "liquidFillEnter",
                "params": ["progress", "w", "h", "x", "y", "id"],
                "commands": [
                  { "createOffscreenBitmap": "offscreenBitmap" },
                  { "drawComponentToBitmap": { "id": "@id", "bitmap": "@offscreenBitmap" } },
                  {
                    "save": [
                      { "translate": { "dx": "@x", "dy": "@y" } },
                      {
                        "paint": {
                          "shader": {
                            "agsl": "uniform shader uTexture; uniform float uProgress; uniform float2 uResolution; float hash21(vec2 p) { p = fract(p * vec2(123.34, 456.21)); p += dot(p, p + 45.32); return fract(p.x * p.y); } half4 sampleClamped(vec2 coord, vec2 res) { if (coord.x < -1.0 || coord.x >= res.x + 2.0 || coord.y < -1.0 || coord.y >= res.y + 2.0) { return half4(0.0); } return uTexture.eval(coord); } half4 main(vec2 fragCoord) { if (uResolution.x <= 0.0 || uResolution.y <= 0.0) return uTexture.eval(fragCoord); vec2 uv = fragCoord / uResolution; float p = uProgress; float h = uResolution.y; float settle = 1.0 - smoothstep(0.75, 0.97, p); float baseLevel = (1.0 - smoothstep(0.0, 0.95, p)) * h; float waveDecay = settle; float wave1 = sin(uv.x * 13.0 - p * 15.0) * (h * 0.065) * waveDecay; float wave2 = cos(uv.x * 24.0 + p * 21.0) * (h * 0.035) * waveDecay; float wave3 = sin((uv.x - 0.5) * 7.0 - p * 10.0) * (h * 0.045) * waveDecay; float surfaceY = baseLevel + wave1 + wave2 + wave3; if (fragCoord.y < surfaceY) { return half4(0.0); } float colWidth = h * 0.22; float colIdx = floor(fragCoord.x / colWidth); float hBubble = hash21(vec2(colIdx, 3.77)); float bubbleSpeed = mix(1.0, 2.1, hBubble); float bubbleProgress = fract(p * 2.0 * bubbleSpeed + hBubble); float bubbleRadius = mix(h * 0.032, h * 0.068, hBubble); float bubbleY = h + bubbleRadius - bubbleProgress * (h - surfaceY + bubbleRadius * 2.5); vec2 bubblePos = vec2((colIdx + 0.5) * colWidth + sin(p * 9.0 + hBubble * 6.28) * (colWidth * 0.2), bubbleY); float distToBubble = length(fragCoord - bubblePos); float bubbleFill = smoothstep(bubbleRadius, bubbleRadius * 0.6, distToBubble); float bubbleRim = smoothstep(bubbleRadius, bubbleRadius * 0.78, distToBubble) - smoothstep(bubbleRadius * 0.78, bubbleRadius * 0.4, distToBubble); float isBubble = (bubbleFill * 0.45 + bubbleRim * 1.1) * step(surfaceY, fragCoord.y) * settle; float shimmer = sin(uv.y * 18.0 - p * 12.0) * cos(uv.x * 16.0) * (h * 0.02) * settle; vec2 sampleCoord = fragCoord + vec2(shimmer, 0.0); half4 texCol = sampleClamped(sampleCoord, uResolution); if (texCol.a <= 0.0) return half4(0.0); float distToSurface = abs(fragCoord.y - surfaceY); float surfaceGlow = smoothstep(h * 0.075, 0.0, distToSurface) * settle; float crestHighlight = smoothstep(h * 0.025, 0.0, distToSurface) * 2.2 * settle; vec3 waveCol = mix(vec3(0.45, 0.9, 1.0), vec3(1.0), 0.8) * (surfaceGlow + crestHighlight); vec3 bubbleCol = vec3(0.9, 1.0, 1.0) * isBubble * 1.5; vec3 rgb = texCol.rgb + (waveCol + bubbleCol) * texCol.a; float alpha = texCol.a; return half4(rgb, alpha); }",
                            "uniforms": {
                              "uTexture": "@offscreenBitmap",
                              "uProgress": "@progress",
                              "uResolution": ["@w", "@h"]
                            }
                          }
                        }
                      },
                      {
                        "drawRect": {
                          "left": "-@h * 0.5",
                          "top": "-@h * 0.5",
                          "right": "@w + @h",
                          "bottom": "@h + @h"
                        }
                      },
                      { "paint": { "shader": 0 } }
                    ]
                  }
                ]
              }
            },
            {
              "defineVisibilityAnimation": {
                "name": "liquidDrainExit",
                "params": ["progress", "w", "h", "x", "y", "id"],
                "commands": [
                  { "createOffscreenBitmap": "offscreenBitmap" },
                  { "drawComponentToBitmap": { "id": "@id", "bitmap": "@offscreenBitmap" } },
                  {
                    "save": [
                      { "translate": { "dx": "@x", "dy": "@y" } },
                      {
                        "paint": {
                          "shader": {
                            "agsl": "uniform shader uTexture; uniform float uProgress; uniform float2 uResolution; float hash21(vec2 p) { p = fract(p * vec2(123.34, 456.21)); p += dot(p, p + 45.32); return fract(p.x * p.y); } half4 sampleClamped(vec2 coord, vec2 res) { if (coord.x < -1.0 || coord.x >= res.x + 2.0 || coord.y < -1.0 || coord.y >= res.y + 2.0) { return half4(0.0); } return uTexture.eval(coord); } half4 main(vec2 fragCoord) { if (uResolution.x <= 0.0 || uResolution.y <= 0.0) return uTexture.eval(fragCoord); vec2 uv = fragCoord / uResolution; float p = uProgress; float h = uResolution.y; float rampIn = smoothstep(0.0, 0.14, p); float rampOut = 1.0 - smoothstep(0.85, 0.98, p); float env = rampIn * rampOut; float baseLevel = smoothstep(0.04, 0.98, p) * h; float waveDecay = env; float wave1 = sin(uv.x * 13.0 + p * 15.0) * (h * 0.065) * waveDecay; float wave2 = cos(uv.x * 24.0 - p * 21.0) * (h * 0.035) * waveDecay; float wave3 = sin((uv.x - 0.5) * 7.0 + p * 10.0) * (h * 0.045) * waveDecay; float surfaceY = baseLevel + wave1 + wave2 + wave3; if (fragCoord.y < surfaceY) { return half4(0.0); } float colWidth = h * 0.22; float colIdx = floor(fragCoord.x / colWidth); float hBubble = hash21(vec2(colIdx, 5.13)); float bubbleSpeed = mix(1.0, 2.1, hBubble); float bubbleProgress = fract(p * 2.0 * bubbleSpeed + hBubble); float bubbleRadius = mix(h * 0.032, h * 0.068, hBubble); float bubbleY = h + bubbleRadius - bubbleProgress * (h - surfaceY + bubbleRadius * 2.5); vec2 bubblePos = vec2((colIdx + 0.5) * colWidth + sin(p * 9.0 + hBubble * 6.28) * (colWidth * 0.2), bubbleY); float distToBubble = length(fragCoord - bubblePos); float bubbleFill = smoothstep(bubbleRadius, bubbleRadius * 0.6, distToBubble); float bubbleRim = smoothstep(bubbleRadius, bubbleRadius * 0.78, distToBubble) - smoothstep(bubbleRadius * 0.78, bubbleRadius * 0.4, distToBubble); float isBubble = (bubbleFill * 0.45 + bubbleRim * 1.1) * step(surfaceY, fragCoord.y) * env; float shimmer = sin(uv.y * 18.0 + p * 12.0) * cos(uv.x * 16.0) * (h * 0.02) * env; vec2 sampleCoord = fragCoord + vec2(shimmer, 0.0); half4 texCol = sampleClamped(sampleCoord, uResolution); if (texCol.a <= 0.0) return half4(0.0); float distToSurface = abs(fragCoord.y - surfaceY); float surfaceGlow = smoothstep(h * 0.075, 0.0, distToSurface) * env; float crestHighlight = smoothstep(h * 0.025, 0.0, distToSurface) * 2.2 * env; vec3 waveCol = mix(vec3(0.45, 0.9, 1.0), vec3(1.0), 0.8) * (surfaceGlow + crestHighlight); vec3 bubbleCol = vec3(0.9, 1.0, 1.0) * isBubble * 1.5; vec3 rgb = texCol.rgb + (waveCol + bubbleCol) * texCol.a; float alpha = texCol.a * (1.0 - smoothstep(0.92, 1.0, p)); return half4(rgb, alpha); }",
                            "uniforms": {
                              "uTexture": "@offscreenBitmap",
                              "uProgress": "@progress",
                              "uResolution": ["@w", "@h"]
                            }
                          }
                        }
                      },
                      {
                        "drawRect": {
                          "left": "-@h * 0.5",
                          "top": "-@h * 0.5",
                          "right": "@w + @h",
                          "bottom": "@h + @h"
                        }
                      },
                      { "paint": { "shader": 0 } }
                    ]
                  }
                ]
              }
            },
            {
              "column": {
                "modifiers": [
                  "fillMaxSize",
                  { "background": "#121212" },
                  { "padding": 16 }
                ],
                "horizontalAlignment": "center",
                "verticalArrangement": "top",
                "children": [
                  {
                    "text": {
                      "value": "Custom Visibility",
                      "fontSize": 20,
                      "color": "#FFFFFF"
                    }
                  },
                  {
                    "spacer": { "modifiers": [{ "size": 12 }] }
                  },
                  {
                    "box": {
                      "modifiers": [
                        { "width": 300 },
                        { "height": 64 },
                        { "visibility": "@card1Vis" },
                        {
                          "animationSpec": {
                            "motionDuration": 600,
                            "visibilityDuration": 1000,
                            "enterAnimation": "CUSTOM",
                            "exitAnimation": "CUSTOM",
                            "enterFunction": "cloudPuffEnter",
                            "exitFunction": "cloudPuffExit",
                            "enterSequence": "AFTER"
                          }
                        },
                        { "clip": { "type": "RoundedRect", "radius": 14 } },
                        { "background": "#FF5722" }
                      ],
                      "horizontalAlignment": "center",
                      "verticalArrangement": "center",
                      "children": [
                        {
                          "column": {
                            "horizontalAlignment": "center",
                            "verticalArrangement": "center",
                            "children": [
                              {
                                "text": {
                                  "value": "Card 1: Dramatic Cloud Puff",
                                  "fontSize": 16,
                                  "color": "#FFFFFF"
                                }
                              },
                              {
                                "text": {
                                  "value": "Poof! Billowing smoke cloud exit",
                                  "fontSize": 12,
                                  "color": "#FFE0B2"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  },
                  {
                    "spacer": { "modifiers": [{ "size": 12 }] }
                  },
                  {
                    "box": {
                      "modifiers": [
                        { "width": 300 },
                        { "height": 64 },
                        { "visibility": "@card2Vis" },
                        {
                          "animationSpec": {
                            "motionDuration": 600,
                            "visibilityDuration": 1000,
                            "enterAnimation": "CUSTOM",
                            "exitAnimation": "CUSTOM",
                            "enterFunction": "blockShatterEnter",
                            "exitFunction": "blockShatterExit",
                            "enterSequence": "AFTER"
                          }
                        },
                        { "clip": { "type": "RoundedRect", "radius": 14 } },
                        { "background": "#3F51B5" }
                      ],
                      "horizontalAlignment": "center",
                      "verticalArrangement": "center",
                      "children": [
                        {
                          "column": {
                            "horizontalAlignment": "center",
                            "verticalArrangement": "center",
                            "children": [
                              {
                                "text": {
                                  "value": "Card 2: Block Shatter",
                                  "fontSize": 16,
                                  "color": "#FFFFFF"
                                }
                              },
                              {
                                "text": {
                                  "value": "Breaks into 12 tumbling blocks",
                                  "fontSize": 12,
                                  "color": "#C5CAE9"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  },
                  {
                    "spacer": { "modifiers": [{ "size": 12 }] }
                  },
                  {
                    "box": {
                      "modifiers": [
                        { "width": 300 },
                        { "height": 64 },
                        { "visibility": "@card3Vis" },
                        {
                          "animationSpec": {
                            "motionDuration": 600,
                            "visibilityDuration": 1000,
                            "enterAnimation": "CUSTOM",
                            "exitAnimation": "CUSTOM",
                            "enterFunction": "shaderMeltEnter",
                            "exitFunction": "shaderMeltExit",
                            "enterSequence": "AFTER",
                            "exitSequence": "BEFORE"
                          }
                        },
                        { "clip": { "type": "RoundedRect", "radius": 14 } },
                        { "background": "#006064" }
                      ],
                      "horizontalAlignment": "center",
                      "verticalArrangement": "center",
                      "children": [
                        {
                          "column": {
                            "horizontalAlignment": "center",
                            "verticalArrangement": "center",
                            "children": [
                              {
                                "text": {
                                  "value": "Card 3: Shader Melt",
                                  "fontSize": 16,
                                  "color": "#84FFFF"
                                }
                              },
                              {
                                "text": {
                                  "value": "AGSL plasma dissolve & ripple",
                                  "fontSize": 12,
                                  "color": "#E0F7FA"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  },
                  {
                    "spacer": { "modifiers": [{ "size": 12 }] }
                  },
                  {
                    "box": {
                      "modifiers": [
                        { "width": 300 },
                        { "height": 64 },
                        { "visibility": "@card4Vis" },
                        {
                          "animationSpec": {
                            "motionDuration": 600,
                            "visibilityDuration": 1000,
                            "enterAnimation": "CUSTOM",
                            "exitAnimation": "CUSTOM",
                            "enterFunction": "liquidFillEnter",
                            "exitFunction": "liquidDrainExit",
                            "enterSequence": "AFTER",
                            "exitSequence": "BEFORE"
                          }
                        },
                        { "clip": { "type": "RoundedRect", "radius": 14 } },
                        { "background": "#004D40" }
                      ],
                      "horizontalAlignment": "center",
                      "verticalArrangement": "center",
                      "children": [
                        {
                          "column": {
                            "horizontalAlignment": "center",
                            "verticalArrangement": "center",
                            "children": [
                              {
                                "text": {
                                  "value": "Card 4: Drain & Fill",
                                  "fontSize": 16,
                                  "color": "#64FFDA"
                                }
                              },
                              {
                                "text": {
                                  "value": "AGSL liquid waves & bubbles",
                                  "fontSize": 12,
                                  "color": "#E0F2F1"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  },
                  {
                    "spacer": { "modifiers": [{ "size": 16 }] }
                  },
                  {
                    "row": {
                      "modifiers": [
                        { "animationSpec": { "motionDuration": 600 } }
                      ],
                      "children": [
                        {
                          "box": {
                            "modifiers": [
                              { "clip": { "type": "RoundedRect", "radius": 10 } },
                              { "background": "#37474F" },
                              { "padding": 10 },
                              {
                                "onclick": {
                                  "type": "ValueIntegerExpressionChange",
                                  "targetId": "@card1Vis",
                                  "value": "1 - @card1Vis"
                                }
                              }
                            ],
                            "horizontalAlignment": "center",
                            "verticalArrangement": "center",
                            "children": [
                              {
                                "text": {
                                  "value": "Toggle Card 1",
                                  "fontSize": 14,
                                  "color": "#FFFFFF"
                                }
                              }
                            ]
                          }
                        },
                        {
                          "spacer": { "modifiers": [{ "size": 12 }] }
                        },
                        {
                          "box": {
                            "modifiers": [
                              { "clip": { "type": "RoundedRect", "radius": 10 } },
                              { "background": "#283593" },
                              { "padding": 10 },
                              {
                                "onclick": {
                                  "type": "ValueIntegerExpressionChange",
                                  "targetId": "@card2Vis",
                                  "value": "1 - @card2Vis"
                                }
                              }
                            ],
                            "horizontalAlignment": "center",
                            "verticalArrangement": "center",
                            "children": [
                              {
                                "text": {
                                  "value": "Toggle Card 2",
                                  "fontSize": 14,
                                  "color": "#FFFFFF"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  },
                  {
                    "spacer": { "modifiers": [{ "size": 10 }] }
                  },
                  {
                    "row": {
                      "modifiers": [
                        { "animationSpec": { "motionDuration": 600 } }
                      ],
                      "children": [
                        {
                          "box": {
                            "modifiers": [
                              { "clip": { "type": "RoundedRect", "radius": 10 } },
                              { "background": "#006064" },
                              { "padding": 10 },
                              {
                                "onclick": {
                                  "type": "ValueIntegerExpressionChange",
                                  "targetId": "@card3Vis",
                                  "value": "1 - @card3Vis"
                                }
                              }
                            ],
                            "horizontalAlignment": "center",
                            "verticalArrangement": "center",
                            "children": [
                              {
                                "text": {
                                  "value": "3: Shader Melt",
                                  "fontSize": 14,
                                  "color": "#FFFFFF"
                                }
                              }
                            ]
                          }
                        },
                        {
                          "spacer": { "modifiers": [{ "size": 12 }] }
                        },
                        {
                          "box": {
                            "modifiers": [
                              { "clip": { "type": "RoundedRect", "radius": 10 } },
                              { "background": "#00695C" },
                              { "padding": 10 },
                              {
                                "onclick": {
                                  "type": "ValueIntegerExpressionChange",
                                  "targetId": "@card4Vis",
                                  "value": "1 - @card4Vis"
                                }
                              }
                            ],
                            "horizontalAlignment": "center",
                            "verticalArrangement": "center",
                            "children": [
                              {
                                "text": {
                                  "value": "4: Drain & Fill",
                                  "fontSize": 14,
                                  "color": "#FFFFFF"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """
            .trimIndent()

    val tags = RemoteComposeJsonParser.parseHeaderOnly(json)
    val apiLevel = RemoteComposeJsonParser.parseApiLevel(json)
    val writer = RemoteComposeWriter(platform, apiLevel, *tags)
    val parser = RemoteComposeJsonParser(writer)
    parser.parse(json)
    return RemoteComposeContext(writer)
}
