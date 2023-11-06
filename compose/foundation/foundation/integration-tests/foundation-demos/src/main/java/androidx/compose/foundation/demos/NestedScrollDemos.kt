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

package androidx.compose.foundation.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun NestedScrollDemo() {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Red)
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(30.dp))
    ) {
        repeat(6) { outerOuterIndex ->
            OuterLvl1(outerOuterIndex)
        }
    }
}

@Composable
private fun OuterLvl1(outerOuterIndex: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .border(3.dp, Color.Black)
            .height(350.dp)
            .background(Color.Yellow),
        reverseLayout = true,
        contentPadding = PaddingValues(60.dp)
    ) {
        repeat(3) { outerIndex ->
            item {
                InnerColumn(outerOuterIndex, outerIndex)
            }

            item {
                Spacer(Modifier.height(5.dp))
            }
        }
    }
    Spacer(Modifier.height(5.dp))
}

@Composable
private fun InnerColumn(outerOuterIndex: Int, outerIndex: Int) {
    Column(
        Modifier
            .fillMaxSize()
            .border(3.dp, Color.Blue)
            .height(150.dp)
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(30.dp))
    ) {
        repeat(10) { innerIndex ->
            Box(
                Modifier
                    .height(38.dp)
                    .fillMaxWidth()
                    .background(Color.Magenta)
                    .border(2.dp, Color.Yellow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.focusable(),
                    text = "$outerOuterIndex : $outerIndex : $innerIndex",
                    fontSize = 24.sp
                )
            }
        }
    }
}

@Composable
fun NestedScrollConnectionSample() {
    var availableOffset by remember { mutableStateOf(Offset.Zero) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                availableOffset = available

                // In this case we aren't consuming any of the offset, just showing what is
                // available.
                return Offset.Zero
            }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            // attach as a parent to the nested scroll system
            .nestedScroll(nestedScrollConnection)
    ) {
        Column {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Scroll Connection PreScroll available: $availableOffset")

            // our list with build in nested scroll support that will notify us about its scroll
            LazyColumn {
                items(100) { index ->
                    Text("I'm item $index", modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                        .padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun SimpleColumnNestedScrollSample() {
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.Red)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Outer Scrollable Column"
        )

        for (i in 0 until 4) {
            SimpleColumn("Inner Scrollable Column: $i")
        }
    }
}

@Composable
fun SimpleColumn(label: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Green)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "$label INNER, scrollable only"
        )

        for (i in 0 until 20) {
            Text(
                modifier = Modifier.fillMaxWidth().focusable(),
                text = "Text $i",
            )
        }
    }
}
