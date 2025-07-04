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

package androidx.appsearch.localstorage;

import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.OnCloseListener;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.io.IOException;

/**
 * A custom {@link ParcelFileDescriptor} that provides an additional mechanism to register
 * a {@link ParcelFileDescriptor.OnCloseListener} which will be invoked when the file
 * descriptor is closed.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppSearchRevocableFileDescriptor {

    /**
     * Gets the mode of this {@link AppSearchRevocableFileDescriptor}, It should be
     * {@link ParcelFileDescriptor#MODE_READ_ONLY} or {@link ParcelFileDescriptor#MODE_READ_WRITE}.
     */
    int getMode();

    /**
     * Gets the revocable {@link ParcelFileDescriptor} that could be sent to an untrusted caller.
     *
     * <p> AppSearch will retain control of this {@link ParcelFileDescriptor}'s access to the file.
     *
     * <p> Call {@link #revoke()} to invoke the sent {@link ParcelFileDescriptor}.
     */
    @NonNull
    ParcelFileDescriptor getRevocableFileDescriptor();

    /**
     * Revoke the sent {@link ParcelFileDescriptor} returned by
     * {@link #getRevocableFileDescriptor()}.
     *
     * <p>After calling this method, any access to the file descriptors will fail.
     *
     * @throws IOException If an I/O error occurs while revoking file descriptors.
     */
    void revoke() throws IOException;

    /**
     * Callback for indicating that the {@link ParcelFileDescriptor} returned by
     * {@link #getRevocableFileDescriptor()} has been closed.
     */
    void setOnCloseListener(@NonNull OnCloseListener onCloseListener);
}
