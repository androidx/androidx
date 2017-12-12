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

package android.arch.background.integration.testapp.imageprocessing;

import android.arch.background.integration.testapp.db.Image;
import android.arch.background.integration.testapp.db.TestDatabase;
import android.arch.background.workmanager.Arguments;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.Worker;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates a compressed transformed image from a given {@link Uri} and writes
 * the information as a {@link Image} entity
 */
public class ImageProcessingWorker extends Worker {
    private static final String URI_KEY = "uri";
    private static final String TAG = "ImageProcessingWorker";

    @Override
    public @WorkerResult int doWork() {
        Log.d(TAG, "Started");

        String uriString = getArguments().getString(URI_KEY, null);
        if (TextUtils.isEmpty(uriString)) {
            Log.e(TAG, "Invalid URI!");
            return WORKER_RESULT_FAILURE;
        }

        Bitmap image = retrieveImage(uriString);

        if (image == null) {
            Log.e(TAG, "Could not retrieve image!");
            return WORKER_RESULT_FAILURE;
        }

        invertColors(image);
        String filePath = compressImage(image);

        if (TextUtils.isEmpty(filePath)) {
            Log.e(TAG, "Could not compress image!");
            return WORKER_RESULT_FAILURE;
        }

        int processed = TestDatabase.getInstance(getAppContext())
                .getImageDao()
                .setProcessed(uriString, filePath);

        if (processed != 1) {
            Log.e(TAG, "Database was not updated!");
            return WORKER_RESULT_FAILURE;
        }

        Log.d(TAG, "Image Processing Complete!");
        return WORKER_RESULT_SUCCESS;
    }

    private Bitmap retrieveImage(String uriString) {
        Uri uri = Uri.parse(uriString);
        InputStream inputStream = null;
        try {
            inputStream = getAppContext().getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // Sample down to save memory
            options.inMutable = true; // Allow editing of bitmap
            return BitmapFactory.decodeStream(inputStream, null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void invertColors(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                int color = bitmap.getPixel(x, y);
                int reversedColor = (color & 0xff000000) | (0x00ffffff - (color & 0x00ffffff));
                bitmap.setPixel(x, y, reversedColor);
            }
        }
    }

    private String compressImage(Bitmap bitmap) {
        FileOutputStream os = null;
        try {
            File tempFile = File.createTempFile("compressed_", ".jpg",
                    getAppContext().getCacheDir());
            os = new FileOutputStream(tempFile);
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 25, os)) {
                return null;
            }
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static Work createWork(String uriString) {
        Arguments arguments = new Arguments.Builder().putString(URI_KEY, uriString).build();
        return Work.newBuilder(ImageProcessingWorker.class)
                .withArguments(arguments)
                .build();
    }
}
