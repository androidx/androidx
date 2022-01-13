/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.graphics;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.provider.FontsContractCompat;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for TypefaceCompat.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class TypefaceCompatUtil {
    private static final String TAG = "TypefaceCompatUtil";

    private TypefaceCompatUtil() {}  // Do not instantiate.

    private static final String CACHE_FILE_PREFIX = ".font";

    /**
     * Creates a temp file.
     *
     * Returns null if failed to create temp file.
     */
    @Nullable
    public static File getTempFile(@NonNull Context context) {
        File cacheDir = context.getCacheDir();
        if (cacheDir == null) {
            return null;
        }

        final String prefix = CACHE_FILE_PREFIX + Process.myPid() + "-" + Process.myTid() + "-";
        for (int i = 0; i < 100; ++i) {
            final File file = new File(cacheDir, prefix + i);
            try {
                if (file.createNewFile()) {
                    return file;
                }
            } catch (IOException e) {
                // ignore. Try next file.
            }
        }
        return null;
    }

    /**
     * Copy the file contents to the direct byte buffer.
     */
    @Nullable
    @RequiresApi(19)
    private static ByteBuffer mmap(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            FileChannel channel = fis.getChannel();
            final long size = channel.size();
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Copy the file contents to the direct byte buffer.
     */
    @Nullable
    @RequiresApi(19)
    public static ByteBuffer mmap(@NonNull Context context,
            @Nullable CancellationSignal cancellationSignal, @NonNull Uri uri) {
        final ContentResolver resolver = context.getContentResolver();
        try (ParcelFileDescriptor pfd = Api19Impl.openFileDescriptor(resolver, uri, "r",
                cancellationSignal)) {
            if (pfd == null) {
                return null;
            }
            try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor())) {
                FileChannel channel = fis.getChannel();
                final long size = channel.size();
                return channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Copy the resource contents to the direct byte buffer.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Nullable
    @RequiresApi(19)
    public static ByteBuffer copyToDirectBuffer(@NonNull Context context, @NonNull Resources res,
            int id) {
        File tmpFile = getTempFile(context);
        if (tmpFile == null) {
            return null;
        }
        try {
            if (!copyToFile(tmpFile, res, id)) {
                return null;
            }
            return mmap(tmpFile);
        } finally {
            tmpFile.delete();
        }
    }

    /**
     * Copy the input stream contents to file.
     */
    public static boolean copyToFile(@NonNull File file, @NonNull InputStream is) {
        FileOutputStream os = null;
        StrictMode.ThreadPolicy old = StrictMode.allowThreadDiskWrites();
        try {
            os = new FileOutputStream(file, false);
            byte[] buffer = new byte[1024];
            int readLen;
            while ((readLen = is.read(buffer)) != -1) {
                os.write(buffer, 0, readLen);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error copying resource contents to temp file: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(os);
            StrictMode.setThreadPolicy(old);
        }
    }

    /**
     * Copy the resource contents to file.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean copyToFile(@NonNull File file, @NonNull Resources res, int id) {
        InputStream is = null;
        try {
            is = res.openRawResource(id);
            return copyToFile(file, is);
        } finally {
            closeQuietly(is);
        }
    }

    /**
     * Attempts to close a Closeable, swallowing any resulting IOException.
     *
     * @param c the closeable to close
     */
    public static void closeQuietly(@Nullable Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // Quietly!
            }
        }
    }

    /**
     * A helper function to create a mapping from {@link Uri} to {@link ByteBuffer}.
     *
     * Skip if the file contents is not ready to be read.
     *
     * @param context A {@link Context} to be used for resolving content URI in
     *                {@link FontsContractCompat.FontInfo}.
     * @param fonts An array of {@link FontsContractCompat.FontInfo}.
     * @return A map from {@link Uri} to {@link ByteBuffer}.
     * @hide
     */
    @RestrictTo(LIBRARY)
    @NonNull
    @RequiresApi(19)
    public static Map<Uri, ByteBuffer> readFontInfoIntoByteBuffer(
            @NonNull Context context,
            @NonNull FontsContractCompat.FontInfo[] fonts,
            @Nullable CancellationSignal cancellationSignal
    ) {
        final HashMap<Uri, ByteBuffer> out = new HashMap<>();

        for (FontsContractCompat.FontInfo font : fonts) {
            if (font.getResultCode() != FontsContractCompat.Columns.RESULT_CODE_OK) {
                continue;
            }

            final Uri uri = font.getUri();
            if (out.containsKey(uri)) {
                continue;
            }

            ByteBuffer buffer = TypefaceCompatUtil.mmap(context, cancellationSignal, uri);
            out.put(uri, buffer);
        }
        return Collections.unmodifiableMap(out);
    }

    @RequiresApi(19)
    static class Api19Impl {
        private Api19Impl() {
            // This class is not instantiable.
        }

        @SuppressWarnings("SameParameterValue")
        @DoNotInline
        static ParcelFileDescriptor openFileDescriptor(ContentResolver contentResolver, Uri uri,
                String mode, CancellationSignal cancellationSignal) throws FileNotFoundException {
            return contentResolver.openFileDescriptor(uri, mode, cancellationSignal);
        }
    }
}
