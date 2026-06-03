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
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.json.RemoteComposeJsonParser
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.util.Random
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/** Version of RcTicker initialized via JSON string, byte-to-byte compatible with RcDslTicker. */
@Suppress("RestrictedApiAndroidX")
@JvmOverloads
fun rcJsonTicker(
    platform: RcPlatformServices = AndroidxRcPlatformServices()
): RemoteComposeContext {
    println("### FORCINGTickRebuildV5 ###")
    val stockData = generateStockDataArrayLocal(101, 100f, 8000f, 2000f, 0.01f)
    val stockDataJson = stockData.joinToString(", ")

    val refreshStr =
        "M480,800Q346,800 253,707Q160,614 160,480Q160,346 253,253Q346," +
            "160 480,160Q549,160 612,188.5Q675,217 720,270L720,160L800,160L800,440L520," +
            "440L520,360L688,360Q656,304 600.5,272Q545,240 480,240Q380,240 310,310Q240," +
            "380 240,480Q240,580 310,650Q380,720 480,720Q557,720 619,676Q681,632 706," +
            "560L790,560Q762,666 676,733Q590,800 480,800Z"

    val json =
        """
    {
      "header": {
        "apiLevel": 8,
        "profiles": 513,
        "orderedResources": true
      },
      "root": [
        {
          "global": [
            {
              "resources": {
                "order": [ "colors" ],
                "colors": [
                  { "color.system_accent2_800": "#FF0000" },
                  { "color.system_accent2_50": "#113311" },
                  { "background": { "light": "@colors.color.system_accent2_50", "dark": "@colors.color.system_accent2_800", "export": false } },
                  { "color.system_on_surface_light": "#113311" },
                  { "color.system_on_surface_dark": "#FF9966" },
                  { "textColor": { "light": "@colors.color.system_on_surface_light", "dark": "@colors.color.system_on_surface_dark", "export": false } },
                  { "color.system_accent3_600": "#113311" },
                  { "color.system_accent3_100": "#FF9966" },
                  { "dotColor": { "light": "@colors.color.system_accent3_600", "dark": "@colors.color.system_accent3_100", "export": false } },
                  { "color.system_accent2_10": "#113311" },
                  { "color.system_accent2_900": "#FF9966" },
                  { "panels": { "light": "@colors.color.system_accent2_10", "dark": "@colors.color.system_accent2_900", "export": false } },
                  { "color.system_accent1_200": "#222222" },
                  { "followText": { "light": "@colors.color.system_accent2_800", "dark": "@colors.color.system_accent1_200", "export": false } },
                  { "color.system_neutral2_800": "#113311" },
                  { "color.system_neutral2_400": "#FF9966" },
                  { "stockName": { "light": "@colors.color.system_neutral2_800", "dark": "@colors.color.system_neutral2_400", "export": false } },
                  { "color.system_accent1_900": "#113311" },
                  { "color.system_accent1_50": "#FF9966" },
                  { "stockPrice": { "light": "@colors.color.system_accent1_900", "dark": "@colors.color.system_accent1_50", "export": false } },
                  { "arrowColor": { "light": "@colors.color.system_accent1_50", "dark": "@colors.color.system_accent1_900", "export": false } }
                ]
              }
            },
            { "variable": { "name": "system.font_size", "value": 37.0, "export": true } },
            { "variable": { "name": "head1", "value": "42.0 * (@vars.system.font_size / 37.0)" } },
            { "variable": { "name": "priceDollars", "value": "64.0 * (@vars.system.font_size / 37.0)" } },
            { "variable": { "name": "priceCents", "value": "48.0 * (@vars.system.font_size / 37.0)" } },
            { "variable": { "name": "nameFontSize", "value": "32.0 * (@vars.system.font_size / 37.0)" } },
            { "variable": { "name": "defaultFontSize", "value": "32.0 * (@vars.system.font_size / 37.0)" } }
          ]
        },
        {
          "column": {
            "modifiers": [ "fillMaxWidth", { "background": "@colors.background" } ],
            "children": [
              {
                "row": {
                  "modifiers": [ { "padding": 32.0 } ],
                  "children": [
                    {
                      "text": {
                        "value": "Watchlist",
                        "fontSize": "@vars.head1",
                        "color": "@colors.textColor",
                        "modifiers": [ { "padding": 24.0 } ]
                      }
                    },
                    { "spacer": {} },
                    {
                      "canvas": {
                        "modifiers": [ { "width": 64.0 }, { "height": 64.0 } ],
                        "commands": [
                          { "paint": { "color": "@colors.textColor" } },
                          { "variable": { "name": "refreshPath", "value": "${refreshStr}", "vtype": "path", "named": false } },
                          { "scale": { "sx": 0.06666667, "sy": 0.06666667 } },
                          { "drawPath": "refreshPath" }
                        ]
                      }
                    }
                  ]
                }
              },
              {
                "box": {
                  "modifiers": [ "fillMaxWidth" ],
                  "children": [
                    {
                      "column": {
                        "horizontalAlignment": "center",
                        "modifiers": [
                          "fillMaxSize",
                          { "id": 4343 },
                          { "verticalScroll": 0.0 }
                        ],
                        "children": [
                          {
                            "column": {
                              "horizontalAlignment": "center",
                              "children": [
                                {
                                  "column": {
                                    "modifiers": [ { "padding": [32.0, 40.0, 48.0, 1.0] } ],
                                    "children": [
                                      {
                                        "row": [
                                          {
                                            "column": [
                                              {
                                                "row": {
                                                  "verticalAlignment": "bottom",
                                                  "children": [
                                                    {
                                                      "text": {
                                                        "textFromFloat": { "value": 47739.32, "whole": 8, "decimal": 0, "flags": ${Rc.TextFromFloat.PAD_PRE_NONE or Rc.TextFromFloat.GROUPING_BY3 or Rc.TextFromFloat.PAD_AFTER_ZERO} },
                                                        "color": "@colors.stockPrice",
                                                        "fontSize": "@vars.priceDollars"
                                                      }
                                                    },
                                                    {
                                                      "text": {
                                                        "textFromFloat": { "value": 47739.32, "whole": 0, "decimal": 2, "flags": ${Rc.TextFromFloat.PAD_PRE_NONE or Rc.TextFromFloat.GROUPING_BY3 or Rc.TextFromFloat.PAD_AFTER_ZERO} },
                                                        "color": "@colors.stockName",
                                                        "fontSize": "@vars.priceCents"
                                                      }
                                                    }
                                                  ]
                                                }
                                              },
                                              {
                                                "row": {
                                                  "modifiers": [ { "padding": { "top": 16.0 } } ],
                                                  "children": [
                                                    { "text": { "value": "Dow Jones", "fontSize": "@vars.nameFontSize", "color": "@colors.stockName" } },
                                                    { "text": { "value": "-0.45%", "fontSize": "@vars.nameFontSize", "color": "@colors.dotColor", "modifiers": [ { "padding": { "start": 8.0 } } ] } }
                                                  ]
                                                }
                                              }
                                            ]
                                          },
                                          { "spacer": {} },
                                          {
                                            "box": {
                                              "horizontalAlignment": "center",
                                              "verticalAlignment": "center",
                                              "modifiers": [ { "width": 120.0 }, { "height": 120.0 }, { "padding": 16.0 }, { "clip": { "type": "roundRect", "radius": 60.0 } }, { "background": "@colors.dotColor" } ],
                                              "children": [ { "text": { "value": "\u2193", "fontSize": 48.0, "color": "@colors.arrowColor" } } ]
                                            }
                                          }
                                        ]
                                      },
                                      {
                                        "canvas": {
                                          "modifiers": [ { "height": 260.0 }, "fillMaxWidth" ],
                                          "commands": [
                                            { "variable": { "name": "w", "value": "width", "commit": true } },
                                            { "variable": { "name": "h", "value": "height", "commit": true } },
                                            { "variable": { "name": "cx", "value": "@vars.w / 2.0", "commit": true } },
                                            { "variable": { "name": "cy", "value": "@vars.h / 2.0", "commit": true } },
                                            { "variable": { "name": "rad", "value": "min(@vars.cx, @vars.cy)", "commit": true } },
                                            { "variable": { "name": "stockValues", "value": [ ${stockDataJson} ], "vtype": "floatArrays" } },
                                            { "variable": { "name": "margin", "value": "@vars.rad * 0.3", "commit": true } },
                                            { "variable": { "name": "lineBottom", "value": "@vars.h - @vars.margin", "commit": true } },
                                            { "pathCreate": { "id": "graphPath", "x": "@vars.margin", "y": "@vars.lineBottom" } },
                                            { "variable": { "name": "maxValue", "value": "arrayMax(@vars.stockValues)", "commit": true } },
                                            { "variable": { "name": "minValue", "value": "arrayMin(@vars.stockValues) - 100.0", "commit": true } },
                                            { "variable": { "name": "xEnd", "value": "@vars.w - @vars.margin", "commit": true } },
                                            {
                                              "loop": {
                                                "from": "@vars.margin", "step": 1.0, "until": "@vars.xEnd",
                                                "index": "index",
                                                "commands": [
                                                  { "variable": { "name": "pos", "value": "(@vars.index - @vars.margin) / (@vars.w - @vars.margin * 2.0)", "commit": true } },
                                                  { "variable": { "name": "v", "value": "(arraySpline(@vars.stockValues, @vars.pos) - @vars.minValue) / (@vars.maxValue - @vars.minValue)", "commit": true } },
                                                  { "variable": { "name": "y", "value": "@vars.lineBottom - @vars.v * (@vars.lineBottom - @vars.margin)", "commit": true } },
                                                  { "pathAppendLineTo": { "path": "graphPath", "x": "@vars.index", "y": "@vars.y" } }
                                                ]
                                              }
                                            },
                                            { "pathAppendLineTo": { "path": "graphPath", "x": "@vars.xEnd", "y": "@vars.lineBottom" } },
                                            { "pathAppendClose": { "path": "graphPath" } },
                                            {
                                              "paint": {
                                                "ops": [
                                                  { "style": "fill" },
                                                  { "linearGradient": { "x1": 0.0, "y1": 0.0, "x2": 0.0, "y2": "@vars.lineBottom", "colors": [ "@colors.dotColor", 0 ], "tileMode": 0 } },
                                                  { "pathEffect": null },
                                                  { "color": "#000000" }
                                                ]
                                              }
                                            },
                                            { "save": {} },
                                            { "clipRect": { "left": "@vars.margin + 5.0", "top": "@vars.margin + 5.0", "right": "@vars.xEnd - 5.0", "bottom": "@vars.lineBottom - 5.0" } },
                                            { "drawPath": "graphPath" },
                                            {
                                              "paint": {
                                                "ops": [
                                                  { "shader": 0 },
                                                  { "color": "@colors.dotColor" },
                                                  { "style": "stroke" },
                                                  { "width": 6.0 }
                                                ]
                                              }
                                            },
                                            { "drawPath": "graphPath" },
                                            { "restore": {} }
                                          ]
                                        }
                                      }
                                    ]
                                  }
                                },
                                {
                                  "flow": {
                                    "modifiers": [ "fillMaxWidth" ],
                                    "children": [
                                      {
                                        "row": {
                                          "modifiers": [ { "padding": [32.0, 0.0, 32.0, 28.0] }, { "clip": { "type": "roundRect", "radius": 48.0 } }, { "background": "@colors.panels" }, { "weight": 1.0 }, { "widthIn": [120.0, 3.4028235e38] }, { "padding": 24.0 } ],
                                          "children": [
                                            {
                                              "column": [
                                                {
                                                  "row": {
                                                    "verticalAlignment": "bottom",
                                                    "children": [
                                                      { "text": { "textFromFloat": { "value": 6846.51, "whole": 8, "decimal": 0, "flags": ${Rc.TextFromFloat.PAD_PRE_NONE or Rc.TextFromFloat.GROUPING_BY3 or Rc.TextFromFloat.PAD_AFTER_ZERO} }, "fontSize": "@vars.priceDollars", "color": "@colors.stockPrice" } },
                                                      { "text": { "textFromFloat": { "value": 6846.51, "whole": 0, "decimal": 2, "flags": ${Rc.TextFromFloat.PAD_PRE_NONE or Rc.TextFromFloat.GROUPING_BY3 or Rc.TextFromFloat.PAD_AFTER_ZERO} }, "fontSize": "@vars.priceCents", "color": "@colors.stockName" } }
                                                    ]
                                                  }
                                                },
                                                {
                                                  "row": [
                                                    {
                                                      "column": [
                                                        { "text": { "value": "S&P 500", "fontSize": "@vars.defaultFontSize", "color": "@colors.stockName" } },
                                                        { "text": { "value": "-0.35%", "fontSize": "@vars.defaultFontSize", "color": "@colors.dotColor" } }
                                                      ]
                                                    },
                                                    { "spacer": {} },
                                                    {
                                                      "box": {
                                                        "horizontalAlignment": "center",
                                                        "verticalAlignment": "center",
                                                        "modifiers": [ { "width": 120.0 }, { "height": 120.0 }, { "padding": 16.0 }, { "clip": { "type": "roundRect", "radius": 60.0 } }, { "background": "@colors.dotColor" } ],
                                                        "children": [ { "text": { "value": "\u2193", "fontSize": 48.0, "color": "@colors.arrowColor" } } ]
                                                      }
                                                    }
                                                  ]
                                                }
                                              ]
                                            }
                                          ]
                                        }
                                      },
                                      {
                                        "row": {
                                          "modifiers": [ { "padding": [32.0, 0.0, 32.0, 28.0] }, { "clip": { "type": "roundRect", "radius": 48.0 } }, { "background": "@colors.panels" }, { "weight": 1.0 }, { "widthIn": [120.0, 3.4028235e38] }, { "padding": 24.0 } ],
                                          "children": [
                                            {
                                              "column": [
                                                {
                                                  "row": {
                                                    "verticalAlignment": "bottom",
                                                    "children": [
                                                      { "text": { "textFromFloat": { "value": 23545.9, "whole": 8, "decimal": 0, "flags": ${Rc.TextFromFloat.PAD_PRE_NONE or Rc.TextFromFloat.GROUPING_BY3 or Rc.TextFromFloat.PAD_AFTER_ZERO} }, "fontSize": "@vars.priceDollars", "color": "@colors.stockPrice" } },
                                                      { "text": { "textFromFloat": { "value": 23545.9, "whole": 0, "decimal": 2, "flags": ${Rc.TextFromFloat.PAD_PRE_NONE or Rc.TextFromFloat.GROUPING_BY3 or Rc.TextFromFloat.PAD_AFTER_ZERO} }, "fontSize": "@vars.priceCents", "color": "@colors.stockName" } }
                                                    ]
                                                  }
                                                },
                                                {
                                                  "row": [
                                                    {
                                                      "column": [
                                                        { "text": { "value": "Nasdaq", "fontSize": "@vars.defaultFontSize", "color": "@colors.stockName" } },
                                                        { "text": { "value": "-0.14%", "fontSize": "@vars.defaultFontSize", "color": "@colors.dotColor" } }
                                                      ]
                                                    },
                                                    { "spacer": {} },
                                                    {
                                                      "box": {
                                                        "horizontalAlignment": "center",
                                                        "verticalAlignment": "center",
                                                        "modifiers": [ { "width": 120.0 }, { "height": 120.0 }, { "padding": 16.0 }, { "clip": { "type": "roundRect", "radius": 60.0 } }, { "background": "@colors.dotColor" } ],
                                                        "children": [ { "text": { "value": "\u2193", "fontSize": 48.0, "color": "@colors.arrowColor" } } ]
                                                      }
                                                    }
                                                  ]
                                                }
                                              ]
                                            }
                                          ]
                                        }
                                      },
                                      {
                                        "row": {
                                          "modifiers": [ { "padding": [32.0, 0.0, 32.0, 28.0] }, { "clip": { "type": "roundRect", "radius": 48.0 } }, { "background": "@colors.panels" }, { "weight": 1.0 }, { "widthIn": [120.0, 3.4028235e38] }, { "padding": 24.0 } ],
                                          "children": [
                                            {
                                              "column": [
                                                {
                                                  "row": {
                                                    "verticalAlignment": "bottom",
                                                    "children": [
                                                      { "text": { "textFromFloat": { "value": 2520.98, "whole": 8, "decimal": 0, "flags": ${Rc.TextFromFloat.PAD_PRE_NONE or Rc.TextFromFloat.GROUPING_BY3 or Rc.TextFromFloat.PAD_AFTER_ZERO} }, "fontSize": "@vars.priceDollars", "color": "@colors.stockPrice" } },
                                                      { "text": { "textFromFloat": { "value": 2520.98, "whole": 0, "decimal": 2, "flags": ${Rc.TextFromFloat.PAD_PRE_NONE or Rc.TextFromFloat.GROUPING_BY3 or Rc.TextFromFloat.PAD_AFTER_ZERO} }, "fontSize": "@vars.priceCents", "color": "@colors.stockName" } }
                                                    ]
                                                  }
                                                },
                                                {
                                                  "row": [
                                                    {
                                                      "column": [
                                                        { "text": { "value": "Russell", "fontSize": "@vars.defaultFontSize", "color": "@colors.stockName" } },
                                                        { "text": { "value": "-0.020%", "fontSize": "@vars.defaultFontSize", "color": "@colors.dotColor" } }
                                                      ]
                                                    },
                                                    { "spacer": {} },
                                                    {
                                                      "box": {
                                                        "horizontalAlignment": "center",
                                                        "verticalAlignment": "center",
                                                        "modifiers": [ { "width": 120.0 }, { "height": 120.0 }, { "padding": 16.0 }, { "clip": { "type": "roundRect", "radius": 60.0 } }, { "background": "@colors.dotColor" } ],
                                                        "children": [ { "text": { "value": "\u2193", "fontSize": 48.0, "color": "@colors.arrowColor" } } ]
                                                      }
                                                    }
                                                  ]
                                                }
                                              ]
                                            }
                                          ]
                                        }
                                      },
                                      {
                                        "row": {
                                          "modifiers": [ { "padding": [32.0, 0.0, 32.0, 28.0] }, { "clip": { "type": "roundRect", "radius": 48.0 } }, { "background": "@colors.panels" }, { "weight": 1.0 }, { "widthIn": [120.0, 3.4028235e38] }, { "padding": 24.0 } ],
                                          "children": [
                                            {
                                              "column": [
                                                {
                                                  "row": {
                                                    "verticalAlignment": "bottom",
                                                    "children": [
                                                      { "text": { "textFromFloat": { "value": 21703.2, "whole": 8, "decimal": 0, "flags": ${Rc.TextFromFloat.PAD_PRE_NONE or Rc.TextFromFloat.GROUPING_BY3 or Rc.TextFromFloat.PAD_AFTER_ZERO} }, "fontSize": "@vars.priceDollars", "color": "@colors.stockPrice" } },
                                                      { "text": { "textFromFloat": { "value": 21703.2, "whole": 0, "decimal": 2, "flags": ${Rc.TextFromFloat.PAD_PRE_NONE or Rc.TextFromFloat.GROUPING_BY3 or Rc.TextFromFloat.PAD_AFTER_ZERO} }, "fontSize": "@vars.priceCents", "color": "@colors.stockName" } }
                                                    ]
                                                  }
                                                },
                                                {
                                                  "row": [
                                                    {
                                                      "column": [
                                                        { "text": { "value": "NYA", "fontSize": "@vars.defaultFontSize", "color": "@colors.stockName" } },
                                                        { "text": { "value": "-0.49%", "fontSize": "@vars.defaultFontSize", "color": "@colors.dotColor" } }
                                                      ]
                                                    },
                                                    { "spacer": {} },
                                                    {
                                                      "box": {
                                                        "horizontalAlignment": "center",
                                                        "verticalAlignment": "center",
                                                        "modifiers": [ { "width": 120.0 }, { "height": 120.0 }, { "padding": 16.0 }, { "clip": { "type": "roundRect", "radius": 60.0 } }, { "background": "@colors.dotColor" } ],
                                                        "children": [ { "text": { "value": "\u2193", "fontSize": 48.0, "color": "@colors.arrowColor" } } ]
                                                      }
                                                    }
                                                  ]
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
                                  "box": {
                                    "horizontalAlignment": "center",
                                    "verticalAlignment": "center",
                                    "modifiers": [ { "padding": 8.0 }, { "height": 100.0 }, { "clip": { "type": "roundRect", "radius": 48.0 } }, { "background": "@colors.followText" }, { "padding": 4.0 }, { "clip": { "type": "roundRect", "radius": 48.0 } }, { "background": "@colors.background" }, { "padding": 16.0 } ],
                                    "children": [
                                      {
                                        "row": {
                                          "verticalAlignment": "center",
                                          "children": [
                                            { "text": { "value": "+ ", "color": "@colors.followText", "fontSize": 48.0 } },
                                            { "text": { "value": "Follow investments", "color": "@colors.followText", "fontSize": "@vars.defaultFontSize" } }
                                          ]
                                        }
                                      }
                                    ]
                                  }
                                },
                                { "variable": { "name": "sHeight", "value": "height", "commit": true } }
                              ]
                            }
                          }
                        ]
                      }
                    },
                    {
                "canvas": {
                  "modifiers": [ "fillMaxSize" ],
                  "commands": [
                    { "variable": { "name": "w", "value": "width", "commit": true } },
                    { "variable": { "name": "h", "value": "height", "commit": true } },
                    { "variable": { "name": "alpha", "value": { "value": "sign(max(0.0, touchTime - animationTime + 0.1))", "anim": 1.2 }, "commit": true } },
                    {
                      "paint": {
                        "ops": [
                          { "color": "@colors.stockName" },
                          { "alpha": "@vars.alpha" },
                          { "width": 10.0 }
                        ]
                      }
                    },
                    { "variable": { "name": "safeSize", "value": "max(1.0, sHeight)", "commit": true } },
                    { "variable": { "name": "len", "value": "h * h / @vars.safeSize", "commit": true } },
                    { "variable": { "name": "off", "value": "h * 0.0 / @vars.safeSize", "commit": true } },
                    { "drawLine": { "x1": "@vars.w - 5.0", "y1": "@vars.off", "x2": "@vars.w - 5.0", "y2": "@vars.off + @vars.len" } }
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

@Preview @Composable private fun RcJsonTickerPreview() = RemoteDocumentPreview(rcJsonTicker())

@Suppress("RestrictedApiAndroidX")
fun generateStockDataArrayLocal(
    numPoints: Int,
    startPrice: Float,
    annualDrift: Float,
    annualVolatility: Float,
    daysPerPoint: Float,
): FloatArray {
    val random = Random(42)
    val prices = FloatArray(numPoints)
    prices[0] = startPrice
    val dt = daysPerPoint / 252.0
    val drift = annualDrift / 100.0
    val volatility = annualVolatility / 100.0
    for (i in 1 until numPoints) {
        val u1 = random.nextFloat()
        val u2 = random.nextFloat()
        val randNormal =
            (sqrt(-2.0 * ln(u1.toDouble())) * cos(2.0 * Math.PI * u2.toDouble())).toFloat()
        val driftTerm = (drift - volatility * volatility / 2.0) * dt
        val randomTerm = volatility * sqrt(dt) * randNormal
        prices[i] = prices[i - 1] * exp(driftTerm + randomTerm).toFloat()
    }
    return prices
}
