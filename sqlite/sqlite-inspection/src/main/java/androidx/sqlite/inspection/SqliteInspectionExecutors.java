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

package androidx.sqlite.inspection;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

class SqliteInspectionExecutors {

    private SqliteInspectionExecutors() {}

    private enum DirectExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    }

    static Executor directExecutor() {
        return DirectExecutor.INSTANCE;
    }

    static Future<Void> submit(Executor executor, Runnable runnable) {
        FutureTask<Void> task = new FutureTask<>(runnable, null);
        executor.execute(task);
        return task;
    }
}
