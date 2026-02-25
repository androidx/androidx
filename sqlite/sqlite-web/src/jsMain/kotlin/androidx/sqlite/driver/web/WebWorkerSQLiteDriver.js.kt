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

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import org.w3c.dom.Worker

/**
 * A [SQLiteDriver] that communicates with a web worker delegating database operations to the web
 * worker.
 */
public actual class WebWorkerSQLiteDriver(worker: Worker) : SQLiteDriver {

    private val dbWebWorker = DatabaseWebWorkerImpl(worker)

    override val hasConnectionPool: Boolean
        get() = false

    override suspend fun openAsync(fileName: String): SQLiteConnection {
        return dbWebWorker.open(fileName)
    }
}
