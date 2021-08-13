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

package androidx.work.multiprocess;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Logger;

/**
 * The {@link Service} which hosts an implementation of a {@link androidx.work.ListenableWorker}.
 */
public class RemoteWorkerService extends Service {
    static final String TAG = Logger.tagWithPrefix("RemoteWorkerService");
    private IBinder mBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new ListenableWorkerImpl(this);
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        Logger.get().info(TAG, "Binding to RemoteWorkerService");
        return mBinder;
    }
}
