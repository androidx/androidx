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
fun rcJsonMacroLocalDemo(
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
                "name": "CounterButton",
                "parameters": ["label"],
                "locals": ["counter"],
                "children": [
                  {
                    "variable": {
                      "name": "counter",
                      "value": 0,
                      "named": false
                    }
                  },
                  {
                    "variable": {
                      "name": "displayText",
                      "vtype": "string",
                      "value": {
                        "type": "textFromFloat",
                        "value": "@counter",
                        "decimal": 0,
                        "whole": 3,
                        "flags": 0
                      },
                      "named": false
                    }
                  },
                  {
                    "variable": {
                      "name": "nextValueExpr",
                      "value": "counter + 1",
                      "flush": true,
                      "named": false
                    }
                  },
                  {
                    "column": {
                      "modifiers": [
                        "fillMaxWidth",
                        { "padding": 10 },
                        { "clip": { "type": "RoundedRect", "topStart": 24, "topEnd": 24, "bottomStart": 24, "bottomEnd": 24 } },
                        { "background": "#CCCCCC" },
                        { "padding": 20 },
                        { "onclick": { "type": "ValueFloatExpressionChange", "targetId": "@counter", "value": "@nextValueExpr" } }
                      ],
                      "children": [
                        {
                          "text": {
                            "value": "@label",
                            "fontSize": 28,
                            "color": "#000000",
                            "textAlign": "left"
                          }
                        },
                        {
                          "row": {
                            "children": [
                              { "text": { "value": "Count: ", "fontSize": 24, "color": "#444444", "textAlign": "left" } },
                              { "text": { "value": "@displayText", "fontSize": 24, "color": "#0000FF", "textAlign": "left" } }
                            ]
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
                "modifiers": [
                  "fillMaxSize",
                  { "background": "#FFFFFF" },
                  { "padding": 40 }
                ],
                "children": [
                  {
                    "text": {
                      "value": "Macro-Local State Demo",
                      "fontSize": 48,
                      "textAlign": "left",
                      "modifiers": [
                        { "padding": [0, 0, 0, 20] }
                      ]
                    }
                  },
                  {
                    "text": {
                      "value": "Each button below has its own independent internal count.",
                      "fontSize": 24,
                      "textAlign": "left",
                      "modifiers": [
                        { "padding": [0, 0, 0, 40] }
                      ]
                    }
                  },
                  {
                    "column": {
                      "modifiers": [ "fillMaxWidth" ],
                      "children": [
                        {
                          "box": {
                            "modifiers": [ { "padding": [0, 0, 0, 20] } ],
                            "children": [
                              {
                                "CounterButton": {
                                  "label": "Button Alpha"
                                }
                              }
                            ]
                          }
                        },
                        {
                          "box": {
                            "modifiers": [ { "padding": [0, 0, 0, 20] } ],
                            "children": [
                              {
                                "CounterButton": {
                                  "label": "Button Beta"
                                }
                              }
                            ]
                          }
                        },
                        {
                          "box": {
                            "modifiers": [ { "padding": [0, 0, 0, 20] } ],
                            "children": [
                              {
                                "CounterButton": {
                                  "label": "Button Gamma"
                                }
                              }
                            ]
                          }
                        },
                        {
                          "box": {
                            "modifiers": [ { "padding": [0, 0, 0, 20] } ],
                            "children": [
                              {
                                "CounterButton": {
                                  "label": "Button Delta"
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
