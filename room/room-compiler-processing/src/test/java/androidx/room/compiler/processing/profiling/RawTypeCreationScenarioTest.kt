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

package androidx.room.compiler.processing.profiling

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runKspTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RawTypeCreationScenarioTest {
    @get:Rule
    val profileRule = ProfileRule()

    @Test
    fun profile() {
        val classCount = 1000
        val contents = buildString {
            (0 until classCount).forEach { cnt ->
                appendLine("class Sample$cnt")
            }
        }
        val sources = Source.kotlin("Sample.kt", contents)
        val classNames = (0 until classCount).map { "Sample$it" }
        profileRule.runRepeated(
            warmUps = 10,
            repeat = 20
        ) { profileScope ->
            runKspTest(
                sources = listOf(sources)
            ) { invocation ->
                profileScope.trace {
                    classNames.forEach { className ->
                        invocation.processingEnv.requireTypeElement(
                            className
                        ).type.rawType
                    }
                }
            }
        }
    }
}
