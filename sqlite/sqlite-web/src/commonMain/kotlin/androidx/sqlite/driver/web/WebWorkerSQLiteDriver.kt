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

package androidx.sqlite.driver.web

import androidx.sqlite.SQLiteDriver

/**
 * A [SQLiteDriver] that communicates with a web worker delegating database operations to the web
 * worker.
 *
 * The driver communicates with the web worker though a message protocol. All messages, requests and
 * responses are wrapper in a container message format. It contains an `id` to correlate between
 * requests and responses along with the message `data` and an optional `error`. Requests are
 * differentiated from responses because their data have a `cmd` identifier.
 *
 * A request from the driver to the worker has the following structure:
 * ```json
 * {
 *   "id": <number>,
 *   "data": {
 *     "cmd": "<command_name>",
 *     ...
 *   }
 * }
 * ```
 *
 * A response from the worker to the driver has the following structure:
 *
 * **Success:**
 *
 * ```json
 * {
 *   "id": <number>,
 *   "data": { ... }
 * }
 * ```
 *
 * **Error:**
 *
 * ```json
 * {
 *   "id": <number>,
 *   "error": "<error_message>"
 * }
 * ```
 *
 * The list of request commands and responses are as following:
 *
 * #### `open`
 *
 * Opens a new database connection. If successful responds with the database connection unique ID.
 *
 * **Request**
 *
 * ```json
 * {
 *   "cmd": "open",
 *   "fileName": "<string>"
 * }
 * ```
 *
 * **Response**
 *
 * ```json
 * {
 * "databaseId": <number>
 * }
 * ```
 *
 * #### `prepare`
 *
 * Prepares a new SQL statement for execution. If successful responds with the statement unique ID
 * and information about the prepared statement.
 *
 * **Request**
 *
 * ```json
 * {
 *   "cmd": "prepare",
 *   "databaseId": <number>,
 *   "sql": "<string>"
 * }
 * ```
 *
 * **Response:**
 *
 * ```json
 * {
 *   "statementId": <number>,
 *   "parameterCount": <number>,
 *   "columnNames": ["<string>", ...]
 * }
 * ```
 *
 * #### `step`
 *
 * Executes a prepared statement. Involves binding parameters sent in the request and then stepping
 * through all the result rows of the statement to respond with their column result.
 *
 * **Request**
 *
 * ```json
 * {
 *   "cmd": "step",
 *   "statementId": <number>,
 *   "bindings": [...]
 * }
 * ```
 *
 * **Response**
 *
 * ```json
 * {
 *   "rows": [[...], ...],
 *   "columnTypes": [<number>, ...]
 * }
 * ```
 *
 * #### `close`
 *
 * Closes a prepared statement or a database connection. This is a one-way command; no success
 * response is sent.
 *
 * **Request**
 *
 * ```json
 * {
 *   "cmd": "close",
 *   "statementId": <number>,
 *   "databaseId": <number>
 * }
 * ```
 */
public expect class WebWorkerSQLiteDriver : SQLiteDriver
