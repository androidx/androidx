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

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.integration.testapp.db.Image;
import androidx.work.integration.testapp.db.TestDatabase;

/**
 * Creates initial {@link Image} entity in db
 */
public class ImageSetupWorker extends Worker {
    private static final String TAG = "ImageSetupWorker";
    private static final String URI_KEY = "uri";

    @Override
    public @NonNull Result doWork() {
        Log.d(TAG, "Started");

        String uriString = getInputData().getString(URI_KEY);
        if (TextUtils.isEmpty(uriString)) {
            Log.e(TAG, "Invalid URI!");
            return Result.FAILURE;
        }

        Image image = new Image();
        image.mOriginalAssetName = uriString;
        image.mIsProcessed = false;
        TestDatabase.getInstance(getApplicationContext()).getImageDao().insert(image);
        return Result.SUCCESS;
    }

    static OneTimeWorkRequest createWork(String uriString) {
        Data input = new Data.Builder().putString(URI_KEY, uriString).build();
        return new OneTimeWorkRequest.Builder(ImageSetupWorker.class).setInputData(input).build();
    }
}
