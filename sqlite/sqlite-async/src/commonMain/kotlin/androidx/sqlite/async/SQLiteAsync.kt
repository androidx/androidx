/*
 * Copyright 2026 The Android Open Source Project
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
@file:JvmName("SQLiteAsync")

package androidx.sqlite.async

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Opens a new database connection.
 *
 * On web targets this function is asynchronous while for non-web it is synchronous.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public expect suspend fun SQLiteDriver.open(fileName: String): SQLiteConnection

/**
 * Prepares a new SQL statement.
 *
 * On web targets this function is asynchronous while for non-web it is synchronous.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public expect suspend fun SQLiteConnection.prepare(sql: String): SQLiteStatement

/**
 * Executes a single SQL statement that returns no values.
 *
 * On web targets this function is asynchronous while for non-web it is synchronous.
 */
public expect suspend fun SQLiteConnection.executeSQL(sql: String)

/**
 * Executes the statement and evaluates the next result row if available.
 *
 * On web targets this function is asynchronous while for non-web it is synchronous.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") public expect suspend fun SQLiteStatement.step(): Boolean
