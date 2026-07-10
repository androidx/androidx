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
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/** Version of RcTextDemo8 initialized via JSON string. */
@Suppress("RestrictedApiAndroidX")
fun rcJsonTextDemo8(
    platform: RcPlatformServices = AndroidxRcPlatformServices()
): RemoteComposeContext {
    val json =
        """
        {
          "header": {
            "apiLevel": 7,
            "width": 600,
            "height": 600,
            "featurePaintMeasure": 0,
            "contentDescription": "Demo",
            "profiles": 513
          },
          "root": [
            {
              "type": "row",
              "modifiers": [
                { "background": "#FF00FF00" },
                { "padding": 8.0 },
                { "fillMaxWidth": "NaN" }
              ],
              "horizontalAlignment": "start",
              "verticalAlignment": "center",
              "children": [
                {
                  "type": "column",
                  "modifiers": [
                    { "weight": 1.0 },
                    { "background": "#FFFFFF00" }
                  ],
                  "horizontalAlignment": "start",
                  "verticalAlignment": "top",
                  "children": [
                    { "type": "text", "value": "New Arsenal Game", "maxLines": 1, "overflow": "ellipsis" },
                    { "type": "text", "value": "Arsenal vs Bayern Munich", "fontSize": 64.0, "maxLines": 3, "overflow": "ellipsis" },
                    { "type": "text", "value": "UEFA Champions League Group Stage", "maxLines": 2, "overflow": "ellipsis" },
                    { "type": "text", "value": "Wednesday 26th November", "maxLines": 1, "overflow": "ellipsis" }
                  ]
                },
                {
                  "type": "column",
                  "modifiers": [
                    { "size": 130.0 },
                    { "background": "#FF00FFFF" },
                    { "padding": 8.0 }
                  ],
                  "horizontalAlignment": "center",
                  "verticalAlignment": "center",
                  "children": [
                    {
                      "type": "box",
                      "modifiers": [
                        { "size": 100.0 },
                        { "background": "#FFFFFF00" }
                      ],
                      "horizontalAlignment": "center",
                      "verticalAlignment": "center",
                      "children": [
                        { "type": "text", "value": "IMG" }
                      ]
                    }
                  ]
                }
              ]
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

@Preview @Composable private fun RcJsonTextDemo8Preview() = RemoteDocumentPreview(rcJsonTextDemo8())
