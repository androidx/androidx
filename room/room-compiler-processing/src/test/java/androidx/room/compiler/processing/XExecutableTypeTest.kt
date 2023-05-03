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

import androidx.room.compiler.processing.util.CONTINUATION_JCLASS_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.UNIT_JCLASS_NAME
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName
import org.junit.Test

class XExecutableTypeTest {
    @Test
    fun constructorInheritanceResolution() {
        runProcessorTest(
            sources = listOf(
                Source.kotlin(
                    "KotlinClass.kt",
                    """
                    abstract class KotlinClass<T> constructor(t: T) {
                        abstract fun foo(): KotlinClass<String>
                    }
                    """.trimIndent()
                ),
                Source.java(
                    "JavaClass",
                    """
                    abstract class JavaClass<T> {
                        JavaClass(T t) {}
                        abstract JavaClass<String> foo();
                    }
                    """.trimIndent()
                )
            )
        ) { invocation ->
            fun checkConstructor(className: String) {
                val typeElement = invocation.processingEnv.requireTypeElement(className)
                val constructorElement = typeElement.getConstructors().single()
                val constructorType = constructorElement.executableType

                assertThat(constructorType.parameterTypes.single().typeName)
                    .isEqualTo(TypeVariableName.get("T"))
                val resolvedType = typeElement.getDeclaredMethods().single().returnType
                val resolvedConstructorType = constructorElement.asMemberOf(resolvedType)

                // Assert that the XConstructorType parameter is resolved type, String
                assertThat(resolvedConstructorType.parameterTypes.single().typeName)
                    .isEqualTo(ClassName.get("java.lang", "String"))
            }
            checkConstructor("JavaClass")
            checkConstructor("KotlinClass")
        }
    }

    @Test
    fun inheritanceResolution() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            interface MyInterface<T> {
                fun getT(): T
                fun setT(t:T): Unit
                suspend fun suspendGetT(): T
                suspend fun suspendSetT(t:T): Unit
            }
            abstract class Subject : MyInterface<String>
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val myInterface = invocation.processingEnv.requireTypeElement("MyInterface")

            // helper method to get executable types both from sub class and also as direct child of
            // the given type
            fun checkMethods(
                methodName: String,
                vararg subjects: XTypeElement,
                callback: (XMethodType) -> Unit
            ) {
                assertThat(subjects).isNotEmpty()
                subjects.forEach {
                    callback(myInterface.getMethodByJvmName(methodName).asMemberOf(it.type))
                    callback(it.getMethodByJvmName(methodName).asMemberOf(it.type))
                }
            }

            val subject = invocation.processingEnv.requireTypeElement("Subject")
            checkMethods("getT", subject) { type ->
                assertThat(type.parameterTypes).isEmpty()
                assertThat(type.returnType.typeName).isEqualTo(String::class.typeName())
            }
            checkMethods("setT", subject) { type ->
                assertThat(type.parameterTypes).containsExactly(
                    invocation.processingEnv.requireType(String::class)
                )
                assertThat(type.returnType.typeName).isEqualTo(TypeName.VOID)
            }
            checkMethods("suspendGetT", subject) { type ->
                assertThat(type.parameterTypes.first().typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        CONTINUATION_JCLASS_NAME,
                        WildcardTypeName.supertypeOf(String::class.java)
                    )
                )
                assertThat(type.returnType.typeName).isEqualTo(TypeName.OBJECT)
            }
            checkMethods("suspendSetT", subject) { type ->
                assertThat(type.parameterTypes.first().typeName).isEqualTo(
                    String::class.typeName()
                )
                assertThat(type.parameterTypes[1].typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        CONTINUATION_JCLASS_NAME,
                        WildcardTypeName.supertypeOf(UNIT_JCLASS_NAME)
                    )
                )
                assertThat(type.returnType.typeName).isEqualTo(TypeName.OBJECT)
            }
        }
    }

    @Test
    fun isSameMethodTypeTest() {
        val src = Source.kotlin(
            "MyInterface.kt",
            """
            interface MyInterface {
              fun method(foo: Foo): Bar
              fun methodWithDifferentName(foo: Foo): Bar
              @kotlin.jvm.Throws(RuntimeException::class)
              fun methodWithDifferentThrows(foo: Foo): Bar
              fun methodWithDifferentReturn(foo: Foo): Unit
              fun methodWithDifferentParameters(): Bar
              fun methodWithDefault(foo: Foo): Bar = TODO()
            }
            class MyClass {
              fun classMethod(foo: Foo): Bar = TODO()
              companion object {
                fun companionMethod(foo: Foo): Bar = TODO()
              }
            }
            object MyObject {
              fun objectMethod(foo: Foo): Bar = TODO()
              @JvmStatic fun staticMethod(foo: Foo): Bar = TODO()
            }
            class Foo
            class Bar
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val myInterface = invocation.processingEnv.requireTypeElement("MyInterface")
            val method = myInterface.getMethodByJvmName("method")
            val methodWithDifferentName =
                myInterface.getMethodByJvmName("methodWithDifferentName")
            val methodWithDifferentThrows =
                myInterface.getMethodByJvmName("methodWithDifferentThrows")
            val methodWithDifferentReturn =
                myInterface.getMethodByJvmName("methodWithDifferentReturn")
            val methodWithDifferentParameters =
                myInterface.getMethodByJvmName("methodWithDifferentParameters")
            val methodWithDefault = myInterface.getMethodByJvmName("methodWithDefault")
            val myObject = invocation.processingEnv.requireTypeElement("MyObject")
            val objectMethod = myObject.getMethodByJvmName("objectMethod")
            val staticMethod = myObject.getMethodByJvmName("staticMethod")
            val myClass = invocation.processingEnv.requireTypeElement("MyClass")
            val classMethod = myClass.getMethodByJvmName("classMethod")
            val companionObject = myClass.getEnclosedElements()
                .mapNotNull { it as? XTypeElement }
                .filter { it.isCompanionObject() }
                .single()
            val companionMethod = companionObject.getMethodByJvmName("companionMethod")

            assertIsSameType(method, methodWithDifferentName)
            assertIsSameType(method, methodWithDifferentThrows)
            assertIsSameType(method, methodWithDefault)
            assertIsSameType(method, objectMethod)
            assertIsSameType(method, staticMethod)
            assertIsSameType(method, objectMethod)
            assertIsSameType(method, classMethod)
            assertIsSameType(method, companionMethod)

            // Assert that different return type or parameters result in different method types.
            assertIsNotSameType(method, methodWithDifferentReturn)
            assertIsNotSameType(method, methodWithDifferentParameters)
        }
    }

    @Test
    fun isSameGenericMethodTypeTest() {
        val src = Source.kotlin(
            "MyInterface.kt",
            """
            interface FooBar : MyInterface<Foo, Bar>
            interface BarFoo : MyInterface<Bar, Foo>
            interface MyInterface<T1, T2> {
              fun methodFooBar(foo: Foo): Bar
              fun methodBarFoo(bar: Bar): Foo
              fun method(t1: T1): T2
            }
            class Foo
            class Bar
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val myInterface = invocation.processingEnv.requireTypeElement("MyInterface")
            val fooBar = invocation.processingEnv.requireTypeElement("FooBar")
            val barFoo = invocation.processingEnv.requireTypeElement("BarFoo")
            val myInterfaceMethod = myInterface.getMethodByJvmName("method")
            val myInterfaceMethodFooBar = myInterface.getMethodByJvmName("methodFooBar")
            val myInterfaceMethodBarFoo = myInterface.getMethodByJvmName("methodBarFoo")

            assertIsNotSameType(myInterfaceMethodFooBar, myInterfaceMethod)
            assertIsSameType(
                myInterfaceMethodFooBar.executableType,
                myInterfaceMethod.asMemberOf(fooBar.type)
            )
            assertIsNotSameType(myInterfaceMethodBarFoo, myInterfaceMethod)
            assertIsSameType(
                myInterfaceMethodBarFoo.executableType,
                myInterfaceMethod.asMemberOf(barFoo.type)
            )
        }
    }

    @Test
    fun isSameConstructorTypeTest() {
        val src = Source.kotlin(
            "MyInterface.kt",
            """
            abstract class ClassFoo constructor(foo: Foo)
            abstract class ClassBar constructor(bar: Bar)
            abstract class OtherClassFoo constructor(foo: Foo)
            abstract class SubClassBar constructor(bar: Bar) : ClassBar(bar)
            class Foo
            class Bar
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val classFoo = invocation.processingEnv.requireTypeElement("ClassFoo")
            val classBar = invocation.processingEnv.requireTypeElement("ClassBar")
            val otherClassFoo = invocation.processingEnv.requireTypeElement("OtherClassFoo")
            val subClassBar = invocation.processingEnv.requireTypeElement("SubClassBar")

            assertThat(classFoo.getConstructors()).hasSize(1)
            assertThat(classBar.getConstructors()).hasSize(1)
            assertThat(otherClassFoo.getConstructors()).hasSize(1)
            assertThat(subClassBar.getConstructors()).hasSize(1)
            assertIsSameType(
                classFoo.getConstructors().single(),
                otherClassFoo.getConstructors().single()
            )
            assertIsSameType(
                classBar.getConstructors().single(),
                subClassBar.getConstructors().single()
            )
            assertIsNotSameType(
                classFoo.getConstructors().single(),
                classBar.getConstructors().single()
            )
        }
    }

    @Test
    fun isSameConstructorTypeAndMethodTypeTest() {
        val src = Source.kotlin(
            "ClassFoo.kt",
            """
            abstract class ClassFoo constructor(foo: Foo) {
              abstract fun method(otherFoo: Foo)
            }
            class Foo
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val classFoo = invocation.processingEnv.requireTypeElement("ClassFoo")

            assertThat(classFoo.getConstructors()).hasSize(1)
            // Interestingly, isSameType doesn't care if the type is a constructor or method as long
            // as the parameter types and return type are the same (Note: constructors have return
            // types of "void" in Javac).
            assertIsSameType(
                classFoo.getConstructors().single(),
                classFoo.getMethodByJvmName("method")
            )
        }
    }

    @Test
    fun isSameGenericConstructorTypeTest() {
        val src = Source.kotlin(
            "MyInterface.kt",
            """
            abstract class GenericClass<T> constructor(t: T)
            abstract class GenericClassFoo constructor(foo: Foo) : GenericClass<Foo>(foo)
            abstract class ClassFoo constructor(foo: Foo)
            class Foo
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val genericClass = invocation.processingEnv.requireTypeElement("GenericClass")
            val genericClassFoo = invocation.processingEnv.requireTypeElement("GenericClassFoo")
            val classFoo = invocation.processingEnv.requireTypeElement("ClassFoo")

            assertThat(genericClass.getConstructors()).hasSize(1)
            assertThat(genericClassFoo.getConstructors()).hasSize(1)
            assertThat(classFoo.getConstructors()).hasSize(1)
            assertIsNotSameType(
                genericClass.getConstructors().single(),
                genericClassFoo.getConstructors().single()
            )
            assertIsNotSameType(
                genericClass.getConstructors().single(),
                classFoo.getConstructors().single()
            )
            assertIsSameType(
                genericClassFoo.getConstructors().single(),
                classFoo.getConstructors().single()
            )
        }
    }

    @Test
    fun isSamePropertyMethodTypeTest() {
        val src = Source.kotlin(
            "MyInterface.kt",
            """
            class MyClass {
                var fooField: Foo = TODO()
                var fooFieldWithDifferentName: Foo = TODO()
                val fooFieldWithVal: Foo = TODO()
                var barField: Bar = TODO()

                fun fooMethodGetter(): Foo = TODO()
                fun fooMethodSetter(foo: Foo) {}
            }
            class Foo
            class Bar
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val myClass = invocation.processingEnv.requireTypeElement("MyClass")
            val fooFieldGetter = myClass.getMethodByJvmName("getFooField")
            val fooFieldSetter = myClass.getMethodByJvmName("setFooField")
            val fooFieldWithDifferentNameGetter =
                myClass.getMethodByJvmName("getFooFieldWithDifferentName")
            val fooFieldWithDifferentNameSetter =
                myClass.getMethodByJvmName("setFooFieldWithDifferentName")
            val fooFieldWithValGetter = myClass.getMethodByJvmName("getFooFieldWithVal")
            val barFieldGetter = myClass.getMethodByJvmName("getBarField")
            val barFieldSetter = myClass.getMethodByJvmName("setBarField")
            val fooMethodGetter = myClass.getMethodByJvmName("fooMethodGetter")
            val fooMethodSetter = myClass.getMethodByJvmName("fooMethodSetter")

            assertIsSameType(fooFieldGetter, fooFieldWithDifferentNameGetter)
            assertIsSameType(fooFieldGetter, fooFieldWithValGetter)
            assertIsSameType(fooMethodGetter, fooFieldGetter)
            assertIsSameType(fooFieldSetter, fooFieldWithDifferentNameSetter)
            assertIsSameType(fooFieldSetter, fooMethodSetter)
            assertIsNotSameType(fooFieldGetter, barFieldGetter)
            assertIsNotSameType(fooFieldSetter, barFieldSetter)
        }
    }

    private fun assertIsSameType(element1: XExecutableElement, element2: XExecutableElement) {
        assertIsSameType(element1.executableType, element2.executableType)
    }

    private fun assertIsSameType(type1: XExecutableType, type2: XExecutableType) {
        // Assert both directions to ensure isSameType is symmetric
        assertThat(type1.isSameType(type2)).isTrue()
        assertThat(type2.isSameType(type1)).isTrue()
    }

    private fun assertIsNotSameType(element1: XExecutableElement, element2: XExecutableElement) {
        assertIsNotSameType(element1.executableType, element2.executableType)
    }

    private fun assertIsNotSameType(type1: XExecutableType, type2: XExecutableType) {
        // Assert both directions to ensure isSameType is symmetric
        assertThat(type1.isSameType(type2)).isFalse()
        assertThat(type2.isSameType(type1)).isFalse()
    }

    @Test
    fun kotlinPropertyInheritance() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            interface MyInterface<T> {
                val immutableT: T
                var mutableT: T?
                val list: List<T>
                val nullableList: List<T?>
            }
            abstract class Subject : MyInterface<String>
            abstract class NullableSubject: MyInterface<String?>
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val myInterface = invocation.processingEnv.requireTypeElement("MyInterface")

            // helper method to get executable types both from sub class and also as direct child of
            // the given type
            fun checkMethods(
                methodName: String,
                vararg subjects: XTypeElement,
                callback: (XMethodType) -> Unit
            ) {
                assertThat(subjects).isNotEmpty()
                subjects.forEach {
                    callback(myInterface.getMethodByJvmName(methodName).asMemberOf(it.type))
                    callback(it.getMethodByJvmName(methodName).asMemberOf(it.type))
                }
            }

            val subject = invocation.processingEnv.requireTypeElement("Subject")
            checkMethods("getImmutableT", subject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(String::class.typeName())
                if (invocation.isKsp) {
                    // we don't get proper nullable here for kapt
                    // partially related to b/169629272
                    assertThat(method.returnType.nullability).isEqualTo(XNullability.NONNULL)
                }

                assertThat(method.parameterTypes).isEmpty()
                assertThat(method.typeVariableNames).isEmpty()
            }
            checkMethods("getMutableT", subject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(String::class.typeName())
                if (invocation.isKsp) {
                    // we don't get proper nullable here for kapt
                    // partially related to b/169629272
                    assertThat(method.returnType.nullability).isEqualTo(XNullability.NULLABLE)
                }
                assertThat(method.parameterTypes).isEmpty()
                assertThat(method.typeVariableNames).isEmpty()
            }
            checkMethods("setMutableT", subject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(TypeName.VOID)
                assertThat(method.parameterTypes.first().nullability)
                    .isEqualTo(XNullability.NULLABLE)
                assertThat(method.parameterTypes.first().typeName)
                    .isEqualTo(String::class.typeName())
                assertThat(method.typeVariableNames).isEmpty()
            }
            checkMethods("getList", subject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.java,
                        String::class.java
                    )
                )
                assertThat(method.returnType.nullability).isEqualTo(
                    XNullability.NONNULL
                )
                assertThat(method.returnType.typeArguments.first().extendsBound()).isNull()
                if (invocation.isKsp) {
                    // kapt cannot read type parameter nullability yet
                    assertThat(
                        method.returnType.typeArguments.first().nullability
                    ).isEqualTo(
                        XNullability.NONNULL
                    )
                }
            }

            val nullableSubject = invocation.processingEnv.requireTypeElement("NullableSubject")
            // check that nullability is inferred from type parameters as well
            checkMethods("getImmutableT", nullableSubject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(String::class.typeName())
                if (invocation.isKsp) {
                    // we don't get proper nullable here for kapt
                    // partially related to b/169629272
                    assertThat(method.returnType.nullability).isEqualTo(XNullability.NULLABLE)
                }
                assertThat(method.parameterTypes).isEmpty()
                assertThat(method.typeVariableNames).isEmpty()
            }

            checkMethods("getMutableT", nullableSubject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(String::class.typeName())
                if (invocation.isKsp) {
                    // we don't get proper nullable here for kapt
                    // partially related to b/169629272
                    assertThat(method.returnType.nullability).isEqualTo(XNullability.NULLABLE)
                }
                assertThat(method.parameterTypes).isEmpty()
                assertThat(method.typeVariableNames).isEmpty()
            }

            checkMethods("setMutableT", nullableSubject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(TypeName.VOID)
                assertThat(method.parameterTypes.first().nullability)
                    .isEqualTo(XNullability.NULLABLE)
                assertThat(method.parameterTypes.first().typeName)
                    .isEqualTo(String::class.typeName())
                assertThat(method.typeVariableNames).isEmpty()
            }

            checkMethods("getList", nullableSubject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.java,
                        String::class.java
                    )
                )
                assertThat(method.returnType.nullability).isEqualTo(
                    XNullability.NONNULL
                )
                if (invocation.isKsp) {
                    assertThat(
                        method.returnType.typeArguments.first().nullability
                    ).isEqualTo(
                        XNullability.NULLABLE
                    )
                }
            }
            checkMethods("getNullableList", subject, nullableSubject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.java,
                        String::class.java
                    )
                )
                assertThat(method.returnType.nullability).isEqualTo(
                    XNullability.NONNULL
                )
                if (invocation.isKsp) {
                    assertThat(
                        method.returnType.typeArguments.first().nullability
                    ).isEqualTo(
                        XNullability.NULLABLE
                    )
                }
            }
        }
    }
}
