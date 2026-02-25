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

package androidx.driver.web.worker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

/**
 * Create a [WebWorkerSQLiteDriver] whose web worker implementation is `@androidx/sqlite-web-worker`
 * which itself utilizes SQLite WASM (`@sqlite.org/sqlite-wasm`) to persist the database in the
 * Origin-Private File System (OPFS).
 */
public actual fun createDefaultWebWorkerDriver(): WebWorkerSQLiteDriver {
    return WebWorkerSQLiteDriver(jsWorker())
}

private fun jsWorker(): Worker =
    js("""new Worker(new URL("@androidx/sqlite-web-worker/worker.js", import.meta.url))""")
