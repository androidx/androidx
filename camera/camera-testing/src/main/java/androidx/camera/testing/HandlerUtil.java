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

package androidx.camera.testing;

import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;

import androidx.annotation.RequiresApi;
import androidx.camera.testing.compat.LooperCompat;

import java.util.concurrent.Semaphore;

/** Utility functions for {@link Handler} */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class HandlerUtil {
    /**
     * Wait for the {@link Looper} of the given {@link Handler} to idle.
     *
     * @throws InterruptedException if thread for the handler is interrupted while waiting
     * @throws RuntimeException if unable to obtain the {@link MessageQueue} for the {@link
     * Handler}.
     */
    public static void waitForLooperToIdle(Handler handler) throws InterruptedException {
        final Looper looper = handler.getLooper();
        final Semaphore semaphore = new Semaphore(0);

        // Post a message that will add the idle handler. This will ensure the handler is not
        // already idle before setting the idle handler, causing the idle handler to never be
        // called.
        handler.post(new Runnable() {
            @Override
            public void run() {
                MessageQueue messageQueue = LooperCompat.getQueue(looper);
                messageQueue.addIdleHandler(new MessageQueue.IdleHandler() {
                    @Override
                    public boolean queueIdle() {
                        semaphore.release();
                        return false;
                    }
                });
            }
        });

        // Wait for idle
        semaphore.acquire();
    }

    private HandlerUtil() {
    }
}
