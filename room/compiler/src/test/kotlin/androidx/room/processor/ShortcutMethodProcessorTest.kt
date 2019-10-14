/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.processor

import COMMON
import androidx.room.Dao
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.typeName
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.ShortcutMethod
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import toJFO
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.tools.JavaFileObject
import kotlin.reflect.KClass

/**
 * Base test class for shortcut methods.
 */
abstract class ShortcutMethodProcessorTest<out T : ShortcutMethod>(
    val annotation: KClass<out Annotation>
) {
    companion object {
        const val DAO_PREFIX = """
                package foo.bar;
                import androidx.room.*;
                import java.util.*;
                @Dao
                abstract class MyClass {
                """
        const val DAO_SUFFIX = "}"
        val USER_TYPE_NAME: TypeName = COMMON.USER_TYPE_NAME
        val USERNAME_TYPE_NAME: TypeName = ClassName.get("foo.bar", "Username")
        val BOOK_TYPE_NAME: TypeName = ClassName.get("foo.bar", "Book")
    }

    @Test
    fun noParams() {
        singleShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public void foo();
                """) { shortcut, _ ->
            assertThat(shortcut.name, `is`("foo"))
            assertThat(shortcut.parameters.size, `is`(0))
        }.failsToCompile().withErrorContaining(noParamsError())
    }

    abstract fun noParamsError(): String

    @Test
    fun single() {
        singleShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public int foo(User user);
                """) { shortcut, _ ->
            assertThat(shortcut.name, `is`("foo"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(param.type.typeName(), `is`(USER_TYPE_NAME))
            assertThat(param.pojoType?.typeName(), `is`(USER_TYPE_NAME))
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["user"]?.isPartialEntity, `is`(false))
            assertThat(shortcut.entities["user"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
        }.compilesWithoutError()
    }

    @Test
    fun notAnEntity() {
        singleShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public void foo(NotAnEntity notValid);
                """) { shortcut, _ ->
            assertThat(shortcut.name, `is`("foo"))
            assertThat(shortcut.parameters.size, `is`(1))
            assertThat(shortcut.entities.size, `is`(0))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER
        )
    }

    @Test
    fun two() {
        singleShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public void foo(User u1, User u2);
                """) { shortcut, _ ->
            assertThat(shortcut.name, `is`("foo"))

            assertThat(shortcut.parameters.size, `is`(2))
            shortcut.parameters.forEach {
                assertThat(it.type.typeName(), `is`(USER_TYPE_NAME))
                assertThat(it.pojoType?.typeName(), `is`(USER_TYPE_NAME))
            }
            assertThat(shortcut.entities.size, `is`(2))
            assertThat(shortcut.entities["u1"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(shortcut.entities["u1"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(shortcut.parameters.map { it.name },
                    `is`(listOf("u1", "u2")))
        }.compilesWithoutError()
    }

    @Test
    fun list() {
        listOf(
                "int",
                "Integer",
                "${RxJava2TypeNames.SINGLE}<Integer>",
                "${RxJava2TypeNames.MAYBE}<Integer>",
                RxJava2TypeNames.COMPLETABLE,
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE}<Integer>"
        ).forEach { type ->
            singleShortcutMethod(
                    """
                @${annotation.java.canonicalName}
                abstract public $type users(List<User> users);
                """) { shortcut, _ ->
                assertThat(shortcut.name, `is`("users"))
                assertThat(shortcut.parameters.size, `is`(1))
                val param = shortcut.parameters.first()
                assertThat(param.type.typeName(), `is`(
                        ParameterizedTypeName.get(
                                ClassName.get("java.util", "List"), USER_TYPE_NAME) as TypeName))
                assertThat(param.pojoType?.typeName(), `is`(USER_TYPE_NAME))
                assertThat(shortcut.entities.size, `is`(1))
                assertThat(shortcut.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            }.compilesWithoutError()
        }
    }

    @Test
    fun array() {
        singleShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public void users(User[] users);
                """) { shortcut, _ ->
            assertThat(shortcut.name, `is`("users"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ArrayTypeName.of(COMMON.USER_TYPE_NAME) as TypeName))
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
        }.compilesWithoutError()
    }

    @Test
    fun set() {
        singleShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public void modifyUsers(Set<User> users);
                """) { shortcut, _ ->
            assertThat(shortcut.name, `is`("modifyUsers"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(
                            ClassName.get("java.util", "Set"),
                            COMMON.USER_TYPE_NAME
                    ) as TypeName))
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
        }.compilesWithoutError()
    }

    @Test
    fun iterable() {
        singleShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public void modifyUsers(Iterable<User> users);
                """) { shortcut, _ ->
            assertThat(shortcut.name, `is`("modifyUsers"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(ClassName.get("java.lang", "Iterable"),
                            COMMON.USER_TYPE_NAME) as TypeName))
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
        }.compilesWithoutError()
    }

    @Test
    fun customCollection() {
        singleShortcutMethod(
                """
                static class MyList<Irrelevant, Item> extends ArrayList<Item> {}
                @${annotation.java.canonicalName}
                abstract public void modifyUsers(MyList<String, User> users);
                """) { shortcut, _ ->
            assertThat(shortcut.name, `is`("modifyUsers"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(ClassName.get("foo.bar", "MyClass.MyList"),
                            CommonTypeNames.STRING, COMMON.USER_TYPE_NAME) as TypeName))
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
        }.compilesWithoutError()
    }

    @Test
    fun differentTypes() {
        listOf(
                "void",
                "int",
                "Integer",
                "${RxJava2TypeNames.SINGLE}<Integer>",
                "${RxJava2TypeNames.MAYBE}<Integer>",
                RxJava2TypeNames.COMPLETABLE,
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE}<Integer>"
        ).forEach { type ->
            singleShortcutMethod(
                    """
                @${annotation.java.canonicalName}
                abstract public $type foo(User u1, Book b1);
                """) { shortcut, _ ->
                assertThat(shortcut.parameters.size, `is`(2))
                assertThat(shortcut.parameters[0].type.typeName().toString(),
                        `is`("foo.bar.User"))
                assertThat(shortcut.parameters[1].type.typeName().toString(),
                        `is`("foo.bar.Book"))
                assertThat(shortcut.parameters.map { it.name }, `is`(listOf("u1", "b1")))
                assertThat(shortcut.entities.size, `is`(2))
                assertThat(shortcut.entities["u1"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
                assertThat(shortcut.entities["b1"]?.pojo?.typeName, `is`(BOOK_TYPE_NAME))
            }.compilesWithoutError()
        }
    }

    @Test
    fun invalidReturnType() {
        listOf(
                "long",
                "String",
                "User",
                "${RxJava2TypeNames.SINGLE}<Int>",
                "${RxJava2TypeNames.MAYBE}<Int>",
                "${RxJava2TypeNames.SINGLE}<String>",
                "${RxJava2TypeNames.MAYBE}<String>",
                "${RxJava2TypeNames.SINGLE}<User>",
                "${RxJava2TypeNames.MAYBE}<User>",
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE}<Int>",
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE}<String>",
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE}<User>"
        ).forEach { type ->
            singleShortcutMethod(
                    """
                @${annotation.java.canonicalName}
                abstract public $type foo(User user);
                """) { _, _ ->
            }.failsToCompile().withErrorContaining(invalidReturnTypeError())
        }
    }

    @Test
    fun targetEntity() {
        val usernameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
            }
        """.toJFO("foo.bar.Username")
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(Username username);
                """,
            additionalJFOs = listOf(usernameJfo)) { shortcut, _ ->
            assertThat(shortcut.name, `is`("foo"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(param.type.typeName(), `is`(USERNAME_TYPE_NAME))
            assertThat(param.pojoType?.typeName(), `is`(USERNAME_TYPE_NAME))
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["username"]?.isPartialEntity, `is`(true))
            assertThat(shortcut.entities["username"]?.entityTypeName, `is`(USER_TYPE_NAME))
            assertThat(shortcut.entities["username"]?.pojo?.typeName, `is`(USERNAME_TYPE_NAME))
        }.compilesWithoutError()
    }

    @Test
    fun targetEntitySameAsPojo() {
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(User user);
                """) { _, _ ->
        }.compilesWithoutError()
    }

    @Test
    fun targetEntityExtraColumn() {
        val usernameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
                long extraField;
            }
        """.toJFO("foo.bar.Username")
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(Username username);
                """,
            additionalJFOs = listOf(usernameJfo)) { _, _ ->
        }.failsToCompile().withErrorContaining(
            ProcessorErrors.cannotFindAsEntityField("foo.bar.User"))
    }

    @Test
    fun targetEntityExtraColumnIgnored() {
        val usernameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
                @Ignore
                long extraField;
            }
        """.toJFO("foo.bar.Username")
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(Username username);
                """,
            additionalJFOs = listOf(usernameJfo)) { _, _ ->
        }.compilesWithoutError()
    }

    @Test
    fun targetEntityWithEmbedded() {
        val usernameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                @Embedded
                Fullname name;
            }
        """.toJFO("foo.bar.Username")
        val fullnameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class Fullname {
                @ColumnInfo(name = "name")
                String firstName;
                String lastName;
            }
        """.toJFO("foo.bar.Fullname")
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(Username username);
                """,
            additionalJFOs = listOf(usernameJfo, fullnameJfo)) { _, _ ->
        }.compilesWithoutError()
    }

    @Test
    fun targetEntityWithRelation() {
        val userPetsJfo = """
            package foo.bar;
            import androidx.room.*;
            import java.util.List;

            public class UserPets {
                int uid;
                @Relation(parentColumn = "uid", entityColumn = "ownerId")
                List<Pet> pets;
            }
        """.toJFO("foo.bar.UserPets")
        val petJfo = """
            package foo.bar;
            import androidx.room.*;

            @Entity
            public class Pet {
                @PrimaryKey
                int petId;
                int ownerId;
            }
        """.toJFO("foo.bar.Pet")
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(UserPets userPets);
                """,
            additionalJFOs = listOf(userPetsJfo, petJfo)) { _, _ ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.INVALID_RELATION_IN_PARTIAL_ENTITY)
    }

    abstract fun invalidReturnTypeError(): String

    abstract fun process(
        baseContext: Context,
        containing: DeclaredType,
        executableElement: ExecutableElement
    ): T

    fun singleShortcutMethod(
        vararg input: String,
        additionalJFOs: List<JavaFileObject> = emptyList(),
        handler: (T, TestInvocation) -> Unit
    ):
            CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyClass",
                        DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX
                ), COMMON.USER, COMMON.BOOK, COMMON.NOT_AN_ENTITY, COMMON.COMPLETABLE, COMMON.MAYBE,
                    COMMON.SINGLE, COMMON.LISTENABLE_FUTURE, COMMON.GUAVA_ROOM) + additionalJFOs)
                .processedWith(TestProcessor.builder()
                        .forAnnotations(annotation, Dao::class)
                        .nextRunHandler { invocation ->
                            val (owner, methods) = invocation.roundEnv
                                    .getElementsAnnotatedWith(Dao::class.java)
                                    .map {
                                        Pair(it,
                                                invocation.processingEnv.elementUtils
                                                        .getAllMembers(MoreElements.asType(it))
                                                        .filter {
                                                            MoreElements.isAnnotationPresent(it,
                                                                    annotation.java)
                                                        }
                                        )
                                    }.first { it.second.isNotEmpty() }
                            val processed = process(
                                    baseContext = invocation.context,
                                    containing = MoreTypes.asDeclared(owner.asType()),
                                    executableElement = MoreElements.asExecutable(methods.first()))
                            handler(processed, invocation)
                            true
                        }
                        .build())
    }
}
