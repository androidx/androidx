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

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportCallback;
import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportService;

/**
 * Wrapper class for {@link IUnusedAppRestrictionsBackportService}.
 *
 * The {@link UnusedAppRestrictionsBackportService} provides information about whether the
 * calling application is enabled for permission revocation.
 *
 * This class can be extended by OEMs to provide a custom permission revocation setting retrieval
 * implementation. However, note that the implementation _must_ be by the "package verifier" on
 * the device.
 */
public abstract class UnusedAppRestrictionsBackportService extends Service {

    /**
     * The Intent action that a {@link UnusedAppRestrictionsBackportService} must respond to.
     */
    @SuppressLint("ActionValue")
    public static final String ACTION_UNUSED_APP_RESTRICTIONS_BACKPORT_CONNECTION =
            "android.support.unusedapprestrictions.action"
                    + ".CustomUnusedAppRestrictionsBackportService";

    private IUnusedAppRestrictionsBackportService.Stub mBinder =
            new IUnusedAppRestrictionsBackportService.Stub() {
        @Override
        public void isPermissionRevocationEnabledForApp(
                @Nullable IUnusedAppRestrictionsBackportCallback callback) throws RemoteException {
            if (callback == null) {
                return;
            }

            UnusedAppRestrictionsBackportCallback backportCallback =
                    new UnusedAppRestrictionsBackportCallback(callback);

            UnusedAppRestrictionsBackportService.this.isPermissionRevocationEnabled(
                    backportCallback);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        return mBinder;
    }

    @SuppressWarnings("ExecutorRegistration")
    protected abstract void isPermissionRevocationEnabled(
            @NonNull UnusedAppRestrictionsBackportCallback callback);
}
