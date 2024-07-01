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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun ScaffoldSample() {
    // Declare just one [AppScaffold] per app such as in the activity.
    // [AppScaffold] allows static screen elements (i.e. [TimeText]) to remain visible
    // during in-app transitions such as swipe-to-dismiss.
    AppScaffold {
        // Define the navigation hierarchy within the AppScaffold,
        // such as using SwipeDismissableNavHost.
        // For this sample, we will define a single screen inline.
        val listState = rememberScalingLazyListState()

        // By default, ScreenScaffold will handle transitions showing/hiding ScrollIndicator
        // and showing/hiding/scrolling away TimeText.
        ScreenScaffold(scrollState = listState) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(10) {
                    Button(
                        onClick = {},
                        label = { Text("Item ${it + 1}") },
                    )
                }
            }
        }
    }
}
