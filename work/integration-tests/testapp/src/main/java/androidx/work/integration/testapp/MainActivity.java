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

package androidx.work.integration.testapp;

import static androidx.work.ExistingWorkPolicy.KEEP;
import static androidx.work.ExistingWorkPolicy.REPLACE;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.integration.testapp.imageprocessing.ImageProcessingActivity;
import androidx.work.integration.testapp.sherlockholmes.AnalyzeSherlockHolmesActivity;

import java.util.concurrent.TimeUnit;

/**
 * Main Activity
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String UNIQUE_WORK_NAME = "importantUniqueWork";
    private static final int NUM_WORKERS = 150;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.enqueue_infinite_work_charging).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WorkManager.getInstance().enqueue(
                                new OneTimeWorkRequest.Builder(InfiniteWorker.class)
                                        .setConstraints(new Constraints.Builder()
                                                .setRequiresCharging(true)
                                                .build())
                                        .build());
                    }
                });

        findViewById(R.id.enqueue_infinite_work_network).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WorkManager.getInstance().enqueue(
                                new OneTimeWorkRequest.Builder(InfiniteWorker.class)
                                        .setConstraints(new Constraints.Builder()
                                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                                .build())
                                        .build());
                    }
                });

        findViewById(R.id.sherlock_holmes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AnalyzeSherlockHolmesActivity.class));
            }
        });

        findViewById(R.id.image_processing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ImageProcessingActivity.class));
            }
        });

        findViewById(R.id.image_uri).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < 24) {
                    return;
                }

                WorkManager.getInstance().enqueue(ToastWorker
                        .create("Image URI Updated!")
                        .setConstraints(new Constraints.Builder()
                                .addContentUriTrigger(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                                .build())
                        .build()
                );
            }
        });

        final EditText delayInMs = findViewById(R.id.delay_in_ms);
        findViewById(R.id.schedule_delay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String delayString = delayInMs.getText().toString();
                long delay = Long.parseLong(delayString);
                Log.d(TAG, "Enqueuing job with delay of " + delay + " ms");
                WorkManager.getInstance().enqueue(ToastWorker
                        .create("Delayed Job Ran!")
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .build());
            }
        });

        findViewById(R.id.enqueue_periodic_work).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Data input = new Data.Builder()
                        .putString(ToastWorker.ARG_MESSAGE, "Periodic work")
                        .build();
                PeriodicWorkRequest request =
                        new PeriodicWorkRequest.Builder(ToastWorker.class, 15, TimeUnit.MINUTES)
                                .setInputData(input)
                                .build();
                WorkManager.getInstance().enqueue(request);
            }
        });

        findViewById(R.id.begin_unique_work_loop)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckBox keep = findViewById(R.id.keep);
                        ExistingWorkPolicy policy = keep.isChecked() ? KEEP : REPLACE;
                        for (int i = 0; i < 50; i += 1) {
                            WorkManager.getInstance()
                                    .beginUniqueWork(UNIQUE_WORK_NAME,
                                            policy,
                                            OneTimeWorkRequest.from(SleepWorker.class))
                                    .enqueue();
                        }
                    }
                });

        findViewById(R.id.enqueue_lots_of_work)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (int i = 0; i < NUM_WORKERS; i += 1) {
                            // Exceed Scheduler.MAX_SCHEDULER_LIMIT (100)
                            WorkManager.getInstance()
                                    .beginWith(OneTimeWorkRequest.from(SleepWorker.class))
                                    .enqueue();
                        }
                    }
                });
    }
}
