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

package androidx.sqlite.driver

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.framework.FrameworkSQLiteDatabase

/**
 * A [SQLiteDriver] implemented by [android.database] and that uses the Android's SDK SQLite APIs.
 *
 * This driver internally has a connection pool and a opened connection from it is safe to be used
 * in a multi-thread and concurrent environment.
 */
public class AndroidSQLiteDriver : SQLiteDriver {

    @Suppress("INAPPLICABLE_JVM_NAME") // Due to KT-31420
    @get:JvmName("hasConnectionPool")
    override val hasConnectionPool: Boolean
        get() = true

    override fun open(fileName: String): SQLiteConnection {
        val database = SQLiteDatabase.openOrCreateDatabase(fileName, null)
        return SupportSQLiteConnection(FrameworkSQLiteDatabase(database))
    }
}
