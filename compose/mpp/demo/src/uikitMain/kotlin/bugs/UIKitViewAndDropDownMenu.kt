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

package bugs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.MapKit.MKMapView

val UIKitViewAndDropDownMenu = Screen.Example("UIKitViewAndDropDownMenu") {
    // Issue: https://github.com/JetBrains/compose-multiplatform/issues/3490
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            UIKitView(modifier = Modifier.fillMaxSize(), factory = { MKMapView() })
            ButtonAndDropDownMenu("Menu not over UIKitView")
        }
        Divider()
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ButtonAndDropDownMenu("Menu not over UIKitView")
        }
    }
}

@Composable
private fun ButtonAndDropDownMenu(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Row {
        Button(onClick = { expanded = true }) {
            Text(text)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            content = {
                DropdownMenuItem(onClick = { expanded = false }) {
                    Text("Item 1")
                }
                DropdownMenuItem(onClick = { expanded = false }) {
                    Text("Item 2")
                }
            }
        )
    }
}
