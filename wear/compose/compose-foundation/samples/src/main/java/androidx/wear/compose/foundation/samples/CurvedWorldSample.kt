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
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.curvedColumn
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.curvedRow

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
        curvedColumn {
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