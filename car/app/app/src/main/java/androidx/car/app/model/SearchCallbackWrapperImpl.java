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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.model.SearchTemplate.SearchCallback;

import android.annotation.SuppressLint;
import android.os.RemoteException;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.utils.RemoteUtils;

/**
 * Implementation class for {@link SearchCallbackWrapper}.
 *
 * @hide
 */
// TODO(b/177591476): remove after host references have been cleaned up.
@SuppressWarnings("deprecation")
@RestrictTo(LIBRARY)
public final class SearchCallbackWrapperImpl implements SearchCallbackWrapper {

    @Keep
    private final ISearchCallback mStubCallback;

    @Override
    public void onSearchTextChanged(@NonNull String searchText,
            @NonNull OnDoneCallback callback) {
        try {
            mStubCallback.onSearchTextChanged(searchText,
                    RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSearchSubmitted(@NonNull String searchText,
            @NonNull OnDoneCallback callback) {
        try {
            mStubCallback.onSearchSubmitted(searchText,
                    RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private SearchCallbackWrapperImpl(@NonNull SearchCallback callback) {
        mStubCallback = new SearchCallbackStub(callback);
    }

    /** For serialization. */
    private SearchCallbackWrapperImpl() {
        mStubCallback = null;
    }

    @NonNull
    // This listener relates to UI event and is expected to be triggered on the main thread.
    @SuppressLint("ExecutorRegistration")
    static SearchCallbackWrapper create(@NonNull SearchCallback callback) {
        return new SearchCallbackWrapperImpl(callback);
    }

    @Keep // We need to keep these stub for Bundler serialization logic.
    private static class SearchCallbackStub extends ISearchCallback.Stub {
        private final SearchCallback mCallback;

        SearchCallbackStub(SearchCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onSearchTextChanged(String text, IOnDoneCallback callback) {
            RemoteUtils.dispatchHostCall(
                    () -> mCallback.onSearchTextChanged(text), callback,
                    "onSearchTextChanged");
        }

        @Override
        public void onSearchSubmitted(String text, IOnDoneCallback callback) {
            RemoteUtils.dispatchHostCall(
                    () -> mCallback.onSearchSubmitted(text), callback, "onSearchSubmitted");
        }
    }
}
