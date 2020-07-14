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

import box
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import simpleRun
import javax.lang.model.element.ExecutableElement

@RunWith(JUnit4::class)
class ElementExtTest {
    @Test
    fun methodsInClass() {
        val parentCode = JavaFileObjects.forSourceLines(
            "foo.bar.Parent", """
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
        val childCode = JavaFileObjects.forSourceLines(
            "foo.bar.Child", """
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
        simpleRun(
            jfos = *arrayOf(parentCode, childCode)
        ) {
            val parent = it.processingEnv.requireTypeElement("foo.bar.Parent")
            val child = it.processingEnv.requireTypeElement("foo.bar.Child")
            val objectMethods = it.processingEnv.requireTypeElement("java.lang.Object")
                .getAllMethods(it.processingEnv).map {
                    it.name
                } - "registerNatives"
            val parentMethods = listOf(
                "parentPrivate", "parentPublic", "parentStaticPrivate",
                "parentStaticPublic", "overridden"
            )
            val childMethods = listOf(
                "childPrivate", "childPublic", "childStaticPrivate",
                "childStaticPublic", "overridden"
            )
            assertThat(parent.getDeclaredMethods()).containsExactlyElementsIn(parentMethods)
            assertThat(parent.getAllMethods(it.processingEnv))
                .containsExactlyElementsIn(parentMethods + objectMethods)
            assertThat(parent.getAllNonPrivateInstanceMethods(it.processingEnv))
                .containsExactlyElementsIn(
                    parentMethods + objectMethods -
                            listOf("parentPrivate", "parentStaticPrivate", "parentStaticPublic")
                )

            assertThat(child.getDeclaredMethods()).containsExactlyElementsIn(
                childMethods
            )
            assertThat(child.getAllMethods(it.processingEnv)).containsExactlyElementsIn(
                childMethods + parentMethods + objectMethods -
                        listOf("parentPrivate", "parentStaticPrivate", "overridden") +
                        "overridden" // add 1 overridden back
            )
            assertThat(child.getAllNonPrivateInstanceMethods(it.processingEnv))
                .containsExactlyElementsIn(
                    childMethods + parentMethods + objectMethods -
                            listOf(
                                "parentPrivate", "parentStaticPrivate", "parentStaticPublic",
                                "childPrivate", "childStaticPrivate", "childStaticPublic",
                                "overridden"
                            ) + "overridden" // add 1 overridden back
                )

            assertThat(child.getConstructors()).containsExactly("<init>")
            assertThat(parent.getConstructors()).containsExactly("<init>")
        }.compilesWithoutError()
    }

    @Test
    fun methodsInInterface() {
        val parentCode = JavaFileObjects.forSourceLines(
            "foo.bar.Parent", """
            package foo.bar;

            public interface Parent {
                public void parentPublic();
                public void overridden();
                private static void parentStaticPrivate() {}
                public static void parentStaticPublic() {}
            }
        """.trimIndent()
        )
        val childCode = JavaFileObjects.forSourceLines(
            "foo.bar.Child", """
            package foo.bar;

            public interface Child extends Parent {
                public void childPublic();
                public void overridden();
                private static void childStaticPrivate() {}
                public static void childStaticPublic() {}
            }
        """.trimIndent()
        )
        simpleRun(
            jfos = *arrayOf(parentCode, childCode)
        ) {
            val parent = it.processingEnv.requireTypeElement("foo.bar.Parent")
            val child = it.processingEnv.requireTypeElement("foo.bar.Child")
            val objectMethods = it.processingEnv.requireTypeElement("java.lang.Object")
                .getAllMethods(it.processingEnv).map {
                    it.name
                } - listOf("registerNatives", "clone", "finalize")
            val parentMethods = listOf(
                "parentPublic", "parentStaticPrivate", "parentStaticPublic", "overridden"
            )
            val childMethods = listOf(
                "childPublic", "childStaticPrivate", "childStaticPublic", "overridden"
            )
            assertThat(parent.getDeclaredMethods())
                .containsExactlyElementsIn(parentMethods)
            assertThat(parent.getAllMethods(it.processingEnv))
                .containsExactlyElementsIn(parentMethods + objectMethods)
            assertThat(parent.getAllNonPrivateInstanceMethods(it.processingEnv))
                .containsExactly("parentPublic", "overridden")

            assertThat(child.getDeclaredMethods())
                .containsExactlyElementsIn(childMethods)
            assertThat(child.getAllMethods(it.processingEnv)).containsExactlyElementsIn(
                childMethods + parentMethods + objectMethods -
                        listOf("parentStaticPrivate", "parentStaticPublic", "overridden") +
                        "overridden" // add 1 overridden back
            )
            assertThat(child.getAllNonPrivateInstanceMethods(it.processingEnv))
                .containsExactly(
                    "childPublic", "parentPublic", "overridden"
                )

            assertThat(child.getConstructors()).isEmpty()
            assertThat(parent.getConstructors()).isEmpty()
        }.compilesWithoutError()
    }

    @Test
    fun types() {
        val testCode = JavaFileObjects.forSourceLines(
            "foo.bar.Baz", """
            package foo.bar;

            public class Baz {
                public int field;
                public int method() {
                    return 3;
                }
            }
        """.trimIndent()
        )
        simpleRun(
            jfos = *arrayOf(testCode)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            val field = element.getAllFieldsIncludingPrivateSupers(it.processingEnv)
                .first {
                    it.name == "field"
                }
            val method = element.getDeclaredMethods()
                .first {
                    it.name == "method"
                }
            assertThat(field.type.typeName()).isEqualTo(TypeName.INT)
            assertThat(method.returnType.typeName()).isEqualTo(TypeName.INT)
            assertThat(element.type.typeName()).isEqualTo(ClassName.get("foo.bar", "Baz"))
        }.compilesWithoutError()
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
        simpleRun { invocation ->
            val processingEnv = invocation.processingEnv
            primitiveTypeNames.forEach { primitiveTypeName ->
                val typeMirror = processingEnv.requireTypeMirror(primitiveTypeName)
                assertThat(typeMirror.kind.isPrimitive).isTrue()
                assertThat(typeMirror.typeName()).isEqualTo(primitiveTypeName)
                assertThat(
                    typeMirror.box(invocation.typeUtils).typeName()
                ).isEqualTo(primitiveTypeName.box())
            }
        }.compilesWithoutError()
    }

    private fun assertThat(executables: Iterable<ExecutableElement>) = assertThat(
        executables.map { it.name }
    )
}