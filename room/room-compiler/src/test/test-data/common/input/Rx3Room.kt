/*
 * Copyright 2024 The Android Open Source Project
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

// mock rx2 helper
@file:JvmName("RxRoom")

package androidx.room.rxjava3

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.Callable

class Rx3RoomArtifactMarker private constructor()

@JvmField
val NOTHING: Any = Any()

fun <T : Any> createFlowable(
    db: RoomDatabase,
    inTransaction: Boolean,
    tableNames: Array<String>,
    block: (SQLiteConnection) -> T?
): Flowable<T> {
    TODO()
}

fun <T : Any> createObservable(
    db: RoomDatabase,
    inTransaction: Boolean,
    tableNames: Array<String>,
    block: (SQLiteConnection) -> T?
): Observable<T> {
    TODO()
}

fun <T : Any> createMaybe(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> T?
): Maybe<T> {
    TODO()
}

fun createCompletable(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> Unit
): Completable {
    TODO()
}

fun <T : Any> createSingle(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> T?
): Single<T> {
    TODO()
}

fun createFlowable(
    database: RoomDatabase,
    vararg tableNames: String
): Flowable<Any> {
    TODO()
}

fun <T : Any> createFlowable(
    database: RoomDatabase,
    inTransaction: Boolean,
    tableNames: Array<String>,
    callable: Callable<out T>
): Flowable<T> {
    TODO()
}

fun createObservable(
    database: RoomDatabase,
    vararg tableNames: String
): Observable<Any> {
    TODO()
}

fun <T : Any> createObservable(
    database: RoomDatabase,
    inTransaction: Boolean,
    tableNames: Array<String>,
    callable: Callable<out T>
): Observable<T> {
    TODO()
}

fun <T : Any> createSingle(callable: Callable<out T>): Single<T> {
    TODO()
}
