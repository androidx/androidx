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

package androidx.xr.compose.integration.layout.spatialcomposeapp.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.SpatialPopup

/**
 * Renders a button that opens a dialog containing the provided content and a button to close the
 * dialog.
 *
 * @param modifier a modifier to be applied to the content of the dialog.
 * @param content the content of the dialog.
 */
@Composable
fun TestPopup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Button(onClick = { showDialog = true }) { Text("Show Popup") }
    if (showDialog) {
        SpatialPopup(onDismissRequest = { showDialog = false }) {
            Surface(modifier = modifier.clip(RoundedCornerShape(5.dp))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    content()
                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = { showDialog = false },
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
