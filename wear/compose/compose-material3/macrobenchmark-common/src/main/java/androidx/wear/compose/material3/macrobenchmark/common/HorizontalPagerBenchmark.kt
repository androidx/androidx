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

package androidx.wear.compose.material3.macrobenchmark.common

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

abstract class PagerBenchmark : MacrobenchmarkScreen {
    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            val horizontalPager = device.findObject(By.desc(CONTENT_DESCRIPTION))
            if (horizontalPager != null) {
                horizontalPager.setGestureMargin(device.displayWidth / 5)
                repeat(2) {
                    horizontalPager.swipe(Direction.LEFT, 1f, 1000)
                    device.waitForIdle()
                    SystemClock.sleep(500)
                }
                repeat(2) {
                    horizontalPager.swipe(Direction.RIGHT, 1f, 1000)
                    device.waitForIdle()
                    SystemClock.sleep(500)
                }
            }
        }

    val pager: @Composable (BoxScope.(pagerState: PagerState) -> Unit)
        get() = { pagerState ->
            HorizontalPager(
                modifier =
                    Modifier.fillMaxWidth().semantics { contentDescription = CONTENT_DESCRIPTION },
                state = pagerState,
                flingBehavior =
                    PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
            ) { page ->
                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                    ScreenScaffold {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Page $page")
                        }
                    }
                }
            }
        }
}

object HorizontalPagerBenchmark : PagerBenchmark() {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            AppScaffold {
                val pagerState = rememberPagerState(pageCount = { 10 })
                pager(pagerState)
            }
        }
}
