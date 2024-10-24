/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.platform.client.impl.sdkservice;

import static java.util.Arrays.stream;

import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.BinderThread;
import androidx.annotation.VisibleForTesting;
import androidx.health.platform.client.impl.permission.foregroundstate.ForegroundStateChecker;
import androidx.health.platform.client.impl.permission.token.PermissionTokenManager;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

final class HealthDataSdkServiceStubImpl extends IHealthDataSdkService.Stub {
    private static final String TAG = HealthDataSdkServiceStubImpl.class.getSimpleName();

    @VisibleForTesting
    static final String ALLOWED_PACKAGE_NAME = "com.google.android.apps.healthdata";

    private final Context mContext;
    private final Executor mExecutor;

    HealthDataSdkServiceStubImpl(Context context, Executor executor) {
        this.mContext = context;
        this.mExecutor = executor;
    }

    @Override
    public void setPermissionToken(
            @NonNull String healthDataPackageName,
            @NonNull String permissionToken,
            @NonNull ISetPermissionTokenCallback callback) {
        verifyPackageName(healthDataPackageName);
        mExecutor.execute(
                () -> {
                    PermissionTokenManager.setCurrentToken(mContext, permissionToken);
                    try {
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        Log.e(
                                TAG,
                                String.format(
                                        "HealthDataSdkService#setPermissionToken failed: %s",
                                        e.getMessage()));
                    }
                });
    }

    @Override
    public void getPermissionToken(
            @NonNull String healthDataPackageName, @NonNull IGetPermissionTokenCallback callback) {
        verifyPackageName(healthDataPackageName);
        mExecutor.execute(
                () -> {
                    try {
                        String currentToken = PermissionTokenManager.getCurrentToken(mContext);
                        callback.onSuccess(currentToken == null ? "" : currentToken);
                    } catch (RemoteException e) {
                        Log.e(
                                TAG,
                                String.format(
                                        "HealthDataSdkService#getPermissionToken failed: %s",
                                        e.getMessage()));
                    }
                });
    }

    @Override
    public void getIsInForeground(
            @NonNull String healthDataPackageName, @NonNull IGetIsInForegroundCallback callback) {
        verifyPackageName(healthDataPackageName);
        mExecutor.execute(
                () -> {
                    try {
                        callback.onSuccess(ForegroundStateChecker.isInForeground());
                    } catch (RemoteException e) {
                        Log.e(
                                TAG,
                                String.format(
                                        "HealthDataSdkService#getIsInForeground failed: %s",
                                        e.getMessage()));
                    }
                });
    }

    @BinderThread
    private void verifyPackageName(@NonNull String packageName) {
        String[] callingApp =
                mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packageName == null
                || callingApp == null
                || stream(callingApp).noneMatch(packageName::equals)) {
            throw new SecurityException("Invalid package name!");
        }

        if (!ALLOWED_PACKAGE_NAME.equals(packageName)) {
            throw new SecurityException("Not allowed!");
        }
    }
}
