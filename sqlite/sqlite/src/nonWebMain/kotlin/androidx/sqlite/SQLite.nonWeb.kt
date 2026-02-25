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
@file:JvmMultifileClass
@file:JvmName("SQLite")

package androidx.sqlite

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Opens a new database connection.
 *
 * @see [SQLiteDriver.open]
 */
public actual suspend fun SQLiteDriver.open(fileName: String): SQLiteConnection {
    return this.open(fileName)
}

/**
 * Prepares a new SQL statement.
 *
 * @see [SQLiteConnection.prepare]
 */
public actual suspend fun SQLiteConnection.prepare(sql: String): SQLiteStatement {
    return this.prepare(sql)
}

/** Executes a single SQL statement that returns no values. */
public fun SQLiteConnection.execSQL(sql: String) {
    this.prepare(sql).use { it.step() }
}

/** Executes a single SQL statement that returns no values. */
public actual suspend fun SQLiteConnection.executeSQL(sql: String) {
    this.prepare(sql).use { it.step() }
}

/**
 * Executes the statement and evaluates the next result row if available.
 *
 * @see [SQLiteStatement.step]
 */
public actual suspend fun SQLiteStatement.step(): Boolean {
    return this.step()
}
