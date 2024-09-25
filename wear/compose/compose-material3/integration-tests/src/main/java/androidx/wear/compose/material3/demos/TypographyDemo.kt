/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.material3.CurvedTextDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextToggleButton
import androidx.wear.compose.material3.curvedText

var TypographyDemos =
    listOf(
        DemoCategory(
            "Arc",
            listOf(
                ComposableDemo("Arc Small") {
                    ArcWithLetterSpacing(MaterialTheme.typography.arcSmall, "Arc Small")
                },
                ComposableDemo("Arc Medium") {
                    ArcWithLetterSpacing(MaterialTheme.typography.arcMedium, "Arc Medium")
                },
                ComposableDemo("Arc Large") {
                    ArcWithLetterSpacing(MaterialTheme.typography.arcLarge, "Arc Large")
                },
            )
        ),
        DemoCategory(
            "Display",
            listOf(
                ComposableDemo("Display Small") {
                    Centralize {
                        Text(
                            "Display\nSmall",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                },
                ComposableDemo("Display Medium") {
                    Centralize {
                        Text(
                            "Display\nMedium",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                },
                ComposableDemo("Display Large") {
                    Centralize {
                        Text(
                            "Display\nLarge",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.displayLarge
                        )
                    }
                }
            )
        ),
        ComposableDemo("Title") {
            Centralize {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Title\nSmall",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Title\nMedium",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Title\nLarge",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        ComposableDemo("Label") {
            Centralize {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Label\nSmall",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Label\nMedium",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Label\nLarge",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        ComposableDemo("Body") {
            Centralize {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Body\nExtra\nSmall",
                        style = MaterialTheme.typography.bodyExtraSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Body\nSmall",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Body\nMedium",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Body\nLarge",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        DemoCategory(
            "Numeral",
            listOf(
                ComposableDemo("Extra Small") {
                    Centralize {
                        Text("0123\n6789", style = MaterialTheme.typography.numeralExtraSmall)
                    }
                },
                ComposableDemo("Small") {
                    Centralize { Text("0123\n6789", style = MaterialTheme.typography.numeralSmall) }
                },
                ComposableDemo("Medium") {
                    Centralize {
                        Text("0123\n6789", style = MaterialTheme.typography.numeralMedium)
                    }
                },
                ComposableDemo("Large") {
                    Centralize { Text("0123\n6789", style = MaterialTheme.typography.numeralLarge) }
                },
                ComposableDemo("Extra Large") {
                    Centralize {
                        Text("0123\n6789", style = MaterialTheme.typography.numeralExtraLarge)
                    }
                }
            )
        ),
    )

@Composable
private fun ArcWithLetterSpacing(arcStyle: TextStyle, label: String) {
    var topLetterSpacing by remember { mutableStateOf(0.6f) }
    var bottomLetterSpacing by remember { mutableStateOf(2.0f) }
    val topCurvedStyle = CurvedTextStyle(arcStyle).copy(letterSpacing = topLetterSpacing.sp)
    val bottomCurvedStyle = CurvedTextStyle(arcStyle).copy(letterSpacing = bottomLetterSpacing.sp)
    val mmms = "MMMMMMMMMMMMMMMMMMMM"
    var useMMMs by remember { mutableStateOf(true) }

    Box {
        CurvedLayout {
            curvedText(
                if (useMMMs) mmms else label,
                style = topCurvedStyle,
                maxSweepAngle = CurvedTextDefaults.StaticContentMaxSweepAngle,
                overflow = TextOverflow.Ellipsis
            )
        }
        CurvedLayout(anchor = 90f, angularDirection = CurvedDirection.Angular.Reversed) {
            curvedText(
                if (useMMMs) mmms else label,
                style = bottomCurvedStyle,
                maxSweepAngle = CurvedTextDefaults.StaticContentMaxSweepAngle,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Top=$topLetterSpacing, bottom = $bottomLetterSpacing",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Slider(
                value = topLetterSpacing,
                onValueChange = { topLetterSpacing = it },
                valueRange = 0f..4f,
                steps = 39,
                segmented = false
            )
            Slider(
                value = bottomLetterSpacing,
                onValueChange = { bottomLetterSpacing = it },
                valueRange = 0f..4f,
                steps = 39,
                segmented = false
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextToggleButton(
                    checked = useMMMs,
                    onCheckedChange = { useMMMs = !useMMMs },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(text = "MMM")
                }
            }
        }
    }
}
