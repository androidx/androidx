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

package androidx.room.compiler.processing.ksp

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.CompilationTestCapabilities
import androidx.room.compiler.processing.util.KOTLINC_LANGUAGE_1_9_ARGS
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.runKaptTest
import androidx.room.compiler.processing.util.runKspTest
import com.squareup.javapoet.TypeName
import java.io.File
import org.junit.Before
import org.junit.Test

/**
 * This test generates a brute force of method signatures and ensures they match what we generate in
 * KAPT.
 */
class KspTypeNamesGoldenTest {
    lateinit var sources: List<Source>
    lateinit var classpath: List<File>
    lateinit var subjects: List<String>

    @Before
    fun setup() {
        CompilationTestCapabilities.assumeKspIsEnabled()
        val (mainSources, mainSubjects) = createSubjects(pkg = "main")
        val (libSources, libSubjects) = createSubjects(pkg = "lib")
        sources = mainSources
        classpath = compileFiles(libSources)
        subjects = mainSubjects + libSubjects
    }

    @Test
    fun kaptSignatureTest() {
        var kaptSignatures = listOf<MethodSignature>()
        runKaptTest(sources = sources, classpath = classpath) { invocation ->
            kaptSignatures = collectSignatures(invocation, subjects)
        }

        // make sure we grabbed some values to ensure test is working
        assertThat(kaptSignatures).isNotEmpty()
        subjects.forEach { subject ->
            assertWithMessage("there are no methods from $subject")
                .that(kaptSignatures.any { it.name.startsWith(subject) })
                .isTrue()
        }

        // make sure none of the signatures contain duplicate names
        assertThat(kaptSignatures.map { it.name }).containsNoDuplicates()
    }

    @Test
    fun ksp1SignatureTest() {
        var kaptSignatures = listOf<MethodSignature>()
        runKaptTest(sources = sources, classpath = classpath) { invocation ->
            kaptSignatures = collectSignatures(invocation, subjects)
        }

        var ksp1Signatures = listOf<MethodSignature>()
        runKspTest(
            sources = sources,
            classpath = classpath,
            kotlincArguments = KOTLINC_LANGUAGE_1_9_ARGS
        ) { invocation ->
            ksp1Signatures = collectSignatures(invocation, subjects)
        }

        // make sure none of the signatures contain duplicate names
        assertThat(ksp1Signatures.map { it.name }).containsNoDuplicates()

        // Check that the KSP1 signatures match KAPT
        assertKspSignaturesMatchKapt(
            kaptSignatures = kaptSignatures.associateBy { it.name },
            kspSignatures = ksp1Signatures.associateBy { it.name },
            expectedFailingKeys = emptyList()
        )
    }

    @Test
    fun ksp2SignatureTest() {
        var kaptSignatures = listOf<MethodSignature>()
        runKaptTest(sources = sources, classpath = classpath) { invocation ->
            kaptSignatures = collectSignatures(invocation, subjects)
        }

        var ksp2Signatures = listOf<MethodSignature>()
        runKspTest(sources = sources, classpath = classpath) { invocation ->
            ksp2Signatures = collectSignatures(invocation, subjects)
        }

        // make sure none of the signatures contain duplicate names
        assertThat(ksp2Signatures.map { it.name }).containsNoDuplicates()

        // Check that the KSP2 signatures match KAPT
        assertKspSignaturesMatchKapt(
            kaptSignatures = kaptSignatures.associateBy { it.name },
            kspSignatures = ksp2Signatures.associateBy { it.name },
            // TODO(b/394692559): Fix these cases and remove this list of failing keys.
            expectedFailingKeys =
                listOf(
                    "main.Subject.method47",
                    "main.Subject.method50",
                    "main.VarianceSubject.<init>",
                    "main.VarianceSubject.getValMyMapAliasR",
                    "main.VarianceSubject.getValMyMapAliasMyInterface",
                    "main.VarianceSubject.mapTypeAlias1",
                    "main.VarianceSubject.mapTypeAlias2",
                    "main.VarianceSubject.mapTypeAlias7",
                    "main.VarianceSubject.mapTypeAlias8",
                    "main.VarianceSubjectSuppressed.<init>",
                    "main.VarianceSubjectSuppressed.getValMyMapAliasR",
                    "main.VarianceSubjectSuppressed.getValMyMapAliasMyInterface",
                    "main.VarianceSubjectSuppressed.mapTypeAlias1",
                    "main.VarianceSubjectSuppressed.mapTypeAlias2",
                    "main.VarianceSubjectSuppressed.mapTypeAlias7",
                    "main.VarianceSubjectSuppressed.mapTypeAlias8",
                    "lib.Subject.method47",
                    "lib.Subject.method50",
                    "lib.VarianceSubject.<init>",
                    "lib.VarianceSubject.getValMyMapAliasR",
                    "lib.VarianceSubject.getValMyMapAliasMyInterface",
                    "lib.VarianceSubject.getVarMyMapAliasR",
                    "lib.VarianceSubject.setVarMyMapAliasR",
                    "lib.VarianceSubject.getVarMyMapAliasMyInterface",
                    "lib.VarianceSubject.setVarMyMapAliasMyInterface",
                    "lib.VarianceSubject.mapTypeAlias1",
                    "lib.VarianceSubject.mapTypeAlias2",
                    "lib.VarianceSubject.mapTypeAlias7",
                    "lib.VarianceSubject.mapTypeAlias8",
                    "lib.VarianceSubjectSuppressed.<init>",
                    "lib.VarianceSubjectSuppressed.getValMyMapAliasR",
                    "lib.VarianceSubjectSuppressed.getValMyMapAliasMyInterface",
                    "lib.VarianceSubjectSuppressed.getVarMyMapAliasR",
                    "lib.VarianceSubjectSuppressed.setVarMyMapAliasR",
                    "lib.VarianceSubjectSuppressed.getVarMyMapAliasMyInterface",
                    "lib.VarianceSubjectSuppressed.setVarMyMapAliasMyInterface",
                    "lib.VarianceSubjectSuppressed.mapTypeAlias1",
                    "lib.VarianceSubjectSuppressed.mapTypeAlias2",
                    "lib.VarianceSubjectSuppressed.mapTypeAlias7",
                    "lib.VarianceSubjectSuppressed.mapTypeAlias8",
                )
        )
    }

    private fun assertKspSignaturesMatchKapt(
        kaptSignatures: Map<String, MethodSignature>,
        kspSignatures: Map<String, MethodSignature>,
        expectedFailingKeys: List<String> = listOf()
    ) {
        assertThat(kspSignatures.keys).containsExactlyElementsIn(kaptSignatures.keys)
        assertWithMessage("Found signatures in 'expectedFailingKeys' that don't exist")
            .that(kspSignatures.keys)
            .containsAtLeastElementsIn(expectedFailingKeys)

        val actualFailingKeys =
            kspSignatures.keys.filter { name -> kspSignatures[name] != kaptSignatures[name] }
        assertWithMessage("Found signatures in 'expectedFailingKeys' that aren't actually failing")
            .that(actualFailingKeys)
            .containsAtLeastElementsIn(expectedFailingKeys)

        assertWithMessage(
                "Found incorrect KSP signatures not listed in 'expectedFailingKeys': [\n\t${
                (actualFailingKeys - expectedFailingKeys).joinToString("\n\t")
            }\n]"
            )
            // We filter out the expected failing keys since we only want to report the unexpected.
            .that(kspSignatures.values.filterNot { expectedFailingKeys.contains(it.name) })
            .containsExactlyElementsIn(
                kaptSignatures.values.filterNot { expectedFailingKeys.contains(it.name) }
            )
    }

    private data class MethodSignature(
        val name: String,
        val returnType: TypeName,
        val parameterTypes: List<TypeName>
    ) {
        companion object {
            operator fun invoke(method: XMethodElement, owner: XTypeElement): MethodSignature {
                val methodType = method.asMemberOf(owner.type)
                return MethodSignature(
                    name = "${prefix(method, owner)}.${method.jvmName}",
                    returnType = methodType.returnType.typeName,
                    parameterTypes = methodType.parameterTypes.map { it.typeName }
                )
            }

            operator fun invoke(
                constructor: XConstructorElement,
                owner: XTypeElement
            ): MethodSignature {
                val constructorType = constructor.asMemberOf(owner.type)
                return MethodSignature(
                    name = "${prefix(constructor, owner)}.<init>",
                    returnType = TypeName.VOID,
                    parameterTypes = constructorType.parameterTypes.map { it.typeName }
                )
            }

            fun prefix(executable: XExecutableElement, owner: XTypeElement): String {
                val enclosingElement = executable.closestMemberContainer
                return if (owner == enclosingElement) {
                    owner.qualifiedName
                } else {
                    "${owner.qualifiedName}.${enclosingElement.asClassName().canonicalName}"
                }
            }
        }
    }

    private fun collectSignatures(
        invocation: XTestInvocation,
        subjects: List<String>
    ): List<MethodSignature> {
        // collect all methods in the Object class to exclude them from matching
        val objectMethodNames =
            invocation.processingEnv
                .requireTypeElement(Any::class)
                .getAllMethods()
                .map { it.jvmName }
                .toSet()

        return buildList<MethodSignature>() {
            subjects.forEach {
                val klass = invocation.processingEnv.requireTypeElement(it)
                klass.getConstructors().singleOrNull()?.let { constructor ->
                    add(MethodSignature(constructor, klass))
                }

                // KAPT might duplicate overridden methods. ignore them for test purposes
                // see b/205911014
                val declaredMethodNames = klass.getDeclaredMethods().map { it.jvmName }.toSet()
                val methods =
                    klass.getDeclaredMethods() +
                        klass.getAllMethods().filterNot { it.jvmName in declaredMethodNames }

                methods
                    .filterNot {
                        // remove these synthetics generated by kapt for property annotations
                        it.jvmName.contains("\$annotations") ||
                            objectMethodNames.contains(it.jvmName)
                    }
                    .forEach { method -> add(MethodSignature(method, klass)) }
            }
        }
    }

    private fun createSubjects(pkg: String): TestInput {
        val declarations =
            Source.kotlin(
                "declarations.kt",
                """
                package $pkg
                class MyType
                class MyGeneric<T>
                class MyGenericIn<in T>
                class MyGenericOut<out T>
                class MyGeneric2Parameters<in T1, out T2>
                class MyGenericMultipleParameters<T1: MyGeneric<*>, T2: MyGeneric<T1>>
                interface MyInterface
                typealias MyInterfaceAlias = MyInterface
                typealias MyGenericAlias = MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>
                typealias MyGenericOutAlias<T> = MyGenericOut<T>
                typealias MyGenericOutAliasWithJSW<T> = MyGenericOut<@JSW T>
                typealias MyGeneric2ParametersAlias<T1, T2> = MyGeneric2Parameters<MyGenericOut<T1>, MyGeneric<MyGenericOut<T2>>>
                typealias MyGeneric1ParameterAlias<T> = MyGeneric2Parameters<MyGenericOut<T>, MyGeneric<MyGenericOut<T>>>
                typealias MyLambdaAlias1 = (List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>) -> List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>
                typealias MyLambdaAlias2 = @JSW (List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>) -> List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>
                typealias MyLambdaAlias3 = (@JSW List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>) -> @JSW List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>
                typealias MyLambdaAlias4 = (List<@JSW MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>) -> List<@JSW MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>
                typealias MyLambdaAlias5 = (List<MyGenericIn<@JSW MyGenericOut<MyGenericOut<MyType>>>>) -> List<MyGenericIn<@JSW MyGenericOut<MyGenericOut<MyType>>>>
                typealias MyLambdaAlias6 = (List<MyGenericIn<MyGenericOut<@JSW MyGenericOut<MyType>>>>) -> List<MyGenericIn<MyGenericOut<@JSW MyGenericOut<MyType>>>>
                typealias MyLambdaAlias7 = (List<MyGenericIn<MyGenericOut<MyGenericOut<@JSW MyType>>>>) -> List<MyGenericIn<MyGenericOut<MyGenericOut<@JSW MyType>>>>
                typealias MySuspendLambdaAlias1 = suspend (List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>) -> List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>
                typealias MySuspendLambdaAlias2 = @JSW suspend (List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>) -> List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>
                typealias MySuspendLambdaAlias3 = suspend (@JSW List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>) -> @JSW List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>
                typealias MySuspendLambdaAlias4 = suspend (List<@JSW MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>) -> List<@JSW MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>>
                typealias MySuspendLambdaAlias5 = suspend (List<MyGenericIn<@JSW MyGenericOut<MyGenericOut<MyType>>>>) -> List<MyGenericIn<@JSW MyGenericOut<MyGenericOut<MyType>>>>
                typealias MySuspendLambdaAlias6 = suspend (List<MyGenericIn<MyGenericOut<@JSW MyGenericOut<MyType>>>>) -> List<MyGenericIn<MyGenericOut<@JSW MyGenericOut<MyType>>>>
                typealias MySuspendLambdaAlias7 = suspend (List<MyGenericIn<MyGenericOut<MyGenericOut<@JSW MyType>>>>) -> List<MyGenericIn<MyGenericOut<MyGenericOut<@JSW MyType>>>>
                typealias MySuspendLambdaAlias8 = List<suspend (List<MyGenericIn<MyGenericOut<MyGenericOut<@JSW MyType>>>>) -> List<MyGenericIn<MyGenericOut<MyGenericOut<@JSW MyType>>>>>
                typealias JSW = JvmSuppressWildcards
                typealias JW = JvmWildcard
                typealias MyLambdaTypeAlias = (@JvmWildcard MyType) -> @JvmWildcard MyType
                typealias MyMapAlias<T> = Map<Int, @JvmSuppressWildcards MyGeneric<@JvmSuppressWildcards T>>
                typealias MyMapAliasWithJSW<T> = Map<Int, @JSW MyGeneric<@JSW T>>
                @JvmInline value class MyInlineType(val value: MyType)
                enum class MyEnum {
                    VAL1,
                    VAL2;
                }
                sealed class GrandParentSealed {
                    class Parent1: GrandParentSealed()
                    sealed class Parent2: GrandParentSealed() {
                        class Child1: Parent2()
                        class Child2: Parent2()
                    }
                }
                """
                    .trimIndent()
            )
        return listOf(
                createBasicSubject(pkg),
                createVarianceSubject(
                    pkg = pkg,
                    className = "VarianceSubject",
                    suppressWildcards = false
                ),
                createVarianceSubject(
                    pkg,
                    className = "VarianceSubjectSuppressed",
                    suppressWildcards = true
                ),
                createOverrideSubjects(pkg),
                createSelfReferencingType(pkg),
                createOverridesWithGenericArguments(pkg),
                createKotlinOverridesJavaSubjects(pkg),
                createKotlinOverridesJavaSubjectsWithGenerics(pkg),
                createOverrideSubjectsWithDifferentSuppressLevels(pkg),
                createMultiLevelInheritanceSubjects(pkg),
                createJavaOverridesKotlinSubjects(pkg),
                createOverrideWithBoundsSubjects(pkg),
                createOverrideWithMultipleBoundsSubjects(pkg)
            )
            .fold(TestInput(declarations, emptyList())) { acc, next ->
                acc +
                    next.copy(
                        subjects = next.subjects.map { "$pkg.$it" } // add package
                    )
            }
    }

    /** Simple class, no overrides etc */
    private fun createBasicSubject(pkg: String): TestInput {
        return TestInput(
            Source.kotlin(
                "Foo.kt",
                """
                    package $pkg
                    class Subject {
                        fun method1():MyGeneric<MyType> = TODO()
                        fun method2(items: MyGeneric<in MyType>): MyType = TODO()
                        fun method3(items: MyGeneric<out MyType>): MyType = TODO()
                        fun method4(items: MyGeneric<MyType>): MyType = TODO()
                        fun method5(): MyGeneric<out MyType> = TODO()
                        fun method6(): MyGeneric<in MyType> = TODO()
                        fun method7(): MyGeneric<MyType> = TODO()
                        fun method8(): MyGeneric<*> = TODO()
                        fun method9(args : Array<Int>):Array<Array<String>> = TODO()
                        fun method10(args : Array<Array<Int>>):Array<String> = TODO()
                        fun method11(iter: Iterable<String>): Iterable<String> = TODO()
                        fun method12(iter: Iterable<Number>): Iterable<Number> = TODO()
                        fun method13(iter: Iterable<MyType>): Iterable<MyType> = TODO()
                        fun method14(list: List<MyLambdaTypeAlias>): MyLambdaTypeAlias = TODO()
                        fun method15(
                            list: List<MyGenericMultipleParameters<*, *>>
                        ): List<MyGenericMultipleParameters<*, *>> = TODO()
                        fun method16(
                            param: MyGenericIn<MyGeneric<MyGenericIn<MyGeneric<MyType>>>>
                        ): MyGenericIn<MyGeneric<MyGenericIn<MyGeneric<MyType>>>> = TODO()
                        fun method17(
                            param: MyGeneric<MyGeneric<MyGenericIn<MyGeneric<MyType>>>>
                        ): MyGeneric<MyGeneric<MyGenericIn<MyGeneric<MyType>>>> = TODO()
                        fun method18(
                            param: MyGeneric<in MyGeneric<MyGenericIn<MyGeneric<MyType>>>>
                        ): MyGeneric<in MyGeneric<MyGenericIn<MyGeneric<MyType>>>> = TODO()
                        fun method19(
                            param: MyGeneric<out MyGeneric<MyGenericIn<MyGeneric<MyType>>>>
                        ): MyGeneric<out MyGeneric<MyGenericIn<MyGeneric<MyType>>>> = TODO()
                        fun method20(
                            param: MyGeneric<MyGeneric<out MyGenericIn<MyGeneric<MyType>>>>
                        ): MyGeneric<MyGeneric<out MyGenericIn<MyGeneric<MyType>>>> = TODO()
                        fun method21(
                            param: MyGeneric<MyGeneric<in MyGenericIn<MyGeneric<MyType>>>>
                        ): MyGeneric<MyGeneric<in MyGenericIn<MyGeneric<MyType>>>> = TODO()
                        fun method22(
                            param: MyGenericIn<in MyGeneric<MyGenericIn<MyGeneric<MyType>>>>
                        ): MyGenericIn<in MyGeneric<MyGenericIn<MyGeneric<MyType>>>> = TODO()
                        fun method23(
                            param: MyGenericIn<MyGeneric<in MyGenericIn<MyGeneric<MyType>>>>
                        ): MyGenericIn<MyGeneric<in MyGenericIn<MyGeneric<MyType>>>> = TODO()
                        fun method24(
                            param: MyGenericOut<MyGeneric<MyGenericOut<MyGeneric<MyType>>>>
                        ): MyGenericOut<MyGeneric<MyGenericOut<MyGeneric<MyType>>>> = TODO()
                        fun method25(
                            param: MyGeneric<MyGeneric<MyGenericOut<MyGeneric<MyType>>>>
                        ): MyGeneric<MyGeneric<MyGenericOut<MyGeneric<MyType>>>> = TODO()
                        fun method26(
                            param: MyGeneric<in MyGeneric<MyGenericOut<MyGeneric<MyType>>>>
                        ): MyGeneric<in MyGeneric<MyGenericOut<MyGeneric<MyType>>>> = TODO()
                        fun method27(
                            param: MyGeneric<out MyGeneric<MyGenericOut<MyGeneric<MyType>>>>
                        ): MyGeneric<out MyGeneric<MyGenericOut<MyGeneric<MyType>>>> = TODO()
                        fun method28(
                            param: MyGeneric<MyGeneric<out MyGenericOut<MyGeneric<MyType>>>>
                        ): MyGeneric<MyGeneric<out MyGenericOut<MyGeneric<MyType>>>> = TODO()
                        fun method29(
                            param: MyGeneric<MyGeneric<in MyGenericOut<MyGeneric<MyType>>>>
                        ): MyGeneric<MyGeneric<in MyGenericOut<MyGeneric<MyType>>>> = TODO()
                        fun method30(
                            param: MyGenericOut<out MyGeneric<MyGenericOut<MyGeneric<MyType>>>>
                        ): MyGenericOut<out MyGeneric<MyGenericOut<MyGeneric<MyType>>>> = TODO()
                        fun method31(
                            param: MyGenericOut<MyGeneric<out MyGenericOut<MyGeneric<MyType>>>>
                        ): MyGenericOut<MyGeneric<out MyGenericOut<MyGeneric<MyType>>>> = TODO()
                        fun method32(
                            param: MyGenericOutAlias<MyInterface>
                        ): MyGenericOutAlias<MyInterface> = TODO()
                        fun method33(
                            param: MyGenericOut<MyGenericOutAlias<MyInterface>>
                        ): MyGenericOut<MyGenericOutAlias<MyInterface>> = TODO()
                        fun method34(
                            param: MyGenericIn<MyGenericOutAlias<MyInterface>>
                        ): MyGenericIn<MyGenericOutAlias<MyInterface>> = TODO()
                        fun method35(
                            param: MyGenericOutAlias<MyGenericOut<MyInterface>>
                        ): MyGenericOutAlias<MyGenericOut<MyInterface>> = TODO()
                        fun method36(
                            param: MyGenericIn<MyGenericOutAlias<MyGenericOut<MyInterface>>>
                        ): MyGenericIn<MyGenericOutAlias<MyGenericOut<MyInterface>>> = TODO()
                        fun method37(
                            param: MyGenericIn<MyGenericOutAlias<MyGenericIn<MyInterface>>>
                        ): MyGenericIn<MyGenericOutAlias<MyGenericIn<MyInterface>>> = TODO()
                        fun method38(
                            param: MyGenericOutAliasWithJSW<MyInterface>
                        ): MyGenericOutAliasWithJSW<MyInterface> = TODO()
                        fun method39(
                            param: MyGenericOut<MyGenericOutAliasWithJSW<MyInterface>>
                        ): MyGenericOut<MyGenericOutAliasWithJSW<MyInterface>> = TODO()
                        fun method40(
                            param: MyGenericIn<MyGenericOutAliasWithJSW<MyInterface>>
                        ): MyGenericIn<MyGenericOutAliasWithJSW<MyInterface>> = TODO()
                        fun method41(
                            param: MyGenericOutAliasWithJSW<MyGenericOut<MyInterface>>
                        ): MyGenericOutAliasWithJSW<MyGenericOut<MyInterface>> = TODO()
                        fun method42(
                            param: MyGenericIn<MyGenericOutAliasWithJSW<MyGenericOut<MyInterface>>>
                        ): MyGenericIn<MyGenericOutAliasWithJSW<MyGenericOut<MyInterface>>> = TODO()
                        fun method43(
                            param: MyGenericIn<MyGenericOutAliasWithJSW<MyGenericIn<MyInterface>>>
                        ): MyGenericIn<MyGenericOutAliasWithJSW<MyGenericIn<MyInterface>>> = TODO()
                        fun method44(
                            param: MyGenericOutAliasWithJSW<MyType>
                        ): MyGenericOutAliasWithJSW<MyType> = TODO()
                        fun method45(
                            param: MyGenericOut<MyGenericOutAliasWithJSW<MyType>>
                        ): MyGenericOut<MyGenericOutAliasWithJSW<MyType>> = TODO()
                        fun method46(
                            param: MyGenericIn<MyGenericOutAliasWithJSW<MyType>>
                        ): MyGenericIn<MyGenericOutAliasWithJSW<MyType>> = TODO()
                        fun method47(
                            param: MyGeneric2ParametersAlias<MyType, MyType>
                        ): MyGeneric2ParametersAlias<MyType, MyType> = TODO()
                        fun method48(
                            param: MyGenericOut<MyGeneric2ParametersAlias<MyType, MyType>>
                        ): MyGenericOut<MyGeneric2ParametersAlias<MyType, MyType>> = TODO()
                        fun method49(
                            param: MyGenericIn<MyGeneric2ParametersAlias<MyType, MyType>>
                        ): MyGenericIn<MyGeneric2ParametersAlias<MyType, MyType>> = TODO()
                        fun method50(
                            param: MyGeneric2ParametersAlias<MyGenericOut<MyType>, MyGenericIn<MyType>>
                        ): MyGeneric2ParametersAlias<MyGenericOut<MyType>, MyGenericIn<MyType>> = TODO()
                        fun method51(
                            param: MyGenericOut<MyGeneric2ParametersAlias<MyGenericOut<MyType>, MyGenericIn<MyType>>>
                        ): MyGenericOut<MyGeneric2ParametersAlias<MyGenericOut<MyType>, MyGenericIn<MyType>>> = TODO()
                        fun method52(
                            param: MyGenericIn<MyGeneric2ParametersAlias<MyGenericOut<MyType>, MyGenericIn<MyType>>>
                        ): MyGenericIn<MyGeneric2ParametersAlias<MyGenericOut<MyType>, MyGenericIn<MyType>>> = TODO()
                        @JvmName("method53") // Needed to prevent obfuscation due to inline type.
                        fun method53(param: MyInlineType): MyInlineType = TODO()
                        fun method54(param: MyGenericOut<MyInlineType>): MyGenericOut<MyInlineType> = TODO()
                        fun method55(param: MyGenericIn<MyInlineType>): MyGenericIn<MyInlineType> = TODO()
                        fun method56(
                            param: MyGeneric<out MyGeneric<out MyGenericOut<MyInterface>>>
                        ): MyGeneric<out MyGeneric<out MyGenericOut<MyInterface>>> = TODO()
                        fun method57(
                            param: MyGeneric<out MyGeneric<MyGenericOut<MyInterface>>>
                        ): MyGeneric<out MyGeneric<MyGenericOut<MyInterface>>> = TODO()
                        fun method58(
                            param: MyGeneric<out MyGenericOut<MyGenericOut<MyInterface>>>
                        ): MyGeneric<out MyGenericOut<MyGenericOut<MyInterface>>> = TODO()
                        fun method59(
                            param: MyGeneric<suspend () -> Unit>
                        ): MyGeneric<suspend () -> Unit> = TODO()
                        fun method60(
                            param: MyGenericIn<suspend () -> Unit>
                        ): MyGenericIn<suspend () -> Unit> = TODO()
                        fun method61(
                            param: MyGenericOut<suspend () -> Unit>
                        ): MyGenericOut<suspend () -> Unit> = TODO()
                        fun method62(
                            param: MyGenericOut<suspend (MyGenericOut<suspend () -> Unit>) -> MyGenericOut<suspend () -> Unit>>
                        ): MyGenericOut<suspend (MyGenericOut<suspend () -> Unit>) -> MyGenericOut<suspend () -> Unit>> = TODO()
                    }
                """
                    .trimIndent()
            ),
            listOf("Subject")
        )
    }

    private fun createSelfReferencingType(pkg: String): TestInput {
        return TestInput(
            sources =
                listOf(
                    Source.java(
                        "$pkg.SelfReferencingJava",
                        """
                    package $pkg;
                    public interface SelfReferencingJava<T extends SelfReferencingJava<T>> {
                        void method1(SelfReferencingJava sr);
                        void method2(SelfReferencingJava<?> sr);
                    }
                """
                            .trimIndent()
                    ),
                    Source.java(
                        "$pkg.SubSelfReferencingJava",
                        """
                    package $pkg;
                    public interface SubSelfReferencingJava extends SelfReferencingJava<SubSelfReferencingJava> {}
                """
                            .trimIndent()
                    ),
                    Source.kotlin(
                        "SelfReferencing.kt",
                        """
                    package $pkg
                    interface SelfReferencingKotlin<T : SelfReferencingKotlin<T>> {
                        fun method1(sr: SelfReferencingKotlin<*>)
                    }
                    interface SubSelfReferencingKotlin : SelfReferencingKotlin<SubSelfReferencingKotlin>
                    """
                            .trimIndent()
                    ),
                ),
            subjects =
                listOf(
                    "SubSelfReferencingJava",
                    "SelfReferencingJava",
                    "SelfReferencingKotlin",
                    "SubSelfReferencingKotlin"
                )
        )
    }

    /** Subjects with variance and star projections. */
    private fun createVarianceSubject(
        pkg: String,
        className: String,
        suppressWildcards: Boolean
    ): TestInput {
        val annotation =
            if (suppressWildcards) {
                "@JvmSuppressWildcards"
            } else {
                ""
            }
        return TestInput(
            Source.kotlin(
                "$className.kt",
                """
                package $pkg
                $annotation
                class $className<R>(
                    val valStarList: List<*>,
                    val valTypeArgList: List<R>,
                    val valNumberList: List<Number>,
                    val valStringList: List<String>,
                    val valEnumList: List<MyEnum>,
                    val valSealedListGrandParent: List<GrandParentSealed>,
                    val valSealedListParent: List<GrandParentSealed.Parent1>,
                    val valSealedListChild: List<GrandParentSealed.Parent2.Child1>,
                    val valJvmWildcard: List<@JvmWildcard String>,
                    val valSuppressJvmWildcard: List<@JvmSuppressWildcards Number>,
                    val valSuppressJvmWildcardsGeneric1: @JvmSuppressWildcards List<MyGenericOut<MyGenericIn<MyGeneric<MyType>>>>,
                    val valSuppressJvmWildcardsGeneric2: List<@JvmSuppressWildcards MyGenericOut<MyGenericIn<MyGeneric<MyType>>>>,
                    val valSuppressJvmWildcardsGeneric3: List<MyGenericOut<@JvmSuppressWildcards MyGenericIn<MyGeneric<MyType>>>>,
                    val valSuppressJvmWildcardsGeneric4: List<MyGenericOut<MyGenericIn<@JvmSuppressWildcards MyGeneric<MyType>>>>,
                    val valInterfaceAlias: List<MyInterfaceAlias>,
                    val valGenericAlias: List<MyGenericAlias>,
                    val valJvmWildcardTypeAlias: List<@JW String>,
                    val valSuppressJvmWildcardTypeAlias: List<@JSW Number>,
                    val valMyMapAliasR: MyMapAlias<R>,
                    val valMyMapAliasMyInterface: MyMapAlias<MyInterface>,
                ) {
                    var varPropWithFinalType: String = ""
                    var varPropWithOpenType: Number = 3
                    var varPropWithFinalGeneric: List<String> = TODO()
                    var varPropWithOpenGeneric: List<Number> = TODO()
                    var varPropWithTypeArg: R = TODO()
                    var varPropWithTypeArgGeneric: List<R> = TODO()
                    var varPropSealedListGrandParent: List<GrandParentSealed> = TODO()
                    var varPropSealedListParent: List<GrandParentSealed.Parent1> = TODO()
                    var varPropSealedListChild: List<GrandParentSealed.Parent2.Child1> = TODO()
                    @JvmSuppressWildcards
                    var varPropWithOpenTypeButSuppressAnnotation: Number = 3
                    var varGeneric: List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>> = TODO()
                    @JvmSuppressWildcards var varSuppressJvmWildcardsGeneric1: List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>> = TODO()
                    var varSuppressJvmWildcardsGeneric2: @JvmSuppressWildcards List<MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>> = TODO()
                    var varSuppressJvmWildcardsGeneric3: List<@JvmSuppressWildcards MyGenericIn<MyGenericOut<MyGenericOut<MyType>>>> = TODO()
                    var varSuppressJvmWildcardsGeneric4: List<MyGenericIn<@JvmSuppressWildcards MyGenericOut<MyGenericOut<MyType>>>> = TODO()
                    var varSuppressJvmWildcardsGeneric5: List<MyGenericIn<MyGenericOut<@JvmSuppressWildcards MyGenericOut<MyType>>>> = TODO()
                    var varInterfaceAlias: List<MyInterfaceAlias> = TODO()
                    var varGenericAlias: List<MyGenericAlias> = TODO()
                    var varJvmWildcardTypeAlias: List<@JW String> = TODO()
                    var varSuppressJvmWildcardTypeAlias: List<@JSW Number> = TODO()
                    var varMyMapAliasR: MyMapAlias<R> = TODO()
                    var varMyMapAliasMyInterface: MyMapAlias<MyInterface> = TODO()
                    fun list(list: List<*>): List<*> { TODO() }
                    fun listTypeArg(list: List<R>): List<R> { TODO() }
                    fun listTypeArgNumber(list: List<Number>): List<Number> { TODO() }
                    fun listTypeArgString(list: List<String>): List<String> { TODO() }
                    fun listTypeArgEnum(list: List<MyEnum>): List<MyEnum> { TODO() }
                    fun listSealedListGrandParent(list: List<GrandParentSealed>): List<GrandParentSealed> { TODO() }
                    fun listSealedListParent(list: List<GrandParentSealed.Parent1>): List<GrandParentSealed.Parent1> { TODO() }
                    fun listSealedListChild(list: List<GrandParentSealed.Parent2.Child1>): List<GrandParentSealed.Parent2.Child1> { TODO() }
                    fun explicitOutOnInvariant_onType1(
                        list: MyGeneric<out MyGeneric<MyType>>
                    ): MyGeneric<out MyGeneric<MyType>> { TODO() }
                    fun explicitOutOnInvariant_onType2(
                        list: MyGeneric<MyGeneric<out MyType>>
                    ): MyGeneric<MyGeneric<out MyType>> { TODO() }
                    fun explicitJvmWildcard(
                        list: List<@JvmWildcard String>
                    ): List<@JvmWildcard String> { TODO() }
                    fun explicitJvmSuppressWildcard_OnType(
                        list: List<@JvmSuppressWildcards Number>
                    ): List<@JvmSuppressWildcards Number> { TODO() }
                    fun explicitJvmSuppressWildcard_OnType2(
                        list: @JvmSuppressWildcards List<Number>
                    ): @JvmSuppressWildcards List<Number> { TODO() }
                    fun suspendList(list: List<*>): List<*> { TODO() }
                    fun suspendListTypeArg(list: List<R>): List<R> { TODO() }
                    fun suspendListTypeArgNumber(list: List<Number>): List<Number> { TODO() }
                    fun suspendListTypeArgString(list: List<String>): List<String> { TODO() }
                    fun suspendListTypeArgEnum(list: List<MyEnum>): List<MyEnum> { TODO() }
                    fun suspendListSealedListGrandParent(list: List<GrandParentSealed>): List<GrandParentSealed> { TODO() }
                    fun suspendListSealedListParent(list: List<GrandParentSealed.Parent1>): List<GrandParentSealed.Parent1> { TODO() }
                    fun suspendListSealedListChild(list: List<GrandParentSealed.Parent2.Child1>): List<GrandParentSealed.Parent2.Child1> { TODO() }
                    fun suspendExplicitJvmWildcard(
                        list: List<@JvmWildcard String>
                    ): List<@JvmWildcard String> { TODO() }
                    fun suspendExplicitJvmSuppressWildcard_OnType(
                        list: List<@JvmSuppressWildcards Number>
                    ): List<@JvmSuppressWildcards Number> { TODO() }
                    fun suspendExplicitJvmSuppressWildcard_OnType2(
                        list: @JvmSuppressWildcards List<Number>
                    ): @JvmSuppressWildcards List<Number> { TODO() }
                    fun interfaceAlias(
                        param: List<MyInterfaceAlias>
                    ): List<MyInterfaceAlias> = TODO()
                    fun explicitJvmSuppressWildcardsOnAlias(
                        param: List<@JvmSuppressWildcards MyInterfaceAlias>,
                    ): List<@JvmSuppressWildcards MyInterfaceAlias> = TODO()
                    fun genericAlias(param: List<MyGenericAlias>): List<MyGenericAlias> = TODO()
                    fun explicitJvmSuppressWildcardsOnGenericAlias(
                        param: List<@JvmSuppressWildcards MyGenericAlias>,
                    ): List<@JvmSuppressWildcards MyGenericAlias> = TODO()
                    fun explicitOutOnInvariant_onType1_WithExplicitJvmSuppressWildcardAlias(
                        list: @JSW MyGeneric<out MyGeneric<MyType>>
                    ): @JSW MyGeneric<out MyGeneric<MyType>> { TODO() }
                    fun explicitOutOnInvariant_onType2_WithExplicitJvmSuppressWildcardAlias(
                        list: @JSW MyGeneric<MyGeneric<out MyType>>
                    ): @JSW MyGeneric<MyGeneric<out MyType>> { TODO() }
                    fun explicitOutOnVariant_onType1(
                        list: List<out List<Number>>
                    ): List<out List<Number>> { TODO() }
                    fun explicitOutOnVariant_onType2(
                        list: List<List<out Number>>
                    ): List<List<out Number>> { TODO() }
                    fun explicitOutOnVariant_onType1_WithExplicitJvmSuppressWildcardAlias(
                        list: @JSW List<out List<Number>>
                    ): @JSW List<out List<Number>> { TODO() }
                    fun explicitOutOnVariant_onType2_WithExplicitJvmSuppressWildcardAlias(
                        list: @JSW List<List<out Number>>
                    ): @JSW List<List<out Number>> { TODO() }
                    fun explicitJvmWildcardTypeAlias(
                        list: List<@JW String>
                    ): List<@JW String> { TODO() }
                    fun explicitJvmSuppressWildcardTypeAlias_OnType(
                        list: List<@JSW Number>
                    ): List<@JSW Number> { TODO() }
                    fun explicitJvmSuppressWildcardTypeAlias_OnType2(
                        list: @JSW List<Number>
                    ): @JSW List<Number> { TODO() }
                    fun lambda1(param: MyLambdaAlias1): MyLambdaAlias1 = TODO()
                    fun lambda2(param: MyLambdaAlias2): MyLambdaAlias2 = TODO()
                    fun lambda3(param: MyLambdaAlias3): MyLambdaAlias3 = TODO()
                    fun lambda4(param: MyLambdaAlias4): MyLambdaAlias4 = TODO()
                    fun lambda5(param: MyLambdaAlias5): MyLambdaAlias5 = TODO()
                    fun lambda6(param: MyLambdaAlias6): MyLambdaAlias6 = TODO()
                    fun lambda7(param: MyLambdaAlias7): MyLambdaAlias7 = TODO()
                    @JSW fun lambda1WithJSW(param: MyLambdaAlias1): MyLambdaAlias1 = TODO()
                    @JSW fun lambda2WithJSW(param: MyLambdaAlias2): MyLambdaAlias2 = TODO()
                    @JSW fun lambda3WithJSW(param: MyLambdaAlias3): MyLambdaAlias3 = TODO()
                    @JSW fun lambda4WithJSW(param: MyLambdaAlias4): MyLambdaAlias4 = TODO()
                    @JSW fun lambda5WithJSW(param: MyLambdaAlias5): MyLambdaAlias5 = TODO()
                    @JSW fun lambda6WithJSW(param: MyLambdaAlias6): MyLambdaAlias6 = TODO()
                    @JSW fun lambda7WithJSW(param: MyLambdaAlias7): MyLambdaAlias7 = TODO()
                    fun suspendLambda1(param: MySuspendLambdaAlias1): MySuspendLambdaAlias1 = TODO()
                    fun suspendLambda2(param: MySuspendLambdaAlias2): MySuspendLambdaAlias2 = TODO()
                    fun suspendLambda3(param: MySuspendLambdaAlias3): MySuspendLambdaAlias3 = TODO()
                    fun suspendLambda4(param: MySuspendLambdaAlias4): MySuspendLambdaAlias4 = TODO()
                    fun suspendLambda5(param: MySuspendLambdaAlias5): MySuspendLambdaAlias5 = TODO()
                    fun suspendLambda6(param: MySuspendLambdaAlias6): MySuspendLambdaAlias6 = TODO()
                    fun suspendLambda7(param: MySuspendLambdaAlias7): MySuspendLambdaAlias7 = TODO()
                    fun suspendLambda8(param: MySuspendLambdaAlias8): MySuspendLambdaAlias8 = TODO()
                    @JSW fun suspendLambda1WithJSW(param: MySuspendLambdaAlias1): MySuspendLambdaAlias1 = TODO()
                    @JSW fun suspendLambda2WithJSW(param: MySuspendLambdaAlias2): MySuspendLambdaAlias2 = TODO()
                    @JSW fun suspendLambda3WithJSW(param: MySuspendLambdaAlias3): MySuspendLambdaAlias3 = TODO()
                    @JSW fun suspendLambda4WithJSW(param: MySuspendLambdaAlias4): MySuspendLambdaAlias4 = TODO()
                    @JSW fun suspendLambda5WithJSW(param: MySuspendLambdaAlias5): MySuspendLambdaAlias5 = TODO()
                    @JSW fun suspendLambda6WithJSW(param: MySuspendLambdaAlias6): MySuspendLambdaAlias6 = TODO()
                    @JSW fun suspendLambda7WithJSW(param: MySuspendLambdaAlias7): MySuspendLambdaAlias7 = TODO()
                    @JSW fun suspendLambda8WithJSW(param: MySuspendLambdaAlias8): MySuspendLambdaAlias8 = TODO()
                    fun mapTypeAlias1(param: MyMapAlias<R>): MyMapAlias<R> = TODO()
                    fun mapTypeAlias2(param: MyMapAlias<MyInterface>): MyMapAlias<MyInterface> = TODO()
                    fun mapTypeAlias3(param: MyGeneric<MyMapAlias<R>>): MyGeneric<MyMapAlias<R>> = TODO()
                    fun mapTypeAlias4(param: MyGeneric<MyMapAlias<MyInterface>>): MyGeneric<MyMapAlias<MyInterface>> = TODO()
                    fun mapTypeAlias5(param: MyGeneric<MyMapAlias<MyGeneric<R>>>): MyGeneric<MyMapAlias<MyGeneric<R>>> = TODO()
                    fun mapTypeAlias6(param: MyGeneric<MyMapAlias<MyGeneric<MyInterface>>>): MyGeneric<MyMapAlias<MyGeneric<MyInterface>>> = TODO()
                    fun mapTypeAlias7(param: MyMapAliasWithJSW<R>): MyMapAliasWithJSW<R> = TODO()
                    fun mapTypeAlias8(param: MyMapAliasWithJSW<MyInterface>): MyMapAliasWithJSW<MyInterface> = TODO()
                    fun mapTypeAlias9(param: MyGeneric<MyMapAliasWithJSW<R>>): MyGeneric<MyMapAliasWithJSW<R>> = TODO()
                    fun mapTypeAlias10(param: MyGeneric<MyMapAliasWithJSW<MyInterface>>): MyGeneric<MyMapAliasWithJSW<MyInterface>> = TODO()
                    fun mapTypeAlias11(param: MyGeneric<MyMapAliasWithJSW<MyGeneric<R>>>): MyGeneric<MyMapAliasWithJSW<MyGeneric<R>>> = TODO()
                    fun mapTypeAlias12(param: MyGeneric<MyMapAliasWithJSW<MyGeneric<MyInterface>>>): MyGeneric<MyMapAliasWithJSW<MyGeneric<MyInterface>>> = TODO()
                }
                """
                    .trimIndent()
            ),
            listOf(className)
        )
    }

    /** Create test cases where inheritance and wildcards annotations are involved */
    private fun createOverrideSubjectsWithDifferentSuppressLevels(pkg: String): TestInput {
        val source =
            Source.kotlin(
                "OverridesWithSuppress.kt",
                """
            package $pkg
            interface NormalBase<T> {
                fun receiveReturnListT(t : List<T>): List<T>
                fun receiveReturnListTWithWildcard(t : @JvmWildcard List<T>): List<@JvmWildcard T>
            }
            @JvmSuppressWildcards
            interface SuppressedBase<T> {
                fun receiveReturnListT(t : List<T>): List<T>
            }
            @JvmSuppressWildcards
            interface SuppressedInMethodBase<T> {
                @JvmSuppressWildcards
                fun receiveReturnListT(t : List<T>): List<T>
            }
            interface NoOverrideNormal: NormalBase<Number>
            interface NoOverrideSuppressedBase: SuppressedBase<Number>
            interface NoOverrideSuppressedInMethodBase: SuppressedInMethodBase<Number>

            @JvmSuppressWildcards
            interface SuppressInInterface: NormalBase<Number>

            interface OverrideWithSuppress: NormalBase<Number> {
                @JvmSuppressWildcards
                override fun receiveReturnListT(t : List<Number>): List<Number>
            }

            interface OverrideWildcardMethod: NormalBase<Number> {
                override fun receiveReturnListTWithWildcard(t : List<Number>): List<Number>
            }

            @JvmSuppressWildcards
            interface OverrideWildcardMethodSuppressed: NormalBase<Number> {
                override fun receiveReturnListTWithWildcard(t : List<Number>): List<Number>
            }
            """
                    .trimIndent()
            )
        return TestInput(
            source,
            listOf(
                "NoOverrideNormal",
                "NoOverrideSuppressedBase",
                "NoOverrideSuppressedInMethodBase",
                "SuppressInInterface",
                "OverrideWithSuppress",
                "OverrideWildcardMethod",
                "OverrideWildcardMethodSuppressed"
            )
        )
    }

    /** Create test cases where interfaces are overridden by other interfaces */
    private fun createOverrideSubjects(pkg: String): TestInput {
        val base =
            """
            interface BaseInterface<T> {
                var tProp: T
                var tListProp: List<T>
                var definedProp: Number
                var definedPropList: List<Number>
                var definedPropFinal: String
                var definedPropListFinal: List<String>
                fun receiveReturnT(t: T): T
                suspend fun suspendReceiveReturnT(t:T):T
                fun receiveReturnTList(t:List<T>): List<T>
                suspend fun suspendReceiveReturnTList(t:List<T>): List<T>
                fun receiveReturnTOverridden(t: T): T
                suspend fun suspendReceiveReturnTOverridden(t:T):T
                fun receiveReturnTListOverridden(t:List<T>): List<T>
                suspend fun suspendReceiveReturnTListOverridden(t:List<T>): List<T>
                // b/217210973
                suspend fun suspendDefinedTypesFinal(objList: List<Long>): List<Long>
                suspend fun suspendDefinedTypesOpen(objList: List<Number>): List<Number>
                suspend fun suspendDefinedTypesFinal2(objList: List<List<Long>>): List<List<Long>>
                suspend fun suspendDefinedTypesOpen2(objList: List<List<Number>>): List<List<Number>>
            }
        """
                .trimIndent()

        fun buildInterface(name: String, typeArg: String) =
            """
            interface $name: BaseInterface<$typeArg> {
                override fun receiveReturnTOverridden(t: $typeArg): $typeArg
                override suspend fun suspendReceiveReturnTOverridden(t:$typeArg):$typeArg
                override fun receiveReturnTListOverridden(t:List<$typeArg>): List<$typeArg>
                override suspend fun suspendReceiveReturnTListOverridden(t:List<$typeArg>): List<$typeArg>
            }
        """
                .trimIndent()

        val code =
            listOf(
                    "package $pkg",
                    base,
                    buildInterface("SubInterface<R>", "R"),
                    buildInterface("SubInterfaceOpen", "Number"),
                    buildInterface("SubInterfaceEnum", "MyEnum"),
                    buildInterface("SubInterfaceFinal", "String"),
                    buildInterface("SubInterfacePrimitive", "Int"),
                    buildInterface("SubInterfacePrimitiveNullable", "Int?"),
                )
                .joinToString("\n")
        val classNames =
            listOf(
                "BaseInterface",
                "SubInterface",
                "SubInterfaceOpen",
                "SubInterfaceEnum",
                "SubInterfaceFinal",
                "SubInterfacePrimitive",
                "SubInterfacePrimitiveNullable",
            )
        return TestInput(Source.kotlin("Overrides.kt", code), classNames)
    }

    /**
     * Create test cases where interfaces are overridden by other interfaces and the type argument
     * has a generic type
     */
    private fun createOverridesWithGenericArguments(pkg: String): TestInput {
        // this is not included in buildOverrides case because we don't yet properly handle
        // return type getting variance. see:
        // see: https://issuetracker.google.com/issues/204415667#comment10
        val base =
            """
            interface GenericBaseInterface<T>  {
                fun receiveT(t:T): Unit
                suspend fun suspendReceiveT(t:T): Unit
                fun receiveTList(t:List<T>): Unit
                suspend fun suspendReceiveTList(t:List<T>): Unit
                fun receiveTArray(t: Array<T>): Unit
                suspend fun suspendReceiveTArray(t: Array<T>): Unit
                fun receiveTOverridden(t:T): Unit
                suspend fun suspendTOverridden(t:T): Unit
                fun receiveTListOverridden(t:List<T>): Unit
                suspend fun suspendReceiveTListOverridden(t:List<T>): Unit
                fun receiveTArrayOverridden(t: Array<T>): Unit
                suspend fun suspendReceiveTArrayOverridden(t: Array<T>): Unit
            }
        """
                .trimIndent()

        fun buildInterface(name: String, typeArg: String) =
            """
            interface $name: GenericBaseInterface<$typeArg> {
                override fun receiveTOverridden(t:$typeArg): Unit
                override suspend fun suspendTOverridden(t:$typeArg): Unit
                override fun receiveTListOverridden(t:List<$typeArg>): Unit
                override suspend fun suspendReceiveTListOverridden(t:List<$typeArg>): Unit
                override fun receiveTArrayOverridden(t: Array<$typeArg>): Unit
                override suspend fun suspendReceiveTArrayOverridden(t: Array<$typeArg>): Unit
            }
        """
                .trimIndent()

        val code =
            listOf(
                    "package $pkg",
                    base,
                    buildInterface("SubInterfaceWithGenericR<R>", "List<R>"),
                    buildInterface("SubInterfaceWithGenericOpen", "List<Number>"),
                    buildInterface("SubInterfaceWithGenericEnum", "List<MyEnum>"),
                    buildInterface("SubInterfaceWithGenericFinal", "List<String>"),
                    buildInterface("SubInterfaceWithGenericPrimitive", "List<Int>"),
                    buildInterface("SubInterfaceWithGenericPrimitiveNullable", "List<Int?>"),
                    buildInterface("SubInterfaceWithGenericArray", "Array<Number>"),
                    buildInterface("SubInterfaceWithGenericArrayPrimitive", "Array<IntArray>"),
                )
                .joinToString("\n")
        val classNames =
            listOf(
                "GenericBaseInterface",
                "SubInterfaceWithGenericR",
                "SubInterfaceWithGenericOpen",
                "SubInterfaceWithGenericEnum",
                "SubInterfaceWithGenericFinal",
                "SubInterfaceWithGenericPrimitive",
                "SubInterfaceWithGenericPrimitiveNullable",
                "SubInterfaceWithGenericArray",
                "SubInterfaceWithGenericArrayPrimitive"
            )
        return TestInput(Source.kotlin("OverridesWithGenerics.kt", code), classNames)
    }

    /** Create test cases where kotlin overrides a java interface */
    private fun createKotlinOverridesJavaSubjects(pkg: String): TestInput {
        val javaBase =
            Source.java(
                "$pkg.BaseJavaInterface",
                """
            package $pkg;
            import java.util.List;
            public interface BaseJavaInterface<T> {
                T receiveReturnT(T t);
                List<T> receiveReturnTList(List<T> t);
                T receiveReturnTOverridden(T t);
                List<T> receiveReturnTListOverridden(List<T> t);
            }
            """
            )

        fun buildInterface(name: String, typeArg: String) =
            """
            public interface $name: BaseJavaInterface<$typeArg> {
                override fun receiveReturnTOverridden(t: $typeArg): $typeArg
                override fun receiveReturnTListOverridden(t:List<$typeArg>): List<$typeArg>
            }
        """
                .trimIndent()

        val subInterfaces =
            listOf(
                    "package $pkg",
                    buildInterface("SubInterfaceOverridingJava<R>", "R"),
                    buildInterface("SubInterfaceOverridingJavaOpen", "Number"),
                    buildInterface("SubInterfaceOverridingJavaEnum", "MyEnum"),
                    buildInterface("SubInterfaceOverridingJavaFinal", "String"),
                    buildInterface("SubInterfaceOverridingJavaPrimitive", "Int"),
                    buildInterface("SubInterfaceOverridingJavaPrimitiveNullable", "Int?"),
                )
                .joinToString("\n")
        val sources = listOf(javaBase, Source.kotlin("kotlinOverridingJava.kt", subInterfaces))
        return TestInput(
            sources,
            listOf(
                "BaseJavaInterface",
                "SubInterfaceOverridingJavaOpen",
                "SubInterfaceOverridingJavaEnum",
                "SubInterfaceOverridingJavaFinal",
                "SubInterfaceOverridingJavaPrimitive",
                "SubInterfaceOverridingJavaPrimitiveNullable",
            )
        )
    }

    /**
     * Create test cases where Kotlin overrides a java interface and the type argument includes
     * another generic.
     */
    private fun createKotlinOverridesJavaSubjectsWithGenerics(pkg: String): TestInput {
        val javaBase =
            Source.java(
                "$pkg.BaseGenericJavaInterface",
                """
            package $pkg;
            import java.util.List;
            public interface BaseGenericJavaInterface<T> {
                void receiveT(T t);
                void receiveTList(List<T> t);
                void receiveTOverridden(T t);
                void receiveTListOverridden(List<T> t);
            }
            """
            )

        fun buildInterface(name: String, typeArg: String) =
            """
            interface $name: BaseGenericJavaInterface<$typeArg> {
                override fun receiveTOverridden(t:$typeArg): Unit
                override fun receiveTListOverridden(t:List<$typeArg>): Unit
            }
        """
                .trimIndent()

        val subInterfaces =
            listOf(
                    "package $pkg",
                    buildInterface("SubInterfaceOverridingJavaWithGenericR<R>", "List<R>"),
                    buildInterface("SubInterfaceOverridingJavaWithGenericOpen", "List<Number>"),
                    buildInterface("SubInterfaceOverridingJavaWithGenericEnum", "List<MyEnum>"),
                    buildInterface("SubInterfaceOverridingJavaWithGenericFinal", "List<String>"),
                    buildInterface(
                        "SubInterfaceOverridingJavaWithGenericPrimitive",
                        "List<Number>"
                    ),
                    buildInterface(
                        "SubInterfaceOverridingJavaWithGenericPrimitiveNullable",
                        "List<Number?>"
                    )
                )
                .joinToString("\n")
        val sources =
            listOf(javaBase, Source.kotlin("kotlinOverridingJavaWithGenerics.kt", subInterfaces))
        return TestInput(
            sources,
            listOf(
                "SubInterfaceOverridingJavaWithGenericR",
                "SubInterfaceOverridingJavaWithGenericOpen",
                "SubInterfaceOverridingJavaWithGenericEnum",
                "SubInterfaceOverridingJavaWithGenericFinal",
                "SubInterfaceOverridingJavaWithGenericPrimitive",
                "SubInterfaceOverridingJavaWithGenericPrimitiveNullable",
            )
        )
    }

    private fun createMultiLevelInheritanceSubjects(pkg: String): TestInput {
        val base =
            """
            interface MultiLevelBaseInterface<T> {
                fun receiveReturnT(t : T): T
                suspend fun suspendReceiveReturnT(t : T): T
                fun receiveReturnTList(t : List<T>): List<T>
                suspend fun suspendReceiveReturnTList(t : List<T>): List<T>
                fun receiveReturnTOverriddenInSub(t : T): T
                suspend fun suspendReceiveReturnTOverriddenInSub(t : T): T
            }
            interface MultiLevelSubInterface<R>: MultiLevelBaseInterface<String> {
                fun subReceiveReturnT(r : R): R
                suspend fun subSuspendReceiveReturnT(r : R): R
                override fun receiveReturnTOverriddenInSub(t : String): String
                override suspend fun suspendReceiveReturnTOverriddenInSub(t : String): String
                fun subReceiveReturnROverridden(r : R): R
                suspend fun subSuspendReceiveReturnROverridden(r : R): R
                fun subReceiveReturnRListOverridden(r : List<R>): List<R>
                suspend fun subSuspendReceiveReturnRListOverridden(r : List<R>): List<R>
            }
        """
                .trimIndent()

        fun buildInterface(name: String, typeArg: String) =
            """
            interface $name: MultiLevelSubInterface<$typeArg> {
                override fun subReceiveReturnROverridden(r : $typeArg): $typeArg
                override suspend fun subSuspendReceiveReturnROverridden(r : $typeArg): $typeArg
                override fun subReceiveReturnRListOverridden(r : List<$typeArg>): List<$typeArg>
                override suspend fun subSuspendReceiveReturnRListOverridden(r : List<$typeArg>): List<$typeArg>
            }
        """
                .trimIndent()

        val subInterfaces =
            listOf(
                    "package $pkg",
                    base,
                    buildInterface("MultiSubInterfaceOpen", "Number"),
                    buildInterface("MultiSubInterfaceEnum", "MyEnum"),
                    buildInterface("MultiSubInterfaceFinal", "String"),
                    buildInterface("MultiSubInterfacePrimitive", "Int"),
                    buildInterface("MultiSubInterfacePrimitiveNullable", "Int?"),
                )
                .joinToString("\n")

        return TestInput(
            Source.kotlin("MultiLevel.kt", subInterfaces),
            listOf(
                "MultiSubInterfaceOpen",
                "MultiSubInterfaceEnum",
                "MultiSubInterfaceFinal",
                "MultiSubInterfacePrimitive",
                "MultiSubInterfacePrimitiveNullable",
            )
        )
    }

    private fun createJavaOverridesKotlinSubjects(pkg: String): TestInput {
        val base =
            Source.kotlin(
                "javaOverridesKotlin.kt",
                """
            package $pkg
            interface BaseKotlinOverriddenByJava<T> {
                fun receiveReturnT(t: T): T
                fun receiveReturnTList(t: List<T>): List<T>
            }
        """
                    .trimIndent()
            )

        fun buildInterface(name: String, typeArg: String) =
            Source.java(
                "$pkg.$name",
                """
            package $pkg;
            import java.util.List;
            public interface $name extends BaseKotlinOverriddenByJava<$typeArg> {
            }
        """
                    .trimIndent()
            )
        return TestInput(
            sources =
                listOf(
                    base,
                    buildInterface("JavaOverridesKotlinOpen", "Number"),
                    buildInterface("JavaOverridesKotlinFinal", "String"),
                ),
            subjects = listOf("JavaOverridesKotlinOpen", "JavaOverridesKotlinFinal")
        )
    }

    /** Create test cases where bounded generics are overridden */
    private fun createOverrideWithBoundsSubjects(pkg: String): TestInput {
        val base =
            """
            interface BaseInterfaceWithCloseableBounds<T : Closeable> {
                var tProp: T
                var tListProp: List<T>
                var definedProp: Number
                var definedPropList: List<Number>
                fun receiveReturnT(t: T): T
                suspend fun suspendReceiveReturnT(t:T):T
                fun receiveReturnTList(t:List<T>): List<T>
                suspend fun suspendReceiveReturnTList(t:List<T>): List<T>
                fun receiveReturnTOverridden(t: T): T
                suspend fun suspendReceiveReturnTOverridden(t:T):T
                fun receiveReturnTListOverridden(t:List<T>): List<T>
                suspend fun suspendReceiveReturnTListOverridden(t:List<T>): List<T>
            }
            interface MyCloseable : Closeable
            open class OpenCloseable(): Closeable {
                override open fun close() { TODO() }
            }
            class FinalCloseable(): Closeable {
                override open fun close() { TODO() }
            }
        """
                .trimIndent()

        fun buildInterface(name: String, typeArg: String) =
            """
            interface $name: BaseInterfaceWithCloseableBounds<$typeArg> {
                override fun receiveReturnTOverridden(t: $typeArg): $typeArg
                override suspend fun suspendReceiveReturnTOverridden(t:$typeArg):$typeArg
                override fun receiveReturnTListOverridden(t:List<$typeArg>): List<$typeArg>
                override suspend fun suspendReceiveReturnTListOverridden(t:List<$typeArg>): List<$typeArg>
            }
        """
                .trimIndent()

        val code =
            listOf(
                    "package $pkg",
                    "import java.io.*",
                    base,
                    buildInterface("SubInterfaceCloseable<R : Closeable>", "R"),
                    buildInterface("SubInterfaceMyCloseable<R : MyCloseable>", "R"),
                    buildInterface("SubInterfaceOpenCloseable", "OpenCloseable"),
                    buildInterface("SubInterfaceFinalCloseable", "Closeable"),
                    buildInterface("SubInterfaceByteArrayInputStream", "ByteArrayInputStream"),
                    buildInterface("SubInterfaceFileInputStream", "FileInputStream"),
                )
                .joinToString("\n")
        val classNames =
            listOf(
                "BaseInterfaceWithCloseableBounds",
                "SubInterfaceCloseable",
                "SubInterfaceMyCloseable",
                "SubInterfaceOpenCloseable",
                "SubInterfaceFinalCloseable",
                "SubInterfaceByteArrayInputStream",
                "SubInterfaceFileInputStream",
            )
        return TestInput(Source.kotlin("BoundedOverrides.kt", code), classNames)
    }

    /** Create test cases where generics with multiple upper bounds are overridden */
    private fun createOverrideWithMultipleBoundsSubjects(pkg: String): TestInput {
        val base =
            """
            interface BaseInterfaceWithMultipleBounds<T> where T:FirstBound, T:SecondBound {
                var tProp: T
                var tListProp: List<T>
                var definedProp: Number
                var definedPropList: List<Number>
                fun receiveReturnT(t: T): T
                suspend fun suspendReceiveReturnT(t:T):T
                fun receiveReturnTList(t:List<T>): List<T>
                suspend fun suspendReceiveReturnTList(t:List<T>): List<T>
                fun receiveReturnTOverridden(t: T): T
                suspend fun suspendReceiveReturnTOverridden(t:T):T
                fun receiveReturnTListOverridden(t:List<T>): List<T>
                suspend fun suspendReceiveReturnTListOverridden(t:List<T>): List<T>
            }
            interface FirstBound {
                fun foo()
            }
            interface SecondBound {
                fun bar()
            }
            interface MergedBounds : FirstBound, SecondBound
            open class OpenImpl : FirstBound, SecondBound {
                override fun foo() { TODO() }
                override fun bar() { TODO() }
            }
            class FinalImpl : FirstBound, SecondBound {
                override fun foo() { TODO() }
                override fun bar() { TODO() }
            }
        """
                .trimIndent()

        fun buildInterface(name: String, typeArg: String) =
            """
            interface $name: BaseInterfaceWithMultipleBounds<$typeArg> {
                override fun receiveReturnTOverridden(t: $typeArg): $typeArg
                override suspend fun suspendReceiveReturnTOverridden(t:$typeArg):$typeArg
                override fun receiveReturnTListOverridden(t:List<$typeArg>): List<$typeArg>
                override suspend fun suspendReceiveReturnTListOverridden(t:List<$typeArg>): List<$typeArg>
            }
        """
                .trimIndent()

        val code =
            listOf(
                    "package $pkg",
                    "import java.io.*",
                    base,
                    buildInterface("SubInterfaceMergedBounds<R : MergedBounds>", "R"),
                    buildInterface("SubInterfaceOpenImpl", "OpenImpl"),
                    buildInterface("SubInterfaceFinalImpl", "FinalImpl"),
                )
                .joinToString("\n")
        val classNames =
            listOf(
                "BaseInterfaceWithMultipleBounds",
                "SubInterfaceMergedBounds",
                "SubInterfaceOpenImpl",
                "SubInterfaceFinalImpl"
            )
        return TestInput(Source.kotlin("MultipleBoundsOverrides.kt", code), classNames)
    }

    private data class TestInput(val sources: List<Source>, val subjects: List<String>) {
        constructor(source: Source, subjects: List<String>) : this(listOf(source), subjects)

        operator fun plus(other: TestInput) =
            TestInput(sources = sources + other.sources, subjects = subjects + other.subjects)
    }
}
