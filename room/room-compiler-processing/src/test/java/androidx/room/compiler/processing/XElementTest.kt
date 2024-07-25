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

import androidx.kruth.Subject
import androidx.kruth.assertThat
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.javac.JavacTypeElement
import androidx.room.compiler.processing.ksp.KspExecutableElement
import androidx.room.compiler.processing.ksp.KspFieldElement
import androidx.room.compiler.processing.ksp.KspFileMemberContainer
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticFileMemberContainer
import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.util.KOTLINC_LANGUAGE_1_9_ARGS
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.asJClassName
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getDeclaredField
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.kspProcessingEnv
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.runProcessorTestWithoutKsp
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.javapoet.JClassName
import kotlin.reflect.KClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XElementTest {
    @Test
    fun kotlinAnnotationModifierrs() {
        val src =
            Source.kotlin(
                "Subject.kt",
                """
            object Subject {
                @Transient val transientProp:Int = 0
                @JvmStatic val staticProp:Int = 0
            }
            """
                    .trimIndent()
            )
        runProcessorTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("Subject").let {
                assertThat(it.getField("transientProp").isTransient()).isTrue()
                assertThat(it.getField("staticProp").isStatic()).isTrue()
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
                    """
                        .trimIndent()
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

            element.getMethodByJvmName("privateMethod").assertModifiers("private")
            element.getMethodByJvmName("packagePrivateMethod").assertModifiers()
            element.getMethodByJvmName("publicMethod").assertModifiers("public")
            element.getMethodByJvmName("protectedMethod").assertModifiers("protected")
            element.getMethodByJvmName("finalMethod").assertModifiers("final")
            element.getMethodByJvmName("abstractMethod").assertModifiers("abstract")
            element.getMethodByJvmName("staticMethod").assertModifiers("static")

            assertThat(
                    element.getMethodByJvmName("privateMethod").isOverrideableIgnoringContainer()
                )
                .isFalse()
            assertThat(
                    element
                        .getMethodByJvmName("packagePrivateMethod")
                        .isOverrideableIgnoringContainer()
                )
                .isTrue()
            assertThat(element.getMethodByJvmName("publicMethod").isOverrideableIgnoringContainer())
                .isTrue()
            assertThat(
                    element.getMethodByJvmName("protectedMethod").isOverrideableIgnoringContainer()
                )
                .isTrue()
            assertThat(element.getMethodByJvmName("finalMethod").isOverrideableIgnoringContainer())
                .isFalse()
            assertThat(
                    element.getMethodByJvmName("abstractMethod").isOverrideableIgnoringContainer()
                )
                .isTrue()
            assertThat(element.getMethodByJvmName("staticMethod").isOverrideableIgnoringContainer())
                .isFalse()
        }
    }

    @Test
    fun typeParams() {
        val genericBase =
            Source.java(
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
            """
                    .trimIndent()
            )
        val boundedChild =
            Source.java(
                "foo.bar.Child",
                """
                package foo.bar;
                public class Child extends Base<String> {
                }
            """
                    .trimIndent()
            )
        runProcessorTest(listOf(genericBase, boundedChild)) {
            fun validateMethodElement(
                element: XTypeElement,
                tTypeName: XTypeName,
                rTypeName: XTypeName
            ) {
                element.getMethodByJvmName("returnT").let { method ->
                    assertThat(method.parameters).isEmpty()
                    assertThat(method.returnType.asTypeName()).isEqualTo(tTypeName)
                }
                element.getMethodByJvmName("receiveT").let { method ->
                    assertThat(method.getParameter("param1").type.asTypeName()).isEqualTo(tTypeName)
                    assertThat(method.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_INT)
                }
                element.getMethodByJvmName("receiveR").let { method ->
                    assertThat(method.getParameter("param1").type.asTypeName()).isEqualTo(rTypeName)
                    assertThat(method.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_INT)
                }
                element.getMethodByJvmName("returnR").let { method ->
                    assertThat(method.parameters).isEmpty()
                    assertThat(method.returnType.asTypeName()).isEqualTo(rTypeName)
                }
            }
            fun validateMethodTypeAsMemberOf(
                element: XTypeElement,
                tTypeName: XTypeName,
                rTypeName: XTypeName
            ) {
                element.getMethodByJvmName("returnT").asMemberOf(element.type).let { method ->
                    assertThat(method.parameterTypes).isEmpty()
                    assertThat(method.returnType.asTypeName()).isEqualTo(tTypeName)
                }
                element.getMethodByJvmName("receiveT").asMemberOf(element.type).let { method ->
                    assertThat(method.parameterTypes).hasSize(1)
                    assertThat(method.parameterTypes[0].asTypeName()).isEqualTo(tTypeName)
                    assertThat(method.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_INT)
                }
                element.getMethodByJvmName("receiveR").asMemberOf(element.type).let { method ->
                    assertThat(method.parameterTypes).hasSize(1)
                    assertThat(method.parameterTypes[0].asTypeName()).isEqualTo(rTypeName)
                    assertThat(method.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_INT)
                }
                element.getMethodByJvmName("returnR").let { method ->
                    assertThat(method.parameters).isEmpty()
                    assertThat(method.returnType.asTypeName()).isEqualTo(rTypeName)
                }
            }

            validateMethodElement(
                element = it.processingEnv.requireTypeElement("foo.bar.Base"),
                tTypeName =
                    XTypeName.getTypeVariableName(
                        "T",
                        listOf(XTypeName.ANY_OBJECT.copy(nullable = true))
                    ),
                rTypeName =
                    XTypeName.getTypeVariableName(
                        "R",
                        listOf(XTypeName.ANY_OBJECT.copy(nullable = true))
                    )
            )
            validateMethodElement(
                element = it.processingEnv.requireTypeElement("foo.bar.Child"),
                tTypeName =
                    XTypeName.getTypeVariableName(
                        "T",
                        listOf(XTypeName.ANY_OBJECT.copy(nullable = true))
                    ),
                rTypeName =
                    XTypeName.getTypeVariableName(
                        "R",
                        listOf(XTypeName.ANY_OBJECT.copy(nullable = true))
                    )
            )

            validateMethodTypeAsMemberOf(
                element = it.processingEnv.requireTypeElement("foo.bar.Base"),
                tTypeName =
                    XTypeName.getTypeVariableName(
                        "T",
                        listOf(XTypeName.ANY_OBJECT.copy(nullable = true))
                    ),
                rTypeName =
                    XTypeName.getTypeVariableName(
                        "R",
                        listOf(XTypeName.ANY_OBJECT.copy(nullable = true))
                    )
            )
            validateMethodTypeAsMemberOf(
                element = it.processingEnv.requireTypeElement("foo.bar.Child"),
                tTypeName = String::class.asClassName().copy(nullable = true),
                rTypeName =
                    XTypeName.getTypeVariableName(
                        "R",
                        listOf(XTypeName.ANY_OBJECT.copy(nullable = true))
                    )
            )
        }
    }

    @Test
    fun annotationAvailability() {
        val source =
            Source.java(
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
            """
                    .trimIndent()
            )
        runProcessorTest(listOf(source)) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.hasAnnotation(RunWith::class)).isTrue()
            assertThat(element.hasAnnotation(Test::class)).isFalse()
            element.getMethodByJvmName("testMethod").let { method ->
                assertThat(method.hasAnnotation(Test::class)).isTrue()
                assertThat(method.hasAnnotation(Override::class)).isFalse()
                assertThat(method.hasAnnotationWithPackage("org.junit")).isTrue()
            }
            element.getField("testField").let { field ->
                assertThat(field.hasAnnotation(OtherAnnotation::class)).isTrue()
                assertThat(field.hasAnnotation(Test::class)).isFalse()
            }
            assertThat(element.hasAnnotationWithPackage("org.junit.runner")).isTrue()
            assertThat(element.hasAnnotationWithPackage("org.junit")).isFalse()
            assertThat(element.hasAnnotationWithPackage("foo.bar")).isFalse()
        }
    }

    @Test
    fun hasAllAnnotations() {
        val source =
            Source.java(
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
            """
                    .trimIndent()
            )
        runProcessorTest(listOf(source)) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.hasAllAnnotations(*arrayOf<KClass<Annotation>>())).isTrue()
            assertThat(element.hasAllAnnotations(RunWith::class)).isTrue()
            assertThat(element.hasAllAnnotations(RunWith::class, Test::class)).isFalse()

            assertThat(element.hasAllAnnotations(*arrayOf<JClassName>())).isTrue()
            assertThat(element.hasAllAnnotations(RunWith::class.asJClassName())).isTrue()
            assertThat(
                    element.hasAllAnnotations(
                        RunWith::class.asJClassName(),
                        Test::class.asJClassName()
                    )
                )
                .isFalse()

            assertThat(element.hasAllAnnotations(emptyList<JClassName>())).isTrue()
            assertThat(element.hasAllAnnotations(listOf(RunWith::class.asJClassName()))).isTrue()
            assertThat(
                    element.hasAllAnnotations(
                        listOf(RunWith::class.asJClassName(), Test::class.asJClassName())
                    )
                )
                .isFalse()

            element.getMethodByJvmName("testMethod").let { method ->
                assertThat(method.hasAllAnnotations(*arrayOf<KClass<Annotation>>())).isTrue()
                assertThat(method.hasAllAnnotations(Test::class)).isTrue()
                assertThat(method.hasAllAnnotations(Test::class, OtherAnnotation::class)).isTrue()
                assertThat(
                        method.hasAllAnnotations(
                            Test::class,
                            OtherAnnotation::class,
                            RunWith::class
                        )
                    )
                    .isFalse()

                assertThat(method.hasAllAnnotations(*arrayOf<JClassName>())).isTrue()
                assertThat(method.hasAllAnnotations(Test::class.asJClassName())).isTrue()
                assertThat(
                        method.hasAllAnnotations(
                            Test::class.asJClassName(),
                            OtherAnnotation::class.asJClassName()
                        )
                    )
                    .isTrue()
                assertThat(
                        method.hasAllAnnotations(
                            Test::class.asJClassName(),
                            OtherAnnotation::class.asJClassName(),
                            RunWith::class.asJClassName()
                        )
                    )
                    .isFalse()

                assertThat(method.hasAllAnnotations(emptyList<JClassName>())).isTrue()
                assertThat(method.hasAllAnnotations(listOf(Test::class.asJClassName()))).isTrue()
                assertThat(
                        method.hasAllAnnotations(
                            listOf(
                                Test::class.asJClassName(),
                                OtherAnnotation::class.asJClassName()
                            )
                        )
                    )
                    .isTrue()
                assertThat(
                        method.hasAllAnnotations(
                            listOf(
                                Test::class.asJClassName(),
                                OtherAnnotation::class.asJClassName(),
                                RunWith::class.asJClassName()
                            )
                        )
                    )
                    .isFalse()
            }
            element.getField("testField").let { field ->
                assertThat(field.hasAllAnnotations(*arrayOf<KClass<Annotation>>())).isTrue()
                assertThat(field.hasAllAnnotations(OtherAnnotation::class)).isTrue()
                assertThat(field.hasAllAnnotations(OtherAnnotation::class, Test::class)).isFalse()
                assertThat(field.hasAllAnnotations(OtherAnnotation::class, OtherAnnotation::class))
                    .isTrue()

                assertThat(field.hasAllAnnotations(*arrayOf<JClassName>())).isTrue()
                assertThat(field.hasAllAnnotations(OtherAnnotation::class.asJClassName())).isTrue()
                assertThat(
                        field.hasAllAnnotations(
                            OtherAnnotation::class.asJClassName(),
                            Test::class.asJClassName()
                        )
                    )
                    .isFalse()
                assertThat(
                        field.hasAllAnnotations(
                            OtherAnnotation::class.asJClassName(),
                            OtherAnnotation::class.asJClassName()
                        )
                    )
                    .isTrue()

                assertThat(field.hasAllAnnotations(listOf<JClassName>())).isTrue()
                assertThat(field.hasAllAnnotations(listOf(OtherAnnotation::class.asJClassName())))
                    .isTrue()
                assertThat(
                        field.hasAllAnnotations(
                            listOf(
                                OtherAnnotation::class.asJClassName(),
                                Test::class.asJClassName()
                            )
                        )
                    )
                    .isFalse()
                assertThat(
                        field.hasAllAnnotations(
                            listOf(
                                OtherAnnotation::class.asJClassName(),
                                OtherAnnotation::class.asJClassName()
                            )
                        )
                    )
                    .isTrue()
            }
        }
    }

    @Test
    fun hasAnyAnnotations() {
        val source =
            Source.java(
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
            """
                    .trimIndent()
            )
        runProcessorTest(listOf(source)) { it ->
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.hasAnyAnnotation(*arrayOf<KClass<Annotation>>())).isFalse()
            assertThat(element.hasAnyAnnotation(RunWith::class)).isTrue()
            assertThat(element.hasAnyAnnotation(RunWith::class, Test::class)).isTrue()

            assertThat(element.hasAnyAnnotation(*arrayOf<JClassName>())).isFalse()
            assertThat(element.hasAnyAnnotation(RunWith::class.asJClassName())).isTrue()
            assertThat(
                    element.hasAnyAnnotation(
                        RunWith::class.asJClassName(),
                        Test::class.asJClassName()
                    )
                )
                .isTrue()

            assertThat(element.hasAnyAnnotation(emptyList<JClassName>())).isFalse()
            assertThat(element.hasAnyAnnotation(listOf(RunWith::class.asJClassName()))).isTrue()
            assertThat(
                    element.hasAnyAnnotation(
                        listOf(RunWith::class.asJClassName(), Test::class.asJClassName())
                    )
                )
                .isTrue()

            element.getMethodByJvmName("testMethod").let { method ->
                assertThat(method.hasAnyAnnotation(*arrayOf<KClass<Annotation>>())).isFalse()
                assertThat(method.hasAnyAnnotation(Test::class)).isTrue()
                assertThat(method.hasAnyAnnotation(Test::class, OtherAnnotation::class)).isTrue()
                assertThat(
                        method.hasAnyAnnotation(Test::class, OtherAnnotation::class, RunWith::class)
                    )
                    .isTrue()

                assertThat(method.hasAnyAnnotation(*arrayOf<JClassName>())).isFalse()
                assertThat(method.hasAnyAnnotation(Test::class.asJClassName())).isTrue()
                assertThat(
                        method.hasAnyAnnotation(
                            Test::class.asJClassName(),
                            OtherAnnotation::class.asJClassName()
                        )
                    )
                    .isTrue()
                assertThat(
                        method.hasAnyAnnotation(
                            Test::class.asJClassName(),
                            OtherAnnotation::class.asJClassName(),
                            RunWith::class.asJClassName()
                        )
                    )
                    .isTrue()

                assertThat(method.hasAnyAnnotation(emptyList<JClassName>())).isFalse()
                assertThat(method.hasAnyAnnotation(listOf(Test::class.asJClassName()))).isTrue()
                assertThat(
                        method.hasAnyAnnotation(
                            listOf(
                                Test::class.asJClassName(),
                                OtherAnnotation::class.asJClassName()
                            )
                        )
                    )
                    .isTrue()
                assertThat(
                        method.hasAnyAnnotation(
                            listOf(
                                Test::class.asJClassName(),
                                OtherAnnotation::class.asJClassName(),
                                RunWith::class.asJClassName()
                            )
                        )
                    )
                    .isTrue()
            }
            element.getField("testField").let { field ->
                assertThat(field.hasAnyAnnotation(*arrayOf<KClass<Annotation>>())).isFalse()
                assertThat(field.hasAnyAnnotation(OtherAnnotation::class)).isTrue()
                assertThat(field.hasAnyAnnotation(OtherAnnotation::class, Test::class)).isTrue()
                assertThat(field.hasAnyAnnotation(OtherAnnotation::class, OtherAnnotation::class))
                    .isTrue()

                assertThat(field.hasAnyAnnotation(*arrayOf<JClassName>())).isFalse()
                assertThat(field.hasAnyAnnotation(OtherAnnotation::class.asJClassName())).isTrue()
                assertThat(
                        field.hasAnyAnnotation(
                            OtherAnnotation::class.asJClassName(),
                            Test::class.asJClassName()
                        )
                    )
                    .isTrue()
                assertThat(
                        field.hasAnyAnnotation(
                            OtherAnnotation::class.asJClassName(),
                            OtherAnnotation::class.asJClassName()
                        )
                    )
                    .isTrue()

                assertThat(field.hasAnyAnnotation(listOf<JClassName>())).isFalse()
                assertThat(field.hasAnyAnnotation(listOf(OtherAnnotation::class.asJClassName())))
                    .isTrue()
                assertThat(
                        field.hasAnyAnnotation(
                            listOf(
                                OtherAnnotation::class.asJClassName(),
                                Test::class.asJClassName()
                            )
                        )
                    )
                    .isTrue()
                assertThat(
                        field.hasAnyAnnotation(
                            listOf(
                                OtherAnnotation::class.asJClassName(),
                                OtherAnnotation::class.asJClassName()
                            )
                        )
                    )
                    .isTrue()
            }
        }
    }

    @Test
    fun nonType() {
        val source =
            Source.java(
                "foo.bar.Baz",
                """
            package foo.bar;
            class Baz {
            }
            """
                    .trimIndent()
            )
        runProcessorTest(listOf(source)) {
            val element = it.processingEnv.requireTypeElement("java.lang.Object")
            // make sure we return null for not existing types
            assertThat(element.superClass).isNull()
        }
    }

    @Test
    fun isSomething() {
        val subject =
            Source.java(
                "foo.bar.Baz",
                """
            package foo.bar;
            class Baz {
                int field;

                void method() {}
                static interface Inner {}
            }
            """
                    .trimIndent()
            )
        runProcessorTest(sources = listOf(subject)) {
            val inner = JClassName.get("foo.bar", "Baz.Inner")
            assertThat(it.processingEnv.requireTypeElement(inner.toString()).isInterface()).isTrue()
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
            element.getMethodByJvmName("method").let { method ->
                assertThat(method.isTypeElement()).isFalse()
                assertThat(method.isAbstract()).isFalse()
                assertThat(method.isVariableElement()).isFalse()
                assertThat(method.isMethod()).isTrue()
            }
        }
    }

    @Test
    fun notATypeElement() {
        val source =
            Source.java(
                "foo.bar.Baz",
                """
            package foo.bar;
            class Baz {
                public static int x;
            }
            """
                    .trimIndent()
            )
        runProcessorTest(listOf(source)) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getField("x").let { field ->
                assertThat(field.isStatic()).isTrue()
                assertThat(field.isTypeElement()).isFalse()
            }
        }
    }

    @Test
    fun nullability() {
        val source =
            Source.java(
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
            """
                    .trimIndent()
            )
        // enable once https://github.com/google/ksp/issues/167 is fixed
        runProcessorTestWithoutKsp(sources = listOf(source)) {
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
                val expected =
                    if (invocation.isKsp) {
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
        val javaSrc =
            Source.java(
                "JavaSubject",
                """
            /**
             * javadocs
             */
            public class JavaSubject {}
            """
                    .trimIndent()
            )
        val kotlinSrc =
            Source.kotlin(
                "KotlinSubject.kt",
                """
            /**
             * kdocs
             */
            class KotlinSubject
            """
                    .trimIndent()
            )
        runProcessorTest(sources = listOf(javaSrc, kotlinSrc)) { invocation ->
            assertThat(
                    invocation.processingEnv.requireTypeElement("JavaSubject").docComment?.trim()
                )
                .isEqualTo("javadocs")
            assertThat(
                    invocation.processingEnv.requireTypeElement("KotlinSubject").docComment?.trim()
                )
                .isEqualTo("kdocs")
        }
    }

    @Test
    fun enclosingElement() {
        runProcessorTestHelper(listOf(enclosingElementJavaSource, enclosingElementKotlinSource)) {
            invocation,
            _ ->
            invocation.processingEnv.getTypeElementsFromPackage("foo.bar").forEach { typeElement ->
                assertThat(typeElement.enclosingElement).isNull()

                typeElement.getEnclosedTypeElements().forEach { elem ->
                    elem.getEnclosedElements().forEach { nestedElem ->
                        assertThat(nestedElem.enclosingElement).isEqualTo(elem)
                    }

                    assertThat(elem.enclosingElement).isEqualTo(typeElement)
                }
                if (typeElement is XEnumTypeElement) {
                    typeElement.entries.forEach { entry ->
                        assertThat(entry.enclosingElement).isEqualTo(typeElement)
                    }
                }
                typeElement.getDeclaredFields().forEach { field ->
                    assertThat(field.enclosingElement).isEqualTo(typeElement)
                }
                typeElement.getDeclaredMethods().forEach { method ->
                    assertThat(method.enclosingElement).isEqualTo(typeElement)
                    method.parameters.forEach { p ->
                        assertThat(p.enclosingElement).isEqualTo(method)
                    }
                }
                typeElement.getConstructors().forEach { ctor ->
                    assertThat(ctor.enclosingElement).isEqualTo(typeElement)
                    ctor.parameters.forEach { p -> assertThat(p.enclosingElement).isEqualTo(ctor) }
                }
            }
        }
    }

    // TODO(b/218310483): make structure consistent between KSP and Javac, as well as source and
    //  classpath.
    @Test
    fun enclosingElementKotlinCompanion() {
        runProcessorTestHelper(
            listOf(
                Source.kotlin(
                    "Test.kt",
                    """
            package foo.bar
            class KotlinClass(val property: String) {
                companion object {
                    val companionObjectProperty: String = "hello"
                    @JvmStatic
                    val companionObjectPropertyJvmStatic: String = "hello"
                    @JvmField val companionObjectPropertyJvmField: String = "hello"
                    lateinit var companionObjectPropertyLateinit: String
                    const val companionObjectPropertyConst: String = "hello"
                    fun companionObjectFunction(companionFunctionParam: String) {}
                    @JvmStatic
                    fun companionObjectFunctionJvmStatic(companionFunctionParam: String) {}
                }
            }
            """
                        .trimIndent()
                )
            ),
            kotlincArgs = KOTLINC_LANGUAGE_1_9_ARGS
        ) { invocation, precompiled ->
            val enclosingElement =
                invocation.processingEnv.requireTypeElement("foo.bar.KotlinClass")
            val companionObj =
                enclosingElement.getEnclosedTypeElements().first { it.isCompanionObject() }

            enclosingElement.getDeclaredFields().let { fields ->
                if (invocation.isKsp) {
                    assertThat(fields.map { it.name })
                        .containsExactly(
                            "property",
                            "companionObjectProperty",
                            "companionObjectPropertyJvmStatic",
                            "companionObjectPropertyJvmField",
                            "companionObjectPropertyLateinit",
                            "companionObjectPropertyConst"
                        )
                    fields.forEach {
                        if (it.name.startsWith("companion")) {
                            assertThat(it.enclosingElement).isEqualTo(companionObj)
                        } else {
                            assertThat(it.enclosingElement).isEqualTo(enclosingElement)
                        }
                    }
                } else {
                    assertThat(fields.map { it.name })
                        .containsExactly(
                            "Companion",
                            "property",
                            "companionObjectProperty",
                            "companionObjectPropertyJvmStatic",
                            "companionObjectPropertyJvmField",
                            "companionObjectPropertyLateinit",
                            "companionObjectPropertyConst"
                        )
                    fields.forEach { assertThat(it.enclosingElement).isEqualTo(enclosingElement) }
                }
            }

            enclosingElement.getDeclaredMethods().let { methods ->
                assertThat(methods.map { it.name })
                    .containsExactly(
                        "getProperty",
                        "getCompanionObjectPropertyJvmStatic",
                        "companionObjectFunctionJvmStatic"
                    )
                methods.forEach { assertThat(it.enclosingElement).isEqualTo(enclosingElement) }
            }

            companionObj.getDeclaredFields().let { fields ->
                if (invocation.isKsp) {
                    assertThat(fields.map { it.name })
                        .containsExactly(
                            "companionObjectProperty",
                            "companionObjectPropertyJvmStatic",
                            "companionObjectPropertyJvmField",
                            "companionObjectPropertyLateinit",
                            "companionObjectPropertyConst"
                        )
                    fields.forEach { assertThat(it.enclosingElement).isEqualTo(companionObj) }
                } else {
                    assertThat(companionObj.getDeclaredFields()).isEmpty()
                }
            }

            companionObj.getDeclaredMethods().let { methods ->
                methods.forEach { assertThat(it.enclosingElement).isEqualTo(companionObj) }
                if (invocation.isKsp || precompiled) {
                    assertThat(methods.map { it.name })
                        .containsExactly(
                            "getCompanionObjectProperty",
                            "getCompanionObjectPropertyJvmStatic",
                            "getCompanionObjectPropertyLateinit",
                            "setCompanionObjectPropertyLateinit",
                            "companionObjectFunction",
                            "companionObjectFunctionJvmStatic"
                        )
                        .inOrder()
                } else {
                    // TODO(b/290800523): Remove the synthetic annotations method from the list
                    //  of declared methods so that KAPT matches KSP.
                    assertThat(methods.map { it.name })
                        .containsExactly(
                            "getCompanionObjectProperty",
                            "getCompanionObjectPropertyJvmStatic",
                            "getCompanionObjectPropertyJvmStatic\$annotations",
                            "getCompanionObjectPropertyLateinit",
                            "setCompanionObjectPropertyLateinit",
                            "companionObjectFunction",
                            "companionObjectFunctionJvmStatic"
                        )
                        .inOrder()
                }
            }
        }
    }

    @OptIn(KspExperimental::class)
    @Test
    fun enclosingElementKotlinTopLevel() {
        runProcessorTestHelper(listOf(enclosingElementKotlinSourceTopLevel)) { inv, precompiled ->
            if (inv.isKsp) {
                getTopLevelFunctionOrPropertyElements(inv, "foo.bar").forEach { elem ->
                    assertThat(elem.enclosingElement).isFileContainer(precompiled)
                }
            } else {
                inv.processingEnv.getTypeElementsFromPackage("foo.bar").forEach { typeElement ->
                    assertThat(typeElement).isInstanceOf<JavacTypeElement>()
                    assertThat(typeElement.enclosingElement).isNull()

                    typeElement.getEnclosedElements().forEach { elem ->
                        assertThat(elem.enclosingElement).isEqualTo(typeElement)
                        if (elem is XExecutableElement) {
                            elem.parameters.forEach { p ->
                                assertThat(p.enclosingElement).isEqualTo(elem)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun enclosingTypeElement() {
        runProcessorTestHelper(listOf(enclosingElementJavaSource, enclosingElementKotlinSource)) {
            invocation,
            _ ->
            invocation.processingEnv.getTypeElementsFromPackage("foo.bar").forEach { typeElement ->
                assertThat(typeElement.enclosingTypeElement).isNull()

                typeElement.getEnclosedTypeElements().forEach { elem ->
                    assertThat(elem.enclosingTypeElement).isNotEqualTo(elem)
                    assertThat(elem.enclosingTypeElement).isEqualTo(typeElement)
                }
            }
        }
    }

    @Test
    fun closestMemberContainer() {
        runProcessorTestHelper(listOf(enclosingElementJavaSource, enclosingElementKotlinSource)) {
            invocation,
            _ ->
            invocation.processingEnv.getTypeElementsFromPackage("foo.bar").forEach { typeElement ->
                assertThat(typeElement.closestMemberContainer).isEqualTo(typeElement)

                val companionObj =
                    typeElement.getEnclosedTypeElements().firstOrNull { it.isCompanionObject() }

                typeElement.getEnclosedElements().forEach { elem ->
                    if (elem is XTypeElement) {
                        elem.getEnclosedElements().forEach { nestedElem ->
                            assertThat(nestedElem.closestMemberContainer).isEqualTo(elem)
                        }
                    } else {
                        if (elem.enclosingElement == companionObj) {
                            assertThat(elem.closestMemberContainer).isEqualTo(companionObj)
                        } else {
                            assertThat(elem.closestMemberContainer).isEqualTo(typeElement)
                        }

                        if (elem is XExecutableElement) {
                            elem.parameters.forEach { p ->
                                assertThat(p.closestMemberContainer).isEqualTo(typeElement)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun closestMemberContainerTopLevel() {
        runProcessorTestHelper(listOf(enclosingElementKotlinSourceTopLevel)) {
            invocation,
            precompiled ->
            if (invocation.isKsp) {
                getTopLevelFunctionOrPropertyElements(invocation, "foo.bar").forEach { elem ->
                    assertThat(elem.closestMemberContainer).isFileContainer(precompiled)
                    if (elem is XExecutableElement) {
                        elem.parameters.forEach { p ->
                            assertThat(p.closestMemberContainer).isFileContainer(precompiled)
                        }
                    }
                }
            } else {
                invocation.processingEnv.getTypeElementsFromPackage("foo.bar").forEach { typeElement
                    ->
                    typeElement.getDeclaredMethods().forEach { method ->
                        assertThat(method.closestMemberContainer).isEqualTo(typeElement)

                        method.parameters.forEach { p ->
                            assertThat(p.closestMemberContainer).isEqualTo(typeElement)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun isFromJavaOrKotlin() {
        val javaSource =
            Source.java(
                "foo.bar.Foo",
                """
            package foo.bar;
            class Foo {
                void f(String a) {}
                static class Nested {}
            }
            """
                    .trimIndent()
            )
        val kotlinSource =
            Source.kotlin(
                "Bar.kt",
                """
            package foo.bar
            fun tlf(a: String) = "hello"
            class Bar {
                var p: String = "hello"
                fun f(a: String) {}
                suspend fun sf(a: String) {}
                class Nested
            }
            fun Bar.ef(a: String) {}
            """
                    .trimIndent()
            )
        runProcessorTestHelper(listOf(javaSource, kotlinSource)) { invocation, _ ->
            // Java
            invocation.processingEnv.requireTypeElement("foo.bar.Foo").let {
                assertThat(it.closestMemberContainer.isFromJava()).isTrue()
                assertThat(it.closestMemberContainer.isFromKotlin()).isFalse()
                it.getMethodByJvmName("f").let {
                    assertThat(it.closestMemberContainer.isFromJava()).isTrue()
                    assertThat(it.closestMemberContainer.isFromKotlin()).isFalse()
                }
            }
            invocation.processingEnv.requireTypeElement("foo.bar.Foo.Nested").let {
                assertThat(it.closestMemberContainer.isFromJava()).isTrue()
                assertThat(it.closestMemberContainer.isFromKotlin()).isFalse()
            }

            // Kotlin
            invocation.processingEnv.requireTypeElement("foo.bar.Bar").let {
                assertThat(it.isFromJava()).isFalse()
                assertThat(it.isFromKotlin()).isTrue()
                it.getDeclaredField("p").let {
                    assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                    assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
                    it.setter!!.let {
                        assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                        assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
                    }
                    it.getter!!.let {
                        assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                        assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
                    }
                }
                it.getMethodByJvmName("f").let {
                    assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                    assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()

                    it.parameters.single().let {
                        assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                        assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
                    }
                }
                it.getMethodByJvmName("sf").let {
                    assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                    assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()

                    it.parameters.forEach {
                        assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                        assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
                    }
                }
            }
            invocation.processingEnv.requireTypeElement("foo.bar.Bar.Nested").let {
                assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
            }

            // Kotlin top-level elements
            if (invocation.isKsp) {
                val topLevelElements = invocation.processingEnv.getElementsFromPackage("foo.bar")
                topLevelElements
                    .single { it.name == "tlf" }
                    .let {
                        assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                        assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
                    }
                topLevelElements
                    .single { it.name == "ef" }
                    .let {
                        assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                        assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
                        (it as XMethodElement).parameters.forEach {
                            assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                            assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
                        }
                    }
            } else {
                invocation.processingEnv.requireTypeElement("foo.bar.BarKt").let {
                    assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                    assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()

                    it.getMethodByJvmName("tlf").let {
                        assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                        assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()

                        it.parameters.single().let {
                            assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                            assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
                        }
                    }

                    it.getMethodByJvmName("ef").let {
                        assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                        assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()

                        it.parameters.forEach {
                            assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                            assertThat(it.closestMemberContainer.isFromKotlin()).isTrue()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun isFromJavaOrKotlinErrorTypes() {
        val javaSource =
            Source.java(
                "foo.bar.Foo",
                """
            package foo.bar;
            class Foo {
                DoNotExist ep;
            }
            """
                    .trimIndent()
            )
        val kotlinSource =
            Source.kotlin(
                "Bar.kt",
                """
            package foo.bar
            class Bar {
                val ep: DoNotExist = TODO()
            }
            """
                    .trimIndent()
            )
        // Can't use runProcessorTestHelper() as we can't compile the files referencing an
        // error type
        runProcessorTest(listOf(javaSource, kotlinSource)) { invocation ->
            // Java
            invocation.processingEnv.requireTypeElement("foo.bar.Foo").let {
                it.getField("ep").type.typeElement!!.let {
                    assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                    assertThat(it.closestMemberContainer.isFromKotlin()).isFalse()
                }
            }

            // Kotlin
            invocation.processingEnv.requireTypeElement("foo.bar.Bar").let {
                it.getField("ep").type.typeElement!!.let {
                    if (invocation.isKsp) {
                        assertThat(it.closestMemberContainer.isFromJava()).isFalse()
                        assertThat(it.closestMemberContainer.isFromKotlin()).isFalse()
                    }
                }
            }
            invocation.assertCompilationResult { compilationDidFail() }
        }
    }

    private val enclosingElementJavaSource =
        Source.java(
            "foo.bar.Test",
            """
        package foo.bar;
        enum JavaEnum {
            A(3), B(5), C(7);
            JavaEnum(int i) { javaEnumField = i; }
            private int javaEnumField;
            int javaEnumMethod() {
               return javaEnumField;
            }
            static void javaStaticEnumMethod() {}
        }
        interface JavaInterface {
            void baseMethod(String i);
        }
        class JavaBase implements JavaInterface {
            String baseField;
            static String staticField;
            JavaBase(String s) {}
            @Override public void baseMethod(String s) {}
            static public void staticMethod(String s) {}
            static class JavaNestedStatic {
                String nestedStaticClassField;
                public void nestedStaticClassMethod(String s) {}
            }
            class JavaNestedNonStatic {
                String nestedClassField;
                public void nestedClassMethod(String s) {}
            }
        }
        class JavaDerived extends JavaBase {
            String derivedField;
            JavaDerived(String t) {
                super(t);
            }
            void derivedMethod(String t) {}
            @Override
            public void baseMethod(String t) {}
        }
        """
                .trimIndent()
        )

    private val enclosingElementKotlinSource =
        Source.kotlin(
            "Test.kt",
            """
        package foo.bar
        enum class KotlinEnum(val enumProperty: Int) {
            A(3), B(5), C(7);
            fun enumFunction() = enumProperty + 1
        }
        open class KotlinBaseClass(val baseProperty: String) {
            object NestedObject {
                val nestedObjectProperty: String = "hello"
                fun nestedObjectFunction(nestedObjectFunctionParam: String) {}
            }
            class KotlinNestedClass(val nestedProperty: String) {
                val nestedClassProperty: String = "hello"
                fun nestedClassFunction(nestedClassFunctionParam: String) {}
            }
            fun baseFunction(baseFunctionParam: String) {}
        }
        class KotlinDerivedClass(val derivedProperty: String):
                KotlinBaseClass(derivedProperty) {
            fun derivedFunction(derivedFunctionParam: String) {}
        }
        data class KotlinDataClass(val property: String)
        object KotlinObject {
            val objectProperty: String = "hello"
            fun objectFunction(objectFunctionParam: String) {}
        }
        """
                .trimIndent()
        )

    private val enclosingElementKotlinSourceTopLevel =
        Source.kotlin(
            "Test.kt",
            """
        package foo.bar
        val topLevelProperty: String = "hello"
        fun topLevelFunction(p: String) {}
        """
                .trimIndent()
        )

    @OptIn(KspExperimental::class)
    private fun getTopLevelFunctionOrPropertyElements(
        inv: XTestInvocation,
        pkg: String
    ): Sequence<XElement> =
        inv.kspResolver
            .getDeclarationsFromPackage(pkg)
            .map { declaration ->
                when (declaration) {
                    is KSFunctionDeclaration ->
                        KspExecutableElement.create(inv.kspProcessingEnv, declaration)
                    is KSPropertyDeclaration ->
                        KspFieldElement.create(inv.kspProcessingEnv, declaration)
                    else -> null
                }
            }
            .filterNotNull()

    private fun Subject<XElement>.isFileContainer(precompiled: Boolean) {
        if (precompiled) {
            isInstanceOf<KspSyntheticFileMemberContainer>()
        } else {
            isInstanceOf<KspFileMemberContainer>()
        }
    }

    private fun runProcessorTestHelper(
        sources: List<Source>,
        kotlincArgs: List<String> = emptyList(),
        handler: (XTestInvocation, Boolean) -> Unit
    ) {
        runProcessorTest(sources = sources, kotlincArguments = kotlincArgs) { handler(it, false) }
        runProcessorTest(classpath = compileFiles(sources), kotlincArguments = kotlincArgs) {
            handler(it, true)
        }
    }
}
