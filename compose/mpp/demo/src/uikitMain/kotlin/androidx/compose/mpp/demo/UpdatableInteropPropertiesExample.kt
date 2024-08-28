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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.MapKit.MKMapView

fun Int.interactionModeToString(): String = when (this) {
    1 -> "Cooperative"
    2 -> "Non-cooperative"
    else -> "Non-interactive"
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConfigutableMap(index: Int) {
    var interactionMode by remember { mutableStateOf(0) }

    UIKitView(
        factory = { MKMapView().also { it.tag = index.toLong() } },
        modifier = Modifier.fillMaxWidth().height(200.dp),
        properties = UIKitInteropProperties(
            interactionMode = when (interactionMode) {
                1 -> UIKitInteropInteractionMode.Cooperative()
                2 -> UIKitInteropInteractionMode.NonCooperative
                else -> null
            }
        ),
        update = {
            println("Updated $index")
        },
        onReset = {
            it.tag = index.toLong()
            println("Reset $index")
        }
    )

    var isDropdownExpanded by remember { mutableStateOf(false) }

    Button(
        onClick = { isDropdownExpanded = true }
    ) {
        Text("Interaction mode: ${interactionMode.interactionModeToString()}")
    }

    DropdownMenu(
        expanded = isDropdownExpanded,
        onDismissRequest = { isDropdownExpanded = false }
    ) {
        for (i in 0 until 3) {
            DropdownMenuItem(onClick = { interactionMode = i }) {
                Text(i.interactionModeToString())
            }
        }
    }
}

@Composable
fun ConfigurableMap(nestInsideBox: Boolean, index: Int) {
    if (nestInsideBox) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            ConfigutableMap(index)
        }
    } else {
        ConfigutableMap(index)
    }

}

val UpdatableInteropPropertiesExample = Screen.Example("Updatable interop properties") {
    var nestInsideBox by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Button(
                onClick = { nestInsideBox = !nestInsideBox }
            ) {
                Text("Toggle nesting, current: $nestInsideBox")
            }
        }
        items(100) { index ->
            ConfigurableMap(nestInsideBox, index)
        }
    }
}