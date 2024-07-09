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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun LoadingIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { LoadingIndicator() }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ContainedLoadingIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { ContainedLoadingIndicator() }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun DeterminateLoadingIndicatorSample() {
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by
        animateFloatAsState(
            targetValue = progress,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                    visibilityThreshold = 1 / 1000f
                )
        )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LoadingIndicator(progress = { animatedProgress })
        Spacer(Modifier.requiredHeight(30.dp))
        Text("Set loading progress:")
        Slider(
            modifier = Modifier.width(300.dp),
            value = progress,
            valueRange = 0f..1f,
            onValueChange = { progress = it },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun DeterminateContainedLoadingIndicatorSample() {
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by
        animateFloatAsState(
            targetValue = progress,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                    visibilityThreshold = 1 / 1000f
                )
        )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ContainedLoadingIndicator(progress = { animatedProgress })
        Spacer(Modifier.requiredHeight(30.dp))
        Text("Set loading progress:")
        Slider(
            modifier = Modifier.width(300.dp),
            value = progress,
            valueRange = 0f..1f,
            onValueChange = { progress = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun LoadingIndicatorPullToRefreshSample() {
    var itemCount by remember { mutableStateOf(15) }
    var isRefreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            // fetch something
            delay(5000)
            itemCount += 5
            isRefreshing = false
        }
    }

    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(state.distanceFraction).coerceIn(0f, 1f)
    }

    Scaffold(
        modifier =
            Modifier.pullToRefresh(
                state = state,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            ),
        topBar = {
            TopAppBar(
                title = { Text("TopAppBar") },
                // Provide an accessible alternative to trigger refresh.
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, "Trigger Refresh")
                    }
                }
            )
        }
    ) {
        Box(Modifier.padding(it)) {
            LazyColumn(Modifier.fillMaxSize()) {
                if (!isRefreshing) {
                    items(itemCount) { ListItem({ Text(text = "Item ${itemCount - it}") }) }
                }
            }
            Box(
                Modifier.align(Alignment.TopCenter).graphicsLayer {
                    scaleX = scaleFraction()
                    scaleY = scaleFraction()
                }
            ) {
                PullToRefreshDefaults.LoadingIndicator(state = state, isRefreshing = isRefreshing)
            }
        }
    }
}
