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
 * SQLite connection definition.
 *
 * A connection to a database is a resource that must be released once it is no longer needed via
 * its [close] function.
 *
 * See also [Database Connection](https://www.sqlite.org/c3ref/sqlite3.html)
 */
// TODO(b/315461431): No common Closeable interface in KMP
@Suppress("NotCloseable", "AcronymName") // SQL is a known term and should remain capitalized
public interface SQLiteConnection {
    /**
     * Prepares a new SQL statement.
     *
     * See also [Compiling a SQL statement](https://www.sqlite.org/c3ref/prepare.html)
     *
     * @param sql the SQL statement to prepare
     * @return the prepared statement.
     */
    public fun prepare(sql: String): SQLiteStatement

    /**
     * Closes the database connection.
     *
     * Once a connection is closed it should no longer be used. Calling this function on an already
     * closed database connection is a no-op.
     */
    public fun close()
}
