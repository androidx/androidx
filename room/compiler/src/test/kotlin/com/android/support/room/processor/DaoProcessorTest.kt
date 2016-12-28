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

package com.android.support.room.processor

import COMMON
import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.android.support.room.vo.Dao
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DaoProcessorTest {
    companion object {
        const val DAO_PREFIX = """
            package foo.bar;
            import com.android.support.room.*;
            """
    }

    @Test
    fun testNonAbstract() {
        singleDao("@Dao public class MyDao {}") { dao, invocation -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE)
    }

    @Test
    fun testAbstractMethodWithoutQuery() {
        singleDao("""
                @Dao public interface MyDao {
                    int getFoo();
                }
        """) { dao, invocation ->
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.ABSTRACT_METHOD_IN_DAO_MISSING_ANY_ANNOTATION)
    }

    @Test
    fun testBothAnnotations() {
        singleDao("""
                @Dao public interface MyDao {
                    @Query("select 1")
                    @Insert
                    int getFoo(int x);
                }
        """) { dao, invocation ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.CANNOT_USE_MORE_THAN_ONE_DAO_METHOD_ANNOTATION)
    }

    @Test
    fun testAbstractClass() {
        singleDao("""
                @Dao abstract class MyDao {
                    @Query("SELECT id FROM users")
                    abstract int[] getIds();
                }
                """) { dao, invocation ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
        }.compilesWithoutError()
    }

    @Test
    fun testInterface() {
        singleDao("""
                @Dao interface MyDao {
                    @Query("SELECT id FROM users")
                    abstract int[] getIds();
                }
                """) { dao, invocation ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
        }.compilesWithoutError()
    }

    @Test
    fun testWithInsertAndQuery() {
        singleDao("""
                @Dao abstract class MyDao {
                    @Query("SELECT id FROM users")
                    abstract int[] getIds();
                    @Insert
                    abstract void insert(User user);
                }
                """) { dao, invocation ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
            assertThat(dao.insertionMethods.size, `is`(1))
            val insertMethod = dao.insertionMethods.first()
            assertThat(insertMethod.name, `is`("insert"))
        }.compilesWithoutError()
    }

    fun singleDao(vararg inputs: String, handler: (Dao, TestInvocation) -> Unit):
            CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyDao",
                        DAO_PREFIX + inputs.joinToString("\n")
                ), COMMON.USER))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(com.android.support.room.Dao::class)
                        .nextRunHandler { invocation ->
                            val entity = invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            com.android.support.room.Dao::class.java)
                                    .first()
                            val parser = DaoProcessor(invocation.context)
                            val parsedDao = parser.parse(MoreElements.asType(entity))
                            handler(parsedDao, invocation)
                            true
                        }
                        .build())
    }
}
