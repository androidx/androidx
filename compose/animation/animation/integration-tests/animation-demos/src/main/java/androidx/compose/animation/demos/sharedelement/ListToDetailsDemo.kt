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
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
    SharedTransitionLayout(modifier = Modifier.clipToBounds().fillMaxSize()) {
        val listState = rememberLazyListState()
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
            },
        ) {
            when (it) {
                Screen.List -> {
                    val customConfig = remember {
                        // Creates a custom SharedContentConfig to configure the alternative target
                        // bounds in the case of the target shared element being disposed amid
                        // shared element transition.
                        object : SharedTransitionScope.SharedContentConfig {
                            override fun SharedTransitionScope.SharedContentState
                                .alternativeTargetBoundsInTransitionScopeAfterRemoval(
                                targetBoundsBeforeRemoval: Rect,
                                sharedTransitionLayoutSize: Size,
                            ): Rect? {

                                // If the bottom edge of the target shared element is below the
                                // viewport, we move the target bounds to 300 pixels below the
                                // viewport in this example, while keeping the same left position,
                                // and target size.
                                if (
                                    targetBoundsBeforeRemoval.bottom >=
                                        sharedTransitionLayoutSize.height
                                ) {
                                    return Rect(
                                        Offset(
                                            targetBoundsBeforeRemoval.left,
                                            sharedTransitionLayoutSize.height + 300f,
                                        ),
                                        targetBoundsBeforeRemoval.size,
                                    )
                                }

                                // If the top edge of the target shared element is above the
                                // viewport before it is disposed, we will move the target bounds
                                // to 300 pixels above the viewport in this example, while keeping
                                // the same left position and target size.
                                if (targetBoundsBeforeRemoval.top < 0) {

                                    return Rect(
                                        Offset(
                                            targetBoundsBeforeRemoval.left,
                                            -300 - targetBoundsBeforeRemoval.height,
                                        ),
                                        targetBoundsBeforeRemoval.size,
                                    )
                                }

                                // If the target bounds were well within the range of the viewport
                                // height, we will use the last seen target bounds as the new
                                // target bounds. Note: The default alternative bounds is null,
                                // meaning the animation will be stopped if the target shared
                                // element is removed.
                                return targetBoundsBeforeRemoval
                            }
                        }
                    }
                    LazyColumn(state = listState) {
                        items(50) { item ->
                            Row(
                                modifier =
                                    Modifier.clickable(
                                            interactionSource =
                                                remember { MutableInteractionSource() },
                                            indication = null,
                                        ) {
                                            state = Screen.Details(item)
                                        }
                                        .fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(images[item % 3]),
                                    modifier =
                                        Modifier.size(100.dp)
                                            .sharedElement(
                                                rememberSharedContentState(
                                                    key = "item-image$item",
                                                    config = customConfig,
                                                ),
                                                this@AnimatedContent,
                                            ),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.size(15.dp))
                                Text(
                                    "Item $item",
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                )
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
                                indication = null,
                            ) {
                                state = Screen.List
                            }
                    ) {
                        Image(
                            painter = painterResource(images[item % 3]),
                            modifier =
                                Modifier.sharedElement(
                                        rememberSharedContentState(key = "item-image$item"),
                                        this@AnimatedContent,
                                    )
                                    .fillMaxWidth(),
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                        )
                        Text("Item $item", fontSize = 23.sp)
                    }
                }
            }
        }
    }
}
