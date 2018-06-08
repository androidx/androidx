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
import androidx.room.ext.RoomTypeNames
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.Dao
import androidx.room.vo.Warning
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import createVerifierFromEntities
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DaoProcessorTest(val enableVerification: Boolean) {
    companion object {
        const val DAO_PREFIX = """
            package foo.bar;
            import androidx.room.*;
            """
        @Parameterized.Parameters(name = "enableDbVerification={0}")
        @JvmStatic
        fun getParams() = arrayOf(true, false)
    }

    @Test
    fun testNonAbstract() {
        singleDao("@Dao public class MyDao {}") { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE)
    }

    @Test
    fun testAbstractMethodWithoutQuery() {
        singleDao("""
                @Dao public interface MyDao {
                    int getFoo();
                }
        """) { _, _ ->
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
        """) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD)
    }

    @Test
    fun testAbstractClass() {
        singleDao("""
                @Dao abstract class MyDao {
                    @Query("SELECT uid FROM User")
                    abstract int[] getIds();
                }
                """) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
        }.compilesWithoutError()
    }

    @Test
    fun testInterface() {
        singleDao("""
                @Dao interface MyDao {
                    @Query("SELECT uid FROM User")
                    abstract int[] getIds();
                }
                """) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
        }.compilesWithoutError()
    }

    @Test
    fun testWithInsertAndQuery() {
        singleDao("""
                @Dao abstract class MyDao {
                    @Query("SELECT uid FROM User")
                    abstract int[] getIds();
                    @Insert
                    abstract void insert(User user);
                }
                """) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
            assertThat(dao.insertionMethods.size, `is`(1))
            val insertMethod = dao.insertionMethods.first()
            assertThat(insertMethod.name, `is`("insert"))
        }.compilesWithoutError()
    }

    @Test
    fun skipQueryVerification() {
        singleDao("""
                @Dao @SkipQueryVerification interface MyDao {
                    @Query("SELECT nonExistingField FROM User")
                    abstract int[] getIds();
                }
                """) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
        }.compilesWithoutError()
    }

    @Test
    fun suppressedWarnings() {
        singleDao("""
            @SuppressWarnings({"ALL", RoomWarnings.CURSOR_MISMATCH})
            @Dao interface MyDao {
                @Query("SELECT * from user")
                abstract User users();
            }
            """) { dao, invocation ->
            val dbType = MoreTypes.asDeclared(invocation.context.processingEnv.elementUtils
                    .getTypeElement(RoomTypeNames.ROOM_DB.toString()).asType())
            val daoProcessor = DaoProcessor(invocation.context, dao.element, dbType, null)
            assertThat(daoProcessor.context.logger
                    .suppressedWarnings, `is`(setOf(Warning.ALL, Warning.CURSOR_MISMATCH)))

            dao.queryMethods.forEach {
                assertThat(QueryMethodProcessor(
                        baseContext = daoProcessor.context,
                        containing = MoreTypes.asDeclared(dao.element.asType()),
                        executableElement = it.element,
                        dbVerifier = null).context.logger.suppressedWarnings,
                        `is`(setOf(Warning.ALL, Warning.CURSOR_MISMATCH)))
            }
        }.compilesWithoutError()
    }

    @Test
    fun suppressedWarningsInheritance() {
        singleDao("""
            @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
            @Dao interface MyDao {
                @SuppressWarnings("ALL")
                @Query("SELECT * from user")
                abstract User users();
            }
            """) { dao, invocation ->
            val dbType = MoreTypes.asDeclared(invocation.context.processingEnv.elementUtils
                    .getTypeElement(RoomTypeNames.ROOM_DB.toString()).asType())
            val daoProcessor = DaoProcessor(invocation.context, dao.element, dbType, null)
            assertThat(daoProcessor.context.logger
                    .suppressedWarnings, `is`(setOf(Warning.CURSOR_MISMATCH)))

            dao.queryMethods.forEach {
                assertThat(QueryMethodProcessor(
                        baseContext = daoProcessor.context,
                        containing = MoreTypes.asDeclared(dao.element.asType()),
                        executableElement = it.element,
                        dbVerifier = null).context.logger.suppressedWarnings,
                        `is`(setOf(Warning.ALL, Warning.CURSOR_MISMATCH)))
            }
        }.compilesWithoutError()
    }

    @Test
    fun query_warnIfTransactionIsMissingForRelation() {
        if (!enableVerification) {
            return
        }
        singleDao(
                """
                @Dao interface MyDao {
                    static class Merged extends User {
                       @Relation(parentColumn = "name", entityColumn = "lastName",
                                 entity = User.class)
                       java.util.List<User> users;
                    }
                    @Query("select * from user")
                    abstract java.util.List<Merged> loadUsers();
                }
                """
        ) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            assertThat(dao.queryMethods.first().inTransaction, `is`(false))
        }.compilesWithoutError()
                .withWarningContaining(ProcessorErrors.TRANSACTION_MISSING_ON_RELATION)
    }

    @Test
    fun query_dontWarnIfTransactionIsMissingForRelation_suppressed() {
        if (!enableVerification) {
            return
        }
        singleDao(
                """
                @Dao interface MyDao {
                    static class Merged extends User {
                       @Relation(parentColumn = "name", entityColumn = "lastName",
                                 entity = User.class)
                       java.util.List<User> users;
                    }
                    @SuppressWarnings(RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION)
                    @Query("select * from user")
                    abstract java.util.List<Merged> loadUsers();
                }
                """
        ) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            assertThat(dao.queryMethods.first().inTransaction, `is`(false))
        }.compilesWithoutError()
                .withWarningCount(0)
    }

    @Test
    fun query_dontWarnIfTransactionNotIsMissingForRelation() {
        if (!enableVerification) {
            return
        }
        singleDao(
                """
                @Dao interface MyDao {
                    static class Merged extends User {
                       @Relation(parentColumn = "name", entityColumn = "lastName",
                                 entity = User.class)
                       java.util.List<User> users;
                    }
                    @Transaction
                    @Query("select * from user")
                    abstract java.util.List<Merged> loadUsers();
                }
                """
        ) { dao, _ ->
            // test sanity
            assertThat(dao.queryMethods.size, `is`(1))
            assertThat(dao.queryMethods.first().inTransaction, `is`(true))
        }.compilesWithoutError()
                .withWarningCount(0)
    }

    fun singleDao(vararg inputs: String, handler: (Dao, TestInvocation) -> Unit):
            CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyDao",
                        DAO_PREFIX + inputs.joinToString("\n")
                ), COMMON.USER))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(androidx.room.Dao::class,
                                androidx.room.Entity::class,
                                androidx.room.Relation::class,
                                androidx.room.Transaction::class,
                                androidx.room.ColumnInfo::class,
                                androidx.room.PrimaryKey::class,
                                androidx.room.Query::class)
                        .nextRunHandler { invocation ->
                            val dao = invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            androidx.room.Dao::class.java)
                                    .first()
                            val dbVerifier = if (enableVerification) {
                                createVerifierFromEntities(invocation)
                            } else {
                                null
                            }
                            val dbType = MoreTypes.asDeclared(
                                    invocation.context.processingEnv.elementUtils
                                            .getTypeElement(RoomTypeNames.ROOM_DB.toString())
                                            .asType())
                            val parser = DaoProcessor(invocation.context,
                                    MoreElements.asType(dao), dbType, dbVerifier)

                            val parsedDao = parser.process()
                            handler(parsedDao, invocation)
                            true
                        }
                        .build())
    }
}
