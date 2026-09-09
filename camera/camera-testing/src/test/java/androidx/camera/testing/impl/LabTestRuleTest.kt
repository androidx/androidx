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

package androidx.camera.testing.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class LabTestRuleTest {

    private val labTestRule = LabTestRule()

    private class SampleTestClass {
        @LabTestRule.LabTestFrontCamera fun sampleFrontCameraTest() {}

        @LabTestRule.LabTestRearCamera fun sampleRearCameraTest() {}

        @LabTestRule.LabTestOnly fun sampleLabTestOnly() {}

        fun unannotatedTest() {}
    }

    private class TestStatement : Statement() {
        var evaluated = false

        override fun evaluate() {
            evaluated = true
        }
    }

    @Test
    fun apply_withFrontCameraAnnotation_returnsFrontCameraStatement() {
        val baseStatement = TestStatement()
        val description =
            Description.createTestDescription(
                SampleTestClass::class.java,
                "sampleFrontCameraTest",
                *SampleTestClass::class.java.getMethod("sampleFrontCameraTest").annotations,
            )

        val appliedStatement = labTestRule.apply(baseStatement, description)
        assertThat(appliedStatement)
            .isInstanceOf(LabTestRule.LabTestFrontCameraStatement::class.java)
    }

    @Test
    fun apply_withRearCameraAnnotation_returnsRearCameraStatement() {
        val baseStatement = TestStatement()
        val description =
            Description.createTestDescription(
                SampleTestClass::class.java,
                "sampleRearCameraTest",
                *SampleTestClass::class.java.getMethod("sampleRearCameraTest").annotations,
            )

        val appliedStatement = labTestRule.apply(baseStatement, description)
        assertThat(appliedStatement)
            .isInstanceOf(LabTestRule.LabTestRearCameraStatement::class.java)
    }

    @Test
    fun apply_withLabTestOnlyAnnotation_returnsLabTestStatement() {
        val baseStatement = TestStatement()
        val description =
            Description.createTestDescription(
                SampleTestClass::class.java,
                "sampleLabTestOnly",
                *SampleTestClass::class.java.getMethod("sampleLabTestOnly").annotations,
            )

        val appliedStatement = labTestRule.apply(baseStatement, description)
        assertThat(appliedStatement).isInstanceOf(LabTestRule.LabTestStatement::class.java)
    }

    @Test
    fun apply_unannotatedTest_returnsBaseStatement() {
        val baseStatement = TestStatement()
        val description =
            Description.createTestDescription(
                SampleTestClass::class.java,
                "unannotatedTest",
                *SampleTestClass::class.java.getMethod("unannotatedTest").annotations,
            )

        val appliedStatement = labTestRule.apply(baseStatement, description)
        assertThat(appliedStatement).isSameInstanceAs(baseStatement)
    }
}
