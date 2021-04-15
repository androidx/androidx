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

import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ElementExtTest(
    private val preCompile: Boolean
) {
    @Test
    fun methodsInClass() {
        val parentCode = Source.java(
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
            """.trimIndent()
        )
        val childCode = Source.java(
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
            """.trimIndent()
        )

        runTest(
            sources = listOf(parentCode, childCode)
        ) {
            val parent = it.processingEnv.requireTypeElement("foo.bar.Parent")
            val child = it.processingEnv.requireTypeElement("foo.bar.Child")
            val objectMethodNames = it.objectMethodNames()
            val parentMethods = listOf(
                "parentPrivate", "parentPublic", "parentStaticPrivate",
                "parentStaticPublic", "overridden"
            )
            val childMethods = listOf(
                "childPrivate", "childPublic", "childStaticPrivate",
                "childStaticPublic", "overridden"
            )
            assertThat(parent.getDeclaredMethods().names())
                .containsExactlyElementsIn(parentMethods)
            assertThat(parent.getAllMethods().names())
                .containsExactlyElementsIn(parentMethods + objectMethodNames)
            val shouldNotExistInChild =
                listOf("parentPrivate", "parentStaticPrivate", "parentStaticPublic")

            assertThat(parent.getAllNonPrivateInstanceMethods().names())
                .containsExactlyElementsIn(
                    parentMethods + objectMethodNames - shouldNotExistInChild
                )

            assertThat(child.getDeclaredMethods().names())
                .containsExactlyElementsIn(childMethods)
            assertThat(child.getAllMethods().names())
                .containsExactlyElementsIn(
                    childMethods + parentMethods + objectMethodNames -
                        listOf("parentPrivate", "parentStaticPrivate", "overridden") +
                        "overridden" // add 1 overridden back
                )
            assertThat(child.getAllNonPrivateInstanceMethods().names())
                .containsExactlyElementsIn(
                    childMethods + parentMethods + objectMethodNames - shouldNotExistInChild -
                        listOf(
                            "childPrivate", "childStaticPrivate", "childStaticPublic",
                            "overridden"
                        ) + "overridden" // add 1 overridden back
                )

            assertThat(child.getConstructors()).hasSize(1)
            assertThat(parent.getConstructors()).hasSize(1)
        }
    }

    @Test
    fun methodsInInterface() {
        val parentCode = Source.java(
            "foo.bar.Parent",
            """
            package foo.bar;

            public interface Parent {
                public void parentPublic();
                public void overridden();
                private static void parentStaticPrivate() {}
                public static void parentStaticPublic() {}
            }
            """.trimIndent()
        )
        val childCode = Source.java(
            "foo.bar.Child",
            """
            package foo.bar;

            public interface Child extends Parent {
                public void childPublic();
                public void overridden();
                private static void childStaticPrivate() {}
                public static void childStaticPublic() {}
            }
            """.trimIndent()
        )

        runTest(
            sources = listOf(parentCode, childCode)
        ) {
            // NOTE: technically, an interface should show all methods it receives from Object
            //  In practice, we never need it and would require additional code to implement hence
            //  we don't include object methods in interfaces.
            val parent = it.processingEnv.requireTypeElement("foo.bar.Parent")
            val child = it.processingEnv.requireTypeElement("foo.bar.Child")
            val parentMethods = listOf(
                "parentPublic", "parentStaticPrivate", "parentStaticPublic", "overridden"
            )
            val childMethods = listOf(
                "childPublic", "childStaticPrivate", "childStaticPublic", "overridden"
            )
            val objectMethodNames = it.objectMethodNames()
            assertThat(parent.getDeclaredMethods().names())
                .containsExactlyElementsIn(parentMethods)
            assertThat(parent.getAllMethods().names() - objectMethodNames)
                .containsExactlyElementsIn(parentMethods)
            assertThat(parent.getAllNonPrivateInstanceMethods().names() - objectMethodNames)
                .containsExactly("parentPublic", "overridden")

            assertThat(child.getDeclaredMethods().names())
                .containsExactlyElementsIn(childMethods)
            assertThat(child.getAllMethods().names() - objectMethodNames)
                .containsExactlyElementsIn(
                    childMethods + parentMethods -
                        listOf("parentStaticPrivate", "parentStaticPublic", "overridden") +
                        "overridden" // add 1 overridden back
                )
            assertThat(child.getAllNonPrivateInstanceMethods().names() - objectMethodNames)
                .containsExactly(
                    "childPublic", "parentPublic", "overridden"
                )

            assertThat(child.getConstructors()).isEmpty()
            assertThat(parent.getConstructors()).isEmpty()
        }
    }

    @Test
    fun types() {
        val testCode = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;

            public class Baz {
                public int field;
                public int method() {
                    return 3;
                }
            }
            """.trimIndent()
        )

        runTest(
            sources = listOf(testCode)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            val field = element.getAllFieldsIncludingPrivateSupers()
                .first {
                    it.name == "field"
                }
            val method = element.getDeclaredMethods()
                .first {
                    it.name == "method"
                }
            assertThat(field.type.typeName).isEqualTo(TypeName.INT)
            assertThat(method.returnType.typeName).isEqualTo(TypeName.INT)
            assertThat(element.type.typeName).isEqualTo(ClassName.get("foo.bar", "Baz"))
        }
    }

    @Test
    fun primitiveTypes() {
        // check that we can also find primitive types from the common API
        val primitiveTypeNames = listOf(
            TypeName.BOOLEAN,
            TypeName.BYTE,
            TypeName.SHORT,
            TypeName.INT,
            TypeName.LONG,
            TypeName.CHAR,
            TypeName.FLOAT,
            TypeName.DOUBLE
        )
        runTest { invocation ->
            val processingEnv = invocation.processingEnv
            primitiveTypeNames.forEach { primitiveTypeName ->
                val typeMirror = processingEnv.requireType(primitiveTypeName)
                assertThat(typeMirror.typeName).isEqualTo(primitiveTypeName)
                assertThat(
                    typeMirror.boxed().typeName
                ).isEqualTo(primitiveTypeName.box())
            }
        }
    }

    @Suppress("NAME_SHADOWING") // intentional
    private fun runTest(
        sources: List<Source> = emptyList(),
        handler: (XTestInvocation) -> Unit
    ) {
        val (sources, classpath) = if (preCompile && sources.isNotEmpty()) {
            emptyList<Source>() to listOf(compileFiles(sources))
        } else {
            sources to emptyList()
        }
        runProcessorTest(
            sources = sources,
            classpath = classpath,
            handler = handler
        )
    }

    private fun XTestInvocation.objectMethodNames(): List<String> {
        return processingEnv.requireTypeElement("java.lang.Object")
            .getAllMethods().map {
                it.name
            } - "registerNatives"
    }

    private fun List<XMethodElement>.names() = map { it.name }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "preCompile_{0}")
        fun params() = arrayOf(false, true)
    }
}