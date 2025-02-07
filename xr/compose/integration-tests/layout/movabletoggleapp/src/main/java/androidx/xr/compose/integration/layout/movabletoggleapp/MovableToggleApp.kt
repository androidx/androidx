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
package androidx.xr.compose.integration.layout.movabletoggleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.width

class MovableToggleApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Subspace { SpatialContent() } }
    }

    @Composable
    @SubspaceComposable
    private fun SpatialContent() {
        var enabled by remember { mutableStateOf(true) }

        SpatialPanel(SubspaceModifier.height(200.dp).width(200.dp).movable(enabled = enabled)) {
            Box(
                modifier = Modifier.background(Color.LightGray).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column {
                    Button(onClick = { enabled = !enabled }) {
                        Column {
                            Text(text = "Touch to enable and disable movable modifier")
                            Text(text = "Enabled: $enabled")
                        }
                    }
                }
            }
        }
    }
}
