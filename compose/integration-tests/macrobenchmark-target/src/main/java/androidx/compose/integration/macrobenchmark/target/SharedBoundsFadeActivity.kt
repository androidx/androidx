/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import android.view.Choreographer
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Benchmark target for the `sharedBounds` crossfade: a container transform between a list and a
 * detail screen.
 *
 * Only the `sharedBounds` content fades. [AnimatedContent] uses a zero-offset slide, so every
 * fading layer in the trace belongs to the shared bounds.
 *
 * [EXTRA_CONTENT]: `image` (one draw call) or `page` (hero image, scrim, title, avatar, button,
 * FAB, body text).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class SharedBoundsFadeActivity : ComponentActivity() {

    @Volatile private var clickPending = false
    @Volatile private var transitionActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: CONTENT_PAGE

        setContent {
            var showDetail by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxSize().background(Color(0xFFFAF7FF))) {
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    LaunchedEffect(Unit) {
                        snapshotFlow { isTransitionActive }
                            .collect { active ->
                                transitionActive = active
                                if (active) clickPending = false
                            }
                    }
                    AnimatedContent(
                        targetState = showDetail,
                        transitionSpec = {
                            (slideInHorizontally(tween(TRANSITION_MS)) { 0 } togetherWith
                                    slideOutHorizontally(tween(TRANSITION_MS)) { 0 })
                                .using(sizeTransform = null)
                        },
                        label = "container transform",
                    ) { detail ->
                        val sharedScope = this@SharedTransitionLayout
                        val visibilityScope = this@AnimatedContent
                        if (detail) {
                            DetailScreen(content, sharedScope, visibilityScope)
                        } else {
                            ListScreen(content, sharedScope, visibilityScope)
                        }
                    }
                }
                // Below the status bar: UiAutomator ignores nodes covered by another window.
                Box(
                    Modifier.align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .size(width = 120.dp, height = 44.dp)
                        .background(Color.DarkGray, RoundedCornerShape(8.dp))
                        .semantics { contentDescription = TOGGLE_DESCRIPTION }
                        .clickable {
                            clickPending = true
                            findViewById<View>(android.R.id.content).contentDescription =
                                COMPOSE_BUSY
                            showDetail = !showDetail
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Toggle", color = Color.White)
                }
            }
        }
        launchIdlenessTracking()
    }

    /** Marks the content view idle once the shared element transition has started and ended. */
    private fun launchIdlenessTracking() {
        val contentView: View = findViewById(android.R.id.content)
        val callback =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    val busy =
                        clickPending ||
                            transitionActive ||
                            Recomposer.runningRecomposers.value.any { it.hasPendingWork }
                    contentView.contentDescription = if (busy) COMPOSE_BUSY else COMPOSE_IDLE
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
        Choreographer.getInstance().postFrameCallback(callback)
    }

    companion object {
        const val EXTRA_CONTENT = "content"
        const val CONTENT_IMAGE = "image"
        const val CONTENT_PAGE = "page"
        const val TOGGLE_DESCRIPTION = "toggle-shared-bounds"
        const val COMPOSE_BUSY = "COMPOSE-BUSY"
        const val COMPOSE_IDLE = "COMPOSE-IDLE"

        const val SHARED_KEY = "card"
        const val TRANSITION_MS = 1000
        val Bounds = BoundsTransform { _, _ -> tween(TRANSITION_MS) }
    }
}

/** `sharedBounds` with a crossfade, in the default `scaleToBounds` resize mode. */
@OptIn(ExperimentalSharedTransitionApi::class)
private fun Modifier.sharedBoundsFade(
    sharedScope: SharedTransitionScope,
    state: SharedTransitionScope.SharedContentState,
    visibilityScope: AnimatedVisibilityScope,
): Modifier =
    with(sharedScope) {
        this@sharedBoundsFade.sharedBounds(
            sharedContentState = state,
            animatedVisibilityScope = visibilityScope,
            enter = fadeIn(tween(SharedBoundsFadeActivity.TRANSITION_MS)),
            exit = fadeOut(tween(SharedBoundsFadeActivity.TRANSITION_MS)),
            boundsTransform = SharedBoundsFadeActivity.Bounds,
        )
    }

private val Primary = Color(0xFF6750A4)
private val PhotoBrush =
    Brush.linearGradient(listOf(Color(0xFFFFC107), Color(0xFFE91E63), Color(0xFF3F51B5)))
private const val LoremParagraph =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt " +
        "ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation " +
        "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in " +
        "reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
private val Lorem = List(3) { LoremParagraph }.joinToString(" ")

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ListScreen(
    content: String,
    sharedScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
) {
    val shared =
        Modifier.sharedBoundsFade(
            sharedScope,
            sharedScope.rememberSharedContentState(SharedBoundsFadeActivity.SHARED_KEY),
            visibilityScope,
        )
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Trips", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(12.dp))
        repeat(6) { i ->
            if (i == 2) {
                if (content == SharedBoundsFadeActivity.CONTENT_IMAGE) {
                    Box(shared.size(120.dp).background(PhotoBrush))
                } else {
                    ListCard(shared)
                }
            } else {
                PlainRow()
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DetailScreen(
    content: String,
    sharedScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
) {
    val shared =
        Modifier.sharedBoundsFade(
            sharedScope,
            sharedScope.rememberSharedContentState(SharedBoundsFadeActivity.SHARED_KEY),
            visibilityScope,
        )
    if (content == SharedBoundsFadeActivity.CONTENT_IMAGE) {
        Box(shared.fillMaxWidth().height(320.dp).background(PhotoBrush))
    } else {
        DetailPage(shared.fillMaxSize())
    }
}

@Composable
private fun PlainRow() {
    Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(Color(0xFF9E9E9E), CircleShape))
        Spacer(Modifier.width(12.dp))
        Column {
            Box(Modifier.fillMaxWidth(0.7f).height(12.dp).background(Color(0xFF616161)))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.5f).height(10.dp).background(Color(0xFFBDBDBD)))
        }
    }
}

/** Outgoing shared bounds: a list card with thumbnail, title, subtitle, and a chip. */
@Composable
private fun ListCard(modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(Color.White).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(PhotoBrush))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(
                "Fushimi Inari",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
            Text("Kyoto, Japan", fontSize = 13.sp, color = Color.DarkGray)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.background(Primary, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text("Saved", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

/** Incoming shared bounds: a detail page. Opaque content over other content throughout. */
@Composable
private fun DetailPage(modifier: Modifier) {
    val barHeight = 56.dp
    Box(modifier.background(Color.White)) {
        Column {
            Box(Modifier.fillMaxWidth().height(260.dp).background(PhotoBrush)) {
                Box(
                    Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
                Text(
                    "Fushimi Inari",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                )
            }
            Text(
                List(4) { Lorem }.joinToString(" "),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 48.dp),
            )
        }
        // Avatar overlapping the hero edge.
        Box(
            Modifier.align(Alignment.TopStart)
                .offset(x = 16.dp, y = 228.dp)
                .size(64.dp)
                .background(Color.White, CircleShape)
                .padding(4.dp)
                .background(Color(0xFF00897B), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("DL", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        // Filled button.
        Box(
            Modifier.align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp + barHeight)
                .background(Primary, RoundedCornerShape(50))
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Book", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        // FAB over the body text.
        Box(
            Modifier.align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = -(16.dp + barHeight))
                .size(56.dp)
                .background(Primary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = Color.White, fontSize = 28.sp)
        }
        // Opaque bottom bar over the body text.
        Row(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(barHeight)
                .background(Color(0xFFF3EDF7)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Home", "Saved", "Trips").forEachIndexed { i, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(width = 40.dp, height = 18.dp)
                            .background(
                                if (i == 2) Primary else Color.Transparent,
                                RoundedCornerShape(50),
                            )
                    )
                    Text(label, fontSize = 12.sp, color = Color(0xFF49454F))
                }
            }
        }
    }
}
