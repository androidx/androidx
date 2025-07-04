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

import kotlin.jvm.JvmName

/** An interface to open database connections. */
public interface SQLiteDriver {

    /**
     * Identifies whether the driver has an internal connection pool or not.
     *
     * A driver with an internal pool should be capable of opening connections that are safe to be
     * used in a multi-thread and concurrent environment whereas a driver that does not have an
     * internal pool will require the application to manage connections in a thread-safe manner. A
     * driver might not report containing a connection pool but might still be safe to be used in a
     * multi-thread environment, such behavior will depend on the driver implementation.
     *
     * The value returned should be used as a signal to higher abstractions in order to determine if
     * the driver and its connections should be managed by an external connection pool or not.
     */
    @Suppress("INAPPLICABLE_JVM_NAME") // Due to KT-31420
    @get:JvmName("hasConnectionPool")
    public val hasConnectionPool: Boolean
        get() = false

    /**
     * Opens a new database connection.
     *
     * To open an in-memory database use the special name `:memory:` as the [fileName].
     *
     * @param fileName Name of the database file.
     * @return the database connection.
     */
    public fun open(fileName: String): SQLiteConnection
}
