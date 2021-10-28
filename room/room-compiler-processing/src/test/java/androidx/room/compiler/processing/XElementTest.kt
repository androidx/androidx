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

import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.runProcessorTestWithoutKsp
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.reflect.KClass

@RunWith(JUnit4::class)
class XElementTest {
    @Test
    fun kotlinAnnotationModifierrs() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            object Subject {
                @Transient val transientProp:Int = 0
                @JvmStatic val staticProp:Int = 0
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("Subject").let {
                assertThat(
                    it.getField("transientProp").isTransient()
                ).isTrue()
                assertThat(
                    it.getField("staticProp").isStatic()
                ).isTrue()
            }
        }
    }

    @Test
    fun modifiers() {
        runProcessorTest(
            listOf(
                Source.java(
                    "foo.bar.Baz",
                    """
                package foo.bar;
                public abstract class Baz {
                    private int privateField;
                    int packagePrivateField;
                    protected int protectedField;
                    public int publicField;
                    transient int transientField;
                    static int staticField;

                    private void privateMethod() {}
                    void packagePrivateMethod() {}
                    public void publicMethod() {}
                    protected void protectedMethod() {}
                    final void finalMethod() {}
                    abstract void abstractMethod();
                    static void staticMethod() {}
                }
                    """.trimIndent()
                )
            )
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            fun XHasModifiers.readModifiers(): Set<String> {
                val result = mutableSetOf<String>()
                if (isPrivate()) result.add("private")
                if (isPublic()) result.add("public")
                if (isTransient()) result.add("transient")
                if (isStatic()) result.add("static")
                if (isFinal()) result.add("final")
                if (isAbstract()) result.add("abstract")
                if (isProtected()) result.add("protected")
                return result
            }

            fun XHasModifiers.assertModifiers(vararg expected: String) {
                assertThat(readModifiers()).containsExactlyElementsIn(expected)
            }
            element.assertModifiers("abstract", "public")
            element.getField("privateField").assertModifiers("private")
            element.getField("packagePrivateField").assertModifiers()
            // we don't read isProtected, no reason. we should eventually get rid of most if not
            // all anyways
            element.getField("protectedField").assertModifiers("protected")
            element.getField("publicField").assertModifiers("public")
            element.getField("transientField").assertModifiers("transient")
            element.getField("staticField").assertModifiers("static")

            element.getMethod("privateMethod").assertModifiers("private")
            element.getMethod("packagePrivateMethod").assertModifiers()
            element.getMethod("publicMethod").assertModifiers("public")
            element.getMethod("protectedMethod").assertModifiers("protected")
            element.getMethod("finalMethod").assertModifiers("final")
            element.getMethod("abstractMethod").assertModifiers("abstract")
            element.getMethod("staticMethod").assertModifiers("static")

            assertThat(
                element.getMethod("privateMethod").isOverrideableIgnoringContainer()
            ).isFalse()
            assertThat(
                element.getMethod("packagePrivateMethod").isOverrideableIgnoringContainer()
            ).isTrue()
            assertThat(element.getMethod("publicMethod").isOverrideableIgnoringContainer()).isTrue()
            assertThat(
                element.getMethod("protectedMethod").isOverrideableIgnoringContainer()
            ).isTrue()
            assertThat(element.getMethod("finalMethod").isOverrideableIgnoringContainer()).isFalse()
            assertThat(
                element.getMethod("abstractMethod").isOverrideableIgnoringContainer()
            ).isTrue()
            assertThat(
                element.getMethod("staticMethod").isOverrideableIgnoringContainer()
            ).isFalse()
        }
    }

    @Test
    fun typeParams() {
        val genericBase = Source.java(
            "foo.bar.Base",
            """
                package foo.bar;
                public class Base<T> {
                    protected T returnT() {
                        throw new RuntimeException("Stub");
                    }
                    public int receiveT(T param1) {
                        return 3;
                    }
                    public <R> int receiveR(R param1) {
                        return 3;
                    }
                    public <R> R returnR() {
                        throw new RuntimeException("Stub");
                    }
                }
            """.trimIndent()
        )
        val boundedChild = Source.java(
            "foo.bar.Child",
            """
                package foo.bar;
                public class Child extends Base<String> {
                }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(genericBase, boundedChild)
        ) {
            fun validateElement(element: XTypeElement, tTypeName: TypeName, rTypeName: TypeName) {
                element.getMethod("returnT").let { method ->
                    assertThat(method.parameters).isEmpty()
                    assertThat(method.returnType.typeName).isEqualTo(tTypeName)
                }
                element.getMethod("receiveT").let { method ->
                    method.getParameter("param1").let { param ->
                        assertThat(param.type.typeName).isEqualTo(tTypeName)
                    }
                    assertThat(method.returnType.typeName).isEqualTo(TypeName.INT)
                }
                element.getMethod("receiveR").let { method ->
                    method.getParameter("param1").let { param ->
                        assertThat(param.type.typeName).isEqualTo(rTypeName)
                    }
                    assertThat(method.returnType.typeName).isEqualTo(TypeName.INT)
                }
                element.getMethod("returnR").let { method ->
                    assertThat(method.parameters).isEmpty()
                    assertThat(method.returnType.typeName).isEqualTo(rTypeName)
                }
            }
            validateElement(
                element = it.processingEnv.requireTypeElement("foo.bar.Base"),
                tTypeName = if (it.isKsp) {
                    // when inheritance resolution happens, KSP resolves them to object
                    TypeName.OBJECT
                } else {
                    TypeVariableName.get("T")
                },
                rTypeName = TypeVariableName.get("R")
            )
            validateElement(
                element = it.processingEnv.requireTypeElement("foo.bar.Child"),
                tTypeName = String::class.className(),
                rTypeName = TypeVariableName.get("R")
            )
        }
    }

    @Test
    fun annotationAvailability() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            import org.junit.*;
            import org.junit.runner.*;
            import org.junit.runners.*;
            import androidx.room.compiler.processing.testcode.OtherAnnotation;

            @RunWith(JUnit4.class)
            class Baz {
                @OtherAnnotation(value="xx")
                String testField;

                @org.junit.Test
                void testMethod() {}
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.hasAnnotation(RunWith::class)).isTrue()
            assertThat(element.hasAnnotation(Test::class)).isFalse()
            element.getMethod("testMethod").let { method ->
                assertThat(method.hasAnnotation(Test::class)).isTrue()
                assertThat(method.hasAnnotation(Override::class)).isFalse()
                assertThat(
                    method.hasAnnotationWithPackage(
                        "org.junit"
                    )
                ).isTrue()
            }
            element.getField("testField").let { field ->
                assertThat(field.hasAnnotation(OtherAnnotation::class)).isTrue()
                assertThat(field.hasAnnotation(Test::class)).isFalse()
            }
            assertThat(
                element.hasAnnotationWithPackage(
                    "org.junit.runner"
                )
            ).isTrue()
            assertThat(
                element.hasAnnotationWithPackage(
                    "org.junit"
                )
            ).isFalse()
            assertThat(
                element.hasAnnotationWithPackage(
                    "foo.bar"
                )
            ).isFalse()
        }
    }

    @Test
    fun hasAllAnnotations() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            import org.junit.*;
            import org.junit.runner.*;
            import org.junit.runners.*;
            import androidx.room.compiler.processing.testcode.OtherAnnotation;

            @RunWith(JUnit4.class)
            class Baz {
                @OtherAnnotation(value="xx")
                String testField;

                @org.junit.Test
                @OtherAnnotation(value="yy")
                void testMethod() {}
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.hasAllAnnotations(*arrayOf<KClass<Annotation>>())).isTrue()
            assertThat(element.hasAllAnnotations(RunWith::class)).isTrue()
            assertThat(element.hasAllAnnotations(RunWith::class, Test::class)).isFalse()

            element.getMethod("testMethod").let { method ->
                assertThat(method.hasAllAnnotations(*arrayOf<KClass<Annotation>>())).isTrue()
                assertThat(method.hasAllAnnotations(Test::class)).isTrue()
                assertThat(method.hasAllAnnotations(Test::class, OtherAnnotation::class)).isTrue()
                assertThat(method.hasAllAnnotations(Test::class, OtherAnnotation::class,
                    RunWith::class)).isFalse()
            }
            element.getField("testField").let { field ->
                assertThat(field.hasAllAnnotations(*arrayOf<KClass<Annotation>>())).isTrue()
                assertThat(field.hasAllAnnotations(OtherAnnotation::class)).isTrue()
                assertThat(field.hasAllAnnotations(OtherAnnotation::class, Test::class)).isFalse()
                assertThat(field.hasAllAnnotations(OtherAnnotation::class, OtherAnnotation::class))
                    .isTrue()
            }
        }
    }

    @Test
    fun nonType() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            class Baz {
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("java.lang.Object")
            // make sure we return null for not existing types
            assertThat(element.superType).isNull()
        }
    }

    @Test
    fun isSomething() {
        val subject = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            class Baz {
                int field;

                void method() {}
                static interface Inner {}
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(subject)
        ) {
            val inner = ClassName.get("foo.bar", "Baz.Inner")
            assertThat(
                it.processingEnv.requireTypeElement(inner).isInterface()
            ).isTrue()
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.isInterface()).isFalse()
            assertThat(element.isAbstract()).isFalse()
            assertThat(element.isTypeElement()).isTrue()
            element.getField("field").let { field ->
                assertThat(field.isTypeElement()).isFalse()
                assertThat(field.isAbstract()).isFalse()
                assertThat(field.isVariableElement()).isTrue()
                assertThat(field.isMethod()).isFalse()
            }
            element.getMethod("method").let { method ->
                assertThat(method.isTypeElement()).isFalse()
                assertThat(method.isAbstract()).isFalse()
                assertThat(method.isVariableElement()).isFalse()
                assertThat(method.isMethod()).isTrue()
            }
        }
    }

    @Test
    fun notATypeElement() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            class Baz {
                public static int x;
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getField("x").let { field ->
                assertThat(field.isStatic()).isTrue()
                assertThat(field.isTypeElement()).isFalse()
            }
        }
    }

    @Test
    fun nullability() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;

            import androidx.annotation.*;
            import java.util.List;
            class Baz {
                public static int primitiveInt;
                public static Integer boxedInt;
                @NonNull
                public static List<String> nonNullAnnotated;
                @Nullable
                public static List<String> nullableAnnotated;
            }
            """.trimIndent()
        )
        // enable once https://github.com/google/ksp/issues/167 is fixed
        runProcessorTestWithoutKsp(
            sources = listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getField("primitiveInt").let { field ->
                assertThat(field.type.nullability).isEqualTo(XNullability.NONNULL)
            }
            element.getField("boxedInt").let { field ->
                assertThat(field.type.nullability).isEqualTo(XNullability.UNKNOWN)
            }
            element.getField("nonNullAnnotated").let { field ->
                assertThat(field.type.nullability).isEqualTo(XNullability.NONNULL)
            }
            element.getField("nullableAnnotated").let { field ->
                assertThat(field.type.nullability).isEqualTo(XNullability.NULLABLE)
            }
        }
    }

    @Test
    fun toStringMatchesUnderlyingElement() {
        runProcessorTest { invocation ->
            invocation.processingEnv.findTypeElement("java.util.List").let { list ->
                val expected = if (invocation.isKsp) {
                    "MutableList"
                } else {
                    "java.util.List"
                }
                assertThat(list.toString()).isEqualTo(expected)
            }
        }
    }

    @Test
    fun docComment() {
        val javaSrc = Source.java(
            "JavaSubject",
            """
            /**
             * javadocs
             */
            public class JavaSubject {}
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "KotlinSubject.kt",
            """
            /**
             * kdocs
             */
            class KotlinSubject
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            assertThat(
                invocation.processingEnv.requireTypeElement("JavaSubject").docComment?.trim()
            ).isEqualTo("javadocs")
            assertThat(
                invocation.processingEnv.requireTypeElement("KotlinSubject").docComment?.trim()
            ).isEqualTo("kdocs")
        }
    }
}
