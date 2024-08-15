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
package androidx.room

import android.annotation.SuppressLint
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import java.util.TreeMap

/**
 * This class is used as an intermediate place to keep binding arguments so that we can run Cursor
 * queries with correct types rather than passing everything as a string.
 *
 * Because it is relatively a big object, they are pooled and must be released after each use.
 */
@SuppressLint("WrongConstant")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class RoomSQLiteQuery private constructor(@field:VisibleForTesting val capacity: Int) :
    SupportSQLiteQuery, SupportSQLiteProgram {
    @Volatile private var query: String? = null

    @JvmField @VisibleForTesting val longBindings: LongArray

    @JvmField @VisibleForTesting val doubleBindings: DoubleArray

    @JvmField @VisibleForTesting val stringBindings: Array<String?>

    @JvmField @VisibleForTesting val blobBindings: Array<ByteArray?>

    @Binding private val bindingTypes: IntArray

    // number of arguments in the query
    override var argCount = 0
        private set

    fun init(query: String, initArgCount: Int) {
        this.query = query
        argCount = initArgCount
    }

    init {
        // because, 1 based indices... we don't want to offsets everything with 1 all the time.
        val limit = capacity + 1
        bindingTypes = IntArray(limit)
        longBindings = LongArray(limit)
        doubleBindings = DoubleArray(limit)
        stringBindings = arrayOfNulls(limit)
        blobBindings = arrayOfNulls(limit)
    }

    /**
     * Releases the query back to the pool.
     *
     * After released, the statement might be returned when [.acquire] is called so you should never
     * re-use it after releasing.
     */
    fun release() {
        synchronized(queryPool) {
            queryPool[capacity] = this
            prunePoolLocked()
        }
    }

    override val sql: String
        get() = checkNotNull(this.query)

    override fun bindTo(statement: SupportSQLiteProgram) {
        for (index in 1..argCount) {
            when (bindingTypes[index]) {
                NULL -> statement.bindNull(index)
                LONG -> statement.bindLong(index, longBindings[index])
                DOUBLE -> statement.bindDouble(index, doubleBindings[index])
                STRING -> statement.bindString(index, requireNotNull(stringBindings[index]))
                BLOB -> statement.bindBlob(index, requireNotNull(blobBindings[index]))
            }
        }
    }

    fun bindTo(statement: SQLiteStatement) {
        for (index in 1..argCount) {
            when (bindingTypes[index]) {
                NULL -> statement.bindNull(index)
                LONG -> statement.bindLong(index, longBindings[index])
                DOUBLE -> statement.bindDouble(index, doubleBindings[index])
                STRING -> statement.bindText(index, requireNotNull(stringBindings[index]))
                BLOB -> statement.bindBlob(index, requireNotNull(blobBindings[index]))
            }
        }
    }

    override fun bindNull(index: Int) {
        bindingTypes[index] = NULL
    }

    override fun bindLong(index: Int, value: Long) {
        bindingTypes[index] = LONG
        longBindings[index] = value
    }

    override fun bindDouble(index: Int, value: Double) {
        bindingTypes[index] = DOUBLE
        doubleBindings[index] = value
    }

    override fun bindString(index: Int, value: String) {
        bindingTypes[index] = STRING
        stringBindings[index] = value
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        bindingTypes[index] = BLOB
        blobBindings[index] = value
    }

    override fun close() {
        // no-op. not calling release because it is internal API.
    }

    /**
     * Copies arguments from another RoomSQLiteQuery into this query.
     *
     * @param other The other query, which holds the arguments to be copied.
     */
    fun copyArgumentsFrom(other: RoomSQLiteQuery) {
        val argCount = other.argCount + 1 // +1 for the binding offsets
        System.arraycopy(other.bindingTypes, 0, bindingTypes, 0, argCount)
        System.arraycopy(other.longBindings, 0, longBindings, 0, argCount)
        System.arraycopy(other.stringBindings, 0, stringBindings, 0, argCount)
        System.arraycopy(other.blobBindings, 0, blobBindings, 0, argCount)
        System.arraycopy(other.doubleBindings, 0, doubleBindings, 0, argCount)
    }

    override fun clearBindings() {
        bindingTypes.fill(NULL)
        stringBindings.fill(null)
        blobBindings.fill(null)
        query = null
        // no need to clear others
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(NULL, LONG, DOUBLE, STRING, BLOB)
    internal annotation class Binding

    companion object {
        // Maximum number of queries we'll keep cached.
        @VisibleForTesting const val POOL_LIMIT = 15

        // Once we hit POOL_LIMIT, we'll bring the pool size back to the desired number. We always
        // clear the bigger queries (# of arguments).
        @VisibleForTesting const val DESIRED_POOL_SIZE = 10

        @JvmField @VisibleForTesting val queryPool = TreeMap<Int, RoomSQLiteQuery>()

        /**
         * Copies the given SupportSQLiteQuery and converts it into RoomSQLiteQuery.
         *
         * @param supportSQLiteQuery The query to copy from
         * @return A new query copied from the provided one.
         */
        @JvmStatic
        fun copyFrom(supportSQLiteQuery: SupportSQLiteQuery): RoomSQLiteQuery {
            val query = acquire(supportSQLiteQuery.sql, supportSQLiteQuery.argCount)

            supportSQLiteQuery.bindTo(object : SupportSQLiteProgram by query {})
            return query
        }

        /**
         * Returns a new RoomSQLiteQuery that can accept the given number of arguments and holds the
         * given query.
         *
         * @param query The query to prepare
         * @param argumentCount The number of query arguments
         * @return A RoomSQLiteQuery that holds the given query and has space for the given number
         *   of arguments.
         */
        @JvmStatic
        fun acquire(query: String, argumentCount: Int): RoomSQLiteQuery {
            synchronized(queryPool) {
                val entry = queryPool.ceilingEntry(argumentCount)
                if (entry != null) {
                    queryPool.remove(entry.key)
                    val sqliteQuery = entry.value
                    sqliteQuery.init(query, argumentCount)
                    return sqliteQuery
                }
            }
            val sqLiteQuery = RoomSQLiteQuery(argumentCount)
            sqLiteQuery.init(query, argumentCount)
            return sqLiteQuery
        }

        internal fun prunePoolLocked() {
            if (queryPool.size > POOL_LIMIT) {
                var toBeRemoved = queryPool.size - DESIRED_POOL_SIZE
                val iterator = queryPool.descendingKeySet().iterator()
                while (toBeRemoved-- > 0) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }

        private const val NULL = 1
        private const val LONG = 2
        private const val DOUBLE = 3
        private const val STRING = 4
        private const val BLOB = 5
    }
}
