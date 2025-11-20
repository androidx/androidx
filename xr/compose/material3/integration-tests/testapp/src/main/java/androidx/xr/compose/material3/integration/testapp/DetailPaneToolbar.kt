/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.material3.integration.testapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DetailPaneToolbar() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Scaffold(
        content = { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                var fabEnabled by remember { mutableStateOf(false) }
                LazyColumn(
                    // Apply a floatingToolbarVerticalNestedScroll Modifier toggle the expanded
                    // state of the HorizontalFloatingToolbar.
                    modifier =
                        Modifier.floatingToolbarVerticalNestedScroll(
                            expanded = expanded,
                            onExpand = { expanded = true },
                            onCollapse = { expanded = false },
                        ),
                    state = rememberLazyListState(),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Button(onClick = { fabEnabled = !fabEnabled }) {
                            Text("Toggle FAB in Toolbar")
                        }
                    }

                    val list = (0..75).map { it.toString() }
                    items(count = list.size) {
                        Text(
                            text = list[it],
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                    }
                }
                if (fabEnabled) {
                    VerticalFloatingToolbar(
                        modifier = Modifier.align(Alignment.CenterEnd).offset(x = -ScreenOffset),
                        floatingActionButton = {
                            FloatingToolbarDefaults.StandardFloatingActionButton(
                                onClick = { /* doSomething() */ }
                            ) {
                                PrimaryContent()
                            }
                        },
                        expanded = expanded,
                        content = {
                            LeadingContent()
                            TrailingContent()
                        },
                    )
                    HorizontalFloatingToolbar(
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
                        floatingActionButton = {
                            FloatingToolbarDefaults.StandardFloatingActionButton(
                                onClick = { /* doSomething() */ }
                            ) {
                                PrimaryContent()
                            }
                        },
                        expanded = expanded,
                        content = {
                            LeadingContent()
                            TrailingContent()
                        },
                    )
                } else {
                    VerticalFloatingToolbar(
                        modifier = Modifier.align(Alignment.CenterEnd).offset(x = -ScreenOffset),
                        expanded = expanded,
                        leadingContent = { LeadingContent() },
                        trailingContent = { TrailingContent() },
                        content = {
                            FilledIconButton(onClick = { /* doSomething() */ }) { PrimaryContent() }
                        },
                    )
                    HorizontalFloatingToolbar(
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
                        expanded = expanded,
                        leadingContent = { LeadingContent() },
                        trailingContent = { TrailingContent() },
                        content = {
                            FilledIconButton(onClick = { /* doSomething() */ }) { PrimaryContent() }
                        },
                    )
                }
            }
        }
    )
}

@Composable
private fun LeadingContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Check, contentDescription = "Localized description")
    }
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
    }
}

@Composable
private fun TrailingContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
    }
}

@Composable
private fun PrimaryContent() {
    Icon(Icons.Filled.Add, contentDescription = "Localized description")
}
