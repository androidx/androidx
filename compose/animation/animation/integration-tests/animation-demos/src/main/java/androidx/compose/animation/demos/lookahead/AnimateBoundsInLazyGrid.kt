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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.demos.layoutanimation.turquoiseColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun AnimateBoundsInLazyGrid() {
    val width by
        produceState(300.dp) {
            while (true) {
                delay(1000)
                // Toggle between 300.dp and 400.dp every 1000ms
                value = (700 - value.value).dp
            }
        }
    LookaheadScope {
        LazyVerticalGrid(
            GridCells.Adaptive(160.dp),
            Modifier.padding(3.dp).fillMaxHeight().width(width).border(2.dp, Color.Blue),
        ) {
            items(40, key = { it }) { id ->
                Box(
                    Modifier.animateBounds(this@LookaheadScope)
                        .animateItem()
                        .padding(10.dp)
                        .background(
                            turquoiseColors[id % turquoiseColors.size],
                            RoundedCornerShape(10.dp),
                        )
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }
        }
    }
}
