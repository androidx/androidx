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

import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.creation.json.RemoteComposeJsonParser
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

/** Zero-padded whole number, no decimals: the format of each field of `mm:ss.cc`. */
@Suppress("RestrictedApiAndroidX")
private const val PAD = TextFromFloat.PAD_PRE_ZERO or TextFromFloat.PAD_AFTER_NONE

/**
 * The JSON version of `dslStopwatchDemo`: a dial showing `mm:ss.cc`, START / STOP / LAP buttons,
 * and a scrolling list of lap times.
 *
 * State is three named floats. START sets `start` to 1 and stamps `startTime` with the current
 * `animationTime`, STOP sets `start` to 0, and LAP advances `lapCount` (mod 5). The elapsed time is
 * `start * (animationTime - startTime)`. The lap canvas writes it into `lapTimes[lapCount]` every
 * frame and lists entries 0 through `lapCount`, so the last row is the running lap.
 *
 * `lapTimes` is created inside a canvas `global` block, which hoists it to the start of the
 * document so it is created once rather than on every paint.
 */
@Suppress("RestrictedApiAndroidX")
public fun rcJsonStopwatch(): ByteArray {
    val startActions =
        """
        { "type": "valueFloatChange", "target": "@start", "value": 1.0 },
        { "type": "valueFloatExpressionChange", "target": "@startTime", "value": "@clock" }
        """
    val stopActions = """{ "type": "valueFloatChange", "target": "@start", "value": 0.0 }"""
    val lapActions =
        """
        { "type": "valueFloatExpressionChange", "target": "@lapCount", "value": "(@lapCount + 1) % 5" }
        """
    val elapsed = timeText("floor(@timeExpr / 60)", "floor(@timeExpr % 60)", "@centiseconds")
    // As in the DSL version, a lap's minutes and seconds are formatted unfloored and unwrapped.
    val lap = "arrayGet(@lapTimes, @i)"
    val lapTime = timeText("$lap / 60", lap, "floor($lap % 1 * 100)")
    val json =
        """
        {
          "header": {
            "apiLevel": 8,
            "profiles": 513,
            "measureVersion": 2
          },
          "root": [
            { "variable": { "name": "start", "value": 0.0, "export": true } },
            { "variable": { "name": "startTime", "value": 0.0, "export": true } },
            { "variable": { "name": "lapCount", "value": 0.0, "export": true } },
            { "variable": { "name": "clock", "value": "animationTime", "flush": true } },
            { "variable": { "name": "timeExpr", "value": "@start * (@clock - @startTime)" } },
            {
              "variable": {
                "name": "centiseconds",
                "value": { "value": "floor(@timeExpr % 1 * 100)", "anim": 0.1 }
              }
            },
            {
              "variable": {
                "name": "display",
                "vtype": "string",
                "value": $elapsed
              }
            },
            {
              "column": {
                "horizontalAlignment": "center",
                "modifiers": [ "fillMaxSize", { "background": "#0F172A" }, { "padding": 24.0 } ],
                "children": [
                  {
                    "box": {
                      "modifiers": [ { "size": 400.0 } ],
                      "children": [
                        {
                          "canvas": {
                            "modifiers": [ "fillMaxSize" ],
                            "commands": [
                              {
                                "global": {
                                  "commands": [
                                    { "dynamicFloatArray": { "name": "lapTimes", "size": 5.0 } },
                                    { "setArrayValue": { "id": "@lapTimes", "index": 0.0, "value": 1.0 } },
                                    { "setArrayValue": { "id": "@lapTimes", "index": 1.0, "value": 2.0 } },
                                    { "setArrayValue": { "id": "@lapTimes", "index": 2.0, "value": 3.0 } },
                                    { "setArrayValue": { "id": "@lapTimes", "index": 3.0, "value": 4.0 } },
                                    { "setArrayValue": { "id": "@lapTimes", "index": 4.0, "value": 5.0 } }
                                  ]
                                }
                              },
                              {
                                "paint": {
                                  "ops": [
                                    { "color": "#0D9488" },
                                    { "style": "stroke" },
                                    { "width": 8.0 }
                                  ]
                                }
                              },
                              {
                                "drawCircle": {
                                  "cx": "width / 2",
                                  "cy": "height / 2",
                                  "radius": "min(width / 2, height / 2) - 12"
                                }
                              },
                              {
                                "paint": {
                                  "ops": [
                                    { "color": "#FFFFFF" },
                                    { "textSize": 64.0 },
                                    { "style": "fill" },
                                    { "width": 0.0 }
                                  ]
                                }
                              },
                              {
                                "drawTextAnchored": {
                                  "text": "@display",
                                  "x": "width / 2",
                                  "y": "height / 2",
                                  "panX": 0.0,
                                  "panY": 0.0,
                                  "flags": 0
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  },
                  { "box": { "modifiers": [ { "size": 24.0 } ] } },
                  {
                    "row": {
                      "verticalAlignment": "center",
                      "horizontalAlignment": "spaceEvenly",
                      "modifiers": [ { "fillParentMaxWidth": 0.95 } ],
                      "children": [
                        ${button("START", "#059669", startActions)},
                        ${button("STOP", "#DC2626", stopActions)},
                        ${button("LAP", "#0891B2", lapActions)}
                      ]
                    }
                  },
                  { "box": { "modifiers": [ { "size": 24.0 } ] } },
                  {
                    "column": {
                      "modifiers": [
                        { "fillParentMaxWidth": 0.9 },
                        "fillMaxHeight",
                        { "background": "#15FFFFFF" },
                        { "clip": { "type": "roundRect", "radius": 16.0 } },
                        { "border": { "width": 1.0, "cornerRadius": 16.0, "color": "#33FFFFFF", "shape": ${ShapeType.ROUNDED_RECTANGLE} } },
                        { "verticalScroll": { "position": 0.0, "notches": 0 } }
                      ],
                      "children": [
                        {
                          "canvas": {
                            "modifiers": [ { "fillParentMaxWidth": 1.0 }, { "height": 250.0 } ],
                            "commands": [
                              { "setArrayValue": { "id": "@lapTimes", "index": "@lapCount", "value": "@timeExpr" } },
                              {
                                "save": {
                                  "commands": [
                                    {
                                      "loop": {
                                        "from": 0.0,
                                        "step": 1.0,
                                        "until": "@lapCount",
                                        "index": "i",
                                        "commands": [
                                          { "paint": { "ops": [ { "color": "#E2E8F0" }, { "textSize": 64.0 } ] } },
                                          {
                                            "drawTextAnchored": {
                                              "text": $lapTime,
                                              "x": "width / 2",
                                              "y": 64.0,
                                              "panX": 0.0,
                                              "panY": "@i * 3",
                                              "flags": 0
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
                  }
                ]
              }
            }
          ]
        }
        """
            .trimIndent()
    return RemoteComposeJsonParser.parse(json, AndroidxRcPlatformServices()).array()
}

/** `minutes:seconds.centiseconds`, each field two zero-padded digits, as nested `textMerge`s. */
@Suppress("RestrictedApiAndroidX")
private fun timeText(minutes: String, seconds: String, centiseconds: String): String {
    fun field(value: String) =
        """{ "type": "textFromFloat", "value": "$value", "whole": 2, "decimal": 0, "flags": $PAD }"""
    fun merge(a: String, b: String) = """{ "type": "textMerge", "id1": $a, "id2": $b }"""
    return merge(
        merge(merge(merge(field(minutes), "\":\""), field(seconds)), "\".\""),
        field(centiseconds),
    )
}

/**
 * A rounded, coloured button labelled [label] that runs [actions] (a JSON action list) on click.
 */
private fun button(label: String, color: String, actions: String): String =
    """
    {
      "box": {
        "modifiers": [
          { "background": "$color" },
          { "clip": { "type": "roundRect", "radius": 16.0 } },
          { "padding": [20.0, 12.0, 20.0, 12.0] },
          { "onClick": [ $actions ] }
        ],
        "children": [ { "text": { "value": "$label", "color": "#FFFFFF", "fontSize": 64.0 } } ]
      }
    }
    """
