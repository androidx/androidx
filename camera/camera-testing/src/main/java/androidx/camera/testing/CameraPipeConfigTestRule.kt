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

package androidx.camera.testing

import android.util.Log
import androidx.camera.testing.CameraPipeConfigTestRule.CameraPipeExperimental
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] will ignore the test when there's no [CAMERA_PIPE_TEST_FLAG] enabled on the device.
 *
 *
 * All the test methods will be executed as usual when [active] is set to false.
 * While the [active] is true, it throws the AssumptionViolatedException if the DUT doesn't set
 * the debug [CAMERA_PIPE_TEST_FLAG]. For the case to ignore the test with CameraPipeConfig,
 * please set [active] to true.
 *
 * The [CAMERA_PIPE_TEST_FLAG] can be enabled on the DUT by the command:
 * ```
 * adb shell setprop log.tag.CAMERA_PIPE_TESTING DEBUG
 * ```
 *
 * To apply the [TestRule] for all test methods in a test class, please create the rule with
 * [forAllTests] = true
 * ```
 *  @get:Rule
 *  val testRule: CameraPipeConfigTestRule　= CameraPipeConfigTestRule(
 *      forAllTests = true, active = true
 *  )
 * ```
 *
 * To apply the [TestRule] for specific test methods, please create the [CameraPipeConfigTestRule]
 * directly, and add the [CameraPipeExperimental] annotation to the methods you would like to apply
 * the rule.
 *
 * ```
 *  @get:Rule
 *  val testRule: CameraPipeConfigTestRule　= CameraPipeConfigTestRule(
 *      active = true
 *  )
 *
 *  @CameraPipeExperimental
 *  fun yourTestCase() {
 *      ...
 *  }
 * ```
 * @property active true to activate this rule.
 * @property forAllTests true to apply the rule to all tests under the class, set false may require
 * to use [CameraPipeExperimental] annotation to mark the test method.
 */
class CameraPipeConfigTestRule(
    val active: Boolean,
    val forAllTests: Boolean = false,
) : TestRule {

    /**
     * The annotation for tests only can be executed while the [CAMERA_PIPE_TEST_FLAG] tag is
     * enabled on the device.
     *
     * The [CAMERA_PIPE_TEST_FLAG] can be enabled by using the following command:
     * "adb shell setprop log.tag.CAMERA_PIPE_TESTING DEBUG"
     *
     */
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class CameraPipeExperimental()

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                if (active) {
                    if (forAllTests ||
                        description.getAnnotation(CameraPipeExperimental::class.java) != null
                    ) {
                        Assume.assumeTrue(
                            "Ignore the test, device doesn't set the $CAMERA_PIPE_TEST_FLAG flag.",
                            Log.isLoggable(CAMERA_PIPE_TEST_FLAG, Log.DEBUG)
                        )
                    }
                }

                base.evaluate()
            }
        }

    companion object {
        private const val CAMERA_PIPE_TEST_FLAG = "CAMERA_PIPE_TESTING"
    }
}