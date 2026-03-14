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
import androidx.compose.foundation.layout.Box
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
import androidx.xr.glimmer.DepthEffect
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.depthEffect
import androidx.xr.glimmer.list.VerticalList

@Composable
fun DepthEffectLevelsSample() {
    val depthEffectLevels = GlimmerTheme.depthEffectLevels
    VerticalList(
        modifier = Modifier.fillMaxWidth().wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { DepthEffectItem("level 1", depthEffect = depthEffectLevels.level1) }
        item { DepthEffectItem("level 2", depthEffect = depthEffectLevels.level2) }
        item { DepthEffectItem("level 3", depthEffect = depthEffectLevels.level3) }
        item { DepthEffectItem("level 4", depthEffect = depthEffectLevels.level4) }
        item { DepthEffectItem("level 5", depthEffect = depthEffectLevels.level5) }
    }
}

@Preview
@Composable
private fun DepthEffectPreview() {
    GlimmerTheme { DepthEffectLevelsSample() }
}

@Composable
private fun DepthEffectItem(name: String, depthEffect: DepthEffect, modifier: Modifier = Modifier) {
    val shape = GlimmerTheme.shapes.medium
    Box(
        modifier
            .background(color = Color.White, shape)
            .padding(horizontal = 40.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.depthEffect(depthEffect, shape)
                .border(1.dp, Color.White, shape)
                .background(Color.Black, shape)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(name, color = Color.White, fontSize = 14.sp)
        }
    }
}
