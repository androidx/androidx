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
import androidx.kruth.assertThat
import androidx.room.compiler.processing.isTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.ext.RoomTypeNames.ROOM_DB
import androidx.room.processor.ProcessorErrors.nullableCollectionOrArrayReturnTypeInDaoMethod
import androidx.room.processor.ProcessorErrors.nullableComponentInDaoMethodReturnType
import androidx.room.runKspTestWithK1
import androidx.room.runProcessorTestWithK1
import androidx.room.testing.context
import androidx.room.vo.Dao
import androidx.room.vo.ReadQueryMethod
import androidx.room.vo.Warning
import createVerifierFromEntitiesAndViews
import java.io.File
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DaoProcessorTest(private val enableVerification: Boolean) {

    companion object {
        const val DAO_PREFIX =
            """
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
            invocation.assertCompilationResult { hasErrorCount(0) }
        }
    }

    @Test
    fun testNonAbstract() {
        singleDao("@Dao public class MyDao {}") { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE)
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
        val librarySource =
            Source.java(
                "test.library.MissingAnnotationsBaseDao",
                """
                package test.library;
                public interface MissingAnnotationsBaseDao {
                    int getFoo();
                }
                """
            )
        val libraryClasspath = compileFiles(listOf(librarySource))
        singleDao(
            "@Dao public interface MyDao extends test.library.MissingAnnotationsBaseDao {}",
            classpathFiles = libraryClasspath
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
                hasErrorContaining(ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD).onLine(8)
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
            assertThat(method.element.jvmName, `is`("getIds"))
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
            assertThat(method.element.jvmName, `is`("getIds"))
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
            assertThat(method.element.jvmName, `is`("getIds"))
            assertThat(dao.insertMethods.size, `is`(1))
            val insertMethod = dao.insertMethods.first()
            assertThat(insertMethod.element.jvmName, `is`("insert"))
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
            assertThat(method.element.jvmName, `is`("getIds"))
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
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            val daoProcessor = DaoProcessor(invocation.context, dao.element, dbType, null)

            assertThat(
                daoProcessor.context.logger.suppressedWarnings,
                `is`(setOf(Warning.ALL, Warning.CURSOR_MISMATCH))
            )

            dao.queryMethods.forEach {
                assertThat(
                    QueryMethodProcessor(
                            baseContext = daoProcessor.context,
                            containing = dao.element.type,
                            executableElement = it.element,
                            dbVerifier = null
                        )
                        .context
                        .logger
                        .suppressedWarnings,
                    `is`(setOf(Warning.ALL, Warning.CURSOR_MISMATCH))
                )
            }
        }
    }

    @Test
    fun suppressedWarningsKotlin() {
        val daoSrc =
            Source.kotlin(
                "MyDao.kt",
                """
            package foo.bar
            import androidx.room.*
            @Dao
            @Suppress(RoomWarnings.CURSOR_MISMATCH)
            interface MyDao {
                @Query("SELECT uid from user")
                fun userId(): Int
            }
            """
                    .trimIndent()
            )
        runProcessorTestWithK1(sources = listOf(daoSrc) + COMMON.USER) { invocation ->
            val dao =
                invocation.roundEnv
                    .getElementsAnnotatedWith(androidx.room.Dao::class.qualifiedName!!)
                    .first()
            if (!dao.isTypeElement()) {
                error("Expected DAO to be a type")
            }
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            val daoProcessor = DaoProcessor(invocation.context, dao, dbType, null)
            assertThat(daoProcessor.context.logger.suppressedWarnings)
                .containsExactly(Warning.CURSOR_MISMATCH)
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
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            val daoProcessor = DaoProcessor(invocation.context, dao.element, dbType, null)
            assertThat(
                daoProcessor.context.logger.suppressedWarnings,
                `is`(setOf(Warning.CURSOR_MISMATCH))
            )

            dao.queryMethods.forEach {
                assertThat(
                    QueryMethodProcessor(
                            baseContext = daoProcessor.context,
                            containing = dao.element.type,
                            executableElement = it.element,
                            dbVerifier = null
                        )
                        .context
                        .logger
                        .suppressedWarnings,
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
                hasWarningContaining(ProcessorErrors.TRANSACTION_MISSING_ON_RELATION)
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
            invocation.assertCompilationResult { hasNoWarnings() }
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
            invocation.assertCompilationResult { hasNoWarnings() }
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
            assertThat(method.element.jvmName, `is`("deleteAllIds"))
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
            assertThat(method.element.jvmName, `is`("getAllIds"))
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.cannotFindQueryResultAdapter("void"))
            }
        }
    }

    @Test
    fun jvmNameOnDao() {
        val source =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*;
            @Dao
            interface MyDao {
                @Suppress("INAPPLICABLE_JVM_NAME")
                @JvmName("jvmMethodName")
                @Query("SELECT 1")
                fun method(): Int
            }
        """
                    .trimIndent()
            )
        runProcessorTestWithK1(sources = listOf(source)) { invocation ->
            val dao = invocation.processingEnv.requireTypeElement("MyDao")
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            DaoProcessor(
                    baseContext = invocation.context,
                    element = dao,
                    dbType = dbType,
                    dbVerifier = null
                )
                .process()
            invocation.assertCompilationResult {
                hasWarningContaining(ProcessorErrors.JVM_NAME_ON_OVERRIDDEN_METHOD)
            }
        }
    }

    @Test
    fun allowDaoQueryProperty() {
        val src =
            Source.kotlin(
                "MyDatabase.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @get:Query("SELECT * FROM MyEntity")
              val allEntities: List<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                var pk: Int
            )
            """
                    .trimIndent()
            )
        runKspTestWithK1(
            sources = listOf(src),
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "true"),
        ) { invocation ->
            val dao = invocation.processingEnv.requireTypeElement("MyDao")
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            DaoProcessor(
                    baseContext = invocation.context,
                    element = dao,
                    dbType = dbType,
                    dbVerifier = null
                )
                .process()
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun missingAnnotationInDaoProperty() {
        val src =
            Source.kotlin(
                "MyDatabase.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              val allEntities: List<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                var pk: Int
            )
            """
                    .trimIndent()
            )
        runKspTestWithK1(
            sources = listOf(src),
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "true"),
        ) { invocation ->
            val dao = invocation.processingEnv.requireTypeElement("MyDao")
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            DaoProcessor(
                    baseContext = invocation.context,
                    element = dao,
                    dbType = dbType,
                    dbVerifier = null
                )
                .process()
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_ANNOTATION_IN_DAO_PROPERTY)
            }
        }
    }

    @Test
    fun missplacedAnnotationInDaoProperty() {
        val src =
            Source.kotlin(
                "MyDatabase.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              val allEntities: List<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                var pk: Int
            )
            """
                    .trimIndent()
            )
        runKspTestWithK1(
            sources = listOf(src),
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "true"),
        ) { invocation ->
            val dao = invocation.processingEnv.requireTypeElement("MyDao")
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            DaoProcessor(
                    baseContext = invocation.context,
                    element = dao,
                    dbType = dbType,
                    dbVerifier = null
                )
                .process()
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_ANNOTATION_IN_DAO_PROPERTY)
            }
        }
    }

    @Test
    fun testSelectQueryWithNullableCollectionReturn() {
        val src =
            Source.kotlin(
                "MyDatabase.kt",
                """
            import androidx.room.*
            import com.google.common.collect.ImmutableList

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun nullableList(): List<MyEntity>?

              @Query("SELECT * FROM MyEntity")
              fun nullableImmutableList(): ImmutableList<MyEntity>?

              @Query("SELECT * FROM MyEntity")
              fun nullableArray(): Array<MyEntity>?

              @Query("SELECT * FROM MyEntity")
              fun nullableOptional(): java.util.Optional<MyEntity>?

              @Query("SELECT * FROM MyEntity")
              fun nullableOptionalGuava(): com.google.common.base.Optional<MyEntity>?

              @Query("SELECT * FROM MyEntity JOIN MyOtherEntity ON MyEntity.pk = MyOtherEntity.otherPk")
              fun nullableMap(): Map<MyEntity, MyOtherEntity>?

              @Query("SELECT * FROM MyEntity JOIN MyOtherEntity ON MyEntity.pk = MyOtherEntity.otherPk")
              fun nullableImmutableMap(): com.google.common.collect.ImmutableMap<MyEntity, MyOtherEntity>?

              @Query("SELECT * FROM MyEntity JOIN MyOtherEntity ON MyEntity.pk = MyOtherEntity.otherPk")
              fun nullableImmutableSetMultimap(): com.google.common.collect.ImmutableSetMultimap<MyEntity, MyOtherEntity>?

              @Query("SELECT * FROM MyEntity JOIN MyOtherEntity ON MyEntity.pk = MyOtherEntity.otherPk")
              fun nullableImmutableListMultimap(): com.google.common.collect.ImmutableListMultimap<MyEntity, MyOtherEntity>?
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                var pk: Int
            )

            @Entity
            data class MyOtherEntity(
                @PrimaryKey
                var otherPk: Int
            )
            """
                    .trimIndent()
            )
        runKspTestWithK1(
            sources = listOf(src),
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "true"),
        ) { invocation ->
            val dao = invocation.processingEnv.requireTypeElement("MyDao")
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            DaoProcessor(
                    baseContext = invocation.context,
                    element = dao,
                    dbType = dbType,
                    dbVerifier = null
                )
                .process()
            invocation.assertCompilationResult {
                hasWarningContaining(
                    nullableCollectionOrArrayReturnTypeInDaoMethod(
                        "kotlin.collections.List<MyEntity>?",
                        "Collection"
                    )
                )
                hasWarningContaining(
                    nullableCollectionOrArrayReturnTypeInDaoMethod(
                        "com.google.common.collect.ImmutableList<MyEntity>?",
                        "Collection"
                    )
                )
                hasWarningContaining(
                    nullableCollectionOrArrayReturnTypeInDaoMethod(
                        "kotlin.Array<MyEntity>?",
                        "Array"
                    )
                )
                hasWarningContaining(
                    nullableCollectionOrArrayReturnTypeInDaoMethod(
                        "java.util.Optional<MyEntity>?",
                        "Optional"
                    )
                )
                hasWarningContaining(
                    nullableCollectionOrArrayReturnTypeInDaoMethod(
                        "com.google.common.base.Optional<MyEntity>?",
                        "Optional"
                    )
                )
                hasWarningContaining(
                    nullableCollectionOrArrayReturnTypeInDaoMethod(
                        "kotlin.collections.Map<MyEntity, MyOtherEntity>?",
                        "Collection"
                    )
                )
                hasWarningContaining(
                    nullableCollectionOrArrayReturnTypeInDaoMethod(
                        "com.google.common.collect.ImmutableMap<MyEntity, MyOtherEntity>?",
                        "Collection"
                    )
                )
                hasWarningContaining(
                    nullableCollectionOrArrayReturnTypeInDaoMethod(
                        "com.google.common.collect.ImmutableSetMultimap<MyEntity, MyOtherEntity>?",
                        "Collection"
                    )
                )
                hasWarningContaining(
                    nullableCollectionOrArrayReturnTypeInDaoMethod(
                        "com.google.common.collect.ImmutableListMultimap<MyEntity, MyOtherEntity>?",
                        "Collection"
                    )
                )
                hasWarningCount(9)
            }
        }
    }

    @Test
    fun testSelectQueryWithNullableTypeArgCollectionReturn() {
        val src =
            Source.kotlin(
                "MyDatabase.kt",
                """
            import androidx.room.*
            import com.google.common.collect.ImmutableList

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun nullableList(): List<MyEntity?>

              @Query("SELECT * FROM MyEntity")
              fun nullableImmutableList(): ImmutableList<MyEntity?>

              @Query("SELECT * FROM MyEntity")
              fun nullableArray(): Array<MyEntity?>

              @Query("SELECT * FROM MyEntity")
              fun nullableOptional(): java.util.Optional<MyEntity?>

              @Query("SELECT * FROM MyEntity")
              fun nullableOptionalGuava(): com.google.common.base.Optional<MyEntity?>

              @Query("SELECT * FROM MyEntity JOIN MyOtherEntity ON MyEntity.pk = MyOtherEntity.otherPk")
              fun nullableMap(): Map<MyEntity?, MyOtherEntity>

              @Query("SELECT * FROM MyEntity JOIN MyOtherEntity ON MyEntity.pk = MyOtherEntity.otherPk")
              fun nullableImmutableMap(): com.google.common.collect.ImmutableMap<MyEntity?, MyOtherEntity>

              @Query("SELECT * FROM MyEntity JOIN MyOtherEntity ON MyEntity.pk = MyOtherEntity.otherPk")
              fun nullableImmutableSetMultimap(): com.google.common.collect.ImmutableSetMultimap<MyEntity?, MyOtherEntity>

              @Query("SELECT * FROM MyEntity JOIN MyOtherEntity ON MyEntity.pk = MyOtherEntity.otherPk")
              fun nullableImmutableListMultimap(): com.google.common.collect.ImmutableListMultimap<MyEntity?, MyOtherEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                var pk: Int
            )

            @Entity
            data class MyOtherEntity(
                @PrimaryKey
                var otherPk: Int
            )
            """
                    .trimIndent()
            )
        runKspTestWithK1(
            sources = listOf(src),
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "true"),
        ) { invocation ->
            val dao = invocation.processingEnv.requireTypeElement("MyDao")
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            DaoProcessor(
                    baseContext = invocation.context,
                    element = dao,
                    dbType = dbType,
                    dbVerifier = null
                )
                .process()
            invocation.assertCompilationResult {
                hasWarningContaining(
                    nullableComponentInDaoMethodReturnType("kotlin.collections.List<MyEntity?>")
                )
                hasWarningContaining(
                    nullableComponentInDaoMethodReturnType(
                        "com.google.common.collect.ImmutableList<MyEntity?>"
                    )
                )
                hasWarningContaining(
                    nullableComponentInDaoMethodReturnType("kotlin.Array<MyEntity?>")
                )
                hasWarningContaining(
                    nullableComponentInDaoMethodReturnType("java.util.Optional<MyEntity?>")
                )
                hasWarningContaining(
                    nullableComponentInDaoMethodReturnType(
                        "com.google.common.base.Optional<MyEntity?>"
                    )
                )
                hasWarningContaining(
                    nullableComponentInDaoMethodReturnType(
                        "kotlin.collections.Map<MyEntity?, MyOtherEntity>"
                    )
                )
                hasWarningContaining(
                    nullableComponentInDaoMethodReturnType(
                        "com.google.common.collect.ImmutableMap<MyEntity?, MyOtherEntity>"
                    )
                )
                // We expect "MutableMap" when ImmutableMap is used because TypeAdapterStore will
                // convert the map to a mutable one and re-run the `findQueryResultAdapter`
                // algorithm
                hasWarningContaining(
                    nullableComponentInDaoMethodReturnType(
                        "kotlin.collections.MutableMap<MyEntity?, MyOtherEntity>"
                    )
                )
                hasWarningContaining(
                    nullableComponentInDaoMethodReturnType(
                        "com.google.common.collect.ImmutableSetMultimap<MyEntity?, MyOtherEntity>"
                    )
                )
                hasWarningContaining(
                    nullableComponentInDaoMethodReturnType(
                        "com.google.common.collect.ImmutableListMultimap<MyEntity?, MyOtherEntity>"
                    )
                )
                hasWarningCount(10)
            }
        }
    }

    private fun singleDao(
        vararg inputs: String,
        classpathFiles: List<File> = emptyList(),
        handler: (Dao, XTestInvocation) -> Unit
    ) {
        runProcessorTestWithK1(
            sources =
                listOf(
                    Source.java("foo.bar.MyDao", DAO_PREFIX + inputs.joinToString("\n")),
                    COMMON.USER
                ),
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "false"),
            classpath = classpathFiles
        ) { invocation: XTestInvocation ->
            val dao =
                invocation.roundEnv
                    .getElementsAnnotatedWith(androidx.room.Dao::class.qualifiedName!!)
                    .first()
            check(dao.isTypeElement())
            val dbVerifier =
                if (enableVerification) {
                    createVerifierFromEntitiesAndViews(invocation)
                } else {
                    null
                }
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            val parser = DaoProcessor(invocation.context, dao, dbType, dbVerifier)

            val parsedDao = parser.process()
            handler(parsedDao, invocation)
        }
    }
}
