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

package androidx.ui.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.HorizontalScroller
import androidx.ui.foundation.ScrollerPosition
import androidx.ui.foundation.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.foundation.clickable
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp

private val colors = listOf(
    Color(0xFFffd7d7.toInt()),
    Color(0xFFffe9d6.toInt()),
    Color(0xFFfffbd0.toInt()),
    Color(0xFFe3ffd9.toInt()),
    Color(0xFFd0fff8.toInt())
)

private val phrases = listOf(
    "Easy As Pie",
    "Wouldn't Harm a Fly",
    "No-Brainer",
    "Keep On Truckin'",
    "An Arm and a Leg",
    "Down To Earth",
    "Under the Weather",
    "Up In Arms",
    "Cup Of Joe",
    "Not the Sharpest Tool in the Shed",
    "Ring Any Bells?",
    "Son of a Gun",
    "Hard Pill to Swallow",
    "Close But No Cigar",
    "Beating a Dead Horse",
    "If You Can't Stand the Heat, Get Out of the Kitchen",
    "Cut To The Chase",
    "Heads Up",
    "Goody Two-Shoes",
    "Fish Out Of Water",
    "Cry Over Spilt Milk",
    "Elephant in the Room",
    "There's No I in Team",
    "Poke Fun At",
    "Talk the Talk",
    "Know the Ropes",
    "Fool's Gold",
    "It's Not Brain Surgery",
    "Fight Fire With Fire",
    "Go For Broke"
)

@Sampled
@Composable
fun VerticalScrollerSample() {
    val style = TextStyle(fontSize = 30.sp)
    // Scroller will be clipped to this padding
    VerticalScroller {
        phrases.forEach { phrase ->
            Text(phrase, style)
        }
    }
}

@Sampled
@Composable
fun SimpleHorizontalScrollerSample() {
    HorizontalScroller {
        Row {
            repeat(100) { index ->
                Square(index)
            }
        }
    }
}

@Sampled
@Composable
fun ControlledHorizontalScrollerSample() {
    // Create and own ScrollerPosition to call `smoothScrollTo` later
    val position = ScrollerPosition()
    val scrollable = state { true }
    Column {
        HorizontalScroller(
            scrollerPosition = position,
            isScrollable = scrollable.value
        ) {
            repeat(1000) { index ->
                Square(index)
            }
        }
        // Controls that will call `smoothScrollTo`, `scrollTo` or toggle `scrollable` state
        ScrollControl(position, scrollable)
    }
}

@Composable
private fun Square(index: Int) {
    Box(
        Modifier.preferredSize(75.dp, 200.dp),
        backgroundColor = colors[index % colors.size],
        gravity = ContentGravity.Center
    ) {
        Text(index.toString())
    }
}

@Composable
private fun ScrollControl(position: ScrollerPosition, scrollable: MutableState<Boolean>) {
    Column {
        Row {
            Text("Scroll")
            SquareButton("< -", Color.Red) {
                position.scrollTo(position.value - 1000)
            }
            SquareButton("--- >", Color.Green) {
                position.scrollBy(10000f)
            }
        }
        Row {
            Text("Smooth Scroll")
            SquareButton("< -", Color.Red) {
                position.smoothScrollTo(position.value - 1000)
            }
            SquareButton("--- >", Color.Green) {
                position.smoothScrollBy(10000f)
            }
        }
        Row {
            SquareButton("Scroll: ${scrollable.value}") {
                scrollable.value = !scrollable.value
            }
        }
    }
}

@Composable
private fun SquareButton(text: String, color: Color = Color.LightGray, onClick: () -> Unit) {
    Box(
        Modifier.padding(5.dp).preferredSize(120.dp, 60.dp).clickable(onClick = onClick),
        backgroundColor = color,
        gravity = ContentGravity.Center
    ) {
        Text(text, fontSize = 20.sp)
    }
}

@Composable
private fun Text(text: String, style: TextStyle) {
    Text(text, style = style, modifier = Modifier.clickable { })
}