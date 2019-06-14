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
package androidx.ui.core.test

import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.CraneWrapper
import androidx.ui.framework.test.TestActivity
import androidx.compose.Recompose
import androidx.compose.composer
import androidx.compose.onActive
import androidx.compose.onCommit
import androidx.compose.setContent
import androidx.compose.unaryPlus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class WrapperTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    private lateinit var activity: TestActivity

    @Before
    fun setup() {
        activity = activityTestRule.activity
    }

    @Test
    fun ensureCraneWrapperDoesntPropagateInvalidations() {
        val commitLatch = CountDownLatch(2)
        var rootCount = 0
        var craneWrapperCount = 0
        var innerCount = 0

        runOnUiThread {
            activity.setContent {
                +onCommit { rootCount++ }
                CraneWrapper {
                    +onCommit { craneWrapperCount++ }
                    Recompose { recompose ->
                        +onCommit {
                            innerCount++
                            commitLatch.countDown()
                        }
                        +onActive { recompose() }
                    }
                }
            }
        }
        assertTrue(commitLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, rootCount)
        assertEquals(1, craneWrapperCount)
        assertEquals(2, innerCount)
    }

    // We only need this because IR compiler doesn't like converting lambdas to Runnables
    private fun runOnUiThread(block: () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                block()
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }
}