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

package androidx.media2.test.service;

import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_SUCCESS;

import androidx.concurrent.ListenableFuture;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implements {@link ListenableFuture} for synchrous calls.
 */
public class SyncListenableFuture implements ListenableFuture<SessionPlayer.PlayerResult> {
    private final SessionPlayer.PlayerResult mResult;

    SyncListenableFuture(MediaItem item) {
        mResult = new SessionPlayer.PlayerResult(RESULT_SUCCESS, item);
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        executor.execute(listener);
    }

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public SessionPlayer.PlayerResult get() throws InterruptedException, ExecutionException {
        return mResult;
    }

    @Override
    public SessionPlayer.PlayerResult get(long l, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return mResult;
    }
}
