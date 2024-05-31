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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation.demos.sharedelement

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.demos.R
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class Screen {
    object List : Screen()

    data class Details(val item: Int) : Screen()
}

@SuppressLint("PrimitiveInCollection")
@Composable
@Preview
fun ListToDetailsDemo() {
    var state by remember { mutableStateOf<Screen>(Screen.List) }
    val images = listOf(R.drawable.pepper, R.drawable.waffle, R.drawable.yt_profile)
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            state,
            label = "",
            contentKey = { it.javaClass },
            transitionSpec = {
                if (initialState == Screen.List) {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                } else {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                }
            }
        ) {
            when (it) {
                Screen.List -> {
                    LazyColumn {
                        items(50) { item ->
                            Row(
                                modifier =
                                    Modifier.clickable(
                                            interactionSource =
                                                remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            state = Screen.Details(item)
                                        }
                                        .fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(images[item % 3]),
                                    modifier =
                                        Modifier.size(100.dp)
                                            .then(
                                                if (item % 3 < 2) {
                                                    Modifier.sharedElement(
                                                        rememberSharedContentState(
                                                            key = "item-image$item"
                                                        ),
                                                        this@AnimatedContent,
                                                    )
                                                } else Modifier
                                            ),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null
                                )
                                Spacer(Modifier.size(15.dp))
                                Text("Item $item")
                            }
                        }
                    }
                }
                is Screen.Details -> {
                    val item = it.item
                    Column(
                        modifier =
                            Modifier.fillMaxSize().clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                state = Screen.List
                            }
                    ) {
                        Image(
                            painter = painterResource(images[item % 3]),
                            modifier =
                                Modifier.then(
                                        if (item % 3 < 2) {
                                            Modifier.sharedElement(
                                                rememberSharedContentState(key = "item-image$item"),
                                                this@AnimatedContent,
                                            )
                                        } else Modifier
                                    )
                                    .fillMaxWidth(),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                        Text("Item $item", fontSize = 23.sp)
                    }
                }
            }
        }
    }
}
