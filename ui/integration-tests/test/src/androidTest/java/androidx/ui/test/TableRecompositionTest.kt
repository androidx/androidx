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
import androidx.compose.FrameManager
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.test.cases.TableRecompositionTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Ensure correctness of [TableRecompositionTestCase].
 */
@MediumTest
@RunWith(Parameterized::class)
class TableRecompositionTest(private val numberOfCells: Int) {

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
    fun testTable_recomposition() {
        activityRule.runOnUiThreadSync {
            val testCase = TableRecompositionTestCase(activityRule.activity, numberOfCells)
            testCase.runToFirstDraw()

            testCase.assertMeasureSizeIsPositive()

            FrameManager.nextFrame()
            testCase.recomposeSyncAssertNoChanges()

            testCase.measure()
            testCase.layout()
            testCase.drawSlow()
            FrameManager.nextFrame()
            testCase.recomposeSyncAssertNoChanges()
        }
    }
}
