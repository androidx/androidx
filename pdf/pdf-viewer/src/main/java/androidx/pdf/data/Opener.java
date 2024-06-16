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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.pdf.data.Openable.Open;
import androidx.pdf.util.ContentUriOpener;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.Uris;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Opens an {@link Openable} into a ready-to-use {@link Open} object.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Opener {

    private static final String TAG = Opener.class.getSimpleName();

    private final ContentUriOpener mContentOpener;

    public Opener(@NonNull Context ctx) {
        Context app = ctx.getApplicationContext();
        mContentOpener = new ContentUriOpener(app.getContentResolver());
    }

    @VisibleForTesting
    public Opener(@NonNull ContentUriOpener contentOpener) {
        this.mContentOpener = contentOpener;
    }

    @NonNull
    protected Open open(@NonNull ContentOpenable content) throws FileNotFoundException {
        String contentType = content.getContentType();
        AssetFileDescriptor afd;
        if (content.getSize() != null) {
            // Opens an image preview, not the actual contents.
            Point sizePoint = new Point(content.getSize().getWidth(),
                    content.getSize().getHeight());
            afd = mContentOpener.openPreview(content.getContentUri(), sizePoint);
        } else {
            afd = mContentOpener.open(content.getContentUri(), contentType);
        }
        if (afd == null) {
            throw new FileNotFoundException("Can't open " + content.getContentUri());
        }
        return new OpenContent(afd, contentType);
    }

    /** Opens the given local Uri and returns an {@link Open} object to read its data. */
    @NonNull
    public Open openLocal(@NonNull Uri localUri) throws IOException {
        Preconditions.checkNotNull(localUri);
        if (Uris.isContentUri(localUri)) {
            ContentOpenable content = new ContentOpenable(localUri);
            return open(content);
        } else if (Uris.isFileUri(localUri)) {
            FileOpenable file = new FileOpenable(localUri);
            return file.openWith(this);
        } else {
            throw new IllegalArgumentException("Uri in not local: " + localUri);
        }
    }

    /** Returns the Exif orientation rotation value for a content thumbnail. */
    public int getContentExifOrientation(@NonNull ContentOpenable contentOpenable) {
        return mContentOpener.getExifOrientation(contentOpenable.getContentUri());
    }

    /**
     *
     */
    @Nullable
    public String getContentType(@NonNull Uri uri) {
        if (Uris.isContentUri(uri)) {
            return mContentOpener.getContentType(uri);
        } else {
            return Uris.extractContentType(uri);
        }
    }

    /** An {@link Open} connection to data from a content provider. */
    private static class OpenContent implements Open {

        private final AssetFileDescriptor mAsset;
        private final String mContentType;

        OpenContent(AssetFileDescriptor asset, String type) {
            this.mAsset = asset;
            this.mContentType = type;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return mAsset.createInputStream();
        }

        @Override
        public ParcelFileDescriptor getFd() {
            return mAsset.getParcelFileDescriptor();
        }

        @Override
        public long length() {
            try {
                return mAsset.getLength();
            } catch (IllegalArgumentException iax) {
                // TODO: IllegalArgumentException in Opener#OpenContent
                return AssetFileDescriptor.UNKNOWN_LENGTH;
            }
        }

        @Override
        public String getContentType() {
            return mContentType;
        }
    }
}
