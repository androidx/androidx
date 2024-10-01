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

package androidx.compose.animation.demos.sharedelement

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.demos.R
import androidx.compose.animation.demos.layoutanimation.summerColors
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

sealed class State

object List : State()

class Details(val index: Int) : State()

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun LazyGridSharedElementDemo() {
    var target: State by remember { mutableStateOf(List) }
    SharedTransitionLayout {
        AnimatedContent(
            targetState = target,
            transitionSpec = {
                (fadeIn() togetherWith fadeOut()).apply {
                    targetContentZIndex = if (targetState == List) 1f else 0f
                }
            }
        ) {
            if (it == List) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(5.dp)
                ) {
                    items(20) {
                        if (it % 5 == 0) {
                            // cat
                            Image(
                                painterResource(catResIds[(it / 5) % catResIds.size]),
                                null,
                                Modifier.clickable { target = Details(it) }
                                    .aspectRatio(1f)
                                    .padding(5.dp)
                                    .sharedElement(
                                        rememberSharedContentState("cat $it"),
                                        // Using the AnimatedVisibilityScope from the
                                        // AnimatedContent
                                        // defined above.
                                        this@AnimatedContent,
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                Modifier.aspectRatio(1f)
                                    .padding(5.dp)
                                    .background(summerColors[it % 5 % 4], RoundedCornerShape(10.dp))
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            } else if (it is Details) {
                Column(Modifier.fillMaxSize().clickable { target = List }) {
                    Image(
                        painterResource(catResIds[(it.index / 5) % catResIds.size]),
                        contentDescription = null,
                        Modifier.fillMaxWidth()
                            .aspectRatio(1f)
                            // Creating a shared element. Note that this modifier is *after*
                            // the size modifier and aspectRatio modifier, because those size specs
                            // are not shared between the two shared elements.
                            .sharedElement(
                                rememberSharedContentState("cat ${it.index}"),
                                // Using the AnimatedVisibilityScope from the AnimatedContent
                                // defined above.
                                this@AnimatedContent,
                            ),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent fringilla" +
                            " mollis efficitur. Maecenas sit amet urna eu urna blandit suscipit efficitur" +
                            " eget mauris. Nullam eget aliquet ligula. Nunc id euismod elit. Morbi aliquam" +
                            " enim eros, eget consequat dolor consequat id. Quisque elementum faucibus" +
                            " congue. Curabitur mollis aliquet turpis, ut pellentesque justo eleifend nec.\n" +
                            "\n" +
                            "Suspendisse ac consequat turpis, euismod lacinia quam. Nulla lacinia tellus" +
                            " eu felis tristique ultricies. Vivamus et ultricies dolor. Orci varius" +
                            " natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus." +
                            " Ut gravida porttitor arcu elementum elementum. Phasellus ultrices vel turpis" +
                            " volutpat mollis. Vivamus leo diam, placerat quis leo efficitur, ultrices" +
                            " placerat ex. Nullam mollis et metus ac ultricies. Ut ligula metus, congue" +
                            " gravida metus in, vestibulum posuere velit. Sed et ex nisl. Fusce tempor" +
                            " odio eget sapien pellentesque, sed cursus velit fringilla. Nullam odio" +
                            " ipsum, eleifend non consectetur vitae, congue id libero. Etiam tincidunt" +
                            " mauris at urna dictum ornare.\n" +
                            "\n",
                        // Prefer Modifier.sharedBounds for text, unless the texts in both initial
                        // content and target content are exactly the same (i.e. same
                        // size/font/color)
                        modifier =
                            Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@SuppressLint("PrimitiveInCollection")
private val catResIds = listOf(R.drawable.yt_profile, R.drawable.pepper, R.drawable.yt_profile2)
