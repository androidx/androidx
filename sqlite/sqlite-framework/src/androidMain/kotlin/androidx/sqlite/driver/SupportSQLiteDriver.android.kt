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

package androidx.sqlite.driver

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SupportSQLiteOpenHelper

/**
 * A [SQLiteDriver] implemented by [androidx.sqlite.db.SupportSQLiteOpenHelper] and that uses the
 * Android's SQLite through the `SupportSQLite` APIs.
 *
 * This driver internally has a connection pool and a opened connection from it is safe to be used
 * in a multi-thread and concurrent environment.
 *
 * This driver can only open connections whose `fileName` match the given [openHelper] database name
 * ([SupportSQLiteOpenHelper.databaseName]) or `:memory:` if the [openHelper] was configured with a
 * `null` name, indicating an in-memory database.
 */
public class SupportSQLiteDriver(private val openHelper: SupportSQLiteOpenHelper) : SQLiteDriver {

    @Suppress("INAPPLICABLE_JVM_NAME") // Due to KT-31420
    @get:JvmName("hasConnectionPool")
    override val hasConnectionPool: Boolean
        get() = true

    override fun open(fileName: String): SupportSQLiteConnection {
        val openHelperDatabaseName = openHelper.databaseName
        if (openHelperDatabaseName == null) {
            require(fileName == ":memory:") {
                "This driver is configured to open an in-memory database but a file-based named " +
                    "'$fileName' was requested."
            }
        } else {
            require(
                openHelperDatabaseName == fileName ||
                    openHelperDatabaseName.substringAfterLast('/') ==
                        fileName.substringAfterLast('/')
            ) {
                "This driver is configured to open a database named '${openHelper.databaseName}' " +
                    "but '$fileName' was requested."
            }
        }
        return SupportSQLiteConnection(openHelper.writableDatabase)
    }
}
