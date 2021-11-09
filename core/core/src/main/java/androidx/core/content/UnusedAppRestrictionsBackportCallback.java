/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.content;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportCallback;

/**
 * Wrapper class for {IUnusedAppRestrictionsBackportCallback}.
 *
 * Callback object to return the results from the
 * {@link UnusedAppRestrictionsBackportService} APIs.
 */
public class UnusedAppRestrictionsBackportCallback {

    private IUnusedAppRestrictionsBackportCallback mCallback;

    /** @hide */
    @RestrictTo(LIBRARY)
    public UnusedAppRestrictionsBackportCallback(
            @NonNull IUnusedAppRestrictionsBackportCallback callback) {
        mCallback = callback;
    }

    /**
     * This will be called with the results of the
     * {@link UnusedAppRestrictionsBackportService#isPermissionRevocationEnabled} API.
     *
     * @param success false if there was an error while checking if the app is
     * enabled, otherwise true.
     * @param enabled true if permission revocation is enabled for the app,
     * otherwise false.
     */
    public void onResult(boolean success, boolean enabled) throws RemoteException {
        mCallback.onIsPermissionRevocationEnabledForAppResult(success, enabled);
    }
}
