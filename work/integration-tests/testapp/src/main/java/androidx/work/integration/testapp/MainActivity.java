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
import static androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME;
import static androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME;

import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.impl.background.systemjob.SystemJobService;
import androidx.work.impl.workers.ConstraintTrackingWorker;
import androidx.work.integration.testapp.imageprocessing.ImageProcessingActivity;
import androidx.work.integration.testapp.sherlockholmes.AnalyzeSherlockHolmesActivity;
import androidx.work.multiprocess.RemoteWorkerService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main Activity
 */
public class MainActivity extends AppCompatActivity {

    private static final String PACKAGE_NAME = "androidx.work.integration.testapp";
    private static final String TAG = "MainActivity";
    private static final String CONSTRAINT_TRACKING_TAG = "ConstraintTrackingWorker";
    private static final String UNIQUE_WORK_NAME = "importantUniqueWork";
    private static final String REPLACE_COMPLETED_WORK = "replaceCompletedWork";
    private static final int NUM_WORKERS = 150;

    // Synthetic access
    WorkRequest mLastForegroundWorkRequest;
    int mLastNotificationId = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.enqueue_infinite_work_charging).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WorkManager.getInstance(MainActivity.this).enqueue(
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
                        WorkManager.getInstance(MainActivity.this).enqueue(
                                new OneTimeWorkRequest.Builder(InfiniteWorker.class)
                                        .setConstraints(new Constraints.Builder()
                                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                                .build())
                                        .build());
                    }
                });


        findViewById(R.id.enqueue_battery_not_low).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WorkManager.getInstance(MainActivity.this).enqueue(
                                new OneTimeWorkRequest.Builder(TestWorker.class)
                                        .setConstraints(new Constraints.Builder()
                                                .setRequiresBatteryNotLow(true)
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

                WorkManager.getInstance(MainActivity.this).enqueue(ToastWorker
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
                WorkManager.getInstance(MainActivity.this).enqueue(ToastWorker
                        .create("Delayed Job Ran!")
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .build());
            }
        });

        findViewById(R.id.coroutine_sleep).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String delayString = delayInMs.getText().toString();
                long delay = Long.parseLong(delayString);
                Log.d(TAG, "Enqueuing job with delay of " + delay + " ms");

                Data inputData = new Data.Builder()
                        .put("sleep_time", delay)
                        .build();
                WorkManager.getInstance(MainActivity.this).enqueue(
                        new OneTimeWorkRequest.Builder(CoroutineSleepWorker.class)
                                .setInputData(inputData)
                                .addTag("coroutine_sleep")
                                .build());
            }
        });

        findViewById(R.id.coroutine_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorkManager.getInstance(MainActivity.this).cancelAllWorkByTag("coroutine_sleep");
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
                WorkManager.getInstance(MainActivity.this).enqueue(request);
            }
        });

        findViewById(R.id.enqueue_periodic_work_flex).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Data input = new Data.Builder()
                                .putString(ToastWorker.ARG_MESSAGE, "Periodic work with Flex")
                                .build();
                        PeriodicWorkRequest request =
                                new PeriodicWorkRequest.Builder(ToastWorker.class, 15,
                                        TimeUnit.MINUTES, 10,
                                        TimeUnit.MINUTES)
                                        .setInputData(input)
                                        .build();
                        WorkManager.getInstance(MainActivity.this).enqueue(request);
                    }
                });

        findViewById(R.id.enqueue_periodic_initial_delay).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Data input = new Data.Builder()
                                .putString(ToastWorker.ARG_MESSAGE, "Periodic work")
                                .build();
                        PeriodicWorkRequest request =
                                new PeriodicWorkRequest.Builder(ToastWorker.class, 15,
                                        TimeUnit.MINUTES)
                                        .setInitialDelay(1, TimeUnit.MINUTES)
                                        .setInputData(input)
                                        .build();
                        WorkManager.getInstance(MainActivity.this).enqueue(request);
                    }
                });

        findViewById(R.id.begin_unique_work_loop)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckBox keep = findViewById(R.id.keep);
                        ExistingWorkPolicy policy = keep.isChecked() ? KEEP : REPLACE;
                        for (int i = 0; i < 50; i += 1) {
                            WorkManager.getInstance(MainActivity.this)
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
                            WorkManager.getInstance(MainActivity.this)
                                    .beginWith(OneTimeWorkRequest.from(SleepWorker.class))
                                    .enqueue();
                        }
                    }
                });

        findViewById(R.id.exploding_work).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WorkManager wm = WorkManager.getInstance(MainActivity.this);
                List<WorkContinuation> leaves = new ArrayList<>();
                for (int i = 0; i < 10; ++i) {
                    OneTimeWorkRequest workRequest = createTestWorker();
                    WorkContinuation continuation = wm.beginWith(workRequest);
                    for (int j = 0; j < 10; ++j) {
                        OneTimeWorkRequest primaryDependent = createTestWorker();
                        WorkContinuation primaryContinuation = continuation.then(primaryDependent);
                        for (int k = 0; k < 10; ++k) {
                            OneTimeWorkRequest secondaryDependent = createTestWorker();
                            leaves.add(primaryContinuation.then(secondaryDependent));
                        }
                    }
                }
                WorkContinuation.combine(leaves).then(createTestWorker()).enqueue();
            }

            private OneTimeWorkRequest createTestWorker() {
                return new OneTimeWorkRequest.Builder(TestWorker.class).build();
            }
        });

        findViewById(R.id.replace_completed_work).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WorkManager workManager = WorkManager.getInstance(MainActivity.this);
                workManager.getWorkInfosForUniqueWorkLiveData(REPLACE_COMPLETED_WORK)
                        .observe(MainActivity.this, new Observer<List<WorkInfo>>() {
                            private int mCount;

                            @Override
                            public void onChanged(@Nullable List<WorkInfo> workInfos) {
                                if (workInfos == null) {
                                    return;
                                }
                                if (!workInfos.isEmpty()) {
                                    WorkInfo status = workInfos.get(0);
                                    if (status.getState().isFinished()) {
                                        if (mCount < NUM_WORKERS) {
                                            // Enqueue another worker.
                                            workManager.beginUniqueWork(
                                                    REPLACE_COMPLETED_WORK,
                                                    ExistingWorkPolicy.REPLACE,
                                                    OneTimeWorkRequest.from(
                                                            TestWorker.class)).enqueue();
                                            mCount += 1;
                                        }
                                    }
                                }
                            }
                        });

                workManager.beginUniqueWork(
                        REPLACE_COMPLETED_WORK,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.from(TestWorker.class)).enqueue();

            }
        });

        findViewById(R.id.run_retry_worker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(
                        RetryWorker.class).build();

                WorkManager.getInstance(MainActivity.this)
                        .enqueueUniqueWork(RetryWorker.TAG, REPLACE, request);
                WorkManager.getInstance(MainActivity.this).getWorkInfoByIdLiveData(request.getId())
                        .observe(MainActivity.this, new Observer<WorkInfo>() {
                            @Override
                            public void onChanged(WorkInfo workInfo) {
                                if (workInfo == null) {
                                    return;
                                }
                                Toast.makeText(
                                        MainActivity.this,
                                        "Run attempt count #" + workInfo.getRunAttemptCount(),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
            }
        });

        findViewById(R.id.run_recursive_worker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OneTimeWorkRequest request =
                        new OneTimeWorkRequest.Builder(RecursiveWorker.class)
                                .addTag(RecursiveWorker.TAG)
                                .build();

                WorkManager.getInstance(MainActivity.this).enqueue(request);
            }
        });

        findViewById(R.id.run_constraint_tracking_worker).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Data inputData = new Data.Builder()
                                .putString(ConstraintTrackingWorker.ARGUMENT_CLASS_NAME,
                                        ForegroundWorker.class.getName())
                                .build();

                        OneTimeWorkRequest request =
                                new OneTimeWorkRequest.Builder(ConstraintTrackingWorker.class)
                                        .setConstraints(new Constraints.Builder()
                                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                                .build())
                                        .setInputData(inputData)
                                        .addTag(CONSTRAINT_TRACKING_TAG)
                                        .build();

                        WorkManager.getInstance(MainActivity.this).enqueue(request);
                    }
                });

        findViewById(R.id.cancel_constraint_tracking_worker).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WorkManager.getInstance(MainActivity.this)
                                .cancelAllWorkByTag(CONSTRAINT_TRACKING_TAG);
                    }
                });

        findViewById(R.id.run_foreground_worker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLastNotificationId += 1;

                Data inputData = new Data.Builder()
                        .putInt(ForegroundWorker.InputNotificationId, mLastNotificationId)
                        .build();

                OneTimeWorkRequest request =
                        new OneTimeWorkRequest.Builder(ForegroundWorker.class)
                                .setInputData(inputData)
                                .setConstraints(new Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED).build()
                                ).build();

                mLastForegroundWorkRequest = request;
                WorkManager.getInstance(MainActivity.this).enqueue(request);
            }
        });

        findViewById(R.id.cancel_foreground_worker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastForegroundWorkRequest != null) {
                    WorkManager.getInstance(MainActivity.this)
                            .cancelWorkById(mLastForegroundWorkRequest.getId());
                    mLastForegroundWorkRequest = null;
                } else {
                    WorkManager.getInstance(MainActivity.this)
                            .cancelAllWorkByTag(ForegroundWorker.class.getName());
                }
            }
        });

        findViewById(R.id.cancel_foreground_worker_intent)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mLastForegroundWorkRequest != null) {
                            PendingIntent pendingIntent =
                                    WorkManager.getInstance(MainActivity.this)
                                            .createCancelPendingIntent(
                                                    mLastForegroundWorkRequest.getId()
                                            );

                            try {
                                pendingIntent.send(0);
                                mLastForegroundWorkRequest = null;
                            } catch (PendingIntent.CanceledException exception) {
                                Log.e(TAG, "Pending intent was cancelled.", exception);
                            }
                        } else {
                            Log.d(TAG, "No work to cancel");
                        }
                    }
                });

        findViewById(R.id.enqueue_work_multi_process).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = MainActivity.this;
                        Intent intent = RemoteService.Companion.enqueueIntent(context);
                        context.startService(intent);
                    }
                });

        findViewById(R.id.enqueue_continuation_multi_process).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = MainActivity.this;
                        Intent intent = RemoteService.Companion.enqueueContinuationIntent(context);
                        context.startService(intent);
                    }
                });

        findViewById(R.id.cancel_work_tag_multiprocess).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = MainActivity.this;
                        Intent intent = RemoteService.Companion.cancelWorkByTagIntent(context);
                        context.startService(intent);
                    }
                });

        findViewById(R.id.cancel_all_work_multiprocess).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = MainActivity.this;
                        Intent intent = RemoteService.Companion.cancelAllWorkIntent(context);
                        context.startService(intent);
                    }
                });

        findViewById(R.id.query_work_multiprocess).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = MainActivity.this;
                        Intent intent = RemoteService.Companion.queryWorkInfoIntent(context);
                        context.startService(intent);
                    }
                });

        findViewById(R.id.enqueue_remote_worker_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String serviceName = RemoteWorkerService.class.getName();
                ComponentName componentName = new ComponentName(PACKAGE_NAME, serviceName);
                OneTimeWorkRequest request = buildOneTimeWorkRemoteWorkRequest(componentName);
                WorkManager.getInstance(MainActivity.this)
                        .enqueue(request);
            }
        });

        findViewById(R.id.enqueue_remote_worker_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String serviceName = RemoteWorkerService2.class.getName();
                ComponentName componentName = new ComponentName(PACKAGE_NAME, serviceName);
                OneTimeWorkRequest request = buildOneTimeWorkRemoteWorkRequest(componentName);
                WorkManager.getInstance(MainActivity.this)
                        .enqueue(request);
            }
        });

        findViewById(R.id.cancel_remote_workers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorkManager.getInstance(MainActivity.this)
                        .cancelAllWorkByTag(RemoteWorker.class.getName());
            }
        });

        findViewById(R.id.crash_app).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                throw new RuntimeException("Crashed app");
            }
        });

        Button hundredJobExceptionButton = findViewById(R.id.create_hundred_job_exception);
        // 100 Job limits are only enforced on API 24+.
        if (Build.VERSION.SDK_INT >= 24) {
            hundredJobExceptionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    JobScheduler jobScheduler =
                            (JobScheduler) v.getContext().getSystemService(JOB_SCHEDULER_SERVICE);
                    WorkManager.getInstance(MainActivity.this).cancelAllWork();
                    jobScheduler.cancelAll();
                    for (int i = 0; i < 101; ++i) {
                        jobScheduler.schedule(
                                new JobInfo.Builder(
                                        100000 + i,
                                        new ComponentName(v.getContext(), SystemJobService.class))
                                        .setMinimumLatency(10 * 60 * 1000)
                                        .build());
                    }
                    for (int i = 0; i < 100; ++i) {
                        WorkManager.getInstance(MainActivity.this).enqueue(
                                new OneTimeWorkRequest.Builder(TestWorker.class)
                                        .setInitialDelay(10L, TimeUnit.MINUTES)
                                        .build());
                    }
                }
            });
        } else {
            hundredJobExceptionButton.setVisibility(View.GONE);
        }
    }

    @NonNull
    OneTimeWorkRequest buildOneTimeWorkRemoteWorkRequest(@NonNull ComponentName componentName) {
        Data data = new Data.Builder()
                .putString(ARGUMENT_PACKAGE_NAME, componentName.getPackageName())
                .putString(ARGUMENT_CLASS_NAME, componentName.getClassName())
                .build();

        return new OneTimeWorkRequest.Builder(RemoteWorker.class)
                .setInputData(data)
                .build();
    }
}
