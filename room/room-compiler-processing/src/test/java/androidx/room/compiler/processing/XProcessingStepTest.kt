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

import androidx.room.compiler.processing.javac.JavacBasicAnnotationProcessor
import androidx.room.compiler.processing.ksp.KspBasicAnnotationProcessor
import androidx.room.compiler.processing.ksp.KspElement
import androidx.room.compiler.processing.testcode.AnywhereAnnotation
import androidx.room.compiler.processing.testcode.MainAnnotation
import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.testcode.SingleTypeValueAnnotation
import androidx.room.compiler.processing.util.CompilationTestCapabilities
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import javax.tools.Diagnostic
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
        val mainProcessor = object : JavacBasicAnnotationProcessor() {
            override fun processingSteps() = listOf(processingStep)
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
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps() = listOf(processingStep)
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
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps() = listOf(processingStep)
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
    fun javacProcessingEnvCaching() {
        // Create a scenario to test that the xProcessingEnv instance is available before processing
        // and that the xProcessingEnv is the same instance across rounds.
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

        val processingEnvPerRound = mutableMapOf<Int, XProcessingEnv>()
        val processingStep = object : XProcessingStep {
            var roundCounter = 0
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XTypeElement> {
                processingEnvPerRound[roundCounter++] = env
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

        val xProcessingEnvs = mutableListOf<XProcessingEnv>()
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main)
        ).processedWith(
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps(): Iterable<XProcessingStep> {
                    xProcessingEnvs.add(xProcessingEnv)
                    return listOf(processingStep)
                }
            }
        ).compilesWithoutError()

        // Makes sure processingSteps() was only called once, and that the xProcessingEnv was set.
        assertThat(xProcessingEnvs).hasSize(1)
        assertThat(xProcessingEnvs.get(0)).isNotNull()

        // Make sure there were two rounds, and processingEnv between rounds is the same instance.
        assertThat(processingEnvPerRound).hasSize(2)
        assertThat(xProcessingEnvs.get(0)).isSameInstanceAs(processingEnvPerRound.get(0))
        assertThat(xProcessingEnvs.get(0)).isSameInstanceAs(processingEnvPerRound.get(1))
    }

    @Test
    fun kspProcessingEnvCaching() {
        val main = Source.java(
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

        val processingEnvPerRound = mutableMapOf<Int, XProcessingEnv>()
        // create a scenario where we run multi-step processing so that we can test caching
        val processingStep = object : XProcessingStep {
            var roundCounter = 0
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XTypeElement> {
                processingEnvPerRound[roundCounter++] = env
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

        val xProcessingEnvs = mutableListOf<XProcessingEnv>()
        val processorProvider = object : SymbolProcessorProvider {
            override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
                return object : KspBasicAnnotationProcessor(environment) {
                    override fun processingSteps(): Iterable<XProcessingStep> {
                        xProcessingEnvs.add(xProcessingEnv)
                        return listOf(processingStep)
                    }
                }
            }
        }

        compile(
            workingDir = temporaryFolder.root,
            arguments = TestCompilationArguments(
                sources = listOf(main),
                symbolProcessorProviders = listOf(processorProvider)
            )
        )
        // Makes sure processingSteps() was only called once, and that the xProcessingEnv was set.
        assertThat(xProcessingEnvs).hasSize(1)
        assertThat(xProcessingEnvs.get(0)).isNotNull()

        // Make sure there were two rounds, and processingEnv between rounds is the same instance.
        assertThat(processingEnvPerRound).hasSize(2)
        assertThat(xProcessingEnvs.get(0)).isSameInstanceAs(processingEnvPerRound.get(0))
        assertThat(xProcessingEnvs.get(0)).isSameInstanceAs(processingEnvPerRound.get(1))
    }

    @Test
    fun cachingBetweenSteps() {
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
            class Main {}
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
        val elementsByStep = mutableMapOf<XProcessingStep, XTypeElement>()
        // create a scenario where we can test caching between steps
        val mainStep = object : XProcessingStep {
            override fun annotations(): Set<String> = setOf(MainAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XTypeElement> {
                elementsByStep[this] = env.requireTypeElement("foo.bar.Main")
                return emptySet()
            }
        }
        val otherStep = object : XProcessingStep {
            override fun annotations(): Set<String> = setOf(OtherAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XTypeElement> {
                elementsByStep[this] = env.requireTypeElement("foo.bar.Main")
                return emptySet()
            }
        }
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main, other)
        ).processedWith(
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps() = listOf(mainStep, otherStep)
            }
        ).compilesWithoutError()
        assertThat(elementsByStep.keys).containsExactly(mainStep, otherStep)
        // make sure elements between steps are the same instances
        assertThat(elementsByStep[mainStep]).isSameInstanceAs(elementsByStep[otherStep])
    }

    @Test
    fun javacReturnsUnprocessed() {
        val processingStep = object : XProcessingStep {
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                return elementsByAnnotation.values.flatten().toSet()
            }
            override fun annotations(): Set<String> {
                return setOf(OtherAnnotation::class.qualifiedName!!)
            }
        }
        val main = Source.java(
            "foo.bar.Other",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.*;
            @OtherAnnotation("y")
            class Other {
            }
            """.trimIndent()
        )
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main.toJFO())
        ).processedWith(
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps() = listOf(processingStep)
            }
        ).failsToCompile()
            // processor name is 'null' because it is a local anonymous class
            .withErrorContaining(
                "null was unable to process 'foo.bar.Other' because not all of its dependencies " +
                    "could be resolved. Check for compilation errors or a circular dependency " +
                    "with generated code."
            )
    }

    @Test
    fun kspReturnsUnprocessed() {
        CompilationTestCapabilities.assumeKspIsEnabled()
        var returned: Set<XElement>? = null
        val processingStep = object : XProcessingStep {
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                return elementsByAnnotation.values
                    .flatten()
                    .toSet()
                    .also { returned = it }
            }

            override fun annotations(): Set<String> {
                return setOf(OtherAnnotation::class.qualifiedName!!)
            }
        }
        val processorProvider = object : SymbolProcessorProvider {
            override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
                return object : KspBasicAnnotationProcessor(environment) {
                    override fun processingSteps() = listOf(processingStep)
                }
            }
        }
        val main = Source.kotlin(
            "Other.kt",
            """
            package foo.bar
            import androidx.room.compiler.processing.testcode.*
            @OtherAnnotation("y")
            class Other {
            }
            """.trimIndent()
        )

        val result = compile(
            workingDir = temporaryFolder.root,
            arguments = TestCompilationArguments(
                sources = listOf(main),
                symbolProcessorProviders = listOf(processorProvider)
            )
        )
        assertThat(returned).apply {
            isNotNull()
            isNotEmpty()
        }
        val element =
            returned!!.map { (it as KspElement).declaration }.first() as KSClassDeclaration
        assertThat(element.classKind).isEqualTo(ClassKind.CLASS)
        assertThat(element.qualifiedName!!.asString()).isEqualTo("foo.bar.Other")
        assertThat(result.success).isFalse()
        // processor name is 'null' because it is a local anonymous class
        assertThat(
            result.diagnostics[Diagnostic.Kind.ERROR]?.map { it.msg } ?: emptyList<String>()
        ).containsExactly(
            "null was unable to process 'foo.bar.Other' because not all of its dependencies " +
                "could be resolved. Check for compilation errors or a circular dependency with " +
                "generated code."
        )
    }

    @Test
    fun javacAnnotatedElementsByStep() {
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
        val elementsByStep = mutableMapOf<XProcessingStep, Collection<String>>()
        val mainStep = object : XProcessingStep {
            override fun annotations() = setOf(MainAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                elementsByStep[this] = elementsByAnnotation.values.flatten()
                    .map { (it as XTypeElement).qualifiedName }
                return emptySet()
            }
        }
        val otherStep = object : XProcessingStep {
            override fun annotations() = setOf(OtherAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                elementsByStep[this] = elementsByAnnotation.values.flatten()
                    .map { (it as XTypeElement).qualifiedName }
                return emptySet()
            }
        }
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main, other)
        ).processedWith(
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps() = listOf(mainStep, otherStep)
            }
        ).compilesWithoutError()
        assertThat(elementsByStep[mainStep])
            .containsExactly("foo.bar.Main")
        assertThat(elementsByStep[otherStep])
            .containsExactly("foo.bar.Other")
    }

    @Test
    fun javacDeferredStep() {
        // create a scenario where we defer the first round of processing
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
            class Main {}
            """.trimIndent()
        )
        val stepsProcessed = mutableListOf<XProcessingStep>()
        var invokedProcessOver = 0
        val mainStep = object : XProcessingStep {
            var round = 0
            override fun annotations() = setOf(MainAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                stepsProcessed.add(this)
                val deferredElements = if (round++ == 0) {
                    // Generate a random class to trigger another processing round
                    val className = ClassName.get("foo.bar", "Main_Impl")
                    val spec = TypeSpec.classBuilder(className).build()
                    JavaFile.builder(className.packageName(), spec)
                        .build()
                        .writeTo(env.filer)

                    // Defer all processing to the next round
                    elementsByAnnotation.values.flatten().toSet()
                } else {
                    emptySet()
                }
                return deferredElements
            }

            override fun processOver(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ) {
                invokedProcessOver++
            }
        }
        var invokedProcessingSteps = 0
        val invokedPostRound = mutableListOf<Boolean>()
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main)
        ).processedWith(
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps(): List<XProcessingStep> {
                    invokedProcessingSteps++
                    return listOf(mainStep)
                }

                override fun postRound(env: XProcessingEnv, round: XRoundEnv) {
                    invokedPostRound.add(round.isProcessingOver)
                }
            }
        ).compilesWithoutError()

        // Assert that mainStep was processed twice due to deferring
        assertThat(stepsProcessed).containsExactly(mainStep, mainStep)

        // Assert processingSteps() was only called once
        assertThat(invokedProcessingSteps).isEqualTo(1)

        // Assert processOver() was only called once
        assertThat(invokedProcessOver).isEqualTo(1)

        // Assert postRound() is invoked exactly 3 times, and the last round env reported
        // that processing was over.
        assertThat(invokedPostRound).containsExactly(false, false, true)
    }

    @Test
    fun javacDeferredViaException() {
        val main = JavaFileObjects.forSourceString(
            "foo.bar.Main",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.*;
            @MainAnnotation(
                typeList = {},
                singleType = AnotherSource.class,
                intMethod = 3,
                singleOtherAnnotation = @OtherAnnotation("y")
            )
            class Main {}
            """.trimIndent()
        )
        val anotherSource = JavaFileObjects.forSourceString(
            "foo.bar.AnotherSource",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.*;
            @SingleTypeValueAnnotation(GeneratedType.class)
            class AnotherSource { }
            """.trimIndent()
        )
        var round = 0
        val genClassName = ClassName.get("foo.bar", "GeneratedType")
        val mainStep = object : XProcessingStep {
            override fun annotations() = setOf(MainAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                if (round++ == 0) {
                    // Generate the interface, should not be resolvable in 1st round
                    val spec = TypeSpec.interfaceBuilder(genClassName).build()
                    JavaFile.builder(genClassName.packageName(), spec)
                        .build()
                        .writeTo(env.filer)
                }
                val mainElement =
                    elementsByAnnotation[MainAnnotation::class.qualifiedName!!]!!.single()
                try {
                    val otherElement = env.requireTypeElement(
                        mainElement.requireAnnotation(MainAnnotation::class.className())
                            .getAsType("singleType")
                            .typeName
                    )
                    val generatedType =
                        otherElement.requireAnnotation(SingleTypeValueAnnotation::class.className())
                            .getAsType("value")
                    assertThat(generatedType.typeName).isEqualTo(genClassName)
                    return emptySet()
                } catch (ex: TypeNotPresentException) {
                    return setOf(mainElement)
                }
            }
        }
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main, anotherSource)
        ).processedWith(
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps(): List<XProcessingStep> {
                    return listOf(mainStep)
                }
            }
        ).compilesWithoutError()

        // Expect two rounds due to implicit deferring caused by missing type.
        assertThat(round).isEqualTo(2)
    }

    @Test
    fun javacStepOnlyCalledIfElementsToProcess() {
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
        val stepsProcessed = mutableListOf<XProcessingStep>()
        val mainStep = object : XProcessingStep {
            override fun annotations() = setOf(MainAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                stepsProcessed.add(this)
                return emptySet()
            }
        }
        val otherStep = object : XProcessingStep {
            override fun annotations() = setOf(OtherAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                stepsProcessed.add(this)
                return emptySet()
            }
        }
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main)
        ).processedWith(
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps() = listOf(mainStep, otherStep)
            }
        ).compilesWithoutError()
        assertThat(stepsProcessed).containsExactly(mainStep)
    }

    @Test
    fun kspAnnotatedElementsByStep() {
        val main = Source.kotlin(
            "Classes.kt",
            """
            package foo.bar
            import androidx.room.compiler.processing.testcode.*
            @MainAnnotation(
                typeList = [],
                singleType = Any::class,
                intMethod = 3,
                singleOtherAnnotation = OtherAnnotation("y")
            )
            class Main {
            }
            @OtherAnnotation("y")
            class Other {
            }
            """.trimIndent()
        )
        val elementsByStep = mutableMapOf<XProcessingStep, Collection<String>>()
        val mainStep = object : XProcessingStep {
            override fun annotations() = setOf(MainAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                elementsByStep[this] = elementsByAnnotation.values.flatten()
                    .map { (it as XTypeElement).qualifiedName }
                return emptySet()
            }
        }
        val otherStep = object : XProcessingStep {
            override fun annotations() = setOf(OtherAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                elementsByStep[this] = elementsByAnnotation.values.flatten()
                    .map { (it as XTypeElement).qualifiedName }
                return emptySet()
            }
        }
        val processorProvider = object : SymbolProcessorProvider {
            override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
                return object : KspBasicAnnotationProcessor(environment) {
                    override fun processingSteps() = listOf(mainStep, otherStep)
                }
            }
        }
        compile(
            workingDir = temporaryFolder.root,
            arguments = TestCompilationArguments(
                sources = listOf(main),
                symbolProcessorProviders = listOf(processorProvider)
            )
        )
        assertThat(elementsByStep[mainStep])
            .containsExactly("foo.bar.Main")
        assertThat(elementsByStep[otherStep])
            .containsExactly("foo.bar.Other")
    }

    @Test
    fun kspDeferredStep() {
        // create a scenario where we defer the first round of processing
        val main = Source.kotlin(
            "Classes.kt",
            """
            package foo.bar
            import androidx.room.compiler.processing.testcode.*
            @MainAnnotation(
                typeList = [],
                singleType = Any::class,
                intMethod = 3,
                singleOtherAnnotation = OtherAnnotation("y")
            )
            class Main {}
            """.trimIndent()
        )
        val stepsProcessed = mutableListOf<XProcessingStep>()
        var invokedProcessOver = 0
        val mainStep = object : XProcessingStep {
            var round = 0
            override fun annotations() = setOf(MainAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                stepsProcessed.add(this)
                val deferredElements = if (round++ == 0) {
                    // Generate a random class to trigger another processing round
                    val className = ClassName.get("foo.bar", "Main_Impl")
                    val spec = TypeSpec.classBuilder(className).build()
                    JavaFile.builder(className.packageName(), spec)
                        .build()
                        .writeTo(env.filer)

                    // Defer all processing to the next round
                    elementsByAnnotation.values.flatten().toSet()
                } else {
                    emptySet()
                }
                return deferredElements
            }

            override fun processOver(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ) {
                invokedProcessOver++
            }
        }
        var invokedProcessingSteps = 0
        val invokedPostRound = mutableListOf<Boolean>()
        val processorProvider = object : SymbolProcessorProvider {
            override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
                return object : KspBasicAnnotationProcessor(environment) {
                    override fun processingSteps(): List<XProcessingStep> {
                        invokedProcessingSteps++
                        return listOf(mainStep)
                    }

                    override fun postRound(env: XProcessingEnv, round: XRoundEnv) {
                        invokedPostRound.add(round.isProcessingOver)
                    }
                }
            }
        }
        compile(
            workingDir = temporaryFolder.root,
            arguments = TestCompilationArguments(
                sources = listOf(main),
                symbolProcessorProviders = listOf(processorProvider)
            )
        )
        // Assert that mainStep was processed twice due to deferring
        assertThat(stepsProcessed).containsExactly(mainStep, mainStep)

        // Assert processingSteps() was only called once
        assertThat(invokedProcessingSteps).isEqualTo(1)

        // Assert processOver() was only called once
        assertThat(invokedProcessOver).isEqualTo(1)

        // Assert postRound() is invoked exactly 3 times, and the last round env reported
        // that processing was over.
        assertThat(invokedPostRound).containsExactly(false, false, true)
    }

    @Test
    fun kspStepOnlyCalledIfElementsToProcess() {
        val main = Source.kotlin(
            "Classes.kt",
            """
            package foo.bar
            import androidx.room.compiler.processing.testcode.*
            @MainAnnotation(
                typeList = [],
                singleType = Any::class,
                intMethod = 3,
                singleOtherAnnotation = OtherAnnotation("y")
            )
            class Main {
            }
            """.trimIndent()
        )
        val stepsProcessed = mutableListOf<XProcessingStep>()
        val mainStep = object : XProcessingStep {
            override fun annotations() = setOf(MainAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                stepsProcessed.add(this)
                return emptySet()
            }
        }
        val otherStep = object : XProcessingStep {
            override fun annotations() = setOf(OtherAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                stepsProcessed.add(this)
                return emptySet()
            }
        }
        val processorProvider = object : SymbolProcessorProvider {
            override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
                return object : KspBasicAnnotationProcessor(environment) {
                    override fun processingSteps() = listOf(mainStep, otherStep)
                }
            }
        }
        compile(
            workingDir = temporaryFolder.root,
            arguments = TestCompilationArguments(
                sources = listOf(main),
                symbolProcessorProviders = listOf(processorProvider)
            )
        )
        assertThat(stepsProcessed).containsExactly(mainStep)
    }

    @Test
    fun kspVariousDeferredElements() {
        val main = Source.kotlin(
            "Main.kt",
            """
            package foo.bar
            import androidx.room.compiler.processing.testcode.*
            class Main {
                @AnywhereAnnotation fun mainMethod() {}
                class InnerMain {
                    @AnywhereAnnotation fun innerMethod() {}
                }
            }
            """.trimIndent()
        )
        val extra = Source.kotlin(
            "Extra.kt",
            """
            package foo.bar
            import androidx.room.compiler.processing.testcode.*
            class Extra {
                fun mainMethod(@AnywhereAnnotation param: String) {}
            }
            """.trimIndent()
        )
        val assertRound: (Int, List<XElement>) -> Unit = { roundIndex, roundReceivedElements ->
            if (roundIndex == 1) {
                // Verify the deferred elements
                roundReceivedElements.let { elements ->
                    val methods = elements.filterIsInstance<XMethodElement>()
                    val params = elements.filterIsInstance<XExecutableParameterElement>()
                    assertThat(methods).hasSize(2)
                    assertThat(methods.firstOrNull { it.name == "mainMethod" }).isNotNull()
                    assertThat(methods.firstOrNull { it.name == "innerMethod" }).isNotNull()
                    assertThat(params).hasSize(1)
                    assertThat(params.firstOrNull { it.name == "param" }).isNotNull()
                }
            }
        }
        val roundReceivedElementsHashes = mutableListOf<Set<Int>>()
        val mainStep = object : XProcessingStep {
            var round = 0
            override fun annotations() = setOf(AnywhereAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                elementsByAnnotation.values.flatten().let {
                    assertRound(round, it)
                    roundReceivedElementsHashes.add(it.map { it.hashCode() }.toSet())
                }
                return if (round++ == 0) {
                    // Generate a random class to trigger another processing round
                    val className = ClassName.get("foo.bar", "Main_Impl")
                    val spec = TypeSpec.classBuilder(className).build()
                    JavaFile.builder(className.packageName(), spec)
                        .build()
                        .writeTo(env.filer)
                    elementsByAnnotation.values.flatten().toSet()
                } else {
                    emptySet()
                }
            }
        }
        val processorProvider = SymbolProcessorProvider { environment ->
            object : KspBasicAnnotationProcessor(environment) {
                override fun processingSteps() = listOf(mainStep)
            }
        }
        val compileResult = compile(
            workingDir = temporaryFolder.root,
            arguments = TestCompilationArguments(
                sources = listOf(main, extra),
                symbolProcessorProviders = listOf(processorProvider)
            )
        )
        assertThat(compileResult.success).isTrue()

        // Expect 2 rounds of processing
        assertThat(roundReceivedElementsHashes).hasSize(2)
        // Expect 3 annotated elements at around 0
        assertThat(roundReceivedElementsHashes[0]).hasSize(3)
        // Expect 3 annotated elements at around 1 (they were deferred)
        assertThat(roundReceivedElementsHashes[1]).hasSize(3)
        // Verify none of the deferred elements match in equality of previous rounds, i.e. they
        // are not being cached.
        assertThat(
            roundReceivedElementsHashes[0].none { roundReceivedElementsHashes[1].contains(it) }
        ).isTrue()
    }

    @Test
    fun javacVariousDeferredElements() {
        val main = Source.java(
            "foo.bar.Main",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.*;
            public class Main {
                @AnywhereAnnotation void mainMethod() {}
                static class InnerMain {
                    @AnywhereAnnotation void innerMethod() {}
                }
            }
            """.trimIndent()
        ).toJFO()
        val extra = Source.java(
            "foo.bar.Extra",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.*;
            public class Extra {
                void mainMethod(@AnywhereAnnotation String param) {}
            }
            """.trimIndent()
        ).toJFO()
        val assertRound: (Int, List<XElement>) -> Unit = { roundIndex, roundReceivedElements ->
            if (roundIndex == 1) {
                // Verify the deferred elements
                roundReceivedElements.let { elements ->
                    val methods = elements.filterIsInstance<XMethodElement>()
                    val params = elements.filterIsInstance<XExecutableParameterElement>()
                    assertThat(methods).hasSize(2)
                    assertThat(methods.firstOrNull { it.name == "mainMethod" }).isNotNull()
                    assertThat(methods.firstOrNull { it.name == "innerMethod" }).isNotNull()
                    assertThat(params).hasSize(1)
                    assertThat(params.firstOrNull { it.name == "param" }).isNotNull()
                }
            }
        }
        val roundReceivedElementsHashes = mutableListOf<Set<Int>>()
        val mainStep = object : XProcessingStep {
            var round = 0
            override fun annotations() = setOf(AnywhereAnnotation::class.qualifiedName!!)
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>
            ): Set<XElement> {
                elementsByAnnotation.values.flatten().let {
                    assertRound(round, it)
                    roundReceivedElementsHashes.add(it.map { it.hashCode() }.toSet())
                }
                return if (round++ == 0) {
                    // Generate a random class to trigger another processing round
                    val className = ClassName.get("foo.bar", "Main_Impl")
                    val spec = TypeSpec.classBuilder(className).build()
                    JavaFile.builder(className.packageName(), spec)
                        .build()
                        .writeTo(env.filer)
                    elementsByAnnotation.values.flatten().toSet()
                } else {
                    emptySet()
                }
            }
        }
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main, extra)
        ).processedWith(
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps() = listOf(mainStep)
            }
        ).compilesWithoutError()

        // Expect 2 rounds of processing
        assertThat(roundReceivedElementsHashes).hasSize(2)
        // Expect 3 annotated elements at around 0
        assertThat(roundReceivedElementsHashes[0]).hasSize(3)
        // Expect 3 annotated elements at around 1 (they were deferred)
        assertThat(roundReceivedElementsHashes[1]).hasSize(3)
        // Verify none of the deferred elements match in equality of previous rounds, i.e. they
        // are not being cached.
        assertThat(
            roundReceivedElementsHashes[0].none { roundReceivedElementsHashes[1].contains(it) }
        ).isTrue()
    }
}