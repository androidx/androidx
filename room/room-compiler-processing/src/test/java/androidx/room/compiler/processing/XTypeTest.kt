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

import androidx.room.compiler.codegen.XTypeName.Companion.UNAVAILABLE_KTYPE_NAME
import androidx.room.compiler.processing.ksp.ERROR_JTYPE_NAME
import androidx.room.compiler.processing.ksp.ERROR_KTYPE_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.asJClassName
import androidx.room.compiler.processing.util.asKClassName
import androidx.room.compiler.processing.util.dumpToString
import androidx.room.compiler.processing.util.getDeclaredMethodByJvmName
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.javaElementUtils
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.devtools.ksp.getClassDeclarationByName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeVariableName
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
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XTypeTest {
    @Test
    fun typeArguments() {
        val parent = Source.java(
            "foo.bar.Parent",
            """
            package foo.bar;
            import java.io.InputStream;
            import java.util.Set;
            class Parent<InputStreamType extends InputStream> {
                public void wildcardParam(Set<?> param1) {}
                public void rawTypeParam(Set param1) {}
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
                        .parameterizedBy(KClassName("", "InputStreamType"))
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
            type.typeElement!!.getMethodByJvmName("rawTypeParam").let { method ->
                val rawTypeParam = method.parameters.first()
                assertThat(rawTypeParam.type.typeArguments).isEmpty()
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
                    .isEqualTo("kotlin.collections.List<out R>")
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
}
