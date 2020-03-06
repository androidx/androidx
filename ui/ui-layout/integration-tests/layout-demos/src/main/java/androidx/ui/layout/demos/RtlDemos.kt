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

import android.app.Activity
import android.os.Bundle
import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.core.setContent
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Row
import androidx.ui.layout.Arrangement
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Stack
import androidx.ui.foundation.DrawBackground
import androidx.ui.layout.LayoutDirectionModifier
import androidx.ui.unit.dp

class RtlDemosActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(arrangement = Arrangement.SpaceEvenly) {
                Text("TEXT", LayoutGravity.Center)
                testText()
                Text("ROW", LayoutGravity.Center)
                testRow()
                Text("ROW WITH MODIFIER", LayoutGravity.Center)
                testRow_modifier()
                Text("RELATIVE TO SIBLINGS", LayoutGravity.Center)
                testSiblings()
            }
        }
    }
}

val boxSize = LayoutSize(50.dp, 50.dp)
val size = LayoutSize(10.dp, 10.dp)

@Composable
fun testRow() {
    Row {
        Stack(boxSize + DrawBackground(Color.Red)) {}
        Stack(boxSize + DrawBackground(Color.Green)) {}
        Row {
            Stack(boxSize + DrawBackground(Color.Magenta)) {}
            Stack(boxSize + DrawBackground(Color.Yellow)) {}
            Stack(boxSize + DrawBackground(Color.Cyan)) {}
        }
        Stack(boxSize + DrawBackground(Color.Blue)) {}
    }
}

@Composable
fun testRow_modifier() {
    Row {
        Stack(boxSize + DrawBackground(Color.Red)) {}
        Stack(boxSize + DrawBackground(Color.Green)) {}
        Row(LayoutDirectionModifier.Ltr) {
            Stack(boxSize + DrawBackground(Color.Magenta)) {}
            Stack(boxSize + DrawBackground(Color.Yellow)) {}
            Stack(boxSize + DrawBackground(Color.Cyan)) {}
        }
        Stack(boxSize + DrawBackground(Color.Blue)) {}
    }
}

@Composable
fun testText() {
    Column {
        Row {
            Stack(boxSize + DrawBackground(Color.Red)) {}
            Stack(boxSize + DrawBackground(Color.Green)) {}
            Stack(boxSize + DrawBackground(Color.Blue)) {}
        }
        Text("Text.")
        Text("Width filled text.", LayoutWidth.Fill)
        Text("שלום!")
        Text("שלום!", LayoutWidth.Fill)
        Text("-->")
        Text("-->", LayoutWidth.Fill)
    }
}

@Composable
fun testSiblings() {
    Column {
        Stack(boxSize +
                DrawBackground(Color.Red) +
                LayoutGravity.RelativeToSiblings { p -> p.width }
        ) {}
        Stack(boxSize +
                DrawBackground(Color.Green) +
                LayoutGravity.RelativeToSiblings { p -> p.width * 0.5 }
        ) {}
        Stack(boxSize +
                DrawBackground(Color.Blue) +
                LayoutGravity.RelativeToSiblings { p -> p.width / 4 }
        ) {}
    }
}