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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

class CrossfadeActivity : ComponentActivity() {

    @Suppress("UnusedCrossfadeTargetStateParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column {
                var toggled by remember { mutableStateOf(false) }
                var targetState by remember { mutableStateOf(false) }
                key(toggled) {
                    Crossfade(
                        modifier = Modifier.size(150.dp),
                        label = "Crossfade",
                        targetState = targetState
                    ) {}
                }
                Button(
                    modifier = Modifier.semantics { contentDescription = "toggle-crossfade" },
                    onClick = { toggled = !toggled }
                ) {
                    Text(toggled.toString())
                }
                Button(
                    modifier = Modifier.semantics { contentDescription = "toggle-target" },
                    onClick = { targetState = !targetState }
                ) {
                    Text(targetState.toString())
                }
            }
            launchIdlenessTracking()
        }
    }
}
