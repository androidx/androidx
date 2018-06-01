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

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.integration.testapp.db.Image;
import androidx.work.integration.testapp.db.TestDatabase;

import java.io.File;
import java.util.List;

/**
 * Removes all existing {@link Image} entities and deletes associated compressed files
 */
public class ImageCleanupWorker extends Worker {
    private static final String TAG = "ImageProcessingWorker";

    @Override
    public @NonNull Result doWork() {
        Log.d(TAG, "Started");
        List<Image> images = TestDatabase.getInstance(getApplicationContext())
                .getImageDao().getImages();
        for (Image image : images) {
            if (!TextUtils.isEmpty(image.mProcessedFilePath)) {
                if (new File(image.mProcessedFilePath).delete()) {
                    Log.d(TAG, "Deleted : " + image.mProcessedFilePath);
                } else {
                    Log.e(TAG, "Failed to delete : " + image.mProcessedFilePath);
                }
            } else {
                Log.d(TAG, image.mOriginalAssetName + "was not processed");
            }
        }
        TestDatabase.getInstance(getApplicationContext()).getImageDao().clear();
        Log.d(TAG, "Cleanup Complete!");
        return Result.SUCCESS;
    }
}
