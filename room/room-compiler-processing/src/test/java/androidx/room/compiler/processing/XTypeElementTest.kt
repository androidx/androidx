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
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.javac.JavacType
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.asKClassName
import androidx.room.compiler.processing.util.asMutableKClassName
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getAllFieldNames
import androidx.room.compiler.processing.util.getDeclaredField
import androidx.room.compiler.processing.util.getDeclaredMethodByJvmName
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.kspProcessingEnv
import androidx.room.compiler.processing.util.runJavaProcessorTest
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JTypeVariableName
import com.squareup.kotlinpoet.javapoet.KClassName
import com.squareup.kotlinpoet.javapoet.KTypeVariableName
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class XTypeElementTest(
    private val isPreCompiled: Boolean,
) {
    private fun runTest(
        sources: List<Source>,
        kotlincArgs: List<String> = emptyList(),
        handler: (XTestInvocation) -> Unit
    ) {
        if (isPreCompiled) {
            val compiled = compileFiles(sources)
            val hasKotlinSources = sources.any { it is Source.KotlinSource }
            val kotlinSources =
                if (hasKotlinSources) {
                    listOf(Source.kotlin("placeholder.kt", "class PlaceholderKotlin"))
                } else {
                    emptyList()
                }
            val newSources =
                kotlinSources +
                    Source.java("PlaceholderJava", "public class " + "PlaceholderJava {}")
            runProcessorTest(
                sources = newSources,
                handler = handler,
                classpath = compiled,
                kotlincArguments = kotlincArgs
            )
        } else {
            runProcessorTest(sources = sources, handler = handler, kotlincArguments = kotlincArgs)
        }
    }

    @Test
    fun qualifiedNames() {
        val src1 =
            Source.kotlin(
                "Foo.kt",
                """
            class TopLevel
            """
                    .trimIndent()
            )
        val src2 =
            Source.kotlin(
                "Bar.kt",
                """
            package foo.bar
            class InFooBar
            """
                    .trimIndent()
            )
        val src3 =
            Source.java(
                "foo.bar.Outer",
                """
            package foo.bar;
            public class Outer {
                public static class Nested {
                }
            }
            """
            )
        runTest(sources = listOf(src1, src2, src3)) { invocation ->
            invocation.processingEnv.requireTypeElement("TopLevel").let {
                assertThat(it.packageName).isEqualTo("")
                assertThat(it.name).isEqualTo("TopLevel")
                assertThat(it.qualifiedName).isEqualTo("TopLevel")
                assertThat(it.asClassName().java).isEqualTo(JClassName.get("", "TopLevel"))
                if (invocation.isKsp) {
                    assertThat(it.asClassName().kotlin).isEqualTo(KClassName("", "TopLevel"))
                } else {
                    // In javac / kapt we don't have KotlinPoet names
                    assertThat(it.asClassName().kotlin).isEqualTo(XTypeName.UNAVAILABLE_KTYPE_NAME)
                }
            }
            invocation.processingEnv.requireTypeElement("foo.bar.InFooBar").let {
                assertThat(it.packageName).isEqualTo("foo.bar")
                assertThat(it.name).isEqualTo("InFooBar")
                assertThat(it.qualifiedName).isEqualTo("foo.bar.InFooBar")
                assertThat(it.asClassName()).isEqualTo(XClassName.get("foo.bar", "InFooBar"))
            }
            invocation.processingEnv.requireTypeElement("foo.bar.Outer").let {
                assertThat(it.packageName).isEqualTo("foo.bar")
                assertThat(it.name).isEqualTo("Outer")
                assertThat(it.qualifiedName).isEqualTo("foo.bar.Outer")
                assertThat(it.asClassName()).isEqualTo(XClassName.get("foo.bar", "Outer"))
            }
            invocation.processingEnv.requireTypeElement("foo.bar.Outer.Nested").let {
                assertThat(it.packageName).isEqualTo("foo.bar")
                assertThat(it.name).isEqualTo("Nested")
                assertThat(it.qualifiedName).isEqualTo("foo.bar.Outer.Nested")
                assertThat(it.asClassName()).isEqualTo(XClassName.get("foo.bar", "Outer", "Nested"))
            }
            if (invocation.isKsp) {
                // these are KSP specific tests, typenames are tested elsewhere
                invocation.processingEnv.requireTypeElement("java.lang.Integer").let {
                    // always return kotlin types, this is what compiler does
                    assertThat(it.packageName).isEqualTo("kotlin")
                    assertThat(it.name).isEqualTo("Int")
                    assertThat(it.qualifiedName).isEqualTo("kotlin.Int")
                    assertThat(it.asClassName().java).isEqualTo(JTypeName.INT.box())
                    assertThat(it.asClassName().kotlin).isEqualTo(INT)
                }
                invocation.processingEnv.requireTypeElement("kotlin.Int").let {
                    assertThat(it.packageName).isEqualTo("kotlin")
                    assertThat(it.name).isEqualTo("Int")
                    assertThat(it.qualifiedName).isEqualTo("kotlin.Int")
                    assertThat(it.asClassName().java).isEqualTo(JTypeName.INT.box())
                    assertThat(it.asClassName().kotlin).isEqualTo(INT)
                }
            }
        }
    }

    @Test
    fun typeAndSuperType() {
        val src =
            Source.kotlin(
                "foo.kt",
                """
            package foo.bar
            class Baz : MyInterface, AbstractClass()
            abstract class AbstractClass
            interface MyInterface
            interface AnotherInterface : MyInterface
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("foo.bar.Baz").let {
                assertThat(it.superClass!!.asTypeName())
                    .isEqualTo(
                        invocation.processingEnv.requireType("foo.bar.AbstractClass").asTypeName()
                    )
                assertThat(it.type.superTypes.map(XType::asTypeName))
                    .containsExactly(
                        invocation.processingEnv.requireType("foo.bar.AbstractClass").asTypeName(),
                        invocation.processingEnv.requireType("foo.bar.MyInterface").asTypeName()
                    )
                assertThat(it.type.asTypeName())
                    .isEqualTo(invocation.processingEnv.requireType("foo.bar.Baz").asTypeName())
                assertThat(it.isInterface()).isFalse()
                assertThat(it.isKotlinObject()).isFalse()
                assertThat(it.isAbstract()).isFalse()
            }
            invocation.processingEnv.requireTypeElement("foo.bar.AbstractClass").let {
                assertThat(it.superClass!!.asTypeName())
                    .isEqualTo(
                        invocation.processingEnv.requireType(XTypeName.ANY_OBJECT).asTypeName()
                    )
                assertThat(it.type.superTypes.map(XType::asTypeName))
                    .containsExactly(
                        invocation.processingEnv.requireType(XTypeName.ANY_OBJECT).asTypeName()
                    )
                assertThat(it.isAbstract()).isTrue()
                assertThat(it.isInterface()).isFalse()
                assertThat(it.type.asTypeName())
                    .isEqualTo(
                        invocation.processingEnv.requireType("foo.bar.AbstractClass").asTypeName()
                    )
            }
            invocation.processingEnv.requireTypeElement("foo.bar.MyInterface").let {
                assertThat(it.superClass).isNull()
                assertThat(it.type.superTypes.map(XType::asTypeName))
                    .containsExactly(
                        invocation.processingEnv.requireType(XTypeName.ANY_OBJECT).asTypeName()
                    )
                assertThat(it.isInterface()).isTrue()
                assertThat(it.type.asTypeName())
                    .isEqualTo(
                        invocation.processingEnv.requireType("foo.bar.MyInterface").asTypeName()
                    )
            }
            invocation.processingEnv.requireTypeElement("foo.bar.AnotherInterface").let {
                assertThat(it.superClass).isNull()
                assertThat(it.type.superTypes.map(XType::asTypeName))
                    .containsExactly(
                        invocation.processingEnv.requireType("java.lang.Object").asTypeName(),
                        invocation.processingEnv.requireType("foo.bar.MyInterface").asTypeName()
                    )
                assertThat(it.isInterface()).isTrue()
                assertThat(it.type.asTypeName())
                    .isEqualTo(
                        invocation.processingEnv
                            .requireType("foo.bar.AnotherInterface")
                            .asTypeName()
                    )
            }
        }
    }

    @Test
    fun superTypeWithNoSuperClass() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "foo.bar.KotlinClass.kt",
                        """
                    package foo.bar
                    class KotlinClass
                    class KotlinClassWithInterface : KotlinInterface
                    interface KotlinInterface
                    """
                            .trimIndent()
                    ),
                    Source.java(
                        "foo.bar.JavaClass",
                        """
                    package foo.bar;
                    class JavaClass {}
                    class JavaClassWithInterface implements JavaInterface {}
                    interface JavaInterface {}
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            invocation.processingEnv.requireTypeElement("foo.bar.KotlinClass").let {
                assertThat(it.superClass?.asTypeName()).isEqualTo(XTypeName.ANY_OBJECT)
            }
            invocation.processingEnv.requireTypeElement("foo.bar.KotlinClassWithInterface").let {
                assertThat(it.superClass?.asTypeName()).isEqualTo(XTypeName.ANY_OBJECT)
            }
            invocation.processingEnv.requireTypeElement("foo.bar.JavaClass").let {
                assertThat(it.superClass?.asTypeName()).isEqualTo(XTypeName.ANY_OBJECT)
            }
            invocation.processingEnv.requireTypeElement("foo.bar.JavaClassWithInterface").let {
                assertThat(it.superClass?.asTypeName()).isEqualTo(XTypeName.ANY_OBJECT)
            }
        }
    }

    @Test
    fun superTypeWithAlias() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "foo.bar.KotlinClass.kt",
                        """
                    package foo.bar
                    class MyClass : MyAliasClass(), MyAliasInterface
                    typealias MyAliasClass = MyBaseClass
                    typealias MyAliasInterface = MyBaseInterface
                    abstract class MyBaseClass
                    interface MyBaseInterface
                    """
                            .trimIndent()
                    ),
                )
        ) { invocation ->
            invocation.processingEnv.requireTypeElement("foo.bar.MyClass").let { myClass ->
                val myBaseClassName = XClassName.get("foo.bar", "MyBaseClass")
                val myBaseInterfaceName = XClassName.get("foo.bar", "MyBaseInterface")
                assertThat(myClass.superClass?.asTypeName()).isEqualTo(myBaseClassName)
                assertThat(myClass.superInterfaces.map { it.asTypeName() }.toList())
                    .containsExactly(myBaseInterfaceName)
                assertThat(myClass.getSuperInterfaceElements().map { it.asClassName() }.toList())
                    .containsExactly(myBaseInterfaceName)
                assertThat(myClass.type.superTypes.map { it.asTypeName() }.toList())
                    .containsExactly(myBaseClassName, myBaseInterfaceName)
                    .inOrder()
            }
        }
    }

    @Test
    fun superTypeOfAny() {
        runTest(sources = listOf()) { invocation ->
            val any = invocation.processingEnv.requireTypeElement(Any::class)
            val obj = invocation.processingEnv.requireTypeElement(Object::class)
            assertThat(any.superClass).isNull()
            assertThat(obj.superClass).isNull()
        }
    }

    @Test
    fun superInterfaces() {
        val src =
            Source.kotlin(
                "foo.kt",
                """
            package foo.bar
            class Baz : MyInterface<String>, AbstractClass()
            abstract class AbstractClass
            interface MyInterface<E>
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("foo.bar.Baz").let {
                assertThat(it.superInterfaces).hasSize(1)
                val superInterface =
                    it.superInterfaces.first { type ->
                        type.rawType.toString() == "foo.bar.MyInterface"
                    }
                assertThat(superInterface.typeArguments).hasSize(1)
                assertThat(superInterface.typeArguments[0].asTypeName().java)
                    .isEqualTo(JClassName.get("java.lang", "String"))
                if (invocation.isKsp) {
                    assertThat(superInterface.typeArguments[0].asTypeName().kotlin)
                        .isEqualTo(KClassName("kotlin", "String"))
                }
                if (!invocation.isKsp) {
                    assertThat((superInterface as JavacType).kotlinType).isNotNull()
                }
            }
        }
    }

    @Test
    fun nestedClassName() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            package foo.bar
            class Outer {
                class Inner
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("foo.bar.Outer").let {
                assertThat(it.asClassName()).isEqualTo(XClassName.get("foo.bar", "Outer"))
                assertThat(it.isNested()).isFalse()
                assertThat(it.enclosingTypeElement).isNull()
            }
            invocation.processingEnv.requireTypeElement("foo.bar.Outer.Inner").let {
                assertThat(it.asClassName()).isEqualTo(XClassName.get("foo.bar", "Outer", "Inner"))
                assertThat(it.packageName).isEqualTo("foo.bar")
                assertThat(it.name).isEqualTo("Inner")
                assertThat(it.isNested()).isTrue()
                assertThat(it.enclosingTypeElement)
                    .isEqualTo(invocation.processingEnv.requireTypeElement("foo.bar.Outer"))
            }
        }
    }

    @Test
    fun modifiers() {
        val kotlinSrc =
            Source.kotlin(
                "Foo.kt",
                """
            open class OpenClass
            abstract class AbstractClass
            object MyObject
            interface MyInterface
            class Final
            private class PrivateClass
            class OuterKotlinClass {
                inner class InnerKotlinClass
                class NestedKotlinClass
            }
            annotation class KotlinAnnotation
            data class DataClass(val foo: Int)
            inline class InlineClass(val foo: Int)
            fun interface FunInterface {
               fun foo()
            }
            """
                    .trimIndent()
            )
        val javaSrc =
            Source.java(
                "OuterJavaClass",
                """
            public class OuterJavaClass {
                public class InnerJavaClass {}
                public static class NestedJavaClass {}
            }
            """
                    .trimIndent()
            )
        val javaAnnotationSrc =
            Source.java(
                "JavaAnnotation",
                """
            public @interface JavaAnnotation {
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(kotlinSrc, javaSrc, javaAnnotationSrc)) { invocation ->
            fun getModifiers(element: XTypeElement): Set<String> {
                val result = mutableSetOf<String>()
                if (element.isAbstract()) result.add("abstract")
                if (element.isFinal()) result.add("final")
                if (element.isPrivate() || element.isKtPrivate()) result.add("private")
                if (element.isProtected()) result.add("protected")
                if (element.isPublic()) result.add("public")
                if (element.isKotlinObject()) result.add("object")
                if (element.isCompanionObject()) result.add("companion")
                if (element.isFunctionalInterface()) result.add("fun")
                if (element.isClass()) result.add("class")
                if (element.isDataClass()) result.add("data")
                if (element.isValueClass()) result.add("value")
                if (element.isExpect()) result.add("expect")
                if (element.isInterface()) result.add("interface")
                if (element.isStatic()) result.add("static")
                if (element.isAnnotationClass()) result.add("annotation")
                return result
            }

            fun getModifiers(qName: String): Set<String> =
                getModifiers(invocation.processingEnv.requireTypeElement(qName))

            assertThat(getModifiers("OpenClass")).containsExactly("public", "class")
            assertThat(getModifiers("AbstractClass")).containsExactly("abstract", "public", "class")
            assertThat(getModifiers("MyObject")).containsExactly("final", "public", "object")
            assertThat(getModifiers("MyInterface"))
                .containsExactly("abstract", "interface", "public")
            assertThat(getModifiers("Final")).containsExactly("final", "public", "class")
            assertThat(getModifiers("PrivateClass")).containsExactly("private", "final", "class")
            assertThat(getModifiers("OuterKotlinClass.InnerKotlinClass"))
                .containsExactly("final", "public", "class")
            assertThat(getModifiers("OuterKotlinClass.NestedKotlinClass"))
                .containsExactly("final", "public", "static", "class")
            assertThat(getModifiers("OuterJavaClass.InnerJavaClass"))
                .containsExactly("public", "class")
            assertThat(getModifiers("OuterJavaClass.NestedJavaClass"))
                .containsExactly("public", "static", "class")
            assertThat(getModifiers("JavaAnnotation")).apply {
                // KSP vs KAPT metadata have a difference in final vs abstract modifiers
                // for annotation types.
                if (isPreCompiled && invocation.isKsp) {
                    containsExactly("final", "public", "annotation")
                } else {
                    containsExactly("abstract", "public", "annotation")
                }
            }
            assertThat(getModifiers("KotlinAnnotation")).apply {
                // KSP vs KAPT metadata have a difference in final vs abstract modifiers
                // for annotation types.
                if (invocation.isKsp) {
                    containsExactly("final", "public", "annotation")
                } else {
                    containsExactly("abstract", "public", "annotation")
                }
            }
            assertThat(getModifiers("DataClass"))
                .containsExactly("public", "final", "class", "data")
            assertThat(getModifiers("InlineClass")).apply {
                containsExactly("public", "final", "class", "value")
            }
            assertThat(getModifiers("FunInterface"))
                .containsExactly("public", "abstract", "interface", "fun")
        }
    }

    @Test
    fun kindName() {
        val kotlinSrc =
            Source.kotlin(
                "Foo.kt",
                """
            class KotlinClass
            interface KotlinInterface
            annotation class KotlinAnnotation
            """
                    .trimIndent()
            )
        val javaSrc =
            Source.java(
                "Bar",
                """
            class JavaClass {}
            interface JavaInterface {}
            @interface JavaAnnotation {}
            """
                    .trimIndent()
            )
        runTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            invocation.processingEnv.requireTypeElement("KotlinClass").let {
                assertThat(it.kindName()).isEqualTo("class")
            }
            invocation.processingEnv.requireTypeElement("KotlinInterface").let {
                assertThat(it.kindName()).isEqualTo("interface")
            }
            invocation.processingEnv.requireTypeElement("KotlinAnnotation").let {
                // TODO(b/270557392): make the result consistent between KSP and JavaAP
                if (invocation.isKsp) {
                    assertThat(it.kindName()).isEqualTo("annotation_class")
                } else {
                    assertThat(it.kindName()).isEqualTo("annotation_type")
                }
            }

            invocation.processingEnv.requireTypeElement("JavaClass").let {
                assertThat(it.kindName()).isEqualTo("class")
            }
            invocation.processingEnv.requireTypeElement("JavaInterface").let {
                assertThat(it.kindName()).isEqualTo("interface")
            }
            invocation.processingEnv.requireTypeElement("JavaAnnotation").let {
                // TODO(b/270557392): make the result consistent between KSP and JavaAP
                if (invocation.isKsp) {
                    assertThat(it.kindName()).isEqualTo("annotation_class")
                } else {
                    assertThat(it.kindName()).isEqualTo("annotation_type")
                }
            }
        }
    }

    @Test
    fun fieldBasic() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            open class BaseClass<T>(val genericProp : T) {
                fun baseMethod(input: T) {}
            }
            class SubClass(x : Int) : BaseClass<Int>(x) {
                val subClassProp : String = "abc"
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val baseClass = invocation.processingEnv.requireTypeElement("BaseClass")
            assertThat(baseClass.getAllFieldNames()).containsExactly("genericProp")
            assertThat(baseClass.getDeclaredFields().map { it.name }).containsExactly("genericProp")
            val subClass = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(subClass.getAllFieldNames()).containsExactly("genericProp", "subClassProp")
            assertThat(subClass.getDeclaredFields().map { it.name }).containsExactly("subClassProp")

            val baseMethod = baseClass.getMethodByJvmName("baseMethod")
            baseMethod.asMemberOf(subClass.type).let { methodType ->
                val genericArg = methodType.parameterTypes.first()
                assertThat(genericArg.asTypeName()).isEqualTo(Int::class.asClassName())
            }

            baseClass.getField("genericProp").let { field ->
                assertThat(field.type.asTypeName()).isEqualTo(XTypeName.getTypeVariableName("T"))
            }

            subClass.getField("genericProp").let { field ->
                // this is tricky because even though it is non-null it, it should still be boxed
                assertThat(field.asMemberOf(subClass.type).asTypeName())
                    .isEqualTo(Int::class.asClassName())
            }
        }
    }

    @Test
    fun fieldsOverride() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            open class BaseClass(
                open val value : List<Int>
            )
            class SubClass(
                override val value : MutableList<Int>
            ) : BaseClass(value)
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val baseClass = invocation.processingEnv.requireTypeElement("BaseClass")
            assertThat(baseClass.getAllFieldNames()).containsExactly("value")
            val subClass = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(subClass.getAllFieldNames()).containsExactly("value")
            val baseFieldType = baseClass.getField("value").type
            val subFieldType = subClass.getField("value").type
            assertThat(baseFieldType.asTypeName().java)
                .isEqualTo(JParameterizedTypeName.get(List::class.java, Integer::class.java))
            assertThat(subFieldType.asTypeName().java)
                .isEqualTo(JParameterizedTypeName.get(List::class.java, Integer::class.java))
            if (invocation.isKsp) {
                assertThat(baseFieldType.asTypeName().kotlin)
                    .isEqualTo(List::class.asKClassName().parameterizedBy(INT))
                assertThat(subFieldType.asTypeName().kotlin)
                    .isEqualTo(List::class.asMutableKClassName().parameterizedBy(INT))
            }
        }
    }

    @Test
    fun fieldsMethodsWithoutBacking() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "Foo.kt",
                        """
            package test
            class Subject {
                val realField: String = ""
                    get() = field
                val noBackingVal: String
                    get() = ""
                var noBackingVar: String
                    get() = ""
                    set(value) {}

                companion object {
                    @JvmStatic
                    val staticRealField: String = ""
                    get() = field
                    @JvmStatic
                    val staticNoBackingVal: String
                        get() = ""
                    @JvmStatic
                    var staticNoBackingVar: String
                        get() = ""
                        set(value) {}
                }
            }
            """
                            .trimIndent()
                    )
                ),
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("test.Subject")
            val declaredFields =
                subject.getDeclaredFields().map { it.name } -
                    listOf("Companion") // skip Companion, KAPT generates it
            val expectedFields = listOf("realField", "staticRealField")
            assertWithMessage(subject.qualifiedName)
                .that(declaredFields)
                .containsExactlyElementsIn(expectedFields)
            val allFields =
                subject.getAllFieldsIncludingPrivateSupers().map { it.name } -
                    listOf("Companion") // skip Companion, KAPT generates it
            assertWithMessage(subject.qualifiedName)
                .that(allFields.toList())
                .containsExactlyElementsIn(expectedFields)
            val methodNames = subject.getDeclaredMethods().map { it.jvmName }
            assertWithMessage(subject.qualifiedName)
                .that(methodNames)
                .containsAtLeast("getNoBackingVal", "getNoBackingVar", "setNoBackingVar")
            assertWithMessage(subject.qualifiedName)
                .that(methodNames)
                .doesNotContain("setNoBackingVal")
        }
    }

    @Test
    fun abstractFields() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "Foo.kt",
                        """
            package test
            abstract class Subject {
                val value: String = ""
                abstract val abstractValue: String
                companion object {
                    var realCompanion: String = ""
                    @JvmStatic
                    var jvmStatic: String = ""
                }
            }
            """
                            .trimIndent()
                    )
                ),
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("test.Subject")
            val declaredFields = subject.getDeclaredFields().map { it.name } - listOf("Companion")
            val expectedFields = listOf("value", "realCompanion", "jvmStatic")
            assertWithMessage(subject.qualifiedName)
                .that(declaredFields)
                .containsExactlyElementsIn(expectedFields)
        }
    }

    @Test
    fun lateinitFields() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "Foo.kt",
                        """
            package test
            class Subject {
                lateinit var x:String
                var y:String = "abc"
            }
            """
                            .trimIndent()
                    )
                ),
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("test.Subject")
            assertWithMessage(subject.fallbackLocationText)
                .that(subject.getDeclaredFields().map { it.name })
                .containsExactly("x", "y")
            assertWithMessage(subject.fallbackLocationText)
                .that(subject.getDeclaredMethods().map { it.jvmName })
                .containsExactly("getX", "setX", "getY", "setY")
            subject.getField("x").let { field ->
                assertThat(field.isFinal()).isFalse()
                assertThat(field.isPrivate()).isFalse()
            }
        }
    }

    @Test
    fun fieldsInInterfaces() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            annotation class MyAnnotation
            interface MyInterface {
                var x: Int
                var y: Int
                  @MyAnnotation get
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("MyInterface")
            assertThat(element.getAllFieldsIncludingPrivateSupers().toList()).isEmpty()
            element.getMethodByJvmName("getX").let { assertThat(it.isAbstract()).isTrue() }
            element.getMethodByJvmName("setX").let { assertThat(it.isAbstract()).isTrue() }
            element.getMethodByJvmName("getY").let {
                if (!isPreCompiled && invocation.isKsp) {
                    // The modifier set is empty for both the property and accessor
                    assertThat(it.isAbstract()).isFalse()
                } else {
                    assertThat(it.isAbstract()).isTrue()
                }
            }
        }
    }

    @Test
    fun fieldsInAbstractClass() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            abstract class MyAbstractClass {
                @JvmField
                var jvmVar: Int = 0
                abstract var abstractVar: Int
                var nonAbstractVar: Int = 0
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("MyAbstractClass")
            assertThat(element.getAllFieldNames()).containsExactly("nonAbstractVar", "jvmVar")
            assertThat(element.getDeclaredMethods().map { it.jvmName })
                .containsExactly(
                    "getAbstractVar",
                    "setAbstractVar",
                    "getNonAbstractVar",
                    "setNonAbstractVar"
                )
            element.getMethodByJvmName("getAbstractVar").let {
                assertThat(it.isAbstract()).isTrue()
            }
            element.getMethodByJvmName("setAbstractVar").let {
                assertThat(it.isAbstract()).isTrue()
            }

            element.getMethodByJvmName("getNonAbstractVar").let {
                assertThat(it.isAbstract()).isFalse()
            }
            element.getMethodByJvmName("setNonAbstractVar").let {
                assertThat(it.isAbstract()).isFalse()
            }
        }
    }

    @Test
    fun propertyGettersSetters() {
        runTest(
            listOf(
                Source.java(
                    "JavaSubject",
                    """
                    class JavaSubject {
                        int myField;
                        private int mutable;
                        int immutable;
                        int getMutable() {return 3;}
                        void setMutable(int x) {}
                        int getImmutable() {return 3;}
                    }
                    """
                        .trimIndent()
                ),
                Source.kotlin(
                    "KotlinSubject.kt",
                    """
                    class KotlinSubject {
                        private val myField = 0
                        var mutable: Int = 0
                        val immutable:Int = 0
                    }
                    """
                        .trimIndent()
                )
            ),
        ) { invocation ->
            listOf("JavaSubject", "KotlinSubject")
                .map { invocation.processingEnv.requireTypeElement(it) }
                .forEach { subject ->
                    val methods = subject.getDeclaredMethods()
                    assertWithMessage(subject.qualifiedName)
                        .that(methods.map { it.jvmName })
                        .containsExactly("getMutable", "setMutable", "getImmutable")

                    subject.getDeclaredMethodByJvmName("getMutable").let {
                        if (subject.name.contains("Kotlin")) {
                            assertThat(it.isKotlinPropertyMethod()).isTrue()
                            assertThat(it.isKotlinPropertySetter()).isFalse()
                            assertThat(it.isKotlinPropertyGetter()).isTrue()
                            assertThat(it.propertyName).isEqualTo("mutable")
                        } else {
                            assertThat(it.isKotlinPropertyMethod()).isFalse()
                            assertThat(it.isKotlinPropertySetter()).isFalse()
                            assertThat(it.isKotlinPropertyGetter()).isFalse()
                            assertThat(it.propertyName).isNull()
                        }
                    }

                    subject.getDeclaredMethodByJvmName("setMutable").let {
                        if (subject.name.contains("Kotlin")) {
                            assertThat(it.isKotlinPropertyMethod()).isTrue()
                            assertThat(it.isKotlinPropertySetter()).isTrue()
                            assertThat(it.propertyName).isEqualTo("mutable")
                        } else {
                            assertThat(it.isKotlinPropertyMethod()).isFalse()
                            assertThat(it.isKotlinPropertySetter()).isFalse()
                            assertThat(it.isKotlinPropertyGetter()).isFalse()
                            assertThat(it.propertyName).isNull()
                        }
                    }

                    subject.getDeclaredMethodByJvmName("getImmutable").let {
                        if (subject.name.contains("Kotlin")) {
                            assertThat(it.isKotlinPropertyMethod()).isTrue()
                            assertThat(it.isKotlinPropertySetter()).isFalse()
                            assertThat(it.isKotlinPropertyGetter()).isTrue()
                            assertThat(it.propertyName).isEqualTo("immutable")
                        } else {
                            assertThat(it.isKotlinPropertyMethod()).isFalse()
                            assertThat(it.isKotlinPropertySetter()).isFalse()
                            assertThat(it.isKotlinPropertyGetter()).isFalse()
                            assertThat(it.propertyName).isNull()
                        }
                    }
                }
        }
    }

    @Test
    fun lazyProperty() {
        val src =
            Source.kotlin(
                "Subject.kt",
                """
            class Subject {
              val myLazy by lazy { "wow" }
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) {
            val subject = it.processingEnv.requireTypeElement("Subject")
            val fields = subject.getDeclaredFields()
            assertThat(fields).isEmpty()

            val method = subject.getDeclaredMethodByJvmName("getMyLazy")
            assertThat(method).isNotNull()
            assertThat(method.isKotlinPropertyMethod()).isTrue()
            assertThat(method.isKotlinPropertySetter()).isFalse()
        }
    }

    @Test
    fun declaredAndInstanceMethods() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            open class Base(x:Int) {
                open fun baseFun(): Int = TODO()
                suspend fun suspendFun(): Int = TODO()
                companion object {
                    @JvmStatic
                    fun staticBaseFun(): Int = TODO()
                    fun companionMethod(): Int = TODO()
                    @JvmStatic val name: String get() = "hello"
                    @JvmStatic suspend fun suspendFun2():Int = TODO()
                    @JvmStatic fun String.extFun(): Int = TODO()
                }
                private fun privateBaseFun(): Int = TODO()
            }
            open class SubClass : Base {
                constructor(y:Int): super(y) {
                }
                constructor(x:Int, y:Int): super(y) {
                }
                override fun baseFun(): Int = TODO()
                fun subFun(): Int = TODO()
                private fun privateSubFun(): Int = TODO()
                companion object {
                    @JvmStatic
                    fun staticFun(): Int = TODO()
                }
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val base = invocation.processingEnv.requireTypeElement("Base")
            val baseCompanion = invocation.processingEnv.requireTypeElement("Base.Companion")
            val objectMethodNames = invocation.objectMethodNames()
            val declaredMethods = base.getDeclaredMethods()
            assertThat(declaredMethods.jvmNames())
                .containsExactly(
                    "baseFun",
                    "suspendFun",
                    "privateBaseFun",
                    "staticBaseFun",
                    "getName",
                    "suspendFun2",
                    "extFun"
                )
                .inOrder()
            declaredMethods.forEach { method ->
                assertWithMessage("Enclosing element of method ${method.jvmName}")
                    .that(method.enclosingElement.name)
                    .isEqualTo("Base")
            }
            baseCompanion.getDeclaredMethods().forEach { method ->
                assertWithMessage("Enclosing element of method ${method.jvmName}")
                    .that(method.enclosingElement.name)
                    .isEqualTo("Companion")
            }

            val sub = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(sub.getDeclaredMethods().jvmNames())
                .containsExactly("baseFun", "subFun", "privateSubFun", "staticFun")
            assertThat(sub.getAllNonPrivateInstanceMethods().jvmNames() - objectMethodNames)
                .containsExactly("baseFun", "suspendFun", "subFun")
        }
    }

    @Test
    fun diamondOverride() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "Foo.kt",
                        """
            package test
            interface Parent<T> {
                fun parent(t: T)
            }

            interface Child1<T> : Parent<T> {
                fun child1(t: T)
            }

            interface Child2<T> : Parent<T> {
                fun child2(t: T)
            }

            abstract class Subject1 : Child1<String>, Child2<String>, Parent<String>
            abstract class Subject2 : Child1<String>, Parent<String>
            abstract class Subject3 : Child1<String>, Parent<String> {
                abstract override fun parent(t: String)
            }
            """
                            .trimIndent()
                    )
                ),
        ) { invocation ->
            invocation.processingEnv.requireTypeElement("test.Subject1").let { subject ->
                assertWithMessage(subject.qualifiedName)
                    .that(invocation.nonObjectMethodSignatures(subject))
                    .containsExactly(
                        "child1(java.lang.String):void",
                        "child2(java.lang.String):void",
                        "parent(java.lang.String):void",
                    )
            }
            invocation.processingEnv.requireTypeElement("test.Subject2").let { subject ->
                assertWithMessage(subject.qualifiedName)
                    .that(invocation.nonObjectMethodSignatures(subject))
                    .containsExactly(
                        "child1(java.lang.String):void",
                        "parent(java.lang.String):void",
                    )
            }
            invocation.processingEnv.requireTypeElement("test.Subject3").let { subject ->
                assertWithMessage(subject.qualifiedName)
                    .that(invocation.nonObjectMethodSignatures(subject))
                    .containsExactly(
                        "child1(java.lang.String):void",
                        "parent(java.lang.String):void",
                    )
            }
        }
    }

    @Test
    fun suspendOverride() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            interface Base<T> {
                suspend fun get(): T
                suspend fun getAll(): List<T>
                @JvmSuppressWildcards
                suspend fun getAllSuppressWildcards(): List<T>
                suspend fun putAll(input: List<T>)
                suspend fun getAllWithDefault(): List<T>
            }

            interface DerivedInterface : Base<String> {
                override suspend fun get(): String
                override suspend fun getAll(): List<String>
                @JvmSuppressWildcards
                override suspend fun getAllSuppressWildcards(): List<String>
                override suspend fun putAll(input: List<String>)
                override suspend fun getAllWithDefault(): List<String> {
                    return emptyList()
                }
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val base = invocation.processingEnv.requireTypeElement("DerivedInterface")
            val methodNames = base.getAllMethods().toList().jvmNames()
            assertThat(methodNames)
                .containsExactly(
                    "get",
                    "getAll",
                    "getAllSuppressWildcards",
                    "putAll",
                    "getAllWithDefault"
                )
        }
    }

    @Test
    fun suspendOverride_abstractClass() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            abstract class Base<T> {
                abstract suspend fun get(): T
                abstract suspend fun getAll(): List<T>
                abstract suspend fun putAll(input: List<T>)
            }

            abstract class DerivedClass : Base<Int>() {
                abstract override suspend fun get(): Int
                abstract override suspend fun getAll(): List<Int>
                override suspend fun putAll(input: List<Int>) {
                }
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val base = invocation.processingEnv.requireTypeElement("DerivedClass")
            val methodNamesCount =
                base.getAllMethods().toList().jvmNames().groupingBy { it }.eachCount()
            assertThat(methodNamesCount["get"]).isEqualTo(1)
            assertThat(methodNamesCount["getAll"]).isEqualTo(1)
            assertThat(methodNamesCount["putAll"]).isEqualTo(1)
        }
    }

    // b/274328611
    @Test
    fun suspendOverride_distinct() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            data class Foo(val txt: String)

            interface Base {
                suspend fun getAll(): List<Foo>
            }

            abstract class DerivedClass : Base {
                abstract suspend fun getAll(param: String): List<Foo>
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val base = invocation.processingEnv.requireTypeElement("DerivedClass")
            val methodNamesCount =
                base.getAllMethods().toList().jvmNames().groupingBy { it }.eachCount()
            assertThat(methodNamesCount["getAll"]).isEqualTo(2)
        }
    }

    @Test
    fun overrideMethodWithCovariantReturnType() {
        val src =
            Source.kotlin(
                "ParentWithExplicitOverride.kt",
                """
            interface ParentWithExplicitOverride: ChildInterface, Child {
                override fun child(): Child
            }

            interface ParentWithoutExplicitOverride: ChildInterface, Child

            interface Child: ChildInterface {
                override fun child(): Child
            }

            interface ChildInterface {
                fun child(): ChildInterface
            }
            """
                    .trimIndent()
            )

        runTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("ParentWithExplicitOverride").let { parent
                ->
                assertWithMessage(parent.qualifiedName)
                    .that(invocation.nonObjectMethodSignatures(parent))
                    .containsExactly("child():Child")
            }

            invocation.processingEnv.requireTypeElement("ParentWithoutExplicitOverride").let {
                parent ->
                assertWithMessage(parent.qualifiedName)
                    .that(invocation.nonObjectMethodSignatures(parent))
                    .containsExactly("child():Child")
            }
        }
    }

    @Test
    fun allMethodsFiltersInAccessibleMethods() {
        val srcs =
            listOf(
                Source.java(
                    "foo.Foo",
                    """
                package foo;
                public interface Foo {
                    void foo_Public();
                }
                """
                        .trimIndent()
                ),
                Source.java(
                    "foo.parent.FooParent",
                    """
                package foo.parent;
                public abstract class FooParent implements foo.Foo {
                    public void fooParent_Public() {}
                    protected void fooParent_Protected() {}
                    private void fooParent_Private() {}
                    void fooParent_PackagePrivate() {}
                }
                """
                        .trimIndent()
                ),
                Source.java(
                    "foo.child.FooChild",
                    """
                package foo.child;
                public abstract class FooChild extends foo.parent.FooParent {
                    public void fooChild_Public() {}
                    protected void fooChild_Protected() {}
                    private void fooChild_Private() {}
                    void fooChild_PackagePrivate() {}
                }
                """
                        .trimIndent()
                ),
            )
        runTest(sources = srcs) { invocation ->
            invocation.processingEnv.requireTypeElement("foo.child.FooChild").let { fooChild ->
                assertWithMessage(fooChild.qualifiedName)
                    .that(invocation.nonObjectMethodSignatures(fooChild))
                    .containsExactly(
                        "foo_Public():void",
                        "fooParent_Public():void",
                        "fooParent_Protected():void",
                        "fooChild_Public():void",
                        "fooChild_Protected():void",
                        "fooChild_Private():void",
                        "fooChild_PackagePrivate():void"
                    )
            }
        }
    }

    @Test
    fun allMethods() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            open class Base(x:Int) {
                constructor(x:Int, y:Int): this(x) {
                }
                fun baseMethod(): Int = TODO()
                open fun overriddenMethod(): Int = TODO()
                private fun privateBaseMethod(): Int = TODO()
                companion object {
                    @JvmStatic
                    private fun privateBaseCompanionMethod(): Int = TODO()
                    @JvmStatic
                    fun baseCompanionMethod(): Int = TODO()
                }
            }
            interface MyInterface {
                fun interfaceMethod(): Int = TODO()
            }
            class SubClass : Base, MyInterface {
                constructor(x:Int): super(x) {
                }
                constructor(x:Int, y:Int): super(y) {
                }
                fun subMethod(): Int = TODO()
                fun privateSubMethod(): Int = TODO()
                override fun overriddenMethod(): Int = TODO()
                override fun interfaceMethod(): Int = TODO()
                companion object {
                    fun dontSeeThisOne(): Int = TODO()
                    @JvmStatic
                    fun subCompanionMethod(): Int = TODO()
                }
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val objectMethodNames = invocation.objectMethodNames()
            val klass = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(klass.getAllMethods().jvmNames() - objectMethodNames)
                .containsExactly(
                    "baseMethod",
                    "overriddenMethod",
                    "baseCompanionMethod",
                    "interfaceMethod",
                    "subMethod",
                    "privateSubMethod",
                    "subCompanionMethod"
                )
        }
    }

    /**
     * When JvmNames is used along with a suppression over the error, the behavior becomes
     * complicated. Normally, JvmName annotation is not allowed in overrides and open methods, yet
     * developers can still use it by putting a suppression over it. The compiler will generate a
     * delegating method in these cases in the .class file, yet in KSP, we don't really see that
     * method (also shouldn't ideally).
     *
     * This test is here to acknowledge that the behavior is inconsistent yet working as intended
     * from XProcessing's perspective.
     *
     * Also see: https://youtrack.jetbrains.com/issue/KT-50782 as a sign why this suppression is not
     * worth supporting :).
     */
    @Test
    fun allMethods_withJvmNames() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "Foo.kt",
                        """
                package test
                interface Interface {
                    fun f1()
                    @JvmName("notF2")
                    @Suppress("INAPPLICABLE_JVM_NAME")
                    fun f2()
                }
                abstract class Subject : Interface {
                    @JvmName("notF1")
                    @Suppress("INAPPLICABLE_JVM_NAME")
                    override fun f1() {
                    }
                }
            """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            val appSubject = invocation.processingEnv.requireTypeElement("test.Subject")
            val methodNames = appSubject.getAllMethods().map { it.name }.toList()
            val methodJvmNames = appSubject.getAllMethods().map { it.jvmName }.toList()
            val objectMethodNames = invocation.objectMethodNames()
            if (invocation.isKsp) {
                assertThat(methodNames - objectMethodNames).containsExactly("f1", "f2")
                if (invocation.kspProcessingEnv.isKsp2 && isPreCompiled) {
                    assertThat(methodJvmNames - objectMethodNames).containsExactly("f1", "f2")
                } else {
                    assertThat(methodJvmNames - objectMethodNames).containsExactly("notF1", "notF2")
                }
            } else {
                assertThat(methodNames - objectMethodNames).containsExactly("f1", "f1", "f2")
                assertThat(methodJvmNames - objectMethodNames)
                    .containsExactly("f1", "notF1", "notF2")
            }
        }
    }

    @Test
    fun gettersSetters() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            open class JustGetter(val x:Int) {
                private val invisible:Int = TODO()
                private var invisibleMutable:Int = TODO()
            }
            class GetterSetter(var y:Int) : JustGetter(y) {
                private val subInvisible:Int = TODO()
                private var subInvisibleMutable:Int = TODO()
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val objectMethodNames = invocation.objectMethodNames()
            invocation.processingEnv.requireTypeElement("JustGetter").let { base ->
                assertThat(base.getDeclaredMethods().jvmNames()).containsExactly("getX")
                assertThat(base.getAllMethods().jvmNames() - objectMethodNames)
                    .containsExactly("getX")
                assertThat(base.getAllNonPrivateInstanceMethods().jvmNames() - objectMethodNames)
                    .containsExactly("getX")
            }
            invocation.processingEnv.requireTypeElement("GetterSetter").let { sub ->
                assertThat(sub.getDeclaredMethods().jvmNames()).containsExactly("getY", "setY")
                assertThat(sub.getAllMethods().jvmNames() - objectMethodNames)
                    .containsExactly("getX", "getY", "setY")
                assertThat(sub.getAllNonPrivateInstanceMethods().jvmNames() - objectMethodNames)
                    .containsExactly("getX", "getY", "setY")
            }
        }
    }

    @Test
    fun companion() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            open class CompanionSubject {
                companion object {
                    @JvmStatic
                    var mutableStatic: String = "a"
                    @JvmStatic
                    val immutableStatic: String = "bar"
                    val companionProp: Int = 3
                    @get:JvmStatic
                    var companionProp_getterJvmStatic:Int =3
                    @set:JvmStatic
                    var companionProp_setterJvmStatic:Int =3

                    fun companionMethod() {
                    }

                    @JvmStatic
                    fun companionMethodWithJvmStatic() {}
                }
            }
            class SubClass : CompanionSubject()
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val objectMethodNames =
                invocation.processingEnv.requireTypeElement(Any::class).getAllMethods().jvmNames()
            val subject = invocation.processingEnv.requireTypeElement("CompanionSubject")
            assertThat(subject.getAllFieldNames() - "Companion")
                .containsExactly(
                    "mutableStatic",
                    "immutableStatic",
                    "companionProp",
                    "companionProp_getterJvmStatic",
                    "companionProp_setterJvmStatic"
                )
            val expectedMethodNames =
                listOf(
                    "getMutableStatic",
                    "setMutableStatic",
                    "getImmutableStatic",
                    "getCompanionProp_getterJvmStatic",
                    "setCompanionProp_setterJvmStatic",
                    "companionMethodWithJvmStatic"
                )
            assertThat(subject.getDeclaredMethods().jvmNames())
                .containsExactlyElementsIn(expectedMethodNames)
            assertThat(subject.getAllMethods().jvmNames() - objectMethodNames)
                .containsExactlyElementsIn(expectedMethodNames)
            assertThat(subject.getAllNonPrivateInstanceMethods().jvmNames() - objectMethodNames)
                .isEmpty()
            val subClass = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(subClass.getDeclaredMethods()).isEmpty()
            assertThat(subClass.getAllMethods().jvmNames() - objectMethodNames)
                .containsExactlyElementsIn(expectedMethodNames)

            // make sure everything coming from companion is marked as static
            subject.getDeclaredFields().forEach {
                assertWithMessage(it.name).that(it.isStatic()).isTrue()
            }
            subject.getDeclaredMethods().forEach {
                assertWithMessage(it.jvmName).that(it.isStatic()).isTrue()
            }

            // make sure asMemberOf works fine for statics
            val subClassType = subClass.type
            subject.getDeclaredFields().forEach {
                try {
                    it.asMemberOf(subClassType)
                } catch (th: Throwable) {
                    throw AssertionError("Couldn't run asMemberOf for ${it.name}")
                }
            }
            subject.getDeclaredMethods().forEach {
                try {
                    it.asMemberOf(subClassType)
                } catch (th: Throwable) {
                    throw AssertionError("Couldn't run asMemberOf for ${it.jvmName}")
                }
            }
        }
    }

    @Test
    fun gettersSetters_interface() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            interface JustGetter {
                val x:Int
            }
            interface GetterSetter : JustGetter {
                var y:Int
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("JustGetter").let { base ->
                assertThat(base.getDeclaredMethods().jvmNames()).containsExactly("getX")
                assertThat(base.getAllMethods().jvmNames()).containsExactly("getX")
                assertThat(base.getAllNonPrivateInstanceMethods().jvmNames())
                    .containsExactly("getX")
            }
            invocation.processingEnv.requireTypeElement("GetterSetter").let { sub ->
                assertThat(sub.getDeclaredMethods().jvmNames()).containsExactly("getY", "setY")
                assertThat(sub.getAllMethods().jvmNames()).containsExactly("getX", "getY", "setY")
                assertThat(sub.getAllNonPrivateInstanceMethods().jvmNames())
                    .containsExactly("getX", "getY", "setY")
            }
        }
    }

    @Test
    fun constructors() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            interface MyInterface
            class NoExplicitConstructor
            open class Base(x:Int)
            open class ExplicitConstructor {
                constructor(x:Int)
            }
            open class BaseWithSecondary(x:Int) {
                constructor(y:String):this(3)
            }
            class Sub(x:Int) : Base(x)
            class SubWith3Constructors() : BaseWithSecondary("abc") {
                constructor(list:List<String>): this()
                constructor(list:List<String>, x:Int): this()
            }
            abstract class AbstractNoExplicit
            abstract class AbstractExplicit(x:Int)
            annotation class AnnotationClass
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val subjects =
                listOf(
                    "MyInterface",
                    "NoExplicitConstructor",
                    "Base",
                    "ExplicitConstructor",
                    "BaseWithSecondary",
                    "Sub",
                    "SubWith3Constructors",
                    "AbstractNoExplicit",
                    "AbstractExplicit",
                    "AnnotationClass"
                )
            val constructorCounts =
                subjects.map {
                    it to invocation.processingEnv.requireTypeElement(it).getConstructors().size
                }
            assertThat(constructorCounts)
                .containsExactly(
                    "MyInterface" to 0,
                    "NoExplicitConstructor" to 1,
                    "Base" to 1,
                    "ExplicitConstructor" to 1,
                    "BaseWithSecondary" to 2,
                    "Sub" to 1,
                    "SubWith3Constructors" to 3,
                    "AbstractNoExplicit" to 1,
                    "AbstractExplicit" to 1,
                    "AnnotationClass" to 0
                )

            val primaryConstructorParameterNames =
                subjects.map {
                    it to
                        invocation.processingEnv
                            .requireTypeElement(it)
                            .findPrimaryConstructor()
                            ?.parameters
                            ?.map { it.name }
                }
            assertThat(primaryConstructorParameterNames)
                .containsExactly(
                    "MyInterface" to null,
                    "NoExplicitConstructor" to emptyList<String>(),
                    "Base" to listOf("x"),
                    "ExplicitConstructor" to null,
                    "BaseWithSecondary" to listOf("x"),
                    "Sub" to listOf("x"),
                    "SubWith3Constructors" to emptyList<String>(),
                    "AbstractNoExplicit" to emptyList<String>(),
                    "AbstractExplicit" to listOf("x"),
                    "AnnotationClass" to null
                )
        }
    }

    @Test
    fun constructorsWithOverloads() {
        val src =
            Source.kotlin(
                "Subject.kt",
                """
            class DefaultArgs @JvmOverloads constructor(x:Int = 1, y: Double, z: Long = 1) {}
            class NoDefaultArgs @JvmOverloads constructor(x:Int, y: Double, z: Long) {}
            class AllDefaultArgs @JvmOverloads constructor(x:Int = 1, y: Double = 0.0, z: Long = 1) {}
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val defaultArgsConstructors =
                invocation.processingEnv
                    .requireTypeElement("DefaultArgs")
                    .getConstructorSignatures()
            val noDefaultArgsConstructors =
                invocation.processingEnv
                    .requireTypeElement("NoDefaultArgs")
                    .getConstructorSignatures()
            val allDefaultArgsConstructors =
                invocation.processingEnv
                    .requireTypeElement("AllDefaultArgs")
                    .getConstructorSignatures()

            if (isPreCompiled) {
                assertThat(defaultArgsConstructors)
                    .containsExactly(
                        "DefaultArgs(int,double,long)",
                        "DefaultArgs(int,double)",
                        "DefaultArgs(double)"
                    )
                    .inOrder()
                assertThat(noDefaultArgsConstructors)
                    .containsExactly("NoDefaultArgs(int,double,long)")
                    .inOrder()
                assertThat(allDefaultArgsConstructors)
                    .containsExactly(
                        "AllDefaultArgs(int,double,long)",
                        "AllDefaultArgs(int,double)",
                        "AllDefaultArgs(int)",
                        "AllDefaultArgs()"
                    )
                    .inOrder()
            } else {
                if (invocation.isKsp) {
                    assertThat(defaultArgsConstructors)
                        .containsExactly(
                            "DefaultArgs(int,double,long)",
                            "DefaultArgs(double)",
                            "DefaultArgs(int,double)",
                        )
                        .inOrder()
                } else {
                    assertThat(defaultArgsConstructors)
                        .containsExactly(
                            "DefaultArgs(double)",
                            "DefaultArgs(int,double)",
                            "DefaultArgs(int,double,long)",
                        )
                        .inOrder()
                }
                assertThat(noDefaultArgsConstructors)
                    .containsExactly("NoDefaultArgs(int,double,long)")
                    .inOrder()
                if (invocation.isKsp) {
                    assertThat(allDefaultArgsConstructors)
                        .containsExactly(
                            "AllDefaultArgs(int,double,long)",
                            "AllDefaultArgs()",
                            "AllDefaultArgs(int)",
                            "AllDefaultArgs(int,double)",
                        )
                        .inOrder()
                } else {
                    assertThat(allDefaultArgsConstructors)
                        .containsExactly(
                            "AllDefaultArgs()",
                            "AllDefaultArgs(int)",
                            "AllDefaultArgs(int,double)",
                            "AllDefaultArgs(int,double,long)",
                        )
                        .inOrder()
                }
            }

            val subjects = listOf("DefaultArgs", "NoDefaultArgs", "AllDefaultArgs")
            if (invocation.isKsp) {
                val syntheticConstructorCounts =
                    subjects.map {
                        it to
                            invocation.processingEnv
                                .requireTypeElement(it)
                                .getConstructors()
                                .filter { it.isSyntheticConstructorForJvmOverloads() }
                                .size
                    }
                assertThat(syntheticConstructorCounts)
                    .containsExactly(
                        "DefaultArgs" to 2,
                        "NoDefaultArgs" to 0,
                        "AllDefaultArgs" to 3,
                    )
            }
        }
    }

    @Test
    fun constructorsWithDefaultValues() {
        val src =
            Source.kotlin(
                "Subject.kt",
                """
            // These should have a no-arg constructor
            class DefaultCtor
            class DefaultArgsPrimary(val x: String = "")
            class AlreadyHasPrimaryNoArgsCtor() {
                constructor(y: Int = 1) : this()
            }
            class AlreadyHasSecondaryNoArgsCtor(val x: String) {
                constructor() : this("")
            }

            // These can't have no arg constructor
            class DefaultArgsSecondary(val x: String) {
                constructor(y: Int = 1) : this("")
            }
            class CantHaveNoArgsCtor(val x: String = "", val y: Int)

            // Shouldn't synthesize no-arg for annotation classes
            annotation class AnnotationClass(val x: String = "")
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val subjects =
                listOf(
                    "DefaultCtor",
                    "DefaultArgsPrimary",
                    "AlreadyHasPrimaryNoArgsCtor",
                    "AlreadyHasSecondaryNoArgsCtor",
                    "DefaultArgsSecondary",
                    "CantHaveNoArgsCtor",
                    "AnnotationClass"
                )
            val constructorCounts =
                subjects.associateWith {
                    invocation.processingEnv.requireTypeElement(it).getConstructors().size
                }
            assertThat(constructorCounts)
                .containsExactlyEntriesIn(
                    mapOf(
                        "DefaultCtor" to 1,
                        "DefaultArgsPrimary" to 2,
                        "AlreadyHasPrimaryNoArgsCtor" to 2,
                        "AlreadyHasSecondaryNoArgsCtor" to 2,
                        "DefaultArgsSecondary" to 2,
                        "CantHaveNoArgsCtor" to 1,
                        "AnnotationClass" to 0,
                    )
                )

            val hasNoArgConstructor =
                subjects.associateWith {
                    invocation.processingEnv.requireTypeElement(it).getConstructors().any {
                        it.parameters.isEmpty()
                    }
                }
            assertThat(hasNoArgConstructor)
                .containsExactlyEntriesIn(
                    mapOf(
                        "DefaultCtor" to true,
                        "DefaultArgsPrimary" to true,
                        "AlreadyHasPrimaryNoArgsCtor" to true,
                        "AlreadyHasSecondaryNoArgsCtor" to true,
                        "DefaultArgsSecondary" to false,
                        "CantHaveNoArgsCtor" to false,
                        "AnnotationClass" to false,
                    )
                )
        }
    }

    @Ignore("b/284452502")
    @Test
    fun jvmDefault() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            interface MyInterface {
                fun notJvmDefault()
                @JvmDefault
                fun jvmDefault() {}
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("MyInterface")
            assertThat(subject.getMethodByJvmName("notJvmDefault").isJavaDefault()).isFalse()
            assertThat(subject.getMethodByJvmName("jvmDefault").isJavaDefault()).isTrue()
        }
    }

    @Test
    fun constructors_java() {
        val src =
            Source.java(
                "Source",
                """
            import java.util.List;
            interface MyInterface {}
            class NoExplicitConstructor{}
            class Base {
                Base(int x){}
            }
            class ExplicitConstructor {
                ExplicitConstructor(int x){}
            }
            class BaseWithSecondary {
                BaseWithSecondary(int x){}
                BaseWithSecondary(String y){}
            }
            class Sub extends Base {
                Sub(int x) {
                    super(x);
                }
            }
            class SubWith3Constructors extends BaseWithSecondary {
                SubWith3Constructors() {
                    super(3);
                }
                SubWith3Constructors(List<String> list) {
                    super(3);
                }
                SubWith3Constructors(List<String> list, int x) {
                    super(3);
                }
            }
            abstract class AbstractNoExplicit {}
            abstract class AbstractExplicit {
                AbstractExplicit(int x) {}
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val subjects =
                listOf(
                    "MyInterface",
                    "NoExplicitConstructor",
                    "Base",
                    "ExplicitConstructor",
                    "BaseWithSecondary",
                    "Sub",
                    "SubWith3Constructors",
                    "AbstractNoExplicit",
                    "AbstractExplicit"
                )
            val constructorCounts =
                subjects.map {
                    it to invocation.processingEnv.requireTypeElement(it).getConstructors().size
                }
            assertThat(constructorCounts)
                .containsExactly(
                    "MyInterface" to 0,
                    "NoExplicitConstructor" to 1,
                    "Base" to 1,
                    "ExplicitConstructor" to 1,
                    "BaseWithSecondary" to 2,
                    "Sub" to 1,
                    "SubWith3Constructors" to 3,
                    "AbstractNoExplicit" to 1,
                    "AbstractExplicit" to 1
                )

            subjects.forEach {
                assertWithMessage(it)
                    .that(invocation.processingEnv.requireTypeElement(it).findPrimaryConstructor())
                    .isNull()
            }
        }
    }

    @Test
    fun enumTypeElement() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "test/KotlinEnum.kt",
                        """
                    package test
                    enum class KotlinEnum(private val x:Int) {
                        VAL1(1),
                        VAL2(2);

                        fun enumMethod(): Unit {}
                    }
                    """
                            .trimIndent()
                    ),
                    Source.java(
                        "test.JavaEnum",
                        """
                    package test;
                    public enum JavaEnum {
                        VAL1(1),
                        VAL2(2);

                        private int x;

                        JavaEnum(int x) {
                            this.x = x;
                        }
                        void enumMethod() {}
                    }
                    """
                            .trimIndent()
                    )
                ),
        ) { invocation ->
            listOf(
                    "test.KotlinEnum",
                    "test.JavaEnum",
                )
                .forEach { qName ->
                    val typeElement = invocation.processingEnv.requireTypeElement(qName)
                    assertWithMessage("$qName is enum").that(typeElement.isEnum()).isTrue()
                    assertWithMessage("$qName does not report enum constants in methods")
                        .that(typeElement.getDeclaredMethods().map { it.jvmName })
                        .run {
                            contains("enumMethod")
                            containsNoneOf("VAL1", "VAL2")
                        }
                    assertWithMessage("$qName can return enum constants")
                        .that((typeElement as XEnumTypeElement).entries.map { it.name })
                        .containsExactly("VAL1", "VAL2")
                    assertWithMessage("$qName  does not report enum constants in fields")
                        .that(typeElement.getAllFieldNames())
                        .run {
                            contains("x")
                            containsNoneOf("VAL1", "VAL2")
                        }
                    assertWithMessage("$qName does not report enum constants in declared fields")
                        .that(typeElement.getDeclaredFields().map { it.name })
                        .containsExactly("x")
                    assertWithMessage("$qName enum entries are XEnumEntry")
                        .that(typeElement.getEnclosedElements().filter { it.isEnumEntry() })
                        .hasSize(2)
                    assertWithMessage("$qName  enum entries are not type elements")
                        .that(typeElement.getEnclosedElements().filter { it.isTypeElement() })
                        .isEmpty()
                    // TODO(kuanyingchou): https://github.com/google/ksp/issues/1761
                    val parent = typeElement.superClass!!.typeElement!!
                    if (qName == "test.KotlinEnum" && !isPreCompiled && invocation.isKsp) {
                        if (
                            invocation.isKsp &&
                                (invocation.processingEnv as KspProcessingEnv).isKsp2
                        ) {
                            assertThat(parent.asClassName()).isEqualTo(XTypeName.ENUM)
                        } else {
                            assertThat(parent.asClassName()).isEqualTo(Any::class.asClassName())
                        }
                    } else {
                        assertThat(parent.asClassName()).isEqualTo(Enum::class.asClassName())
                    }
                    // TODO(kuanyingchou): make this more consistent.
                    val methodNames = typeElement.getDeclaredMethods().map { it.jvmName }
                    if (qName == "test.KotlinEnum") {
                        if (invocation.isKsp) {
                            if (
                                isPreCompiled ||
                                    (invocation.processingEnv as KspProcessingEnv).isKsp2
                            ) {
                                assertThat(methodNames)
                                    .containsExactly(
                                        "enumMethod",
                                        "values",
                                        "valueOf",
                                    )
                            } else {
                                assertThat(methodNames)
                                    .containsExactly(
                                        "enumMethod",
                                    )
                            }
                        } else {
                            assertThat(methodNames)
                                .containsExactly(
                                    "values",
                                    "valueOf",
                                    "enumMethod",
                                    // `entries` became stable in Kotlin 1.9.0 but somehow only
                                    // appears
                                    // in KAPT. We can't find an `entries` property in KSP yet.
                                    "getEntries"
                                )
                        }
                    } else {
                        assertThat(methodNames)
                            .containsExactly(
                                "values",
                                "valueOf",
                                "enumMethod",
                            )
                    }
                }
        }
    }

    @Test
    fun enclosedTypes() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            class TopLevelClass {
                class NestedClass
                object NestedObject
                interface NestedInterface
                enum class NestedEnum {
                    A, B
                }
                companion object {
                    val foo = 1
                }
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val topLevelClass = invocation.processingEnv.requireTypeElement("TopLevelClass")
            val enclosedTypeElements = topLevelClass.getEnclosedTypeElements()

            assertThat(enclosedTypeElements)
                .containsExactly(
                    invocation.processingEnv.requireTypeElement("TopLevelClass.NestedClass"),
                    invocation.processingEnv.requireTypeElement("TopLevelClass.NestedObject"),
                    invocation.processingEnv.requireTypeElement("TopLevelClass.NestedInterface"),
                    invocation.processingEnv.requireTypeElement("TopLevelClass.NestedEnum"),
                    invocation.processingEnv.requireTypeElement("TopLevelClass.Companion"),
                )
        }
    }

    @Test
    fun enclosedTypes_java() {
        val src =
            Source.java(
                "Source",
                """
            class TopLevelClass {
                class InnerClass { }
                static class NestedClass { }
                interface NestedInterface { }
                enum NestedEnum {
                    A, B
                }
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val topLevelClass = invocation.processingEnv.requireTypeElement("TopLevelClass")
            val enclosedTypeElements = topLevelClass.getEnclosedTypeElements()

            assertThat(enclosedTypeElements)
                .containsExactly(
                    invocation.processingEnv.requireTypeElement("TopLevelClass.InnerClass"),
                    invocation.processingEnv.requireTypeElement("TopLevelClass.NestedClass"),
                    invocation.processingEnv.requireTypeElement("TopLevelClass.NestedInterface"),
                    invocation.processingEnv.requireTypeElement("TopLevelClass.NestedEnum"),
                )
        }
    }

    @Test
    fun kotlinObjects() {
        val kotlinSrc =
            Source.kotlin(
                "Test.kt",
                """
            package foo.bar
            class KotlinClass {
                companion object
                object NestedObject
            }
            """
                    .trimIndent()
            )
        runTest(listOf(kotlinSrc)) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("foo.bar.KotlinClass")
            val companionObjects =
                kotlinClass.getEnclosedTypeElements().filter { it.isCompanionObject() }
            assertThat(companionObjects.size).isEqualTo(1)
            val companionObj = companionObjects.first()
            assertThat(companionObj.isKotlinObject()).isTrue()
        }
    }

    @Test
    fun inheritedGenericMethod() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "test.ConcreteClass.kt",
                        """
                    package test
                    class ConcreteClass: AbstractClass<Foo, Bar>() {}
                    abstract class AbstractClass<T1, T2> {
                        fun method(t1: T1, t2: T2): T2 {
                          return t2
                        }
                    }
                    class Foo
                    class Bar
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            val concreteClass = invocation.processingEnv.requireTypeElement("test.ConcreteClass")
            val abstractClass = invocation.processingEnv.requireTypeElement("test.AbstractClass")
            val concreteClassMethod = concreteClass.getMethodByJvmName("method")
            val abstractClassMethod = abstractClass.getMethodByJvmName("method")
            val fooType = invocation.processingEnv.requireType("test.Foo")
            val barType = invocation.processingEnv.requireType("test.Bar")

            fun checkMethodElement(method: XMethodElement) {
                checkMethodElement(
                    method = method,
                    name = "method",
                    enclosingElement = abstractClass,
                    returnType = XTypeName.getTypeVariableName("T2"),
                    parameterTypes =
                        arrayOf(
                            XTypeName.getTypeVariableName("T1"),
                            XTypeName.getTypeVariableName("T2")
                        )
                )
                checkMethodType(
                    methodType = method.executableType,
                    returnType = XTypeName.getTypeVariableName("T2"),
                    parameterTypes =
                        arrayOf(
                            XTypeName.getTypeVariableName("T1"),
                            XTypeName.getTypeVariableName("T2")
                        )
                )
                checkMethodType(
                    methodType = method.asMemberOf(abstractClass.type),
                    returnType = XTypeName.getTypeVariableName("T2"),
                    parameterTypes =
                        arrayOf(
                            XTypeName.getTypeVariableName("T1"),
                            XTypeName.getTypeVariableName("T2")
                        )
                )
                checkMethodType(
                    methodType = method.asMemberOf(concreteClass.type),
                    returnType = barType.asTypeName(),
                    parameterTypes = arrayOf(fooType.asTypeName(), barType.asTypeName())
                )
            }

            assertThat(concreteClassMethod).isEqualTo(abstractClassMethod)
            checkMethodElement(concreteClassMethod)
            checkMethodElement(abstractClassMethod)
        }
    }

    @Test
    fun overriddenGenericMethod() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "test.ConcreteClass.kt",
                        """
                    package test
                    class ConcreteClass: AbstractClass<Foo, Bar>() {
                        override fun method(t1: Foo, t2: Bar): Bar {
                          return t2
                        }
                    }
                    abstract class AbstractClass<T1, T2> {
                        abstract fun method(t1: T1, t2: T2): T2
                    }
                    class Foo
                    class Bar
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            val concreteClass = invocation.processingEnv.requireTypeElement("test.ConcreteClass")
            val abstractClass = invocation.processingEnv.requireTypeElement("test.AbstractClass")
            val concreteClassMethod = concreteClass.getMethodByJvmName("method")
            val abstractClassMethod = abstractClass.getMethodByJvmName("method")
            val fooType = invocation.processingEnv.requireType("test.Foo")
            val barType = invocation.processingEnv.requireType("test.Bar")

            assertThat(concreteClassMethod).isNotEqualTo(abstractClassMethod)
            assertThat(concreteClassMethod.overrides(abstractClassMethod, concreteClass)).isTrue()

            // Check the abstract method and method type
            checkMethodElement(
                method = abstractClassMethod,
                name = "method",
                enclosingElement = abstractClass,
                returnType = XTypeName.getTypeVariableName("T2"),
                parameterTypes =
                    arrayOf(
                        XTypeName.getTypeVariableName("T1"),
                        XTypeName.getTypeVariableName("T2")
                    )
            )
            checkMethodType(
                methodType = abstractClassMethod.executableType,
                returnType = XTypeName.getTypeVariableName("T2"),
                parameterTypes =
                    arrayOf(
                        XTypeName.getTypeVariableName("T1"),
                        XTypeName.getTypeVariableName("T2")
                    )
            )
            checkMethodType(
                methodType = abstractClassMethod.asMemberOf(abstractClass.type),
                returnType = XTypeName.getTypeVariableName("T2"),
                parameterTypes =
                    arrayOf(
                        XTypeName.getTypeVariableName("T1"),
                        XTypeName.getTypeVariableName("T2"),
                    )
            )
            checkMethodType(
                methodType = abstractClassMethod.asMemberOf(concreteClass.type),
                returnType = barType.asTypeName(),
                parameterTypes = arrayOf(fooType.asTypeName(), barType.asTypeName())
            )

            // Check the concrete method and method type
            checkMethodElement(
                method = concreteClassMethod,
                name = "method",
                enclosingElement = concreteClass,
                returnType = barType.asTypeName(),
                parameterTypes = arrayOf(fooType.asTypeName(), barType.asTypeName())
            )
            checkMethodType(
                methodType = concreteClassMethod.executableType,
                returnType = barType.asTypeName(),
                parameterTypes = arrayOf(fooType.asTypeName(), barType.asTypeName())
            )
            checkMethodType(
                methodType = concreteClassMethod.asMemberOf(concreteClass.type),
                returnType = barType.asTypeName(),
                parameterTypes = arrayOf(fooType.asTypeName(), barType.asTypeName())
            )
        }
    }

    private fun checkMethodElement(
        method: XMethodElement,
        name: String,
        enclosingElement: XTypeElement,
        returnType: XTypeName,
        parameterTypes: Array<XTypeName>
    ) {
        assertThat(method.name).isEqualTo(name)
        assertThat(method.enclosingElement).isEqualTo(enclosingElement)
        assertThat(method.returnType.asTypeName()).isEqualTo(returnType)
        assertThat(method.parameters.map { it.type.asTypeName() })
            .containsExactly(*parameterTypes)
            .inOrder()
    }

    private fun checkMethodType(
        methodType: XMethodType,
        returnType: XTypeName,
        parameterTypes: Array<XTypeName>
    ) {
        assertThat(methodType.returnType.asTypeName()).isEqualTo(returnType)
        assertThat(methodType.parameterTypes.map { it.asTypeName() })
            .containsExactly(*parameterTypes)
            .inOrder()
    }

    @Test
    fun overriddenGenericConstructor() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "test.ConcreteClass.kt",
                        """
                    package test
                    class ConcreteClass(foo: Foo): AbstractClass<Foo>(foo) {}
                    abstract class AbstractClass<T>(t: T)
                    class Foo
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            val concreteClass = invocation.processingEnv.requireTypeElement("test.ConcreteClass")
            val abstractClass = invocation.processingEnv.requireTypeElement("test.AbstractClass")
            val fooType = invocation.processingEnv.requireType("test.Foo")

            fun checkConstructorParameters(
                typeElement: XTypeElement,
                expectedParameters: Array<XTypeName>
            ) {
                assertThat(typeElement.getConstructors()).hasSize(1)
                val constructor = typeElement.getConstructors()[0]
                assertThat(constructor.parameters.map { it.type.asTypeName() })
                    .containsExactly(*expectedParameters)
                    .inOrder()
            }

            checkConstructorParameters(
                typeElement = abstractClass,
                expectedParameters = arrayOf(XTypeName.getTypeVariableName("T"))
            )
            checkConstructorParameters(
                typeElement = concreteClass,
                expectedParameters = arrayOf(fooType.asTypeName())
            )
        }
    }

    @Test
    fun inheritedGenericField() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "test.ConcreteClass.kt",
                        """
                    package test
                    class ConcreteClass: AbstractClass<Foo>()
                    abstract class AbstractClass<T> {
                        val field: T = TODO()
                    }
                    class Foo
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            val concreteClass = invocation.processingEnv.requireTypeElement("test.ConcreteClass")
            val abstractClass = invocation.processingEnv.requireTypeElement("test.AbstractClass")
            val concreteClassField = concreteClass.getField("field")
            val abstractClassField = abstractClass.getField("field")
            val fooType = invocation.processingEnv.requireType("test.Foo")

            fun checkFieldElement(field: XFieldElement) {
                assertThat(field.name).isEqualTo("field")
                assertThat(field.type.asTypeName().java).isEqualTo(JTypeVariableName.get("T"))
                assertThat(field.asMemberOf(abstractClass.type).asTypeName().java)
                    .isEqualTo(JTypeVariableName.get("T"))
                assertThat(field.asMemberOf(concreteClass.type).asTypeName().java)
                    .isEqualTo(fooType.asTypeName().java)
                if (invocation.isKsp) {
                    assertThat(field.type.asTypeName().kotlin).isEqualTo(KTypeVariableName("T"))
                    assertThat(field.asMemberOf(abstractClass.type).asTypeName().kotlin)
                        .isEqualTo(KTypeVariableName("T"))
                    assertThat(field.asMemberOf(concreteClass.type).asTypeName().kotlin)
                        .isEqualTo(fooType.asTypeName().kotlin)
                }
            }

            assertThat(concreteClassField).isEqualTo(abstractClassField)
            checkFieldElement(abstractClassField)
            checkFieldElement(concreteClassField)
        }
    }

    @Test
    fun internalModifier() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "test.Foo.kt",
                        """
                    package test
                    internal class InternalClass internal constructor() {
                      internal val valField: String = TODO()
                      internal var varField: String = TODO()
                      internal fun method(): String = TODO()
                      internal lateinit var lateinitVarField: String
                    }
                    class PublicClass constructor() {
                      val valField: String = TODO()
                      var varField: String = TODO()
                      fun method(): String = TODO()
                      lateinit var lateinitVarField: String
                    }
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            // Matches by name rather than jvmName to avoid dealing with name mangling.
            fun XTypeElement.getDeclaredMethod(name: String): XMethodElement {
                return getDeclaredMethods().filter { it.name == name }.single()
            }

            val internalClass = invocation.processingEnv.requireTypeElement("test.InternalClass")
            assertThat(internalClass.isInternal()).isTrue()
            assertThat(internalClass.getConstructors().single().isInternal()).isTrue()
            assertThat(internalClass.getDeclaredField("valField").isInternal()).isTrue()
            assertThat(internalClass.getDeclaredField("varField").isInternal()).isTrue()
            assertThat(internalClass.getDeclaredField("lateinitVarField").isInternal()).isTrue()
            assertThat(internalClass.getDeclaredMethod("method").isInternal()).isTrue()
            assertThat(internalClass.getDeclaredMethod("getValField").isInternal()).isTrue()
            assertThat(internalClass.getDeclaredMethod("getVarField").isInternal()).isTrue()
            assertThat(internalClass.getDeclaredMethod("getLateinitVarField").isInternal()).isTrue()
            assertThat(internalClass.getDeclaredMethod("setVarField").isInternal()).isTrue()
            assertThat(internalClass.getDeclaredMethod("setLateinitVarField").isInternal()).isTrue()

            val publicClass = invocation.processingEnv.requireTypeElement("test.PublicClass")
            assertThat(publicClass.isInternal()).isFalse()
            assertThat(publicClass.getConstructors().single().isInternal()).isFalse()
            assertThat(publicClass.getDeclaredField("valField").isInternal()).isFalse()
            assertThat(publicClass.getDeclaredField("varField").isInternal()).isFalse()
            assertThat(publicClass.getDeclaredField("lateinitVarField").isInternal()).isFalse()
            assertThat(publicClass.getDeclaredMethod("method").isInternal()).isFalse()
            assertThat(publicClass.getDeclaredMethod("getValField").isInternal()).isFalse()
            assertThat(publicClass.getDeclaredMethod("getVarField").isInternal()).isFalse()
            assertThat(publicClass.getDeclaredMethod("getLateinitVarField").isInternal()).isFalse()
            assertThat(publicClass.getDeclaredMethod("setVarField").isInternal()).isFalse()
            assertThat(publicClass.getDeclaredMethod("setLateinitVarField").isInternal()).isFalse()
        }
    }

    @Test
    fun sameMethodNameOrder() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "test.Foo.kt",
                        """
                    package test
                    class Foo<T1: Bar, T2: Baz> {
                        fun method(): String = TODO()
                        fun method(param: String): String = TODO()
                        fun method(param: Any): String = TODO()
                        fun method(param: T1): T2 = TODO()
                        fun method(param: T2): T1 = TODO()
                        fun <U1: Baz, U2> method(param1: U1, param2: U2) {}
                        fun <U1: Baz, U2: U1> method(param1: U1, param2: U2): T1 = TODO()
                    }
                    interface Bar
                    interface Baz
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("test.Foo")
            assertThat(foo.getDeclaredMethods().map { it.jvmDescriptor }.toList())
                .containsExactly(
                    "method()Ljava/lang/String;",
                    "method(Ljava/lang/String;)Ljava/lang/String;",
                    "method(Ljava/lang/Object;)Ljava/lang/String;",
                    "method(Ltest/Bar;)Ltest/Baz;",
                    "method(Ltest/Baz;)Ltest/Bar;",
                    "method(Ltest/Baz;Ljava/lang/Object;)V",
                    "method(Ltest/Baz;Ltest/Baz;)Ltest/Bar;"
                )
                .inOrder()
        }
    }

    @Test
    fun jvmDescriptors() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "test.Foo.kt",
                        """
                    package test
                    class Foo<T1: Bar, T2: Baz> {
                        val field1: String = TODO()
                        var field2: String? = TODO()
                        val field3: T1 = TODO()
                        fun method(): String = TODO()
                        fun method(param: String): String = TODO()
                        fun method(param: Any): String = TODO()
                        fun method(param: T1): T2 = TODO()
                    }
                    interface Bar
                    interface Baz
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("test.Foo")
            assertThat(
                    foo.getEnclosedElements()
                        .map {
                            when {
                                it.isField() -> it.jvmDescriptor
                                it.isMethod() -> it.jvmDescriptor
                                it.isConstructor() -> it.jvmDescriptor
                                else -> error("Unsupported element to describe.")
                            }
                        }
                        .toList()
                )
                .containsExactly(
                    "<init>()V",
                    "field1:Ljava/lang/String;",
                    "field2:Ljava/lang/String;",
                    "field3:Ltest/Bar;",
                    "getField1()Ljava/lang/String;",
                    "getField2()Ljava/lang/String;",
                    "setField2(Ljava/lang/String;)V",
                    "getField3()Ltest/Bar;",
                    "method()Ljava/lang/String;",
                    "method(Ljava/lang/String;)Ljava/lang/String;",
                    "method(Ljava/lang/Object;)Ljava/lang/String;",
                    "method(Ltest/Bar;)Ltest/Baz;",
                )
        }
    }

    @Test
    fun testClassNameWithDollarSign() {
        runTest(
            sources =
                listOf(
                    Source.java(
                        "test.MyClass\$Foo",
                        """
                    package test;
                    class MyClass${'$'}Foo {}
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("test.MyClass\$Foo")
            assertThat(subject.asClassName().canonicalName).isEqualTo("test.MyClass\$Foo")
        }
    }

    @Test
    fun propertyAccessors() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "Subject.kt",
                        """
                    class Subject {
                        val val1: String = TODO()
                        @get:JvmName("getVal2JvmName")
                        val val2: String = TODO()
                        var var1: String = TODO()
                        @get:JvmName("getVar2JvmName")
                        var var2: String = TODO()
                        @set:JvmName("setVar3JvmName")
                        var var3: String = TODO()
                        @get:JvmName("getVar4JvmName")
                        @set:JvmName("setVar4JvmName")
                        var var4: String = TODO()
                        @set:JvmName("setVar5JvmName")
                        @get:JvmName("getVar5JvmName")
                        var var5: String = TODO()
                    }
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")

            // Check method names
            assertThat(subject.getDeclaredMethods().map { it.name })
                .containsExactly(
                    "getVal1", // val1 accessors
                    "getVal2", // val2 accessors
                    "getVar1",
                    "setVar1", // var1 accessors
                    "getVar2",
                    "setVar2", // var2 accessors
                    "getVar3",
                    "setVar3", // var3 accessors
                    "getVar4",
                    "setVar4", // var4 accessors
                    "getVar5",
                    "setVar5", // var5 accessors
                )
                .inOrder()

            // Check property names and corresponding accessors
            assertThat(
                    subject.getDeclaredFields().map {
                        it.name to listOf(it.getter?.name, it.setter?.name)
                    }
                )
                .containsExactly(
                    "val1" to listOf("getVal1", null),
                    "val2" to listOf("getVal2", null),
                    "var1" to listOf("getVar1", "setVar1"),
                    "var2" to listOf("getVar2", "setVar2"),
                    "var3" to listOf("getVar3", "setVar3"),
                    "var4" to listOf("getVar4", "setVar4"),
                    "var5" to listOf("getVar5", "setVar5"),
                )
        }
    }

    @Test
    fun isRecordClass() {
        val javaSrc =
            Source.java(
                "JavaRecord",
                """
            public record JavaRecord(String name) { }
            """
                    .trimIndent()
            )
        val kotlinSrc =
            Source.kotlin(
                "KotlinRecord.kt",
                """
            @JvmRecord
            data class KotlinRecord(val name: String)
            """
                    .trimIndent()
            )
        val handler: (XTestInvocation) -> Unit = {
            val javaElement = it.processingEnv.requireTypeElement("JavaRecord")
            assertThat(javaElement.isRecordClass())
            if (it.isKsp) {
                val kotlinElement = it.processingEnv.requireTypeElement("KotlinRecord")
                assertThat(kotlinElement.isRecordClass())
            }
        }
        // Run only javac and KSP tests as @JvmRecord is broken with KAPT (KT-44706)
        if (isPreCompiled) {
            val compiled =
                compileFiles(
                    sources = listOf(javaSrc, kotlinSrc),
                    kotlincArguments = listOf("-jvm-target=16")
                )
            runJavaProcessorTest(
                sources = listOf(Source.java("PlaceholderJava", "public class PlaceholderJava {}")),
                classpath = compiled,
                handler = handler
            )
            runKspTest(
                sources = listOf(Source.kotlin("placeholder.kt", "class PlaceholderKotlin")),
                kotlincArguments = listOf("-jvm-target=16"),
                classpath = compiled,
                handler = handler
            )
        } else {
            runJavaProcessorTest(sources = listOf(javaSrc), handler = handler)
            runKspTest(
                sources = listOf(javaSrc, kotlinSrc),
                kotlincArguments = listOf("-jvm-target=16"),
                handler = handler
            )
        }
    }

    /**
     * it is good to exclude methods coming from Object when testing as they differ between KSP and
     * KAPT but irrelevant for Room.
     */
    private fun XTestInvocation.objectMethodNames() =
        processingEnv.requireTypeElement("java.lang.Object").getAllMethods().jvmNames()

    private fun Sequence<XMethodElement>.jvmNames() = map { it.jvmName }.toList()

    private fun List<XMethodElement>.jvmNames() = map { it.jvmName }.toList()

    private fun XTypeElement.getConstructorSignatures(): List<String> =
        getConstructors().map { it.signature() }

    private fun XConstructorElement.signature(): String {
        val params =
            executableType.parameterTypes.joinToString(",") { it.asTypeName().java.toString() }
        val enclosingName = enclosingElement.name
        return "$enclosingName($params)"
    }

    private fun XMethodElement.signature(owner: XType): String {
        val methodType = this.asMemberOf(owner)
        val params = methodType.parameterTypes.joinToString(",") { it.asTypeName().java.toString() }
        return "$jvmName($params):${returnType.asTypeName().java}"
    }

    private fun XTestInvocation.nonObjectMethodSignatures(typeElement: XTypeElement): List<String> =
        typeElement
            .getAllMethods()
            .filterNot { it.jvmName in objectMethodNames() }
            .map { it.signature(typeElement.type) }
            .toList()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "isPreCompiled_{0}")
        fun params(): List<Array<Any>> {
            return listOf(arrayOf(false), arrayOf(true))
        }
    }
}
