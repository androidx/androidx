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
@file:JvmName("Rx3DaoReturnTypeConverters")

package androidx.room3.rxjava3

import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import androidx.room3.RoomDatabase
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.rx3.rxMaybe
import kotlinx.coroutines.rx3.rxSingle

/**
 * A [DaoReturnTypeConverter] that allows Room to return RxJava3 types from `@Dao` functions.
 *
 * When defining a converter for a reactive type that supports null values or empty states (e.g.
 * [Maybe]), the function type parameter must be restricted to `<T : Any>`, and the
 * `executeAndConvert` lambda must return `T?`, signaling Room to handle a null/empty result when
 * converting this DAO return type.
 *
 * You can register this converter via annotating a [androidx.room3.Database] or
 * [androidx.room3.Dao] using the annotation [androidx.room3.DaoReturnTypeConverters]:
 * ```
 * @DaoReturnTypeConverters(
 *     Rx3DaoReturnTypeConverters::class
 * )
 * ```
 */
public class RxDaoReturnTypeConverters {
    /**
     * This [convertFlowable] function will be called from Room generated code to convert a Room
     * query result to the return type of this function.
     *
     * This converter is restricted to [OperationType.READ] via the
     * [DaoReturnTypeConverter.operations] property, as [Flowable] is intended for observing
     * continuous data changes.
     *
     * @param database RoomDatabase instance
     * @param tableNames List of names of the tables of the RoomDatabase
     * @param executeAndConvert A suspend lambda function that invokes the part of the generated
     *   code that executes the query.
     */
    @DaoReturnTypeConverter(operations = [OperationType.READ])
    public fun <T : Any> convertFlowable(
        database: RoomDatabase,
        tableNames: Array<String>,
        executeAndConvert: suspend () -> T?,
    ): Flowable<T> {
        return convertObservable(
                database = database,
                tableNames = tableNames,
                executeAndConvert = executeAndConvert,
            )
            .toFlowable(BackpressureStrategy.LATEST)
    }

    /**
     * This [convertObservable] function will be called from Room generated code to convert a Room
     * query result to the return type of this function.
     *
     * This converter is restricted to [OperationType.READ] via the
     * [DaoReturnTypeConverter.operations] property, as [Flowable] is intended for observing
     * continuous data changes.
     *
     * @param database RoomDatabase instance
     * @param tableNames List of names of the tables of the RoomDatabase
     * @param executeAndConvert A suspend lambda function that invokes the part of the generated
     *   code that executes the query.
     */
    @DaoReturnTypeConverter(operations = [OperationType.READ])
    public fun <T : Any> convertObservable(
        database: RoomDatabase,
        tableNames: Array<String>,
        executeAndConvert: suspend () -> T?,
    ): Observable<T> {
        return database.invalidationTracker
            .createFlow(*tableNames, emitInitialState = true)
            .conflate()
            .map { executeAndConvert.invoke() }
            .filterNotNull()
            .asObservable(database.getQueryContext())
    }

    /**
     * This [convertMaybe] function will be called from Room generated code to convert a Room query
     * result to the return type of this function.
     *
     * This converter can be used for both [OperationType.READ] and [OperationType.WRITE]. Note that
     * Room shortcut methods (@Insert, @Update, @Delete) are always treated as
     * [OperationType.WRITE].
     *
     * @param database RoomDatabase instance
     * @param executeAndConvert A suspend lambda function that invokes the part of the generated
     *   code that executes the query.
     */
    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
    public fun <T : Any> convertMaybe(
        database: RoomDatabase,
        executeAndConvert: suspend () -> T?,
    ): Maybe<T> {
        return rxMaybe(database.getQueryContext().minusKey(Job)) { executeAndConvert.invoke() }
    }

    /**
     * This [convertCompletable] function will be called from Room generated code to convert a Room
     * query result to the return type of this function.
     *
     * This converter is restricted to [OperationType.WRITE] via the
     * [DaoReturnTypeConverter.operations] property, as [Completable] is typically used for
     * operations that modify the database without returning a value, such as Room shortcut methods
     * (@Insert, @Update, @Delete).
     *
     * @param database RoomDatabase instance
     * @param executeAndConvert A suspend lambda function that invokes the part of the generated
     *   code that executes the query.
     */
    @DaoReturnTypeConverter(operations = [OperationType.WRITE])
    public fun convertCompletable(
        database: RoomDatabase,
        executeAndConvert: suspend () -> Unit?,
    ): Completable {
        return rxCompletable(database.getQueryContext().minusKey(Job)) {
            executeAndConvert.invoke()
        }
    }

    /**
     * This [convertSingle] function will be called from Room generated code to convert a Room query
     * result to the return type of this function.
     *
     * This converter can be used for both [OperationType.READ] and [OperationType.WRITE]. Note that
     * Room shortcut methods (@Insert, @Update, @Delete) are always treated as
     * [OperationType.WRITE].
     *
     * @param database RoomDatabase instance
     * @param executeAndConvert A suspend lambda function that invokes the part of the generated
     *   code that executes the query.
     */
    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
    public fun <T : Any> convertSingle(
        database: RoomDatabase,
        executeAndConvert: suspend () -> T?,
    ): Single<T> {
        return rxSingle(database.getQueryContext().minusKey(Job)) {
            executeAndConvert.invoke()
                ?: throw EmptyResultSetException("Query returned null, but Single<T> was expected.")
        }
    }
}
