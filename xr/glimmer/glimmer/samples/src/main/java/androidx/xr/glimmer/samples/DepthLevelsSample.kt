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

package androidx.xr.glimmer.samples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.glimmer.Depth
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.depth
import androidx.xr.glimmer.list.VerticalList

@Composable
fun DepthLevelsSample() {
    val depthLevels = GlimmerTheme.depthLevels
    VerticalList(
        modifier = Modifier.fillMaxWidth().wrapContentSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { DepthItem("level 1", depth = depthLevels.level1) }
        item { DepthItem("level 2", depth = depthLevels.level2) }
        item { DepthItem("level 3", depth = depthLevels.level3) }
        item { DepthItem("level 4", depth = depthLevels.level4) }
        item { DepthItem("level 5", depth = depthLevels.level5) }
    }
}

@Preview
@Composable
private fun DepthPreview() {
    GlimmerTheme { DepthLevelsSample() }
}

@Composable
private fun DepthItem(name: String, depth: Depth, modifier: Modifier = Modifier) {
    val shape = GlimmerTheme.shapes.medium
    Box(
        modifier
            .background(color = Color.White, shape)
            .padding(horizontal = 40.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.depth(depth, shape)
                .border(1.dp, Color.White, shape)
                .background(Color.Black, shape)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(name, color = Color.White, fontSize = 14.sp)
        }
    }
}
