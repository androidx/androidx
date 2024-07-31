/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.sqlite

/**
 * SQLite statement definition.
 *
 * A prepared statement is a resource that must be released once it is no longer needed via its
 * [close] function.
 *
 * See also [Prepared Statement](https://www.sqlite.org/c3ref/stmt.html)
 */
// TODO(b/315461431): No common Closeable interface in KMP
@Suppress("NotCloseable", "AcronymName") // SQL is a known term and should remain capitalized
public interface SQLiteStatement {
    /**
     * Binds a ByteArray value to this statement at an index.
     *
     * @param index the 1-based index of the parameter to bind
     * @param value the value to bind
     */
    public fun bindBlob(index: Int, value: ByteArray)

    /**
     * Binds a Double value to this statement at an index.
     *
     * @param index the 1-based index of the parameter to bind
     * @param value the value to bind
     */
    public fun bindDouble(index: Int, value: Double)

    /**
     * Binds a Float value to this statement at an index.
     *
     * @param index the 1-based index of the parameter to bind
     * @param value the value to bind
     */
    public fun bindFloat(index: Int, value: Float) {
        bindDouble(index, value.toDouble())
    }

    /**
     * Binds a Long value to this statement at an index.
     *
     * @param index the 1-based index of the parameter to bind
     * @param value the value to bind
     */
    public fun bindLong(index: Int, value: Long)

    /**
     * Binds a Int value to this statement at an index.
     *
     * @param index the 1-based index of the parameter to bind
     * @param value the value to bind
     */
    public fun bindInt(index: Int, value: Int) {
        bindLong(index, value.toLong())
    }

    /**
     * Binds a Boolean value to this statement at an index.
     *
     * @param index the 1-based index of the parameter to bind
     * @param value the value to bind
     */
    public fun bindBoolean(index: Int, value: Boolean) {
        bindLong(index, if (value) 1L else 0L)
    }

    /**
     * Binds a String value to this statement at an index.
     *
     * @param index the 1-based index of the parameter to bind
     * @param value the value to bind
     */
    public fun bindText(index: Int, value: String)

    /**
     * Binds a NULL value to this statement at an index.
     *
     * @param index the 1-based index of the parameter to bind
     */
    public fun bindNull(index: Int)

    /**
     * Returns the value of the column at [index] as a ByteArray.
     *
     * @param index the 0-based index of the column
     * @return the value of the column
     */
    public fun getBlob(index: Int): ByteArray

    /**
     * Returns the value of the column at [index] as a Double.
     *
     * @param index the 0-based index of the column
     * @return the value of the column
     */
    public fun getDouble(index: Int): Double

    /**
     * Returns the value of the column at [index] as a Float.
     *
     * @param index the 0-based index of the column
     * @return the value of the column
     */
    public fun getFloat(index: Int): Float {
        return getDouble(index).toFloat()
    }

    /**
     * Returns the value of the column at [index] as a Long.
     *
     * @param index the 0-based index of the column
     * @return the value of the column
     */
    public fun getLong(index: Int): Long

    /**
     * Returns the value of the column at [index] as a Int.
     *
     * @param index the 0-based index of the column
     * @return the value of the column
     */
    public fun getInt(index: Int): Int {
        return getLong(index).toInt()
    }

    /**
     * Returns the value of the column at [index] as a Boolean.
     *
     * @param index the 0-based index of the column
     * @return the value of the column
     */
    public fun getBoolean(index: Int): Boolean {
        return getLong(index) != 0L
    }

    /**
     * Returns the value of the column at [index] as a String.
     *
     * @param index the 0-based index of the column
     * @return the value of the column
     */
    public fun getText(index: Int): String

    /**
     * Returns true if the value of the column at [index] is NULL.
     *
     * @param index the 0-based index of the column
     * @return true if the column value is NULL, false otherwise
     */
    public fun isNull(index: Int): Boolean

    /**
     * Returns the number of columns in the result of the statement.
     *
     * @return the number of columns
     */
    public fun getColumnCount(): Int

    /**
     * Returns the name of a column at [index] in the result of the statement.
     *
     * @param index the 0-based index of the column
     * @return the name of the column
     */
    public fun getColumnName(index: Int): String

    /**
     * Returns the name of the columns in the result of the statement ordered by their index.
     *
     * @return the names of the columns
     */
    public fun getColumnNames(): List<String> {
        return List(getColumnCount()) { i -> getColumnName(i) }
    }

    /**
     * Executes the statement and evaluates the next result row if available.
     *
     * A statement is initially prepared and compiled but is not executed until one or more calls to
     * this function. If the statement execution produces result rows then this function will return
     * `true` indicating there is a new row of data ready to be read.
     *
     * @return true if there are more rows to evaluate or false if the statement is done executing
     */
    public fun step(): Boolean

    /**
     * Resets the prepared statement back to initial state so that it can be re-executed via [step].
     * Any parameter bound via the bind*() APIs will retain their value.
     */
    public fun reset()

    /** Clears all parameter bindings. Unset bindings are treated as NULL. */
    public fun clearBindings()

    /**
     * Closes the statement.
     *
     * Once a statement is closed it should no longer be used. Calling this function on an already
     * closed statement is a no-op.
     */
    public fun close()
}
