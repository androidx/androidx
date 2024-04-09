/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work

import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

/**
 * A simple [Executor].
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class DirectExecutor : Executor {
    /**
     * Inlined by old LF.await() implementation, so mustn't be removed.
     * See aosp/2960744
     */
    INSTANCE;

    override fun execute(command: Runnable) {
        command.run()
    }

    override fun toString(): String {
        return "DirectExecutor"
    }
}
