/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.room

import androidx.sqlite.SQLiteConnection
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.Callable

// mock rx2 helper
 class RxRoom {

    companion object {

        @JvmField
        val NOTHING: Any = Any()

        @JvmStatic
        fun <T : Any> createFlowable(
            db: RoomDatabase,
            inTransaction: Boolean,
            tableNames: Array<String>,
            block: (SQLiteConnection) -> T?
        ): Flowable<T> {
            TODO()
        }

        @JvmStatic
        fun <T : Any> createObservable(
            db: RoomDatabase,
            inTransaction: Boolean,
            tableNames: Array<String>,
            block: (SQLiteConnection) -> T?
        ): Observable<T> {
            TODO()
        }

        @JvmStatic
        fun <T : Any> createMaybe(
            db: RoomDatabase,
            isReadOnly: Boolean,
            inTransaction: Boolean,
            block: (SQLiteConnection) -> T?
        ): Maybe<T> {
            TODO()
        }

        @JvmStatic
        fun createCompletable(
            db: RoomDatabase,
            isReadOnly: Boolean,
            inTransaction: Boolean,
            block: (SQLiteConnection) -> Unit
        ): Completable {
            TODO()
        }

        @JvmStatic
        fun <T : Any> createSingle(
            db: RoomDatabase,
            isReadOnly: Boolean,
            inTransaction: Boolean,
            block: (SQLiteConnection) -> T?
        ): Single<T> {
            TODO()
        }

        @JvmStatic
        fun createFlowable(database: RoomDatabase, vararg tableNames: String): Flowable<Any> {
            TODO()
        }

        @JvmStatic
        fun createObservable(database: RoomDatabase, vararg tableNames: String): Observable<Any> {
            TODO()
        }
    }
}
