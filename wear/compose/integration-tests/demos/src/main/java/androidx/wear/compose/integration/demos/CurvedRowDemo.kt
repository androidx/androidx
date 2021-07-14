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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.AnchorType
import androidx.wear.compose.foundation.CurvedRow
import androidx.wear.compose.foundation.CurvedRowScope
import androidx.wear.compose.foundation.RadialAlignment
import androidx.wear.compose.material.Text

@Composable
fun CurvedRowDemo() {
    CurvedRow(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.Red)
        )
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
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.Red)
        )
    }
    CurvedRow(
        anchor = 90F,
        anchorType = AnchorType.Start,
        clockwise = false
    ) {
        Text(
            text = "Start",
            color = Color.Black,
            fontSize = 30.sp,
            modifier = Modifier.background(Color.White).padding(horizontal = 10.dp)
        )
    }
    CurvedRow(
        anchor = 90F,
        anchorType = AnchorType.End,
        clockwise = false
    ) {
        Text(
            text = "End",
            color = Color.Black,
            fontSize = 30.sp,
            modifier = Modifier.background(Color.White).padding(horizontal = 10.dp)
        )
    }
}

@Composable
private fun CurvedRowScope.SeparatorBlock() {
    Box(
        modifier = Modifier
            .size(10.dp, 40.dp)
            .background(Color.Gray)
            .radialAlignment(RadialAlignment.Outer)
    )
}

@Composable
private fun CurvedRowScope.RgbBlocks() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color.Red)
            .radialAlignment(RadialAlignment.Outer)
    )
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color.Green)
            .radialAlignment(RadialAlignment.Center)
    )
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color.Blue)
            .radialAlignment(RadialAlignment.Inner)
    )
}

@Composable
fun CurvedRowAlignmentDemo() {
    CurvedRow(modifier = Modifier.fillMaxSize()) {
        SeparatorBlock()
        RgbBlocks()
        SeparatorBlock()
        (0..10).forEach {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.White)
                    .radialAlignment(RadialAlignment.Custom(it / 10.0f))
            )
        }
        SeparatorBlock()
    }
    CurvedRow(
        anchor = 90f,
        clockwise = false
    ) {
        SeparatorBlock()
        RgbBlocks()
        SeparatorBlock()
    }
}
