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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ExperimentalWearComposeMaterial3Api
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.PullToRefreshBox
import androidx.wear.compose.material3.PullToRefreshDefaults
import androidx.wear.compose.material3.PullToRefreshState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalWearComposeMaterial3Api::class)
@Sampled
@Composable
@Preview
fun PullToRefreshSample() {
    var itemCount by remember { mutableIntStateOf(10) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val state = remember { PullToRefreshState() }

    AppScaffold {
        ScreenScaffold(scrollState = listState, timeText = { TimeText() }) { contentPadding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch {
                        delay(2000)
                        itemCount += 5
                        isRefreshing = false
                    }
                },
                state = state,
                modifier = Modifier.fillMaxSize(),
            ) {
                TransformingLazyColumn(
                    state = listState,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(itemCount) { index ->
                        Button(
                            onClick = {},
                            label = { Text("Item ${index + 1}") },
                            transformation = SurfaceTransformation(transformationSpec),
                            modifier =
                                Modifier.transformedHeight(this, transformationSpec)
                                    .minimumVerticalContentPadding(
                                        ButtonDefaults.minimumVerticalListContentPadding
                                    )
                                    .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalWearComposeMaterial3Api::class)
@Sampled
@Composable
@Preview
fun PullToRefreshCustomIndicatorSample() {
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val state = remember { PullToRefreshState() }
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    AppScaffold {
        ScreenScaffold(scrollState = listState, timeText = { TimeText() }) { contentPadding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch {
                        delay(2000)
                        isRefreshing = false
                    }
                },
                state = state,
                indicator = {
                    PullToRefreshDefaults.IndicatorBox(
                        state = state,
                        isRefreshing = isRefreshing,
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                colors =
                                    ProgressIndicatorDefaults.colors(
                                        indicatorColor =
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            val rotation = state.distanceFraction * 180f
                            val scale = state.distanceFraction.coerceIn(0f, 1f)
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Pull to refresh",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier =
                                    Modifier.size(24.dp).graphicsLayer {
                                        rotationZ = rotation
                                        scaleX = scale
                                        scaleY = scale
                                    },
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                TransformingLazyColumn(
                    state = listState,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(10) { index ->
                        Button(
                            onClick = {},
                            label = { Text("Custom item ${index + 1}") },
                            transformation = SurfaceTransformation(transformationSpec),
                            modifier =
                                Modifier.transformedHeight(this, transformationSpec)
                                    .minimumVerticalContentPadding(
                                        ButtonDefaults.minimumVerticalListContentPadding
                                    )
                                    .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
