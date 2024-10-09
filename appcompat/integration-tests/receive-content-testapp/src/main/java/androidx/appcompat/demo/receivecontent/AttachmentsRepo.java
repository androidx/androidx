/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appcompat.demo.receivecontent;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Stores attachments as files in the app's private storage directory (see
 * {@link Context#getDataDir()}, {@link Context#getFilesDir()}, etc).
 */
final class AttachmentsRepo {

    // This matches the name declared in AndroidManifest.xml
    private static final String FILE_PROVIDER_AUTHORITY =
            "androidx.appcompat.demo.receivecontent.fileprovider";

    private final Context mContext;
    private final File mAttachmentsDir;

    AttachmentsRepo(@NonNull Context context) {
        mContext = context;
        mAttachmentsDir = new File(mContext.getFilesDir(), "attachments");
    }

    /**
     * Reads the content at the given URI and writes it to private storage. Then returns a content
     * URI referencing the newly written file.
     */
    public @NonNull Uri write(@NonNull Uri uri) {
        ContentResolver contentResolver = mContext.getContentResolver();
        String mimeType = contentResolver.getType(uri);
        String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        try (InputStream is = contentResolver.openInputStream(uri)) {
            if (is == null) {
                throw new IllegalArgumentException(String.valueOf(uri));
            }
            mAttachmentsDir.mkdirs();
            String fileName = "a-" + UUID.randomUUID().toString() + "." + ext;
            File newAttachment = new File(mAttachmentsDir, fileName);
            try (OutputStream os = new FileOutputStream(newAttachment);) {
                ByteStreams.copy(is, os);
            }
            Uri resultUri = getUriForFile(newAttachment);
            Log.i(Logcat.TAG, "Saved content: originalUri=" + uri + ", resultUri=" + resultUri);
            return resultUri;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    public void deleteAll() {
        File[] files = mAttachmentsDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            file.delete();
        }
    }

    public @NonNull ImmutableList<Uri> getAllUris() {
        File[] files = mAttachmentsDir.listFiles();
        if (files == null || files.length == 0) {
            return ImmutableList.of();
        }
        ImmutableList.Builder<Uri> uris = ImmutableList.builderWithExpectedSize(files.length);
        for (File file : files) {
            uris.add(getUriForFile(file));
        }
        return uris.build();
    }

    private @NonNull Uri getUriForFile(@NonNull File file) {
        return FileProvider.getUriForFile(mContext, FILE_PROVIDER_AUTHORITY, file);
    }
}
