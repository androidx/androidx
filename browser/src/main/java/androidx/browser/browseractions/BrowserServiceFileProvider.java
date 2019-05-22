/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.browser.browseractions;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.content.FileProvider;
import androidx.core.util.AtomicFile;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The class to pass images asynchronously between different Browser Services provider and Browser
 * client.
 *
 * Call {@link #saveBitmap} to save the image and {@link #loadBitmap} to read it.
 * @hide
 */
@RestrictTo(LIBRARY)
public final class BrowserServiceFileProvider extends FileProvider {
    private static final String TAG = "BrowserServiceFP";
    private static final String AUTHORITY_SUFFIX = ".image_provider";
    private static final String CONTENT_SCHEME = "content";
    private static final String FILE_SUB_DIR = "image_provider";
    private static final String FILE_SUB_DIR_NAME = "image_provider_images/";
    private static final String FILE_EXTENSION = ".png";
    private static final String CLIP_DATA_LABEL = "image_provider_uris";
    private static final String LAST_CLEANUP_TIME_KEY = "last_cleanup_time";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static Object sFileCleanupLock = new Object();

    private static class FileCleanupTask extends AsyncTask<Void, Void, Void> {
        private final Context mAppContext;
        private static final long IMAGE_RETENTION_DURATION = TimeUnit.DAYS.toMillis(7);
        private static final long CLEANUP_REQUIRED_TIME_SPAN = TimeUnit.DAYS.toMillis(7);
        private static final long DELETION_FAILED_REATTEMPT_DURATION = TimeUnit.DAYS.toMillis(1);

        FileCleanupTask(Context context) {
            super();
            mAppContext = context.getApplicationContext();
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(
                    mAppContext.getPackageName() + AUTHORITY_SUFFIX, Context.MODE_PRIVATE);
            if (!shouldCleanUp(prefs)) return null;
            synchronized (sFileCleanupLock) {
                boolean allFilesDeletedSuccessfully = true;
                File path = new File(mAppContext.getFilesDir(), FILE_SUB_DIR);
                if (!path.exists()) return null;
                File[] files = path.listFiles();
                long retentionDate = System.currentTimeMillis() - IMAGE_RETENTION_DURATION;
                for (File file : files) {
                    if (!isImageFile(file)) continue;
                    long lastModified = file.lastModified();
                    if (lastModified < retentionDate && !file.delete()) {
                        Log.e(TAG, "Fail to delete image: " + file.getAbsoluteFile());
                        allFilesDeletedSuccessfully = false;
                    }
                }
                // If fail to delete some files, kill off clean up task after one day.
                long lastCleanUpTime;
                if (allFilesDeletedSuccessfully) {
                    lastCleanUpTime = System.currentTimeMillis();
                } else {
                    lastCleanUpTime = System.currentTimeMillis() - CLEANUP_REQUIRED_TIME_SPAN
                            + DELETION_FAILED_REATTEMPT_DURATION;
                }
                Editor editor = prefs.edit();
                editor.putLong(LAST_CLEANUP_TIME_KEY, lastCleanUpTime);
                editor.apply();
            }
            return null;
        }

        private static boolean isImageFile(File file) {
            String filename = file.getName();
            return filename.endsWith("." + FILE_EXTENSION);
        }

        private static boolean shouldCleanUp(SharedPreferences prefs) {
            long lastCleanup = prefs.getLong(LAST_CLEANUP_TIME_KEY, System.currentTimeMillis());
            return System.currentTimeMillis() > lastCleanup + CLEANUP_REQUIRED_TIME_SPAN;
        }
    }

    private static class FileSaveTask extends AsyncTask<String, Void, Void> {
        private final Context mAppContext;
        private final String mFilename;
        private final Bitmap mBitmap;
        private final Uri mFileUri;
        private final ResolvableFuture<Uri> mResultFuture;

        FileSaveTask(Context context, String filename, Bitmap bitmap, Uri fileUri,
                ResolvableFuture<Uri> resultFuture) {
            super();
            mAppContext = context.getApplicationContext();
            mFilename = filename;
            mBitmap = bitmap;
            mFileUri = fileUri;
            mResultFuture = resultFuture;
        }

        @Override
        protected Void doInBackground(String... params) {
            saveFileIfNeededBlocking();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            new FileCleanupTask(mAppContext).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }

        private void saveFileIfNeededBlocking() {
            File path = new File(mAppContext.getFilesDir(), FILE_SUB_DIR);
            synchronized (sFileCleanupLock) {
                if (!path.exists() && !path.mkdir()) {
                    mResultFuture.setException(new IOException("Could not create file directory."));
                    return;
                }
                File img = new File(path, mFilename + FILE_EXTENSION);

                if (img.exists()) {
                    mResultFuture.set(mFileUri);
                } else {
                    saveFileBlocking(img);
                }

                img.setLastModified(System.currentTimeMillis());
            }
        }

        private void saveFileBlocking(File img) {
            FileOutputStream fOut = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                AtomicFile atomicFile = new AtomicFile(img);
                try {
                    fOut = atomicFile.startWrite();
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.close();
                    atomicFile.finishWrite(fOut);

                    mResultFuture.set(mFileUri);
                } catch (IOException e) {
                    atomicFile.failWrite(fOut);

                    mResultFuture.setException(e);
                }
            } else {
                try {
                    fOut = new FileOutputStream(img);
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.close();

                    mResultFuture.set(mFileUri);
                } catch (IOException e) {
                    mResultFuture.setException(e);
                }
            }
        }
    }

    /**
     * Request a {@link Uri} used to access the bitmap through the file provider.
     * @param context The {@link Context} used to generate the uri, save the bitmap and grant the
     *                read permission.
     * @param bitmap The {@link Bitmap} to be saved and access through the file provider.
     * @param name The name of the bitmap.
     * @param version The version number of the bitmap. Note: This plus the name decides the
     *                filename of the bitmap. If it matches with existing file, bitmap will skip
     *                saving.
     * @return A {@link ResolvableFuture} that will be fulfilled with the uri of the bitmap once
     *         file writing has completed or an IOException describing the reason for failure.
     */
    @UiThread
    @NonNull
    public static ResolvableFuture<Uri> saveBitmap(@NonNull Context context, @NonNull Bitmap bitmap,
            @NonNull String name, int version) {
        String filename = name + "_" + Integer.toString(version);
        Uri uri = generateUri(context, filename);

        ResolvableFuture<Uri> result = ResolvableFuture.create();
        new FileSaveTask(context, filename, bitmap, uri, result)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return result;
    }

    private static Uri generateUri(Context context, String filename) {
        String fileName = FILE_SUB_DIR_NAME + filename + FILE_EXTENSION;
        return new Uri.Builder()
                .scheme(CONTENT_SCHEME)
                .authority(context.getPackageName() + AUTHORITY_SUFFIX)
                .path(fileName)
                .build();
    }

    /**
     * Grant the read permission to a list of {@link Uri} sent through a {@link Intent}.
     * @param intent The sending Intent which holds a list of Uri.
     * @param uris A list of Uri generated by saveBitmap(Context, Bitmap, String, int,
     *             List<String>), if null, nothing will be done.
     * @param context The context requests to grant the permission.
     */
    public static void grantReadPermission(@NonNull Intent intent, @Nullable List<Uri> uris,
            @NonNull Context context) {
        if (uris == null || uris.size() == 0) return;
        ContentResolver resolver = context.getContentResolver();
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clipData = ClipData.newUri(resolver, CLIP_DATA_LABEL, uris.get(0));
        for (int i = 1; i < uris.size(); i++) {
            clipData.addItem(new ClipData.Item(uris.get(i)));
        }
        intent.setClipData(clipData);
    }

    /**
     * Asynchronously loads a {@link Bitmap} from the uri generated by {@link #saveBitmap}.
     * @param resolver {@link ContentResolver} to access the Bitmap.
     * @param uri {@link Uri} pointing to the Bitmap.
     * @return A {@link ListenableFuture} that will be fulfilled with the Bitmap once the load has
     *         completed or with an IOException describing the reason for failure.
     */
    @NonNull
    public static ListenableFuture<Bitmap> loadBitmap(@NonNull final ContentResolver resolver,
            @NonNull final Uri uri) {
        final ResolvableFuture<Bitmap> result = ResolvableFuture.create();

        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ParcelFileDescriptor descriptor = resolver.openFileDescriptor(uri, "r");

                    if (descriptor == null) {
                        result.setException(new FileNotFoundException());
                        return;
                    }

                    FileDescriptor fileDescriptor = descriptor.getFileDescriptor();
                    Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    descriptor.close();

                    if (bitmap == null) {
                        result.setException(new IOException("File could not be decoded."));
                        return;
                    }

                    result.set(bitmap);
                } catch (IOException e) {
                    result.setException(e);
                }
            }
        });

        return result;
    }
}
