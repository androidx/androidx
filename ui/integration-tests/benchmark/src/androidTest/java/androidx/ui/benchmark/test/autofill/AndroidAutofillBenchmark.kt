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

package androidx.ui.benchmark.test.autofill

import android.app.Activity
import android.graphics.Rect
import android.util.SparseArray
import android.view.autofill.AutofillValue
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.autofill.AutofillNode
import androidx.test.filters.SdkSuppress
import androidx.ui.autofill.AutofillType
import androidx.ui.core.AndroidComposeView
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class AndroidAutofillBenchmark {

    @get:Rule
    val activityRule = ActivityTestRule(Activity::class.java)

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var composeView: AndroidComposeView

    @Before
    fun setup() {
        composeView = AndroidComposeView(activityRule.activity)
    }

    @Test
    @UiThreadTest
    @SdkSuppress(minSdkVersion = 26)
    fun provideAutofillVirtualStructure_performAutofill() {

        // Arrange.
        val autofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.Name),
            boundingBox = Rect(0, 0, 0, 0)
        )
        val autofillValues = SparseArray<AutofillValue>().apply {
            append(autofillNode.id, AutofillValue.forText("Name"))
        }
        composeView.autofillTree += autofillNode

        // Assess.
        benchmarkRule.measureRepeated {
            composeView.autofill(autofillValues)
        }
    }
}