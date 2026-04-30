/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.inspection.testdata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

class SharedTransitionLayoutTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SharedTransitionExample() }
    }
}

@Composable
fun SharedTransitionExample() {
    // 1. SharedTransitionLayout acts as the container/coordinator for shared elements
    SharedTransitionLayout {
        var isDetails by remember { mutableStateOf(false) }

        // AnimatedContent acts as the container that swaps between the two views
        AnimatedContent(targetState = isDetails, label = "shared_transition") { targetState ->
            if (!targetState) {
                // LIST VIEW
                this@SharedTransitionLayout.ListScreen(
                    onItemClick = { isDetails = true },
                    animatedVisibilityScope = this,
                )
            } else {
                // DETAILS VIEW
                this@SharedTransitionLayout.DetailScreen(
                    onBackClick = { isDetails = false },
                    animatedVisibilityScope = this,
                )
            }
        }
    }
}

@Composable
fun SharedTransitionScope.ListScreen(
    onItemClick: () -> Unit,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("List Screen", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Shared Element: Box
        MyBox(
            modifier =
                Modifier.size(100.dp)
                    .sharedElement(
                        // Key must match between screens
                        sharedContentState = rememberSharedContentState(key = "shared_box"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .background(Color.Blue, RoundedCornerShape(16.dp))
                    .clickable { onItemClick() }
        )
    }
}

@Composable
fun SharedTransitionScope.DetailScreen(
    onBackClick: () -> Unit,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
) {
    Column(
        modifier = Modifier.fillMaxSize().clickable { onBackClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 2. Shared Element: Box (Matches the key "shared_box")
        MyBox(
            modifier =
                Modifier.fillMaxWidth()
                    .height(300.dp)
                    .padding(32.dp)
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "shared_box"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .background(Color.Blue, RoundedCornerShape(32.dp))
        )
        Text("Detail Screen (Click to Back)", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun MyBox(modifier: Modifier) {
    Box(modifier.testTag("MyBox"))
}
