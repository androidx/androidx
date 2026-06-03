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
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.json.RemoteComposeJsonParser
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

/** JSON rewrite of the ParticleSphere DSL demo using the compact Tag-Key shorthand syntax. */
@Suppress("RestrictedApiAndroidX")
@JvmOverloads
fun rcJsonParticleSphere(
    platform: RcPlatformServices = AndroidxRcPlatformServices()
): RemoteComposeContext {
    val rc =
        RemoteComposeContextAndroid(800, 800, "spinning", 6, 0, platform) {
            val json =
                """
                {
                  "header": {
                    "width": 800,
                    "height": 800,
                    "contentDescription": "spinning",
                    "apiLevel": 6,
                    "profiles": 0
                  },
                  "root": {
                    "box": {
                      "modifiers": [ "fillMaxSize" ],
                      "children": [
                        {
                          "canvas": {
                            "modifiers": [ "fillMaxSize" ],
                            "commands": [
                              { "variable": { "name": "w", "value": "width", "commit": true } },
                              { "variable": { "name": "h", "value": "height", "commit": true } },
                              { "variable": { "name": "cx", "value": "w / 2.0" } },
                              { "variable": { "name": "cy", "value": "h / 2.0" } },

                              { "paint": { "color": "#110F0C" } },
                              { "drawRoundRect": {
                                "left": 10.0,
                                "top": 10.0,
                                "right": "w - 20.0",
                                "bottom": "h - 20.0",
                                "rx": 120.0,
                                "ry": 120.0
                              } },

                              { "paint": {
                                "ops": [
                                  { "linearGradient": {
                                    "x1": 0.0, "y1": 0.0, "x2": 0.0, "y2": "h",
                                    "colors": [ "#9FAAC9", "#111122" ],
                                    "tileMode": "clamp"
                                  } },
                                  { "color": "#0000FF" }
                                ]
                              } },

                              { "variable": { "name": "rad", "value": "min(w, h) * 0.3" } },
                              { "drawCircle": { "cx": "@vars.cx", "cy": "@vars.cy", "radius": "@vars.rad" } },
                              { "addBitmap": { "image": "image" } },

                              { "paint": {
                                "ops": [
                                  { "color": "#FFC45E" },
                                  { "alpha": 1.0 },
                                  { "shader": 0 }
                                ]
                              } },
                              { "variable": { "name": "angle", "value": "time * 0.2" } },

                              {
                                "impulse": {
                                  "duration": 200.0,
                                  "start": 0.0,
                                  "commands": [
                                    {
                                      "createParticles": {
                                        "id": "ps",
                                        "variables": [ "lat", "lon", "pos" ],
                                        "initialValues": [
                                          "rand * 6.2831853",
                                          "((rand * 797.0) % 800.0) / 800.0 * 6.2831853",
                                          "sign(max(0.0, rand - 0.8)) * 0.2"
                                        ],
                                        "count": 800
                                      }
                                    },
                                    {
                                      "impulseProcess": {
                                        "commands": [
                                          {
                                            "particlesLoop": {
                                              "system": "@vars.ps",
                                              "equations": [ "@vars.lat", "@vars.lon", "@vars.pos" ],
                                              "commands": [
                                                { "save": {} },
                                                { "variable": { "name": "x", "value": "cos(@vars.lon + (time * 0.2)) * sqrt(@vars.rad * @vars.rad - (cos(@vars.lat) * @vars.rad) * (cos(@vars.lat) * @vars.rad))", "commit": true } },
                                                { "variable": { "name": "y", "value": "cos(@vars.lat) * @vars.rad", "commit": true } },
                                                { "translate": {
                                                  "dx": "@vars.x * (@vars.pos + 1.0) + @vars.cx - 1.0",
                                                  "dy": "@vars.y * (@vars.pos + 1.0) + @vars.cy - 1.0"
                                                } },
                                                { "drawRoundRect": { "left": 0.0, "top": 0.0, "right": 4.0, "bottom": 4.0, "rx": 4.0, "ry": 4.0 } },
                                                { "restore": {} }
                                              ]
                                            }
                                          }
                                        ]
                                      }
                                    }
                                  ]
                                }
                              },

                              { "variable": { "name": "textId", "value": { "type": "textFromFloat", "value": "seconds", "whole": 2, "decimal": 0, "flags": 0 }, "commit": true } },
                              { "paint": {
                                "ops": [
                                  { "color": "#A0FF99" },
                                  { "textSize": 123.0 }
                                ]
                              } },
                              { "drawTextAnchored": { "text": "@vars.textId", "x": "cx", "y": "cy", "panX": 0.0, "panY": 0.0, "flags": 2 } }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
                """
                    .trimIndent()
            val parser = RemoteComposeJsonParser(this.writer)
            parser.defineBitmap("image", createMark(2))
            parser.parse(json)
        }
    return rc
}
