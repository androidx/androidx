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

package androidx.work.multiprocess;

import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Manages callbacks from {@link IWorkManagerImpl}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCallback extends IWorkManagerImplCallback.Stub {
    private final SettableFuture<byte[]> mFuture;

    // The binder represents scope of the request
    private IBinder mBinder;
    private final IBinder.DeathRecipient mRecipient;

    @NonNull
    public ListenableFuture<byte[]> getFuture() {
        return mFuture;
    }

    public RemoteCallback() {
        mBinder = null;
        mFuture = SettableFuture.create();
        mRecipient = new DeathRecipient(this);
    }

    /**
     * Sets the {@link IBinder} which represents the scope / lifetime for this callback.
     *
     * @param binder The instance of {@link IBinder}
     */
    public void setBinder(@NonNull IBinder binder) {
        mBinder = binder;
        try {
            mBinder.linkToDeath(mRecipient, 0);
        } catch (RemoteException exception) {
            onFailure(exception);
        }
    }

    @Override
    public void onSuccess(@NonNull byte[] result) throws RemoteException {
        mFuture.set(result);
        unlinkToDeath();
    }

    @Override
    public void onFailure(@NonNull String error) {
        onFailure(new RuntimeException(error));
    }

    private void onFailure(@NonNull Throwable throwable) {
        mFuture.setException(throwable);
        unlinkToDeath();
    }

    private void unlinkToDeath() {
        if (mBinder != null) {
            mBinder.unlinkToDeath(mRecipient, 0);
        }
    }

    /**
     * The {@link DeathRecipient} which helps track the lifetime of the {@link IBinder} that the
     * callback is associated to.
     */
    public static class DeathRecipient implements IBinder.DeathRecipient {
        private final RemoteCallback mCallback;

        public DeathRecipient(@NonNull RemoteCallback callback) {
            mCallback = callback;
        }

        @Override
        public void binderDied() {
            mCallback.onFailure("Binder died");
        }
    }
}
