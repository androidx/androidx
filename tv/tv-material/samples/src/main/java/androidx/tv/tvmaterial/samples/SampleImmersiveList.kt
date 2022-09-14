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

package androidx.tv.tvmaterial.samples

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.material.ExperimentalTvMaterialApi
import androidx.tv.material.immersivelist.ImmersiveList

@OptIn(ExperimentalTvMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun SampleImmersiveList() {
    ImmersiveList(
        modifier = Modifier
            .height(130.dp)
            .fillMaxWidth()
            .border(1.dp, Color.Black),
        background = { index, _ ->
            AnimatedContent(targetState = index) { SampleBackground(it) } },
    ) {
        TvLazyRow {
            items(immersiveClusterMediaItems.size) {
                SampleCard(Modifier.focusableItem(it), (it + 1).toString())
            }
        }
    }
}

@Composable
fun SampleBackground(idx: Int) {
    val item = immersiveClusterMediaItems[idx]

    Box(
        Modifier
            .background(item.backgroundColor)
            .fillMaxWidth()
            .height(90.dp)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SampleCard(modifier: Modifier, cardText: String) {
    var cardScale by remember { mutableStateOf(0.5f) }
    val borderGlowColorTransition = rememberInfiniteTransition()
    var initialValue by remember { mutableStateOf(Color.Transparent) }
    val glowingColor by borderGlowColorTransition.animateColor(
        initialValue = initialValue,
        targetValue = Color.Transparent,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier
            .width(100.dp)
            .height(100.dp)
            .scale(cardScale)
            .border(2.dp, glowingColor, RoundedCornerShape(12.dp))
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    cardScale = 1.0f
                    initialValue = Color.White
                } else {
                    cardScale = 0.5f
                    initialValue = Color.Transparent
                }
            }
            .focusable()
    ) {
        Text(
            text = cardText,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(12.dp),
            color = Color.Red,
            fontWeight = FontWeight.Bold

        )
    }
}

val immersiveClusterMediaItems = listOf(
    Media(id = "1", title = "Title 1", description = "Description 1", backgroundColor = Color.Gray),
    Media(id = "2", title = "Title 2", description = "Description 2", backgroundColor = Color.Blue),
    Media(id = "3", title = "Title 3", description = "Description 3", backgroundColor = Color.Cyan)
)