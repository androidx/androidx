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

package androidx.health.services.client.impl.ipc;

import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.common.util.concurrent.SettableFuture;

/**
 * General operation that will be executed against given service. A new operation can be created by
 * implementing this interface. User is then responsible for setting the result Future with the
 * result value.
 *
 * @param <R> Type of the returned value.
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public interface ServiceOperation<R> {

    /**
     * Method executed against the service.
     *
     * @param binder Already connected binder to the target service.
     * @param resultFuture A {@link SettableFuture} that should be set with the result.
     */
    void execute(IBinder binder, SettableFuture<R> resultFuture) throws RemoteException;
}
