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
package androidx.ui.layout.demos

import android.app.Activity
import android.os.Bundle
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Density
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.layout.Column
import androidx.ui.layout.Padding
import androidx.ui.layout.VerticalScroller
import androidx.ui.painting.TextStyle
import androidx.compose.composer
import androidx.compose.setContent
import androidx.ui.core.sp

class ScrollerActivity : Activity() {
    val phrases = listOf(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = Density(this)
        withDensity(density) {
            val style = TextStyle(fontSize = 30.sp)
            setContent {
                CraneWrapper {
                    Padding(padding = 10.dp) {
                        VerticalScroller {
                            Column {
                                phrases.forEach { phrase ->
                                    Text(text = phrase, style = style)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
