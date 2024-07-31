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
package androidx.sqlite.db

import java.io.Closeable

/** An interface to map the behavior of [android.database.sqlite.SQLiteProgram]. */
@Suppress("AcronymName") // SQL is a known term and should remain capitalized
public interface SupportSQLiteProgram : Closeable {
    /**
     * Bind a NULL value to this statement. The value remains bound until [.clearBindings] is
     * called.
     *
     * @param index The 1-based index to the parameter to bind null to
     */
    public fun bindNull(index: Int)

    /**
     * Bind a long value to this statement. The value remains bound until [clearBindings] is called.
     * addToBindArgs
     *
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind
     */
    public fun bindLong(index: Int, value: Long)

    /**
     * Bind a double value to this statement. The value remains bound until [.clearBindings] is
     * called.
     *
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind
     */
    public fun bindDouble(index: Int, value: Double)

    /**
     * Bind a String value to this statement. The value remains bound until [.clearBindings] is
     * called.
     *
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind, must not be null
     */
    public fun bindString(index: Int, value: String)

    /**
     * Bind a byte array value to this statement. The value remains bound until [.clearBindings] is
     * called.
     *
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind, must not be null
     */
    public fun bindBlob(index: Int, value: ByteArray)

    /** Clears all existing bindings. Unset bindings are treated as NULL. */
    public fun clearBindings()
}
