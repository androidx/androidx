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
import androidx.room.compiler.processing.isTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getSystemClasspathFiles
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.RoomTypeNames
import androidx.room.testing.context
import androidx.room.vo.Dao
import androidx.room.vo.ReadQueryMethod
import androidx.room.vo.Warning
import com.squareup.javapoet.TypeName
import createVerifierFromEntitiesAndViews
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class DaoProcessorTest(private val enableVerification: Boolean) {

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
    fun testUnusedEnumCompilesWithoutError() {
        singleDao(
            """
                @Dao abstract class MyDao {
                    @Query("SELECT uid FROM User")
                    abstract int[] getIds();
                    enum Fruit {
                        APPLE,
                        BANANA,
                        STRAWBERRY
                    }
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorCount(0)
            }
        }
    }

    @Test
    fun testNonAbstract() {
        singleDao("@Dao public class MyDao {}") { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE
                )
            }
        }
    }

    @Test
    fun testAbstractMethodWithoutQuery() {
        singleDao(
            """
                @Dao public interface MyDao {
                    int getFoo();
                }
        """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD)
            }
        }
    }

    @Test
    fun testAbstractMethodWithoutQueryInLibraryClass() {
        val librarySource = Source.java(
            "test.library.MissingAnnotationsBaseDao",
            """
                package test.library;
                public interface MissingAnnotationsBaseDao {
                    int getFoo();
                }
                """
        )
        val libraryClasspath = compileFiles(
            listOf(librarySource)
        )
        singleDao(
            "@Dao public interface MyDao extends test.library.MissingAnnotationsBaseDao {}",
            classpathFiles = listOf(libraryClasspath) + getSystemClasspathFiles()
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasRawOutputContaining(
                    ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD +
                        " - test.library.MissingAnnotationsBaseDao.getFoo()"
                )
                hasErrorContaining(ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD)
            }
        }
    }

    @Test
    fun testBothAnnotations() {
        singleDao(
            """
                @Dao public interface MyDao {
                    @Query("select 1")
                    @Insert
                    int getFoo(int x);
                }
        """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD)
            }
        }
    }

    @Test
    fun testAbstractClass() {
        singleDao(
            """
                @Dao abstract class MyDao {
                    @Query("SELECT uid FROM User")
                    abstract int[] getIds();
                }
                """
        ) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
        }
    }

    @Test
    fun testInterface() {
        singleDao(
            """
                @Dao interface MyDao {
                    @Query("SELECT uid FROM User")
                    abstract int[] getIds();
                }
                """
        ) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
        }
    }

    @Test
    fun testWithInsertAndQuery() {
        singleDao(
            """
                @Dao abstract class MyDao {
                    @Query("SELECT uid FROM User")
                    abstract int[] getIds();
                    @Insert
                    abstract void insert(User user);
                }
                """
        ) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
            assertThat(dao.insertionMethods.size, `is`(1))
            val insertMethod = dao.insertionMethods.first()
            assertThat(insertMethod.name, `is`("insert"))
        }
    }

    @Test
    fun skipQueryVerification() {
        singleDao(
            """
                @Dao @SkipQueryVerification interface MyDao {
                    @Query("SELECT nonExistingField FROM User")
                    abstract int[] getIds();
                }
                """
        ) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getIds"))
        }
    }

    @Test
    fun suppressedWarnings() {
        singleDao(
            """
            @SuppressWarnings({"ALL", RoomWarnings.CURSOR_MISMATCH})
            @Dao interface MyDao {
                @Query("SELECT * from user")
                abstract User users();
            }
            """
        ) { dao, invocation ->
            val dbType = invocation.context.processingEnv
                .requireType(RoomTypeNames.ROOM_DB)
            val daoProcessor =
                DaoProcessor(invocation.context, dao.element, dbType, null)

            assertThat(
                daoProcessor.context.logger
                    .suppressedWarnings,
                `is`(setOf(Warning.ALL, Warning.CURSOR_MISMATCH))
            )

            dao.queryMethods.forEach {
                assertThat(
                    QueryMethodProcessor(
                        baseContext = daoProcessor.context,
                        containing = dao.element.type,
                        executableElement = it.element,
                        dbVerifier = null
                    ).context.logger.suppressedWarnings,
                    `is`(setOf(Warning.ALL, Warning.CURSOR_MISMATCH))
                )
            }
        }
    }

    @Test
    fun suppressedWarningsInheritance() {
        singleDao(
            """
            @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
            @Dao interface MyDao {
                @SuppressWarnings("ALL")
                @Query("SELECT * from user")
                abstract User users();
            }
            """
        ) { dao, invocation ->
            val dbType = invocation.context.processingEnv
                .requireType(RoomTypeNames.ROOM_DB)
            val daoProcessor =
                DaoProcessor(invocation.context, dao.element, dbType, null)
            assertThat(
                daoProcessor.context.logger
                    .suppressedWarnings,
                `is`(setOf(Warning.CURSOR_MISMATCH))
            )

            dao.queryMethods.forEach {
                assertThat(
                    QueryMethodProcessor(
                        baseContext = daoProcessor.context,
                        containing = dao.element.type,
                        executableElement = it.element,
                        dbVerifier = null
                    ).context.logger.suppressedWarnings,
                    `is`(setOf(Warning.ALL, Warning.CURSOR_MISMATCH))
                )
            }
        }
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
        ) { dao, invocation ->
            assertThat(dao.queryMethods.size, `is`(1))
            assertThat(
                dao.queryMethods.filterIsInstance<ReadQueryMethod>().first().inTransaction,
                `is`(false)
            )
            invocation.assertCompilationResult {
                hasWarning(ProcessorErrors.TRANSACTION_MISSING_ON_RELATION)
            }
        }
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
        ) { dao, invocation ->
            assertThat(dao.queryMethods.size, `is`(1))
            assertThat(
                dao.queryMethods.filterIsInstance<ReadQueryMethod>().first().inTransaction,
                `is`(false)
            )
            invocation.assertCompilationResult {
                hasNoWarnings()
            }
        }
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
        ) { dao, invocation ->
            // test sanity
            assertThat(dao.queryMethods.size, `is`(1))
            assertThat(
                dao.queryMethods.filterIsInstance<ReadQueryMethod>().first().inTransaction,
                `is`(true)
            )
            invocation.assertCompilationResult {
                hasNoWarnings()
            }
        }
    }

    @Test
    fun testDeleteQueryWithVoidReturn() {
        singleDao(
            """
                @Dao interface MyDao {
                    @Query("DELETE FROM User")
                    abstract void deleteAllIds();
                }
                """
        ) { dao, _ ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("deleteAllIds"))
        }
    }

    @Test
    fun testSelectQueryWithVoidReturn() {
        singleDao(
            """
                @Dao interface MyDao {
                    @Query("SELECT * FROM User")
                    abstract void getAllIds();
                }
                """
        ) { dao, invocation ->
            assertThat(dao.queryMethods.size, `is`(1))
            val method = dao.queryMethods.first()
            assertThat(method.name, `is`("getAllIds"))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.cannotFindQueryResultAdapter(TypeName.VOID)
                )
            }
        }
    }

    private fun singleDao(
        vararg inputs: String,
        classpathFiles: List<File> = emptyList(),
        handler: (Dao, XTestInvocation) -> Unit
    ) {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "foo.bar.MyDao",
                    DAO_PREFIX + inputs.joinToString("\n")
                ),
                COMMON.USER
            ),
            classpath = classpathFiles
        ) { invocation: XTestInvocation ->
            val dao = invocation.roundEnv
                .getElementsAnnotatedWith(
                    androidx.room.Dao::class.qualifiedName!!
                )
                .first()
            check(dao.isTypeElement())
            val dbVerifier = if (enableVerification) {
                createVerifierFromEntitiesAndViews(invocation)
            } else {
                null
            }
            val dbType = invocation.context.processingEnv
                .requireType(RoomTypeNames.ROOM_DB)
            val parser = DaoProcessor(
                invocation.context,
                dao, dbType, dbVerifier
            )

            val parsedDao = parser.process()
            handler(parsedDao, invocation)
        }
    }
}
