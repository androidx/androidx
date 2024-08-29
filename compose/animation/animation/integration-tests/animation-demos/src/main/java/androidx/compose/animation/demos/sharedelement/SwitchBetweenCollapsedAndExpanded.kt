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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.demos.R
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Preview
@Composable
fun SwitchBetweenCollapsedAndExpanded() {
    var showExpandedCard by remember { mutableStateOf(false) }
    Box(
        Modifier.clickable(
            onClick = { showExpandedCard = !showExpandedCard },
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        )
    ) {
        SharedTransitionLayout {
            HomePage(this@SharedTransitionLayout, !showExpandedCard)
            ExpandedCard(this@SharedTransitionLayout, showExpandedCard)
        }
    }
}

@Composable
fun HomePage(sharedTransitionScope: SharedTransitionScope, showCard: Boolean) {
    with(sharedTransitionScope) {
        Box(Modifier.fillMaxSize().background(Color.White)) {
            Column {
                SearchBarAndTabs()
                Box(Modifier.fillMaxWidth().aspectRatio(1.1f)) {
                    androidx.compose.animation.AnimatedVisibility(visible = showCard) {
                        Column(
                            Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp)
                                .sharedBounds(
                                    rememberSharedContentState(key = "container"),
                                    this@AnimatedVisibility,
                                    clipInOverlayDuringTransition =
                                        OverlayClip(RoundedCornerShape(20.dp))
                                )
                                .clip(shape = RoundedCornerShape(20.dp))
                                .background(color = cardBackgroundColor),
                        ) {
                            Box {
                                Column {
                                    Image(
                                        painterResource(R.drawable.quiet_night),
                                        contentDescription = null,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .sharedElement(
                                                    rememberSharedContentState(key = "quiet_night"),
                                                    this@AnimatedVisibility,
                                                    zIndexInOverlay = 0.5f,
                                                ),
                                        contentScale = ContentScale.FillWidth
                                    )
                                    Text(
                                        text = longText,
                                        color = Color.Gray,
                                        fontSize = 15.sp,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                                                .height(14.dp)
                                                .sharedElement(
                                                    rememberSharedContentState(key = "longText"),
                                                    this@AnimatedVisibility,
                                                )
                                                .clipToBounds()
                                                .wrapContentHeight(
                                                    align = Alignment.Top,
                                                    unbounded = true
                                                )
                                                .skipToLookaheadSize(),
                                    )
                                }

                                Text(
                                    text = title,
                                    fontFamily = FontFamily.Default,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .renderInSharedTransitionScopeOverlay(
                                                zIndexInOverlay = 1f
                                            )
                                            .animateEnterExit(
                                                fadeIn(tween(1000)) + slideInVertically { -it / 3 },
                                                fadeOut(tween(50)) + slideOutVertically { -it / 3 }
                                            )
                                            .skipToLookaheadSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Transparent,
                                                        Color.Black,
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                            .padding(20.dp),
                                )
                            }
                            InstallBar(
                                Modifier.fillMaxWidth()
                                    .zIndex(1f)
                                    .sharedElementWithCallerManagedVisibility(
                                        rememberSharedContentState(key = "install_bar"),
                                        showCard,
                                    )
                            )
                        }
                    }
                }
                Cluster()
            }
            Image(
                painterResource(R.drawable.navigation_bar),
                contentDescription = null,
                Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@Composable
fun Cluster() {
    Text(
        text = "Lorem ipsum",
        Modifier.padding(top = 20.dp, bottom = 10.dp, start = 20.dp),
        color = Color.Black,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W500,
        fontSize = 25.sp
    )
    Row(Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true)) {
        Image(
            painterResource(R.drawable.item0),
            contentDescription = null,
            modifier =
                Modifier.requiredHeight(200.dp)
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
                    .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.FillHeight
        )
        Image(
            painterResource(R.drawable.item1),
            contentDescription = null,
            modifier =
                Modifier.requiredHeight(200.dp).padding(10.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.FillHeight
        )
    }
}

@Composable
fun ExpandedCard(sharedTransitionScope: SharedTransitionScope, visible: Boolean) {
    with(sharedTransitionScope) {
        AnimatedVisibility(
            visible = visible,
            Modifier.fillMaxSize(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(Modifier.fillMaxSize().background(Color(0x55000000))) {
                Column(
                    Modifier.align(Alignment.Center)
                        .padding(20.dp)
                        .sharedBounds(
                            rememberSharedContentState(key = "container"),
                            this@AnimatedVisibility,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(20.dp))
                        )
                        .clip(shape = RoundedCornerShape(20.dp))
                        .background(cardBackgroundColor)
                ) {
                    Column(
                        Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
                            .animateEnterExit(
                                fadeIn() + slideInVertically { it / 3 },
                                fadeOut() + slideOutVertically { it / 3 }
                            )
                            .skipToLookaheadSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black, Color.Transparent)
                                )
                            )
                            .padding(start = 20.dp, end = 20.dp),
                    ) {
                        Text(
                            text = "Lorem ipsum",
                            Modifier.padding(top = 20.dp, bottom = 10.dp)
                                .background(Color.LightGray, shape = RoundedCornerShape(15.dp))
                                .padding(top = 8.dp, bottom = 8.dp, start = 15.dp, end = 15.dp),
                            color = Color.Black,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 30.sp,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        )
                    }
                    Image(
                        painterResource(R.drawable.quiet_night),
                        contentDescription = null,
                        modifier =
                            Modifier.fillMaxWidth()
                                .sharedElement(
                                    rememberSharedContentState("quiet_night"),
                                    this@AnimatedVisibility,
                                ),
                        contentScale = ContentScale.FillWidth
                    )

                    Text(
                        text = longText,
                        color = Color.Gray,
                        fontSize = 15.sp,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(start = 15.dp, end = 10.dp, top = 10.dp)
                                .height(50.dp)
                                .sharedElement(
                                    rememberSharedContentState("longText"),
                                    this@AnimatedVisibility,
                                )
                                .clipToBounds()
                                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                                .skipToLookaheadSize(),
                    )

                    InstallBar(
                        Modifier.fillMaxWidth()
                            .zIndex(1f)
                            .sharedElement(
                                rememberSharedContentState("install_bar"),
                                this@AnimatedVisibility,
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBarAndTabs() {
    Image(
        painterResource(R.drawable.search_bar),
        contentDescription = null,
        modifier = Modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth
    )
    Image(
        painterResource(R.drawable.tabs),
        contentDescription = null,
        modifier = Modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth
    )
}

@Composable
private fun InstallBar(modifier: Modifier) {
    Row(
        modifier
            .background(Color(0xff444448))
            .padding(start = 5.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
            .fillMaxWidth()
            .requiredHeight(60.dp)
    ) {
        Image(
            painterResource(R.drawable.quiet_night_thumb),
            contentDescription = null,
            Modifier.padding(10.dp).requiredSize(40.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            Modifier.fillMaxHeight().padding(top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text("Lorem ipsum dolor", color = Color.LightGray, fontSize = 15.sp)
            Text("Lorem", color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "Lorem",
            Modifier.background(Color.Gray, shape = RoundedCornerShape(25.dp))
                .align(Alignment.CenterVertically)
                .padding(top = 8.dp, bottom = 8.dp, start = 25.dp, end = 25.dp),
            color = Color.White,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

private const val title = "Lorem ipsum dolor sit amet, sed do eiusmod tempor"

private const val longText =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt" +
        " ut labore et dolore magna aliqua. Adipiscing enim eu turpis egestas pretium aenean" +
        " pharetra magna. Consectetur libero id faucibus nisl tincidunt eget. Est placerat in " +
        "egestas erat imperdiet sed euismod nisi. Mauris a diam maecenas sed. Urna nunc id" +
        " cursus metus aliquam eleifend mi in nulla. Pellentesque sit amet porttitor eget " +
        "dolor morbi. A lacus vestibulum sed arcu non odio euismod. Integer enim neque volutpat" +
        " ac tincidunt vitae. Nunc lobortis mattis aliquam faucibus purus in. In egestas erat " +
        "imperdiet sed euismod nisi porta lorem. Fermentum leo vel orci porta non."

private val cardBackgroundColor = Color(0xff222222)
