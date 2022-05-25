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

package androidx.health.platform.client.impl.ipc;

import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * General operation that will be executed against given remote service.
 *
 * @param <S> type of the remote service
 * @param <R> type of the returned value
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public interface RemoteOperation<S, R> {

    /**
     * Executes a task against the remote service.
     *
     * @param service the already connected remote service to execute the task against
     * @return the operation result
     * @throws RemoteException on binder error
     */
    R execute(S service) throws RemoteException;
}
