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

package androidx.compose.remote.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Generates baseline profiles for Remote Compose core and player libraries across feed, animation,
 * state-update, and startup paths.
 */
@LargeTest
@RunWith(JUnit4::class)
class RemoteComposeBaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = PACKAGE_NAME,
            includeInStartupProfile = true,
            profileBlock = {
                // Embedded feed fling (mixed templates)
                startActivityAndWait(
                    Intent(FEED_ACTIVITY).apply {
                        putExtra("player", "embedded")
                        putExtra("templates", "mixed")
                        putExtra("count", 100)
                    }
                )
                val feed = device.wait(Until.findObject(By.desc(FEED_CONTENT_DESCRIPTION)), 10_000)
                if (feed != null) {
                    feed.setGestureMargin(device.displayWidth / 5)
                    repeat(3) {
                        feed.fling(Direction.DOWN)
                        device.waitForIdle()
                    }
                }

                // View feed fling (mixed templates)
                startActivityAndWait(
                    Intent(FEED_ACTIVITY).apply {
                        putExtra("player", "view")
                        putExtra("templates", "mixed")
                        putExtra("count", 100)
                    }
                )
                val viewFeed =
                    device.wait(Until.findObject(By.desc(FEED_CONTENT_DESCRIPTION)), 10_000)
                if (viewFeed != null) {
                    viewFeed.setGestureMargin(device.displayWidth / 5)
                    repeat(2) {
                        viewFeed.fling(Direction.DOWN)
                        device.waitForIdle()
                    }
                }

                // State updates & state layout
                startActivityAndWait(
                    Intent(STATE_BENCH_ACTIVITY).apply {
                        putExtra("player", "embedded")
                        putExtra("mode", "s2a")
                        putExtra("intervalMs", 100L)
                    }
                )
                Thread.sleep(500L)
            },
        )
    }
}
