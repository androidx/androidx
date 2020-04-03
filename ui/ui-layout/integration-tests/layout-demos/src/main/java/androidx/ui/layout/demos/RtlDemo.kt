/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.layout.demos

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.core.WithConstraints
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.ColumnAlign
import androidx.ui.layout.Row
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.ltr
import androidx.ui.layout.preferredSize
import androidx.ui.layout.rtl
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

@Composable
fun RtlDemo() {
    Column(arrangement = Arrangement.SpaceEvenly) {
        Text("TEXT", Modifier.gravity(ColumnAlign.Center))
        testText()
        Text("ROW", Modifier.gravity(ColumnAlign.Center))
        testRow()
        Text("ROW WITH LTR MODIFIER", Modifier.gravity(ColumnAlign.Center))
        testRow_modifier()
        Text("RELATIVE TO SIBLINGS", Modifier.gravity(ColumnAlign.Center))
        testSiblings()
        Text("PLACE WITH AUTO RTL SUPPORT IN CUSTOM LAYOUT", Modifier.gravity(ColumnAlign.Center))
        CustomLayout(true)
        Text("PLACE WITHOUT RTL SUPPORT IN CUSTOM LAYOUT", Modifier.gravity(ColumnAlign.Center))
        CustomLayout(false)
        Text("WITH CONSTRAINTS", Modifier.gravity(ColumnAlign.Center))
        LayoutWithConstraints(Modifier.ltr, "LD: LTR modifier")
        LayoutWithConstraints(Modifier.rtl, "LD: RTL modifier")
        LayoutWithConstraints(text = "LD: locale")
    }
}

private val boxSize = Modifier.preferredSize(50.dp, 30.dp)
private val size = Modifier.preferredSize(10.dp, 10.dp)

@Composable
private fun testRow() {
    Row {
        Stack(boxSize.drawBackground(Color.Red)) {}
        Stack(boxSize.drawBackground(Color.Green)) {}
        Row {
            Stack(boxSize.drawBackground(Color.Magenta)) {}
            Stack(boxSize.drawBackground(Color.Yellow)) {}
            Stack(boxSize.drawBackground(Color.Cyan)) {}
        }
        Stack(boxSize.drawBackground(Color.Blue)) {}
    }
}

@Composable
private fun testRow_modifier() {
    Row {
        Stack(boxSize.drawBackground(Color.Red)) {}
        Stack(boxSize.drawBackground(Color.Green)) {}
        Row(Modifier.ltr) {
            Stack(boxSize.drawBackground(Color.Magenta)) {}
            Stack(boxSize.drawBackground(Color.Yellow)) {}
            Stack(boxSize.drawBackground(Color.Cyan)) {}
        }
        Stack(boxSize.drawBackground(Color.Blue)) {}
    }
}

@Composable
private fun testText() {
    Column {
        Row {
            Stack(size.drawBackground(Color.Red)) {}
            Stack(size.drawBackground(Color.Green)) {}
            Stack(size.drawBackground(Color.Blue)) {}
        }
        Text("Text.")
        Text("Width filled text.", Modifier.fillMaxWidth())
        Text("שלום!")
        Text("שלום!", Modifier.fillMaxWidth())
        Text("-->")
        Text("-->", Modifier.fillMaxWidth())
    }
}

@Composable
private fun testSiblings() {
    Column {
        Stack(boxSize.drawBackground(Color.Red).alignWithSiblings { p -> p.width }
        ) {}
        Stack(boxSize.drawBackground(Color.Green).alignWithSiblings { p -> p.width * 0.5 }
        ) {}
        Stack(boxSize.drawBackground(Color.Blue).alignWithSiblings { p -> p.width / 4 }
        ) {}
    }
}

@Composable
private fun CustomLayout(rtlSupport: Boolean) {
    Layout(children = @Composable {
        Stack(boxSize.drawBackground(Color.Red)) {}
        Stack(boxSize.drawBackground(Color.Green)) {}
        Stack(boxSize.drawBackground(Color.Blue)) {}
    }) { measurables, constraints, _ ->
        val p = measurables.map { e ->
            e.measure(constraints.copy(minWidth = 0.ipx, minHeight = 0.ipx))
        }
        val w = p.fold(0.ipx) { sum, e -> sum + e.width }
        val h = p.maxBy { it.height }!!.height
        layout(w, h) {
            var xPosition = 0.ipx
            for (child in p) {
                child.placeAbsolute(IntPxPosition(xPosition, 0.ipx))
                if (rtlSupport) {
                    child.place(IntPxPosition(xPosition, 0.ipx))
                } else {
                    child.placeAbsolute(IntPxPosition(xPosition, 0.ipx))
                }
                xPosition += child.width
            }
        }
    }
}

@Composable
private fun LayoutWithConstraints(modifier: Modifier = Modifier.None, text: String) {
    WithConstraints(modifier) { constraints, direction ->
        with(DensityAmbient.current) {
            val w = (constraints.maxWidth / 3).toDp()
            val h = (constraints.maxHeight / 2).toDp()
            val color = if (direction == LayoutDirection.Ltr) Color.Red else Color.Magenta
            Stack(Modifier.preferredSize(w, h).drawBackground(color)) {
                Text(text, Modifier.gravity(Alignment.Center))
            }
        }
    }
}
