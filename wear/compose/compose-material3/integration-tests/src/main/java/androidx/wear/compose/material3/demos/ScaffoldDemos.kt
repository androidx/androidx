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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.VerticalPagerScaffold

@Composable
fun HorizontalPagerScaffoldFadeOutIndicatorDemo() {
    AppScaffold {
        val pagerState = rememberPagerState(pageCount = { 10 })

        HorizontalPagerScaffold(
            pagerState = pagerState,
            pageIndicatorAnimationSpec = PagerScaffoldDefaults.FadeOutAnimation
        ) { page ->
            ScreenScaffold {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Page $page")
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Page $page")
                }
            }
        }
    }
}
