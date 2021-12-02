/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.textandicons;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/** A simple file provider that returns files after a random delay. */
public class DelayedFileProvider extends FileProvider {
    private static final String FILE_PROVIDER_AUTHORITY = "com.showcase.fileprovider";
    private static final String RESOURCE_DIR = "res";
    private static final long MIN_DELAY_MILLIS = 1000;
    private static final long MAX_DELAY_MILLIS = 3000;

    /** Creates a file from the given resource id and returns the URI for it. */
    @NonNull
    public static Uri getUriForResource(@NonNull Context context,
            @NonNull String hostPackageName, int resId) {
        File resourceFile =
                new File(context.getFilesDir().getAbsolutePath(), RESOURCE_DIR + "/" + resId);
        if (!resourceFile.exists()) {
            resourceFile.getParentFile().mkdir();

            Bitmap bm = BitmapFactory.decodeResource(context.getResources(), resId);
            try (FileOutputStream fos = new FileOutputStream(resourceFile)) {
                bm.compress(CompressFormat.PNG, 10, fos);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Invalid resource " + resId);
            }
        }
        Uri uri = getUriForFile(context, FILE_PROVIDER_AUTHORITY, resourceFile);

        // FileProvider requires the app to grant temporary access to the car hosts for the file.
        // A URI from a content provider may not need to do this if its contents are public.
        context.grantUriPermission(hostPackageName, uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return uri;
    }

    @Override
    @NonNull
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        try {
            // Wait for a random period between the minimum and maximum delay.
            Thread.sleep(ThreadLocalRandom.current().nextLong(MIN_DELAY_MILLIS, MAX_DELAY_MILLIS));
        } catch (InterruptedException e) {
            throw new FileNotFoundException(e.getMessage());
        }

        return super.openFile(uri, mode);
    }
}
