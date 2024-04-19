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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay

val AccessibilityLiveRegionExample = Screen.Example("Accessibility LiveRegion example") {
    var number by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Polite:")
        Text("Number $number", modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        })

        Text("Assertive:")
        Text("Number $number", modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Assertive
        })
    }

    // Update the number every second
    LaunchedEffect(Unit) {
        while (true) {
            number++
            delay(1000)
        }
    }
}