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

package androidx.ui.test

import android.app.Activity
import androidx.compose.composer
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Ensure correctness of [RectanglesInColumnTestCase].
 */
@MediumTest
@RunWith(Parameterized::class)
class ColoredRectTest(private val numberOfRectangles: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun initParameters(): Array<Any> = arrayOf(1, 10)
    }

    @get:Rule
    val activityRule = ActivityTestRule(Activity::class.java)

    @get:Rule
    val disableAnimationRule = DisableTransitions()

    @Test
    fun toggleRectangleColor_compose() {
        activityRule.runOnUiThread(object : Runnable {
            override fun run() {
                val testCase = RectanglesInColumnTestCase(activityRule.activity, numberOfRectangles)
                    .apply { runSetup() }

                testCase.compositionContext.recomposeSyncAssertNoChanges()

                // Change state
                testCase.toggleState()

                // Recompose our changes
                testCase.compositionContext.recomposeSyncAssertHadChanges()

                // No other compositions should be pending
                testCase.compositionContext.recomposeSyncAssertNoChanges()
            }
        })
    }

}
