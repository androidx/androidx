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

package androidx.work.integration.testapp.imageprocessing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.integration.testapp.db.Image;
import androidx.work.integration.testapp.db.TestDatabase;

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
    public @NonNull Result doWork() {
        Log.d(TAG, "Started");

        String uriString = getInputData().getString(URI_KEY);
        if (TextUtils.isEmpty(uriString)) {
            Log.e(TAG, "Invalid URI!");
            return Result.FAILURE;
        }

        Bitmap image = retrieveImage(uriString);

        if (image == null) {
            Log.e(TAG, "Could not retrieve image!");
            return Result.FAILURE;
        }

        invertColors(image);
        String filePath = compressImage(image);

        if (TextUtils.isEmpty(filePath)) {
            Log.e(TAG, "Could not compress image!");
            return Result.FAILURE;
        }

        int processed = TestDatabase.getInstance(getApplicationContext())
                .getImageDao()
                .setProcessed(uriString, filePath);

        if (processed != 1) {
            Log.e(TAG, "Database was not updated!");
            return Result.FAILURE;
        }

        Log.d(TAG, "Image Processing Complete!");
        return Result.SUCCESS;
    }

    private Bitmap retrieveImage(String uriString) {
        Uri uri = Uri.parse(uriString);
        InputStream inputStream = null;
        try {
            inputStream = getApplicationContext().getContentResolver().openInputStream(uri);
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
                    getApplicationContext().getCacheDir());
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

    static OneTimeWorkRequest createWork(String uriString) {
        Data input = new Data.Builder().putString(URI_KEY, uriString).build();
        return new OneTimeWorkRequest.Builder(ImageProcessingWorker.class)
                .setInputData(input).build();
    }
}
