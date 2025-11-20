/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark.common

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.HorizontalPageIndicator

object HorizontalPageIndicatorBenchmark : PagerBenchmark() {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val pagerState = rememberPagerState(pageCount = { 10 })
            HorizontalPager(
                modifier =
                    Modifier.fillMaxWidth().semantics { contentDescription = CONTENT_DESCRIPTION },
                state = pagerState,
            ) { page ->
                Spacer(modifier = Modifier.fillMaxSize())
            }
            HorizontalPageIndicator(pagerState, modifier = Modifier.align(Alignment.BottomCenter))
        }
}
