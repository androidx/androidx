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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

val BugReproducers = Screen.Selection("Bug Reproducers",
    // https://github.com/JetBrains/compose-multiplatform/issues/3475
    Screen.Example("No Recomposition in Lazy Grid") { NoRecompositionInLazyGrid() },
    Screen.Example("RoundedCornerCrashOnJS") { RoundedCornerCrashOnJS() },
    Screen.Example("Keyboard animation repro") { KeyboardAnimationRepro() }
)

@Composable
fun NoRecompositionInLazyGrid() {
    var string by remember { mutableStateOf("1") }
    val value by derivedStateOf {
        maxOf(string.toIntOrNull() ?: 1, 1)
    }

    Column(Modifier.fillMaxSize()) {
        TextField(string, onValueChange = {
            string = it
        })

        Box {
            BoxWithConstraints {
                val gridItems = (0 until 20).toList()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(value),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(gridItems) { index, _->
                        Text("Item index: $index")
                    }
                }
            }
        }
    }
}