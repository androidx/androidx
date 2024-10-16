/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.ext

import androidx.kruth.assertThat
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XProcessingEnvConfig
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.runProcessorTestWithK1
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ElementExtTest(private val preCompile: Boolean) {
    @Test
    fun methodsInClass() {
        val parentCode =
            Source.java(
                "foo.bar.Parent",
                """
            package foo.bar;

            public class Parent {
                public Parent() {}
                private void parentPrivate() {}
                public void parentPublic() {}
                public void overridden() {}
                private static void parentStaticPrivate() {}
                public static void parentStaticPublic() {}
            }
            """
                    .trimIndent()
            )
        val childCode =
            Source.java(
                "foo.bar.Child",
                """
            package foo.bar;

            public class Child extends Parent {
                public Child() {}
                private void childPrivate() {}
                public void childPublic() {}
                public void overridden() {}
                private static void childStaticPrivate() {}
                public static void childStaticPublic() {}
            }
            """
                    .trimIndent()
            )

        runTest(sources = listOf(parentCode, childCode)) {
            val parent = it.processingEnv.requireTypeElement("foo.bar.Parent")
            val child = it.processingEnv.requireTypeElement("foo.bar.Child")
            val objectMethodNames = it.objectMethodNames()
            val parentMethods =
                listOf(
                    "parentPrivate",
                    "parentPublic",
                    "parentStaticPrivate",
                    "parentStaticPublic",
                    "overridden"
                )
            val childMethods =
                listOf(
                    "childPrivate",
                    "childPublic",
                    "childStaticPrivate",
                    "childStaticPublic",
                    "overridden"
                )
            assertThat(parent.getDeclaredMethods().names()).containsExactlyElementsIn(parentMethods)
            assertThat(parent.getAllMethods().names())
                .containsExactlyElementsIn(parentMethods + objectMethodNames)
            val shouldNotExistInChild =
                listOf("parentPrivate", "parentStaticPrivate", "parentStaticPublic")

            assertThat(parent.getAllNonPrivateInstanceMethods().names())
                .containsExactlyElementsIn(
                    parentMethods + objectMethodNames - shouldNotExistInChild
                )

            assertThat(child.getDeclaredMethods().names()).containsExactlyElementsIn(childMethods)
            assertThat(child.getAllMethods().names())
                .containsExactlyElementsIn(
                    childMethods + parentMethods + objectMethodNames -
                        listOf("parentPrivate", "parentStaticPrivate", "overridden") +
                        "overridden" // add 1 overridden back
                )
            assertThat(child.getAllNonPrivateInstanceMethods().names())
                .containsExactlyElementsIn(
                    childMethods + parentMethods + objectMethodNames -
                        shouldNotExistInChild -
                        listOf(
                            "childPrivate",
                            "childStaticPrivate",
                            "childStaticPublic",
                            "overridden"
                        ) + "overridden" // add 1 overridden back
                )

            assertThat(child.getConstructors()).hasSize(1)
            assertThat(parent.getConstructors()).hasSize(1)
        }
    }

    @Test
    fun methodsInInterface() {
        val parentCode =
            Source.java(
                "foo.bar.Parent",
                """
            package foo.bar;

            public interface Parent {
                public void parentPublic();
                public void overridden();
                private static void parentStaticPrivate() {}
                public static void parentStaticPublic() {}
            }
            """
                    .trimIndent()
            )
        val childCode =
            Source.java(
                "foo.bar.Child",
                """
            package foo.bar;

            public interface Child extends Parent {
                public void childPublic();
                public void overridden();
                private static void childStaticPrivate() {}
                public static void childStaticPublic() {}
            }
            """
                    .trimIndent()
            )

        runTest(sources = listOf(parentCode, childCode)) {
            // NOTE: technically, an interface should show all methods it receives from Object
            //  In practice, we never need it and would require additional code to implement hence
            //  we don't include object methods in interfaces.
            val parent = it.processingEnv.requireTypeElement("foo.bar.Parent")
            val child = it.processingEnv.requireTypeElement("foo.bar.Child")
            val parentMethods =
                listOf("parentPublic", "parentStaticPrivate", "parentStaticPublic", "overridden")
            val childMethods =
                listOf("childPublic", "childStaticPrivate", "childStaticPublic", "overridden")
            val objectMethodNames = it.objectMethodNames()
            assertThat(parent.getDeclaredMethods().names()).containsExactlyElementsIn(parentMethods)
            assertThat(parent.getAllMethods().names() - objectMethodNames)
                .containsExactlyElementsIn(parentMethods)
            assertThat(parent.getAllNonPrivateInstanceMethods().names() - objectMethodNames)
                .containsExactly("parentPublic", "overridden")

            assertThat(child.getDeclaredMethods().names()).containsExactlyElementsIn(childMethods)
            assertThat(child.getAllMethods().names() - objectMethodNames)
                .containsExactlyElementsIn(
                    childMethods + parentMethods -
                        listOf("parentStaticPrivate", "parentStaticPublic", "overridden") +
                        "overridden" // add 1 overridden back
                )
            assertThat(child.getAllNonPrivateInstanceMethods().names() - objectMethodNames)
                .containsExactly("childPublic", "parentPublic", "overridden")

            assertThat(child.getConstructors()).isEmpty()
            assertThat(parent.getConstructors()).isEmpty()
        }
    }

    @Test
    fun types() {
        val testCode =
            Source.java(
                "foo.bar.Baz",
                """
            package foo.bar;

            public class Baz {
                public int field;
                public int method() {
                    return 3;
                }
            }
            """
                    .trimIndent()
            )

        runTest(sources = listOf(testCode)) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            val field = element.getAllFieldsIncludingPrivateSupers().first { it.name == "field" }
            val method = element.getDeclaredMethods().first { it.jvmName == "method" }
            assertThat(field.type.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_INT)
            assertThat(method.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_INT)
            assertThat(element.type.asTypeName()).isEqualTo(XClassName.get("foo.bar", "Baz"))
        }
    }

    @Test
    fun valueClassUnderlyingProperty() {
        val src =
            Source.kotlin(
                "Subject.kt",
                """
            package foo
            class Subject {
              fun uLongFunction(): ULong = TODO()
              fun durationFunction(): kotlin.time.Duration = TODO()
            }
            """
                    .trimIndent()
            )
        runKspTest(
            sources = listOf(src),
            config =
                XProcessingEnvConfig.DEFAULT.copy(excludeMethodsWithInvalidJvmSourceNames = false)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("foo.Subject")
            subject
                .getDeclaredMethods()
                .first { it.name == "uLongFunction" }
                .let { uLongFunction ->
                    val returnType = uLongFunction.returnType
                    val info = checkNotNull(returnType.typeElement).getValueClassUnderlyingInfo()
                    assertThat(info.parameter.name).isEqualTo("data")
                    assertThat(info.getter!!.propertyName).isEqualTo("data")
                    assertThat(info.parameter.type)
                        .isEqualTo(invocation.processingEnv.requireType(XTypeName.PRIMITIVE_LONG))
                    assertThat(info.getter!!.returnType)
                        .isEqualTo(invocation.processingEnv.requireType(XTypeName.PRIMITIVE_LONG))
                }
            subject
                .getDeclaredMethods()
                .first { it.name == "durationFunction" }
                .let { durationFunction ->
                    val returnType = durationFunction.returnType
                    val info = checkNotNull(returnType.typeElement).getValueClassUnderlyingInfo()
                    assertThat(info.parameter.name).isEqualTo("rawValue")
                    assertThat(info.getter).isNull()
                    assertThat(info.parameter.type)
                        .isEqualTo(invocation.processingEnv.requireType(XTypeName.PRIMITIVE_LONG))
                }
        }
    }

    @Suppress("NAME_SHADOWING") // intentional
    private fun runTest(sources: List<Source> = emptyList(), handler: (XTestInvocation) -> Unit) {
        val (sources, classpath) =
            if (preCompile && sources.isNotEmpty()) {
                emptyList<Source>() to compileFiles(sources)
            } else {
                sources to emptyList()
            }
        runProcessorTestWithK1(sources = sources, classpath = classpath, handler = handler)
    }

    private fun XTestInvocation.objectMethodNames(): List<String> {
        return processingEnv
            .requireTypeElement("java.lang.Object")
            .getAllMethods()
            .filterNot { it.isPrivate() }
            .map { it.jvmName }
            .toList()
    }

    private fun List<XMethodElement>.names() = map { it.jvmName }

    private fun Sequence<XMethodElement>.names() = map { it.jvmName }.toList()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "preCompile_{0}")
        fun params() = arrayOf(false, true)
    }
}
