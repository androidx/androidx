/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.room.compiler.processing.XProcessingStep.Companion.asAutoCommonProcessor
import androidx.room.compiler.processing.XProcessingStep.Companion.executeInKsp
import androidx.room.compiler.processing.testcode.MainAnnotation
import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.util.CompilationTestCapabilities
import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.reflect.KClass

class XProcessingStepTest {
    @field:Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Test
    fun xProcessingStep() {
        val annotatedElements = mutableMapOf<KClass<out Annotation>, String>()
        val processingStep = object : XProcessingStep {
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XTypeElement> {
                elementsByAnnotation[OtherAnnotation::class.qualifiedName]
                    ?.filterIsInstance<XTypeElement>()
                    ?.forEach {
                        annotatedElements[OtherAnnotation::class] = it.qualifiedName
                    }
                elementsByAnnotation[MainAnnotation::class.qualifiedName]
                    ?.filterIsInstance<XTypeElement>()
                    ?.forEach {
                        annotatedElements[MainAnnotation::class] = it.qualifiedName
                    }
                return emptySet()
            }

            override fun annotations(): Set<String> {
                return setOf(
                    OtherAnnotation::class.qualifiedName!!,
                    MainAnnotation::class.qualifiedName!!
                )
            }
        }
        val mainProcessor = object : BasicAnnotationProcessor() {
            override fun steps(): Iterable<Step> {
                return listOf(
                    processingStep.asAutoCommonProcessor(processingEnv)
                )
            }
        }
        val main = JavaFileObjects.forSourceString(
            "foo.bar.Main",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.*;
            @MainAnnotation(
                typeList = {},
                singleType = Object.class,
                intMethod = 3,
                singleOtherAnnotation = @OtherAnnotation("y")
            )
            class Main {
            }
            """.trimIndent()
        )
        val other = JavaFileObjects.forSourceString(
            "foo.bar.Other",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.*;
            @OtherAnnotation("x")
            class Other {
            }
            """.trimIndent()
        )
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main, other)
        ).processedWith(
            mainProcessor
        ).compilesWithoutError()
        assertThat(annotatedElements).containsExactlyEntriesIn(
            mapOf(
                MainAnnotation::class to "foo.bar.Main",
                OtherAnnotation::class to "foo.bar.Other"
            )
        )
    }

    @Test
    fun multiStepProcessing() {
        val otherAnnotatedElements = mutableListOf<TypeName>()
        // create a scenario where we run multi-step processing so that we can test caching
        val processingStep = object : XProcessingStep {
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XTypeElement> {
                // for each element annotated with Main annotation, create a class with Other
                // annotation to trigger another round
                elementsByAnnotation[MainAnnotation::class.qualifiedName]
                    ?.filterIsInstance<XTypeElement>()
                    ?.forEach {
                        val className = ClassName.get(it.packageName, "${it.name}_Impl")
                        val spec = TypeSpec.classBuilder(className)
                            .addAnnotation(
                                AnnotationSpec.builder(OtherAnnotation::class.java).apply {
                                    addMember("value", "\"foo\"")
                                }.build()
                            )
                            .build()
                        JavaFile.builder(className.packageName(), spec)
                            .build()
                            .writeTo(env.filer)
                    }
                elementsByAnnotation[OtherAnnotation::class.qualifiedName]
                    ?.filterIsInstance<XTypeElement>()
                    ?.forEach {
                        otherAnnotatedElements.add(it.type.typeName)
                    }
                return emptySet()
            }

            override fun annotations(): Set<String> {
                return setOf(
                    OtherAnnotation::class.qualifiedName!!,
                    MainAnnotation::class.qualifiedName!!
                )
            }
        }
        val main = JavaFileObjects.forSourceString(
            "foo.bar.Main",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.*;
            @MainAnnotation(
                typeList = {},
                singleType = Object.class,
                intMethod = 3,
                singleOtherAnnotation = @OtherAnnotation("y")
            )
            class Main {
            }
            """.trimIndent()
        )
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main)
        ).processedWith(
            object : BasicAnnotationProcessor() {
                override fun steps(): Iterable<Step> {
                    return listOf(
                        processingStep.asAutoCommonProcessor(processingEnv)
                    )
                }

                override fun getSupportedOptions(): Set<String> {
                    return setOf(
                        MainAnnotation::class.qualifiedName!!,
                        OtherAnnotation::class.qualifiedName!!
                    )
                }
            }
        ).compilesWithoutError()
        assertThat(otherAnnotatedElements).containsExactly(
            ClassName.get("foo.bar", "Main_Impl")
        )
    }

    @Test
    fun caching() {
        val elementPerRound = mutableMapOf<Int, List<XTypeElement>>()
        // create a scenario where we run multi-step processing so that we can test caching
        val processingStep = object : XProcessingStep {
            var roundCounter = 0
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XTypeElement> {
                elementPerRound[roundCounter++] = listOf(
                    env.requireTypeElement("foo.bar.Main"),
                    env.requireTypeElement("foo.bar.Main")
                )
                // trigger another round
                elementsByAnnotation[MainAnnotation::class.qualifiedName]
                    ?.filterIsInstance<XTypeElement>()
                    ?.forEach {
                        val className = ClassName.get(it.packageName, "${it.name}_Impl")
                        val spec = TypeSpec.classBuilder(className)
                            .addAnnotation(
                                AnnotationSpec.builder(OtherAnnotation::class.java).apply {
                                    addMember("value", "\"foo\"")
                                }.build()
                            )
                            .build()
                        JavaFile.builder(className.packageName(), spec)
                            .build()
                            .writeTo(env.filer)
                    }
                return emptySet()
            }

            override fun annotations(): Set<String> {
                return setOf(
                    OtherAnnotation::class.qualifiedName!!,
                    MainAnnotation::class.qualifiedName!!
                )
            }
        }
        val main = JavaFileObjects.forSourceString(
            "foo.bar.Main",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.*;
            @MainAnnotation(
                typeList = {},
                singleType = Object.class,
                intMethod = 3,
                singleOtherAnnotation = @OtherAnnotation("y")
            )
            class Main {
            }
            """.trimIndent()
        )
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main)
        ).processedWith(
            object : BasicAnnotationProcessor() {
                override fun steps(): Iterable<Step> {
                    return listOf(
                        processingStep.asAutoCommonProcessor(processingEnv)
                    )
                }

                override fun getSupportedOptions(): Set<String> {
                    return setOf(
                        MainAnnotation::class.qualifiedName!!,
                        OtherAnnotation::class.qualifiedName!!
                    )
                }
            }
        ).compilesWithoutError()
        assertThat(elementPerRound).hasSize(2)
        // in each round, we should be returning the same instance of the TypeElement while
        // returning different instances between different rounds
        elementPerRound.values.forEach {
            // test sanity, we read it twice
            assertThat(it).hasSize(2)
            assertThat(it[0]).isSameInstanceAs(it[1])
        }
        // make sure elements between different rounds are not the same instances
        assertThat(elementPerRound.get(0)?.get(0))
            .isNotSameInstanceAs(elementPerRound.get(1)?.get(0))
    }

    @Test
    fun kspReturnsUnprocessed() {
        CompilationTestCapabilities.assumeKspIsEnabled()
        val processingStep = object : XProcessingStep {
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                return elementsByAnnotation.values
                    .flatten()
                    .toSet()
            }

            override fun annotations(): Set<String> {
                return setOf(OtherAnnotation::class.qualifiedName!!)
            }
        }
        var returned: List<KSAnnotated>? = null
        val processorProvider = object : SymbolProcessorProvider {
            override fun create(
                options: Map<String, String>,
                kotlinVersion: KotlinVersion,
                codeGenerator: CodeGenerator,
                logger: KSPLogger
            ): SymbolProcessor {
                return object : SymbolProcessor {
                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        val env = XProcessingEnv.create(
                            emptyMap(),
                            resolver,
                            codeGenerator,
                            logger,
                            XProcessingEnv.Language.JAVA
                        )
                        return processingStep.executeInKsp(env)
                            .also { returned = it }
                    }
                }
            }
        }
        val main = SourceFile.kotlin(
            "Other.kt",
            """
            package foo.bar
            import androidx.room.compiler.processing.testcode.*
            @OtherAnnotation("y")
            class Other {
            }
            """.trimIndent()
        )

        KotlinCompilation().apply {
            workingDir = temporaryFolder.root
            inheritClassPath = true
            symbolProcessorProviders = listOf(processorProvider)
            sources = listOf(main)
            verbose = false
        }.compile()

        assertThat(returned).apply {
            isNotNull()
            isNotEmpty()
        }
        val element = returned!!.first() as KSClassDeclaration
        assertThat(element.classKind).isEqualTo(ClassKind.CLASS)
        assertThat(element.qualifiedName!!.asString()).isEqualTo("foo.bar.Other")
    }
}