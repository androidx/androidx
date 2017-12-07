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
import android.text.TextUtils;
import android.util.Log;

/**
 * Creates initial {@link Image} entity in db
 */
public class ImageSetupWorker extends Worker {
    private static final String TAG = "ImageSetupWorker";
    private static final String URI_KEY = "uri";

    @Override
    public @WorkerResult int doWork() {
        Log.d(TAG, "Started");

        String uriString = getArguments().getString(URI_KEY, null);
        if (TextUtils.isEmpty(uriString)) {
            Log.e(TAG, "Invalid URI!");
            return WORKER_RESULT_FAILURE;
        }

        Image image = new Image();
        image.mOriginalAssetName = uriString;
        image.mIsProcessed = false;
        TestDatabase.getInstance(getAppContext()).getImageDao().insert(image);
        return WORKER_RESULT_SUCCESS;
    }

    static Work createWork(String uriString) {
        Arguments arguments = new Arguments.Builder().putString(URI_KEY, uriString).build();
        return new Work.Builder(ImageSetupWorker.class)
                .withArguments(arguments)
                .build();
    }
}
