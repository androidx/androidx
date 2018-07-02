/*
 * Copyright 2017 The Android Open Source Project
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
package androidx.work.integration.testapp;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;

/**
 *  A {@link Worker} that shows a given Toast.
 */
public class ToastWorker extends Worker {
    static final String ARG_MESSAGE = "message";

    /**
     * Create a {@link OneTimeWorkRequest.Builder} with the given message.
     *
     * @param message The toast message to display
     * @return A {@link OneTimeWorkRequest.Builder}
     */
    public static OneTimeWorkRequest.Builder create(String message) {
        Data input = new Data.Builder().putString(ARG_MESSAGE, message).build();
        return new OneTimeWorkRequest.Builder(ToastWorker.class).setInputData(input);
    }

    @Override
    public @NonNull Result doWork() {
        Data input = getInputData();
        String message = input.getString(ARG_MESSAGE);
        if (message == null) {
            message = "completed!";
        }
        final String displayMessage = message;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d("ToastWorker", displayMessage);
                Toast.makeText(getApplicationContext(), displayMessage, Toast.LENGTH_SHORT).show();
            }
        });
        return Result.SUCCESS;
    }
}
