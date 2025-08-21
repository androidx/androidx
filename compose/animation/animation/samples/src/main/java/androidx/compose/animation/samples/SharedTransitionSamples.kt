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
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.SpaceEvenly
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.lookaheadScopeCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
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
            exit = ExitTransition.None,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    Modifier.align(Alignment.BottomCenter)
                        .padding(20.dp)
                        .sharedBounds(
                            rememberSharedContentState(key = "container"),
                            this@AnimatedVisibility,
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
                                    end = 20.dp,
                                ),
                        )
                        Icon(
                            Icons.Outlined.Favorite,
                            contentDescription = "Favorite",
                            modifier =
                                Modifier.padding(
                                    top = 10.dp,
                                    bottom = 10.dp,
                                    start = 10.dp,
                                    end = 20.dp,
                                ),
                        )
                        Icon(
                            Icons.Outlined.Create,
                            contentDescription = "Create",
                            tint = Color.White,
                            modifier =
                                Modifier.sharedBounds(
                                        rememberSharedContentState(key = "icon_background"),
                                        this@AnimatedVisibility,
                                    )
                                    .background(selectionColor, RoundedCornerShape(50))
                                    .padding(
                                        top = 10.dp,
                                        bottom = 10.dp,
                                        start = 20.dp,
                                        end = 20.dp,
                                    )
                                    .sharedElement(
                                        rememberSharedContentState(key = "icon"),
                                        this@AnimatedVisibility,
                                    ),
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = !expanded,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
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
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ),
                    shape = RoundedCornerShape(30.dp),
                    color = selectionColor,
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
                                    this@AnimatedVisibility,
                                ),
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
                modifier = Modifier.clip(shape = RoundedCornerShape(cornerRadius.value)),
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
                    !showThumbnail,
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
            modifier = modifier.clip(shape = RoundedCornerShape(10)),
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
                onClick = {},
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
            modifier = modifier.clip(shape = RoundedCornerShape(10)),
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
                                this@AnimatedContent,
                            ),
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
                                        this@AnimatedContent,
                                    ),
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

@Sampled
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementClipRevealSample() {
    // In this sample, we are creating an animating clip bounds using shared element transition.
    // In the meantime, we need to anchor the content that is being clipped at its target position
    // using skipToLookaheadPosition while the clip bounds moves and resizes via animations.
    var target by remember { mutableStateOf(true) }
    BackHandler { target = !target }
    // Creates a SharedTransitionLayout to provide its child content with a SharedTransitionScope.
    // The child content can therefore set up shared element transitions, and access
    // skipToLookaheadPosition modifier, as well as other functionalities available in
    // SharedTransitionScope.
    SharedTransitionLayout {
        AnimatedContent(targetState = target) {
            if (it) {
                Box(Modifier.fillMaxSize()) {
                    Button(
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .sharedBounds(
                                    rememberSharedContentState("clip"),
                                    this@AnimatedContent,
                                ),
                        onClick = { target = false },
                    ) {
                        Text("Toggle State")
                    }
                }
            } else {
                Column(
                    // Use sharedBounds chained with clipToBounds to animate the clip bounds
                    // from the previous size and position derived from the shared bounds above
                    // (when target == true).
                    Modifier.sharedBounds(
                            rememberSharedContentState("clip"),
                            this@AnimatedContent,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        )
                        .clipToBounds()
                        // The sharedBounds above would resize its child layout and move it as
                        // needed. Here we use `skipToLookaheadSize` chained with
                        // `skipToLookaheadPosition` to keep the child content from being resized
                        // or moved. As such, only the clip bounds is being animated, creating
                        // a reveal animation.
                        .skipToLookaheadSize()
                        .skipToLookaheadPosition()
                        .fillMaxSize()
                        .background(Color.Black),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Hello", fontSize = 80.sp, color = Color.White)
                    Text("Shared", fontSize = 80.sp, color = Color.White)
                    Text("Clip", fontSize = 80.sp, color = Color.White)
                    Text("Bounds", fontSize = 80.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun BackHandler(content: @Composable () -> Unit) {
    TODO("Not yet implemented")
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun DynamicallyEnabledSharedElementInPagerSample() {
    // In this example, we will dynamically enable/disable shared elements for the items in the
    // Pager. Specifically, we will only enable shared element transition for items that are
    // completely visible in the viewport.
    val colors = remember {
        listOf(
            Color(0xFFffd7d7.toInt()),
            Color(0xFFffe9d6.toInt()),
            Color(0xFFfffbd0.toInt()),
            Color(0xFFe3ffd9.toInt()),
            Color(0xFFd0fff8.toInt()),
        )
    }
    val TwoPagesPerViewport = remember {
        object : PageSize {
            override fun Density.calculateMainAxisPageSize(
                availableSpace: Int,
                pageSpacing: Int,
            ): Int {
                return (availableSpace - 2 * pageSpacing) / 2
            }
        }
    }
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    val pagerState = rememberPagerState { colors.size }
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(selectedColor) { colorSelected ->
            if (colorSelected == null) {
                HorizontalPager(
                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                    state = pagerState,
                    pageSize = TwoPagesPerViewport,
                    pageSpacing = 8.dp,
                    snapPosition = SnapPosition.Center,
                    flingBehavior =
                        PagerDefaults.flingBehavior(
                            state = pagerState,
                            pagerSnapDistance = PagerSnapDistance.atMost(3),
                        ),
                ) {
                    val color = colors[it]
                    var coordinates: LayoutCoordinates? by remember { mutableStateOf(null) }
                    Box(
                        Modifier.clickable { selectedColor = color }
                            .onPlaced { coordinates = it }
                            .sharedElement(
                                rememberSharedContentState(
                                    color,
                                    SharedContentConfig {
                                        // This is a lambda that returns a Boolean indicating
                                        // whether shared element should be enabled.
                                        val nonNullCoordinates =
                                            // If the item has never been placed, we will consider
                                            // it enabled.
                                            coordinates ?: return@SharedContentConfig true

                                        // In this specific case, we will use the
                                        // SharedTransitionLayout to approximate viewport.
                                        val scopeCoords =
                                            // Obtain the coordinates of the SharedTransitionLayout/
                                            // SharedTransitionScope.
                                            // Since SharedTransitionScope is a LookaheadScope, we
                                            // can use `lookaheadScopeCoordinates` to acquire the
                                            // coordinates of the scope.
                                            nonNullCoordinates.lookaheadScopeCoordinates(
                                                this@SharedTransitionLayout
                                            )
                                        val (w, h) = scopeCoords.size
                                        // Calculate the relative position of the item within
                                        // SharedTransitionLayout.
                                        val positionInScope =
                                            scopeCoords.localPositionOf(nonNullCoordinates)
                                        // Check the left, top, right, bottom of the relative
                                        // bounds of the item to see if it is within
                                        // SharedTransitionLayout. This result will inform
                                        // whether shared element transition should be enabled
                                        // for this item.
                                        positionInScope.x >= 0 &&
                                            positionInScope.y >= 0 &&
                                            positionInScope.x + nonNullCoordinates.size.width <=
                                                w &&
                                            positionInScope.y + nonNullCoordinates.size.height <= h
                                    },
                                ),
                                this@AnimatedContent,
                            )
                            .background(color)
                            .size(150.dp)
                    )
                }
            } else {
                Box(
                    Modifier.sharedElement(
                            rememberSharedContentState(colorSelected),
                            this@AnimatedContent,
                        )
                        .background(colorSelected)
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .clickable { selectedColor = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
fun SharedContentConfigSample() {
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
                if (targetBoundsBeforeRemoval.bottom >= sharedTransitionLayoutSize.height) {
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
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
fun DynamicallyEnableSharedElementsSample() {
    // In this example, we will enable shared element transition for transitioning from A to B,
    // from A to C, but not the other directions such as B -> A, or B -> C
    @Composable
    fun ScreenA(modifier: Modifier = Modifier) {
        Box(modifier.size(200.dp).background(Color.Red), contentAlignment = Alignment.Center) {
            Text(text = "A", fontSize = 50.sp)
        }
    }

    @Composable
    fun ScreenB(modifier: Modifier = Modifier) {
        Box(modifier.size(400.dp).background(Color.Yellow), contentAlignment = Alignment.Center) {
            Text(text = "B", fontSize = 50.sp)
        }
    }

    @Composable
    fun ScreenC(modifier: Modifier = Modifier) {
        Box(
            modifier.size(100.dp, 300.dp).background(Color.Blue),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "C", fontSize = 50.sp)
        }
    }

    var targetState by remember { mutableStateOf("A") }
    val listOfEnabledStatePairs = remember { mutableStateListOf("A" to "B", "A" to "C") }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = SpaceEvenly) {
            Button(onClick = { targetState = "A" }) { Text("To A") }
            Button(onClick = { targetState = "B" }) { Text("To B") }
            Button(onClick = { targetState = "C" }) { Text("To C") }
        }
        SharedTransitionLayout {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val animatedContentTransition = updateTransition(targetState)

                val config = remember {
                    // Creates a SharedContentConfig to dynamically enable/disable shared elements
                    SharedContentConfig {
                        // Returns whether a shared element should be enabled based on
                        // the current state of the target state of the AnimatedContent.
                        listOfEnabledStatePairs.contains(
                            animatedContentTransition.currentState to
                                animatedContentTransition.targetState
                        )
                    }
                }
                animatedContentTransition.AnimatedContent(
                    transitionSpec = { fadeIn() togetherWith fadeOut() using null }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (it) {
                            "A" ->
                                ScreenA(
                                    Modifier.sharedElement(
                                        rememberSharedContentState("square", config),
                                        this@AnimatedContent,
                                    )
                                )

                            "B" ->
                                ScreenB(
                                    Modifier.sharedElement(
                                        rememberSharedContentState("square", config),
                                        this@AnimatedContent,
                                    )
                                )

                            "C" ->
                                ScreenC(
                                    Modifier.sharedElement(
                                        rememberSharedContentState("square", config),
                                        this@AnimatedContent,
                                    )
                                )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
fun SharedBoundsSample() {
    var showText by remember { mutableStateOf(true) }
    val rememberSharedKey = remember { Any() }
    SharedTransitionLayout(Modifier.clickable { showText = !showText }) {
        AnimatedContent(
            targetState = showText,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { showText ->
            if (showText) {
                Text(
                    text =
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent fringilla" +
                            " mollis efficitur. Maecenas sit amet urna eu urna blandit suscipit efficitur" +
                            " eget mauris. Nullam eget aliquet ligula. Nunc id euismod elit. Morbi aliquam" +
                            " enim eros, eget consequat dolor consequat id. Quisque elementum faucibus" +
                            " congue. Curabitur mollis aliquet turpis, ut pellentesque justo eleifend nec.\n",
                    fontSize = 20.sp,
                    modifier =
                        Modifier.padding(20.dp)
                            // Creates a shared bounds for the text so that we can animate from the
                            // bounds of the text to the bounds of the image below.
                            .sharedBounds(
                                // Here we use an object created above as the key for the shared
                                // bounds transition.
                                sharedContentState =
                                    rememberSharedContentState(key = rememberSharedKey),
                                animatedVisibilityScope = this,
                                // As the bounds transition from the text from/to the image, the
                                // text
                                // will be fading within in the bounds. This is also the default
                                // behavior.
                                enter = fadeIn(),
                                exit = fadeOut(),
                                // Since the text and the image have different aspect ratios, as
                                // the bounds transition from one to the other, we need to define
                                // how to fit the content in the changing bounds. Here we will
                                // be using crop to fit the content.
                                resizeMode = scaleToBounds(contentScale = ContentScale.Crop),
                            ),
                )
            } else {
                Image(
                    painterResource(id = R.drawable.yt_profile),
                    contentDescription = "cute cat",
                    modifier =
                        Modifier.wrapContentSize()
                            // Creates a shared bounds for the image so that we can animate from the
                            // bounds of the text to the bounds of this image, and vice versa.
                            .sharedBounds(
                                sharedContentState =
                                    rememberSharedContentState(key = rememberSharedKey),
                                animatedVisibilityScope = this,
                                resizeMode = scaleToBounds(contentScale = ContentScale.Crop),
                            )
                            .requiredSize(200.dp)
                            .clip(shape = RoundedCornerShape(10)),
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
fun ListToDetailSample() {
    var itemSelected by remember { mutableStateOf<Int?>(null) }
    SharedTransitionLayout(modifier = Modifier.clipToBounds().fillMaxSize()) {
        val listState = rememberLazyListState()
        AnimatedContent(itemSelected) { selected ->
            when (selected) {
                null -> { // No item selected, show list
                    LazyColumn(state = listState) {
                        items(50) { item ->
                            Row(
                                modifier = Modifier.clickable { itemSelected = item }.fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.yt_profile),
                                    modifier =
                                        Modifier.size(100.dp)
                                            .sharedElement(
                                                rememberSharedContentState(key = "item-image$item"),
                                                animatedVisibilityScope = this@AnimatedContent,
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
                else -> { // show detail for item selected
                    Column(modifier = Modifier.fillMaxSize().clickable { itemSelected = null }) {
                        Image(
                            painter = painterResource(R.drawable.yt_profile),
                            modifier =
                                Modifier.sharedElement(
                                        rememberSharedContentState(key = "item-image$selected"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                    )
                                    .fillMaxWidth(),
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                        )
                        Text("Item $itemSelected", fontSize = 23.sp)
                    }
                }
            }
        }
    }
}
