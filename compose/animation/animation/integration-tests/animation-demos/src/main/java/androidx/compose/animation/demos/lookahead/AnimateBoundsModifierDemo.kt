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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AnimateBoundsModifierDemo() {
    var height by remember { mutableIntStateOf(200) }
    var left by remember { mutableIntStateOf(0) }
    var top by remember { mutableIntStateOf(0) }
    var right by remember { mutableIntStateOf(0) }
    var bottom by remember { mutableIntStateOf(0) }
    var weight by remember { mutableFloatStateOf(2f) }

    LookaheadScope {
        Column(
            Modifier.fillMaxSize().clickable {
                height = Random.nextInt(10, 300)
                weight = Random.nextDouble(0.5, 4.5).toFloat()

                left = Random.nextInt(0, 200)
                top = Random.nextInt(0, 100)
                right = Random.nextInt(0, 200)
                bottom = Random.nextInt(0, 100)
            }
        ) {
            Box(Modifier.fillMaxHeight(0.5f).fillMaxSize()) {
                Box(
                    Modifier.background(Color.Gray)
                        .animateBounds(
                            this@LookaheadScope,
                            Modifier.padding(left.dp, top.dp, right.dp, bottom.dp)
                        )
                        .background(Color.Red)
                        .fillMaxSize()
                )
            }
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.animateBounds(
                            this@LookaheadScope,
                            Modifier.weight(weight).height(height.dp)
                        )
                        .background(Color(0xffa2d2ff), RoundedCornerShape(5.dp))
                )
                Box(
                    Modifier.animateBounds(
                            this@LookaheadScope,
                            Modifier.weight(1f).height(height.dp)
                        )
                        .background(Color(0xfffff3b0))
                )
            }
        }
    }
}
