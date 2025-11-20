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

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.ResultCode.SQLITE_MISUSE
import androidx.sqlite.throwSQLiteException

/**
 * A [SQLiteConnection] implemented by [androidx.sqlite.db.SupportSQLiteDatabase] and that uses the
 * Android's SQLite through the `SupportSQLite` APIs.
 */
public class SupportSQLiteConnection(public val db: SupportSQLiteDatabase) : SQLiteConnection {

    override fun inTransaction(): Boolean = db.inTransaction()

    override fun prepare(sql: String): SQLiteStatement {
        if (db.isOpen) {
            return SupportSQLiteStatement.create(db, sql)
        } else {
            throwSQLiteException(SQLITE_MISUSE, "connection is closed")
        }
    }

    override fun close() {
        db.close()
    }
}
