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
import androidx.room.ext.typeName
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.InsertionMethod
import androidx.room.vo.InsertionMethod.Type
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
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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
            assertThat(param.entityType?.typeName(), `is`(USER_TYPE_NAME))
            assertThat(insertion.entities["user"]?.typeName,
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
            val param = insertion.parameters.first()
            assertThat(param.entityType, `is`(nullValue()))
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
                assertThat(it.entityType?.typeName(), `is`(USER_TYPE_NAME))
            }
            assertThat(insertion.entities.size, `is`(2))
            assertThat(insertion.entities["u1"]?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.entities["u2"]?.typeName, `is`(USER_TYPE_NAME))
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
                            ClassName.get("java.util", "List"), USER_TYPE_NAME) as TypeName))
            assertThat(param.entityType?.typeName(), `is`(USER_TYPE_NAME))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["users"]?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.returnType.typeName(), `is`(
                    ParameterizedTypeName.get(ClassName.get("java.util", "List"),
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
            assertThat(insertion.entities["users"]?.typeName, `is`(USER_TYPE_NAME))
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
                    ParameterizedTypeName.get(ClassName.get("java.util", "Set")
                            , COMMON.USER_TYPE_NAME) as TypeName))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["users"]?.typeName, `is`(USER_TYPE_NAME))
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
                    ParameterizedTypeName.get(ClassName.get("java.util", "Queue")
                            , USER_TYPE_NAME) as TypeName))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["users"]?.typeName, `is`(USER_TYPE_NAME))
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
            assertThat(param.type.typeName(), `is`(ParameterizedTypeName.get(
                    ClassName.get("java.lang", "Iterable"), USER_TYPE_NAME) as TypeName))
            assertThat(insertion.entities.size, `is`(1))
            assertThat(insertion.entities["users"]?.typeName, `is`(USER_TYPE_NAME))
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
            assertThat(insertion.entities["users"]?.typeName, `is`(USER_TYPE_NAME))
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
            assertThat(insertion.entities["u1"]?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.entities["b1"]?.typeName, `is`(BOOK_TYPE_NAME))
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
        singleInsertMethod(
                """
                @Insert
                abstract public int foo(User user);
                """) { insertion, _ ->
            assertThat(insertion.insertionType, `is`(nullValue()))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.INVALID_INSERTION_METHOD_RETURN_TYPE)
    }

    @Test
    fun mismatchedReturnType() {
        singleInsertMethod(
                """
                @Insert
                abstract public long[] foo(User user);
                """) { insertion, _ ->
            assertThat(insertion.insertionType, `is`(nullValue()))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.insertionMethodReturnTypeMismatch(
                        ArrayTypeName.of(TypeName.LONG),
                        InsertionMethodProcessor.SINGLE_ITEM_SET.map { it.returnTypeName }))
    }

    @Test
    fun mismatchedReturnType2() {
        singleInsertMethod(
                """
                @Insert
                abstract public long foo(User... user);
                """) { insertion, _ ->
            assertThat(insertion.insertionType, `is`(nullValue()))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.insertionMethodReturnTypeMismatch(
                        TypeName.LONG,
                        InsertionMethodProcessor.MULTIPLE_ITEM_SET.map { it.returnTypeName }))
    }

    @Test
    fun mismatchedReturnType3() {
        singleInsertMethod(
                """
                @Insert
                abstract public long foo(User user1, User user2);
                """) { insertion, _ ->
            assertThat(insertion.insertionType, `is`(nullValue()))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.insertionMethodReturnTypeMismatch(
                        TypeName.LONG,
                        InsertionMethodProcessor.VOID_SET.map { it.returnTypeName }))
    }

    @Test
    fun validReturnTypes() {
        listOf(
                Pair("void", Type.INSERT_VOID),
                Pair("long", Type.INSERT_SINGLE_ID),
                Pair("long[]", Type.INSERT_ID_ARRAY),
                Pair("Long[]", Type.INSERT_ID_ARRAY_BOX),
                Pair("List<Long>", Type.INSERT_ID_LIST)
        ).forEach { pair ->
            val dots = if (pair.second in setOf(Type.INSERT_ID_LIST, Type.INSERT_ID_ARRAY,
                    Type.INSERT_ID_ARRAY_BOX)) {
                "..."
            } else {
                ""
            }
            singleInsertMethod(
                    """
                @Insert
                abstract public ${pair.first} foo(User$dots user);
                """) { insertion, _ ->
                assertThat(insertion.insertMethodTypeFor(insertion.parameters.first()),
                        `is`(pair.second))
                assertThat(pair.toString(), insertion.insertionType, `is`(pair.second))
            }.compilesWithoutError()
        }
    }

    fun singleInsertMethod(
            vararg input: String,
            handler: (InsertionMethod, TestInvocation) -> Unit
    ): CompileTester {
        return assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyClass",
                        DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX
                ), COMMON.USER, COMMON.BOOK, COMMON.NOT_AN_ENTITY))
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
