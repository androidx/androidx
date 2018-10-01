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

package androidx.work;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.util.List;

/**
 * Blocking methods for {@link WorkContinuation} operations.  These methods are expected to be
 * called from a background thread.
 */
public interface SynchronousWorkContinuation {
   /**
     * Returns a {@link List} of {@link WorkStatus} that provides information about work,
     * their progress, and any resulting output in the {@link WorkContinuation}.
     *
     * @return A {@link  List} of {@link WorkStatus}es
     */
    @WorkerThread
    @NonNull List<WorkStatus> getStatusesSync();
}
