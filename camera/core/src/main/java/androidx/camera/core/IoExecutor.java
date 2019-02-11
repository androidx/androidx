/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A singleton executor which should be used for I/O tasks.
 *
 * <p>TODO(b/115779693): Make this executor configurable
 */
final class IoExecutor implements Executor {
    private static volatile Executor instance;

    private final ExecutorService ioService =
            Executors.newFixedThreadPool(
                    2,
                    new ThreadFactory() {
                        private static final String THREAD_NAME_STEM =
                                CameraXThreads.TAG + "camerax_io_%d";

                        private final AtomicInteger threadId = new AtomicInteger(0);

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r);
                            t.setName(
                                    String.format(
                                            Locale.US,
                                            THREAD_NAME_STEM,
                                            threadId.getAndIncrement()));
                            return t;
                        }
                    });

    static Executor getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (IoExecutor.class) {
            if (instance == null) {
                instance = new IoExecutor();
            }
        }

        return instance;
    }

    @Override
    public void execute(Runnable command) {
        ioService.execute(command);
    }
}
