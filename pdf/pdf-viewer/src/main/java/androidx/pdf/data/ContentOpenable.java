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

import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.Dimensions;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.Uris;

import java.io.IOException;

/**
 * An {@link Openable} on a 'content' asset, wrapping a {@link AssetFileDescriptor}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ContentOpenable implements Openable, Parcelable {

    private static final String TAG = ContentOpenable.class.getSimpleName();

    /** The content Uri this {@link Openable} opens. */
    private final Uri mContentUri;

    /**
     * The content-type that this content should be opened as (e.g. when more than one
     * available).
     */
    @Nullable
    private final String mContentType;

    /**
     * If not null, this {@link Openable} will open an image preview of the actual contents.
     * The preview will be requested with these specified dimensions.
     */
    @Nullable
    private final Dimensions mSize;

    @Nullable
    private Open mOpen;

    /** Creates an {@link Openable} for the contents @ uri with its default content-type. */
    public ContentOpenable(@NonNull Uri uri) {
        this(uri, null, null);
    }

    /** Creates an {@link Openable} for the contents @ uri with the given content-type. */
    public ContentOpenable(@NonNull Uri uri, @NonNull String contentType) {
        this(uri, contentType, null);
    }

    /**
     * Creates an {@link Openable} for an image preview (of the given size) of the contents @
     * uri.
     */
    public ContentOpenable(@NonNull Uri uri, @NonNull Dimensions size) {
        this(uri, null, size);
    }

    private ContentOpenable(@NonNull Uri uri, @Nullable String contentType,
            @Nullable Dimensions size) {
        Preconditions.checkNotNull(uri);
        Preconditions.checkArgument(Uris.isContentUri(uri),
                "Does not accept Uri " + uri.getScheme());
        this.mContentUri = uri;
        this.mContentType = contentType;
        this.mSize = size;
    }

    @NonNull
    public Uri getContentUri() {
        return mContentUri;
    }

    @Nullable
    public Dimensions getSize() {
        return mSize;
    }

    /**
     * Returns a new instance of {@link androidx.pdf.data.Openable.Open}.
     *
     * NOTE: Clients are responsible for closing each instance that they obtain from this method.
     *
     * @return The {@link androidx.pdf.data.Openable.Open} for this Openable.
     */
    @NonNull
    @Override
    public Open openWith(@NonNull Opener opener) throws IOException {
        /*
         * We want to explicitly return {@link Opener#open(ContentOpenable)} every time instead
         * of just
         * returning {@link #open} if it's not null, in case the underlying data is backed by a
         * pipe,
         * in which case we can't seek or re-read the resulting {@link android.os
         * .ParcelFileDescriptor},
         * so callers can call this again to get a fresh handle on the underlying data.
         */
        mOpen = opener.open(this);
        return mOpen;
    }

    @Override
    @Nullable
    public String getContentType() {
        return mContentType;
    }

    @Override
    public long length() {
        return mOpen != null ? mOpen.length() : -1;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s [%s]: %s / @%s", TAG, mContentType, mContentUri, mSize);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mContentUri, flags);
        if (mContentType != null) {
            dest.writeString(mContentType);
        } else {
            dest.writeString("");
        }
        if (mSize != null) {
            /* Value of 1 indicates that {@code size} is not null, to avoid un-parceling errors. */
            dest.writeInt(1);
            dest.writeParcelable(mSize, flags);
        } else {
            dest.writeInt(0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings("deprecation")
    public static final Creator<ContentOpenable> CREATOR = new Creator<ContentOpenable>() {
        @Override
        public ContentOpenable createFromParcel(Parcel parcel) {
            Uri uri = parcel.readParcelable(Uri.class.getClassLoader());
            String contentType = parcel.readString();
            if (contentType.isEmpty()) {
                contentType = null;
            }
            Dimensions size = null;
            boolean sizeIsPresent = parcel.readInt() > 0;
            if (sizeIsPresent) {
                size = parcel.readParcelable(Dimensions.class.getClassLoader());
            }
            return new ContentOpenable(uri, contentType, size);
        }

        @Override
        public ContentOpenable[] newArray(int size) {
            return new ContentOpenable[size];
        }
    };
}