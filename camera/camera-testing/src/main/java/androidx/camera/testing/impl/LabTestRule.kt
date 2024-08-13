/*
 * Copyright 2023 The Android Open Source Project
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

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.testing.impl.LabTestRule.LabTestFrontCamera
import androidx.camera.testing.impl.LabTestRule.LabTestOnly
import androidx.camera.testing.impl.LabTestRule.LabTestRearCamera
import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] which can be used to limit the test only run in the CameraX lab environment. It
 * throws the AssumptionViolatedException to ignore the test if the test environment is not in the
 * lab. Useful for the tests not needed to run on the PostSubmit.
 *
 * To use this [TestRule], do the following. Add the Rule to your JUnit test:
 * ```
 * @get:Rule
 * val labTestRule = LabTestRule()
 * ```
 *
 * Add only one of [LabTestOnly], [LabTestFrontCamera] or, [LabTestRearCamera] annotation to your
 * test case like:
 * ```
 *  @LabTestOnly
 *  fun yourTestCase() {
 *
 *  }
 * ```
 *
 * To local run the test with the [LabTestOnly] annotation, please run the following command on the
 * DUT:
 * ```
 * adb shell setprop log.tag.MH DEBUG
 * ```
 *
 * [LabTestFrontCamera] and [LabTestRearCamera] can be tested on local DUT with the following debug
 * options:
 * ```
 * adb shell setprop log.tag.frontCameraE2E DEBUG
 * adb shell setprop log.tag.rearCameraE2E DEBUG
 * ```
 */
public class LabTestRule : TestRule {

    /**
     * The annotation for tests that only want to run on the CameraX lab environment. Local device
     * testing will ignore the tests with this annotation. Please reference the doc of [LabTestRule]
     * to test on local devices.
     */
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    public annotation class LabTestOnly()

    /**
     * The annotation for tests that only want to run on the CameraX lab environment with enabling
     * front camera. Local device testing will ignore the tests with this annotation. Please
     * reference the doc of [LabTestRule] to test on local devices.
     */
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    public annotation class LabTestFrontCamera()

    /**
     * The annotation for tests that only want to run on the CameraX lab environment with enabling
     * rear camera. Local device testing will ignore the tests with this annotation. Please
     * reference the doc of [LabTestRule] to test on local devices.
     */
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    public annotation class LabTestRearCamera()

    public class LabTestStatement(private val statement: Statement) : Statement() {

        @Throws(Throwable::class)
        override fun evaluate() {
            // Only test in CameraX lab environment and throw AssumptionViolatedException if not
            // in the lab environment. The loggable tag will be set when running the CameraX
            // daily testing.
            assumeTrue(isInLabTest())
            statement.evaluate()
        }
    }

    public class LabTestFrontCameraStatement(private val statement: Statement) : Statement() {

        @Throws(Throwable::class)
        override fun evaluate() {
            // Only test in CameraX lab environment and the loggable tag will be set when running
            // the CameraX e2e test with enabling front camera.
            assumeTrue(Log.isLoggable("frontCameraE2E", Log.DEBUG))
            statement.evaluate()
        }
    }

    public class LabTestRearCameraStatement(private val statement: Statement) : Statement() {

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

    public companion object {
        @JvmStatic
        public fun isInLabTest(): Boolean {
            return Log.isLoggable("MH", Log.DEBUG)
        }

        /**
         * Checks if it is CameraX lab environment where the enabled camera uses the specified
         * [lensFacing] direction.
         *
         * For example, if [lensFacing] is [CameraSelector.LENS_FACING_BACK], this method will
         * return true if the rear camera is enabled on a device in CameraX lab environment.
         *
         * @param lensFacing the required camera direction relative to the device screen.
         * @return if enabled camera is in same direction as [lensFacing] in CameraX lab environment
         */
        @JvmStatic
        public fun isLensFacingEnabledInLabTest(
            @CameraSelector.LensFacing lensFacing: Int
        ): Boolean =
            when (lensFacing) {
                CameraSelector.LENS_FACING_BACK -> Log.isLoggable("rearCameraE2E", Log.DEBUG)
                CameraSelector.LENS_FACING_FRONT -> Log.isLoggable("frontCameraE2E", Log.DEBUG)
                else -> false
            }
    }
}
