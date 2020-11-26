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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getAllFieldNames
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTestIncludingKsp
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KspTypeElementTest {
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
        runKspTest(
            sources = listOf(src1, src2),
            succeed = true
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
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
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
                assertThat(it.superType).isNull()
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
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
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
        val src = Source.kotlin(
            "Foo.kt",
            """
            open class OpenClass
            abstract class AbstractClass
            object MyObject
            interface MyInterface
            class Final
            private class PrivateClass
            """.trimIndent()
        )
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
            fun getModifiers(element: XTypeElement): Set<String> {
                val result = mutableSetOf<String>()
                if (element.isAbstract()) result.add("abstract")
                if (element.isFinal()) result.add("final")
                if (element.isPrivate()) result.add("private")
                if (element.isProtected()) result.add("protected")
                if (element.isPublic()) result.add("public")
                if (element.isKotlinObject()) result.add("object")
                if (element.isInterface()) result.add("interface")
                return result
            }

            fun getModifiers(qName: String): Set<String> = getModifiers(
                invocation.processingEnv
                    .requireTypeElement(qName)
            )

            assertThat(getModifiers("OpenClass"))
                .containsExactly("public")
            assertThat(getModifiers("AbstractClass"))
                .containsExactly("abstract", "public")
            assertThat(getModifiers("MyObject"))
                .containsExactly("final", "public", "object")
            assertThat(getModifiers("MyInterface"))
                .containsExactly("interface", "public")
            assertThat(getModifiers("Final"))
                .containsExactly("final", "public")
            assertThat(getModifiers("PrivateClass"))
                .containsExactly("private", "final")
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
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
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
        runProcessorTestIncludingKsp(sources = listOf(src)) { invocation ->
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
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
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
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
            val base = invocation.processingEnv.requireTypeElement("Base")
            assertThat(base.getDeclaredMethods().names()).containsExactly(
                "baseFun", "suspendFun", "privateBaseFun", "staticBaseFun"
            )

            val sub = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(sub.getDeclaredMethods().names()).containsExactly(
                "baseFun", "subFun", "privateSubFun", "staticFun"
            )
            assertThat(sub.getAllNonPrivateInstanceMethods().names()).containsExactly(
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
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
            val klass = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(klass.getAllMethods().names()).containsExactly(
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
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
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
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
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
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
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
        runProcessorTestIncludingKsp(sources = listOf(src)) { invocation ->
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
                fun jvmDefault()
            }
            """.trimIndent()
        )
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
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
        runProcessorTestIncludingKsp(sources = listOf(src)) { invocation ->
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
                Truth.assertWithMessage(it)
                    .that(invocation.processingEnv.requireTypeElement(it).findPrimaryConstructor())
                    .isNull()
            }
        }
    }

    private fun List<XMethodElement>.names() = map {
        it.name
    }
}
