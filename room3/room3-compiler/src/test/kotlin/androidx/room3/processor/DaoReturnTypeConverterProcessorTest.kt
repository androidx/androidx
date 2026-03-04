/*
 * Copyright 2025 The Android Open Source Project
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

import COMMON.EITHER
import androidx.room3.compiler.processing.isTypeElement
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.XTestInvocation
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_ANNOTATION_MUST_HAVE_OPERATION_TYPE
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_EMPTY_CLASS
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_WITHOUT_TYPE_PARAM_SHOULD_RETURN_UNIT
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_LAMBDA_MUST_BE_LAST_PARAM
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_MUST_CONTAIN_AN_ANNOTATED_FUNCTION
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_MUST_HAVE_ONE_LAMBDA_PARAM_THAT_IS_SUSPEND
import androidx.room3.processor.ProcessorErrors.FOUND_DAO_TYPE_CONVERTER_WITH_NON_SUSPEND_LAMBDA
import androidx.room3.processor.ProcessorErrors.daoReturnTypeConverterFunctionsWithATypeParamShouldHaveReturnTypeContainingTheSameTypeArg
import androidx.room3.processor.ProcessorErrors.duplicateDaoReturnTypeConverters
import androidx.room3.testing.context
import org.junit.Ignore
import org.junit.Test

class DaoReturnTypeConverterProcessorTest {

    @Test
    fun forgotToAnnotateDaoReturnTypeConverterFunction() {
        val problematicConverter =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class FooReturnTypeConverter {
                    suspend fun convert(
                        executeAndConvert: suspend () -> Unit,
                    ): Foo {
                        TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(problematicConverter, DATABASE, DAO, FOO_BAR_TYPES),
            expectedErrorCount = 1,
            expectedError = DAO_RETURN_TYPE_CONVERTER_MUST_CONTAIN_AN_ANNOTATED_FUNCTION,
        )
    }

    @Test
    fun daoReturnTypeConverterClassMustHaveAtLeastOneFunction() {
        val problematicConverter =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class FooReturnTypeConverter
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(problematicConverter, DATABASE, DAO, FOO_BAR_TYPES),
            expectedErrorCount = 1,
            expectedError = DAO_RETURN_TYPE_CONVERTER_EMPTY_CLASS,
        )
    }

    @Test
    fun methodWithTypeParamMustHaveReturnTypeWithSameTypeParam() {
        val problematicConverter =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class FooReturnTypeConverter {
                    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
                    suspend fun <T> convert(
                        executeAndConvert: suspend () -> T,
                    ): Foo {
                       TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(problematicConverter, DATABASE, DAO, FOO_BAR_TYPES),
            expectedErrorCount = 1,
            expectedError =
                daoReturnTypeConverterFunctionsWithATypeParamShouldHaveReturnTypeContainingTheSameTypeArg(
                    "T",
                    "",
                ),
        )
    }

    @Ignore /// b/482978786
    @Test
    fun withMethodTypeParamMustHaveReturnTypeContainingSameTypeParam() {
        val problematicConverter =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class Baz<E>(val value: E)

                class BazReturnTypeConverter<E> {
                    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
                    suspend fun <T> convert(
                        executeAndConvert: suspend () -> T,
                    ): Baz<E> {
                        TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        val db =
            Source.kotlin(
                "MyDatabase.kt",
                """
                import androidx.room3.*

                @DaoReturnTypeConverters(BazReturnTypeConverter::class)
                @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
                abstract class MyDatabase : RoomDatabase() {
                  abstract val dao: MyDao
                }
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(problematicConverter, db, DAO),
            expectedErrorCount = 1,
            expectedError =
                daoReturnTypeConverterFunctionsWithATypeParamShouldHaveReturnTypeContainingTheSameTypeArg(
                    "T",
                    "E",
                ),
        )
    }

    @Test
    fun withMethodTypeParamMustHaveReturnTypeWithOnlyOneGenericParamType() {
        val problematicConverter =
            Source.kotlin(
                "EitherReturnTypeConverter.kt",
                """
                import androidx.room3.*
                import arrow.core.*

                class EitherReturnTypeConverter {
                    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
                    suspend fun <L, R> convert(
                        executeAndConvert: suspend () -> R,
                    ): Either<L, R> {
                       TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        val dao =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*
                import arrow.core.*

                @Dao
                @DaoReturnTypeConverters(EitherReturnTypeConverter::class)
                abstract class MyDao(private val db: RoomDatabase) {
                  @Query("SELECT * FROM MyEntity")
                  abstract suspend fun <T> leftConvert(): Either<T, Error>

                  @Query("SELECT * FROM MyEntity")
                  abstract suspend fun <T> rightConvert(): Either<Error, T>
                }

                @Entity
                data class MyEntity(@PrimaryKey val pk: Int)
                """
                    .trimIndent(),
            )
        runTest(
            sources =
                listOf(EITHER, problematicConverter, DATABASE, dao, FOO_CONVERTER, FOO_BAR_TYPES),
            expectedErrorCount = 0,
            expectedError = "",
        )
    }

    @Test
    fun duplicateDaoReturnTypeConverter() {
        val duplicateConverter =
            Source.kotlin(
                "OtherFooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class OtherFooReturnTypeConverter {
                    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
                    suspend fun <T> convert(
                        executeAndConvert: suspend () -> T,
                    ): Foo<T> {
                       TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        val db =
            Source.kotlin(
                "MyDatabase.kt",
                """
                import androidx.room3.*

                @DaoReturnTypeConverters(FooReturnTypeConverter::class, OtherFooReturnTypeConverter::class)
                @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
                abstract class MyDatabase : RoomDatabase() {
                  abstract val dao: MyDao
                }
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(duplicateConverter, db, FOO_CONVERTER, FOO_BAR_TYPES, DAO),
            expectedErrorCount = 1,
            // Pass emptyList as asserting the error message itself is sufficient.
            expectedError = duplicateDaoReturnTypeConverters(emptyList()),
        )
    }

    @Test
    fun daoReturnTypeConverterMustHaveLambdaParameter() {
        val problematicConverter =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class FooReturnTypeConverter {
                    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
                    suspend fun convert(
                        database: RoomDatabase,
                        tableNames: Array<String>
                    ): Foo {
                        TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(problematicConverter, DATABASE, DAO, FOO_BAR_TYPES),
            expectedErrorCount = 1,
            expectedError = DAO_RETURN_TYPE_CONVERTER_MUST_HAVE_ONE_LAMBDA_PARAM_THAT_IS_SUSPEND,
        )
    }

    @Test
    fun lambdaParamWithMoreThanOneParamDisallowed() {
        val problematicConverter =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class FooReturnTypeConverter {
                    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
                    suspend fun convert(
                        database: RoomDatabase,
                        roomRawQuery: RoomRawQuery,
                        tableNames: Array<String>,
                        executeAndConvert: suspend (RoomRawQuery, Array<String>) -> Unit,
                    ): Foo {
                        TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(problematicConverter, DATABASE, DAO, FOO_BAR_TYPES),
            expectedErrorCount = 1,
            expectedError = DAO_RETURN_TYPE_CONVERTER_MUST_HAVE_ONE_LAMBDA_PARAM_THAT_IS_SUSPEND,
        )
    }

    @Test
    fun lambdaParamMustBeLast() {
        val problematicConverter =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class FooReturnTypeConverter {
                    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
                    suspend fun <T> convert(
                        database: RoomDatabase,
                        executeAndConvert: suspend () -> T,
                        roomRawQuery: RoomRawQuery,
                        tableNames: Array<String>,
                    ): Foo<T> {
                        TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(problematicConverter, DATABASE, DAO, FOO_BAR_TYPES),
            expectedErrorCount = 1,
            expectedError = DAO_RETURN_TYPE_CONVERTER_LAMBDA_MUST_BE_LAST_PARAM,
        )
    }

    @Test
    fun noMethodTypeParamMustHaveLambdaParameterReturningUnit() {
        val problematicConverter =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class Baz(data: MyEntity)

                class BazReturnTypeConverter {
                    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
                    suspend fun convert(
                        executeAndConvert: suspend () -> MyEntity,
                    ): Baz {
                        TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        val db =
            Source.kotlin(
                "MyDatabase.kt",
                """
                import androidx.room3.*

                @DaoReturnTypeConverters(BazReturnTypeConverter::class)
                @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
                abstract class MyDatabase : RoomDatabase() {
                  abstract val dao: MyDao
                }
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(problematicConverter, db, DAO, FOO_BAR_TYPES),
            expectedErrorCount = 1,
            expectedError =
                DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_WITHOUT_TYPE_PARAM_SHOULD_RETURN_UNIT,
        )
    }

    @Test
    fun foundNonSuspendLambda() {
        val problematicConverter =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class FooReturnTypeConverter {
                    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
                    suspend fun convert(
                        executeAndConvert: () -> Unit,
                    ): Foo {
                        TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(problematicConverter, DATABASE, DAO, FOO_BAR_TYPES),
            expectedErrorCount = 2,
            expectedError = FOUND_DAO_TYPE_CONVERTER_WITH_NON_SUSPEND_LAMBDA,
        )
    }

    @Test
    fun forgotToProvideOperationType() {
        val problematicConverter =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class FooReturnTypeConverter {
                    @DaoReturnTypeConverter
                    suspend fun <T> convert(
                        executeAndConvert: suspend () -> T,
                    ): Foo<T> {
                       TODO()
                    }
                }
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(problematicConverter, DATABASE, DAO, FOO_BAR_TYPES),
            expectedErrorCount = 1,
            expectedError = DAO_RETURN_TYPE_CONVERTER_ANNOTATION_MUST_HAVE_OPERATION_TYPE,
        )
    }

    private fun runTest(sources: List<Source>, expectedErrorCount: Int, expectedError: String) {
        runKspTest(sources = sources, classpath = emptyList()) { invocation: XTestInvocation ->
            invokeProcessor(invocation)
            invocation.assertCompilationResult {
                hasErrorCount(expectedErrorCount)
                if (expectedErrorCount > 0) {
                    hasErrorContaining(expectedError)
                }
            }
        }
    }

    companion object {
        val DATABASE =
            Source.kotlin(
                "MyDatabase.kt",
                """
                import androidx.room3.*

                @DaoReturnTypeConverters(FooReturnTypeConverter::class)
                @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
                abstract class MyDatabase : RoomDatabase() {
                  abstract val dao: MyDao
                }
                """
                    .trimIndent(),
            )

        val DAO =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*

                @Dao
                abstract class MyDao(private val db: RoomDatabase) {
                  @Query("SELECT * FROM MyEntity")
                  abstract suspend fun getFoo(): Foo<MyEntity>
                }

                @Entity
                data class MyEntity(@PrimaryKey val pk: Int)
                """
                    .trimIndent(),
            )

        val FOO_BAR_TYPES =
            Source.kotlin(
                "FooAndBar.kt",
                """
                import androidx.room3.*

                open class Bar<T>(val data: T)
                class Foo<T>(data: T): Bar<T>(data)
                """
                    .trimIndent(),
            )

        val FOO_CONVERTER =
            Source.kotlin(
                "FooReturnTypeConverter.kt",
                """
                import androidx.room3.*

                class FooReturnTypeConverter {
                    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
                    suspend fun <T> convert(
                        executeAndConvert: suspend () -> T,
                    ): Foo<T> {
                       TODO()
                    }
                }
                """
                    .trimIndent(),
            )
    }

    private fun invokeProcessor(invocation: XTestInvocation) {
        val database =
            invocation.roundEnv
                .getElementsAnnotatedWith(androidx.room3.Database::class.qualifiedName!!)
                .first()
        check(database.isTypeElement())
        DaoReturnTypeConverterProcessor.findConverters(invocation.context, database)
    }
}
