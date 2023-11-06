/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.demos.layoutanimation.turquoiseColors
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Preview
@Composable
fun LookaheadWithLAnimateItemPlacement() {
    val visible by produceState(true) {
        while (true) {
            delay(2000)
            value = !value
        }
    }
    LookaheadScope {
        LazyColumn(Modifier.padding(20.dp)) {
            items(3, key = { it }) {
                Column(
                    Modifier
                        .animateItemPlacement()
                        .clip(RoundedCornerShape(15.dp))
                        .background(turquoiseColors[it])

                ) {
                    Box(
                        Modifier
                            .requiredHeight(ItemSize.dp)
                            .fillMaxWidth()
                    )
                    AnimatedVisibility(visible = visible) {
                        Box(
                            Modifier
                                .requiredHeight(ItemSize.dp)
                                .fillMaxWidth()
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Suppress("ConstPropertyName")
private const val ItemSize = 100
