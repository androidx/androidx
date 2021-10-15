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

package androidx.work.testing;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Data;
import androidx.work.Logger;
import androidx.work.ProgressUpdater;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.UUID;

/**
 * A {@link ProgressUpdater} which does nothing. Useful in the context of testing.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TestProgressUpdater implements ProgressUpdater {
    private static final String TAG = Logger.tagWithPrefix("TestProgressUpdater");

    @NonNull
    @Override
    public ListenableFuture<Void> updateProgress(
            @NonNull Context context,
            @NonNull UUID id,
            @NonNull Data data) {
        Logger.get().info(TAG, "Updating progress for " + id + " (" + data + ")");
        SettableFuture<Void> future = SettableFuture.create();
        future.set(null);
        return future;
    }
}
