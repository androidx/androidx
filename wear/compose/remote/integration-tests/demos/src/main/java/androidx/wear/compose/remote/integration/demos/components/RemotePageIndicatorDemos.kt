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

@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.integration.demos.components

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteInt
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.remote.material3.RemoteHorizontalPageIndicator
import androidx.wear.compose.remote.material3.RemoteVerticalPageIndicator
import androidx.wear.compose.remote.material3.rememberRemotePageIndicatorState

@Composable
fun RemoteHorizontalPageIndicator3Demo(modifier: Modifier = Modifier) {
    RemoteHorizontalPageIndicatorDemoHelper(pageCount = 3, modifier = modifier)
}

@Composable
fun RemoteHorizontalPageIndicator10Demo(modifier: Modifier = Modifier) {
    RemoteHorizontalPageIndicatorDemoHelper(pageCount = 10, modifier = modifier)
}

@Composable
fun RemoteVerticalPageIndicator3Demo(modifier: Modifier = Modifier) {
    RemoteVerticalPageIndicatorDemoHelper(pageCount = 3, modifier = modifier)
}

@Composable
fun RemoteVerticalPageIndicator10Demo(modifier: Modifier = Modifier) {
    RemoteVerticalPageIndicatorDemoHelper(pageCount = 10, modifier = modifier)
}

@Composable
private fun RemoteHorizontalPageIndicatorDemoHelper(pageCount: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var document by remember { mutableStateOf<RemoteDocument?>(null) }

    LaunchedEffect(context, pageCount) {
        val captured =
            captureSingleRemoteDocument(context) {
                val selectedPage = rememberNamedRemoteInt("selectedPage", 0)
                val pageOffset = rememberNamedRemoteFloat("pageOffset") { 0f.rf }
                val state =
                    rememberRemotePageIndicatorState(
                        selectedPage = selectedPage,
                        pageOffset = pageOffset,
                        pageCount = pageCount,
                    )
                RemoteHorizontalPageIndicator(state = state)
            }
        document = RemoteDocument(captured.bytes)
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(
                            when (page % 3) {
                                0 -> Color(0xFF2C2C2C)
                                1 -> Color(0xFF3C3C3C)
                                else -> Color(0xFF4C4C4C)
                            }
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Page $page", style = MaterialTheme.typography.titleLarge)
            }
        }

        if (document != null) {
            AndroidView(
                factory = { ctx ->
                    object : RemoteComposePlayer(ctx) {
                            override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
                                return false
                            }
                        }
                        .apply { setDocument(document) }
                },
                modifier = Modifier.fillMaxSize(),
                update = { player ->
                    player.setUserLocalInt("selectedPage", pagerState.currentPage)
                    player.setUserLocalFloat("pageOffset", pagerState.currentPageOffsetFraction)
                    player.invalidate()
                },
            )
        }
    }
}

@Composable
private fun RemoteVerticalPageIndicatorDemoHelper(pageCount: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var document by remember { mutableStateOf<RemoteDocument?>(null) }

    LaunchedEffect(context, pageCount) {
        val captured =
            captureSingleRemoteDocument(context) {
                val selectedPage = rememberNamedRemoteInt("selectedPage", 0)
                val pageOffset = rememberNamedRemoteFloat("pageOffset") { 0f.rf }
                val state =
                    rememberRemotePageIndicatorState(
                        selectedPage = selectedPage,
                        pageOffset = pageOffset,
                        pageCount = pageCount,
                    )
                RemoteVerticalPageIndicator(state = state)
            }
        document = RemoteDocument(captured.bytes)
    }

    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(
                            when (page % 3) {
                                0 -> Color(0xFF2C2C2C)
                                1 -> Color(0xFF3C3C3C)
                                else -> Color(0xFF4C4C4C)
                            }
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Page $page", style = MaterialTheme.typography.titleLarge)
            }
        }

        if (document != null) {
            AndroidView(
                factory = { ctx ->
                    object : RemoteComposePlayer(ctx) {
                            override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
                                return false
                            }
                        }
                        .apply { setDocument(document) }
                },
                modifier = Modifier.fillMaxSize(),
                update = { player ->
                    player.setUserLocalInt("selectedPage", pagerState.currentPage)
                    player.setUserLocalFloat("pageOffset", pagerState.currentPageOffsetFraction)
                    player.invalidate()
                },
            )
        }
    }
}
