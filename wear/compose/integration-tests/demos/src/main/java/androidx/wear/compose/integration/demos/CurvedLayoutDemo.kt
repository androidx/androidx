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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.AnchorType
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.sizeIn
import androidx.wear.compose.foundation.weight
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.curvedText

@Composable
fun CurvedWorldDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        curvedComposable {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.Red)
            )
        }
        curvedComposable {
            Column(
                modifier = Modifier
                    .background(Color.Gray)
                    .padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A", color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.background(Color.Blue)
                )
                Row {
                    Text(
                        text = "B",
                        color = Color.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.background(Color.Green).padding(2.dp)
                    )
                    Text(
                        text = "C",
                        color = Color.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.background(Color.Red)
                    )
                }
            }
        }
        curvedComposable {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.Red)
            )
        }
    }
    CurvedLayout(
        anchor = 90F,
        anchorType = AnchorType.Start,
        clockwise = false
    ) {
        curvedComposable {
            Text(
                text = "Start",
                color = Color.Black,
                fontSize = 30.sp,
                modifier = Modifier.background(Color.White).padding(horizontal = 10.dp)
            )
        }
    }
    CurvedLayout(
        anchor = 90F,
        anchorType = AnchorType.End,
        clockwise = false
    ) {
        curvedComposable {
            Text(
                text = "End",
                color = Color.Black,
                fontSize = 30.sp,
                modifier = Modifier.background(Color.White).padding(horizontal = 10.dp)
            )
        }
    }
    CurvedLayout(
        modifier = Modifier.padding(50.dp),
        anchor = 90f,
        anchorType = AnchorType.Center,
        clockwise = false
    ) {
        listOf("A", "B", "C").forEach {
            curvedComposable {
                Text(
                    text = "$it",
                    color = Color.Black,
                    fontSize = 30.sp,
                    modifier = Modifier.background(Color.White).padding(horizontal = 10.dp)
                )
            }
        }
    }
}

private fun CurvedScope.SeparatorBlock() {
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Outer) {
        Box(
            modifier = Modifier
                .size(10.dp, 40.dp)
                .background(Color.Gray)
        )
    }
}

private fun CurvedScope.RgbBlocks() {
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Outer) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.Red)
        )
    }
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Center) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.Green)
        )
    }
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Inner) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.Blue)
        )
    }
}

@Composable
fun CurvedRowAlignmentDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        SeparatorBlock()
        RgbBlocks()
        SeparatorBlock()
        (0..10).forEach {
            curvedComposable(radialAlignment = CurvedAlignment.Radial.Custom(it / 10.0f)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.White)
                )
            }
        }
        SeparatorBlock()
    }
    CurvedLayout(
        anchor = 90f,
        clockwise = false
    ) {
        SeparatorBlock()
        RgbBlocks()
        SeparatorBlock()
    }
}

@Composable
fun BasicCurvedTextDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize().background(Color.White)) {
        SeparatorBlock()
        basicCurvedText(
            "Curved Text",
            CurvedTextStyle(
                fontSize = 18.sp
            ),
            // TODO: Re-add when we implement alignment modifiers.
            // modifier = Modifier.radialAlignment(RadialAlignment.Outer)
        )
        SeparatorBlock()
        basicCurvedText(
            "And More",
            CurvedTextStyle(
                fontSize = 24.sp
            ),
            clockwise = false,
            contentArcPadding = ArcPaddingValues(angular = 5.dp),
            // TODO: Re-add when we implement alignment modifiers.
            // modifier = Modifier.radialAlignment(RadialAlignment.Inner)
        )
        SeparatorBlock()
    }
}

@Composable
fun CurvedEllipsis() {
    CurvedLayout {
        curvedRow(modifier = CurvedModifier.sizeIn(angularMaxDegrees = 90f)) {
            curvedText(
                "This text too long to actually fit in the provided space",
                modifier = CurvedModifier.weight(1f),
                overflow = TextOverflow.Ellipsis
            )
            curvedText(
                "10:00"
            )
        }
    }
}