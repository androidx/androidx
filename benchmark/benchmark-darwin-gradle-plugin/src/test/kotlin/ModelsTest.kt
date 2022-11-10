/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.benchmark.darwin.gradle.xcode.ActionTestPlanRunSummaries
import androidx.benchmark.darwin.gradle.xcode.ActionTestSummary
import androidx.benchmark.darwin.gradle.xcode.ActionsInvocationRecord
import androidx.benchmark.darwin.gradle.xcode.GsonHelpers
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ModelsTest {
    @Test
    fun parseXcResultOutputs() {
        val json = testData(XCRESULT_OUTPUT_JSON).readText()
        val gson = GsonHelpers.gson()
        val record = gson.fromJson(json, ActionsInvocationRecord::class.java)
        assertThat(record.actions.testReferences().size).isEqualTo(1)
        assertThat(record.metrics.size()).isEqualTo(1)
        assertThat(record.actions.isSuccessful()).isTrue()
    }

    @Test
    fun parseTestsReferenceOutput() {
        val json = testData(XC_TESTS_REFERENCE_OUTPUT_JSON).readText()
        val gson = GsonHelpers.gson()
        val testPlanSummaries = gson.fromJson(json, ActionTestPlanRunSummaries::class.java)
        val testSummaryMetas = testPlanSummaries.testSummaries()
        assertThat(testSummaryMetas.size).isEqualTo(1)
        assertThat(testSummaryMetas[0].summaryRefId()).isNotEmpty()
        assertThat(testSummaryMetas[0].isSuccessful()).isTrue()
    }

    @Test
    fun parseTestOutput() {
        val json = testData(XC_TEST_OUTPUT_JSON).readText()
        val gson = GsonHelpers.gson()
        val testSummary = gson.fromJson(json, ActionTestSummary::class.java)
        assertThat(testSummary.title()).isNotEmpty()
        assertThat(testSummary.isSuccessful()).isTrue()
    }

    companion object {
        private const val XCRESULT_OUTPUT_JSON = "xcresult_output.json"
        private const val XC_TESTS_REFERENCE_OUTPUT_JSON = "tests_reference_output.json"
        private const val XC_TEST_OUTPUT_JSON = "test_output.json"
    }
}
