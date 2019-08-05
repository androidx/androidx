/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.benchmark

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executor

/**
 * An [Executor] where we can await termination of all commands.
 */
class DispatchingExecutor : Executor {
    private val job = CompletableDeferred<Unit>()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    override fun execute(command: Runnable) {
        scope.launch {
            command.run()
        }
    }

    fun runAllCommands() {
        runBlocking {
            job.complete(Unit)
            job.join()
        }
    }
}
