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
fun rcJsonStyleMacroDemo(
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
              "variable": {
                "name": "primaryColor",
                "vtype": "color",
                "value": "#6200EE",
                "named": false
              }
            },
            {
              "variable": {
                "name": "secondaryColor",
                "vtype": "color",
                "value": "#03DAC6",
                "named": false
              }
            },
            {
              "definePattern": {
                "name": "ButtonStyle",
                "parameters": ["backgroundColor"],
                "children": [
                  {
                    "modifier": {
                      "modifiers": [
                        { "clip": { "type": "RoundedRect", "topStart": 8, "topEnd": 8, "bottomStart": 8, "bottomEnd": 8 } },
                        { "background": "@backgroundColor" },
                        { "padding": [24, 12, 24, 12] }
                      ]
                    }
                  }
                ]
              }
            },
            {
              "definePattern": {
                "name": "CardStyle",
                "children": [
                  {
                    "modifier": {
                      "modifiers": [
                        { "padding": 16 },
                        { "border": { "width": 1, "cornerRadius": 12, "color": "#CCCCCC", "shape": 2 } },
                        { "clip": { "type": "RoundedRect", "topStart": 8, "topEnd": 8, "bottomStart": 8, "bottomEnd": 8 } },
                        { "background": "#FFFFFF" },
                        { "padding": 16 },
                        "fillMaxWidth"
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
                  { "padding": 20 }
                ],
                "horizontalAlignment": "center",
                "children": [
                  {
                    "text": {
                      "value": "Modifier Macros (Style Factories)",
                      "fontSize": 40.0,
                      "textAlign": "left",
                      "modifiers": [
                        { "padding": [0, 0, 0, 20] }
                      ]
                    }
                  },
                  {
                    "box": {
                      "modifiers": [
                        { "includemacro": { "pattern": "ButtonStyle", "arguments": ["@primaryColor"] } }
                      ],
                      "children": [
                        { "text": { "value": "Primary Button", "color": "#FFFFFF", "fontSize": 24.0, "textAlign": "left" } }
                      ]
                    }
                  },
                  {
                    "box": {
                      "modifiers": [
                        { "size": 20 }
                      ]
                    }
                  },
                  {
                    "box": {
                      "modifiers": [
                        { "includemacro": { "pattern": "ButtonStyle", "arguments": ["@secondaryColor"] } }
                      ],
                      "children": [
                        { "text": { "value": "Secondary Button", "color": "#000000", "fontSize": 24.0, "textAlign": "left" } }
                      ]
                    }
                  },
                  {
                    "box": {
                      "modifiers": [
                        { "size": 40 }
                      ]
                    }
                  },
                  {
                    "column": {
                      "modifiers": [
                        { "includemacro": { "pattern": "CardStyle", "arguments": [] } }
                      ],
                      "children": [
                        { "text": { "value": "Card Title", "fontSize": 30.0, "fontWeight": 700.0, "textAlign": "left" } },
                        { "text": { "value": "This card uses a macro to define its padding, background, and border.", "fontSize": 20.0, "textAlign": "left" } }
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
