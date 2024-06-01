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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Experimenting with sharedBounds default transition for texts of different sizes, colors and font
 * styles, using the default scaleToBounds resize mode and fadeIn/Out // TODO: A demo to compare
 * different contentScale (i.e. Fill, Crop) for texts // TODO: Play with scale in sharedElement
 */
@Preview
@Composable
fun TextSharedBoundsExperiments() {
    var isHorizontal by remember { mutableStateOf(true) }
    SharedTransitionLayout(
        Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isHorizontal = !isHorizontal
            }
            .fillMaxSize()
            .padding(20.dp)
    ) {
        AnimatedContent(
            isHorizontal,
            Modifier.fillMaxSize()
                .aspectRatio(1f)
                .shadow(10.dp, RoundedCornerShape(10), clip = false)
                .background(color = Color.White, shape = RoundedCornerShape(10))
                .clip(RoundedCornerShape(10))
        ) { isHorizontal ->
            if (isHorizontal) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        "Bold",
                        fontSize = 30.sp,
                        modifier =
                            Modifier.sharedBounds(
                                rememberSharedContentState(key = "Bold"),
                                this@AnimatedContent
                            ),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xff4f6d7a)
                    )
                    Text(
                        "Italic",
                        fontSize = 30.sp,
                        modifier =
                            Modifier.sharedBounds(
                                rememberSharedContentState(key = "Italic"),
                                this@AnimatedContent
                            ),
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray
                    )
                    Text(
                        "Normal",
                        fontSize = 30.sp,
                        color = Color(0xff56a3a6),
                        modifier =
                            Modifier.sharedBounds(
                                rememberSharedContentState(key = "Normal"),
                                this@AnimatedContent
                            )
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        "Normal",
                        fontSize = 50.sp,
                        modifier =
                            Modifier.fillMaxWidth()
                                .wrapContentWidth(align = Alignment.CenterHorizontally)
                                .sharedBounds(
                                    rememberSharedContentState(key = "Normal"),
                                    this@AnimatedContent,
                                ),
                        color = Color(0xffE85D04)
                    )
                    Text(
                        "Bold",
                        fontSize = 70.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xffF48C06),
                        modifier =
                            Modifier.fillMaxWidth()
                                .wrapContentWidth(align = Alignment.CenterHorizontally)
                                .sharedBounds(
                                    rememberSharedContentState(key = "Bold"),
                                    this@AnimatedContent
                                )
                    )
                    Text(
                        "Italic",
                        fontSize = 50.sp,
                        fontStyle = FontStyle.Italic,
                        modifier =
                            Modifier.sharedBounds(
                                rememberSharedContentState(key = "Italic"),
                                this@AnimatedContent
                            ),
                        color = Color(0xffFFBA08)
                    )
                }
            }
        }
    }
}
