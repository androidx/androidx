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

package androidx.work.integration.testapp;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Result;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An activity to test retries.
 */
public class RetryActivity extends AppCompatActivity {

    private static final String TAG = "RetryActivity";

    private Button mButton;
    private TextView mTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.retry_activity);
        mButton = findViewById(R.id.btn);
        mTextView = findViewById(R.id.textview);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scheduleWork("one", 3, 0.2);
                scheduleWork("two", 5, 0.5);
            }
        });

        WorkManager.getInstance().getWorkInfosByTagLiveData("test")
                .observe(this, new Observer<List<WorkInfo>>() {
                    @Override
                    public void onChanged(@Nullable List<WorkInfo> workInfos) {
                        String text = "";
                        for (WorkInfo workInfo : workInfos) {
                            text = text + "id: " + workInfo.getId().toString().substring(0, 4)
                                    + " (" + workInfo.getState() + ")\n";
                        }

                        if (text.equals("")) {
                            mTextView.setText("nothing to show");
                        } else {
                            mTextView.setText(text);
                        }
                    }
                });
    }

    private void scheduleWork(String name, int timeTaken, double errorRate) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(Worker.class)
                .setConstraints(constraints)
                .setInputData(
                        new Data.Builder()
                                .putString(Worker.NAME, name)
                                .putInt(Worker.TIME_TAKEN, timeTaken)
                                .putDouble(Worker.ERROR_RATE, errorRate)
                                .build()
                )
                .addTag("test")
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance()
                .beginUniqueWork(name, ExistingWorkPolicy.KEEP, workRequest)
                .enqueue();
    }

    /**
     * A Worker to test retries.
     */
    public static class Worker extends androidx.work.Worker {
        public static final String NAME = "name";
        public static final String TIME_TAKEN = "time_taken";
        public static final String ERROR_RATE = "error_rate";

        public Worker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {

            String name = getInputData().getString(NAME);
            int timeTaken = getInputData().getInt(TIME_TAKEN, 3);
            double errorRate = getInputData().getDouble(ERROR_RATE, 0.5);

            try {
                Log.i(TAG,
                        String.format("[%s] %s started (run attempt = %d)",
                                name,
                                getId(),
                                getRunAttemptCount()));
                for (int i = 0; i < timeTaken; i++) {
                    Thread.sleep(1000L);
                    Log.v(TAG, String.format("[%s] %s completed stage = %d", name, getId(), i));
                }
                if (Math.random() < errorRate) {
                    throw new RuntimeException("random failure");
                }
                Log.i(TAG, String.format("[%s] %s successful", name, getId()));
                return Result.success();
            } catch (Exception e) {
                Log.e(TAG, String.format("[%s] %s failed: %s", name, getId(), e.getMessage()));
                return Result.retry();
            }
        }
    }
}
