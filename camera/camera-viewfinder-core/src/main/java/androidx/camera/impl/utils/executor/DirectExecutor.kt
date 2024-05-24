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

package androidx.camera.impl.utils.executor

import java.util.concurrent.Executor

/** An [Executor] that runs each task in the thread that invokes [Executor.execute]. */
internal class DirectExecutor : Executor {
    override fun execute(command: Runnable) {
        command.run()
    }

    companion object {
        @Volatile private var directExecutor: DirectExecutor? = null
        val instance: Executor
            get() {
                if (directExecutor != null) {
                    return directExecutor!!
                }
                synchronized(DirectExecutor::class.java) {
                    if (directExecutor == null) {
                        directExecutor = DirectExecutor()
                    }
                }
                return directExecutor!!
            }
    }
}
