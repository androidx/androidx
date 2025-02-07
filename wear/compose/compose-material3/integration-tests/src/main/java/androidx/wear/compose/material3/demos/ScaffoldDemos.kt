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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.VerticalPagerScaffold
import androidx.wear.compose.material3.samples.HorizontalPagerScaffoldSample
import androidx.wear.compose.material3.samples.ScaffoldSample
import androidx.wear.compose.material3.samples.ScaffoldWithEdgeButtonSample
import androidx.wear.compose.material3.samples.VerticalPagerScaffoldSample

val ScaffoldDemos =
    listOf(
        ComposableDemo("Scaffold Sample") { ScaffoldSample() },
        ComposableDemo("Screen Scaffold") { ScaffoldWithEdgeButtonSample() },
        ComposableDemo("Horizontal Pager Scaffold") { HorizontalPagerScaffoldSample() },
        ComposableDemo("Horizontal Pager Scaffold (Fade Out Indicator)") {
            HorizontalPagerScaffoldFadeOutIndicatorDemo()
        },
        ComposableDemo("Vertical Pager Scaffold") { VerticalPagerScaffoldSample() },
        ComposableDemo("Vertical Pager Scaffold (Fade Out Indicator)") {
            VerticalPagerScaffoldFadeOutIndicatorDemo()
        },
    )

@Composable
fun RandomComponent(page: Int) {
    when (page % 3) {
        0 -> Button(onClick = {}) { Text("Button") }
        1 ->
            RadioButton(
                label = {
                    Text(
                        "Radio Button",
                    )
                },
                selected = true,
                onSelect = {},
                enabled = true,
            )
        2 ->
            DefaultSlider(
                value = 5f,
                enabled = true,
                valueRange = 1f..10f,
                steps = 10,
                onValueChange = {}
            )
    }
}

@Composable
fun HorizontalPagerScaffoldFadeOutIndicatorDemo() {
    AppScaffold {
        val pagerState = rememberPagerState(pageCount = { 10 })

        HorizontalPagerScaffold(
            pagerState = pagerState,
            pageIndicatorAnimationSpec = PagerScaffoldDefaults.FadeOutAnimation
        ) { page ->
            ScreenScaffold {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Page $page")
                    Spacer(modifier = Modifier.height(16.dp))
                    RandomComponent(page)
                }
            }
        }
    }
}

@Composable
fun VerticalPagerScaffoldFadeOutIndicatorDemo() {
    AppScaffold {
        val pagerState = rememberPagerState(pageCount = { 10 })

        VerticalPagerScaffold(
            pagerState = pagerState,
            pageIndicatorAnimationSpec = PagerScaffoldDefaults.FadeOutAnimation
        ) { page ->
            ScreenScaffold {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Page $page")
                    Spacer(modifier = Modifier.height(16.dp))
                    RandomComponent(page)
                }
            }
        }
    }
}
