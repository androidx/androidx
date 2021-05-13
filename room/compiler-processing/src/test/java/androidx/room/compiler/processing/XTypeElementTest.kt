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

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getAllFieldNames
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XTypeElementTest {
    @Test
    fun qualifiedNames() {
        val src1 = Source.kotlin(
            "Foo.kt",
            """
            class TopLevel
            """.trimIndent()
        )
        val src2 = Source.kotlin(
            "Bar.kt",
            """
            package foo.bar
            class InFooBar
            """.trimIndent()
        )
        val src3 = Source.java(
            "foo.bar.Outer",
            """
            package foo.bar;
            public class Outer {
                public static class Nested {
                }
            }
            """
        )
        runProcessorTest(
            sources = listOf(src1, src2, src3)
        ) { invocation ->
            invocation.processingEnv.requireTypeElement("TopLevel").let {
                assertThat(it.packageName).isEqualTo("")
                assertThat(it.name).isEqualTo("TopLevel")
                assertThat(it.qualifiedName).isEqualTo("TopLevel")
                assertThat(it.className).isEqualTo(ClassName.get("", "TopLevel"))
            }
            invocation.processingEnv.requireTypeElement("foo.bar.InFooBar").let {
                assertThat(it.packageName).isEqualTo("foo.bar")
                assertThat(it.name).isEqualTo("InFooBar")
                assertThat(it.qualifiedName).isEqualTo("foo.bar.InFooBar")
                assertThat(it.className).isEqualTo(ClassName.get("foo.bar", "InFooBar"))
            }
            invocation.processingEnv.requireTypeElement("foo.bar.Outer").let {
                assertThat(it.packageName).isEqualTo("foo.bar")
                assertThat(it.name).isEqualTo("Outer")
                assertThat(it.qualifiedName).isEqualTo("foo.bar.Outer")
                assertThat(it.className).isEqualTo(
                    ClassName.get("foo.bar", "Outer")
                )
            }
            invocation.processingEnv.requireTypeElement("foo.bar.Outer.Nested").let {
                assertThat(it.packageName).isEqualTo("foo.bar")
                assertThat(it.name).isEqualTo("Nested")
                assertThat(it.qualifiedName).isEqualTo("foo.bar.Outer.Nested")
                assertThat(it.className).isEqualTo(
                    ClassName.get("foo.bar", "Outer", "Nested")
                )
            }
            if (invocation.isKsp) {
                // these are KSP specific tests, typenames are tested elsewhere
                invocation.processingEnv.requireTypeElement("java.lang.Integer").let {
                    // always return kotlin types, this is what compiler does
                    assertThat(it.packageName).isEqualTo("kotlin")
                    assertThat(it.name).isEqualTo("Int")
                    assertThat(it.qualifiedName).isEqualTo("kotlin.Int")
                }
                invocation.processingEnv.requireTypeElement("kotlin.Int").let {
                    assertThat(it.packageName).isEqualTo("kotlin")
                    assertThat(it.name).isEqualTo("Int")
                    assertThat(it.qualifiedName).isEqualTo("kotlin.Int")
                }
            }
        }
    }

    @Test
    fun typeAndSuperType() {
        val src = Source.kotlin(
            "foo.kt",
            """
            package foo.bar;
            class Baz : MyInterface, AbstractClass() {
            }
            abstract class AbstractClass {}
            interface MyInterface {}
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("foo.bar.Baz").let {
                assertThat(it.superType).isEqualTo(
                    invocation.processingEnv.requireType("foo.bar.AbstractClass")
                )
                assertThat(it.type).isEqualTo(
                    invocation.processingEnv.requireType("foo.bar.Baz")
                )
                assertThat(it.isInterface()).isFalse()
                assertThat(it.isKotlinObject()).isFalse()
                assertThat(it.isAbstract()).isFalse()
            }
            invocation.processingEnv.requireTypeElement("foo.bar.AbstractClass").let {
                assertThat(it.superType).let {
                    // KSP does not return Object / Any as super class
                    if (invocation.isKsp) {
                        it.isNull()
                    } else {
                        it.isEqualTo(
                            invocation.processingEnv.requireType(TypeName.OBJECT)
                        )
                    }
                }
                assertThat(it.isAbstract()).isTrue()
                assertThat(it.isInterface()).isFalse()
                assertThat(it.type).isEqualTo(
                    invocation.processingEnv.requireType("foo.bar.AbstractClass")
                )
            }
            invocation.processingEnv.requireTypeElement("foo.bar.MyInterface").let {
                assertThat(it.superType).isNull()
                assertThat(it.isInterface()).isTrue()
                assertThat(it.type).isEqualTo(
                    invocation.processingEnv.requireType("foo.bar.MyInterface")
                )
            }
        }
    }

    @Test
    fun nestedClassName() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            package foo.bar;
            class Outer {
                class Inner
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("foo.bar.Outer").let {
                assertThat(it.className).isEqualTo(ClassName.get("foo.bar", "Outer"))
                assertThat(it.enclosingTypeElement).isNull()
            }
            invocation.processingEnv.requireTypeElement("foo.bar.Outer.Inner").let {
                assertThat(it.className).isEqualTo(ClassName.get("foo.bar", "Outer", "Inner"))
                assertThat(it.packageName).isEqualTo("foo.bar")
                assertThat(it.name).isEqualTo("Inner")
                assertThat(it.enclosingTypeElement).isEqualTo(
                    invocation.processingEnv.requireTypeElement("foo.bar.Outer")
                )
            }
        }
    }

    @Test
    fun modifiers() {
        val kotlinSrc = Source.kotlin(
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
            """.trimIndent()
        )
        val javaSrc = Source.java(
            "OuterJavaClass",
            """
            public class OuterJavaClass {
                public class InnerJavaClass {}
                public static class NestedJavaClass {}
            }
            """.trimIndent()
        )
        val javaAnnotationSrc = Source.java(
            "JavaAnnotation",
            """
            public @interface JavaAnnotation {
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(kotlinSrc, javaSrc, javaAnnotationSrc)
        ) { invocation ->
            fun getModifiers(element: XTypeElement): Set<String> {
                val result = mutableSetOf<String>()
                if (element.isAbstract()) result.add("abstract")
                if (element.isFinal()) result.add("final")
                if (element.isPrivate()) result.add("private")
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

            fun getModifiers(qName: String): Set<String> = getModifiers(
                invocation.processingEnv
                    .requireTypeElement(qName)
            )

            assertThat(getModifiers("OpenClass"))
                .containsExactly("public", "class")
            assertThat(getModifiers("AbstractClass"))
                .containsExactly("abstract", "public", "class")
            assertThat(getModifiers("MyObject"))
                .containsExactly("final", "public", "object")
            assertThat(getModifiers("MyInterface"))
                .containsExactly("abstract", "interface", "public")
            assertThat(getModifiers("Final"))
                .containsExactly("final", "public", "class")
            assertThat(getModifiers("PrivateClass"))
                .containsExactlyElementsIn(
                    if (invocation.isKsp) {
                        listOf("private", "final", "class")
                    } else {
                        // java does not support top level private classes.
                        listOf("final", "class")
                    }
                )
            assertThat(getModifiers("OuterKotlinClass.InnerKotlinClass"))
                .containsExactly("final", "public", "class")
            assertThat(getModifiers("OuterKotlinClass.NestedKotlinClass"))
                .containsExactly("final", "public", "static", "class")
            assertThat(getModifiers("OuterJavaClass.InnerJavaClass"))
                .containsExactly("public", "class")
            assertThat(getModifiers("OuterJavaClass.NestedJavaClass"))
                .containsExactly("public", "static", "class")
            assertThat(getModifiers("JavaAnnotation"))
                .containsExactly("abstract", "public", "annotation")
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
            assertThat(getModifiers("InlineClass"))
                .containsExactly("public", "final", "class", "value")

            if (!invocation.isKsp) {
                // TODO: Enable for ksp too once it supports fun interfaces
                //  https://github.com/google/ksp/issues/393
                assertThat(getModifiers("FunInterface"))
                    .containsExactly("public", "abstract", "interface", "fun")
            }

        }
    }

    @Test
    fun kindName() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class MyClass
            interface MyInterface
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("MyClass").let {
                assertThat(it.kindName()).isEqualTo("class")
            }
            invocation.processingEnv.requireTypeElement("MyInterface").let {
                assertThat(it.kindName()).isEqualTo("interface")
            }
        }
    }

    @Test
    fun fieldBasic() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            open class BaseClass<T>(val genericProp : T) {
                fun baseMethod(input: T) {}
            }
            class SubClass(x : Int) : BaseClass<Int>(x) {
                val subClassProp : String = "abc"
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val baseClass = invocation.processingEnv.requireTypeElement("BaseClass")
            assertThat(baseClass.getAllFieldNames()).containsExactly("genericProp")
            val subClass = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(subClass.getAllFieldNames()).containsExactly("genericProp", "subClassProp")

            val baseMethod = baseClass.getMethod("baseMethod")
            baseMethod.asMemberOf(subClass.type).let { methodType ->
                val genericArg = methodType.parameterTypes.first()
                assertThat(genericArg.typeName).isEqualTo(TypeName.INT.box())
            }

            baseClass.getField("genericProp").let { field ->
                if (invocation.isKsp) {
                    // ksp replaces these with Any?
                    assertThat(field.type.typeName).isEqualTo(TypeName.OBJECT)
                } else {
                    assertThat(field.type.typeName).isEqualTo(TypeVariableName.get("T"))
                }
            }

            subClass.getField("genericProp").let { field ->
                // this is tricky because even though it is non-null it, it should still be boxed
                assertThat(field.type.typeName).isEqualTo(TypeName.INT.box())
            }
        }
    }

    @Test
    fun fieldsOverride() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            open class BaseClass(
                open val value : List<Int>
            )
            class SubClass(
                override val value : MutableList<Int>
            ) : BaseClass(value)
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val baseClass = invocation.processingEnv.requireTypeElement("BaseClass")
            assertThat(baseClass.getAllFieldNames()).containsExactly("value")
            val subClass = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(subClass.getAllFieldNames()).containsExactly("value")
            assertThat(
                baseClass.getField("value").type.typeName
            ).isEqualTo(
                ParameterizedTypeName.get(List::class.java, Integer::class.java)
            )
            assertThat(
                subClass.getField("value").type.typeName
            ).isEqualTo(
                ParameterizedTypeName.get(List::class.java, Integer::class.java)
            )
        }
    }

    @Test
    fun fieldsInInterfaces() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            interface MyInterface {
                var x:Int
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("MyInterface")
            assertThat(element.getAllFieldsIncludingPrivateSupers()).isEmpty()
            element.getMethod("getX").let {
                assertThat(it.isAbstract()).isTrue()
            }
            element.getMethod("setX").let {
                assertThat(it.isAbstract()).isTrue()
            }
        }
    }

    @Test
    fun fieldsInAbstractClass() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            abstract class MyAbstractClass {
                @JvmField
                var jvmVar: Int = 0
                abstract var abstractVar: Int
                var nonAbstractVar: Int = 0
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("MyAbstractClass")
            assertThat(
                element.getAllFieldNames()
            ).containsExactly(
                "nonAbstractVar", "jvmVar"
            )
            assertThat(
                element.getDeclaredMethods().map { it.name }
            ).containsExactly(
                "getAbstractVar", "setAbstractVar",
                "getNonAbstractVar", "setNonAbstractVar"
            )
            element.getMethod("getAbstractVar").let {
                assertThat(it.isAbstract()).isTrue()
            }
            element.getMethod("setAbstractVar").let {
                assertThat(it.isAbstract()).isTrue()
            }

            element.getMethod("getNonAbstractVar").let {
                assertThat(it.isAbstract()).isFalse()
            }
            element.getMethod("setNonAbstractVar").let {
                assertThat(it.isAbstract()).isFalse()
            }
        }
    }

    @Test
    fun propertyGettersSetters() {
        val dependencyJavaSource = Source.java(
            "DependencyJavaSubject.java",
            """
            class DependencyJavaSubject {
                int myField;
                private int mutable;
                int immutable;
                int getMutable() {return 3;}
                void setMutable(int x) {}
                int getImmutable() {return 3;}
            }
            """.trimIndent()
        )
        val dependencyKotlinSource = Source.kotlin(
            "DependencyKotlinSubject.kt",
            """
            class DependencyKotlinSubject {
                private val myField = 0
                var mutable: Int = 0
                val immutable:Int = 0
            }
            """.trimIndent()
        )
        val dependency = compileFiles(listOf(dependencyJavaSource, dependencyKotlinSource))
        val javaSource = Source.java(
            "JavaSubject.java",
            """
            class JavaSubject {
                int myField;
                private int mutable;
                int immutable;
                int getMutable() {return 3;}
                void setMutable(int x) {}
                int getImmutable() {return 3;}
            }
            """.trimIndent()
        )
        val kotlinSource = Source.kotlin(
            "KotlinSubject.kt",
            """
            class KotlinSubject {
                private val myField = 0
                var mutable: Int = 0
                val immutable:Int = 0
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(javaSource, kotlinSource),
            classpath = listOf(dependency)
        ) { invocation ->
            listOf(
                "JavaSubject", "DependencyJavaSubject",
                "KotlinSubject", "DependencyKotlinSubject"
            ).map {
                invocation.processingEnv.requireTypeElement(it)
            }.forEach { subject ->
                assertWithMessage(subject.qualifiedName)
                    .that(
                        subject.getDeclaredMethods().map {
                            it.name
                        }
                    ).containsExactly(
                        "getMutable", "setMutable", "getImmutable"
                    )
            }
        }
    }

    @Test
    fun declaredAndInstanceMethods() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            open class Base(x:Int) {
                open fun baseFun(): Int = TODO()
                suspend fun suspendFun(): Int = TODO()
                private fun privateBaseFun(): Int = TODO()
                companion object {
                    @JvmStatic
                    fun staticBaseFun(): Int = TODO()
                    fun companionMethod(): Int = TODO()
                }
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
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val base = invocation.processingEnv.requireTypeElement("Base")
            val objectMethodNames = invocation.objectMethodNames()
            assertThat(base.getDeclaredMethods().names()).containsExactly(
                "baseFun", "suspendFun", "privateBaseFun", "staticBaseFun"
            )

            val sub = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(sub.getDeclaredMethods().names()).containsExactly(
                "baseFun", "subFun", "privateSubFun", "staticFun"
            )
            assertThat(
                sub.getAllNonPrivateInstanceMethods().names() - objectMethodNames
            ).containsExactly(
                "baseFun", "suspendFun", "subFun"
            )
        }
    }

    @Test
    fun allMethods() {
        val src = Source.kotlin(
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
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val objectMethodNames = invocation.objectMethodNames()
            val klass = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(
                klass.getAllMethods().names() - objectMethodNames
            ).containsExactly(
                "baseMethod", "overriddenMethod", "baseCompanionMethod",
                "interfaceMethod", "subMethod", "privateSubMethod", "subCompanionMethod"
            )
        }
    }

    @Test
    fun gettersSetters() {
        val src = Source.kotlin(
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
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val objectMethodNames = invocation.objectMethodNames()
            invocation.processingEnv.requireTypeElement("JustGetter").let { base ->
                assertThat(base.getDeclaredMethods().names()).containsExactly(
                    "getX"
                )
                assertThat(base.getAllMethods().names() - objectMethodNames).containsExactly(
                    "getX"
                )
                assertThat(
                    base.getAllNonPrivateInstanceMethods().names() - objectMethodNames
                ).containsExactly(
                    "getX"
                )
            }
            invocation.processingEnv.requireTypeElement("GetterSetter").let { sub ->
                assertThat(sub.getDeclaredMethods().names()).containsExactly(
                    "getY", "setY"
                )
                assertThat(sub.getAllMethods().names() - objectMethodNames).containsExactly(
                    "getX", "getY", "setY"
                )
                assertThat(
                    sub.getAllNonPrivateInstanceMethods().names() - objectMethodNames
                ).containsExactly(
                    "getX", "getY", "setY"
                )
            }
        }
    }

    @Test
    fun gettersSetters_companion() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            open class CompanionSubject {
                companion object {
                    @JvmStatic
                    var mutableStatic: String = "a"
                    @JvmStatic
                    val immutableStatic: String = "bar"
                    val companionProp: Int = 3
                }
            }
            class SubClass : CompanionSubject()
            """.trimIndent()
        )
        // KAPT is a bit aggressive in adding fields, specifically, it adds companionProp and
        // Companion as static fields which are not really fields from room's perspective.
        runKspTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("CompanionSubject")
            assertThat(subject.getAllFieldNames()).containsExactly(
                "mutableStatic", "immutableStatic"
            )
            assertThat(subject.getDeclaredMethods().names()).containsExactly(
                "getMutableStatic", "setMutableStatic", "getImmutableStatic"
            )
            assertThat(subject.getAllMethods().names()).containsExactly(
                "getMutableStatic", "setMutableStatic", "getImmutableStatic"
            )
            assertThat(subject.getAllNonPrivateInstanceMethods().names()).isEmpty()
            val subClass = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(subClass.getDeclaredMethods()).isEmpty()
            assertThat(subClass.getAllMethods().names()).containsExactly(
                "getMutableStatic", "setMutableStatic", "getImmutableStatic"
            )
        }
    }

    @Test
    fun gettersSetters_interface() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            interface JustGetter {
                val x:Int
            }
            interface GetterSetter : JustGetter {
                var y:Int
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("JustGetter").let { base ->
                assertThat(base.getDeclaredMethods().names()).containsExactly(
                    "getX"
                )
                assertThat(base.getAllMethods().names()).containsExactly(
                    "getX"
                )
                assertThat(base.getAllNonPrivateInstanceMethods().names()).containsExactly(
                    "getX"
                )
            }
            invocation.processingEnv.requireTypeElement("GetterSetter").let { sub ->
                assertThat(sub.getDeclaredMethods().names()).containsExactly(
                    "getY", "setY"
                )
                assertThat(sub.getAllMethods().names()).containsExactly(
                    "getX", "getY", "setY"
                )
                assertThat(sub.getAllNonPrivateInstanceMethods().names()).containsExactly(
                    "getX", "getY", "setY"
                )
            }
        }
    }

    @Test
    fun constructors() {
        val src = Source.kotlin(
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
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val subjects = listOf(
                "MyInterface", "NoExplicitConstructor", "Base", "ExplicitConstructor",
                "BaseWithSecondary", "Sub", "SubWith3Constructors",
                "AbstractNoExplicit", "AbstractExplicit"
            )
            val constructorCounts = subjects.map {
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

            val primaryConstructorParameterNames = subjects.map {
                it to invocation.processingEnv.requireTypeElement(it)
                    .findPrimaryConstructor()
                    ?.parameters?.map {
                        it.name
                    }
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
                    "AbstractExplicit" to listOf("x")
                )
        }
    }

    @Test
    fun jvmDefault() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            interface MyInterface {
                fun notJvmDefault()
                @JvmDefault
                fun jvmDefault() {}
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("MyInterface")
            assertThat(subject.getMethod("notJvmDefault").isJavaDefault()).isFalse()
            assertThat(subject.getMethod("jvmDefault").isJavaDefault()).isTrue()
        }
    }

    @Test
    fun constructors_java() {
        val src = Source.java(
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
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val subjects = listOf(
                "MyInterface", "NoExplicitConstructor", "Base", "ExplicitConstructor",
                "BaseWithSecondary", "Sub", "SubWith3Constructors",
                "AbstractNoExplicit", "AbstractExplicit"
            )
            val constructorCounts = subjects.map {
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
        fun createSources(packageName: String) = listOf(
            Source.kotlin(
                "$packageName/KotlinEnum.kt",
                """
                package $packageName
                enum class KotlinEnum(private val x:Int) {
                    VAL1(1),
                    VAL2(2);

                    fun enumMethod():Unit {}
                }
                """.trimIndent()
            ),
            Source.java(
                "$packageName.JavaEnum",
                """
                package $packageName;
                public enum JavaEnum {
                    VAL1(1),
                    VAL2(2);

                    private int x;

                    JavaEnum(int x) {
                        this.x = x;
                    }
                    void enumMethod() {}
                }
                """.trimIndent()
            )
        )

        val classpath = compileFiles(
            createSources("lib")
        )
        runProcessorTest(
            sources = createSources("app"),
            classpath = listOf(classpath)
        ) { invocation ->
            listOf(
                "lib.KotlinEnum", "lib.JavaEnum",
                "app.KotlinEnum", "app.JavaEnum"
            ).forEach { qName ->
                val typeElement = invocation.processingEnv.requireTypeElement(qName)
                assertWithMessage("$qName is enum")
                    .that(typeElement.isEnum())
                    .isTrue()
                assertWithMessage("$qName does not report enum constants in methods")
                    .that(typeElement.getDeclaredMethods().map { it.name })
                    .run {
                        contains("enumMethod")
                        containsNoneOf("VAL1", "VAL2")
                    }
                assertWithMessage("$qName can return enum constants")
                    .that((typeElement as XEnumTypeElement).enumConstantNames)
                    .containsExactly("VAL1", "VAL2")
                assertWithMessage("$qName  does not report enum constants in fields")
                    .that(typeElement.getAllFieldNames())
                    .run {
                        contains("x")
                        containsNoneOf("VAL1", "VAL2")
                    }
            }
        }
    }

    /**
     * it is good to exclude methods coming from Object when testing as they differ between KSP
     * and KAPT but irrelevant for Room.
     */
    private fun XTestInvocation.objectMethodNames() = processingEnv
        .requireTypeElement("java.lang.Object")
        .getAllMethods()
        .names()

    private fun List<XMethodElement>.names() = map {
        it.name
    }
}
