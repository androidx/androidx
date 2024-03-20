/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LookaheadWithIntrinsicsDemo() {
    Column {
        LookaheadScope {
            var isWide by remember { mutableStateOf(true) }
            Column {
                Button(
                    modifier = Modifier.padding(top = 20.dp, bottom = 20.dp),
                    onClick = { isWide = !isWide }
                ) {
                    Text("Toggle")
                }
                Text("IntrinsicSize.Min Column")
                Spacer(Modifier.size(5.dp))
                Column(
                    Modifier.background(Color(0xfffdedac), RoundedCornerShape(10))
                        .padding(20.dp)
                        .width(IntrinsicSize.Min)
                ) {
                    Box(
                        Modifier.animateBounds(
                                lookaheadScope = this@LookaheadScope,
                                if (isWide) Modifier.width(300.dp) else Modifier.width(150.dp)
                            )
                            .height(50.dp)
                            .background(colors[1])
                    ) {
                        Text("Width: ${if (isWide) 300 else 150}.dp")
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(colors[2])) {
                        Text("Match parent")
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(colors[3])) {
                        Text("Match parent", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MatchParentDividerForText() {
    // Builds a layout containing two pieces of text separated by a divider, where the divider
    // is sized according to the height of the longest text.
    //
    // Here height min intrinsic is adding a height premeasurement pass for the Row,
    // whose minimum intrinsic height will correspond to the height of the largest Text. Then
    // height min intrinsic will measure the Row with tight height, the same as the
    // premeasured minimum intrinsic height, which due to fillMaxHeight will force the Texts and
    // the divider to use the same height.
    Box {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Text(text = "This is a really short text", modifier = Modifier.fillMaxHeight())
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            Text(
                text =
                    "This is a much much much much much much much much much much" +
                        " much much much much much much longer text",
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

@Composable
fun SameWidthTextBoxes() {
    // Builds a layout containing three Text boxes having the same width as the widest one.
    //
    // Here width max intrinsic is adding a width premeasurement pass for the Column,
    // whose maximum intrinsic width will correspond to the preferred width of the largest
    // Box. Then width max intrinsic will measure the Column with tight width, the
    // same as the premeasured maximum intrinsic width, which due to fillMaxWidth modifiers will
    // force the Boxs to use the same width.

    Box {
        Column(Modifier.width(IntrinsicSize.Min).fillMaxHeight()) {
            Box(Modifier.fillMaxWidth().background(Color.Gray)) { Text("Short text") }
            Box(Modifier.fillMaxWidth().background(Color.Blue)) {
                Text("Extremely long text giving the width of its siblings")
            }
            Box(Modifier.fillMaxWidth().background(Color.Magenta)) { Text("Medium length text") }
        }
    }
}

@Composable
fun MatchParentDividerForAspectRatio() {
    // Builds a layout containing two aspectRatios separated by a divider, where the divider
    // is sized according to the height of the taller aspectRatio.
    //
    // Here height max intrinsic is adding a height premeasurement pass for the
    // Row, whose maximum intrinsic height will correspond to the height of the taller
    // aspectRatio. Then height max intrinsic will measure the Row with tight height,
    // the same as the premeasured maximum intrinsic height, which due to fillMaxHeight modifier
    // will force the aspectRatios and the divider to use the same height.
    //
    Box {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            val modifier = Modifier
            Box(modifier.width(160.dp).aspectRatio(2f).background(Color.Gray))
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            Box(modifier.widthIn(120.dp, 200.dp).aspectRatio(1f).background(Color.Blue))
        }
    }
}

private val colors =
    listOf(Color(0xffff6f69), Color(0xffffcc5c), Color(0xff2a9d84), Color(0xff264653))
