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

import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.Openable.Open;
import androidx.pdf.util.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * File data that can be displayed in a Viewer. This class contains meta-data specific to Projector
 * (e.g. display type), and an {@link Openable} that can be used to access the data.
 * Instances are parcelable (in the form of a {@link Bundle}).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DisplayData {

    private static final String TAG = DisplayData.class.getSimpleName();

    private static final String KEY_NAME = "n";
    private static final String KEY_URI = "uri";
    private static final String KEY_PARCELABLE_OPENABLE = "po";

    /**
     * This is used for identifying this data, and as the base Uri in the case of HTML. In order to
     * actually access the data, {@link #mOpenable} should be used instead.
     */
    private final Uri mUri;

    private final String mName;

    private final Openable mOpenable;

    public DisplayData(
            @NonNull Uri uri,
            @NonNull String name,
            @NonNull Openable openable) {
        this.mName = Preconditions.checkNotNull(name);
        this.mUri = Preconditions.checkNotNull(uri);
        this.mOpenable = Preconditions.checkNotNull(openable);
    }

    @NonNull
    public Uri getUri() {
        return mUri;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull
    public Openable getOpenable() {
        return mOpenable;
    }

    /** Converts an Opener into ParcelFileDescriptor */
    @Nullable
    public ParcelFileDescriptor openFd(@NonNull Opener opener) {
        // TODO: StrictMode: close() not explicitly called on PFD.
        try {
            return open(opener).getFd();
        } catch (IOException e) {
            return null;
        }
    }

    /** Converts Opener to InputStream */
    @Nullable
    public InputStream openInputStream(@NonNull Opener opener) throws IOException {
        return open(opener).getInputStream();
    }

    /**
     *
     */
    public long length() {
        return mOpenable.length();
    }

    /**
     *
     */
    @NonNull
    private Open open(Opener opener) throws IOException {
        return mOpenable.openWith(opener);
    }

    /**
     *
     */
    @NonNull
    public Bundle asBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_NAME, mName);
        bundle.putParcelable(KEY_URI, mUri);
        bundle.putParcelable(KEY_PARCELABLE_OPENABLE, mOpenable);

        return bundle;
    }

    /**
     *
     */
    @NonNull
    @SuppressWarnings("deprecation")
    public static DisplayData fromBundle(@NonNull Bundle bundle) {
        bundle.setClassLoader(DisplayData.class.getClassLoader());
        Uri uri = bundle.getParcelable(KEY_URI);
        String name = bundle.getString(KEY_NAME);
        Openable openable = bundle.getParcelable(KEY_PARCELABLE_OPENABLE);

        return new DisplayData(uri, name, openable);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                "Display Data [%s] +%s, uri: %s",
                mName, mOpenable.getClass().getSimpleName(), mUri);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof DisplayData)) {
            return false;
        }

        DisplayData other = (DisplayData) obj;
        return mUri.equals(other.mUri)
                && mName.equals(other.mName)
                && mOpenable.equals(other.mOpenable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mUri, mName, mOpenable);
    }
}
