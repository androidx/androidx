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

package androidx.compose.animation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
fun NestedSharedBoundsSample() {
    // Nested shared bounds sample.
    val selectionColor = Color(0xff3367ba)
    var expanded by remember { mutableStateOf(true) }
    SharedTransitionLayout(
        Modifier.fillMaxSize().clickable { expanded = !expanded }.background(Color(0x88000000))
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    Modifier.align(Alignment.BottomCenter)
                        .padding(20.dp)
                        .sharedBounds(
                            rememberSharedContentState(key = "container"),
                            this@AnimatedVisibility
                        )
                        .requiredHeightIn(max = 60.dp),
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        Modifier.padding(10.dp)
                            // By using Modifier.skipToLookaheadSize(), we are telling the layout
                            // system to layout the children of this node as if the animations had
                            // all finished. This avoid re-laying out the Row with animated width,
                            // which is _sometimes_ desirable. Try removing this modifier and
                            // observe the effect.
                            .skipToLookaheadSize()
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "Share",
                            modifier =
                                Modifier.padding(
                                    top = 10.dp,
                                    bottom = 10.dp,
                                    start = 10.dp,
                                    end = 20.dp
                                )
                        )
                        Icon(
                            Icons.Outlined.Favorite,
                            contentDescription = "Favorite",
                            modifier =
                                Modifier.padding(
                                    top = 10.dp,
                                    bottom = 10.dp,
                                    start = 10.dp,
                                    end = 20.dp
                                )
                        )
                        Icon(
                            Icons.Outlined.Create,
                            contentDescription = "Create",
                            tint = Color.White,
                            modifier =
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "icon_background"),
                                        this@AnimatedVisibility
                                    )
                                    .background(selectionColor, RoundedCornerShape(50))
                                    .padding(
                                        top = 10.dp,
                                        bottom = 10.dp,
                                        start = 20.dp,
                                        end = 20.dp
                                    )
                                    .sharedElement(
                                        rememberSharedContentState(key = "icon"),
                                        this@AnimatedVisibility
                                    )
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = !expanded,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    Modifier.align(Alignment.BottomEnd)
                        .padding(30.dp)
                        .sharedBounds(
                            rememberSharedContentState(key = "container"),
                            this@AnimatedVisibility,
                            enter = EnterTransition.None,
                        )
                        .sharedBounds(
                            rememberSharedContentState(key = "icon_background"),
                            this@AnimatedVisibility,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None
                        ),
                    shape = RoundedCornerShape(30.dp),
                    color = selectionColor
                ) {
                    Icon(
                        Icons.Outlined.Create,
                        contentDescription = "Create",
                        tint = Color.White,
                        modifier =
                            Modifier.padding(30.dp)
                                .size(40.dp)
                                .sharedElement(
                                    rememberSharedContentState(key = "icon"),
                                    this@AnimatedVisibility
                                )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
fun SharedElementWithMovableContentSample() {
    var showThumbnail by remember { mutableStateOf(true) }
    val movableContent = remember {
        movableContentOf {
            val cornerRadius = animateDpAsState(targetValue = if (!showThumbnail) 20.dp else 5.dp)
            Image(
                painterResource(id = R.drawable.yt_profile),
                contentDescription = "cute cat",
                contentScale = ContentScale.FillHeight,
                modifier = Modifier.clip(shape = RoundedCornerShape(cornerRadius.value))
            )
        }
    }
    SharedTransitionLayout(
        Modifier.clickable { showThumbnail = !showThumbnail }.fillMaxSize().padding(10.dp)
    ) {
        Column {
            Box(
                // When using Modifier.sharedElementWithCallerManagedVisibility(), even when
                // visible == false, the layout will continue to occupy space in its parent layout.
                // The content will continue to be composed, unless the content is [MovableContent]
                // like in this example below.
                Modifier.sharedElementWithCallerManagedVisibility(
                        rememberSharedContentState(key = "YT"),
                        showThumbnail,
                    )
                    .size(100.dp)
            ) {
                if (showThumbnail) {
                    movableContent()
                }
            }
            Box(
                Modifier.fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xffffcc5c), RoundedCornerShape(5.dp))
            )
            Box(
                Modifier.fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xff2a9d84), RoundedCornerShape(5.dp))
            )
        }
        Box(
            Modifier.fillMaxSize()
                .aspectRatio(1f)
                .sharedElementWithCallerManagedVisibility(
                    rememberSharedContentState(key = "YT"),
                    !showThumbnail
                )
        ) {
            if (!showThumbnail) {
                movableContent()
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
fun SharedElementWithFABInOverlaySample() {
    // Create an Image that will be shared between the two shared elements.
    @Composable
    fun Cat(modifier: Modifier = Modifier) {
        Image(
            painterResource(id = R.drawable.yt_profile),
            contentDescription = "cute cat",
            contentScale = ContentScale.FillHeight,
            modifier = modifier.clip(shape = RoundedCornerShape(10))
        )
    }

    var showThumbnail by remember { mutableStateOf(true) }
    SharedTransitionLayout(
        Modifier.clickable { showThumbnail = !showThumbnail }.fillMaxSize().padding(10.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            // Create an AnimatedVisibility for the shared element, so that the layout siblings
            // (i.e. the two boxes below) will move in to fill the space during the exit transition.
            AnimatedVisibility(visible = showThumbnail) {
                Cat(
                    Modifier.size(100.dp)
                        // Create a shared element, using string as the key
                        .sharedElement(
                            rememberSharedContentState(key = "YT"),
                            this@AnimatedVisibility,
                        )
                )
            }
            Box(
                Modifier.fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xffffcc5c), RoundedCornerShape(5.dp))
            )
            Box(
                Modifier.fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xff2a9d84), RoundedCornerShape(5.dp))
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(!showThumbnail) {
                Cat(
                    Modifier.fillMaxSize()
                        // Create another shared element, and make sure the string key matches
                        // the other shared element.
                        .sharedElement(
                            rememberSharedContentState(key = "YT"),
                            this@AnimatedVisibility,
                        )
                )
            }
            FloatingActionButton(
                modifier =
                    Modifier.padding(20.dp)
                        .align(Alignment.BottomEnd)
                        // During shared element transition, shared elements will be rendered in
                        // overlay to escape any clipping or layer transform from parents. It also
                        // means they will render over on top of UI elements such as Floating Action
                        // Button. Once the transition is finished, they will be dropped from the
                        // overlay to their own DrawScopes. To help support keeping specific UI
                        // elements always on top, Modifier.renderInSharedTransitionScopeOverlay
                        // will temporarily elevate them into the overlay as well. By default,
                        // this modifier keeps content in overlay during the time when the
                        // shared transition is active (i.e.
                        // SharedTransitionScope#isTransitionActive).
                        // The duration can be customize via `renderInOverlay` parameter.
                        .renderInSharedTransitionScopeOverlay(
                            // zIndexInOverlay by default is 0f for this modifier and for shared
                            // elements. By overwriting zIndexInOverlay to 1f, we can ensure this
                            // FAB is rendered on top of the shared elements.
                            zIndexInOverlay = 1f
                        ),
                onClick = {}
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "favorite")
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
@Sampled
fun SharedElementInAnimatedContentSample() {
    // This is the Image that we will add shared element modifier on. It's important to make sure
    // modifiers that are not shared between the two shared elements (such as size modifiers if
    // the size changes) are the parents (i.e. on the left side) of Modifier.sharedElement.
    // Meanwhile, the modifiers that are shared between the shared elements (e.g. Modifier.clip
    // in this case) are on the right side of the Modifier.sharedElement.
    @Composable
    fun Cat(modifier: Modifier = Modifier) {
        Image(
            painterResource(id = R.drawable.yt_profile),
            contentDescription = "cute cat",
            contentScale = ContentScale.FillHeight,
            modifier = modifier.clip(shape = RoundedCornerShape(10))
        )
    }

    // Shared element key is of type `Any`, which means it can be id, string, etc. The only
    // requirement for the key is that it should be the same for shared elements that you intend
    // to match. Here we use the image resource id as the key.
    val sharedElementKey = R.drawable.yt_profile
    var showLargeImage by remember { mutableStateOf(true) }

    // First, we need to create a SharedTransitionLayout, this Layout will provide the coordinator
    // space for shared element position animation, as well as an overlay for shared elements to
    // render in. Children content in this Layout will be able to create shared element transition
    // using the receiver scope: SharedTransitionScope
    SharedTransitionLayout(
        Modifier.clickable { showLargeImage = !showLargeImage }.fillMaxSize().padding(10.dp)
    ) {
        // In the SharedTransitionLayout, we will be able to access the receiver scope (i.e.
        // SharedTransitionScope) in order to create shared element transition.
        AnimatedContent(targetState = showLargeImage) { showLargeImageMode ->
            if (showLargeImageMode) {
                Cat(
                    Modifier.fillMaxSize()
                        .aspectRatio(1f)
                        // Creating a shared element. Note that this modifier is *after*
                        // the size modifier and aspectRatio modifier, because those size specs
                        // are not shared between the two shared elements.
                        .sharedElement(
                            rememberSharedContentState(sharedElementKey),
                            // Using the AnimatedVisibilityScope from the AnimatedContent
                            // defined above.
                            this@AnimatedContent,
                        )
                )
                Text(
                    "Cute Cat YT",
                    fontSize = 40.sp,
                    color = Color.Blue,
                    // Prefer Modifier.sharedBounds for text, unless the texts in both initial
                    // content and target content are exactly the same (i.e. same
                    // size/font/color)
                    modifier =
                        Modifier.fillMaxWidth()
                            // IMPORTANT: Prefer using wrapContentWidth/wrapContentSize over
                            // textAlign
                            // for shared text transition. This allows the layout system sees actual
                            // position and size of the text to facilitate bounds animation.
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .sharedBounds(
                                rememberSharedContentState(key = "text"),
                                this@AnimatedContent
                            )
                )
            } else {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Cat(
                            Modifier.size(100.dp)
                                // Creating another shared element with the same key.
                                // Note that this modifier is *after* the size modifier,
                                // The size changes between these two shared elements, i.e. the size
                                // is not shared between the two shared elements.
                                .sharedElement(
                                    rememberSharedContentState(sharedElementKey),
                                    this@AnimatedContent,
                                )
                        )
                        Text(
                            "Cute Cat YT",
                            // Change text color & size
                            fontSize = 20.sp,
                            color = Color.DarkGray,
                            // Prefer Modifier.sharedBounds for text, unless the texts in both
                            // initial content and target content are exactly the same (i.e. same
                            // size/font/color)
                            modifier =
                                Modifier
                                    // The modifier that is not a part of the shared content, but
                                    // rather
                                    // for positioning and sizes should be on the *left* side of
                                    // sharedBounds/sharedElement.
                                    .padding(start = 20.dp)
                                    .sharedBounds(
                                        // Here we use a string-based key, in contrast to the key
                                        // above.
                                        rememberSharedContentState(key = "text"),
                                        this@AnimatedContent
                                    )
                        )
                    }
                    Box(
                        Modifier.fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xffffcc5c), RoundedCornerShape(5.dp))
                    )
                    Box(
                        Modifier.fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xff2a9d84), RoundedCornerShape(5.dp))
                    )
                }
            }
        }
    }
}
