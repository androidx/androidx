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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.ksp.KspExecutableElement
import androidx.room.compiler.processing.ksp.KspFieldElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.kspProcessingEnv
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertWithMessage
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(KspExperimental::class)
class TopLevelMembersTest {
    @Test
    fun topLevelInDependency() {
        val libSrc = Source.kotlin(
            "lib/Foo.kt",
            """
                package lib
                fun topLevelFun() {
                }
                val topLevelVal: String = ""
                var topLevelVar: String = ""
            """.trimIndent()
        )
        val classpath = compileFiles(listOf(libSrc))
        val appSrc = Source.kotlin(
            "app/Foo.kt",
            """
                package app
                fun topLevelFun() {
                }
                val topLevelVal: String = ""
                var topLevelVar: String = ""
            """.trimIndent()
        )
        runKspTest(
            sources = listOf(appSrc),
            classpath = classpath
        ) { invocation ->
            listOf("lib", "app").forEach { pkg ->
                val declarations = invocation.kspResolver.getDeclarationsFromPackage(pkg)
                declarations.filterIsInstance<KSFunctionDeclaration>()
                    .toList().let { methods ->
                        assertWithMessage(pkg).that(methods).hasSize(1)
                        methods.forEach { method ->
                            val element = KspExecutableElement.create(
                                env = invocation.kspProcessingEnv,
                                declaration = method
                            )
                            assertWithMessage(pkg).that(
                                element.containing.isTypeElement()
                            ).isFalse()
                            assertWithMessage(pkg).that(element.isStatic()).isTrue()
                        }
                    }
                declarations.filterIsInstance<KSPropertyDeclaration>()
                    .toList().let { properties ->
                        assertWithMessage(pkg).that(properties).hasSize(2)
                        properties.forEach {
                            val element = KspFieldElement.create(
                                env = invocation.kspProcessingEnv,
                                declaration = it
                            )
                            assertWithMessage(pkg).that(
                                element.containing.isTypeElement()
                            ).isFalse()
                            assertWithMessage(pkg).that(element.isStatic()).isTrue()
                        }
                    }
            }
        }
    }
}