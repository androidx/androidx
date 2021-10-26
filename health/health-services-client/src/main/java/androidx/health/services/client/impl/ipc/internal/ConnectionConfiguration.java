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

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * Internal representation of configuration of IPC service connection.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public final class ConnectionConfiguration {
    private final String mPackageName;
    private final String mClientName;
    private final String mBindAction;
    private final QueueOperation mRefreshVersionOperation;

    public ConnectionConfiguration(
            String packageName,
            String clientName,
            String bindAction,
            QueueOperation refreshVersionOperation) {
        this.mPackageName = checkNotNull(packageName);
        this.mClientName = checkNotNull(clientName);
        this.mBindAction = checkNotNull(bindAction);
        this.mRefreshVersionOperation = checkNotNull(refreshVersionOperation);
    }

    /** A key that defines the connection among other IPC connections. It should be unique. */
    String getKey() {
        return String.format("%s#%s#%s", mClientName, mPackageName, mBindAction);
    }

    /** Returns name of the service, use for logging and debugging only. */
    String getClientName() {
        return mClientName;
    }

    /** Returns the action used to bind to the remote service. */
    String getBindAction() {
        return mBindAction;
    }

    /** Returns the application package of the remote service. */
    String getPackageName() {
        return mPackageName;
    }

    QueueOperation getRefreshVersionOperation() {
        return mRefreshVersionOperation;
    }
}
