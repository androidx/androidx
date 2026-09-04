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

package androidx.wear.compose.prototypes.windowinsets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

private val menuScreens =
    listOf(
        Screen.Recents,
        Screen.GlobalStatusBarSandbox,
        Screen.HorizontalPager,
        Screen.VerticalPager,
        Screen.SelfRenderedSandbox,
    )

@Composable
fun MenuScreen(onNavigateTo: (Screen) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    TransformingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader(
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
            ) {
                Text(
                    text = "Wear Windowinsets Prototypes",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(menuScreens) { screen ->
            Button(
                onClick = { onNavigateTo(screen) },
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                label = { Text(screen.title) },
            )
        }
    }
}

/**
 * Placeholder screen for the Global Status Bar Sandbox demo.
 *
 * TODO: Add Global Status Bar demo content and transition tests later.
 */
@Composable
fun GlobalStatusBarSandboxScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Placeholder Screen")
            Button(onClick = onBack, label = { Text("Back") })
        }
    }
}

/**
 * Placeholder screen for the Horizontal Pager demo.
 *
 * TODO: Add horizontal pager demo content and transitions later.
 */
@Composable
fun HorizontalPagerScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Placeholder Screen")
            Button(onClick = onBack, label = { Text("Back") })
        }
    }
}

/**
 * Placeholder screen for the Vertical Pager demo.
 *
 * TODO: Add vertical pager demo content and transitions later.
 */
@Composable
fun VerticalPagerScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Placeholder Screen")
            Button(onClick = onBack, label = { Text("Back") })
        }
    }
}

/**
 * Placeholder screen for the Self-Rendered Sandbox demo.
 *
 * TODO: Add self-rendered status bar demo content later.
 */
@Composable
fun SelfRenderedSandboxScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Placeholder Screen")
            Button(onClick = onBack, label = { Text("Back") })
        }
    }
}

@Composable
fun RecentsScreen(recents: List<Screen>, onNavigateTo: (Screen) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    TransformingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader(
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
            ) {
                Text(
                    text = "Recents",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (recents.isEmpty()) {
            item {
                Text(
                    text = "No recent screens",
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.fillMaxWidth()
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            )
                            .transformedHeight(this, transformationSpec),
                )
            }
        }
        items(recents) { screen ->
            Button(
                onClick = { onNavigateTo(screen) },
                modifier =
                    Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
                        .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                label = { Text(screen.title) },
            )
        }
    }
}
