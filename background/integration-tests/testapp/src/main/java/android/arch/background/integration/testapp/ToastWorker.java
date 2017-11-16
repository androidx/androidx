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
package android.arch.background.integration.testapp;

import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.model.Arguments;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 *  A {@link Worker} that shows a given Toast.
 */
public class ToastWorker extends Worker {
    private static final String ARG_MESSAGE = "message";

    /**
     * Create a {@link Work.Builder} with the given message.
     *
     * @param message The toast message to display
     * @return A {@link Work.Builder}
     */
    public static Work.Builder create(String message) {
        Arguments args = new Arguments.Builder().putString(ARG_MESSAGE, message).build();
        return new Work.Builder(ToastWorker.class).withArguments(args);
    }

    @Override
    public @WorkerResult int doWork() {
        Arguments args = getArguments();
        final String message = args.getString(ARG_MESSAGE, "completed!");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d("ToastWorker", message);
                Toast.makeText(getAppContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
        return WORKER_RESULT_SUCCESS;
    }
}
