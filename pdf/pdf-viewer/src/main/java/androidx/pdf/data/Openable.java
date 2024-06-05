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

package androidx.pdf.data;

import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.IOException;
import java.io.InputStream;

/**
 * A portable (i.e. {@link Parcelable}) handle to some data that can be opened with an
 * {@link Opener}. In addition to an Uri, this also includes additional parameters to fully resolve
 * requests to obtain the contents, e.g. a choice of one single content type.
 * <br>
 * {@link Open} instances may create {@link ParcelFileDescriptor}/{@link InputStream}
 * instances lazily or eagerly. Create a new {@code Open} each time a new instance of either is
 * required.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface Openable extends Parcelable {

    /** Open this data and return an object that allows reading it. */
    @NonNull
    Open openWith(@NonNull Opener opener) throws IOException;

    /** Returns the length of the data in bytes (pre-connection, so might be an estimate). */
    long length();

    /** Returns the MIME type of the data (pre-connection, so might not be available). */
    @Nullable
    String getContentType();

    /** An object that represents an open connection to obtain the data, and gives ways to read
     * it. */
    interface Open {

        /**
         * Gives an {@link InputStream} to read the data.
         * <br>
         * Callers take ownership of the returned InputStream and are responsible for closing it.
         */
        @NonNull
        InputStream getInputStream() throws IOException;

        /**
         * Returns a file descriptor on this data, if available (e.g. doesn't work for http).
         * <br>
         * Callers take ownership of the returned ParcelFileDescriptor and are responsible for
         * closing
         * it.
         */
        @NonNull
        ParcelFileDescriptor getFd() throws IOException;

        /** Returns the declared length of the data. */
        long length();

        /** Returns the declared content-type of the data. */
        @NonNull
        String getContentType();
    }
}
