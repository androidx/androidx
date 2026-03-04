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

package androidx.room3

/**
 * Marks a function as a DAO function return type converter.
 *
 * This annotation is used on functions within a converter class (which is registered via the
 * [DaoReturnTypeConverters] annotation on the [Database] or [Dao] annotated definitions).
 *
 * This function intercepts the result of a DAO function and wraps or transforms it into a custom
 * type (e.g., `Result<T>`).
 *
 * The [operations] parameter is required and defines whether this converter applies to read
 * operations, write operations, or both.
 *
 * ### Converter Functions Rules
 * 1. Converters that are `suspend` functions can only match with suspend DAO functions.
 * 2. **Lambda Parameter:** The function MUST include a functional parameter (e.g.
 *    `executeAndConvert`), which MUST be a `suspend` lambda and have at most one [RoomRawQuery]
 *    parameter.
 * 3. **Additional Parameters:** The function can have optional parameters for `database:
 *    RoomDatabase`, `inTransaction: Boolean` and `tableNames: Array<String>`.
 *
 * ### `executeAndConvert` Lambda Rules
 * The lambda can have a parameter of type [RoomRawQuery] to be used if query transformation is
 * needed. The `executeAndConvert` lambda can return a `List<T>` (e.g. for a [PagingSource]) or an
 * `Array<T>`.
 *
 * ### Rules for Generics and Return Types
 * * Generic Use Case:
 * * If a converter function has a generic type parameter (e.g., `<T>`), its return type must
 *   contain the same generic type parameter by name (e.g., `Result<T>`).
 * * The `executeAndConvert` lambda must return the generic type `T` (i.e., `suspend () -> T`).
 * * Non-Generic Use Case:
 * * If a converter function has no generic type parameter, its return type must have no generic
 *   type argument (e.g., `Int` or `Completable`).
 * * The `executeAndConvert` lambda must return `Unit` (i.e., `suspend () -> Unit`).
 * * Multiple Type Arguments:
 * * When attempting to match a converter with a DAO method's return type, if the DAO method return
 *   type has multiple type arguments (e.g., `MyResult<T, Error>`), only one type argument can be
 *   generic, and this single generic argument must match the generic type parameter on the
 *   converter function (`<T>`) at the corresponding position and must match the position of the
 *   generic type argument in the return type of the converter function.
 *
 * ### Example Signatures
 * 1. Generic Example Use-Case
 *
 * ```
 * @DaoReturnTypeConverter
 * suspend fun <T> convertGeneric(
 *   database: RoomDatabase,
 *   tableNames: Array<String>,
 *   executeAndConvert: suspend () -> T
 * ): Result<T> {
 *   val result = executeAndConvert.invoke()
 *   return Foo(result)
 * }
 * ```
 * 2. Non-Generic Example Use-Case
 *
 * ```
 * @DaoReturnTypeConverter
 *   suspend fun convertNonGeneric(
 *   database: RoomDatabase,
 *   tableNames: Array<String>,
 *   executeAndConvert: suspend () -> Unit
 * ): Int {
 *   executeAndConvert.invoke() // Execute the operation
 *   return 1 // E.g. return a non-generic status code
 * }
 * ```
 *
 * @property operations Defines which type of DAO operations (e.g., [OperationType.READ] or
 *   [OperationType.WRITE]) this converter should be applied to.
 *
 * If no [operations] are supplied, this converter function will be used for all DAO operation types
 * ([OperationType.READ] and [OperationType.WRITE]) that match the return type.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class DaoReturnTypeConverter(val operations: Array<OperationType>)

/**
 * Describes the type of database operation a [DaoReturnTypeConverter] function supports.
 *
 * A converter function defines the operation type it supports to provide distinct logic between
 * [READ] ([Query]) versus [WRITE] ([Insert], [Update], [Delete], and [Upsert]) operations.
 *
 * Note that `INSERT`, `UPDATE`, and `DELETE` statements executed within a [Query] annotation are
 * also considered write operations.
 *
 * If a converter function is used for a write operation, its return type's parameterized types are
 * limited to those supported by Room's write operations.
 */
public enum class OperationType {
    READ,
    WRITE,
}
