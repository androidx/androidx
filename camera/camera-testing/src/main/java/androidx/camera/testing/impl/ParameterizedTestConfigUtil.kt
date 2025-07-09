/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.testing.impl.LabTestRule.Companion.isInLabTest

/** Utility functions of parameterized tests. */
public object ParameterizedTestConfigUtil {
    /**
     * Generates the parameterized test configs with Camera2Config and CameraPipeConfig.
     *
     * If inLabTestRequired is true but the test is not in the lab test environment, an empty list
     * will be returned.
     *
     * @param configs the configs that will be used to generate the parameterized test configs.
     * @param inLabTestRequired whether the test should be run in the lab test environment.
     */
    @JvmStatic
    public fun generateCameraXConfigParameterizedTestConfigs(
        configs: List<Array<Any?>> = listOf(arrayOf()),
        inLabTestRequired: Boolean = false,
    ): List<Array<Any?>> =
        if (inLabTestRequired && !isInLabTest()) {
            emptyList()
        } else {
            mutableListOf<Array<Any?>>().apply {
                configs.forEach { testConfigArray ->
                    if (!Log.isLoggable("CAMERA2_TEST_DISABLE", Log.DEBUG)) {
                        add(
                            testConfigArray +
                                Camera2Config::class.simpleName +
                                Camera2Config.defaultConfig()
                        )
                    }

                    if (Log.isLoggable("CameraPipeMH", Log.DEBUG)) {
                        add(
                            testConfigArray +
                                CameraPipeConfig::class.simpleName +
                                CameraPipeConfig.defaultConfig()
                        )
                    }
                }
            }
        }

    /**
     * Generates the parameterized test configs for in-lab-test-required condition.
     *
     * If the test is not in the lab test environment, an empty list will be returned.
     */
    public fun generateInLabRequiredTestDefaultParameterizedTestConfigs(): List<Array<Any?>> =
        if (!isInLabTest()) {
            emptyList()
        } else {
            listOf(arrayOf("InLabRequiredTest"))
        }
}
