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

package androidx.hilt.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

/**
 * Factory interface to be implemented by generated code.
 *
 * @param <T> The Worker type.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface WorkerAssistedFactory<T extends ListenableWorker> {
    /**
     * Create the Worker.
     *
     * @param context the app context.
     * @param parameters the worker parameters.
     * @return the created Worker.
     */
    @NonNull T create(@NonNull Context context, @NonNull WorkerParameters parameters);
}
