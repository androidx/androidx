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
fun rcJsonReferencedOperationsMacroDemo(
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
                "name": "CardMacro",
                "parameters": ["title", "contentId"],
                "children": [
                  {
                    "column": {
                      "modifiers": [
                        { "padding": 16 },
                        { "clip": { "type": "RoundedRect", "topStart": 12, "topEnd": 12, "bottomStart": 12, "bottomEnd": 12 } },
                        { "background": "#FFFFFF" },
                        { "border": { "width": 2, "cornerRadius": 12, "color": "#888888", "shape": 2 } },
                        { "padding": 16 },
                        "fillMaxWidth"
                      ],
                      "children": [
                        {
                          "text": {
                            "value": "@title",
                            "fontSize": 35.0,
                            "color": "#0000FF",
                            "textAlign": "left",
                            "modifiers": [
                              { "padding": [0, 0, 0, 10] }
                            ]
                          }
                        },
                        {
                          "canvas": {
                            "modifiers": [ "fillMaxWidth", { "height": 2 } ],
                            "commands": [
                              { "drawLine": { "x1": 0, "y1": 0, "x2": "windowWidth", "y2": 0 } }
                            ]
                          }
                        },
                        {
                          "box": {
                            "modifiers": [ { "size": 10 } ]
                          }
                        },
                        {
                          "include": {
                            "value": "@contentId"
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
                  "fillMaxSize"
                ],
                "horizontalAlignment": "center",
                "children": [
                  {
                    "text": {
                      "value": "Macros with Content Inclusion",
                      "fontSize": 50.0,
                      "textAlign": "left",
                      "modifiers": [ { "padding": 20 } ]
                    }
                  },
                  {
                    "referencedOperations": {
                      "name": "ContentAlpha",
                      "children": [
                        { "text": { "value": "First item in A", "fontSize": 25.0, "textAlign": "left" } },
                        { "text": { "value": "Second item in A", "fontSize": 25.0, "textAlign": "left" } },
                        {
                          "box": {
                            "modifiers": [ { "padding": 5 }, { "background": "#CCCCCC" } ],
                            "children": [
                              { "text": { "value": "Nested box in A", "fontSize": 20.0, "textAlign": "left" } }
                            ]
                          }
                        }
                      ]
                    }
                  },
                  {
                    "CardMacro": {
                      "title": "Section Alpha",
                      "contentId": "@ContentAlpha"
                    }
                  },
                  {
                    "box": {
                      "modifiers": [ { "size": 20 } ]
                    }
                  },
                  {
                    "referencedOperations": {
                      "name": "ContentBeta",
                      "children": [
                        {
                          "flow": {
                            "modifiers": [
                              "fillMaxWidth",
                              { "background": "#E8F5E9" },
                              { "padding": 10 }
                            ],
                            "children": [
                              {
                                "text": {
                                  "value": "Row Item 1",
                                  "fontSize": 25.0,
                                  "textAlign": "left",
                                  "modifiers": [
                                    { "padding": 8 },
                                    { "background": "#FFFF00" }
                                  ]
                                }
                              },
                              {
                                "text": {
                                  "value": "Row Item 2",
                                  "fontSize": 25.0,
                                  "textAlign": "left",
                                  "modifiers": [
                                    { "padding": 8 },
                                    { "background": "#00FFFF" }
                                  ]
                                }
                              }
                            ]
                          }
                        },
                        {
                          "text": {
                            "value": "Footer in B",
                            "fontSize": 20.0,
                            "textAlign": "left",
                            "color": "#888888"
                          }
                        }
                      ]
                    }
                  },
                  {
                    "CardMacro": {
                      "title": "Section Beta",
                      "contentId": "@ContentBeta"
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
