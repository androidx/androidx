/*
 * Copyright (C) 2017 The Android Open Source Project
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
@file:Suppress("DEPRECATION")

package androidx.room.paging

import android.database.Cursor
import androidx.annotation.RestrictTo
import androidx.paging.PositionalDataSource
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A simple data source implementation that uses Limit & Offset to page the query.
 *
 * This is NOT the most efficient way to do paging on SQLite. It is
 * [recommended](http://www.sqlite.org/cvstrac/wiki?p=ScrollingCursor) to use an indexed
 * ORDER BY statement but that requires a more complex API. This solution is technically equal to
 * receiving a [Cursor] from a large query but avoids the need to manually manage it, and
 * never returns inconsistent data if it is invalidated.
 *
 * This class is used for both Paging2 and Paging3 (via its compat API). When used with Paging3,
 * it does lazy registration for observers to be suitable for initialization on the main thread
 * whereas in Paging2, it will register observer eagerly to obey Paging2's strict Data Source
 * rules. (Paging2 does not let data source to possibly return invalidated data).
 *
 * @property <T> Data type returned by the data source.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
abstract class LimitOffsetDataSource<T : Any> protected constructor(
    private val db: RoomDatabase,
    private val sourceQuery: RoomSQLiteQuery,
    private val inTransaction: Boolean,
    registerObserverImmediately: Boolean,
    vararg tables: String
) : PositionalDataSource<T>() {
    private val countQuery = "SELECT COUNT(*) FROM ( " + sourceQuery.sql + " )"
    private val limitOffsetQuery = "SELECT * FROM ( " + sourceQuery.sql + " ) LIMIT ? OFFSET ?"
    private val observer: InvalidationTracker.Observer
    private val registeredObserver = AtomicBoolean(false)

    protected constructor(
        db: RoomDatabase,
        query: SupportSQLiteQuery,
        inTransaction: Boolean,
        vararg tables: String
    ) : this(db, RoomSQLiteQuery.copyFrom(query), inTransaction, *tables)

    protected constructor(
        db: RoomDatabase,
        query: SupportSQLiteQuery,
        inTransaction: Boolean,
        registerObserverImmediately: Boolean,
        vararg tables: String
    ) : this(
        db, RoomSQLiteQuery.copyFrom(query), inTransaction, registerObserverImmediately, *tables
    )

    protected constructor(
        db: RoomDatabase,
        query: RoomSQLiteQuery,
        inTransaction: Boolean,
        vararg tables: String
    ) : this(db, query, inTransaction, true, *tables)

    init {
        observer = object : InvalidationTracker.Observer(tables) {
            override fun onInvalidated(tables: Set<String>) {
                invalidate()
            }
        }
        if (registerObserverImmediately) {
            registerObserverIfNecessary()
        }
    }

    private fun registerObserverIfNecessary() {
        if (registeredObserver.compareAndSet(false, true)) {
            db.invalidationTracker.addWeakObserver(observer)
        }
    }

    /**
     * Count number of rows query can return
     *
     * @hide
     */
    fun countItems(): Int {
        registerObserverIfNecessary()
        val sqLiteQuery = RoomSQLiteQuery.acquire(
            countQuery,
            sourceQuery.argCount
        )
        sqLiteQuery.copyArgumentsFrom(sourceQuery)
        val cursor = db.query(sqLiteQuery)
        return try {
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else 0
        } finally {
            cursor.close()
            sqLiteQuery.release()
        }
    }

    override val isInvalid: Boolean
        get() {
        registerObserverIfNecessary()
        db.invalidationTracker.refreshVersionsSync()
        return super.isInvalid
    }

    protected abstract fun convertRows(cursor: Cursor): List<T>

    @Suppress("deprecation")
    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
        registerObserverIfNecessary()
        val onResultCaller: () -> Unit
        db.beginTransaction()
        try {
            val totalCount = countItems()
            if (totalCount != 0) {
                // bound the size requested, based on known count
                val firstLoadPosition = computeInitialLoadPosition(params, totalCount)
                val firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount)
                db.query(getSQLiteQuery(firstLoadPosition, firstLoadSize)).use { cursor ->
                    val rows = convertRows(cursor)
                    db.setTransactionSuccessful()
                    onResultCaller = { callback.onResult(rows, firstLoadPosition, totalCount) }
                }
            } else {
                onResultCaller = { callback.onResult(emptyList(), 0, totalCount) }
            }
        } finally {
            db.endTransaction()
        }
        onResultCaller.invoke()
    }

    override fun loadRange(
        params: LoadRangeParams,
        callback: LoadRangeCallback<T>
    ) {
        callback.onResult(loadRange(params.startPosition, params.loadSize))
    }

    /**
     * Return the rows from startPos to startPos + loadCount
     *
     * @hide
     */
    @Suppress("deprecation")
    fun loadRange(startPosition: Int, loadCount: Int): List<T> {
        val sqLiteQuery = getSQLiteQuery(startPosition, loadCount)
        try {
            if (inTransaction) {
                db.beginTransaction()
                try {
                    db.query(sqLiteQuery).use { cursor ->
                        val rows = convertRows(cursor)
                        db.setTransactionSuccessful()
                        return rows
                    }
                } finally {
                    db.endTransaction()
                }
            } else {
                db.query(sqLiteQuery).use { cursor ->
                    return convertRows(cursor)
                }
            }
        } finally {
            sqLiteQuery.release()
        }
    }

    private fun getSQLiteQuery(startPosition: Int, loadCount: Int): RoomSQLiteQuery {
        val sqLiteQuery = RoomSQLiteQuery.acquire(
            limitOffsetQuery,
            sourceQuery.argCount + 2
        )
        sqLiteQuery.copyArgumentsFrom(sourceQuery)
        sqLiteQuery.bindLong(sqLiteQuery.argCount - 1, loadCount.toLong())
        sqLiteQuery.bindLong(sqLiteQuery.argCount, startPosition.toLong())
        return sqLiteQuery
    }
}