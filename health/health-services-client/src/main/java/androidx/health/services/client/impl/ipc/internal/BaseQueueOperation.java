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

import static com.google.common.base.Preconditions.checkNotNull;

import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * Abstract implementation of QueueOperation that accepts {@link ConnectionConfiguration} describing
 * the service where it will be executed.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class BaseQueueOperation implements QueueOperation {
    private final ConnectionConfiguration mConnectionConfiguration;

    public BaseQueueOperation(ConnectionConfiguration connectionConfiguration) {
        this.mConnectionConfiguration = checkNotNull(connectionConfiguration);
    }

    @Override
    public void execute(IBinder binder) throws RemoteException {}

    @Override
    public void setException(Throwable exception) {}

    @Override
    public QueueOperation trackExecution(ExecutionTracker tracker) {
        return this;
    }

    /** Configuration of the service connection on which the operation will be executed. */
    @Override
    public ConnectionConfiguration getConnectionConfiguration() {
        return mConnectionConfiguration;
    }
}
