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

import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.KotlinTypeNames.INT_CLASS_NAME
import androidx.room.compiler.processing.util.KotlinTypeNames.LIST_CLASS_NAME
import androidx.room.compiler.processing.util.KotlinTypeNames.MUTABLELIST_CLASS_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getAllFieldNames
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KspTypeElementTest {
    @Test
    fun qualifiedNames() {
        val src1 = Source.kotlin(
            "Foo.kt", """
            class TopLevel
        """.trimIndent()
        )
        val src2 = Source.kotlin(
            "Bar.kt", """
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
                assertThat(it.packageName).isEqualTo("java.lang")
                assertThat(it.name).isEqualTo("Integer")
                assertThat(it.qualifiedName).isEqualTo("java.lang.Integer")
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
            "foo.kt", """
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
            "Foo.kt", """
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
            "Foo.kt", """
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
            "Foo.kt", """
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
            "Foo.kt", """
            open class BaseClass<T>(val genericProp : T)
            class SubClass(x : Int) : BaseClass<Int>(x) {
                val subClassProp : String = "abc"
            }
        """.trimIndent()
        )
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
            val baseClass = invocation.processingEnv.requireTypeElement("BaseClass")
            assertThat(baseClass.getAllFieldNames()).containsExactly("genericProp")
            val subClass = invocation.processingEnv.requireTypeElement("SubClass")
            assertThat(subClass.getAllFieldNames()).containsExactly("genericProp", "subClassProp")
            baseClass.getField("genericProp").let { field ->
                assertThat(field.type.typeName).isEqualTo(ClassName.get("", "BaseClass", "T"))
            }
            subClass.getField("genericProp").let { field ->
                assertThat(field.type.typeName).isEqualTo(ClassName.get("kotlin", "Int"))
            }
        }
    }

    @Test
    fun fieldsOverride() {
        val src = Source.kotlin(
            "Foo.kt", """
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
                ParameterizedTypeName.get(LIST_CLASS_NAME, INT_CLASS_NAME)
            )
            assertThat(
                subClass.getField("value").type.typeName
            ).isEqualTo(
                ParameterizedTypeName.get(MUTABLELIST_CLASS_NAME, INT_CLASS_NAME)
            )
        }
    }
}
