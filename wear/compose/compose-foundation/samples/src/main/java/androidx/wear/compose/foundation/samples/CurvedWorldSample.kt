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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.angularGradientBackground
import androidx.wear.compose.foundation.angularSize
import androidx.wear.compose.foundation.angularSizeDp
import androidx.wear.compose.foundation.background
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.curvedBox
import androidx.wear.compose.foundation.curvedColumn
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.foundation.radialGradientBackground
import androidx.wear.compose.foundation.radialSize
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
        curvedComposable { Box(modifier = Modifier.size(20.dp).background(Color.Gray)) }
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
        curvedComposable { Box(modifier = Modifier.size(20.dp).background(Color.Red)) }
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
        curvedComposable { Box(modifier = Modifier.size(20.dp).background(Color.Red)) }
    }
}

@Sampled
@Composable
fun CurvedAndNormalText() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        basicCurvedText(
            "Curved Text",
            CurvedModifier.padding(10.dp),
            style = {
                CurvedTextStyle(fontSize = 16.sp, color = Color.Black, background = Color.White)
            }
        )
        curvedComposable { Box(modifier = Modifier.size(20.dp).background(Color.Gray)) }
        curvedComposable {
            BasicText(
                "Normal Text",
                Modifier.padding(5.dp),
                TextStyle(fontSize = 16.sp, color = Color.Black, background = Color.White)
            )
        }
    }
}

@Sampled
@Composable
fun CurvedFixedSize() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        basicCurvedText(
            "45 deg",
            style = { CurvedTextStyle(fontSize = 16.sp, color = Color.Black) },
            modifier =
                CurvedModifier.background(Color.White).size(sweepDegrees = 45f, thickness = 40.dp),
        )
        basicCurvedText(
            "40 dp",
            style = { CurvedTextStyle(fontSize = 16.sp, color = Color.Black) },
            modifier =
                CurvedModifier.background(Color.Yellow).radialSize(40.dp).angularSizeDp(40.dp),
        )
    }
}

@Sampled
@Composable
fun CurvedBackground() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        basicCurvedText(
            "Radial",
            style = { CurvedTextStyle(fontSize = 16.sp, color = Color.Black) },
            modifier =
                CurvedModifier.radialGradientBackground(0f to Color.White, 1f to Color.Black)
                    .padding(5.dp)
        )
        basicCurvedText(
            "Angular",
            style = { CurvedTextStyle(fontSize = 16.sp, color = Color.Black) },
            modifier =
                CurvedModifier.angularGradientBackground(0f to Color.White, 1f to Color.Black)
                    .padding(5.dp)
        )
    }
}

@Sampled
@Composable
fun CurvedWeight() {
    CurvedLayout(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Evenly spread A, B & C in a 90 degree angle.
        curvedRow(modifier = CurvedModifier.angularSize(90f)) {
            basicCurvedText("A")
            curvedRow(
                modifier = CurvedModifier.weight(1f),
            ) {}
            basicCurvedText("B")
            curvedRow(
                modifier = CurvedModifier.weight(1f),
            ) {}
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
                CurvedTextStyle(fontSize = 16.sp, color = Color.Black, background = Color.White)
            }
        )
        curvedComposable { Spacer(modifier = Modifier.size(5.dp)) }
        basicCurvedText(
            "text",
            style = {
                CurvedTextStyle(fontSize = 16.sp, color = Color.Black, background = Color.White)
            }
        )
    }
}

@Sampled
@Composable
fun CurvedBoxSample() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        curvedBox(
            modifier = CurvedModifier.background(Color.Red),
            radialAlignment = CurvedAlignment.Radial.Inner,
            angularAlignment = CurvedAlignment.Angular.End
        ) {
            curvedComposable {
                Box(modifier = Modifier.width(40.dp).height(80.dp).background(Color.Green))
            }
            curvedComposable {
                Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}

@Composable
fun CurvedFontWeight() {
    CurvedLayout(
        modifier = Modifier.fillMaxSize(),
    ) {
        (100..900 step 100).forEach {
            basicCurvedText(
                "W$it",
                style = CurvedTextStyle(color = Color.White, fontWeight = FontWeight(it)),
                modifier = CurvedModifier.padding(5.dp)
            )
        }
    }
}

@Composable
fun CurvedFontHeight() {
    Box(
        modifier =
            Modifier.aspectRatio(1f)
                .fillMaxSize()
                .padding(2.dp)
                .border(2.dp, Color.White, CircleShape)
    ) {
        CurvedLayout() {
            basicCurvedText(
                "9⎪:⎪0",
                style = CurvedTextStyle(color = Color.Green, fontSize = 30.sp),
            )
        }
        CurvedLayout(anchor = 90f, angularDirection = CurvedDirection.Angular.CounterClockwise) {
            basicCurvedText(
                "9⎪:⎪0",
                style = CurvedTextStyle(color = Color.Green, fontSize = 30.sp),
            )
        }
    }
}

@Composable
fun CurvedFonts() {
    CurvedLayout(
        modifier = Modifier.fillMaxSize(),
    ) {
        listOf(
                "Serif" to FontFamily.Serif,
                "SansSerif" to FontFamily.SansSerif,
                "Monospace" to FontFamily.Monospace,
                "Cursive" to FontFamily.Cursive,
            )
            .forEach { (name, ff) ->
                basicCurvedText(
                    "$name",
                    style = CurvedTextStyle(color = Color.White, fontFamily = ff),
                    modifier = CurvedModifier.padding(5.dp)
                )
            }
    }
}

@Composable
fun OversizeComposable() {
    val modBase = CurvedModifier.size(sweepDegrees = 30f, thickness = 20.dp)
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        curvedComposable(modifier = modBase.background(Color.Red)) {}
        curvedComposable(modifier = modBase.background(Color.Green)) {
            Box(Modifier.size(80.dp, 30.dp).background(Color.White))
        }
        curvedComposable(modifier = modBase.background(Color.Blue)) {}
    }
    CurvedLayout(modifier = Modifier.fillMaxSize(), anchor = 90f) {
        curvedComposable(modifier = CurvedModifier.background(Color.Green)) {
            Box(Modifier.size(80.dp, 30.dp).background(Color.White))
        }
    }
}
