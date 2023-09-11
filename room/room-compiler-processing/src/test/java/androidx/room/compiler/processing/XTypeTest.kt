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

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName.Companion.ANY_OBJECT
import androidx.room.compiler.codegen.XTypeName.Companion.UNAVAILABLE_KTYPE_NAME
import androidx.room.compiler.processing.ksp.ERROR_JTYPE_NAME
import androidx.room.compiler.processing.ksp.ERROR_KTYPE_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.asJClassName
import androidx.room.compiler.processing.util.asKClassName
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.dumpToString
import androidx.room.compiler.processing.util.getDeclaredField
import androidx.room.compiler.processing.util.getDeclaredMethodByJvmName
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.isCollection
import androidx.room.compiler.processing.util.javaElementUtils
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JTypeVariableName
import com.squareup.kotlinpoet.javapoet.JWildcardTypeName
import com.squareup.kotlinpoet.javapoet.KClassName
import com.squareup.kotlinpoet.javapoet.KTypeVariableName
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class XTypeTest {
    @Test
    fun typeArguments() {
        val parent = Source.java(
            "foo.bar.Parent",
            """
            package foo.bar;
            import java.io.InputStream;
            import java.util.Set;
            import java.util.List;
            class Parent<InputStreamType extends InputStream> {
                public void wildcardParam(Set<?> param1) {}
                public void rawParamType(Set param1) {}
                public void rawParamTypeArgument(List<Set> param1) {}
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(parent)
        ) {
            val type = it.processingEnv.requireType("foo.bar.Parent")
            assertThat(type.asTypeName().java).isEqualTo(
                JParameterizedTypeName.get(
                    JClassName.get("foo.bar", "Parent"),
                    JClassName.get("", "InputStreamType")
                )
            )
            if (it.isKsp) {
                assertThat(type.asTypeName().kotlin).isEqualTo(
                    KClassName("foo.bar", "Parent")
                        .parameterizedBy(
                            KTypeVariableName(
                                "InputStreamType",
                                KClassName("java.io", "InputStream")
                            )
                        )
                )
            }

            val typeArguments = type.typeArguments
            assertThat(typeArguments).hasSize(1)
            typeArguments.first().let { firstType ->
                val expected = TypeVariableName.get(
                    "InputStreamType",
                    JClassName.get("java.io", "InputStream")
                )
                assertThat(firstType.asTypeName().java).isEqualTo(expected)
                // equals in TypeVariableName just checks the string representation but we want
                // to assert the upper bound as well
                assertThat(
                    (firstType.asTypeName().java as JTypeVariableName).bounds
                ).containsExactly(
                    JClassName.get("java.io", "InputStream")
                )
            }
            if (it.isKsp) {
                typeArguments.first().let { firstType ->
                    val expected = KTypeVariableName(
                        "InputStreamType",
                        KClassName("java.io", "InputStream")
                    )
                    assertThat(firstType.asTypeName().kotlin).isEqualTo(expected)
                    assertThat(
                        (firstType.asTypeName().kotlin as KTypeVariableName).bounds
                    ).containsExactly(
                        KClassName("java.io", "InputStream")
                    )
                }
            }

            type.typeElement!!.getMethodByJvmName("wildcardParam").let { method ->
                val wildcardParam = method.parameters.first()
                val extendsBoundOrSelf = wildcardParam.type.extendsBoundOrSelf()
                assertThat(wildcardParam.type.asTypeName().java).isEqualTo(
                    JParameterizedTypeName.get(
                        JClassName.get("java.util", "Set"),
                        JWildcardTypeName.subtypeOf(Any::class.java)
                    )
                )
                if (it.isKsp) {
                    assertThat(wildcardParam.type.asTypeName().kotlin).isEqualTo(
                        MUTABLE_SET.parameterizedBy(STAR)
                    )
                    assertThat(extendsBoundOrSelf.rawType)
                        .isEqualTo(
                            it.processingEnv.requireType("kotlin.collections.MutableSet").rawType
                        )
                } else {
                    assertThat(extendsBoundOrSelf.rawType)
                        .isEqualTo(
                            it.processingEnv.requireType("java.util.Set").rawType
                        )
                }
            }
            type.typeElement!!.getMethodByJvmName("rawParamType").let { method ->
                val rawParamType = method.parameters.first()
                assertThat(rawParamType.type.typeArguments).isEmpty()
                assertThat(rawParamType.type.asTypeName().java).isEqualTo(
                    JClassName.get("java.util", "Set")
                )
                if (it.isKsp) {
                    assertThat(rawParamType.type.asTypeName().kotlin).isEqualTo(
                        KClassName("kotlin.collections", "MutableSet")
                    )
                }
            }
            type.typeElement!!.getMethodByJvmName("rawParamTypeArgument").let { method ->
                val rawParamTypeArgument = method.parameters.first()
                assertThat(rawParamTypeArgument.type.asTypeName().java).isEqualTo(
                    JParameterizedTypeName.get(
                        JClassName.get("java.util", "List"),
                        JClassName.get("java.util", "Set"),
                    )
                )
                if (it.isKsp) {
                    assertThat(rawParamTypeArgument.type.asTypeName().kotlin).isEqualTo(
                        KClassName("kotlin.collections", "MutableList").parameterizedBy(
                            KClassName("kotlin.collections", "MutableSet")
                        )
                    )
                }
                val rawTypeArgument = rawParamTypeArgument.type.typeArguments.single()
                assertThat(rawTypeArgument.asTypeName().java).isEqualTo(
                    JClassName.get("java.util", "Set")
                )
                if (it.isKsp) {
                    assertThat(rawTypeArgument.asTypeName().kotlin).isEqualTo(
                        KClassName("kotlin.collections", "MutableSet")
                    )
                }
            }
        }
    }

    @Test
    fun errorType() {
        val missingTypeRef = Source.java(
            "foo.bar.Baz",
            """
                package foo.bar;
                public class Baz {
                    NotExistingType badField;
                    NotExistingType badMethod() {
                        throw new RuntimeException("Stub");
                    }
                }
            """.trimIndent()
        )

        runProcessorTest(
            sources = listOf(missingTypeRef)
        ) {
            val errorJTypeName = if (it.isKsp) {
                // In ksp, we lose the name when resolving the type. b/175246617
                ERROR_JTYPE_NAME
            } else {
                ClassName.get("", "NotExistingType")
            }
            val errorKTypeName = if (it.isKsp) {
                // In ksp, we lose the name when resolving the type. b/175246617
                // But otherwise the name is not available in javac / kapt
                ERROR_KTYPE_NAME
            } else {
                UNAVAILABLE_KTYPE_NAME
            }
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getField("badField").let { field ->
                assertThat(field.type.isError()).isTrue()
                assertThat(field.type.asTypeName().java).isEqualTo(errorJTypeName)
                assertThat(field.type.asTypeName().kotlin).isEqualTo(errorKTypeName)
            }
            element.getDeclaredMethodByJvmName("badMethod").let { method ->
                assertThat(method.returnType.isError()).isTrue()
                assertThat(method.returnType.asTypeName().java).isEqualTo(errorJTypeName)
                assertThat(method.returnType.asTypeName().kotlin).isEqualTo(errorKTypeName)
            }
            it.assertCompilationResult {
                compilationDidFail()
            }
        }
    }

    @Test
    fun sameType() {
        val subject = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            interface Baz {
                void method(String... inputs);
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(subject)
        ) {
            val type = it.processingEnv.requireType("foo.bar.Baz")
            val list = it.processingEnv.requireType("java.util.List")
            val string = it.processingEnv.requireType("java.lang.String")
            assertThat(type.isSameType(type)).isTrue()
            assertThat(type.isSameType(list)).isFalse()
            assertThat(list.isSameType(string)).isFalse()
        }
    }

    @Test
    fun sameType_kotlinJava() {
        val javaSrc = Source.java(
            "JavaClass",
            """
            class JavaClass {
                int intField;
                Integer integerField;
            }
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "Foo.kt",
            """
            class KotlinClass {
                val intProp: Int = 0
                val integerProp : Int? = null
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            val javaElm = invocation.processingEnv.requireTypeElement("JavaClass")
            val kotlinElm = invocation.processingEnv.requireTypeElement("KotlinClass")
            fun XFieldElement.isSameType(other: XFieldElement): Boolean {
                return type.isSameType(other.type)
            }
            val fields = javaElm.getAllFieldsIncludingPrivateSupers() +
                kotlinElm.getAllFieldsIncludingPrivateSupers()
            val results = fields.flatMap { f1 ->
                fields.map { f2 ->
                    f1 to f2
                }.filter { (first, second) ->
                    first.isSameType(second)
                }
            }.map { (first, second) ->
                first.name to second.name
            }.toList()

            val expected = setOf(
                "intField" to "intProp",
                "intProp" to "intField",
                "integerField" to "integerProp",
                "integerProp" to "integerField"
            ) + fields.map { it.name to it.name }.toSet()
            assertThat(results).containsExactlyElementsIn(expected)
        }
    }

    @Test
    fun isCollection() {
        runProcessorTest {
            it.processingEnv.requireType("java.util.List").let { list ->
                assertThat(list.isCollection()).isTrue()
            }
            it.processingEnv.requireType("java.util.ArrayList").let { list ->
                // isCollection is overloaded name, it is actually just checking list or set.
                assertThat(list.isCollection()).isFalse()
            }
            it.processingEnv.requireType("java.util.Set").let { list ->
                assertThat(list.isCollection()).isTrue()
            }
            it.processingEnv.requireType("java.util.Map").let { list ->
                assertThat(list.isCollection()).isFalse()
            }
        }
    }

    @Test
    fun isCollection_kotlin() {
        runKspTest(sources = emptyList()) { invocation ->
            val subjects = listOf(
                "Map" to false,
                "List" to true,
                "Set" to true
            )
            subjects.forEach { (subject, expected) ->
                invocation.processingEnv.requireType("kotlin.collections.$subject").let { type ->
                    assertWithMessage(type.asTypeName().java.toString())
                        .that(type.isCollection()).isEqualTo(expected)
                    if (invocation.isKsp) {
                        assertWithMessage(type.asTypeName().kotlin.toString())
                            .that(type.isCollection()).isEqualTo(expected)
                    }
                }
            }
        }
    }

    @Test
    fun toStringMatchesUnderlyingElement() {
        runProcessorTest {
            val subject = "java.lang.String"
            val expected = if (it.isKsp) {
                it.kspResolver.getClassDeclarationByName(subject)?.toString()
            } else {
                it.javaElementUtils.getTypeElement(subject)?.toString()
            }
            val actual = it.processingEnv.requireType(subject).toString()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun errorTypeForSuper() {
        val missingTypeRef = Source.java(
            "foo.bar.Baz",
            """
                package foo.bar;
                public class Baz extends IDontExist {
                    NotExistingType foo() {
                        throw new RuntimeException("Stub");
                    }
                }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(missingTypeRef)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.superClass?.isError()).isTrue()
            it.assertCompilationResult {
                compilationDidFail()
            }
        }
    }

    @Test
    fun defaultValues() {
        runProcessorTest {
            assertThat(
                it.processingEnv.requireType("int").defaultValue()
            ).isEqualTo("0")
            assertThat(
                it.processingEnv.requireType("java.lang.String").defaultValue()
            ).isEqualTo("null")
            assertThat(
                it.processingEnv.requireType("double").defaultValue()
            ).isEqualTo("0.0")
            assertThat(
                it.processingEnv.requireType("float").defaultValue()
            ).isEqualTo("0f")
            assertThat(
                it.processingEnv.requireType("char").defaultValue()
            ).isEqualTo("0")
            assertThat(
                it.processingEnv.requireType("long").defaultValue()
            ).isEqualTo("0L")
        }
    }

    @Test
    fun boxed() {
        runProcessorTest {
            val intBoxed = it.processingEnv.requireType("int").boxed()
            val stringBoxed = it.processingEnv.requireType("java.lang.String").boxed()
            assertThat(intBoxed.asTypeName().java)
                .isEqualTo(java.lang.Integer::class.asJClassName())
            assertThat(stringBoxed.asTypeName().java)
                .isEqualTo(String::class.asJClassName())
            if (it.isKsp) {
                assertThat(intBoxed.asTypeName().kotlin)
                    .isEqualTo(Integer::class.asKClassName())
                assertThat(stringBoxed.asTypeName().kotlin)
                    .isEqualTo(String::class.asKClassName())
            }
        }
    }

    @Test
    fun rawType() {
        runProcessorTest {
            val subject = it.processingEnv.getDeclaredType(
                it.processingEnv.requireTypeElement(List::class),
                it.processingEnv.requireType(String::class)
            )
            assertThat(subject.asTypeName().java).isEqualTo(
                ParameterizedTypeName.get(List::class.asJClassName(), String::class.asJClassName())
            )
            assertThat(subject.rawType.asTypeName().java)
                .isEqualTo(List::class.asJClassName())
            if (it.isKsp) {
                assertThat(subject.asTypeName().kotlin).isEqualTo(
                    KClassName(
                        "kotlin.collections",
                        "MutableList"
                    ).parameterizedBy(String::class.asKClassName())
                )
                assertThat(subject.rawType.asTypeName().kotlin)
                    .isEqualTo(KClassName("kotlin.collections", "MutableList"))
            }

            val listOfInts = it.processingEnv.getDeclaredType(
                it.processingEnv.requireTypeElement(List::class),
                it.processingEnv.requireType(Integer::class)
            )
            assertThat(subject.rawType).isEqualTo(listOfInts.rawType)

            val setOfStrings = it.processingEnv.getDeclaredType(
                it.processingEnv.requireTypeElement(Set::class),
                it.processingEnv.requireType(String::class)
            )
            assertThat(subject.rawType).isNotEqualTo(setOfStrings.rawType)
        }
    }

    @Test
    fun isKotlinUnit() {
        val kotlinSubject = Source.kotlin(
            "Subject.kt",
            """
            class KotlinSubject {
                suspend fun unitSuspend() {}
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(kotlinSubject)) { invocation ->
            invocation.processingEnv.requireTypeElement("KotlinSubject").let {
                val continuationParam = it.getMethodByJvmName("unitSuspend").parameters.last()
                val typeArg = continuationParam.type.typeArguments.first().let {
                    // KAPT will include the bounds directly whereas in KSP, we use bounds only
                    // when resolving the jvm wildcard type.
                    if (invocation.isKsp) {
                        it
                    } else {
                        checkNotNull(it.extendsBound()) {
                            "In KAPT, continuation should've had an extends bound"
                        }
                    }
                }
                assertThat(
                    typeArg.isKotlinUnit()
                ).isTrue()
                assertThat(
                    typeArg.extendsBound()
                ).isNull()
            }
        }
    }

    @Test
    fun isVoidObject() {
        val javaBase = Source.java(
            "JavaInterface",
            """
            import java.lang.Void;
            interface JavaInterface {
                Void getVoid();
                Void anotherVoid();
            }
            """.trimIndent()
        )
        val kotlinSubject = Source.kotlin(
            "Subject.kt",
            """
            abstract class KotlinSubject: JavaInterface {
                fun voidMethod() {}
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(javaBase, kotlinSubject)) { invocation ->
            invocation.processingEnv.requireTypeElement("KotlinSubject").let {
                it.getMethodByJvmName("voidMethod").returnType.let {
                    assertThat(it.isVoidObject()).isFalse()
                    assertThat(it.isVoid()).isTrue()
                    assertThat(it.isKotlinUnit()).isFalse()
                }
                val method = it.getMethodByJvmName("getVoid")
                method.returnType.let {
                    assertThat(it.isVoidObject()).isTrue()
                    assertThat(it.isVoid()).isFalse()
                    assertThat(it.isKotlinUnit()).isFalse()
                }
                it.getMethodByJvmName("anotherVoid").returnType.let {
                    assertThat(it.isVoidObject()).isTrue()
                    assertThat(it.isVoid()).isFalse()
                    assertThat(it.isKotlinUnit()).isFalse()
                }
            }
        }
    }

    @Test
    fun selfReferencingType_kotlin() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class SelfReferencing<T : SelfReferencing<T>> {
                fun method(sr: SelfReferencing<*>) { TODO() }
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val typeElement = invocation.processingEnv.requireTypeElement("SelfReferencing")
            val parameter = typeElement.getMethodByJvmName("method").parameters.single()
            val expectedTypeStringDump = """
                SelfReferencing<T>
                | T
                | > SelfReferencing<T>
                | > | T
                | > | > SelfReferencing<T>
                | > | > | T
                """.trimIndent()
            assertThat(typeElement.type.asTypeName().java.dumpToString(5))
                .isEqualTo(expectedTypeStringDump)
            if (invocation.isKsp) {
                assertThat(typeElement.type.asTypeName().kotlin.dumpToString(5))
                    .isEqualTo(expectedTypeStringDump)
            }
            assertThat(parameter.type.asTypeName().java.dumpToString(5))
                .isEqualTo(
                    """
                SelfReferencing<?>
                | ?
                """.trimIndent()
                )
            if (invocation.isKsp) {
                assertThat(parameter.type.asTypeName().kotlin.dumpToString(5))
                    .isEqualTo(
                        """
                    SelfReferencing<*>
                    | *
                    """.trimIndent()
                    )
            }
        }
    }

    @Test
    fun selfReferencingType_java() {
        val src = Source.java(
            "SelfReferencing",
            """
            class SelfReferencing<T extends SelfReferencing<T>> {
                static void method(SelfReferencing sr) {}
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val typeElement = invocation.processingEnv.requireTypeElement("SelfReferencing")
            val parameter = typeElement.getMethodByJvmName("method").parameters.single()
            val expectedTypeStringDump = """
                SelfReferencing<T>
                | T
                | > SelfReferencing<T>
                | > | T
                | > | > SelfReferencing<T>
                | > | > | T
                """.trimIndent()
            assertThat(typeElement.type.asTypeName().java.dumpToString(5))
                .isEqualTo(expectedTypeStringDump)
            if (invocation.isKsp) {
                assertThat(typeElement.type.asTypeName().kotlin.dumpToString(5))
                    .isEqualTo(expectedTypeStringDump)
            }
            val expectedParamStringDump = """
                SelfReferencing
                """.trimIndent()
            assertThat(parameter.type.asTypeName().java.dumpToString(5))
                .isEqualTo(expectedParamStringDump)
            if (invocation.isKsp) {
                assertThat(parameter.type.asTypeName().kotlin.dumpToString(5))
                    .isEqualTo(expectedParamStringDump)
            }
        }
    }

    @Test
    fun multiLevelSelfReferencingType() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            open class Node<TX : Node<TX, RX>, RX : Node<RX, TX>> {
                fun allStar(node : Node<*, *>) { TODO() }
                fun secondStar(node : Node<TX, *>) { TODO() }
                fun firstStar(node : Node<*, RX>) { TODO() }
                fun noStar(node : Node<TX, RX>) { TODO() }
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val nodeElm = invocation.processingEnv.requireTypeElement("Node")
            val expectedStringDump = """
                Node<TX, RX>
                | TX
                | > Node<TX, RX>
                | > | TX
                | > | > Node<TX, RX>
                | > | > | TX
                | > | > | RX
                | > | RX
                | > | > Node<RX, TX>
                | > | > | RX
                | > | > | TX
                | RX
                | > Node<RX, TX>
                | > | RX
                | > | > Node<RX, TX>
                | > | > | RX
                | > | > | TX
                | > | TX
                | > | > Node<TX, RX>
                | > | > | TX
                | > | > | RX
                """.trimIndent()
            assertThat(nodeElm.type.asTypeName().java.dumpToString(5))
                .isEqualTo(expectedStringDump)
            if (invocation.isKsp) {
                assertThat(nodeElm.type.asTypeName().kotlin.dumpToString(5))
                    .isEqualTo(expectedStringDump)
            }
            val expectedStringDumps = mapOf(
                "allStar" to """
                        Node<?, ?>
                        | ?
                        | ?
                        """.trimIndent(),
                "firstStar" to """
                        Node<?, RX>
                        | ?
                        | RX
                        | > Node<RX, TX>
                        | > | RX
                        | > | > Node<RX, TX>
                        | > | > | RX
                        | > | > | TX
                        | > | TX
                        | > | > Node<TX, RX>
                        | > | > | TX
                        | > | > | RX
                    """.trimIndent(),
                "secondStar" to """
                        Node<TX, ?>
                        | TX
                        | > Node<TX, RX>
                        | > | TX
                        | > | > Node<TX, RX>
                        | > | > | TX
                        | > | > | RX
                        | > | RX
                        | > | > Node<RX, TX>
                        | > | > | RX
                        | > | > | TX
                        | ?
                    """.trimIndent(),
                "noStar" to """
                        Node<TX, RX>
                        | TX
                        | > Node<TX, RX>
                        | > | TX
                        | > | > Node<TX, RX>
                        | > | > | TX
                        | > | > | RX
                        | > | RX
                        | > | > Node<RX, TX>
                        | > | > | RX
                        | > | > | TX
                        | RX
                        | > Node<RX, TX>
                        | > | RX
                        | > | > Node<RX, TX>
                        | > | > | RX
                        | > | > | TX
                        | > | TX
                        | > | > Node<TX, RX>
                        | > | > | TX
                        | > | > | RX
                    """.trimIndent()
            )
            nodeElm.getDeclaredMethods().associate {
                it.name to it.parameters.single().type.asTypeName()
            }.let {
                assertThat(it.mapValues { entry -> entry.value.java.dumpToString(5) })
                    .containsExactlyEntriesIn(expectedStringDumps)
                if (invocation.isKsp) {
                    assertThat(it.mapValues { entry -> entry.value.kotlin.dumpToString(5) })
                        .containsExactlyEntriesIn(
                            // Quick replace ? to * to correctly compare with KotlinPoet
                            expectedStringDumps.mapValues { entry ->
                                entry.value.replace('?', '*')
                            }
                        )
                }
            }
        }
    }

    @Test
    fun selfReferencing_withGenericClassBounds() {
        val src = Source.kotlin(
            "SelfReferencing.kt",
            """
            class SelfReferencing<TX : SelfReferencing<TX, RX>, RX : List<TX>>
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val elm = invocation.processingEnv.requireTypeElement("SelfReferencing")
            val typeDump = elm.type.typeName.dumpToString(5)
            // KSP and Javac diverge here when the generic type parameter in the declaration has
            // variance. This test is kept here to know that the difference is expected.
            // KAPT generates a wildcard for the List<T> from the variance of it. In XProcessing,
            // such resolution is expected to happen at code generation time.
            //
            // This inconsistency is not great but it is fairly complicated to do variance
            // resolution (see: OverrideVarianceResolver.kt) and this level of detail almost
            // never matters.
            if (invocation.isKsp) {
                assertThat(typeDump).isEqualTo(
                    """
                    SelfReferencing<TX, RX>
                    | TX
                    | > SelfReferencing<TX, RX>
                    | > | TX
                    | > | > SelfReferencing<TX, RX>
                    | > | > | TX
                    | > | > | RX
                    | > | RX
                    | > | > java.util.List<TX>
                    | > | > | TX
                    | RX
                    | > java.util.List<TX>
                    | > | TX
                    | > | > SelfReferencing<TX, RX>
                    | > | > | TX
                    | > | > | RX
                    """.trimIndent()
                )
            } else {
                assertThat(typeDump).isEqualTo(
                    """
                    SelfReferencing<TX, RX>
                    | TX
                    | > SelfReferencing<TX, RX>
                    | > | TX
                    | > | > SelfReferencing<TX, RX>
                    | > | > | TX
                    | > | > | RX
                    | > | RX
                    | > | > java.util.List<? extends TX>
                    | > | > | ? extends TX
                    | RX
                    | > java.util.List<? extends TX>
                    | > | ? extends TX
                    """.trimIndent()
                )
            }
        }
    }

    @Test
    fun selfReferencing_withGeneric() {
        val src = Source.kotlin(
            "SelfReferencing.kt",
            """
            class Generic<T>
            class SelfReferencing<TX : SelfReferencing<TX, RX>, RX : Generic<TX>>
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val elm = invocation.processingEnv.requireTypeElement("SelfReferencing")
            val expected = """
                SelfReferencing<TX, RX>
                | TX
                | > SelfReferencing<TX, RX>
                | > | TX
                | > | > SelfReferencing<TX, RX>
                | > | > | TX
                | > | > | RX
                | > | RX
                | > | > Generic<TX>
                | > | > | TX
                | RX
                | > Generic<TX>
                | > | TX
                | > | > SelfReferencing<TX, RX>
                | > | > | TX
                | > | > | RX
                """.trimIndent()
            assertThat(elm.type.asTypeName().java.dumpToString(5))
                .isEqualTo(expected)
            if (invocation.isKsp) {
                assertThat(elm.type.asTypeName().kotlin.dumpToString(5))
                    .isEqualTo(expected)
            }
        }
    }

    /**
     * Repro for b/208207043
     */
    @Test
    fun selfReferencing_starProjectedJava() {
        val src = Source.kotlin(
            "StyleBuilder.kt",
            """
            class StyleApplier<X, Y>
            class StyleBuilder<out B : StyleBuilder<B, A>, out A : StyleApplier<*, *>>
            class KotlinSubject {
                fun subject_1(builder: StyleBuilder<*, *>)  {
                }
            }
            """.trimIndent()
        )
        val javaSource = Source.java(
            "JavaSubject",
            """
                public class JavaSubject {
                    static void subject_1(StyleBuilder<?, ?> builder)  {
                    }
                    static void subject_2(StyleBuilder builder)  {
                    }
                }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src, javaSource)
        ) { invocation ->
            val styleApplier = invocation.processingEnv.requireType("StyleApplier")
            val styleBuilder = invocation.processingEnv.requireType("StyleBuilder")
            assertThat(styleApplier.typeName.dumpToString(5)).isEqualTo(
                """
                StyleApplier<X, Y>
                | X
                | Y
                """.trimIndent()
            )
            // we don't match what kapt generates here so this test is kept here to acknowledge the
            // skew. Otoh, definition of B extending a type that has a type parameter that
            // extends B is very very weird in practice :).
            val bArgSignature = if (invocation.isKsp) {
                "StyleBuilder<B, A>"
            } else {
                "StyleBuilder<? extends B, ? extends A>"
            }
            assertThat(styleBuilder.typeName.dumpToString(2)).isEqualTo(
                """
                StyleBuilder<B, A>
                | B
                | > $bArgSignature
                | A
                | > StyleApplier<?, ?>
                """.trimIndent()
            )

            val javaSubject = invocation.processingEnv.requireTypeElement("JavaSubject")
            val kotlinSubject = invocation.processingEnv.requireTypeElement("KotlinSubject")
            // detect raw java types properly to be consistent
            assertThat(
                javaSubject.getMethodByJvmName("subject_2").parameters.single()
                    .type.typeName.dumpToString(5)
            ).isEqualTo("StyleBuilder")

            val javaTypeName = javaSubject.getMethodByJvmName("subject_1").parameters.single()
                .type.typeName.dumpToString(5)
            val kotlinTypeName = kotlinSubject.getMethodByJvmName("subject_1").parameters.single()
                .type.typeName.dumpToString(5)
            assertThat(javaTypeName).isEqualTo(
                """
                StyleBuilder<?, ?>
                | ?
                | ?
                """.trimIndent()
            )
            assertThat(kotlinTypeName).isEqualTo(
                """
                StyleBuilder<?, ?>
                | ?
                | ?
                """.trimIndent()
            )
        }
    }

    /**
     * Reproduces the first bug in b/204415667
     */
    @Test
    fun starVarianceInParameter() {
        val libSource = Source.kotlin(
            "lib.kt",
            """
            class MyClass<R> {
                fun setLists(starList: List<*>, rList: List<R>) {}
            }
            """.trimIndent()
        )
        runProcessorTest(listOf(libSource)) { invocation ->
            val actual = invocation.processingEnv.requireTypeElement("MyClass")
                .getDeclaredMethodByJvmName("setLists").parameters.associate {
                    it.name to it.type.asTypeName()
                }
            assertThat(actual["starList"]?.java.toString())
                .isEqualTo("java.util.List<?>")
            assertThat(actual["rList"]?.java.toString())
                .isEqualTo("java.util.List<? extends R>")
            if (invocation.isKsp) {
                assertThat(actual["starList"]?.kotlin.toString())
                    .isEqualTo("kotlin.collections.List<*>")
                assertThat(actual["rList"]?.kotlin.toString())
                    .isEqualTo("kotlin.collections.List<R>")
            }
        }
    }

    @Test
    fun superTypes() {
        val libSource = Source.kotlin(
            "foo.kt",
            """
            package foo.bar;
            class Baz : MyInterface, AbstractClass<String>() {
            }
            abstract class AbstractClass<T> {}
            interface MyInterface {}
            """.trimIndent()
        )
        runProcessorTest(listOf(libSource)) { invocation ->
            invocation.processingEnv.requireType("foo.bar.Baz").let {
                val superTypes = it.superTypes
                assertThat(superTypes).hasSize(2)
                val superClass =
                    superTypes.first { type -> type.rawType.toString() == "foo.bar.AbstractClass" }
                val superInterface =
                    superTypes.first { type -> type.rawType.toString() == "foo.bar.MyInterface" }
                assertThat(superClass.typeArguments).hasSize(1)
                assertThat(superClass.typeArguments[0].asTypeName().java)
                    .isEqualTo(JClassName.get("java.lang", "String"))
                if (invocation.isKsp) {
                    assertThat(superClass.typeArguments[0].asTypeName().kotlin)
                        .isEqualTo(KClassName("kotlin", "String"))
                }
                assertThat(superInterface.typeArguments).isEmpty()
            }
        }
    }

    @Test
    fun arrayTypes() {
        // Used to test both java and kotlin sources.
        fun XTestInvocation.checkArrayTypesTest() {
            val fooElement = processingEnv.requireTypeElement("foo.bar.Foo")
            val barElement = processingEnv.requireTypeElement("foo.bar.Bar")
            val barType = fooElement.getField("bar").type
            val barArrayType = fooElement.getField("barArray").type

            assertThat(barType.typeElement).isEqualTo(barElement)
            assertThat(barType.isArray()).isFalse()
            assertThat(barArrayType.typeElement).isNull()
            assertThat(barArrayType.isArray()).isTrue()
        }

        runProcessorTest(listOf(Source.java(
            "foo.bar.Foo",
            """
            package foo.bar;
            class Foo {
              Bar bar;
              Bar[] barArray;
            }
            class Bar {}
            """.trimIndent()
        ))) { it.checkArrayTypesTest() }

        runProcessorTest(listOf(Source.kotlin(
            "foo.bar.Foo.kt",
            """
            package foo.bar;
            class Foo {
              val bar: Bar = TODO()
              val barArray: Array<Bar> = TODO()
            }
            class Bar
            """.trimIndent()
        ))) { it.checkArrayTypesTest() }
    }

    @Test
    fun primitiveTypes() {
        fun XTestInvocation.checkPrimitiveType() {
            val fooElement = processingEnv.requireTypeElement("foo.bar.Foo")
            val primitiveType = fooElement.getField("i").type
            assertThat(primitiveType.asTypeName().java).isEqualTo(JTypeName.INT)
            if (isKsp) {
                assertThat(primitiveType.asTypeName().kotlin).isEqualTo(INT)
            }
            assertThat(primitiveType.typeElement).isNull()
        }

        runProcessorTest(listOf(Source.java(
            "foo.bar.Foo",
            """
            package foo.bar;
            class Foo {
              int i;
            }
            """.trimIndent()
        ))) { it.checkPrimitiveType() }

        runProcessorTest(listOf(Source.kotlin(
            "foo.bar.Foo.kt",
            """
            package foo.bar
            class Foo {
              val i: Int = TODO()
            }
            """.trimIndent()
        ))) { it.checkPrimitiveType() }
    }

    @Test
    fun setSuperTypeNames() {
        fun superTypeHierarchy(type: XType, depth: Int = 0): String {
            val sb: StringBuilder = StringBuilder()
            sb.append("${"  ".repeat(depth)}> ${type.typeName}")
            type.superTypes.forEach {
                sb.append("\n").append(superTypeHierarchy(it, depth + 1))
            }
            return sb.toString()
        }

        fun XTestInvocation.checkType() {
            val fooElement = processingEnv.requireTypeElement("test.Foo")
            val method1 = fooElement.getMethodByJvmName("method1")
            val method2 = fooElement.getMethodByJvmName("method2")

            // Check the return types of the unresolved methods
            assertThat(method1.returnType.typeName).isEqualTo(TypeVariableName.get("T1"))
            assertThat(method2.returnType.typeName).isEqualTo(TypeVariableName.get("T2"))

            // Check the return types of the methods resolved into Usage
            val usageElement = processingEnv.requireTypeElement("test.Usage")
            assertThat(method1.asMemberOf(usageElement.type).returnType.typeName.toString())
                .isEqualTo("test.Baz<java.lang.Long, java.lang.Number>")
            assertThat(method2.asMemberOf(usageElement.type).returnType.typeName.toString())
                .isEqualTo("java.lang.Integer")

            // Check the supertypes of the unresolved Foo
            assertThat(superTypeHierarchy(fooElement.type)).isEqualTo(
                """
                > test.Foo<V1, V2>
                  > java.lang.Object
                  > test.Bar<test.Baz<V1, java.lang.Number>, V2>
                    > java.lang.Object
                    > test.Baz<test.Baz<V1, java.lang.Number>, V2>
                      > java.lang.Object
                """.trimIndent()
            )

            // Check the supertypes of Foo<Long, Integer>
            assertThat(superTypeHierarchy(usageElement.type)).isEqualTo(
                """
                > test.Usage
                  > java.lang.Object
                  > test.Foo<java.lang.Long, java.lang.Integer>
                    > java.lang.Object
                    > test.Bar<test.Baz<java.lang.Long, java.lang.Number>, java.lang.Integer>
                      > java.lang.Object
                      > test.Baz<test.Baz<java.lang.Long, java.lang.Number>, java.lang.Integer>
                        > java.lang.Object
                """.trimIndent()
            )

            // Check the supertypes of Foo<String, Integer>
            val methodFoo = usageElement.getMethodByJvmName("foo")
            assertThat(superTypeHierarchy(methodFoo.returnType)).isEqualTo(
                """
                > test.Foo<java.lang.String, java.lang.Integer>
                  > java.lang.Object
                  > test.Bar<test.Baz<java.lang.String, java.lang.Number>, java.lang.Integer>
                    > java.lang.Object
                    > test.Baz<test.Baz<java.lang.String, java.lang.Number>, java.lang.Integer>
                      > java.lang.Object
                """.trimIndent()
            )

            // Check the supertypes of Foo<Double, Integer>
            assertThat(superTypeHierarchy(methodFoo.parameters[0].type)).isEqualTo(
                """
                > test.Foo<java.lang.Double, java.lang.Integer>
                  > java.lang.Object
                  > test.Bar<test.Baz<java.lang.Double, java.lang.Number>, java.lang.Integer>
                    > java.lang.Object
                    > test.Baz<test.Baz<java.lang.Double, java.lang.Number>, java.lang.Integer>
                      > java.lang.Object
                """.trimIndent()
            )
        }

        runProcessorTest(listOf(Source.java(
            "test.Usage",
            """
            package test;
            interface Usage extends Foo<Long, Integer> {
                Foo<String, Integer> foo(Foo<Double, Integer> param);
            }
            interface Foo<V1, V2 extends Integer> extends Bar<Baz<V1, Number>, V2> {}
            interface Bar<U1, U2 extends Integer> extends Baz<U1, U2> {}
            interface Baz<T1, T2 extends Number> {
                T1 method1();
                T2 method2();
            }
            """.trimIndent()
        ))) { it.checkType() }

        runProcessorTest(listOf(Source.kotlin(
            "test.Usage.kt",
            """
            package test
            interface Usage : Foo<Long, Integer> {
                fun foo(param: Foo<Double, Integer>): Foo<String, Integer>
            }
            interface Foo<V1, V2: Integer> : Bar<Baz<V1, Number>, V2> {}
            interface Bar<U1, U2: Integer> : Baz<U1, U2> {}
            interface Baz<T1, T2: Number> {
                fun method1(): T1
                fun method2(): T2
            }
            """.trimIndent()
        ))) { it.checkType() }
    }

    @Test
    fun typeArgumentMissingType() {
        class TypeArgumentProcessingStep : XProcessingStep {
            override fun annotations() = setOf("test.Inspect")

            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>,
                isLastRound: Boolean
            ): Set<XElement> {
                val barElement = env.requireTypeElement("test.Bar")
                val missingTypeName = if (
                    env.backend == XProcessingEnv.Backend.KSP ||
                    // There's a bug in KAPT that doesn't replace NonExistentClass even when
                    // correctErrorTypes is enabled, so we account for that here.
                    // https://youtrack.jetbrains.com/issue/KT-34193/Kapt-CorrectErrorTypes-doesnt-work-for-generics
                    barElement.hasAnnotation(Metadata::class)
                ) {
                    ClassName.get("error", "NonExistentClass")
                } else {
                    ClassName.get("", "MissingType")
                }
                val barType = barElement.type
                val fooTypeName = ParameterizedTypeName.get(
                    ClassName.get("test", "Foo"),
                    missingTypeName
                )

                val fooType = barType.superTypes.single()
                assertThat(fooType.typeName).isEqualTo(fooTypeName)
                assertThat(fooType.isError()).isFalse()

                val typeArgument = fooType.typeArguments.single()
                assertThat(typeArgument.typeName).isEqualTo(missingTypeName)
                assertThat(typeArgument.isError()).isTrue()

                return emptySet()
            }
        }

        val step = TypeArgumentProcessingStep()
        runProcessorTest(
            sources = listOf(Source.java(
                "test.Foo",
                """
                package test;
                @Inspect
                class Bar extends Foo<MissingType> {}
                class Foo<T> {}
                @interface Inspect {}
                """.trimIndent()
            )),
        ) { invocation ->
            val elements =
                step.annotations()
                    .associateWith { annotation ->
                        invocation.roundEnv.getElementsAnnotatedWith(annotation)
                            .filterIsInstance<XTypeElement>()
                            .toSet()
                    }
            step.process(
                env = invocation.processingEnv,
                elementsByAnnotation = elements,
                isLastRound = false
            )
            invocation.assertCompilationResult {
                hasError()
                hasErrorCount(1)
                hasErrorContaining("cannot find symbol")
            }
        }

        runProcessorTest(
            sources = listOf(Source.kotlin(
                "test.Foo.kt",
                """
            package test
            class Bar : Foo<MissingType>()
            open class Foo<T>
            """.trimIndent()
            )),
            kotlincArguments = listOf(
                "-P", "plugin:org.jetbrains.kotlin.kapt3:correctErrorTypes=true"
            ),
            createProcessingSteps = { listOf(TypeArgumentProcessingStep()) }
        ) { result ->
            result.hasError()
            result.hasErrorCount(1)
            result.hasErrorContaining("Unresolved reference")
        }
    }

    @Test
    fun wildcardWithMissingType() {
        class WildcardProcessingStep : XProcessingStep {
            override fun annotations() = setOf("test.Inspect")

            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>,
                isLastRound: Boolean
            ): Set<XElement> {
                val missingTypeName = if (env.backend == XProcessingEnv.Backend.KSP) {
                    ClassName.get("error", "NonExistentClass")
                } else {
                    ClassName.get("", "MissingType")
                }
                val wildcardTypeName = WildcardTypeName.subtypeOf(missingTypeName)
                val fooTypeName = ParameterizedTypeName.get(
                    ClassName.get("test", "Foo"),
                    wildcardTypeName
                )

                val fooElement = env.requireTypeElement("test.Foo")
                val fooType = fooElement.getField("foo").type
                assertThat(fooType.typeName).isEqualTo(fooTypeName)
                assertThat(fooType.isError()).isFalse()

                val wildcardType = fooType.typeArguments.single()
                assertThat(wildcardType.typeName).isEqualTo(wildcardTypeName)
                assertThat(wildcardType.isError()).isFalse()

                assertThat(wildcardType.extendsBound()).isNotNull()
                val errorType = wildcardType.extendsBound()!!
                assertThat(errorType.typeName).isEqualTo(missingTypeName)
                assertThat(errorType.isError()).isTrue()

                return emptySet()
            }
        }

        runProcessorTest(
            sources = listOf(Source.java(
                "test.Foo",
                """
                package test;
                @Inspect
                class Foo<T> {
                  Foo<? extends MissingType> foo;
                }
                @interface Inspect {}
                """.trimIndent()
            )),
            createProcessingSteps = { listOf(WildcardProcessingStep()) }
        ) { result ->
            result.hasError()
            result.hasErrorCount(1)
            result.hasErrorContaining("cannot find symbol")
        }

        runProcessorTest(
            sources = listOf(Source.kotlin(
                "test.Foo.kt",
                """
            package test
            class Foo<T> {
              val foo: Foo<out MissingType> = TODO()
            }
            """.trimIndent()
            )),
            kotlincArguments = listOf(
                "-P", "plugin:org.jetbrains.kotlin.kapt3:correctErrorTypes=true"
            ),
            createProcessingSteps = { listOf(WildcardProcessingStep()) }
        ) { result ->
            result.hasError()
            result.hasErrorCount(1)
            result.hasErrorContaining("Unresolved reference")
        }
    }

    @Test
    fun getWildcardType() {
        fun XTestInvocation.checkType() {
            val usageElement = processingEnv.requireTypeElement("test.Usage")
            val fooElement = processingEnv.requireTypeElement("test.Foo")
            val barType = processingEnv.requireType("test.Bar")

            // Test a manually constructed Foo<Bar>
            val fooBarType = processingEnv.getDeclaredType(fooElement, barType)
            val fooBarUsageType = usageElement.getDeclaredField("fooBar").type
            assertThat(fooBarType.asTypeName()).isEqualTo(fooBarUsageType.asTypeName())

            // Test a manually constructed Foo<? extends Bar>
            val fooExtendsBarType = processingEnv.getDeclaredType(
                fooElement,
                processingEnv.getWildcardType(producerExtends = barType)
            )
            val fooExtendsBarUsageType = usageElement.getDeclaredField("fooExtendsBar").type
            assertThat(fooExtendsBarType.asTypeName())
                .isEqualTo(fooExtendsBarUsageType.asTypeName())

            // Test a manually constructed Foo<? super Bar>
            val fooSuperBarType = processingEnv.getDeclaredType(
                fooElement,
                processingEnv.getWildcardType(consumerSuper = barType)
            )
            val fooSuperBarUsageType = usageElement.getDeclaredField("fooSuperBar").type
            assertThat(fooSuperBarType.asTypeName()).isEqualTo(fooSuperBarUsageType.asTypeName())

            // Test a manually constructed Foo<?>
            val fooUnboundedType = processingEnv.getDeclaredType(
                fooElement,
                processingEnv.getWildcardType()
            )
            val fooUnboundedUsageType = usageElement.getDeclaredField("fooUnbounded").type
            assertThat(fooUnboundedType.asTypeName()).isEqualTo(fooUnboundedUsageType.asTypeName())
        }

        runProcessorTest(listOf(Source.java(
            "test.Foo",
            """
            package test;
            class Usage {
              Foo<?> fooUnbounded;
              Foo<Bar> fooBar;
              Foo<? extends Bar> fooExtendsBar;
              Foo<? super Bar> fooSuperBar;
            }
            interface Foo<T> {}
            interface Bar {}
            """.trimIndent()
        ))) { it.checkType() }

        runProcessorTest(listOf(Source.kotlin(
            "test.Usage.kt",
            """
            package test
            class Usage {
              val fooUnbounded: Foo<*> = TODO()
              val fooBar: Foo<Bar> = TODO()
              val fooExtendsBar: Foo<out Bar> = TODO()
              val fooSuperBar: Foo<in Bar> = TODO()
            }
            interface Foo<T>
            interface Bar
            """.trimIndent()
        ))) { it.checkType() }
    }

    @Test
    fun isTypeVariable() {
        val javaSubject = Source.java(
            "test.JavaFoo",
            """
            package test;
            class JavaFoo<T> {
                T field;
                T method(T param) {
                    return null;
                }
            }
            """.trimIndent()
        )
        val javaImplSubject = Source.java(
            "test.JavaFooImpl",
            """
            package test;
            class JavaFooImpl extends JavaFoo<String> {
            }
            """.trimIndent()
        )
        val kotlinSubject = Source.kotlin(
            "Foo.kt",
            """
            package test
            open class KotlinFoo<T> {
                val field: T = TODO();
                fun method(param: T): T {
                    TODO()
                }
            }

            class KotlinFooImpl : KotlinFoo<String>()
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(javaSubject, javaImplSubject, kotlinSubject)
        ) { invocation ->
            listOf("test.JavaFoo", "test.KotlinFoo").forEach { fqn ->
                val typeElement = invocation.processingEnv.requireTypeElement(fqn)
                typeElement.getDeclaredField("field").let {
                    assertThat(it.type.isTypeVariable()).isTrue()
                    val asMemberOf =
                        it.asMemberOf(invocation.processingEnv.requireType(fqn + "Impl"))
                    assertThat(asMemberOf.isTypeVariable()).isFalse()
                }
                typeElement.getDeclaredMethodByJvmName("method").let {
                    assertThat(it.returnType.isTypeVariable()).isTrue()
                    assertThat(it.parameters.single().type.isTypeVariable()).isTrue()
                    val asMemberOf =
                        it.asMemberOf(invocation.processingEnv.requireType(fqn + "Impl"))
                    assertThat(asMemberOf.returnType.isTypeVariable()).isFalse()
                    assertThat(asMemberOf.parameterTypes.single().isTypeVariable()).isFalse()
                }
            }
        }
    }

    @Test
    fun typeParameter_extendBound() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class Foo<E> {
                fun justOneGeneric(): E = TODO()
                fun listOfGeneric(): List<E> = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) {
            val fooTypeElement = it.processingEnv.requireTypeElement("Foo")
            fooTypeElement.getMethodByJvmName("justOneGeneric").returnType.let { type ->
                assertThat(type.extendsBound()).isNull()
            }
            fooTypeElement.getMethodByJvmName("listOfGeneric").returnType.let { type ->
                assertThat(type.extendsBound()).isNull()
                type.typeArguments.forEach { typeArg ->
                    assertThat(typeArg.extendsBound()).isNull()
                }
            }
        }
    }

    @Test
    fun missingTypes_names() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            package test

            class Foo {
              fun bar(missing: MissingType) = TODO()
              fun barQualified(missing: bar.MissingType) = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src),
            kotlincArguments = listOf(
                "-P", "plugin:org.jetbrains.kotlin.kapt3:correctErrorTypes=true"
            ),
        ) {
            val fooTypeElement = it.processingEnv.requireTypeElement("test.Foo")
            fooTypeElement.getMethodByJvmName("bar").parameters.single().let { param ->
                if (it.isKsp) {
                    // TODO(b/248552462): KSP doesn't expose simple names on error types, see:
                    //  https://github.com/google/ksp/issues/1232
                    assertThat(param.type.asTypeName())
                        .isEqualTo(XClassName.get("error", "NonExistentClass"))
                } else {
                    assertThat(param.type.asTypeName())
                        .isEqualTo(XClassName.get("", "MissingType"))
                }
            }
            fooTypeElement.getMethodByJvmName("barQualified").parameters.single().let { param ->
                if (it.isKsp) {
                    // TODO(b/248552462): KSP doesn't expose simple names on error types, see:
                    //  https://github.com/google/ksp/issues/1232
                    assertThat(param.type.asTypeName())
                        .isEqualTo(XClassName.get("error", "NonExistentClass"))
                } else {
                    assertThat(param.type.asTypeName())
                        .isEqualTo(XClassName.get("", "bar.MissingType"))
                }
            }
            it.assertCompilationResult { hasErrorContaining("Unresolved reference: MissingType") }
        }
    }

    @Test
    fun rawTypeNames() {
        val src = Source.java(
            "test.Subject",
            """
            package test;
            import java.util.Set;
            @SuppressWarnings("rawtypes")
            class Subject {
                Foo foo;
                Foo<Foo> fooFoo;
                Foo<Foo<Foo>> fooFooFoo;
                Bar<Foo, Foo> barFooFoo;
            }
            class Foo<T> {}
            class Bar<T1, T2> {}
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            fun assertHasTypeName(type: XType, expectedTypeName: String) {
                assertThat(type.asTypeName().java.toString()).isEqualTo(expectedTypeName)
                if (invocation.isKsp) {
                    assertThat(type.asTypeName().kotlin.toString()).isEqualTo(expectedTypeName)
                }
            }

            val subject = invocation.processingEnv.requireTypeElement("test.Subject")
            assertHasTypeName(subject.getDeclaredField("foo").type, "test.Foo")
            assertHasTypeName(subject.getDeclaredField("fooFoo").type, "test.Foo<test.Foo>")
            assertHasTypeName(
                subject.getDeclaredField("fooFooFoo").type, "test.Foo<test.Foo<test.Foo>>")
            assertHasTypeName(
                subject.getDeclaredField("barFooFoo").type, "test.Bar<test.Foo, test.Foo>")

            // Test manually wrapping raw type using XProcessingEnv#getDeclaredType()
            subject.getDeclaredField("foo").type.let { foo ->
                val fooTypeElement = invocation.processingEnv.requireTypeElement("test.Foo")
                val fooFoo: XType = invocation.processingEnv.getDeclaredType(fooTypeElement, foo)
                assertHasTypeName(fooFoo, "test.Foo<test.Foo>")

                val fooFooFoo: XType =
                    invocation.processingEnv.getDeclaredType(fooTypeElement, fooFoo)
                assertHasTypeName(fooFooFoo, "test.Foo<test.Foo<test.Foo>>")

                val barTypeElement = invocation.processingEnv.requireTypeElement("test.Bar")
                val barFooFoo: XType =
                    invocation.processingEnv.getDeclaredType(barTypeElement, foo, foo)
                assertHasTypeName(barFooFoo, "test.Bar<test.Foo, test.Foo>")
            }

            // Test manually unwrapping a type with a raw type argument:
            subject.getDeclaredField("fooFoo").type.let { fooFoo ->
                assertHasTypeName(fooFoo.typeArguments.single(), "test.Foo")
            }
            subject.getDeclaredField("barFooFoo").type.let { barFooFoo ->
                assertThat(barFooFoo.typeArguments).hasSize(2)
                assertHasTypeName(barFooFoo.typeArguments[0], "test.Foo")
                assertHasTypeName(barFooFoo.typeArguments[1], "test.Foo")
            }
        }
    }

    @Test
    fun hasAnnotationWithPackage() {
        val kotlinSrc = Source.kotlin(
            "KotlinClass.kt",
            """
            package foo.bar
            interface KotlinInterface
            open class KotlinBase
            @Target(AnnotationTarget.TYPE)
            annotation class KotlinAnnotation {
                @Target(AnnotationTarget.TYPE)
                annotation class KotlinNestedAnnotation
            }
            class KotlinClass : @KotlinAnnotation.KotlinNestedAnnotation KotlinBase(),
                    @KotlinAnnotation KotlinInterface {
                inner class KotlinInner : @KotlinAnnotation KotlinInterface
                class KotlinNested : @KotlinAnnotation KotlinInterface
            }
            """.trimIndent()
        )
        // KSP can't read nested annotations in Java sources if the filename does not match
        // the outer class.
        val javaAnnotationSource = Source.java(
            "foo.bar.JavaAnnotation",
            """
            package foo.bar;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            @Target(ElementType.TYPE_USE)
            @interface JavaAnnotation {
                @Target(ElementType.TYPE_USE)
                @interface JavaNestedAnnotation {}
            }
            """.trimIndent()
        )
        val javaSrc = Source.java(
            "foo.bar.JavaClass",
            """
            package foo.bar;
            interface JavaInterface {}
            class JavaBase {}
            class JavaClass extends @JavaAnnotation.JavaNestedAnnotation JavaBase
                implements @JavaAnnotation JavaInterface {}
            """.trimIndent()
        )
        fun checkKotlin(invocation: XTestInvocation) {
            val kotlinTypeElement = invocation.processingEnv.requireTypeElement(
                "foo.bar.KotlinClass")
            kotlinTypeElement.superInterfaces.single().let {
                assertThat(it.getAllAnnotations().single().typeElement.packageName)
                        .isEqualTo("foo.bar")
                assertThat(it.getAllAnnotations().single().typeElement.qualifiedName)
                    .isEqualTo("foo.bar.KotlinAnnotation")

                assertThat(it.hasAnnotationWithPackage("foo.bar.KotlinAnnotation")).isFalse()
                assertThat(it.hasAnnotationWithPackage("foo.bar")).isTrue()
                assertThat(it.hasAnnotationWithPackage("foo")).isFalse()
            }
            kotlinTypeElement.superClass!!.let {
                assertThat(it.getAllAnnotations().single().typeElement.packageName)
                    .isEqualTo("foo.bar")
                assertThat(it.getAllAnnotations().single().typeElement.qualifiedName)
                    .isEqualTo("foo.bar.KotlinAnnotation.KotlinNestedAnnotation")

                assertThat(it.getAllAnnotations().single().typeElement.packageName)
                    .isEqualTo("foo.bar")
                assertThat(it.getAllAnnotations().single().typeElement.qualifiedName)
                    .isEqualTo("foo.bar.KotlinAnnotation.KotlinNestedAnnotation")
            }
        }
        fun checkJava(invocation: XTestInvocation) {
            val javaTypeElement = invocation.processingEnv.requireTypeElement(
                "foo.bar.JavaClass")
            javaTypeElement.superInterfaces.first().let {
                assertThat(it.getAllAnnotations().single().typeElement.packageName)
                    .isEqualTo("foo.bar")
                assertThat(it.getAllAnnotations().single().typeElement.qualifiedName)
                    .isEqualTo("foo.bar.JavaAnnotation")

                assertThat(it.hasAnnotationWithPackage("foo.bar.JavaClass")).isFalse()
                assertThat(it.hasAnnotationWithPackage("foo.bar")).isTrue()
                assertThat(it.hasAnnotationWithPackage("foo")).isFalse()
            }
            javaTypeElement.superClass!!.let {
                assertThat(it.getAllAnnotations().single().typeElement.packageName)
                    .isEqualTo("foo.bar")
                assertThat(it.getAllAnnotations().single().typeElement.qualifiedName)
                    .isEqualTo("foo.bar.JavaAnnotation.JavaNestedAnnotation")

                assertThat(it.hasAnnotationWithPackage("foo.bar.JavaClass")).isFalse()
                assertThat(it.hasAnnotationWithPackage("foo.bar")).isTrue()
                assertThat(it.hasAnnotationWithPackage("foo")).isFalse()
            }
        }
        runProcessorTest(
            sources = listOf(kotlinSrc, javaAnnotationSource, javaSrc),
            handler = {
                checkKotlin(it)
                checkJava(it)
            }
        )
        runProcessorTest(
            classpath = compileFiles(listOf(kotlinSrc)),
            handler = {
                // We can't see type annotations from precompiled Java classes. Skipping it
                // for now: https://github.com/google/ksp/issues/1296
                checkKotlin(it)
            }
        )
    }

    @Test
    fun selfReferenceTypesDoesNotInfinitelyRecurse() {
        fun runTest(src: Source) {
            runProcessorTest(
                sources = listOf(src),
            ) { invocation ->
                val fooTypeElement = invocation.processingEnv.requireTypeElement("test.Usage")
                val fooType = fooTypeElement.getDeclaredField("foo").type

                assertThat(fooType.asTypeName().java)
                    .isEqualTo(
                        JParameterizedTypeName.get(
                            JClassName.get("test", "Foo"),
                            JWildcardTypeName.subtypeOf(JClassName.OBJECT)
                        )
                    )

                val typeParam = fooType.typeArguments.single()
                assertThat(typeParam.asTypeName().java)
                    .isEqualTo(JWildcardTypeName.subtypeOf(JClassName.OBJECT))

                assertThat(typeParam.extendsBound()).isNull()
            }
        }
        runTest(
            Source.java(
                "test.Usage",
                """
                package test;
                public final class Usage {
                  private final Foo<?> foo = null;
                  private final Foo<? extends Foo<?>> fooExtendsFoo = null;
                }
                abstract class Foo<T extends Foo<T>> {}
                """.trimIndent()
            ),
        )
        runTest(
            Source.kotlin(
                "test.Foo.kt",
                """
            package test
            class Usage {
                val foo: Foo<*> = TODO()
                val fooExtendsFoo: Foo<out Foo<*>> = TODO()
            }
            abstract class Foo<T: Foo<T>>
            """.trimIndent()
            )
        )
    }

    @Test
    fun selfReferenceSuperTypesDoesNotInfinitelyRecurse() {
        val baseInterface = Source.java(
            "test.BaseInterface",
            """
            package test;
            public interface BaseInterface<T> {}
            """.trimIndent()
        )
        val selfReferenceClass = Source.java(
            "test.SelfRef",
            """
            package test;
            public abstract class SelfRef<T extends SelfRef<T>> { }
            """.trimIndent()
        )
        val source = Source.java(
            "test.Subject",
            """
            package test;
            public final class Subject implements BaseInterface<SelfRef<?>> { }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(
                baseInterface,
                selfReferenceClass,
                source
            ),
        ) {
            val subject = it.processingEnv.requireTypeElement("test.Subject")
            val superType = subject.type.superTypes
                .map { it.asTypeName() }
                .filterNot { it == ANY_OBJECT }
                .single()
            assertThat(superType.java)
                .isEqualTo(
                    JParameterizedTypeName.get(
                        JClassName.get("test", "BaseInterface"),
                        JParameterizedTypeName.get(
                            JClassName.get("test", "SelfRef"),
                            JWildcardTypeName.subtypeOf(JClassName.OBJECT)
                        )
                    )
                )
        }
    }

    @Test
    fun valueTypes(@TestParameter isPrecompiled: Boolean,) {
        val kotlinSrc = Source.kotlin(
            "KotlinClass.kt",
            """
            @JvmInline value class PackageName(val value: String)
            @JvmInline value class MyResult<T>(val value: T)

            class KotlinClass {
                // @JvmName disables name mangling for functions that use inline classes and
                // make them visible to Java:
                // https://kotlinlang.org/docs/inline-classes.html#calling-from-java-code
                @JvmName("getResult")
                fun getResult(): MyResult<String> = TODO()
                @JvmName("setResult")
                fun setResult(result: MyResult<String>) { }
                fun getPackageNames(): Set<PackageName> = emptySet()
                fun setPackageNames(pkgNames: Set<PackageName>) { }
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = if (isPrecompiled) { emptyList() } else { listOf(kotlinSrc) },
            classpath = if (isPrecompiled) { compileFiles(listOf(kotlinSrc)) } else { emptyList() }
        ) { invocation ->
            val kotlinElm = invocation.processingEnv.requireTypeElement("KotlinClass")

            kotlinElm.getDeclaredMethodByJvmName("getResult").apply {
                assertThat(returnType.asTypeName().java.toString())
                    .isEqualTo("java.lang.Object")
                if (invocation.isKsp) {
                    assertThat(returnType.asTypeName().kotlin.toString())
                        .isEqualTo("MyResult<kotlin.String>")
                } else {
                    // Can't generate Kotlin code with KAPT
                    assertThat(returnType.asTypeName().kotlin.toString())
                        .isEqualTo("androidx.room.compiler.codegen.Unavailable")
                }
            }
            kotlinElm.getDeclaredMethodByJvmName("setResult").apply {
                assertThat(parameters.single().type.asTypeName().java.toString())
                    .isEqualTo("java.lang.Object")
                if (invocation.isKsp) {
                    assertThat(parameters.single().type.asTypeName().kotlin.toString())
                        .isEqualTo("MyResult<kotlin.String>")
                } else {
                    // Can't generate Kotlin code with KAPT
                    assertThat(parameters.single().type.asTypeName().kotlin.toString())
                        .isEqualTo("androidx.room.compiler.codegen.Unavailable")
                }
            }

            kotlinElm.getMethodByJvmName("getPackageNames").apply {
                assertThat(returnType.typeName.toString())
                    .isEqualTo("java.util.Set<PackageName>")
                assertThat(returnType.typeArguments.single().typeName.toString())
                    .isEqualTo("PackageName")
            }
            kotlinElm.getMethodByJvmName("setPackageNames").apply {
                val paramType = parameters.single().type
                assertThat(paramType.typeName.toString())
                    .isEqualTo("java.util.Set<PackageName>")
                assertThat(paramType.typeArguments.single().typeName.toString())
                    .isEqualTo("PackageName")
            }
        }
    }
}
