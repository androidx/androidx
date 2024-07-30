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
import androidx.kruth.assertThat
import androidx.room.Dao
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.asMutableClassName
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.runProcessorTestWithK1
import androidx.room.testing.context
import androidx.room.vo.DeleteOrUpdateShortcutMethod
import kotlin.reflect.KClass
import org.junit.Test

/** Base test class for shortcut methods. */
abstract class DeleteOrUpdateShortcutMethodProcessorTest<out T : DeleteOrUpdateShortcutMethod>(
    val annotation: KClass<out Annotation>
) {
    companion object {
        const val DAO_PREFIX =
            """
                package foo.bar;
                import androidx.room.*;
                import java.util.*;
                @Dao
                abstract class MyClass {
                """
        const val DAO_PREFIX_KT =
            """
                package foo.bar
                import androidx.room.*
                import java.util.*
                import io.reactivex.*         
                import io.reactivex.rxjava3.core.*
                import androidx.lifecycle.*
                import com.google.common.util.concurrent.*
                import org.reactivestreams.*
                import kotlinx.coroutines.flow.*
            
                @Dao
                abstract class MyClass {
                """

        const val DAO_SUFFIX = "}"
        val USER_TYPE_NAME: XTypeName = COMMON.USER_TYPE_NAME
        val USERNAME_TYPE_NAME: XTypeName = XClassName.get("foo.bar", "Username")
        val BOOK_TYPE_NAME: XTypeName = XClassName.get("foo.bar", "Book")
    }

    @Test
    fun noParams() {
        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void foo();
                """
        ) { shortcut, invocation ->
            assertThat(shortcut.element.jvmName).isEqualTo("foo")
            assertThat(shortcut.parameters.size).isEqualTo(0)
            invocation.assertCompilationResult { hasErrorContaining(noParamsError()) }
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
            assertThat(shortcut.element.jvmName).isEqualTo("foo")
            assertThat(shortcut.parameters.size).isEqualTo(1)
            val param = shortcut.parameters.first()
            assertThat(param.type.asTypeName()).isEqualTo(USER_TYPE_NAME.copy(nullable = true))
            assertThat(param.pojoType?.asTypeName()).isEqualTo(USER_TYPE_NAME.copy(nullable = true))
            assertThat(shortcut.entities.size).isEqualTo(1)
            assertThat(shortcut.entities["user"]?.isPartialEntity).isEqualTo(false)
            assertThat(shortcut.entities["user"]?.pojo?.typeName).isEqualTo(USER_TYPE_NAME)
        }
    }

    @Test
    fun singleNullableParamError() {
        singleShortcutMethodKotlin(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(user: User?)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.nullableParamInShortcutMethod("foo.bar.User"))
            }
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
            assertThat(shortcut.element.jvmName).isEqualTo("foo")
            assertThat(shortcut.parameters.size).isEqualTo(1)
            assertThat(shortcut.entities.size).isEqualTo(0)
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER)
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
            assertThat(shortcut.element.jvmName).isEqualTo("foo")

            assertThat(shortcut.parameters.size).isEqualTo(2)
            shortcut.parameters.forEach {
                assertThat(it.type.asTypeName()).isEqualTo(USER_TYPE_NAME.copy(nullable = true))
                assertThat(it.pojoType?.asTypeName())
                    .isEqualTo(USER_TYPE_NAME.copy(nullable = true))
            }
            assertThat(shortcut.entities.size).isEqualTo(2)
            assertThat(shortcut.entities["u1"]?.pojo?.typeName).isEqualTo(USER_TYPE_NAME)

            assertThat(shortcut.entities["u1"]?.pojo?.typeName).isEqualTo(USER_TYPE_NAME)

            assertThat(shortcut.parameters.map { it.name }).isEqualTo(listOf("u1", "u2"))
        }
    }

    @Test
    fun twoNullableParamError() {
        singleShortcutMethodKotlin(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(user1: User?, user2: User?)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.nullableParamInShortcutMethod("foo.bar.User"))
                hasErrorCount(2)
            }
        }
    }

    @Test
    fun list() {
        listOf(
                "int",
                "Integer",
                "${RxJava2TypeNames.SINGLE.canonicalName}<Integer>",
                "${RxJava2TypeNames.MAYBE.canonicalName}<Integer>",
                RxJava2TypeNames.COMPLETABLE.canonicalName,
                "${RxJava3TypeNames.SINGLE.canonicalName}<Integer>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<Integer>",
                RxJava3TypeNames.COMPLETABLE.canonicalName,
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.canonicalName}<Integer>"
            )
            .forEach { type ->
                singleShortcutMethod(
                    """
                @${annotation.java.canonicalName}
                abstract public $type users(List<User> users);
                """
                ) { shortcut, _ ->
                    assertThat(shortcut.element.jvmName).isEqualTo("users")
                    assertThat(shortcut.parameters.size).isEqualTo(1)
                    val param = shortcut.parameters.first()
                    assertThat(param.type.asTypeName())
                        .isEqualTo(
                            CommonTypeNames.MUTABLE_LIST.parametrizedBy(
                                    USER_TYPE_NAME.copy(nullable = true)
                                )
                                .copy(nullable = true)
                        )

                    assertThat(param.pojoType?.asTypeName())
                        .isEqualTo(USER_TYPE_NAME.copy(nullable = true))
                    assertThat(shortcut.entities.size).isEqualTo(1)
                    assertThat(shortcut.entities["users"]?.pojo?.typeName).isEqualTo(USER_TYPE_NAME)
                }
            }
    }

    @Test
    fun nullableListParamError() {
        singleShortcutMethodKotlin(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(users: List<User?>)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.nullableParamInShortcutMethod(
                        "java.util.List<? extends foo.bar.User>"
                    )
                )
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
            assertThat(shortcut.element.jvmName).isEqualTo("users")
            assertThat(shortcut.parameters.size).isEqualTo(1)
            val param = shortcut.parameters.first()
            assertThat(param.type.asTypeName())
                .isEqualTo(
                    XTypeName.getArrayName(COMMON.USER_TYPE_NAME.copy(nullable = true))
                        .copy(nullable = true)
                )

            assertThat(shortcut.entities.size).isEqualTo(1)
            assertThat(shortcut.entities["users"]?.pojo?.typeName).isEqualTo(USER_TYPE_NAME)
        }
    }

    @Test
    fun nullableArrayParamError() {
        singleShortcutMethodKotlin(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(users: Array<User?>)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.nullableParamInShortcutMethod("foo.bar.User[]"))
            }
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
            assertThat(shortcut.element.jvmName).isEqualTo("modifyUsers")
            assertThat(shortcut.parameters.size).isEqualTo(1)
            val param = shortcut.parameters.first()
            assertThat(param.type.asTypeName())
                .isEqualTo(
                    CommonTypeNames.MUTABLE_SET.parametrizedBy(
                            COMMON.USER_TYPE_NAME.copy(nullable = true)
                        )
                        .copy(nullable = true)
                )

            assertThat(shortcut.entities.size).isEqualTo(1)
            assertThat(shortcut.entities["users"]?.pojo?.typeName).isEqualTo(USER_TYPE_NAME)
        }
    }

    @Test
    fun nullableSetParamError() {
        singleShortcutMethodKotlin(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(users: Set<User?>)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.nullableParamInShortcutMethod(
                        "java.util.Set<? extends foo.bar.User>"
                    )
                )
            }
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
            assertThat(shortcut.element.jvmName).isEqualTo("modifyUsers")
            assertThat(shortcut.parameters.size).isEqualTo(1)
            val param = shortcut.parameters.first()
            assertThat(param.type.asTypeName())
                .isEqualTo(
                    Iterable::class.asMutableClassName()
                        .parametrizedBy(COMMON.USER_TYPE_NAME.copy(nullable = true))
                        .copy(nullable = true)
                )

            assertThat(shortcut.entities.size).isEqualTo(1)
            assertThat(shortcut.entities["users"]?.pojo?.typeName).isEqualTo(USER_TYPE_NAME)
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
            assertThat(shortcut.element.jvmName).isEqualTo("modifyUsers")
            assertThat(shortcut.parameters.size).isEqualTo(1)
            val param = shortcut.parameters.first()
            assertThat(param.type.asTypeName())
                .isEqualTo(
                    XClassName.get("foo.bar", "MyClass", "MyList")
                        .parametrizedBy(
                            CommonTypeNames.STRING.copy(nullable = true),
                            COMMON.USER_TYPE_NAME.copy(nullable = true)
                        )
                        .copy(nullable = true)
                )

            assertThat(shortcut.entities.size).isEqualTo(1)
            assertThat(shortcut.entities["users"]?.pojo?.typeName).isEqualTo(USER_TYPE_NAME)
        }
    }

    @Test
    fun nullableCustomCollectionParamError() {
        singleShortcutMethodKotlin(
            """
                class MyList<Irrelevant, Item> : ArrayList<Item> {}
                @${annotation.java.canonicalName}
                abstract fun foo(users: MyList<String?, User?>)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.nullableParamInShortcutMethod(
                        "foo.bar.MyClass.MyList<java.lang.String, foo.bar.User>"
                    )
                )
            }
        }
    }

    @Test
    fun differentTypes() {
        listOf(
                "void",
                "int",
                "Integer",
                "${RxJava2TypeNames.SINGLE.canonicalName}<Integer>",
                "${RxJava2TypeNames.MAYBE.canonicalName}<Integer>",
                RxJava2TypeNames.COMPLETABLE.canonicalName,
                "${RxJava3TypeNames.SINGLE.canonicalName}<Integer>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<Integer>",
                RxJava3TypeNames.COMPLETABLE.canonicalName,
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.canonicalName}<Integer>"
            )
            .forEach { type ->
                singleShortcutMethod(
                    """
                @${annotation.java.canonicalName}
                abstract public $type foo(User u1, Book b1);
                """
                ) { shortcut, _ ->
                    assertThat(shortcut.parameters.size).isEqualTo(2)
                    assertThat(shortcut.parameters[0].type.asTypeName().toString(CodeLanguage.JAVA))
                        .isEqualTo("foo.bar.User")

                    assertThat(shortcut.parameters[1].type.asTypeName().toString(CodeLanguage.JAVA))
                        .isEqualTo("foo.bar.Book")

                    assertThat(shortcut.parameters.map { it.name }).isEqualTo(listOf("u1", "b1"))
                    assertThat(shortcut.entities.size).isEqualTo(2)
                    assertThat(shortcut.entities["u1"]?.pojo?.typeName).isEqualTo(USER_TYPE_NAME)

                    assertThat(shortcut.entities["b1"]?.pojo?.typeName).isEqualTo(BOOK_TYPE_NAME)
                }
            }
    }

    @Test
    fun twoNullableDifferentParamError() {
        singleShortcutMethodKotlin(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(user1: User?, book1: Book?)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.nullableParamInShortcutMethod("foo.bar.User"))
                hasErrorContaining(ProcessorErrors.nullableParamInShortcutMethod("foo.bar.Book"))
                hasErrorCount(2)
            }
        }
    }

    @Test
    fun invalidReturnType() {
        listOf(
                "long",
                "String",
                "User",
                "${RxJava2TypeNames.SINGLE.canonicalName}<Int>",
                "${RxJava2TypeNames.MAYBE.canonicalName}<Int>",
                "${RxJava2TypeNames.SINGLE.canonicalName}<String>",
                "${RxJava2TypeNames.MAYBE.canonicalName}<String>",
                "${RxJava2TypeNames.SINGLE.canonicalName}<User>",
                "${RxJava2TypeNames.MAYBE.canonicalName}<User>",
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.canonicalName}<Int>",
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.canonicalName}<String>",
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.canonicalName}<User>"
            )
            .forEach { type ->
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
    fun suspendReturnsDeferredType() {
        listOf(
                "${RxJava2TypeNames.FLOWABLE.canonicalName}<Int>",
                "${RxJava2TypeNames.OBSERVABLE.canonicalName}<Int>",
                "${RxJava2TypeNames.MAYBE.canonicalName}<Int>",
                "${RxJava2TypeNames.SINGLE.canonicalName}<Int>",
                "${RxJava2TypeNames.COMPLETABLE.canonicalName}",
                "${RxJava3TypeNames.FLOWABLE.canonicalName}<Int>",
                "${RxJava3TypeNames.OBSERVABLE.canonicalName}<Int>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<Int>",
                "${RxJava3TypeNames.SINGLE.canonicalName}<Int>",
                "${RxJava3TypeNames.COMPLETABLE.canonicalName}",
                "${LifecyclesTypeNames.LIVE_DATA.canonicalName}<Int>",
                "${LifecyclesTypeNames.COMPUTABLE_LIVE_DATA.canonicalName}<Int>",
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.canonicalName}<Int>",
                "${ReactiveStreamsTypeNames.PUBLISHER.canonicalName}<Int>",
                "${KotlinTypeNames.FLOW.canonicalName}<Int>"
            )
            .forEach { type ->
                singleShortcutMethodKotlin(
                    """
                @${annotation.java.canonicalName}
                abstract suspend fun foo(user: User): $type
                """
                ) { _, invocation ->
                    invocation.assertCompilationResult {
                        val rawTypeName = type.substringBefore("<")
                        hasErrorContaining(ProcessorErrors.suspendReturnsDeferredType(rawTypeName))
                    }
                }
            }
    }

    @Test
    fun targetEntity() {
        val usernameSource =
            Source.java(
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
            assertThat(shortcut.element.jvmName).isEqualTo("foo")
            assertThat(shortcut.parameters.size).isEqualTo(1)
            val param = shortcut.parameters.first()
            assertThat(param.type.asTypeName()).isEqualTo(USERNAME_TYPE_NAME.copy(nullable = true))
            assertThat(param.pojoType?.asTypeName())
                .isEqualTo(USERNAME_TYPE_NAME.copy(nullable = true))
            assertThat(shortcut.entities.size).isEqualTo(1)
            assertThat(shortcut.entities["username"]?.isPartialEntity).isEqualTo(true)
            assertThat(shortcut.entities["username"]?.entityTypeName).isEqualTo(USER_TYPE_NAME)
            assertThat(shortcut.entities["username"]?.pojo?.typeName).isEqualTo(USERNAME_TYPE_NAME)
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
        val usernameSource =
            Source.java(
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
                hasErrorContaining(ProcessorErrors.cannotFindAsEntityField("foo.bar.User"))
            }
        }
    }

    @Test
    fun targetEntityExtraColumnIgnored() {
        val usernameSource =
            Source.java(
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
        val usernameSource =
            Source.java(
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
        val fullnameSource =
            Source.java(
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
        val userPetsSource =
            Source.java(
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
        val petSource =
            Source.java(
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
                hasErrorContaining(ProcessorErrors.shortcutMethodArgumentMustBeAClass("long"))
            }
        }
    }

    @Test
    fun targetEntity_emptyClassParameter() {
        val emptyClass =
            Source.java(
                "foo.bar.EmptyClass",
                """
            package foo.bar;
            public class EmptyClass {}
            """
                    .trimIndent()
            )

        singleShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public int foo(EmptyClass x);
                """,
            additionalSources = listOf(emptyClass)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.noColumnsInPartialEntity("foo.bar.EmptyClass"))
            }
        }
    }

    @Test
    fun nonNullVoidGuava() {
        singleShortcutMethodKotlin(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(user: User): ListenableFuture<Void>
                """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorContaining(ProcessorErrors.NONNULL_VOID) }
        }
    }

    abstract fun invalidReturnTypeError(): String

    abstract fun process(
        baseContext: Context,
        containing: XType,
        executableElement: XMethodElement
    ): T

    protected fun singleShortcutMethod(
        vararg input: String,
        additionalSources: List<Source> = emptyList(),
        handler: (T, XTestInvocation) -> Unit
    ) {
        val inputSource =
            Source.java("foo.bar.MyClass", DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX)
        val commonSources =
            listOf(
                COMMON.USER,
                COMMON.BOOK,
                COMMON.NOT_AN_ENTITY,
                COMMON.RX2_COMPLETABLE,
                COMMON.RX2_MAYBE,
                COMMON.RX2_SINGLE,
                COMMON.RX3_COMPLETABLE,
                COMMON.RX3_MAYBE,
                COMMON.RX3_SINGLE,
                COMMON.LISTENABLE_FUTURE,
                COMMON.GUAVA_ROOM
            )
        runProcessorTestWithK1(
            sources = commonSources + additionalSources + inputSource,
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "false"),
        ) { invocation ->
            val (owner, methods) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map {
                        Pair(
                            it,
                            it.getAllMethods().filter { it.hasAnnotation(annotation) }.toList()
                        )
                    }
                    .first { it.second.isNotEmpty() }
            val processed =
                process(
                    baseContext = invocation.context,
                    containing = owner.type,
                    executableElement = methods.first()
                )
            handler(processed, invocation)
        }
    }

    protected fun singleShortcutMethodKotlin(
        vararg input: String,
        additionalSources: List<Source> = emptyList(),
        handler: (T, XTestInvocation) -> Unit
    ) {
        val inputSource =
            Source.kotlin("MyClass.kt", DAO_PREFIX_KT + input.joinToString("\n") + DAO_SUFFIX)

        val commonSources =
            listOf(
                COMMON.USER,
                COMMON.BOOK,
                COMMON.NOT_AN_ENTITY,
                COMMON.RX2_COMPLETABLE,
                COMMON.RX2_MAYBE,
                COMMON.RX2_SINGLE,
                COMMON.RX2_FLOWABLE,
                COMMON.RX2_OBSERVABLE,
                COMMON.RX3_COMPLETABLE,
                COMMON.RX3_MAYBE,
                COMMON.RX3_SINGLE,
                COMMON.RX3_FLOWABLE,
                COMMON.RX3_OBSERVABLE,
                COMMON.LISTENABLE_FUTURE,
                COMMON.LIVE_DATA,
                COMMON.COMPUTABLE_LIVE_DATA,
                COMMON.PUBLISHER,
                COMMON.FLOW,
                COMMON.GUAVA_ROOM
            )

        runProcessorTestWithK1(
            sources = commonSources + additionalSources + inputSource,
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "false"),
        ) { invocation ->
            val (owner, methods) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map {
                        Pair(
                            it,
                            it.getAllMethods().filter { it.hasAnnotation(annotation) }.toList()
                        )
                    }
                    .first { it.second.isNotEmpty() }

            val processed =
                process(
                    baseContext = invocation.context,
                    containing = owner.type,
                    executableElement = methods.first()
                )
            handler(processed, invocation)
        }
    }
}
