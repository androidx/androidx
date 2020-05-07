/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.tooling

import android.os.Handler
import androidx.compose.Composable
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Modifier
import androidx.ui.core.onPositioned
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.layout.fillMaxSize
import org.junit.Before
import org.junit.Rule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class ToolingTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    lateinit var activity: TestActivity
    lateinit var handler: Handler
    lateinit var positionedLatch: CountDownLatch

    @Before
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)

        activityTestRule.onUiThread { handler = Handler() }
    }

    internal fun show(composable: @Composable () -> Unit) {
        positionedLatch = CountDownLatch(1)
        activityTestRule.onUiThread {
            activity.setContent {
                Box(
                    Modifier.onPositioned { positionedLatch.countDown() }.fillMaxSize(),
                    children = composable
                )
            }
        }

        // Wait for the layout to be performed
        positionedLatch.await(1, TimeUnit.SECONDS)

        // Wait for the UI thread to complete its current work so we know that layout is done.
        activityTestRule.onUiThread { }
    }
}

// Kotlin IR compiler doesn't seem too happy with auto-conversion from
// lambda to Runnable, so separate it here
fun ActivityTestRule<TestActivity>.onUiThread(block: () -> Unit) {
    val runnable: Runnable = object : Runnable {
        override fun run() {
            block()
        }
    }
    runOnUiThread(runnable)
}