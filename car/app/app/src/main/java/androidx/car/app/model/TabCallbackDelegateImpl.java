/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.utils.RemoteUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Implementation class for {@link TabCallbackDelegate}.
 *
 */
@RestrictTo(LIBRARY)
@CarProtocol
@RequiresCarApi(6)
@KeepFields
public class TabCallbackDelegateImpl implements TabCallbackDelegate {
    private final @Nullable ITabCallback mStubCallback;
    @Override
    public void sendTabSelected(@NonNull String tabContentId, @NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mStubCallback).onTabSelected(tabContentId,
                    RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private TabCallbackDelegateImpl(TabTemplate.@NonNull TabCallback callback) {
        mStubCallback = new TabCallbackStub(callback);
    }

    /** For serialization. */
    private TabCallbackDelegateImpl() {
        mStubCallback = null;
    }

    // This listener relates to UI event and is expected to be triggered on the main thread.
    @SuppressLint("ExecutorRegistration")
    public static @NonNull TabCallbackDelegate create(TabTemplate.@NonNull TabCallback callback) {
        return new TabCallbackDelegateImpl(callback);
    }

    @CarProtocol
    @KeepFields // We need to keep these stub for Bundler serialization logic.
    private static class TabCallbackStub extends ITabCallback.Stub {
        private final TabTemplate.TabCallback mCallback;

        TabCallbackStub(TabTemplate.TabCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onTabSelected(String tabContentId, IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(
                    callback, "onTabSelected", () -> {
                        mCallback.onTabSelected(tabContentId);
                        return null;
                    }
            );
        }
    }
}
