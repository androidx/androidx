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
 * Blocking methods for {@link WorkManager} operations.  These methods are expected to be called
 * from a background thread.
 */
public interface BlockingWorkManager {

    /**
     * Cancels work with the given id in a blocking fashion if it isn't finished.  Note that
     * cancellation is dependent on timing (for example, the work could have completed in a
     * different thread just as you issue this call).  Use {@link #getStatusByIdBlocking(String)} to
     * find out the actual state of the work after this call.  This method is expected to be called
     * from a background thread.
     *
     * @param id The id of the work
     */
    @WorkerThread
    void cancelWorkByIdBlocking(@NonNull String id);


    /**
     * Cancels all unfinished work with the given tag in a blocking fashion.  Note that cancellation
     * is dependent on timing (for example, the work could have completed in a different thread just
     * as you issue this call).  Use {@link #getStatusByIdBlocking(String)} to find out the actual
     * state of the work after this call.  This method is expected to be called from a background
     * thread.
     *
     * @param tag The tag used to identify the work
     */
    @WorkerThread
    void cancelAllWorkByTagBlocking(@NonNull String tag);

    /**
     * Cancels all unfinished work in the work chain with the given name in a blocking fashion.
     * Note that cancellation is dependent on timing (for example, the work could have completed in
     * a different thread just as you issue this call).  Use {@link #getStatusByIdBlocking(String)}
     * to find out the actual state of the work after this call.  This method is expected to be
     * called from a background thread.
     *
     * @param name The name used to identify the chain of work
     */
    @WorkerThread
    void cancelAllWorkByNameBlocking(@NonNull String name);

    /**
     * Gets the {@link WorkStatus} of a given work id in a blocking fashion.  This method is
     * expected to be called from a background thread.
     *
     * @param id The id of the work
     * @return A {@link WorkStatus} associated with {@code id}
     */
    @WorkerThread
    WorkStatus getStatusByIdBlocking(@NonNull String id);

    /**
     * Gets the {@link WorkStatus} for all work with a given tag in a blocking fashion.  This method
     * is expected to be called from a background thread.
     *
     * @param tag The tag of the work
     * @return A list of {@link WorkStatus} for work tagged with {@code tag}
     */
    @WorkerThread
    List<WorkStatus> getStatusesByTagBlocking(@NonNull String tag);

    /**
     * Gets the {@link WorkStatus} for all work for the chain of work with a given name in a
     * blocking fashion.  This method is expected to be called from a background thread.
     *
     * @param name The name used to identify the chain of work
     * @return A list of {@link WorkStatus} for work in the chain named {@code name}
     */
    @WorkerThread
    List<WorkStatus> getStatusesByNameBlocking(@NonNull String name);
}
