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

package androidx.camera.core.impl.utils.executor;

import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraXThreads;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A singleton executor which is suitable for audio I/O tasks.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AudioExecutor implements Executor {
    private static volatile Executor sExecutor;

    private final ExecutorService mAudioService =
            Executors.newFixedThreadPool(
                    2,
                    new ThreadFactory() {
                        private static final String THREAD_NAME_STEM =
                                CameraXThreads.TAG + "camerax_audio_%d";

                        private final AtomicInteger mThreadId = new AtomicInteger(0);

                        @Override
                        public Thread newThread(final Runnable r) {
                            Runnable wrapper = () -> {
                                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                                r.run();
                            };
                            Thread t = new Thread(wrapper);
                            t.setName(
                                    String.format(
                                            Locale.US,
                                            THREAD_NAME_STEM,
                                            mThreadId.getAndIncrement()));
                            return t;
                        }
                    });

    static Executor getInstance() {
        if (sExecutor != null) {
            return sExecutor;
        }
        synchronized (AudioExecutor.class) {
            if (sExecutor == null) {
                sExecutor = new AudioExecutor();
            }
        }

        return sExecutor;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        mAudioService.execute(command);
    }
}
