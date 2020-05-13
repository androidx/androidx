/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text.demos

import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.compose.state
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.clipToBounds
import androidx.ui.core.tag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextField
import androidx.ui.foundation.VerticalScroller
import androidx.ui.foundation.drawBackground
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.OffsetMap
import androidx.ui.input.PasswordVisualTransformation
import androidx.ui.input.TransformedText
import androidx.ui.input.VisualTransformation
import androidx.ui.layout.Column
import androidx.ui.text.AnnotatedString
import androidx.ui.text.LocaleList
import androidx.ui.foundation.TextFieldValue
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.height
import androidx.ui.layout.padding
import androidx.ui.savedinstancestate.savedInstanceState
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextStyle
import androidx.ui.text.toUpperCase
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.unit.min
import androidx.ui.unit.px

@Composable
fun TailFollowingTextFieldDemo() {
    Column {
        val hstate = savedInstanceState(saver = TextFieldValue.Saver) {
            TextFieldValue("abc def ghi jkl mno pqr stu vwx yz")
        }
        HorizontalTailFollowingTextField(
            value = hstate.value,
            onValueChange = { hstate.value = it },
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .drawBackground(Color.Gray)
                .clipToBounds()
        )

        val vstate = savedInstanceState(saver = TextFieldValue.Saver) {
            TextFieldValue("a\nb\nc\nd\ne\nf\ng\nh")
        }
        VerticalTailFollowintTextField(
            value = vstate.value,
            onValueChange = { vstate.value = it },
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .height(120.dp)
                .drawBackground(Color.Gray)
                .clipToBounds()
        )
    }
}

@Composable
private fun HorizontalTailFollowingTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    textStyle: TextStyle = TextStyle(fontSize = fontSize8)
) {
    Layout(
        children = @Composable() {
            TextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle
            )
        },
        modifier = modifier
    ) { measurable, constraints, _ ->

        val p = measurable[0].measure(
            Constraints(
                minWidth = IntPx.Zero,
                maxWidth = IntPx.Infinity,
                minHeight = constraints.minHeight,
                maxHeight = constraints.maxHeight
            )
        )

        val width = p.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val xOffset = min(IntPx.Zero, constraints.maxWidth - p.width)

        layout(width, p.height) {
            p.place(xOffset, 0.ipx)
        }
    }
}

@Composable
private fun VerticalTailFollowintTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    textStyle: TextStyle = TextStyle(fontSize = fontSize8)
) {
    Layout(
        children = @Composable() {
            TextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle
            )
        },
        modifier = modifier
    ) { measurable, constraints, _ ->

        val p = measurable[0].measure(
            Constraints(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                minHeight = IntPx.Zero,
                maxHeight = IntPx.Infinity
            )
        )

        val height = min(p.height, constraints.maxHeight)
        val yOffset = min(IntPx.Zero, constraints.maxHeight - p.height)

        layout(p.width, height) {
            p.place(0.ipx, yOffset)
        }
    }
}