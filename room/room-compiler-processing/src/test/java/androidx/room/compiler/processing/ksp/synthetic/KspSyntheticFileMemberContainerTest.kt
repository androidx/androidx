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

package androidx.room.compiler.processing.ksp.synthetic

import androidx.room.compiler.processing.ksp.KspFieldElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(KspExperimental::class)
class KspSyntheticFileMemberContainerTest {
    @Test
    fun topLevel_noPackage() {
        val annotation = Source.kotlin(
            "MyAnnotation.kt",
            """
            annotation class MyAnnotation
            """.trimIndent()
        )
        val appSrc = Source.kotlin(
            "App.kt",
            """
                @MyAnnotation
                val appMember = 1
            """.trimIndent()
        )
        runKspTest(
            sources = listOf(annotation, appSrc)
        ) { invocation ->
            val elements = invocation.kspResolver.getSymbolsWithAnnotation("MyAnnotation").toList()
            assertThat(elements).hasSize(1)
            val className = elements.map {
                val owner = invocation.kspResolver.getOwnerJvmClassName(it as KSPropertyDeclaration)
                assertWithMessage(it.toString()).that(owner).isNotNull()
                KspSyntheticFileMemberContainer(owner!!).className
            }.first()
            assertThat(className.packageName()).isEmpty()
            assertThat(className.simpleNames()).containsExactly("AppKt")
        }
    }

    @Test
    fun nestedClassNames() {
        fun buildSources(pkg: String) = listOf(
            Source.java(
                "$pkg.JavaClass",
                """
                package $pkg;
                public class JavaClass {
                    int member;
                    public static class NestedClass {
                        int member;
                    }
                    public class InnerClass {
                        int member;
                    }
                }
                """.trimIndent()
            ),
            Source.java(
                "${pkg}JavaClass",
                """
                public class ${pkg}JavaClass {
                    int member;
                    public static class NestedClass {
                        int member;
                    }
                    public class InnerClass {
                        int member;
                    }
                }
                """.trimIndent()
            ),
            Source.kotlin(
                "$pkg/KotlinClass.kt",
                """
                package $pkg
                class KotlinClass {
                    val member = 1
                    class NestedClass {
                        val member = 1
                    }
                    inner class InnerClass {
                        val member = 1
                    }
                }
                """.trimIndent()
            ),
            Source.kotlin(
                "KotlinClass.kt",
                """
                class ${pkg}KotlinClass {
                    val member = 1
                    class NestedClass {
                        val member = 1
                    }
                    inner class InnerClass {
                        val member = 1
                    }
                }
                """.trimIndent()
            )
        )
        val lib = compileFiles(buildSources("lib"))
        runKspTest(
            sources = buildSources("app"),
            classpath = lib
        ) { invocation ->
            fun runTest(qName: String) {
                invocation.processingEnv.requireTypeElement(qName).let { target ->
                    val field = target.getField("member") as KspFieldElement
                    val owner = invocation.kspResolver.getOwnerJvmClassName(field.declaration)
                    assertWithMessage(qName).that(owner).isNotNull()
                    val synthetic = KspSyntheticFileMemberContainer(owner!!)
                    assertWithMessage(qName).that(target.className).isEqualTo(synthetic.className)
                }
            }
            listOf("lib", "app").forEach { pkg ->
                // test both top level and in package cases
                listOf(pkg, "$pkg.").forEach { prefix ->
                    runTest("${prefix}JavaClass")
                    runTest("${prefix}JavaClass.NestedClass")
                    runTest("${prefix}JavaClass.InnerClass")
                    runTest("${prefix}KotlinClass")
                    runTest("${prefix}KotlinClass.NestedClass")
                    runTest("${prefix}KotlinClass.InnerClass")
                }
            }
        }
    }
}