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

package androidx.baselineprofile.gradle.consumer

import androidx.baselineprofile.gradle.utils.BaselineProfileProjectSetupRule
import androidx.baselineprofile.gradle.utils.Fixtures
import androidx.baselineprofile.gradle.utils.build
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class BaselineProfileKotlinMultiplatformLibraryTest {
    @get:Rule val projectSetup = BaselineProfileProjectSetupRule()

    private val gradleRunner by lazy { projectSetup.consumer.gradleRunner }

    private fun readBaselineProfileFileContent() =
        projectSetup.readBaselineProfileFileContent("androidMain")

    @Test
    fun testGenerateTaskWithNoFlavorsForLibrary() {
        projectSetup.consumer.setupKotlinMultiplatformLibrary()
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines =
                listOf(
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_2_METHOD_1,
                    Fixtures.CLASS_2,
                ),
            releaseStartupProfileLines =
                listOf(
                    Fixtures.CLASS_3_METHOD_1,
                    Fixtures.CLASS_3,
                    Fixtures.CLASS_4_METHOD_1,
                    Fixtures.CLASS_4,
                ),
        )

        gradleRunner.build("generateBaselineProfile") {
            // Nothing to assert here.
        }

        assertThat(readBaselineProfileFileContent())
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
                Fixtures.CLASS_3_METHOD_1,
                Fixtures.CLASS_3,
                Fixtures.CLASS_4_METHOD_1,
                Fixtures.CLASS_4,
            )
    }
}
