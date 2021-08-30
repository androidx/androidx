/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.collection

import kotlin.native.concurrent.TransferMode.SAFE
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

actual fun testBody(body: () -> Unit) {
    // Run the test body twice, once on main and once on a worker. These are encapsulated in
    // functions so that the stacktrace tells you which failed.
    runOnMainThread(body)
    runOnWorkerThread(body)
}

private fun runOnMainThread(body: () -> Unit) {
    body()
}

private fun runOnWorkerThread(body: () -> Unit) {
    body.freeze()

    val worker = Worker.start()
    val future = worker.execute(SAFE, { body }) {
        runCatching(it)
    }
    future.result.getOrThrow()
}
