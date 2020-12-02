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

package androidx.browser.trusted;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.trusted.ITrustedWebActivityCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TrustedWebActivityCallbackRemote is used by client app to execute callbacks from a binder of
 * {@link TrustedWebActivityCallback}.
 */
public class TrustedWebActivityCallbackRemote {
    private final ITrustedWebActivityCallback mCallbackBinder;

    private TrustedWebActivityCallbackRemote(@NonNull ITrustedWebActivityCallback callbackBinder) {
        mCallbackBinder = callbackBinder;
    }

    /**
     * Create a TrustedWebActivityCallbackRemote from a binder.
     */
    @Nullable
    static TrustedWebActivityCallbackRemote fromBinder(@Nullable IBinder binder) {
        ITrustedWebActivityCallback callbackBinder = binder == null ? null :
                ITrustedWebActivityCallback.Stub.asInterface(binder);
        if (callbackBinder == null) {
            return null;
        }
        return new TrustedWebActivityCallbackRemote(callbackBinder);
    }

    /**
     * Runs the free-form callbacks that may be provided by the implementation.
     *
     * @param callbackName Name of the extra callback.
     * @param args         Arguments for the callback
     */
    public void runExtraCallback(@NonNull String callbackName, @NonNull Bundle args)
            throws RemoteException {
        mCallbackBinder.onExtraCallback(callbackName, args);
    }
}
