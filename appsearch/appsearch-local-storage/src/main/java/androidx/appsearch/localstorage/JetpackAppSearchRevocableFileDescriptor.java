/*
 * Copyright 2024 The Android Open Source Project
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

// @exportToFramework:copyToPath(../../../cts/tests/appsearch/testutils/src/android/app/appsearch/testutil/external/JetpackAppSearchRevocableFileDescriptor.java)

package androidx.appsearch.localstorage;

import android.os.ParcelFileDescriptor;

import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.io.IOException;

/**
 * The local storage implementation of {@link AppSearchRevocableFileDescriptor}.
 *
 * <p> Since the {@link ParcelFileDescriptor} sent to the client side from the local storage
 * won't cross the binder, we could revoke the {@link ParcelFileDescriptor} in the client side
 * by directly close the one in AppSearch side. This class just adding close listener to the
 * inner {@link ParcelFileDescriptor}.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class JetpackAppSearchRevocableFileDescriptor extends ParcelFileDescriptor
        implements AppSearchRevocableFileDescriptor {
    private ParcelFileDescriptor.OnCloseListener mOnCloseListener;
    private int mMode;

    /**
     * Create a new ParcelFileDescriptor wrapped around another descriptor. By
     * default all method calls are delegated to the wrapped descriptor.
     */
    JetpackAppSearchRevocableFileDescriptor(@NonNull ParcelFileDescriptor parcelFileDescriptor,
            int mode) {
        super(parcelFileDescriptor);
        mMode = mode;
    }

    @Override
    public int getMode() {
        return mMode;
    }

    @Override
    @NonNull
    public ParcelFileDescriptor getRevocableFileDescriptor() {
        return this;
    }

    @Override
    public void setOnCloseListener(@NonNull OnCloseListener onCloseListener) {
        if (mOnCloseListener != null) {
            throw new IllegalStateException("The close listener has already been set.");
        }
        mOnCloseListener = Preconditions.checkNotNull(onCloseListener);
    }

    @Override
    public void revoke() throws IOException {
        // In jetpack, we already remove this revokeFd from cached map, we could directly close the
        // super ParcelFileDescriptor without invoke close listener.
        super.close();
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
            if (mOnCloseListener != null) {
                // Success closed and invoke call back without exception.
                mOnCloseListener.onClose(null);
            }
        } catch (IOException e) {
            if (mOnCloseListener != null) {
                mOnCloseListener.onClose(e);
            }
            throw e;
        }
    }
}
