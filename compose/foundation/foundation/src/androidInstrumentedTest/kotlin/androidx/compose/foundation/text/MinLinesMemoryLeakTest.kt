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

package androidx.compose.foundation.text

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.LargeTest
import leakcanary.DetectLeaksAfterTestSuccess
import leakcanary.LeakCanary
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import shark.AndroidReferenceMatchers

@RunWith(Parameterized::class)
@LargeTest
class MinLinesMemoryLeakTest(private val numLines: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = arrayOf(1, 2, 100) // potential when numLines greater than 1

        // Config is a global static instance, so make sure to reset it after this test to prevent
        // issues for other tests that use leak canary
        private lateinit var savedLeakCanaryConfig: LeakCanary.Config

        private val IgnoreFrameTrackerLeaks =
            listOf(
                AndroidReferenceMatchers.instanceFieldLeak(
                    className = "com.android.internal.jank.FrameTracker",
                    fieldName = "mConfig",
                    "Ignoring a leak due to misconfigured framework jank tracking b/349355283",
                ),
                AndroidReferenceMatchers.instanceFieldLeak(
                    className = "com.android.internal.jank.FrameTracker",
                    fieldName = "mListener",
                    "Ignoring a leak due to misconfigured framework jank tracking b/349355283",
                ),
            )

        @JvmStatic
        @BeforeClass
        fun configureLeakCanaryReporting() {
            val current = LeakCanary.config
            savedLeakCanaryConfig = current
            LeakCanary.config =
                current.copy(
                    referenceMatchers = current.referenceMatchers + IgnoreFrameTrackerLeaks
                )
        }

        @JvmStatic
        @AfterClass
        fun cleanupLeakCanaryReporting() {
            LeakCanary.config = savedLeakCanaryConfig
        }
    }

    private val composeTestRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(composeTestRule)

    @Test
    fun MinLinesMemoryLeakTest() {
        composeTestRule.setContent {
            BasicText(
                text = "Lorem ipsum dolor sit amet.",
                minLines = numLines, // Set this to a non-default value (potential leak)
            )
        }
        composeTestRule.waitForIdle()
    }
}
