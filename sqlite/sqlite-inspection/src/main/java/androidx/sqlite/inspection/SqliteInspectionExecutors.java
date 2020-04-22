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

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

class SqliteInspectionExecutors {

    private SqliteInspectionExecutors() {}

    private enum DirectExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    }

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        AtomicLong mNextId = new AtomicLong(0);

        @Override
        public Thread newThread(@NonNull Runnable target) {
            Thread thread = new Thread(target, generateThreadName());
            thread.setDaemon(true); // Don't prevent JVM from exiting
            return thread;
        }

        private String generateThreadName() {
            return String.format(Locale.ROOT, "Studio: SqliteInspector thread-%d",
                    mNextId.getAndIncrement());
        }
    };

    static Executor directExecutor() {
        return DirectExecutor.INSTANCE;
    }

    /**
     * Thread factory satisfying required thread naming conventions.
     */
    static ThreadFactory threadFactory() {
        return sThreadFactory;
    }

    static Future<Void> submit(Executor executor, Runnable runnable) {
        FutureTask<Void> task = new FutureTask<>(runnable, null);
        executor.execute(task);
        return task;
    }
}
