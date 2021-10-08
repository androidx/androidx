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
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.testing.context
import androidx.room.vo.ShortcutMethod
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
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
                """
        ) { shortcut, invocation ->
            assertThat(shortcut.name, `is`("foo"))
            assertThat(shortcut.parameters.size, `is`(0))
            invocation.assertCompilationResult {
                hasErrorContaining(noParamsError())
            }
        }
    }

    abstract fun noParamsError(): String

    @Test
    fun single() {
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public int foo(User user);
                """
        ) { shortcut, _ ->
            assertThat(shortcut.name, `is`("foo"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(param.type.typeName, `is`(USER_TYPE_NAME))
            assertThat(param.pojoType?.typeName, `is`(USER_TYPE_NAME))
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["user"]?.isPartialEntity, `is`(false))
            assertThat(shortcut.entities["user"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
        }
    }

    @Test
    fun notAnEntity() {
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void foo(NotAnEntity notValid);
                """
        ) { shortcut, invocation ->
            assertThat(shortcut.name, `is`("foo"))
            assertThat(shortcut.parameters.size, `is`(1))
            assertThat(shortcut.entities.size, `is`(0))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER
                )
            }
        }
    }

    @Test
    fun two() {
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void foo(User u1, User u2);
                """
        ) { shortcut, _ ->
            assertThat(shortcut.name, `is`("foo"))

            assertThat(shortcut.parameters.size, `is`(2))
            shortcut.parameters.forEach {
                assertThat(it.type.typeName, `is`(USER_TYPE_NAME))
                assertThat(it.pojoType?.typeName, `is`(USER_TYPE_NAME))
            }
            assertThat(shortcut.entities.size, `is`(2))
            assertThat(shortcut.entities["u1"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(shortcut.entities["u1"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(
                shortcut.parameters.map { it.name },
                `is`(listOf("u1", "u2"))
            )
        }
    }

    @Test
    fun list() {
        listOf(
            "int",
            "Integer",
            "${RxJava2TypeNames.SINGLE}<Integer>",
            "${RxJava2TypeNames.MAYBE}<Integer>",
            RxJava2TypeNames.COMPLETABLE,
            "${RxJava3TypeNames.SINGLE}<Integer>",
            "${RxJava3TypeNames.MAYBE}<Integer>",
            RxJava3TypeNames.COMPLETABLE,
            "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE}<Integer>"
        ).forEach { type ->
            singleShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public $type users(List<User> users);
                """
            ) { shortcut, _ ->
                assertThat(shortcut.name, `is`("users"))
                assertThat(shortcut.parameters.size, `is`(1))
                val param = shortcut.parameters.first()
                assertThat(
                    param.type.typeName,
                    `is`(
                        ParameterizedTypeName.get(
                            ClassName.get("java.util", "List"), USER_TYPE_NAME
                        ) as TypeName
                    )
                )
                assertThat(param.pojoType?.typeName, `is`(USER_TYPE_NAME))
                assertThat(shortcut.entities.size, `is`(1))
                assertThat(shortcut.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            }
        }
    }

    @Test
    fun array() {
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void users(User[] users);
                """
        ) { shortcut, _ ->
            assertThat(shortcut.name, `is`("users"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(
                param.type.typeName,
                `is`(
                    ArrayTypeName.of(COMMON.USER_TYPE_NAME) as TypeName
                )
            )
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
        }
    }

    @Test
    fun set() {
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void modifyUsers(Set<User> users);
                """
        ) { shortcut, _ ->
            assertThat(shortcut.name, `is`("modifyUsers"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(
                param.type.typeName,
                `is`(
                    ParameterizedTypeName.get(
                        ClassName.get("java.util", "Set"),
                        COMMON.USER_TYPE_NAME
                    ) as TypeName
                )
            )
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
        }
    }

    @Test
    fun iterable() {
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void modifyUsers(Iterable<User> users);
                """
        ) { shortcut, _ ->
            assertThat(shortcut.name, `is`("modifyUsers"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(
                param.type.typeName,
                `is`(
                    ParameterizedTypeName.get(
                        ClassName.get("java.lang", "Iterable"),
                        COMMON.USER_TYPE_NAME
                    ) as TypeName
                )
            )
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
        }
    }

    @Test
    fun customCollection() {
        singleShortcutMethod(
            """
                static class MyList<Irrelevant, Item> extends ArrayList<Item> {}
                @${annotation.java.canonicalName}
                abstract public void modifyUsers(MyList<String, User> users);
                """
        ) { shortcut, _ ->
            assertThat(shortcut.name, `is`("modifyUsers"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(
                param.type.typeName,
                `is`(
                    ParameterizedTypeName.get(
                        ClassName.get("foo.bar", "MyClass.MyList"),
                        CommonTypeNames.STRING, COMMON.USER_TYPE_NAME
                    ) as TypeName
                )
            )
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
        }
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
            "${RxJava3TypeNames.SINGLE}<Integer>",
            "${RxJava3TypeNames.MAYBE}<Integer>",
            RxJava3TypeNames.COMPLETABLE,
            "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE}<Integer>"
        ).forEach { type ->
            singleShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public $type foo(User u1, Book b1);
                """
            ) { shortcut, _ ->
                assertThat(shortcut.parameters.size, `is`(2))
                assertThat(
                    shortcut.parameters[0].type.typeName.toString(),
                    `is`("foo.bar.User")
                )
                assertThat(
                    shortcut.parameters[1].type.typeName.toString(),
                    `is`("foo.bar.Book")
                )
                assertThat(shortcut.parameters.map { it.name }, `is`(listOf("u1", "b1")))
                assertThat(shortcut.entities.size, `is`(2))
                assertThat(shortcut.entities["u1"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
                assertThat(shortcut.entities["b1"]?.pojo?.typeName, `is`(BOOK_TYPE_NAME))
            }
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
                """
            ) { _, invocation ->
                invocation.assertCompilationResult {
                    hasErrorContaining(invalidReturnTypeError())
                }
            }
        }
    }

    @Test
    fun targetEntity() {
        val usernameSource = Source.java(
            "foo.bar.Username",
            """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
            }
            """
        )
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(Username username);
                """,
            additionalSources = listOf(usernameSource)
        ) { shortcut, _ ->
            assertThat(shortcut.name, `is`("foo"))
            assertThat(shortcut.parameters.size, `is`(1))
            val param = shortcut.parameters.first()
            assertThat(param.type.typeName, `is`(USERNAME_TYPE_NAME))
            assertThat(param.pojoType?.typeName, `is`(USERNAME_TYPE_NAME))
            assertThat(shortcut.entities.size, `is`(1))
            assertThat(shortcut.entities["username"]?.isPartialEntity, `is`(true))
            assertThat(shortcut.entities["username"]?.entityTypeName, `is`(USER_TYPE_NAME))
            assertThat(shortcut.entities["username"]?.pojo?.typeName, `is`(USERNAME_TYPE_NAME))
        }
    }

    @Test
    fun targetEntitySameAsPojo() {
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(User user);
                """
        ) { _, _ ->
        }
    }

    @Test
    fun targetEntityExtraColumn() {
        val usernameSource = Source.java(
            "foo.bar.Username",
            """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
                long extraField;
            }
            """
        )
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(Username username);
                """,
            additionalSources = listOf(usernameSource)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.cannotFindAsEntityField("foo.bar.User")
                )
            }
        }
    }

    @Test
    fun targetEntityExtraColumnIgnored() {
        val usernameSource = Source.java(
            "foo.bar.Username",
            """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
                @Ignore
                long extraField;
            }
            """
        )
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(Username username);
                """,
            additionalSources = listOf(usernameSource)
        ) { _, _ ->
        }
    }

    @Test
    fun targetEntityWithEmbedded() {
        val usernameSource = Source.java(
            "foo.bar.Username",
            """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                @Embedded
                Fullname name;
            }
            """
        )
        val fullnameSource = Source.java(
            "foo.bar.Fullname",
            """
            package foo.bar;
            import androidx.room.*;

            public class Fullname {
                @ColumnInfo(name = "name")
                String firstName;
                String lastName;
            }
            """
        )
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(Username username);
                """,
            additionalSources = listOf(usernameSource, fullnameSource)
        ) { _, _ ->
        }
    }

    @Test
    fun targetEntityWithRelation() {
        val userPetsSource = Source.java(
            "foo.bar.UserPets",
            """
            package foo.bar;
            import androidx.room.*;
            import java.util.List;

            public class UserPets {
                int uid;
                @Relation(parentColumn = "uid", entityColumn = "ownerId")
                List<Pet> pets;
            }
            """
        )
        val petSource = Source.java(
            "foo.bar.Pet",
            """
            package foo.bar;
            import androidx.room.*;

            @Entity
            public class Pet {
                @PrimaryKey
                int petId;
                int ownerId;
            }
            """
        )
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(UserPets userPets);
                """,
            additionalSources = listOf(userPetsSource, petSource)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_RELATION_IN_PARTIAL_ENTITY)
            }
        }
    }

    @Test
    fun targetEntity_notDeclared() {
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(long x);
                """,
        ) { _, invocation ->
            invocation.assertCompilationResult {
                if (invocation.isKsp) {
                    hasErrorContaining(
                        ProcessorErrors.noColumnsInPartialEntity(
                            "java.lang.Long"
                        )
                    )
                } else {
                    // javac has a different error for primitives.
                    hasErrorContaining(
                        ProcessorErrors.shortcutMethodArgumentMustBeAClass(
                            TypeName.LONG
                        )
                    )
                }
            }
        }
    }

    @Test
    fun targetEntity_emptyClassParameter() {
        val emptyClass = Source.java(
            "foo.bar.EmptyClass",
            """
            package foo.bar;
            public class EmptyClass {}
            """.trimIndent()
        )

        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(EmptyClass x);
                """,
            additionalSources = listOf(emptyClass)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.noColumnsInPartialEntity(
                        "foo.bar.EmptyClass"
                    )
                )
            }
        }
    }

    abstract fun invalidReturnTypeError(): String

    abstract fun process(
        baseContext: Context,
        containing: XType,
        executableElement: XMethodElement
    ): T

    fun singleShortcutMethod(
        vararg input: String,
        additionalSources: List<Source> = emptyList(),
        handler: (T, XTestInvocation) -> Unit
    ) {
        val inputSource = Source.java(
            "foo.bar.MyClass",
            DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX
        )
        val commonSources = listOf(
            COMMON.USER, COMMON.BOOK, COMMON.NOT_AN_ENTITY, COMMON.RX2_COMPLETABLE,
            COMMON.RX2_MAYBE, COMMON.RX2_SINGLE, COMMON.RX3_COMPLETABLE,
            COMMON.RX3_MAYBE, COMMON.RX3_SINGLE, COMMON.LISTENABLE_FUTURE,
            COMMON.GUAVA_ROOM
        )
        runProcessorTest(
            sources = commonSources + additionalSources + inputSource
        ) { invocation ->
            val (owner, methods) = invocation.roundEnv
                .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                .filterIsInstance<XTypeElement>()
                .map {
                    Pair(
                        it,
                        it.getAllMethods().filter {
                            it.hasAnnotation(annotation)
                        }.toList()
                    )
                }.first { it.second.isNotEmpty() }
            val processed = process(
                baseContext = invocation.context,
                containing = owner.type,
                executableElement = methods.first()
            )
            handler(processed, invocation)
        }
    }
}
