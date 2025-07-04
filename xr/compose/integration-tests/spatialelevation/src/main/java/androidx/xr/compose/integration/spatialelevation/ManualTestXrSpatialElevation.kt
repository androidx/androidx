/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.integration.spatialelevation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.LocalSpatialConfiguration
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.compose.spatial.SpatialDialog
import androidx.xr.compose.spatial.SpatialElevation
import androidx.xr.compose.spatial.SpatialElevationLevel
import androidx.xr.compose.spatial.SpatialPopup
import androidx.xr.scenecore.scene
import kotlinx.coroutines.launch

class ManualTestXrSpatialElevationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(android.R.style.Theme_Translucent_NoTitleBar)
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
private fun App() {
    var showDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var shouldExpand by remember { mutableStateOf(false) }
    var contentText by remember { mutableStateOf("Opening additional context.") }
    var showPopup by remember { mutableStateOf(false) }

    Orbiter(position = ContentEdge.Start, offset = 8.dp, offsetType = OrbiterOffsetType.Overlap) {
        NavigationRail(
            modifier =
                Modifier.width(80.dp).height(IntrinsicSize.Min).clip(RoundedCornerShape(20.dp))
        ) {
            NavigationRailItem(
                selected = false,
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(
                            (listState.firstVisibleItemIndex -
                                    listState.layoutInfo.visibleItemsInfo.size)
                                .coerceAtLeast(0)
                        )
                    }
                },
                icon = { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Up") },
                label = { Text("Up") },
            )
            NavigationRailItem(
                selected = false,
                onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                icon = { Icon(Icons.Rounded.Menu, contentDescription = "Menu") },
                label = { Text("Top") },
            )
            NavigationRailItem(
                selected = false,
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(
                            listState.firstVisibleItemIndex +
                                listState.layoutInfo.visibleItemsInfo.size
                        )
                    }
                },
                icon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Down") },
                label = { Text("Down") },
            )
        }
    }
    Orbiter(position = ContentEdge.End, offset = 72.dp, offsetType = OrbiterOffsetType.OuterEdge) {
        Row(
            modifier = Modifier.animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            if (shouldExpand) {
                Card(Modifier.widthIn(max = 320.dp)) {
                    Text(
                        text = contentText,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.size(16.dp))
            }
            IconMenuOrnament(
                onClick = {
                    shouldExpand = true
                    contentText = "Opening additional context for $it."
                }
            )
        }
    }
    Scaffold(
        floatingActionButton = {
            SpatialElevation(SpatialElevationLevel.Level2) {
                Row {
                    Column { Button(onClick = { showPopup = true }) { Text("Show Popup Test") } }
                    Column {
                        Button(onClick = { showDialog = true }) { Text("Show Dialog Popup Test") }
                    }
                }
            }
        },
        topBar = { Surface { Text(text = "CL Number: N/A", modifier = Modifier.padding(10.dp)) } },
        bottomBar = {
            if (LocalSpatialConfiguration.current.hasXrSpatialFeature) {
                val session =
                    checkNotNull(LocalSession.current) {
                        "LocalSession.current was null. Session must be available."
                    }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                        Button(onClick = { session.scene.requestHomeSpaceMode() }) {
                            Text("Enter Home Space Mode")
                        }
                    } else {
                        Button(onClick = { session.scene.requestFullSpaceMode() }) {
                            Text("Enter Full Space Mode")
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding), state = listState) {
            items(30) { RowItem { EditItem(it, modifier = Modifier.padding(20.dp)) } }
        }
    }

    if (shouldExpand) {
        DismissBox(
            onDismiss = {
                shouldExpand = false
                showDialog = false
            }
        )
    }

    // TODO(b/344599930): Issue with rendering rounded corners of elevated panel.
    if (showPopup) {
        SpatialPopup(alignment = Alignment.Center, onDismissRequest = { showPopup = false }) {
            Box(Modifier.size(150.dp, 50.dp).background(Color.White, RoundedCornerShape(16.dp))) {
                Text("This is a popup: click anywhere to exit", modifier = Modifier.padding(10.dp))
            }
        }
    }

    if (showDialog) {
        SpatialDialog(onDismissRequest = { showDialog = false }) {
            Surface(color = Color.White, modifier = Modifier.clip(RoundedCornerShape(5.dp))) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("This is a popup dialog", modifier = Modifier.padding(10.dp))
                    Button(onClick = { showDialog = false }) { Text("Dismiss") }
                }
            }
        }
    }
}

@Composable
private fun RowItem(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = Color.Gray,
        modifier = modifier.padding(20.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        content = content,
    )
}

@Composable
private fun DismissBox(onDismiss: () -> Unit) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .alpha(0.2f)
                .background(Color.Gray)
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
                .semantics(mergeDescendants = true) {
                    onClick {
                        onDismiss()
                        true
                    }
                }
    ) {}
}

@Composable
fun IconMenuOrnament(modifier: Modifier = Modifier, onClick: (text: String) -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        modifier =
            modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            IconButton(onClick = { onClick("Home") }) {
                Icon(Icons.Filled.Home, contentDescription = "Home")
            }
            IconButton(onClick = { onClick("Search") }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            IconButton(onClick = { onClick("Shopping Cart") }) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = "ShoppingCart")
            }
        }
    }
}

@Composable
private fun EditItem(index: Int, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf("Test item $index") }
    var showEditPopup by remember { mutableStateOf(false) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (showEditPopup) {
            SpatialElevation(SpatialElevationLevel.Level4) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        TextField(value = text, onValueChange = { text = it })
                        Spacer(modifier = Modifier.size(10.dp))
                        Button(onClick = { showEditPopup = false }, modifier = Modifier) {
                            Text("Done")
                        }
                    }
                }
            }
        } else {
            Text(text = text, fontSize = MaterialTheme.typography.headlineLarge.fontSize)
            IconButton(onClick = { showEditPopup = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}
