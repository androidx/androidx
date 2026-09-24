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

package androidx.wear.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.background
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.foundation.sizeIn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.curvedText

class SimpleCurvedLayoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var showCurvedText by remember { mutableStateOf(true) }
                if (showCurvedText) {
                    val style = MaterialTheme.typography.arcMedium
                    CurvedLayout {
                        curvedRow {
                            curvedText(
                                "Curved Text",
                                style =
                                    style.merge(
                                        CurvedTextStyle(
                                            warpOffset =
                                                CurvedTextStyle.WarpOffset.HalfOpticalHeight
                                        )
                                    ),
                            )
                        }
                    }
                }
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Button(
                        onClick = { showCurvedText = !showCurvedText },
                        modifier = Modifier.semantics { contentDescription = TOGGLE_DISPLAY },
                    ) {
                        Text("Toggle")
                    }
                }
            }
        }
    }
}

class ComplexCurvedLayoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var showCurvedText by remember { mutableStateOf(true) }
                if (showCurvedText) {
                    val style = MaterialTheme.typography.arcMedium
                    CurvedLayout {
                        curvedRow(
                            CurvedModifier.padding(ArcPaddingValues(2.dp))
                                .background(Color(0xff0077ff), StrokeCap.Round)
                                .sizeIn(minThickness = 30.dp, maxThickness = 30.dp)
                        ) {
                            curvedComposable {
                                Box(Modifier.paint(painterResource(R.drawable.ic_favorite_rounded)))
                            }
                            curvedComposable { Spacer(Modifier.size(4.dp)) }
                            curvedText(
                                "Curved Text",
                                style =
                                    style.merge(
                                        CurvedTextStyle(
                                            warpOffset =
                                                CurvedTextStyle.WarpOffset.HalfOpticalHeight
                                        )
                                    ),
                            )
                            curvedComposable { SwitchButton(showCurvedText, {}) {} }
                            curvedText(
                                "More Text",
                                style =
                                    style.merge(
                                        CurvedTextStyle(
                                            warpOffset =
                                                CurvedTextStyle.WarpOffset.HalfOpticalHeight
                                        )
                                    ),
                            )
                        }
                    }
                }

                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Button(
                        onClick = { showCurvedText = !showCurvedText },
                        modifier = Modifier.semantics { contentDescription = TOGGLE_DISPLAY },
                    ) {
                        Text("Toggle")
                    }
                }
            }
        }
    }
}

private const val TOGGLE_DISPLAY = "TOGGLE_DISPLAY"
