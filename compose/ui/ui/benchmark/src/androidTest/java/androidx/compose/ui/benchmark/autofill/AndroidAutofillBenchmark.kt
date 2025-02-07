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

package androidx.compose.ui.benchmark.autofill

import android.util.SparseArray
import android.view.View
import android.view.autofill.AutofillValue
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeatedOnMainThread
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidAutofillBenchmark {

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val benchmarkRule = BenchmarkRule()

    private lateinit var autofillTree:
        @Suppress("Deprecation")
        androidx.compose.ui.autofill.AutofillTree
    private lateinit var composeView: View

    @Before
    fun setup() {
        composeTestRule.setContent {
            autofillTree = @Suppress("Deprecation") LocalAutofillTree.current
            composeView = LocalView.current
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun provideAutofillVirtualStructure_performAutofill() {
        val autofillValues =
            composeTestRule.runOnUiThread {
                // Arrange.
                val autofillNode =
                    @Suppress("Deprecation")
                    androidx.compose.ui.autofill.AutofillNode(
                        onFill = {},
                        autofillTypes =
                            listOf(androidx.compose.ui.autofill.AutofillType.PersonFullName),
                        boundingBox = Rect(0f, 0f, 0f, 0f)
                    )

                autofillTree += autofillNode

                SparseArray<AutofillValue>().apply {
                    append(autofillNode.id, AutofillValue.forText("Name"))
                }
            }

        // Assess.
        benchmarkRule.measureRepeatedOnMainThread { composeView.autofill(autofillValues) }
    }
}
