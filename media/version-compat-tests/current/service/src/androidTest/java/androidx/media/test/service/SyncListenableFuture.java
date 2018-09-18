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

package androidx.media.test.service;

import androidx.media2.CommandResult2;
import androidx.media2.MediaItem2;
import androidx.media2.SessionPlayer2;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implements {@link ListenableFuture} for synchrous calls.
 */
public class SyncListenableFuture implements ListenableFuture<CommandResult2> {
    private final CommandResult2 mResult;

    SyncListenableFuture(MediaItem2 item) {
        mResult = new CommandResult2(SessionPlayer2.RESULT_CODE_NO_ERROR, item);
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
    public CommandResult2 get() throws InterruptedException, ExecutionException {
        return mResult;
    }

    @Override
    public CommandResult2 get(long l, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return mResult;
    }
}
