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
fun rcJsonMaterialToggleButtonMacroDemo(
    platform: RcPlatformServices = AndroidxRcPlatformServices()
): RemoteComposeContext {
    val json =
        """
        {
          "header": {
            "profiles": 513
          },
          "resources": {
            "integers": [
              { "count": 0 }
            ]
          },
          "root": [
            {
              "definePattern": {
                "name": "CounterButton",
                "parameters": ["targetVar", "expression", "label", "buttonColor"],
                "children": [
                  {
                    "box": {
                      "horizontalAlignment": "center",
                      "verticalAlignment": "center",
                      "modifiers": [
                        { "width": 80 },
                        { "height": 56 },
                        { "clip": { "roundedRect": 16 } },
                        { "background": "@buttonColor" },
                        "ripple",
                        {
                          "onClick": {
                            "type": "valueIntegerExpressionChange",
                            "target": "@targetVar",
                            "expression": "@expression"
                          }
                        }
                      ],
                      "children": [
                        {
                          "text": {
                            "value": "@label",
                            "fontSize": 18,
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
              "column": {
                "horizontalAlignment": "center",
                "verticalAlignment": "center",
                "modifiers": [
                  "fillMaxSize",
                  { "background": "#121212" },
                  { "padding": 24 },
                  { "spacedBy": 32 }
                ],
                "children": [
                  {
                    "box": {
                      "horizontalAlignment": "center",
                      "verticalAlignment": "center",
                      "modifiers": [
                        { "width": 280 },
                        { "height": 140 },
                        { "clip": { "roundedRect": 20 } },
                        { "background": "#1E1E2E" },
                        { "padding": 16 }
                      ],
                      "children": [
                        {
                          "column": {
                            "horizontalAlignment": "center",
                            "verticalAlignment": "center",
                            "modifiers": [
                              { "spacedBy": 8 }
                            ],
                            "children": [
                              {
                                "text": {
                                  "value": "COUNTER",
                                  "fontSize": 14,
                                  "color": "#A0A0B0"
                                }
                              },
                              {
                                "text": {
                                  "textFromFloat": {
                                    "value": "@vars.count",
                                    "whole": 1,
                                    "decimal": 0
                                  },
                                  "fontSize": 64,
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
                    "row": {
                      "horizontalAlignment": "center",
                      "verticalAlignment": "center",
                      "modifiers": [
                        { "spacedBy": 16 }
                      ],
                      "children": [
                        {
                          "CounterButton": {
                            "targetVar": "@vars.count",
                            "expression": "@vars.count - 1",
                            "label": "-",
                            "buttonColor": "#E53935"
                          }
                        },
                        {
                          "CounterButton": {
                            "targetVar": "@vars.count",
                            "expression": "0",
                            "label": "RESET",
                            "buttonColor": "#757575"
                          }
                        },
                        {
                          "CounterButton": {
                            "targetVar": "@vars.count",
                            "expression": "@vars.count + 1",
                            "label": "+",
                            "buttonColor": "#4CAF50"
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
