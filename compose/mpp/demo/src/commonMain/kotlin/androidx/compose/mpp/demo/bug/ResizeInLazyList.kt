/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.mpp.demo.bug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@OptIn(ExperimentalMaterialApi::class)
internal val ResizeInLazyList = Screen.Example("Resize In LazyList") {
    val names = remember {
        listOf(
            "Fiona Apple",
            "Charlie Brown",
            "George Clooney",
            "Penelope Cruz",
            "Laura Dern",
            "Nancy Drew",
            "Rachel Green",
            "Yara Greyjoy",
            "Kevin Hart",
            "Samuel Jackson",
            "Alice Johnson",
            "Michael Jordan",
            "Ian McKellen",
            "Hannah Montana",
            "Edward Norton",
            "Diana Prince",
            "Julia Roberts",
            "Bob Smith",
            "Taylor Swift",
            "Quentin Tarantino",
            "Uma Thurman",
            "Vincent Vega",
            "Walter White",
            "Oscar Wilde",
            "Xavier Woods",
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(names) { index, name ->
                println("$name placed as view")

                val backgroundColor = remember(name) {
                    Color(
                        red = Random.nextFloat(),
                        green = Random.nextFloat(),
                        blue = Random.nextFloat(),
                        alpha = 1f,
                    )
                }
                val textColor = remember(backgroundColor) {
                    if (backgroundColor.luminance() < 0.5f) Color.White else Color.Black
                }

                Text(
                    text = "${index + 1}. $name",
                    color = textColor,
                    modifier = Modifier
                        .background(backgroundColor)
                        .padding(16.dp)
                        .width(300.dp)

                )
            }

        }
    }

}
