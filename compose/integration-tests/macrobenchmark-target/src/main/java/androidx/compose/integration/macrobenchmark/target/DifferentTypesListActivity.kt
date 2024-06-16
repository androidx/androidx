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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import android.view.Choreographer
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DifferentTypesListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemCount = intent.getIntExtra(EXTRA_ITEM_COUNT, 3000)

        setContent {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "IamLazy" }
            ) {
                items(count = itemCount, key = { it }, contentType = { it % 2 }) {
                    if (it % 2 == 0) {
                        EvenItem(it)
                    } else {
                        OddItem(it)
                    }
                }
            }
        }

        launchIdlenessTracking()
    }

    internal fun ComponentActivity.launchIdlenessTracking() {
        val contentView: View = findViewById(android.R.id.content)
        val callback: Choreographer.FrameCallback =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (Recomposer.runningRecomposers.value.any { it.hasPendingWork }) {
                        contentView.contentDescription = "COMPOSE-BUSY"
                    } else {
                        contentView.contentDescription = "COMPOSE-IDLE"
                    }
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
        Choreographer.getInstance().postFrameCallback(callback)
    }

    companion object {
        const val EXTRA_ITEM_COUNT = "ITEM_COUNT"
    }
}

@Composable
private fun OddItem(index: Int) {
    Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Odd item $index",
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(16.dp)
                )
                Spacer(modifier = Modifier.weight(1f, fill = true))
                Checkbox(checked = false, onCheckedChange = {}, modifier = Modifier.padding(16.dp))
            }
            Row {
                LazyRow {
                    items(10) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = "ic launcher",
                            modifier =
                                Modifier.padding(8.dp)
                                    .border(2.dp, Color.White)
                                    .padding(8.dp)
                                    .border(2.dp, Color.Green)
                                    .padding(8.dp)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Load") }, onClick = {})
                        DropdownMenuItem(text = { Text("Save") }, onClick = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun EvenItem(index: Int) {
    Column(modifier = Modifier.padding(16.dp)) { Text(text = "Even item $index", fontSize = 17.sp) }
}
