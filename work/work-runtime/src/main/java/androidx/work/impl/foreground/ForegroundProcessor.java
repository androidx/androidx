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

package androidx.work.impl.foreground;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.ForegroundInfo;

/**
 * An interface that provides {@link androidx.work.impl.WorkerWrapper} the hooks to move a
 * {@link androidx.work.ListenableWorker}s execution to the foreground.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ForegroundProcessor {
    /**
     * Makes the foreground service aware of the {@link androidx.work.impl.model.WorkSpec} id.
     *
     * @param workSpecId     The {@link androidx.work.impl.model.WorkSpec} id
     * @param foregroundInfo The {@link ForegroundInfo} associated
     */
    void startForeground(@NonNull String workSpecId, @NonNull ForegroundInfo foregroundInfo);
}
