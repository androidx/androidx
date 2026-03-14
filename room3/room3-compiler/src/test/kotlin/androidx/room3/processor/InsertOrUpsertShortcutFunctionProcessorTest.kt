/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.room3.processor

import COMMON
import androidx.kruth.assertThat
import androidx.room3.Dao
import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XClassName
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.codegen.asMutableClassName
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.XTestInvocation
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.GuavaUtilConcurrentTypeNames
import androidx.room3.ext.KotlinTypeNames
import androidx.room3.ext.LifecyclesTypeNames
import androidx.room3.ext.ReactiveStreamsTypeNames
import androidx.room3.ext.RxJava3TypeNames
import androidx.room3.solver.shortcut.result.InsertOrUpsertFunctionAdapter
import androidx.room3.testing.context
import androidx.room3.vo.InsertOrUpsertShortcutFunction
import kotlin.reflect.KClass
import org.junit.Test

/** Base test class for insert and upsert methods. */
abstract class InsertOrUpsertShortcutFunctionProcessorTest<out T : InsertOrUpsertShortcutFunction>(
    val annotation: KClass<out Annotation>
) {
    companion object {
        const val DAO_PREFIX =
            """
                package foo.bar;
                import androidx.room3.*;
                import java.util.*;
                import androidx.room3.guava.GuavaDaoReturnTypeConverter;
                import androidx.room3.rxjava3.RxDaoReturnTypeConverters;
                @Dao
                @DaoReturnTypeConverters({GuavaDaoReturnTypeConverter.class, RxDaoReturnTypeConverters.class})
                abstract class MyClass {
                """
        const val DAO_PREFIX_KT =
            """
                package foo.bar
                import androidx.room3.*
                import java.util.*
                import io.reactivex.*
                import io.reactivex.rxjava3.core.*
                import androidx.lifecycle.*
                import com.google.common.util.concurrent.*
                import org.reactivestreams.*
                import kotlinx.coroutines.flow.*
                import androidx.room3.guava.GuavaDaoReturnTypeConverter
                import androidx.room3.rxjava3.RxDaoReturnTypeConverters

                @Dao
                @DaoReturnTypeConverters(GuavaDaoReturnTypeConverter::class, RxDaoReturnTypeConverters::class)
                abstract class MyClass {
                """
        const val DAO_SUFFIX = "}"
        val USER_TYPE_NAME: XTypeName = COMMON.USER_TYPE_NAME
        val USERNAME_TYPE_NAME: XTypeName = XClassName.get("foo.bar", "Username")
        val BOOK_TYPE_NAME: XTypeName = XClassName.get("foo.bar", "Book")
    }

    @Test
    fun readNoParams() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void foo();
                """
        ) { insertionUpsertion, invocation ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("foo")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(0)
            assertThat(insertionUpsertion.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)
            assertThat(insertionUpsertion.entities.size).isEqualTo(0)
            invocation.assertCompilationResult { hasErrorContaining(noParamsError()) }
        }
    }

    @Test
    fun notAnEntity() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void foo(NotAnEntity notValid);
                """
        ) { insertionUpsertion, invocation ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("foo")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)
            assertThat(insertionUpsertion.entities.size).isEqualTo(0)
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER)
            }
        }
    }

    abstract fun noParamsError(): String

    @Test
    fun single() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public long foo(User user);
                """
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("foo")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)

            val param = insertionUpsertion.parameters.first()
            assertThat(param.type.asTypeName()).isEqualTo(USER_TYPE_NAME.copy(nullable = true))

            assertThat(param.dataClassType?.asTypeName())
                .isEqualTo(USER_TYPE_NAME.copy(nullable = true))

            assertThat(insertionUpsertion.entities["user"]?.isPartialEntity).isEqualTo(false)

            assertThat(insertionUpsertion.entities["user"]?.dataClass?.typeName)
                .isEqualTo(XClassName.get("foo.bar", "User"))

            assertThat(insertionUpsertion.returnType.asTypeName())
                .isEqualTo(XTypeName.PRIMITIVE_LONG)
        }
    }

    @Test
    fun singleNullableParamError() {
        singleInsertUpsertShortcutFunction(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(user: User?)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.nullableParamInShortcutFunction("foo.bar.User?"))
            }
        }
    }

    @Test
    fun two() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void foo(User u1, User u2);
                """
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("foo")

            assertThat(insertionUpsertion.parameters.size).isEqualTo(2)
            insertionUpsertion.parameters.forEach {
                assertThat(it.type.asTypeName()).isEqualTo(USER_TYPE_NAME.copy(nullable = true))
                assertThat(it.dataClassType?.asTypeName())
                    .isEqualTo(USER_TYPE_NAME.copy(nullable = true))
            }
            assertThat(insertionUpsertion.entities.size).isEqualTo(2)

            assertThat(insertionUpsertion.entities["u1"]?.dataClass?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.entities["u2"]?.dataClass?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.parameters.map { it.name }).isEqualTo(listOf("u1", "u2"))

            assertThat(insertionUpsertion.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)
        }
    }

    @Test
    fun twoNullableParamError() {
        singleInsertUpsertShortcutFunction(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(user1: User?, user2: User?)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.nullableParamInShortcutFunction("foo.bar.User?"))
                hasErrorCount(2)
            }
        }
    }

    @Test
    fun list() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public List<Long> insertUsers(List<User> users);
                """
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("insertUsers")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)
            val param = insertionUpsertion.parameters.first()
            assertThat(param.type.asTypeName())
                .isEqualTo(
                    CommonTypeNames.MUTABLE_LIST.parametrizedBy(
                            USER_TYPE_NAME.copy(nullable = true)
                        )
                        .copy(nullable = true)
                )

            assertThat(param.dataClassType?.asTypeName())
                .isEqualTo(USER_TYPE_NAME.copy(nullable = true))

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.dataClass?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.asTypeName())
                .isEqualTo(
                    CommonTypeNames.MUTABLE_LIST.parametrizedBy(
                            XTypeName.BOXED_LONG.copy(nullable = true)
                        )
                        .copy(nullable = true)
                )
        }
    }

    @Test
    fun nullableListParamError() {
        singleInsertUpsertShortcutFunction(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(users: List<User?>)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.nullableParamInShortcutFunction(
                        "kotlin.collections.List<foo.bar.User?>"
                    )
                )
            }
        }
    }

    @Test
    fun array() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void insertUsers(User[] users);
                """
        ) { insertionUpsertion, invocation ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("insertUsers")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)
            val param = insertionUpsertion.parameters.first()
            assertThat(param.type.asTypeName())
                .isEqualTo(
                    XTypeName.getArrayName(
                            if (invocation.isKsp2) {
                                XTypeName.getProducerExtendsName(
                                    COMMON.USER_TYPE_NAME.copy(nullable = true)
                                )
                            } else {
                                COMMON.USER_TYPE_NAME.copy(nullable = true)
                            }
                        )
                        .copy(nullable = true)
                )

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.dataClass?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)
        }
    }

    @Test
    fun nullableArrayParamError() {
        singleInsertUpsertShortcutFunction(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(users: Array<User?>)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.nullableParamInShortcutFunction("kotlin.Array<foo.bar.User?>")
                )
            }
        }
    }

    @Test
    fun set() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void insertUsers(Set<User> users);
                """
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("insertUsers")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)
            val param = insertionUpsertion.parameters.first()
            assertThat(param.type.asTypeName())
                .isEqualTo(
                    CommonTypeNames.MUTABLE_SET.parametrizedBy(
                            COMMON.USER_TYPE_NAME.copy(nullable = true)
                        )
                        .copy(nullable = true)
                )

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.dataClass?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)
        }
    }

    @Test
    fun nullableSetParamError() {
        singleInsertUpsertShortcutFunction(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(users: Set<User?>)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.nullableParamInShortcutFunction(
                        "kotlin.collections.Set<foo.bar.User?>"
                    )
                )
            }
        }
    }

    @Test
    fun queue() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void insertUsers(Queue<User> users);
                """
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("insertUsers")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)
            val param = insertionUpsertion.parameters.first()
            assertThat(param.type.asTypeName())
                .isEqualTo(
                    CommonTypeNames.QUEUE.parametrizedBy(USER_TYPE_NAME.copy(nullable = true))
                        .copy(nullable = true)
                )

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.dataClass?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)
        }
    }

    @Test
    fun iterable() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void insert(Iterable<User> users);
                """
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("insert")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)
            val param = insertionUpsertion.parameters.first()
            assertThat(param.type.asTypeName())
                .isEqualTo(
                    Iterable::class.asMutableClassName()
                        .parametrizedBy(USER_TYPE_NAME.copy(nullable = true))
                        .copy(nullable = true)
                )

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.dataClass?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)
        }
    }

    @Test
    fun customCollection() {
        singleInsertUpsertShortcutMethod(
            """
                static class MyList<Irrelevant, Item> extends ArrayList<Item> {}
                @${annotation.java.canonicalName}
                abstract public void insert(MyList<String, User> users);
                """
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("insert")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)
            val param = insertionUpsertion.parameters.first()
            assertThat(param.type.asTypeName())
                .isEqualTo(
                    XClassName.get("foo.bar", "MyClass", "MyList")
                        .parametrizedBy(
                            CommonTypeNames.STRING.copy(nullable = true),
                            USER_TYPE_NAME.copy(nullable = true),
                        )
                        .copy(nullable = true)
                )

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.dataClass?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)
        }
    }

    @Test
    fun nullableCustomCollectionParamError() {
        singleInsertUpsertShortcutFunction(
            """
                class MyList<Irrelevant, Item> : ArrayList<Item> {}
                @${annotation.java.canonicalName}
                abstract fun foo(users: MyList<String?, User?>)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.nullableParamInShortcutFunction(
                        "foo.bar.MyClass.MyList<kotlin.String?, foo.bar.User?>"
                    )
                )
            }
        }
    }

    @Test
    fun differentTypes() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void foo(User u1, Book b1);
                """
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.parameters.size).isEqualTo(2)
            assertThat(
                    insertionUpsertion.parameters[0].type.asTypeName().toString(CodeLanguage.JAVA)
                )
                .isEqualTo("foo.bar.User")

            assertThat(
                    insertionUpsertion.parameters[1].type.asTypeName().toString(CodeLanguage.JAVA)
                )
                .isEqualTo("foo.bar.Book")

            assertThat(insertionUpsertion.parameters.map { it.name }).isEqualTo(listOf("u1", "b1"))

            assertThat(insertionUpsertion.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)

            assertThat(insertionUpsertion.entities.size).isEqualTo(2)

            assertThat(insertionUpsertion.entities["u1"]?.dataClass?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.entities["b1"]?.dataClass?.typeName)
                .isEqualTo(BOOK_TYPE_NAME)
        }
    }

    @Test
    fun multipleParamCompletable() {
        listOf(RxJava3TypeNames.COMPLETABLE.canonicalName).forEach { type ->
            singleInsertUpsertShortcutFunction(
                """
                @${annotation.java.canonicalName}
                abstract fun bookUserCompletable(user: User, book: Book): $type
                """
            ) { insertionUpsertion, _ ->
                assertThat(insertionUpsertion.parameters.size).isEqualTo(2)
            }
        }
    }

    @Test
    fun twoNullableDifferentParamError() {
        singleInsertUpsertShortcutFunction(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(user1: User?, book1: Book?)
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.nullableParamInShortcutFunction("foo.bar.User?"))
                hasErrorContaining(ProcessorErrors.nullableParamInShortcutFunction("foo.bar.Book?"))
                hasErrorCount(2)
            }
        }
    }

    @Test
    fun invalidReturnType() {
        listOf(
                "int",
                "${RxJava3TypeNames.SINGLE.canonicalName}<Int>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<Int>",
                "${RxJava3TypeNames.SINGLE.canonicalName}<String>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<String>",
                "${RxJava3TypeNames.SINGLE.canonicalName}<User>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<User>",
            )
            .forEach { type ->
                singleInsertUpsertShortcutMethod(
                    """
                @${annotation.java.canonicalName}
                abstract public $type foo(User user);
                """
                ) { insertionUpsertion, invocation ->
                    assertThat(insertionUpsertion.functionBinder?.adapter).isNull()

                    invocation.assertCompilationResult { hasErrorContaining(noAdapter()) }
                }
            }
    }

    @Test
    fun mismatchedReturnType() {
        listOf(
                "long[]",
                "Long[]",
                "List<Long>",
                "${RxJava3TypeNames.SINGLE.canonicalName}<List<Long>>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<List<Long>>",
            )
            .forEach { type ->
                singleInsertUpsertShortcutMethod(
                    """
                @${annotation.java.canonicalName}
                abstract public $type foo(User user);
                """
                ) { insertionUpsertion, invocation ->
                    assertThat(insertionUpsertion.functionBinder?.adapter).isNull()

                    invocation.assertCompilationResult {
                        hasErrorContaining(singleParamAndMultiReturnMismatchError())
                    }
                }
            }
    }

    @Test
    fun mismatchedReturnType2() {
        listOf(
                "long",
                "Long",
                "${RxJava3TypeNames.SINGLE.canonicalName}<Long>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<Long>",
            )
            .forEach { type ->
                singleInsertUpsertShortcutMethod(
                    """
                @${annotation.java.canonicalName}
                abstract public $type foo(User... user);
                """
                ) { insertionUpsertion, invocation ->
                    assertThat(insertionUpsertion.functionBinder?.adapter).isNull()

                    invocation.assertCompilationResult {
                        hasErrorContaining(multiParamAndSingleReturnMismatchError())
                    }
                }
            }
    }

    @Test
    fun mismatchedReturnType3() {
        listOf(
                "long",
                "Long",
                "${RxJava3TypeNames.SINGLE.canonicalName}<Long>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<Long>",
            )
            .forEach { type ->
                singleInsertUpsertShortcutMethod(
                    """
                @${annotation.java.canonicalName}
                abstract public $type foo(User user1, User user2);
                """
                ) { insertionUpsertion, invocation ->
                    assertThat(insertionUpsertion.functionBinder?.adapter).isNull()

                    invocation.assertCompilationResult { hasErrorContaining(noAdapter()) }
                }
            }
    }

    @Test
    fun validReturnTypes() {
        listOf(
                Pair("void", InsertOrUpsertFunctionAdapter.ReturnInfo.VOID),
                Pair("long", InsertOrUpsertFunctionAdapter.ReturnInfo.SINGLE_ID),
                Pair("long[]", InsertOrUpsertFunctionAdapter.ReturnInfo.ID_ARRAY),
                Pair("Long[]", InsertOrUpsertFunctionAdapter.ReturnInfo.ID_ARRAY_BOX),
                Pair("List<Long>", InsertOrUpsertFunctionAdapter.ReturnInfo.ID_LIST),
                Pair(
                    RxJava3TypeNames.COMPLETABLE.canonicalName,
                    InsertOrUpsertFunctionAdapter.ReturnInfo.VOID_OBJECT,
                ),
                Pair(
                    "${RxJava3TypeNames.SINGLE.canonicalName}<Long>",
                    InsertOrUpsertFunctionAdapter.ReturnInfo.SINGLE_ID,
                ),
                Pair(
                    "${RxJava3TypeNames.SINGLE.canonicalName}<List<Long>>",
                    InsertOrUpsertFunctionAdapter.ReturnInfo.ID_LIST,
                ),
                Pair(
                    "${RxJava3TypeNames.MAYBE.canonicalName}<Long>",
                    InsertOrUpsertFunctionAdapter.ReturnInfo.SINGLE_ID,
                ),
                Pair(
                    "${RxJava3TypeNames.MAYBE.canonicalName}<List<Long>>",
                    InsertOrUpsertFunctionAdapter.ReturnInfo.ID_LIST,
                ),
            )
            .forEach { pair ->
                val dots =
                    if (
                        pair.second in
                            setOf(
                                InsertOrUpsertFunctionAdapter.ReturnInfo.ID_LIST,
                                InsertOrUpsertFunctionAdapter.ReturnInfo.ID_ARRAY,
                                InsertOrUpsertFunctionAdapter.ReturnInfo.ID_ARRAY_BOX,
                            )
                    ) {
                        "..."
                    } else {
                        ""
                    }
                singleInsertUpsertShortcutMethod(
                    """
                @${annotation.java.canonicalName}
                abstract public ${pair.first} foo(User$dots user);
                """
                ) { insertionUpsertion, _ ->
                    assertThat(insertionUpsertion.functionBinder?.adapter).isNotNull()
                }
            }
    }

    abstract fun noAdapter(): String

    abstract fun multiParamAndSingleReturnMismatchError(): String

    abstract fun singleParamAndMultiReturnMismatchError(): String

    @Test
    fun targetEntitySingle() {
        val usernameSource =
            Source.java(
                "foo.bar.Username",
                """
            package foo.bar;
            import androidx.room3.*;

            public class Username {
                int uid;
                String name;
                @ColumnInfo(name = "ageColumn")
                int age;
            }
            """,
            )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(Username username);
            """,
            additionalSources = listOf(usernameSource),
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("foo")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)

            val param = insertionUpsertion.parameters.first()

            assertThat(param.type.asTypeName()).isEqualTo(USERNAME_TYPE_NAME.copy(nullable = true))

            assertThat(param.dataClassType?.asTypeName())
                .isEqualTo(USERNAME_TYPE_NAME.copy(nullable = true))

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["username"]?.isPartialEntity).isEqualTo(true)

            assertThat(insertionUpsertion.entities["username"]?.entityTypeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.entities["username"]?.dataClass?.typeName)
                .isEqualTo(USERNAME_TYPE_NAME)
        }
    }

    @Test
    fun targetEntitySameAsPojo() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(User user);
            """
        ) { _, _ ->
        }
    }

    @Test
    fun targetEntityTwo() {
        val usernameSource =
            Source.java(
                "foo.bar.Username",
                """
            package foo.bar;
            import androidx.room3.*;

            public class Username {
                int uid;
                String name;
                @ColumnInfo(name = "ageColumn")
                int age;
            }
            """,
            )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public void foo(Username usernameA, Username usernameB);
            """,
            additionalSources = listOf(usernameSource),
        ) { _, _ ->
        }
    }

    @Test
    fun targetEntityMissingRequiredColumn() {
        val usernameSource =
            Source.java(
                "foo.bar.Username",
                """
            package foo.bar;
            import androidx.room3.*;

            public class Username {
                int uid;
                String name;
            }
            """,
            )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public void foo(Username username);
            """,
            additionalSources = listOf(usernameSource),
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.missingRequiredColumnsInPartialEntity(
                        partialEntityName = USERNAME_TYPE_NAME.toString(CodeLanguage.JAVA),
                        missingColumnNames = listOf("ageColumn"),
                    )
                )
            }
        }
    }

    @Test
    fun targetEntityColumnDefaultValue() {
        val petNameSource =
            Source.java(
                "foo.bar.PetName",
                """
            package foo.bar;
            import androidx.room3.*;

            public class PetName {
                @ColumnInfo(name = "name")
                String string;
            }
            """,
            )
        val petSource =
            Source.java(
                "foo.bar.Pet",
                """
            package foo.bar;
            import androidx.room3.*;

            @Entity
            public class Pet {
                @PrimaryKey(autoGenerate = true)
                int petId;
                String name;
                @ColumnInfo(defaultValue = "0")
                int age;
            }
            """,
            )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = Pet.class)
                abstract public long foo(PetName petName);
            """,
            additionalSources = listOf(petNameSource, petSource),
        ) { _, _ ->
        }
    }

    @Test
    fun targetEntityMissingPrimaryKey() {
        val petNameSource =
            Source.java(
                "foo.bar.PetName",
                """
            package foo.bar;
            import androidx.room3.*;

            public class PetName {
                @ColumnInfo(name = "name")
                String string;
            }
            """,
            )
        val petSource =
            Source.java(
                "foo.bar.Pet",
                """
            package foo.bar;
            import androidx.room3.*;

            @Entity
            public class Pet {
                @PrimaryKey
                int petId;
                String name;
            }
            """,
            )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = Pet.class)
                abstract public long foo(PetName petName);
            """,
            additionalSources = listOf(petNameSource, petSource),
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(missingPrimaryKey("foo.bar.PetName", listOf("petId")))
            }
        }
    }

    abstract fun missingPrimaryKey(partialEntityName: String, primaryKeyName: List<String>): String

    @Test
    fun targetEntityAutoGeneratedPrimaryKey() {
        val petNameSource =
            Source.java(
                "foo.bar.PetName",
                """
            package foo.bar;
            import androidx.room3.*;

            public class PetName {
                @ColumnInfo(name = "name")
                String string;
            }
            """,
            )
        val petSource =
            Source.java(
                "foo.bar.Pet",
                """
            package foo.bar;
            import androidx.room3.*;

            @Entity
            public class Pet {
                @PrimaryKey(autoGenerate = true)
                int petId;
                String name;
            }
            """,
            )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = Pet.class)
                abstract public long foo(PetName petName);
            """,
            additionalSources = listOf(petNameSource, petSource),
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
            import androidx.room3.*;

            public class Username {
                int uid;
                String name;
                long extraField;
            }
            """,
            )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(Username username);
            """,
            additionalSources = listOf(usernameSource),
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.cannotFindAsEntityProperty("foo.bar.User"))
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
            import androidx.room3.*;

            public class Username {
                int uid;
                String name;
                @ColumnInfo(name = "ageColumn")
                int age;
                @Ignore
                long extraField;
            }
            """,
            )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(Username username);
            """,
            additionalSources = listOf(usernameSource),
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
            import androidx.room3.*;

            public class Username {
                int uid;
                @Embedded
                Fullname name;
                @ColumnInfo(name = "ageColumn")
                int age;
            }
            """,
            )
        val fullnameSource =
            Source.java(
                "foo.bar.Fullname",
                """
            package foo.bar;
            import androidx.room3.*;

            public class Fullname {
                @ColumnInfo(name = "name")
                String firstName;
                String lastName;
            }
            """,
            )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(Username username);
            """,
            additionalSources = listOf(usernameSource, fullnameSource),
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
            import androidx.room3.*;
            import java.util.List;

            public class UserPets {
                int uid;
                @Relation(parentColumn = "uid", entityColumn = "ownerId")
                List<Pet> pets;
            }
            """,
            )
        val petSource =
            Source.java(
                "foo.bar.Pet",
                """
            package foo.bar;
            import androidx.room3.*;

            @Entity
            public class Pet {
                @PrimaryKey
                int petId;
                int ownerId;
            }
            """,
            )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(UserPets userPets);
                """,
            additionalSources = listOf(userPetsSource, petSource),
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_RELATION_IN_PARTIAL_ENTITY)
            }
        }
    }

    @Test
    fun suspendReturnsDeferredType() {
        listOf(
                "${RxJava3TypeNames.FLOWABLE.canonicalName}<Int>",
                "${RxJava3TypeNames.OBSERVABLE.canonicalName}<Int>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<Int>",
                "${RxJava3TypeNames.SINGLE.canonicalName}<Int>",
                "${RxJava3TypeNames.COMPLETABLE.canonicalName}",
                "${LifecyclesTypeNames.LIVE_DATA.canonicalName}<Int>",
                "${LifecyclesTypeNames.COMPUTABLE_LIVE_DATA.canonicalName}<Int>",
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.canonicalName}<Int>",
                "${ReactiveStreamsTypeNames.PUBLISHER.canonicalName}<Int>",
                "${KotlinTypeNames.FLOW.canonicalName}<Int>",
            )
            .forEach { type ->
                singleInsertUpsertShortcutFunction(
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
    fun nonNullVoidGuava() {
        singleInsertUpsertShortcutFunction(
            """
                @${annotation.java.canonicalName}
                abstract fun foo(user: User): ListenableFuture<Void>
                """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorContaining(ProcessorErrors.NONNULL_VOID) }
        }
    }

    abstract fun process(
        baseContext: Context,
        containing: XType,
        executableElement: XMethodElement,
    ): T

    protected fun singleInsertUpsertShortcutMethod(
        vararg input: String,
        additionalSources: List<Source> = emptyList(),
        handler: (T, XTestInvocation) -> Unit,
    ) {
        val inputSource =
            Source.java("foo.bar.MyClass", DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX)
        val commonSources =
            listOf(
                COMMON.USER,
                COMMON.BOOK,
                COMMON.NOT_AN_ENTITY,
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
                COMMON.GUAVA_ROOM,
            )

        runKspTest(sources = commonSources + additionalSources + inputSource) { invocation ->
            val (owner, methods) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map {
                        Pair(
                            it,
                            it.getAllMethods().filter { it.hasAnnotation(annotation) }.toList(),
                        )
                    }
                    .first { it.second.isNotEmpty() }
            val processed =
                process(
                    baseContext = invocation.context.fork(owner),
                    containing = owner.type,
                    executableElement = methods.first(),
                )
            handler(processed, invocation)
        }
    }

    protected fun singleInsertUpsertShortcutFunction(
        vararg input: String,
        additionalSources: List<Source> = emptyList(),
        handler: (T, XTestInvocation) -> Unit,
    ) {
        val inputSource =
            Source.kotlin("MyClass.kt", DAO_PREFIX_KT + input.joinToString("\n") + DAO_SUFFIX)
        val commonSources =
            listOf(
                COMMON.USER,
                COMMON.BOOK,
                COMMON.NOT_AN_ENTITY,
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
                COMMON.GUAVA_ROOM,
            )

        runKspTest(sources = commonSources + additionalSources + inputSource) { invocation ->
            val (owner, functions) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map {
                        Pair(
                            it,
                            it.getAllMethods().filter { it.hasAnnotation(annotation) }.toList(),
                        )
                    }
                    .first { it.second.isNotEmpty() }
            val processed =
                process(
                    baseContext = invocation.context.fork(owner),
                    containing = owner.type,
                    executableElement = functions.first(),
                )
            handler(processed, invocation)
        }
    }
}
