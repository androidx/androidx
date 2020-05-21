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

package androidx.ui.foundation.demos

import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.demos.common.ComposableDemo
import androidx.ui.foundation.AdapterList
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentColorAmbient
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.foundation.currentTextStyle
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.padding
import androidx.ui.unit.dp
import androidx.ui.unit.sp

val ListDemos = listOf(
    ComposableDemo("Simple list") { ListDemo() },
    ComposableDemo("Add/remove items") { ListAddRemoveItemsDemo() }
)

@Composable
private fun ListDemo() {
    AdapterList(
        data = listOf(
            "Hello,", "World:", "It works!", "",
            "this one is really long and spans a few lines for scrolling purposes",
            "these", "are", "offscreen"
        ) + (1..100).map { "$it" }
    ) {
        Text(text = it, fontSize = 80.sp)

        if (it.contains("works")) {
            Text("You can even emit multiple components per item.")
        }
    }
}

@Composable
private fun ListAddRemoveItemsDemo() {
    var numItems by state { 0 }
    var offset by state { 0 }
    Column {
        Row {
            val buttonModifier = Modifier.padding(8.dp)
            Button(modifier = buttonModifier, onClick = { numItems++ }) { Text("Add") }
            Button(modifier = buttonModifier, onClick = { numItems-- }) { Text("Remove") }
            Button(modifier = buttonModifier, onClick = { offset++ }) { Text("Offset") }
        }
        Column {
            AdapterList((1..numItems).map { it + offset }.toList()) {
                Text("$it", style = currentTextStyle().copy(fontSize = 20.sp))
            }
        }
    }
}

@Composable
fun Button(modifier: Modifier, onClick: () -> Unit, children: @Composable () -> Unit) {
    Box(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        backgroundColor = Color(0xFF6200EE),
        paddingStart = 16.dp,
        paddingEnd = 16.dp,
        paddingTop = 8.dp,
        paddingBottom = 8.dp
    ) {
        Providers(ContentColorAmbient provides Color.White) {
            children()
        }
    }
}