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

package androidx.room.compiler.processing

import androidx.kruth.assertThat
import androidx.room.compiler.processing.util.runProcessorTest
import org.junit.Test

class XProcessingEnvConfigTest {
    @Test
    fun testConfig() {
        val myConfig = XProcessingEnvConfig.Builder().build()
        runProcessorTest(
            config = myConfig
        ) {
            assertThat(
                it.processingEnv.config
            ).isSameInstanceAs(myConfig)
        }
    }

    @Test
    fun callItLikeJava() {
        val myConfig = XProcessingEnvConfig.Builder().excludeMethodsWithInvalidJvmSourceNames(
            true
        ).build()
        runProcessorTest(
            config = myConfig
        ) {
            assertThat(
                it.processingEnv.config.excludeMethodsWithInvalidJvmSourceNames
            ).isTrue()
        }
    }

    @Test
    fun callItLikeKotlin() {
        val myConfig = XProcessingEnvConfig.DEFAULT.copy(
            excludeMethodsWithInvalidJvmSourceNames = true
        )
        runProcessorTest(
            config = myConfig
        ) {
            assertThat(
                it.processingEnv.config.excludeMethodsWithInvalidJvmSourceNames
            ).isTrue()
        }
    }
}
