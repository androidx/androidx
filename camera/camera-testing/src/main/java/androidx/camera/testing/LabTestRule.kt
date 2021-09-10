/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.testing

import android.util.Log
import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] which can be used to limit the test only run on CameraX lab environment. It
 * throws the AssumptionViolatedException to ignore the test if the test environment is not in the
 * lab. Useful for the tests not needed to run on the PostSubmit.
 *
 * To use this [TestRule], do the following. <br></br><br></br>
 *
 * Add the Rule to your JUnit test. <br></br><br></br>
 * `LabTestRule mLabTestRule = new LabTestRule();
` *
 * <br></br><br></br>
 *
 * Add only one of [LabTestOnly], [LabTestFrontCamera] or, [LabTestRearCamera] annotation to your
 * test case.
 * <br></br><br></br>
 * `public void yourTestCase() {
 *
 * }
` *
 * <br></br><br></br>
 */
class LabTestRule : TestRule {

    /**
     * The annotation for tests that only want to run on the CameraX lab environment
     */
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class LabTestOnly()

    /**
     * The annotation for tests that only want to run on the CameraX lab environment with
     * enabling front camera.
     */
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class LabTestFrontCamera()

    /**
     * The annotation for tests that only want to run on the CameraX lab environment with
     * enabling rear camera.
     */
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class LabTestRearCamera()

    class LabTestStatement(private val statement: Statement) :
        Statement() {

        @Throws(Throwable::class)
        override fun evaluate() {
            // Only test in CameraX lab environment and throw AssumptionViolatedException if not
            // in the lab environment. The loggable tag will be set when running the CameraX
            // daily testing.
            assumeTrue(Log.isLoggable("MH", Log.DEBUG))
            statement.evaluate()
        }
    }

    class LabTestFrontCameraStatement(private val statement: Statement) :
        Statement() {

        @Throws(Throwable::class)
        override fun evaluate() {
            // Only test in CameraX lab environment and the loggable tag will be set when running
            // the CameraX e2e test with enabling front camera.
            assumeTrue(Log.isLoggable("frontCameraE2E", Log.DEBUG))
            statement.evaluate()
        }
    }

    class LabTestRearCameraStatement(private val statement: Statement) :
        Statement() {

        @Throws(Throwable::class)
        override fun evaluate() {
            // Only test in CameraX lab environment and the loggable tag will be set when running
            // the CameraX e2e test with enabling rear camera.
            assumeTrue(Log.isLoggable("rearCameraE2E", Log.DEBUG))
            statement.evaluate()
        }
    }

    override fun apply(base: Statement, description: Description): Statement {

        return if (description.getAnnotation(LabTestOnly::class.java) != null) {
            LabTestStatement(base)
        } else if (description.getAnnotation(LabTestFrontCamera::class.java) != null) {
            LabTestFrontCameraStatement(base)
        } else if (description.getAnnotation(LabTestRearCamera::class.java) != null) {
            LabTestRearCameraStatement(base)
        } else {
            base
        }
    }
}
