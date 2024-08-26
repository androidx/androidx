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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.demos.R
import androidx.compose.animation.demos.gesture.pastelColors
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun LookaheadWithLazyColumn() {
    LookaheadScope {
        LazyColumn {
            items(10, key = { it }) {
                val index = it % 4
                var expanded by rememberSaveable { mutableStateOf(false) }
                AnimatedVisibility(
                    remember { MutableTransitionState(false) }.apply { targetState = true },
                    enter = slideInHorizontally { 20 } + fadeIn()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = pastelColors[index],
                        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    ) {
                        LookaheadScope {
                            val title = remember {
                                movableContentOf {
                                    Text(names[index], Modifier.padding(20.dp).animateBounds(this))
                                }
                            }
                            val image = remember {
                                if (index < 3) {
                                    movableContentOf {
                                        Image(
                                            painter = painterResource(res[index]),
                                            contentDescription = null,
                                            modifier =
                                                Modifier.padding(10.dp)
                                                    .animateBounds(
                                                        this,
                                                        if (expanded) Modifier.fillMaxWidth()
                                                        else Modifier.size(80.dp),
                                                        { _, _ ->
                                                            spring(
                                                                Spring.DampingRatioNoBouncy,
                                                                Spring.StiffnessLow,
                                                                Rect.VisibilityThreshold
                                                            )
                                                        }
                                                    )
                                                    .clip(RoundedCornerShape(5.dp)),
                                            contentScale =
                                                if (expanded) {
                                                    ContentScale.FillWidth
                                                } else {
                                                    ContentScale.Crop
                                                }
                                        )
                                    }
                                } else {
                                    movableContentOf {
                                        Box(
                                            modifier =
                                                Modifier.padding(10.dp)
                                                    .animateBounds(
                                                        lookaheadScope = this,
                                                        if (expanded)
                                                            Modifier.fillMaxWidth().aspectRatio(1f)
                                                        else Modifier.size(80.dp),
                                                        { _, _ ->
                                                            spring(
                                                                Spring.DampingRatioNoBouncy,
                                                                Spring.StiffnessLow,
                                                                Rect.VisibilityThreshold
                                                            )
                                                        }
                                                    )
                                                    .background(
                                                        Color.LightGray,
                                                        RoundedCornerShape(5.dp)
                                                    ),
                                        )
                                    }
                                }
                            }
                            if (expanded) {
                                Column {
                                    title()
                                    image()
                                }
                            } else {
                                Row {
                                    image()
                                    title()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val names = listOf("YT", "Pepper", "Waffle", "Who?")
val res =
    listOf(
        R.drawable.yt_profile,
        R.drawable.pepper,
        R.drawable.waffle,
    )
