/*
 * Copyright (C) 2016 The Android Open Source Project
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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.typeName
import androidx.room.solver.shortcut.result.InsertMethodAdapter
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.InsertionMethod
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import toJFO
import javax.tools.JavaFileObject

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class InsertionMethodProcessorTest {
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
    fun readNoParams() {
        singleInsertMethod(
                """
                @Insert
                abstract public void foo();
                """) { insertion, _ ->
            assertThat(insertion.name, `is`("foo"))
            assertThat(insertion.parameters.size, `is`(0))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
            assertThat(insertion.entities.size, `is`(0))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.INSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_INSERT)
    }

    @Test
    fun insertSingle() {
        singleInsertMethod(
                """
                @Insert
                abstract public long foo(User user);
                """) { insertion, _ ->
            assertThat(insertion.name, `is`("foo"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(USER_TYPE_NAME))
            assertThat(param.pojoType?.typeName(), `is`(USER_TYPE_NAME))
            assertThat(insertion.entities["user"]?.isPartialEntity, `is`(false))
            assertThat(insertion.entities["user"]?.pojo?.typeName,
                    `is`(ClassName.get("foo.bar", "User") as TypeName))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.LONG))
        }.compilesWithoutError()
    }

    @Test
    fun insertNotAnEntity() {
        singleInsertMethod(
                """
                @Insert
                abstract public void foo(NotAnEntity notValid);
                """) { insertion, _ ->
            assertThat(insertion.name, `is`("foo"))
            assertThat(insertion.parameters.size, `is`(1))
            assertThat(insertion.entities.size, `is`(0))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER
        )
    }

    @Test
    fun insertTwo() {
        singleInsertMethod(
                """
                @Insert
                abstract public void foo(User u1, User u2);
                """) { insertion, _ ->
            assertThat(insertion.name, `is`("foo"))

            assertThat(insertion.parameters.size, `is`(2))
            insertion.parameters.forEach {
                assertThat(it.type.typeName(), `is`(USER_TYPE_NAME))
                assertThat(it.pojoType?.typeName(), `is`(USER_TYPE_NAME))
            }
            assertThat(insertion.entities.size, `is`(2))
            assertThat(insertion.entities["u1"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.entities["u2"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.parameters.map { it.name }, `is`(listOf("u1", "u2")))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
        }.compilesWithoutError()
    }

    @Test
    fun insertList() {
        singleInsertMethod(
                """
                @Insert
                abstract public List<Long> insertUsers(List<User> users);
                """) { insertion, _ ->
            assertThat(insertion.name, `is`("insertUsers"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(
                            ClassName.get("java.util", "List"),
                            USER_TYPE_NAME) as TypeName))
            assertThat(param.pojoType?.typeName(), `is`(USER_TYPE_NAME))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.returnType.typeName(), `is`(
                    ParameterizedTypeName.get(
                            ClassName.get("java.util", "List"),
                            ClassName.get("java.lang", "Long")) as TypeName
            ))
        }.compilesWithoutError()
    }

    @Test
    fun insertArray() {
        singleInsertMethod(
                """
                @Insert
                abstract public void insertUsers(User[] users);
                """) { insertion, _ ->
            assertThat(insertion.name, `is`("insertUsers"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ArrayTypeName.of(COMMON.USER_TYPE_NAME) as TypeName))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
        }.compilesWithoutError()
    }

    @Test
    fun insertSet() {
        singleInsertMethod(
                """
                @Insert
                abstract public void insertUsers(Set<User> users);
                """) { insertion, _ ->
            assertThat(insertion.name, `is`("insertUsers"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(
                            ClassName.get("java.util", "Set"),
                            COMMON.USER_TYPE_NAME) as TypeName))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
        }.compilesWithoutError()
    }

    @Test
    fun insertQueue() {
        singleInsertMethod(
                """
                @Insert
                abstract public void insertUsers(Queue<User> users);
                """) { insertion, _ ->
            assertThat(insertion.name, `is`("insertUsers"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(
                            ClassName.get("java.util", "Queue"),
                            USER_TYPE_NAME) as TypeName))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
        }.compilesWithoutError()
    }

    @Test
    fun insertIterable() {
        singleInsertMethod("""
                @Insert
                abstract public void insert(Iterable<User> users);
                """) { insertion, _ ->
            assertThat(insertion.name, `is`("insert"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(
                            ClassName.get("java.lang", "Iterable"),
                            USER_TYPE_NAME) as TypeName))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
        }.compilesWithoutError()
    }

    @Test
    fun insertCustomCollection() {
        singleInsertMethod("""
                static class MyList<Irrelevant, Item> extends ArrayList<Item> {}
                @Insert
                abstract public void insert(MyList<String, User> users);
                """) { insertion, _ ->
            assertThat(insertion.name, `is`("insert"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(ParameterizedTypeName.get(
                    ClassName.get("foo.bar", "MyClass.MyList"),
                    CommonTypeNames.STRING, USER_TYPE_NAME) as TypeName))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["users"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
        }.compilesWithoutError()
    }

    @Test
    fun insertDifferentTypes() {
        singleInsertMethod(
                """
                @Insert
                abstract public void foo(User u1, Book b1);
                """) { insertion, _ ->
            assertThat(insertion.parameters.size, `is`(2))
            assertThat(insertion.parameters[0].type.typeName().toString(),
                    `is`("foo.bar.User"))
            assertThat(insertion.parameters[1].type.typeName().toString(),
                    `is`("foo.bar.Book"))
            assertThat(insertion.parameters.map { it.name }, `is`(listOf("u1", "b1")))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
            assertThat(insertion.entities.size, `is`(2))
            assertThat(insertion.entities["u1"]?.pojo?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.entities["b1"]?.pojo?.typeName, `is`(BOOK_TYPE_NAME))
        }.compilesWithoutError()
    }

    @Test
    fun onConflict_Default() {
        singleInsertMethod(
                """
                @Insert
                abstract public void foo(User user);
                """) { insertion, _ ->
            assertThat(insertion.onConflict, `is`(OnConflictStrategy.ABORT))
        }.compilesWithoutError()
    }

    @Test
    fun onConflict_Invalid() {
        singleInsertMethod(
                """
                @Insert(onConflict = -1)
                abstract public void foo(User user);
                """) { _, _ ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.INVALID_ON_CONFLICT_VALUE)
    }

    @Test
    fun onConflict_EachValue() {
        listOf(
                Pair("REPLACE", 1),
                Pair("ROLLBACK", 2),
                Pair("ABORT", 3),
                Pair("FAIL", 4),
                Pair("IGNORE", 5)
        ).forEach { pair ->
            singleInsertMethod(
                    """
                @Insert(onConflict=OnConflictStrategy.${pair.first})
                abstract public void foo(User user);
                """) { insertion, _ ->
                assertThat(insertion.onConflict, `is`(pair.second))
            }.compilesWithoutError()
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
            singleInsertMethod(
                    """
                @Insert
                abstract public $type foo(User user);
                """) { insertion, _ ->
                assertThat(insertion.methodBinder.adapter, `is`(nullValue()))
            }.failsToCompile().withErrorContaining(
                    ProcessorErrors.CANNOT_FIND_INSERT_RESULT_ADAPTER)
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
            singleInsertMethod(
                    """
                @Insert
                abstract public $type foo(User user);
                """) { insertion, _ ->
                assertThat(insertion.methodBinder.adapter, `is`(nullValue()))
            }.failsToCompile().withErrorContaining(
                    ProcessorErrors.CANNOT_FIND_INSERT_RESULT_ADAPTER)
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
            singleInsertMethod(
                    """
                @Insert
                abstract public $type foo(User... user);
                """) { insertion, _ ->
                assertThat(insertion.methodBinder.adapter, `is`(nullValue()))
            }.failsToCompile().withErrorContaining(
                    ProcessorErrors.CANNOT_FIND_INSERT_RESULT_ADAPTER)
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
            singleInsertMethod(
                    """
                @Insert
                abstract public $type foo(User user1, User user2);
                """) { insertion, _ ->
                assertThat(insertion.methodBinder.adapter, `is`(nullValue()))
            }.failsToCompile().withErrorContaining(
                    ProcessorErrors.CANNOT_FIND_INSERT_RESULT_ADAPTER)
        }
    }

    @Test
    fun validReturnTypes() {
        listOf(
                Pair("void", InsertMethodAdapter.InsertionType.INSERT_VOID),
                Pair("long", InsertMethodAdapter.InsertionType.INSERT_SINGLE_ID),
                Pair("long[]", InsertMethodAdapter.InsertionType.INSERT_ID_ARRAY),
                Pair("Long[]", InsertMethodAdapter.InsertionType.INSERT_ID_ARRAY_BOX),
                Pair("List<Long>", InsertMethodAdapter.InsertionType.INSERT_ID_LIST),
                Pair(RxJava2TypeNames.COMPLETABLE,
                        InsertMethodAdapter.InsertionType.INSERT_VOID_OBJECT),
                Pair("${RxJava2TypeNames.SINGLE}<Long>",
                        InsertMethodAdapter.InsertionType.INSERT_SINGLE_ID),
                Pair("${RxJava2TypeNames.SINGLE}<List<Long>>",
                        InsertMethodAdapter.InsertionType.INSERT_ID_LIST),
                Pair("${RxJava2TypeNames.MAYBE}<Long>",
                        InsertMethodAdapter.InsertionType.INSERT_SINGLE_ID),
                Pair("${RxJava2TypeNames.MAYBE}<List<Long>>",
                        InsertMethodAdapter.InsertionType.INSERT_ID_LIST)
        ).forEach { pair ->
            val dots = if (pair.second in setOf(
                            InsertMethodAdapter.InsertionType.INSERT_ID_LIST,
                            InsertMethodAdapter.InsertionType.INSERT_ID_ARRAY,
                            InsertMethodAdapter.InsertionType.INSERT_ID_ARRAY_BOX
                    )) {
                "..."
            } else {
                ""
            }
            singleInsertMethod(
                    """
                @Insert
                abstract public ${pair.first} foo(User$dots user);
                """) { insertion, _ ->
                assertThat(insertion.methodBinder.adapter, `is`(notNullValue()))
            }.compilesWithoutError()
        }
    }

    @Test
    fun targetEntitySingle() {
        val usernameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
                @ColumnInfo(name = "ageColumn")
                int age;
            }
        """.toJFO("foo.bar.Username")
        singleInsertMethod(
            """
                @Insert(entity = User.class)
                abstract public long foo(Username username);
                """,
            additionalJFOs = listOf(usernameJfo)) { insertion, _ ->
            assertThat(insertion.name, `is`("foo"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(USERNAME_TYPE_NAME))
            assertThat(param.pojoType?.typeName(), `is`(USERNAME_TYPE_NAME))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["username"]?.isPartialEntity, `is`(true))
            assertThat(insertion.entities["username"]?.entityTypeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.entities["username"]?.pojo?.typeName, `is`(USERNAME_TYPE_NAME))
        }.compilesWithoutError()
    }

    @Test
    fun targetEntitySameAsPojo() {
        singleInsertMethod(
            """
                @Insert(entity = User.class)
                abstract public long foo(User user);
                """) { _, _ ->
        }.compilesWithoutError()
    }

    @Test
    fun targetEntityTwo() {
        val usernameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
                @ColumnInfo(name = "ageColumn")
                int age;
            }
        """.toJFO("foo.bar.Username")
        singleInsertMethod(
            """
                @Insert(entity = User.class)
                abstract public void foo(Username usernameA, Username usernameB);
                """,
            additionalJFOs = listOf(usernameJfo)) { _, _ ->
        }.compilesWithoutError()
    }

    @Test
    fun targetEntityMissingRequiredColumn() {
        val usernameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class Username {
                int uid;
                String name;
            }
        """.toJFO("foo.bar.Username")
        singleInsertMethod(
            """
                @Insert(entity = User.class)
                abstract public void foo(Username username);
                """,
            additionalJFOs = listOf(usernameJfo)) { _, _ ->
        }.failsToCompile().withErrorContaining(
            ProcessorErrors.missingRequiredColumnsInPartialEntity(
                partialEntityName = USERNAME_TYPE_NAME.toString(),
                missingColumnNames = listOf("ageColumn"))
        )
    }

    @Test
    fun targetEntityColumnDefaultValue() {
        val petNameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class PetName {
                @ColumnInfo(name = "name")
                String string;
            }
        """.toJFO("foo.bar.PetName")
        val petJfo = """
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
        """.toJFO("foo.bar.Pet")
        singleInsertMethod(
            """
                @Insert(entity = Pet.class)
                abstract public long foo(PetName petName);
                """,
            additionalJFOs = listOf(petNameJfo, petJfo)) { _, _ ->
        }.compilesWithoutError()
    }

    @Test
    fun targetEntityMissingPrimaryKey() {
        val petNameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class PetName {
                @ColumnInfo(name = "name")
                String string;
            }
        """.toJFO("foo.bar.PetName")
        val petJfo = """
            package foo.bar;
            import androidx.room.*;

            @Entity
            public class Pet {
                @PrimaryKey
                int petId;
                String name;
            }
        """.toJFO("foo.bar.Pet")
        singleInsertMethod(
            """
                @Insert(entity = Pet.class)
                abstract public long foo(PetName petName);
                """,
            additionalJFOs = listOf(petNameJfo, petJfo)) { _, _ ->
        }.failsToCompile().withErrorContaining(
            ProcessorErrors.missingPrimaryKeysInPartialEntityForInsert(
                partialEntityName = "foo.bar.PetName",
                primaryKeyNames = listOf("petId"))
        )
    }

    @Test
    fun targetEntityAutoGeneratedPrimaryKey() {
        val petNameJfo = """
            package foo.bar;
            import androidx.room.*;

            public class PetName {
                @ColumnInfo(name = "name")
                String string;
            }
        """.toJFO("foo.bar.PetName")
        val petJfo = """
            package foo.bar;
            import androidx.room.*;

            @Entity
            public class Pet {
                @PrimaryKey(autoGenerate = true)
                int petId;
                String name;
            }
        """.toJFO("foo.bar.Pet")
        singleInsertMethod(
            """
                @Insert(entity = Pet.class)
                abstract public long foo(PetName petName);
                """,
            additionalJFOs = listOf(petNameJfo, petJfo)) { _, _ ->
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
        singleInsertMethod(
            """
                @Insert(entity = User.class)
                abstract public long foo(Username username);
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
                @ColumnInfo(name = "ageColumn")
                int age;
                @Ignore
                long extraField;
            }
        """.toJFO("foo.bar.Username")
        singleInsertMethod(
            """
                @Insert(entity = User.class)
                abstract public long foo(Username username);
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
                @ColumnInfo(name = "ageColumn")
                int age;
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
        singleInsertMethod(
            """
                @Insert(entity = User.class)
                abstract public long foo(Username username);
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
        singleInsertMethod(
            """
                @Insert(entity = User.class)
                abstract public long foo(UserPets userPets);
                """,
            additionalJFOs = listOf(userPetsJfo, petJfo)) { _, _ ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.INVALID_RELATION_IN_PARTIAL_ENTITY)
    }

    fun singleInsertMethod(
        vararg input: String,
        additionalJFOs: List<JavaFileObject> = emptyList(),
        handler: (InsertionMethod, TestInvocation) -> Unit
    ): CompileTester {
        return assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyClass",
                        DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX),
                        COMMON.USER, COMMON.BOOK, COMMON.NOT_AN_ENTITY,
                        COMMON.COMPLETABLE, COMMON.MAYBE, COMMON.SINGLE) + additionalJFOs
                )
                .processedWith(TestProcessor.builder()
                        .forAnnotations(Insert::class, Dao::class)
                        .nextRunHandler { invocation ->
                            val (owner, methods) = invocation.roundEnv
                                    .getElementsAnnotatedWith(Dao::class.java)
                                    .map {
                                        Pair(it,
                                                invocation.processingEnv.elementUtils
                                                        .getAllMembers(MoreElements.asType(it))
                                                        .filter {
                                                            MoreElements.isAnnotationPresent(it,
                                                                    Insert::class.java)
                                                        }
                                        )
                                    }.first { it.second.isNotEmpty() }
                            val processor = InsertionMethodProcessor(
                                    baseContext = invocation.context,
                                    containing = MoreTypes.asDeclared(owner.asType()),
                                    executableElement = MoreElements.asExecutable(methods.first()))
                            val processed = processor.process()
                            handler(processed, invocation)
                            true
                        }
                        .build())
    }
}
