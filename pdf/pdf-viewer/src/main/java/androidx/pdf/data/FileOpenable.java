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
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.Uris;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A file's data that is saved on disk (e.g. in cache).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FileOpenable implements Openable, Parcelable {

    private static final String TAG = FileOpenable.class.getSimpleName();

    /**
     * Turns this {@link Uri} into a {@link File} if possible
     *
     * @throws IllegalArgumentException If the Uri was not a 'file:' one.
     */
    private static File getFile(Uri fileUri) {
        Preconditions.checkArgument(Uris.isFileUri(fileUri),
                "FileOpenable only valid for file Uris");
        return new File(fileUri.getPath());
    }

    @Nullable
    private final String mContentType;

    private final File mFile;

    /**
     * Constructs an {@link Openable} from a file and a given content-type.
     *
     * @throws FileNotFoundException If the file does not exist.
     */
    public FileOpenable(@NonNull File file, @Nullable String mimeType)
            throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("file does not exist");
        }
        this.mFile = Preconditions.checkNotNull(file);
        this.mContentType = mimeType;
    }

    /**
     * Constructs an {@link Openable} from a file Uri.
     *
     * @throws IllegalArgumentException If the Uri was not a 'file:' one.
     * @throws FileNotFoundException    If the file does not exist.
     */
    public FileOpenable(@NonNull Uri uri) throws FileNotFoundException {
        this(getFile(uri), Uris.extractContentType(uri));
    }

    @NonNull
    @Override
    public Open openWith(@NonNull Opener opener) throws IOException {
        return new Open() {

            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(mFile);
            }

            @Override
            public ParcelFileDescriptor getFd() throws IOException {
                return ParcelFileDescriptor.open(mFile, ParcelFileDescriptor.MODE_READ_ONLY);
            }

            @Override
            public long length() {
                return mFile.length();
            }

            @Override
            public String getContentType() {
                return mContentType;
            }
        };
    }

    @Override
    @Nullable
    public String getContentType() {
        return mContentType;
    }

    @Override
    public long length() {
        return mFile.length();
    }

    @NonNull
    public String getFileName() {
        return mFile.getName();
    }

    @NonNull
    public Uri getFileUri() {
        return Uri.fromFile(mFile);
    }


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mFile.getPath());
        dest.writeString(mContentType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FileOpenable> CREATOR =
            new Creator<FileOpenable>() {
                @Nullable
                @Override
                public FileOpenable createFromParcel(Parcel parcel) {
                    try {
                        return new FileOpenable(makeFile(parcel.readString()), parcel.readString());
                    } catch (FileNotFoundException e) {
                        return null;
                    }
                }

                private File makeFile(String filePath) {
                    return new File(filePath);
                }

                @Override
                public FileOpenable[] newArray(int size) {
                    return new FileOpenable[size];
                }
            };
}
