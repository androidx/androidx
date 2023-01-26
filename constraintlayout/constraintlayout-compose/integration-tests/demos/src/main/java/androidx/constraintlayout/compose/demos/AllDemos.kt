/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ComposeDemo(val title: String, val content: @Composable () -> Unit)

val AllComposeConstraintLayoutDemos: List<ComposeDemo> =
    listOf(
        ComposeDemo("CustomColorInKeyAttributes") { CustomColorInKeyAttributesDemo() },
        ComposeDemo("SimpleOnSwipe") { SimpleOnSwipe() },
        ComposeDemo("AnimatedChainOrientation") { ChainsAnimatedOrientationDemo() }
    )

/**
 * Main screen to explore and interact with all demos from [AllComposeConstraintLayoutDemos].
 */
@Preview
@Composable
fun ComposeConstraintLayoutDemos() {
    var displayedDemo by remember { mutableStateOf<ComposeDemo?>(null) }
    Column {
        Column {
            displayedDemo?.let {
                // Header with back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color.White)
                        .graphicsLayer(shadowElevation = 2f)
                        .clickable { displayedDemo = null }, // Return to list of demos
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    Text(text = it.title)
                }
            } ?: kotlin.run {
                // Main Title
                Text(text = "ComposeConstraintLayoutDemos", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            displayedDemo?.let { demo ->
                // Display selected demo
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1.0f, true)
                ) {
                    demo.content()
                }
            } ?: kotlin.run {
                // Display list of demos
                AllComposeConstraintLayoutDemos.forEach {
                    ComposeDemoItem(it.title) { displayedDemo = it }
                }
            }
        }
    }
}

@Composable
private fun ComposeDemoItem(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(start = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            modifier = Modifier,
            fontSize = 16.sp
        )
    }
}