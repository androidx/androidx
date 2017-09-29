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
package android.arch.background.integration.testapp;

import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.model.Arguments;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 *  A {@link Worker} that sleeps for a given amount of time and then shows a given Toast.
 */
public class SleepyToastWorker extends Worker {

    private static final String ARG_TIMER = "timer";
    private static final String ARG_MESSAGE = "message";

    /**
     * Create a {@link Work.Builder} with the given arguments.
     *
     * @param sleepTimer How long to sleep in the {@link Worker}
     * @param message The toast message to display
     * @return A {@link Work.Builder}
     */
    public static Work.Builder createWithArgs(long sleepTimer, String message) {
        Arguments args = new Arguments();
        args.putLong(ARG_TIMER, sleepTimer);
        args.putString(ARG_MESSAGE, message);

        return new Work.Builder(SleepyToastWorker.class).withArguments(args);
    }

    @Override
    public void doWork() {
        Arguments args = getArguments();
        long sleepTimer = args.getLong(ARG_TIMER, 1000L);
        final String message = args.getString(ARG_MESSAGE, "completed!");

        try {
            Thread.sleep(sleepTimer);
        } catch (InterruptedException e) {
            // Do nothing.
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getAppContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
