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

import androidx.room.compiler.processing.XProcessingEnvConfig
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runKspTest
import org.junit.Rule
import org.junit.Test

class GetMethodsScenarioTest {
    @get:Rule
    val profileRule = ProfileRule()

    @Test
    fun allMethods_keepInline() {
        profileAllMethods(
            XProcessingEnvConfig.DEFAULT.copy(
                excludeMethodsWithInvalidJvmSourceNames = false
            )
        )
    }

    @Test
    fun allMethods_filterOutInline() {
        profileAllMethods(
            XProcessingEnvConfig.DEFAULT.copy(
                excludeMethodsWithInvalidJvmSourceNames = true
            )
        )
    }

    @Test
    fun declaredMethods_keepInline() {
        profileDeclaredMethods(
            XProcessingEnvConfig.DEFAULT.copy(
                excludeMethodsWithInvalidJvmSourceNames = false
            )
        )
    }

    @Test
    fun declaredMethods_filterOutInline() {
        profileDeclaredMethods(
            XProcessingEnvConfig.DEFAULT.copy(
                excludeMethodsWithInvalidJvmSourceNames = true
            )
        )
    }

    @Test
    fun enclosedElements_keepInline() {
        profileEnclosedElements(
            XProcessingEnvConfig.DEFAULT.copy(
                excludeMethodsWithInvalidJvmSourceNames = false
            )
        )
    }

    @Test
    fun enclosedElements_filterOutInline() {
        profileEnclosedElements(
            XProcessingEnvConfig.DEFAULT.copy(
                excludeMethodsWithInvalidJvmSourceNames = true
            )
        )
    }

    private fun profileAllMethods(processingConfig: XProcessingEnvConfig) {
        profile(processingConfig) {
            getAllMethods().toList()
        }
    }

    private fun profileDeclaredMethods(processingConfig: XProcessingEnvConfig) {
        profile(processingConfig) {
            getDeclaredMethods().toList()
        }
    }

    private fun profileEnclosedElements(processingConfig: XProcessingEnvConfig) {
        profile(processingConfig) {
            getEnclosedElements().toList()
        }
    }

    fun profile(processingConfig: XProcessingEnvConfig, block: XTypeElement.() -> Unit) {
        val methodCount = 1000
        val contents = buildString {
            appendLine("class Subject {")
            repeat(methodCount) { index ->
                appendLine("fun method_$index(param1:Int, param2:String):String { TODO() }")
            }
            appendLine("}")
        }
        val sources = Source.kotlin("Subject.kt", contents)
        profileRule.runRepeated(
            warmUps = 5,
            repeat = 10
        ) { profileScope ->
            runKspTest(
                sources = listOf(sources),
                config = processingConfig
            ) { invocation ->
                val subject = invocation.processingEnv.requireTypeElement(
                    "Subject"
                )
                profileScope.trace {
                    subject.block()
                }
            }
        }
    }
}
