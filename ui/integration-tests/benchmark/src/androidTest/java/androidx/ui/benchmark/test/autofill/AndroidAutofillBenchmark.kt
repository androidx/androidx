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

import android.graphics.Rect
import android.util.SparseArray
import android.view.ViewGroup
import android.view.autofill.AutofillValue
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.LargeTest
import androidx.ui.autofill.AutofillNode
import androidx.test.filters.SdkSuppress
import androidx.ui.autofill.AutofillType
import androidx.ui.core.Owner
import androidx.ui.core.OwnerAmbient
import androidx.ui.test.createComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class AndroidAutofillBenchmark {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var owner: Owner
    private lateinit var composeView: ViewGroup

    @Before
    fun setup() {
        composeTestRule.setContent {
            @Suppress("DEPRECATION") // Owner Ambient will be removed by b/139866476.
            owner = OwnerAmbient.current
            composeView = owner as ViewGroup
        }
    }

    @Test
    @UiThreadTest
    @SdkSuppress(minSdkVersion = 26)
    fun provideAutofillVirtualStructure_performAutofill() {

        // Arrange.
        val autofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.PersonFullName),
            boundingBox = Rect(0, 0, 0, 0)
        )
        val autofillValues = SparseArray<AutofillValue>().apply {
            append(autofillNode.id, AutofillValue.forText("Name"))
        }
        owner.autofillTree += autofillNode

        // Assess.
        benchmarkRule.measureRepeated {
            composeView.autofill(autofillValues)
        }
    }
}