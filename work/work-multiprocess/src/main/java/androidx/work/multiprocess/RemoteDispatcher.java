/*
 * Copyright 2021 The Android Open Source Project
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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * @param <T> The remote interface subtype that usually implements {@link android.os.IBinder}.
 * @hide
 */
@SuppressLint("LambdaLast")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteDispatcher<T> {
    /**
     * Perform the actual work given an instance of {@link IWorkManagerImpl} and the
     * {@link IWorkManagerImplCallback} callback.
     *
     * @param binder   the remote interface implementation
     * @param callback the {@link IWorkManagerImplCallback} instance
     */
    void execute(@NonNull T binder,
            @NonNull IWorkManagerImplCallback callback) throws Throwable;
}
