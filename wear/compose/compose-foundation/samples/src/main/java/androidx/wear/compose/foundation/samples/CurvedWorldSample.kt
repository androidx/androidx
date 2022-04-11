/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.angularSize
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.curvedColumn
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.size
import androidx.wear.compose.foundation.weight

@Sampled
@Composable
fun SimpleCurvedWorld() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        curvedComposable {
            BasicText(
                "Simple",
                Modifier.background(Color.White).padding(2.dp),
                TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                )
            )
        }
        curvedComposable {
            Box(modifier = Modifier.size(20.dp).background(Color.Gray))
        }
        curvedComposable {
            BasicText(
                "CurvedWorld",
                Modifier.background(Color.White).padding(2.dp),
                TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                )
            )
        }
    }
}

@Sampled
@Composable
fun CurvedRowAndColumn() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        curvedComposable {
            Box(modifier = Modifier.size(20.dp).background(Color.Red))
        }
        curvedColumn(angularAlignment = CurvedAlignment.Angular.End) {
            repeat(3) {
                curvedRow {
                    curvedComposable {
                        BasicText(
                            "Row #$it",
                            Modifier.background(Color.White).padding(2.dp),
                            TextStyle(
                                color = Color.Black,
                                fontSize = 14.sp,
                            )
                        )
                    }
                    curvedComposable {
                        Box(modifier = Modifier.size(10.dp).background(Color.Green))
                    }
                    curvedComposable {
                        BasicText(
                            "More",
                            Modifier.background(Color.Yellow).padding(2.dp),
                            TextStyle(
                                color = Color.Black,
                                fontSize = 14.sp,
                            )
                        )
                    }
                }
            }
        }
        curvedComposable {
            Box(modifier = Modifier.size(20.dp).background(Color.Red))
        }
    }
}

@Sampled
@Composable
fun CurvedAndNormalText() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        basicCurvedText(
            "Curved Text",
            style = {
                CurvedTextStyle(
                    fontSize = 16.sp,
                    color = Color.Black,
                    background = Color.White
                )
            },
            contentArcPadding = ArcPaddingValues(10.dp)
        )
        curvedComposable {
            Box(modifier = Modifier.size(20.dp).background(Color.Gray))
        }
        curvedComposable {
            BasicText(
                "Normal Text",
                Modifier.padding(5.dp),
                TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black,
                    background = Color.White
                )
            )
        }
    }
}

@Sampled
@Composable
fun CurvedFixedSize() {
    // Note the "gap" in the middle of the two curved texts has a specified size.
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        basicCurvedText(
            "Fixed",
            style = {
                CurvedTextStyle(
                    fontSize = 16.sp,
                    color = Color.Black,
                    background = Color.White
                )
            }
        )
        curvedRow(
            modifier = CurvedModifier.size(angularSizeDegrees = 5f, radialSize = 30.dp),
        ) { }
        basicCurvedText(
            "Gap",
            style = {
                CurvedTextStyle(
                    fontSize = 16.sp,
                    color = Color.Black,
                    background = Color.White
                )
            }
        )
    }
}

@Sampled
@Composable
fun CurvedWeight() {
    CurvedLayout(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Evenly spread A, B & C in a 90 degree angle.
        curvedRow(
            modifier = CurvedModifier.angularSize(90f)
        ) {
            basicCurvedText("A")
            curvedRow(
                modifier = CurvedModifier.weight(1f),
            ) { }
            basicCurvedText("B")
            curvedRow(
                modifier = CurvedModifier.weight(1f),
            ) { }
            basicCurvedText("C")
        }
    }
}

@Sampled
@Composable
fun CurvedBottomLayout() {
    CurvedLayout(
        modifier = Modifier.fillMaxSize(),
        anchor = 90f,
        angularDirection = CurvedDirection.Angular.Reversed
    ) {
        basicCurvedText(
            "Bottom",
            style = {
                CurvedTextStyle(
                    fontSize = 16.sp,
                    color = Color.Black,
                    background = Color.White
                )
            }
        )
        curvedComposable { Spacer(modifier = Modifier.size(5.dp)) }
        basicCurvedText(
            "text",
            style = {
                CurvedTextStyle(
                    fontSize = 16.sp,
                    color = Color.Black,
                    background = Color.White
                )
            }
        )
    }
}