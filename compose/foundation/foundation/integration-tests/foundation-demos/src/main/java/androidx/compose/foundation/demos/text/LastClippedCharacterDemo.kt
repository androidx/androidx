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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun LastClippedCharacterDemo() {
    var lastCharacterBox by remember { mutableStateOf<Rect?>(null) }

    var overflow by remember { mutableStateOf(false) }
    var height by remember { mutableIntStateOf(20) }
    Column {
        Box(modifier = Modifier.width(100.dp).height(height.dp).border(1.dp, Color.Red)) {
            BasicText(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
                overflow =
                    if (!overflow) {
                        TextOverflow.Clip
                    } else {
                        TextOverflow.Visible
                    },
                modifier =
                    Modifier.drawWithContent {
                        drawContent()
                        lastCharacterBox?.let { box ->
                            drawRoundRect(
                                Color.Green,
                                box.topLeft,
                                box.size,
                                CornerRadius(4f, 4f),
                                Stroke(2f)
                            )
                        }
                    },
                onTextLayout = { textLayoutResult ->
                    val lastNonClippedLine = textLayoutResult.findLastNonClippedLine()
                    // this finds the newline
                    val actualLastCharacter =
                        textLayoutResult.getOffsetForPosition(
                            Offset(
                                textLayoutResult.size.width.toFloat(),
                                textLayoutResult.lineVerticalMiddle(lastNonClippedLine),
                            )
                        )
                    lastCharacterBox =
                        textLayoutResult
                            .getBoundingBox(actualLastCharacter)
                            .translate(Offset(0f, textLayoutResult.getLineTop(lastNonClippedLine)))
                },
            )
        }
        Spacer(modifier = Modifier.height(200.dp))
        Button(onClick = { overflow = !overflow }) { Text("Show overflow") }
        Button(onClick = { height += 5 }) { Text("Increase clip height (5)") }
        Button(onClick = { height = 2 }) { Text("Reset height (+5)") }

        Text("For more information see b/319500907")
        Text(
            "Note that this demo considers a line non-clipped when any pixel of the line is" +
                " not clipped, change the logic in findLastNonClippedLine to fit your needs"
        )
    }
}

private fun TextLayoutResult.lineVerticalMiddle(line: Int): Float {
    return (getLineBottom(line) - getLineTop(line)) / 2
}

private fun TextLayoutResult.findLastNonClippedLine(): Int {
    // if N>>10 write a binary search here, but assuming N~=1 a linear walk is fine
    var cur = 0
    val localLineCount = lineCount
    val height = size.height
    // walk while lines don't exceed height

    // this will consider a line non-clipped as soon as 1px is non-clipped
    // however, it is likely most lines won't draw anything in this region - you may want
    // to adjust this to have an offset based on total line vertical area
    // (getLineBottom - getLineTop) if you want some pixels to show before considering a line
    // non-clipped
    while (cur < localLineCount && getLineTop(cur) < height) {
        cur++
    }
    // we walked one too far, go back a line
    return (cur - 1).coerceAtLeast(0)
}
