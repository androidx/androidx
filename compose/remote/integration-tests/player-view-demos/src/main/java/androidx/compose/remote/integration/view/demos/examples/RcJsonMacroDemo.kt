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
fun rcJsonMacroDemo(
    platform: RcPlatformServices = AndroidxRcPlatformServices()
): RemoteComposeContext {
    val json =
        """
        {
          "header": {
            "apiLevel": 7,
            "profiles": 513
          },
          "root": [
            {
              "definePattern": {
                "name": "GlimmerButton",
                "parameters": ["label", "clickData", "fontSize", "targetId"],
                "children": [
                  {
                    "box": {
                      "modifiers": [
                        { "padding": 16 },
                        { "clip": { "type": "RoundedRect", "topStart": 64, "topEnd": 64, "bottomStart": 64, "bottomEnd": 64 } },
                        { "background": "#64FFFFFF" },
                        { "padding": 4 },
                        { "clip": { "type": "RoundedRect", "topStart": 64, "topEnd": 64, "bottomStart": 64, "bottomEnd": 64 } },
                        { "background": "#64646464" },
                        { "padding": 24 },
                        { "onclick": { "type": "ValueStringChange", "targetId": "@targetId", "value": "@clickData" } }
                      ],
                      "children": [
                        {
                          "text": {
                            "value": "@label",
                            "fontSize": "@fontSize",
                            "color": "#FFFFFF",
                            "textAlign": "left"
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            },
            {
              "box": {
                "modifiers": [ "fillMaxSize" ],
                "children": [
                  {
                    "canvas": {
                      "modifiers": [ "fillMaxSize" ],
                      "commands": [
                        {
                          "paint": {
                            "linearGradient": {
                              "x1": 0, "y1": 0, "x2": 400, "y2": 400,
                              "colors": ["#FF0000", "#00FF00"]
                            }
                          }
                        },
                        { "drawRect": { "left": 0, "top": 0, "right": "windowWidth", "bottom": "windowHeight" } },
                        { "paint": { "color": "#000000" } }
                      ]
                    }
                  },
                  {
                    "column": {
                      "modifiers": [ "fillMaxSize" ],
                      "horizontalAlignment": "center",
                      "children": [
                        {
                          "text": {
                            "value": "Macro Buttons Demo",
                            "fontSize": 60,
                            "textAlign": "left",
                            "modifiers": [ { "padding": 20 } ]
                          }
                        },
                        {
                          "variable": {
                            "name": "contentVal",
                            "vtype": "string",
                            "value": "Placeholder"
                          }
                        },
                        {
                          "text": {
                            "value": "@contentVal",
                            "textAlign": "left"
                          }
                        },
                        {
                          "flow": {
                            "children": [
                              {
                                "GlimmerButton": {
                                  "label": "Action A",
                                  "clickData": "new 0",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
                                }
                              },
                              {
                                "GlimmerButton": {
                                  "label": "Action B",
                                  "clickData": "new 1",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
                                }
                              },
                              {
                                "GlimmerButton": {
                                  "label": "Action C",
                                  "clickData": "new 2",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
                                }
                              },
                              {
                                "GlimmerButton": {
                                  "label": "Action D",
                                  "clickData": "new 3",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
                                }
                              },
                              {
                                "GlimmerButton": {
                                  "label": "Action E",
                                  "clickData": "new 4",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
                                }
                              },
                              {
                                "GlimmerButton": {
                                  "label": "Action F",
                                  "clickData": "new 5",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
                                }
                              },
                              {
                                "GlimmerButton": {
                                  "label": "Action G",
                                  "clickData": "new 6",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
                                }
                              },
                              {
                                "GlimmerButton": {
                                  "label": "Action H",
                                  "clickData": "new 7",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
                                }
                              },
                              {
                                "GlimmerButton": {
                                  "label": "Action I",
                                  "clickData": "new 8",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
                                }
                              },
                              {
                                "GlimmerButton": {
                                  "label": "Action J",
                                  "clickData": "new 9",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
                                }
                              },
                              {
                                "GlimmerButton": {
                                  "label": "Action K",
                                  "clickData": "new 10",
                                  "fontSize": 30.0,
                                  "targetId": "@contentVal"
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

    val actualTags = RemoteComposeJsonParser.parseHeaderOnly(json)
    val actualApiLevel = RemoteComposeJsonParser.parseApiLevel(json)
    val writer = RemoteComposeWriter(platform, actualApiLevel, *actualTags!!)
    val parser = RemoteComposeJsonParser(writer)
    parser.parse(json)
    return RemoteComposeContext(writer)
}
