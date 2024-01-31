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

package androidx.lifecycle.compose.samples

import androidx.annotation.Sampled
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.compose.dropUnlessStarted

@Sampled
@Composable
fun DropUnlessStarted() {
    Button(
        onClick = dropUnlessStarted {
            // Run on clicks only when the lifecycle is at least STARTED.
            // Example: navController.navigate("next_screen")
        },
    ) {
        Text(text = "Navigate to next screen")
    }
}

@Sampled
@Composable
fun DropUnlessResumed() {
    Button(
        onClick = dropUnlessResumed {
            // Run on clicks only when the lifecycle is at least RESUMED.
            // Example: navController.navigate("next_screen")
        },
    ) {
        Text(text = "Navigate to next screen")
    }
}
