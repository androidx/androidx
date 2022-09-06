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
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.solver.shortcut.result.InsertOrUpsertMethodAdapter
import androidx.room.testing.context
import androidx.room.vo.InsertOrUpsertShortcutMethod
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import kotlin.reflect.KClass
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ParameterizedTypeName
import org.junit.Test

/**
 * Base test class for insert and upsert methods.
 */
abstract class InsertOrUpsertShortcutMethodProcessorTest <out T : InsertOrUpsertShortcutMethod>(
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
        const val DAO_PREFIX_KT = """
                package foo.bar
                import androidx.room.*
                import java.util.*
                import io.reactivex.*
                io.reactivex.rxjava3.core.*
                androidx.lifecycle.*
                com.google.common.util.concurrent.*
                org.reactivestreams.*
                kotlinx.coroutines.flow.*

                @Dao
                abstract class MyClass {
                """
        const val DAO_SUFFIX = "}"
        val USER_TYPE_NAME: TypeName = COMMON.USER_TYPE_NAME
        val USERNAME_TYPE_NAME: TypeName = ClassName.get("foo.bar", "Username")
        val BOOK_TYPE_NAME: TypeName = ClassName.get("foo.bar", "Book")
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
            assertThat(insertionUpsertion.returnType.typeName).isEqualTo(TypeName.VOID)
            assertThat(insertionUpsertion.entities.size).isEqualTo(0)
            invocation.assertCompilationResult {
                hasErrorContaining(noParamsError())
            }
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
                hasErrorContaining(
                    ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER
                )
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
            assertThat(param.type.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(param.pojoType?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.entities["user"]?.isPartialEntity)
                .isEqualTo(false)

            assertThat(insertionUpsertion.entities["user"]?.pojo?.typeName)
                .isEqualTo(ClassName.get("foo.bar", "User") as TypeName)

            assertThat(insertionUpsertion.returnType.typeName)
                .isEqualTo(TypeName.LONG)
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
                assertThat(it.type.typeName).isEqualTo(USER_TYPE_NAME)
                assertThat(it.pojoType?.typeName).isEqualTo(USER_TYPE_NAME)
            }
            assertThat(insertionUpsertion.entities.size)
                .isEqualTo(2)

            assertThat(insertionUpsertion.entities["u1"]?.pojo?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.entities["u2"]?.pojo?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.parameters.map { it.name })
                .isEqualTo(listOf("u1", "u2"))

            assertThat(insertionUpsertion.returnType.typeName)
                .isEqualTo(TypeName.VOID)
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
            assertThat(param.type.typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(
                        ClassName.get("java.util", "List"),
                        USER_TYPE_NAME
                    ) as TypeName
                )

            assertThat(param.pojoType?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.pojo?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(
                        ClassName.get("java.util", "List"),
                        ClassName.get("java.lang", "Long")
                    ) as TypeName
                )
        }
    }

    @Test
    fun array() {
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}
                abstract public void insertUsers(User[] users);
                """
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("insertUsers")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)
            val param = insertionUpsertion.parameters.first()
            assertThat(param.type.typeName)
                .isEqualTo(ArrayTypeName.of(COMMON.USER_TYPE_NAME) as TypeName)

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.pojo?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.typeName)
                .isEqualTo(TypeName.VOID)
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
            assertThat(param.type.typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(
                        ClassName.get("java.util", "Set"),
                        COMMON.USER_TYPE_NAME
                    ) as TypeName
                )

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.pojo?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.typeName).isEqualTo(TypeName.VOID)
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
            assertThat(param.type.typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(
                        ClassName.get("java.util", "Queue"),
                        USER_TYPE_NAME
                    ) as TypeName
                )

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.pojo?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.typeName).isEqualTo(TypeName.VOID)
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
            assertThat(param.type.typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(
                        ClassName.get("java.lang", "Iterable"),
                        USER_TYPE_NAME
                    ) as TypeName
                )

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.pojo?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.typeName).isEqualTo(TypeName.VOID)
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
            assertThat(param.type.typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(
                        ClassName.get("foo.bar", "MyClass.MyList"),
                        CommonTypeNames.STRING, USER_TYPE_NAME
                    ) as TypeName
                )

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["users"]?.pojo?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.returnType.typeName).isEqualTo(TypeName.VOID)
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
            assertThat(insertionUpsertion.parameters[0].type.typeName.toString())
                .isEqualTo("foo.bar.User")

            assertThat(insertionUpsertion.parameters[1].type.typeName.toString())
                .isEqualTo("foo.bar.Book")

            assertThat(insertionUpsertion.parameters.map { it.name }).isEqualTo(listOf("u1", "b1"))

            assertThat(insertionUpsertion.returnType.typeName).isEqualTo(TypeName.VOID)

            assertThat(insertionUpsertion.entities.size).isEqualTo(2)

            assertThat(insertionUpsertion.entities["u1"]?.pojo?.typeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.entities["b1"]?.pojo?.typeName)
                .isEqualTo(BOOK_TYPE_NAME)
        }
    }

    @Test
    fun invalidReturnType() {
        listOf(
            "int",
            "${RxJava2TypeNames.SINGLE}<Int>",
            "${RxJava2TypeNames.MAYBE}<Int>",
            "${RxJava2TypeNames.SINGLE}<String>",
            "${RxJava2TypeNames.MAYBE}<String>",
            "${RxJava2TypeNames.SINGLE}<User>",
            "${RxJava2TypeNames.MAYBE}<User>"
        ).forEach { type ->
            singleInsertUpsertShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public $type foo(User user);
                """
            ) { insertionUpsertion, invocation ->

                assertThat(insertionUpsertion.methodBinder?.adapter).isNull()

                invocation.assertCompilationResult {
                    hasErrorContaining(noAdapter())
                }
            }
        }
    }

    @Test
    fun mismatchedReturnType() {
        listOf(
            "long[]",
            "Long[]",
            "List<Long>",
            "${RxJava2TypeNames.SINGLE}<List<Long>>",
            "${RxJava2TypeNames.MAYBE}<List<Long>>"
        ).forEach { type ->
            singleInsertUpsertShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public $type foo(User user);
                """
            ) { insertionUpsertion, invocation ->

                assertThat(insertionUpsertion.methodBinder?.adapter).isNull()

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
            "${RxJava2TypeNames.SINGLE}<Long>",
            "${RxJava2TypeNames.MAYBE}<Long>"
        ).forEach { type ->
            singleInsertUpsertShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public $type foo(User... user);
                """
            ) { insertionUpsertion, invocation ->
                assertThat(insertionUpsertion.methodBinder?.adapter).isNull()

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
            "${RxJava2TypeNames.SINGLE}<Long>",
            "${RxJava2TypeNames.MAYBE}<Long>"
        ).forEach { type ->
            singleInsertUpsertShortcutMethod(
                """
                @${annotation.java.canonicalName}
                abstract public $type foo(User user1, User user2);
                """
            ) { insertionUpsertion, invocation ->
                assertThat(insertionUpsertion.methodBinder?.adapter).isNull()

                invocation.assertCompilationResult {
                    hasErrorContaining(noAdapter())
                }
            }
        }
    }

    @Test
    fun validReturnTypes() {
        listOf(
            Pair("void", InsertOrUpsertMethodAdapter.ReturnType.VOID),
            Pair("long", InsertOrUpsertMethodAdapter.ReturnType.SINGLE_ID),
            Pair("long[]", InsertOrUpsertMethodAdapter.ReturnType.ID_ARRAY),
            Pair("Long[]", InsertOrUpsertMethodAdapter.ReturnType.ID_ARRAY_BOX),
            Pair("List<Long>", InsertOrUpsertMethodAdapter.ReturnType.ID_LIST),
            Pair(
                RxJava2TypeNames.COMPLETABLE,
                InsertOrUpsertMethodAdapter.ReturnType.VOID_OBJECT
            ),
            Pair(
                "${RxJava2TypeNames.SINGLE}<Long>",
                InsertOrUpsertMethodAdapter.ReturnType.SINGLE_ID
            ),
            Pair(
                "${RxJava2TypeNames.SINGLE}<List<Long>>",
                InsertOrUpsertMethodAdapter.ReturnType.ID_LIST
            ),
            Pair(
                "${RxJava2TypeNames.MAYBE}<Long>",
                InsertOrUpsertMethodAdapter.ReturnType.SINGLE_ID
            ),
            Pair(
                "${RxJava2TypeNames.MAYBE}<List<Long>>",
                InsertOrUpsertMethodAdapter.ReturnType.ID_LIST
            ),
            Pair(
                RxJava3TypeNames.COMPLETABLE,
                InsertOrUpsertMethodAdapter.ReturnType.VOID_OBJECT
            ),
            Pair(
                "${RxJava3TypeNames.SINGLE}<Long>",
                InsertOrUpsertMethodAdapter.ReturnType.SINGLE_ID
            ),
            Pair(
                "${RxJava3TypeNames.SINGLE}<List<Long>>",
                InsertOrUpsertMethodAdapter.ReturnType.ID_LIST
            ),
            Pair(
                "${RxJava3TypeNames.MAYBE}<Long>",
                InsertOrUpsertMethodAdapter.ReturnType.SINGLE_ID
            ),
            Pair(
                "${RxJava3TypeNames.MAYBE}<List<Long>>",
                InsertOrUpsertMethodAdapter.ReturnType.ID_LIST
            )
        ).forEach { pair ->
            val dots = if (pair.second in setOf(
                    InsertOrUpsertMethodAdapter.ReturnType.ID_LIST,
                    InsertOrUpsertMethodAdapter.ReturnType.ID_ARRAY,
                    InsertOrUpsertMethodAdapter.ReturnType.ID_ARRAY_BOX
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
                assertThat(insertionUpsertion.methodBinder?.adapter).isNotNull()
            }
        }
    }

    abstract fun noAdapter(): String

    abstract fun multiParamAndSingleReturnMismatchError(): String

    abstract fun singleParamAndMultiReturnMismatchError(): String

    @Test
    fun targetEntitySingle() {
        val usernameSource = Source.java(
            "foo.bar.Username",
            """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
                @ColumnInfo(name = "ageColumn")
                int age;
            }
            """
        )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(Username username);
            """,
            additionalSources = listOf(usernameSource)
        ) { insertionUpsertion, _ ->
            assertThat(insertionUpsertion.element.jvmName).isEqualTo("foo")
            assertThat(insertionUpsertion.parameters.size).isEqualTo(1)

            val param = insertionUpsertion.parameters.first()

            assertThat(param.type.typeName).isEqualTo(USERNAME_TYPE_NAME)

            assertThat(param.pojoType?.typeName).isEqualTo(USERNAME_TYPE_NAME)

            assertThat(insertionUpsertion.entities.size).isEqualTo(1)

            assertThat(insertionUpsertion.entities["username"]?.isPartialEntity)
                .isEqualTo(true)

            assertThat(insertionUpsertion.entities["username"]?.entityTypeName)
                .isEqualTo(USER_TYPE_NAME)

            assertThat(insertionUpsertion.entities["username"]?.pojo?.typeName)
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
        val usernameSource = Source.java(
            "foo.bar.Username",
            """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
                @ColumnInfo(name = "ageColumn")
                int age;
            }
            """
        )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public void foo(Username usernameA, Username usernameB);
            """,
            additionalSources = listOf(usernameSource)
        ) { _, _ ->
        }
    }

    @Test
    fun targetEntityMissingRequiredColumn() {
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
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public void foo(Username username);
            """,
            additionalSources = listOf(usernameSource)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.missingRequiredColumnsInPartialEntity(
                        partialEntityName = USERNAME_TYPE_NAME.toString(),
                        missingColumnNames = listOf("ageColumn")
                    )
                )
            }
        }
    }

    @Test
    fun targetEntityColumnDefaultValue() {
        val petNameSource = Source.java(
            "foo.bar.PetName",
            """
            package foo.bar;
            import androidx.room.*;

            public class PetName {
                @ColumnInfo(name = "name")
                String string;
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
                @PrimaryKey(autoGenerate = true)
                int petId;
                String name;
                @ColumnInfo(defaultValue = "0")
                int age;
            }
            """
        )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = Pet.class)
                abstract public long foo(PetName petName);
            """,
            additionalSources = listOf(petNameSource, petSource)
        ) { _, _ ->
        }
    }

    @Test
    fun targetEntityMissingPrimaryKey() {
        val petNameSource = Source.java(
            "foo.bar.PetName",
            """
            package foo.bar;
            import androidx.room.*;

            public class PetName {
                @ColumnInfo(name = "name")
                String string;
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
                String name;
            }
            """
        )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = Pet.class)
                abstract public long foo(PetName petName);
            """,
            additionalSources = listOf(petNameSource, petSource)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(missingPrimaryKey(
                    "foo.bar.PetName",
                    listOf("petId"))
                )
            }
        }
    }

    abstract fun missingPrimaryKey(partialEntityName: String, primaryKeyName: List<String>):
        String

    @Test
    fun targetEntityAutoGeneratedPrimaryKey() {
        val petNameSource = Source.java(
            "foo.bar.PetName",
            """
            package foo.bar;
            import androidx.room.*;

            public class PetName {
                @ColumnInfo(name = "name")
                String string;
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
                @PrimaryKey(autoGenerate = true)
                int petId;
                String name;
            }
            """
        )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = Pet.class)
                abstract public long foo(PetName petName);
            """,
            additionalSources = listOf(petNameSource, petSource)
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
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(Username username);
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
                @ColumnInfo(name = "ageColumn")
                int age;
                @Ignore
                long extraField;
            }
            """
        )
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(Username username);
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
                @ColumnInfo(name = "ageColumn")
                int age;
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
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(Username username);
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
        singleInsertUpsertShortcutMethod(
            """
                @${annotation.java.canonicalName}(entity = User.class)
                abstract public long foo(UserPets userPets);
                """,
            additionalSources = listOf(userPetsSource, petSource)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.INVALID_RELATION_IN_PARTIAL_ENTITY
                )
            }
        }
    }

    @Test
    fun suspendReturnsDeferredType() {
        listOf(
            "${RxJava2TypeNames.FLOWABLE}<Int>",
            "${RxJava2TypeNames.OBSERVABLE}<Int>",
            "${RxJava2TypeNames.MAYBE}<Int>",
            "${RxJava2TypeNames.SINGLE}<Int>",
            "${RxJava2TypeNames.COMPLETABLE}",
            "${RxJava3TypeNames.FLOWABLE}<Int>",
            "${RxJava3TypeNames.OBSERVABLE}<Int>",
            "${RxJava3TypeNames.MAYBE}<Int>",
            "${RxJava3TypeNames.SINGLE}<Int>",
            "${RxJava3TypeNames.COMPLETABLE}",
            "${LifecyclesTypeNames.LIVE_DATA}<Int>",
            "${LifecyclesTypeNames.COMPUTABLE_LIVE_DATA}<Int>",
            "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE}<Int>",
            "${ReactiveStreamsTypeNames.PUBLISHER}<Int>",
            "${KotlinTypeNames.FLOW}<Int>"
        ).forEach { type ->
            singleInsertUpsertShortcutMethodKotlin(
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

    abstract fun process(
        baseContext: Context,
        containing: XType,
        executableElement: XMethodElement
    ): T

    fun singleInsertUpsertShortcutMethod(
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
            COMMON.RX3_MAYBE, COMMON.RX3_SINGLE
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

    fun singleInsertUpsertShortcutMethodKotlin(
        vararg input: String,
        additionalSources: List<Source> = emptyList(),
        handler: (T, XTestInvocation) -> Unit
    ) {
        val inputSource = Source.kotlin(
            "MyClass.kt",
            DAO_PREFIX_KT + input.joinToString("\n") + DAO_SUFFIX
        )
        val commonSources = listOf(
            COMMON.USER, COMMON.BOOK, COMMON.NOT_AN_ENTITY, COMMON.RX2_COMPLETABLE,
            COMMON.RX2_MAYBE, COMMON.RX2_SINGLE, COMMON.RX2_FLOWABLE, COMMON.RX2_OBSERVABLE,
            COMMON.RX3_COMPLETABLE, COMMON.RX3_MAYBE, COMMON.RX3_SINGLE, COMMON.RX3_FLOWABLE,
            COMMON.RX3_OBSERVABLE, COMMON.LISTENABLE_FUTURE, COMMON.LIVE_DATA,
            COMMON.COMPUTABLE_LIVE_DATA, COMMON.PUBLISHER, COMMON.FLOW, COMMON.GUAVA_ROOM
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
