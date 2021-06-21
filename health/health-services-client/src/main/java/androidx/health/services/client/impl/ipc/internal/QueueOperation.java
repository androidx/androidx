/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl.ipc.internal;

import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * A wrapper for SDK operation that will be executed on a connected binder. It is intended for
 * scheduling in execution queue.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public interface QueueOperation {
    /**
     * Method executed against the service.
     *
     * @param binder already connected to the target service.
     */
    void execute(IBinder binder) throws RemoteException;

    /** Sets exception as the result of the operation. */
    void setException(Throwable exception);

    /**
     * Tracks the operation execution with an {@link ExecutionTracker}.
     *
     * @param tracker To track the execution as in progress.
     */
    QueueOperation trackExecution(ExecutionTracker tracker);

    /** Returns configuration of the service connection on which the operation will be executed. */
    ConnectionConfiguration getConnectionConfiguration();
}
