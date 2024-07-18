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
package androidx.work.integration.testapp

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.Constraints.ContentUriTrigger
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequest.Companion.from
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.impl.background.systemjob.SystemJobService
import androidx.work.impl.workers.ConstraintTrackingWorker
import androidx.work.integration.testapp.RemoteService.Companion.cancelAllWorkIntent
import androidx.work.integration.testapp.RemoteService.Companion.cancelWorkByTagIntent
import androidx.work.integration.testapp.RemoteService.Companion.enqueueContinuationIntent
import androidx.work.integration.testapp.RemoteService.Companion.enqueueIntent
import androidx.work.integration.testapp.RemoteService.Companion.enqueueUniquePeriodicIntent
import androidx.work.integration.testapp.RemoteService.Companion.queryWorkInfoIntent
import androidx.work.integration.testapp.RemoteService.Companion.updateUniquePeriodicIntent
import androidx.work.integration.testapp.imageprocessing.ImageProcessingActivity
import androidx.work.integration.testapp.sherlockholmes.AnalyzeSherlockHolmesActivity
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import androidx.work.multiprocess.RemoteWorkerService
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Main Activity
 */
class MainActivity : AppCompatActivity() {
    private var lastForegroundWorkRequest: WorkRequest? = null
    private var lastNotificationId = 10
    private val workManager: WorkManager by lazy { WorkManager.getInstance(this) }

    @SuppressLint("ClassVerificationFailure")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.enqueue_infinite_work_charging).setOnClickListener {
            workManager.enqueue(
                OneTimeWorkRequest.Builder(InfiniteWorker::class.java)
                    .setConstraints(Constraints(requiresCharging = true)).build()
            )
        }
        findViewById<View>(R.id.enqueue_infinite_work_network).setOnClickListener {
            workManager.enqueue(
                OneTimeWorkRequest.Builder(InfiniteWorker::class.java)
                    .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                    .build()
            )
        }
        findViewById<View>(R.id.enqueue_battery_not_low).setOnClickListener {
            workManager.enqueue(
                OneTimeWorkRequest.Builder(TestWorker::class.java)
                    .setConstraints(Constraints(requiresBatteryNotLow = true)).build()
            )
        }
        findViewById<View>(R.id.sherlock_holmes).setOnClickListener {
            startActivity(
                Intent(this@MainActivity, AnalyzeSherlockHolmesActivity::class.java)
            )
        }
        findViewById<View>(R.id.image_processing).setOnClickListener {
            startActivity(
                Intent(this@MainActivity, ImageProcessingActivity::class.java)
            )
        }
        findViewById<View>(R.id.image_uri).setOnClickListener(View.OnClickListener {
            if (Build.VERSION.SDK_INT < 24) {
                return@OnClickListener
            }
            val constraints = Constraints(
                contentUriTriggers = setOf(
                    ContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                )
            )
            workManager.enqueue(
                ToastWorker.create("Image URI Updated!").setConstraints(constraints).build()
            )
        })
        val delayInMs = findViewById<EditText>(R.id.delay_in_ms)
        findViewById<View>(R.id.schedule_delay).setOnClickListener {
            val delayString = delayInMs.text.toString()
            val delay = delayString.toLong()
            Log.d(TAG, "Enqueuing job with delay of $delay ms")
            workManager.enqueue(
                ToastWorker.create("Delayed Job Ran!").setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()
            )
        }
        findViewById<View>(R.id.coroutine_sleep).setOnClickListener {
            val delayString = delayInMs.text.toString()
            val delay = delayString.toLong()
            Log.d(TAG, "Enqueuing job with delay of $delay ms")
            val inputData = workDataOf("sleep_time" to delay)
            workManager.enqueue(
                OneTimeWorkRequest.Builder(CoroutineSleepWorker::class.java).setInputData(inputData)
                    .addTag("coroutine_sleep").build()
            )
        }
        findViewById<View>(R.id.coroutine_cancel).setOnClickListener {
            workManager.cancelAllWorkByTag("coroutine_sleep")
        }
        findViewById<View>(R.id.enqueue_periodic_work).setOnClickListener {
            val input = workDataOf(ToastWorker.ARG_MESSAGE to "Periodic work")
            val request = PeriodicWorkRequest.Builder(
                ToastWorker::class.java, 15, TimeUnit.MINUTES
            ).setInputData(input).build()
            workManager.enqueue(request)
        }
        findViewById<View>(R.id.enqueue_periodic_work_flex).setOnClickListener {
            val input = workDataOf(ToastWorker.ARG_MESSAGE to "Periodic work with Flex")
            val request = PeriodicWorkRequest.Builder(
                ToastWorker::class.java, 15, TimeUnit.MINUTES, 10, TimeUnit.MINUTES
            ).setInputData(input).build()
            workManager.enqueue(request)
        }
        findViewById<View>(R.id.enqueue_periodic_initial_delay).setOnClickListener {
            val input = workDataOf(ToastWorker.ARG_MESSAGE to "Periodic work")
            val request = PeriodicWorkRequest.Builder(
                ToastWorker::class.java, 15, TimeUnit.MINUTES
            ).setInitialDelay(1, TimeUnit.MINUTES).setInputData(input).build()
            workManager.enqueue(request)
        }
        findViewById<View>(R.id.begin_unique_work_loop).setOnClickListener {
            val keep = findViewById<CheckBox>(R.id.keep)
            val policy = if (keep.isChecked) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE
            repeat(50) {
                workManager.beginUniqueWork(UNIQUE_WORK_NAME, policy, from(SleepWorker::class.java))
                    .enqueue()
            }
        }
        findViewById<View>(R.id.enqueue_lots_of_work).setOnClickListener {
            repeat(NUM_WORKERS) {
                // Exceed Scheduler.MAX_SCHEDULER_LIMIT (100)
                workManager.beginWith(from(SleepWorker::class.java)).enqueue()
            }
        }
        findViewById<View>(R.id.exploding_work).setOnClickListener {
            val leaves = mutableListOf<WorkContinuation>()
            repeat(10) {
                val workRequest = from(TestWorker::class.java)
                val continuation = workManager.beginWith(workRequest)
                repeat(10) {
                    val primaryDependent = from(TestWorker::class.java)
                    val primaryContinuation = continuation.then(primaryDependent)
                    repeat(10) {
                        val secondaryDependent = from(TestWorker::class.java)
                        leaves.add(primaryContinuation.then(secondaryDependent))
                    }
                }
            }
            WorkContinuation.combine(leaves).then(from(TestWorker::class.java)).enqueue()
        }
        findViewById<View>(R.id.replace_completed_work).setOnClickListener {
            workManager.getWorkInfosForUniqueWorkLiveData(REPLACE_COMPLETED_WORK)
                .observe(this, object : Observer<List<WorkInfo>> {
                    private var count = 0
                    override fun onChanged(value: List<WorkInfo>) {
                        if (value.isNotEmpty()) {
                            val status = value[0]
                            if (status.state.isFinished && count < NUM_WORKERS) {
                                // Enqueue another worker.
                                workManager.beginUniqueWork(
                                    REPLACE_COMPLETED_WORK,
                                    ExistingWorkPolicy.REPLACE,
                                    from(TestWorker::class.java)
                                ).enqueue()
                                count += 1
                            }
                        }
                    }
                })
            workManager.beginUniqueWork(
                REPLACE_COMPLETED_WORK, ExistingWorkPolicy.REPLACE, from(TestWorker::class.java)
            ).enqueue()
        }
        findViewById<View>(R.id.run_retry_worker).setOnClickListener {
            val request = from(RetryWorker::class.java)
            workManager.enqueueUniqueWork(RetryWorker.TAG, ExistingWorkPolicy.REPLACE, request)
            workManager.getWorkInfoByIdLiveData(request.id)
                .observe(this, Observer<WorkInfo?> { value ->
                    if (value == null) return@Observer

                    Toast.makeText(
                        this@MainActivity,
                        "Run attempt count #${value.runAttemptCount}",
                        Toast.LENGTH_SHORT
                    ).show()
                })
        }
        findViewById<View>(R.id.run_recursive_worker).setOnClickListener {
            val request =
                OneTimeWorkRequest.Builder(RecursiveWorker::class.java).addTag(RecursiveWorker.TAG)
                    .build()
            workManager.enqueue(request)
        }
        findViewById<View>(R.id.run_constraint_tracking_worker).setOnClickListener {
            val inputData = workDataOf(ARGUMENT_CLASS_NAME to ForegroundWorker::class.java.name)

            val request = OneTimeWorkRequest.Builder(ConstraintTrackingWorker::class.java)
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .setInputData(inputData).addTag(CONSTRAINT_TRACKING_TAG).build()
            workManager.enqueue(request)
        }
        findViewById<View>(R.id.cancel_constraint_tracking_worker).setOnClickListener {
            workManager.cancelAllWorkByTag(CONSTRAINT_TRACKING_TAG)
        }
        findViewById<View>(R.id.run_foreground_worker).setOnClickListener {
            lastNotificationId += 1
            val inputData = workDataOf(ForegroundWorker.InputNotificationId to lastNotificationId)

            val request =
                OneTimeWorkRequest.Builder(ForegroundWorker::class.java).setInputData(inputData)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                    .build()
            lastForegroundWorkRequest = request
            workManager.enqueue(request)
        }
        findViewById<View>(R.id.cancel_foreground_worker).setOnClickListener {
            if (lastForegroundWorkRequest != null) {
                workManager.cancelWorkById(lastForegroundWorkRequest!!.id)
                lastForegroundWorkRequest = null
            } else {
                workManager.cancelAllWorkByTag(ForegroundWorker::class.java.name)
            }
        }
        findViewById<View>(R.id.cancel_foreground_worker_intent).setOnClickListener {
            if (lastForegroundWorkRequest != null) {
                val pendingIntent =
                    workManager.createCancelPendingIntent(lastForegroundWorkRequest!!.id)
                try {
                    pendingIntent.send(0)
                    lastForegroundWorkRequest = null
                } catch (exception: PendingIntent.CanceledException) {
                    Log.e(TAG, "Pending intent was cancelled.", exception)
                }
            } else {
                Log.d(TAG, "No work to cancel")
            }
        }
        findViewById<View>(R.id.enqueue_work_multi_process).setOnClickListener {
            startService(enqueueIntent(this))
        }
        findViewById<View>(R.id.enqueue_continuation_multi_process).setOnClickListener {
            startService(enqueueContinuationIntent(this))
        }
        findViewById<View>(R.id.cancel_work_tag_multiprocess).setOnClickListener {
            startService(cancelWorkByTagIntent(this))
        }
        findViewById<View>(R.id.cancel_all_work_multiprocess).setOnClickListener {
            startService(cancelAllWorkIntent(this))
        }
        findViewById<View>(R.id.query_work_multiprocess).setOnClickListener {
            startService(queryWorkInfoIntent(this))
        }
        findViewById<View>(R.id.enqueue_periodic_work_multiprocess).setOnClickListener {
            startService(enqueueUniquePeriodicIntent(this))
        }
        findViewById<View>(R.id.update_periodic_work_multiprocess).setOnClickListener {
            startService(updateUniquePeriodicIntent(this))
        }
        findViewById<View>(R.id.enqueue_remote_worker_1).setOnClickListener {
            val serviceName = RemoteWorkerService::class.java.name
            val componentName = ComponentName(PACKAGE_NAME, serviceName)
            val request = oneTimeWorkRemoteWorkRequest(componentName)
            workManager.enqueue(request)
        }
        findViewById<View>(R.id.enqueue_remote_worker_2).setOnClickListener {
            val serviceName = RemoteWorkerService2::class.java.name
            val componentName = ComponentName(PACKAGE_NAME, serviceName)
            val request = oneTimeWorkRemoteWorkRequest(componentName)
            workManager.enqueue(request)
        }
        findViewById<View>(R.id.cancel_remote_workers).setOnClickListener {
            workManager.cancelAllWorkByTag(RemoteWorker::class.java.name)
        }
        findViewById<View>(R.id.crash_app).setOnClickListener {
            throw RuntimeException("Crashed app")
        }
        findViewById<View>(R.id.enqueue_infinite_work_charging).setOnClickListener {
            queueLotsOfWorkers(workManager)
        }
        val hundredJobExceptionButton = findViewById<Button>(R.id.create_hundred_job_exception)
        // 100 Job limits are only enforced on API 24+.
        if (Build.VERSION.SDK_INT >= 24) {
            hundredJobExceptionButton.setOnClickListener {
                val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
                workManager.cancelAllWork()
                jobScheduler.cancelAll()
                repeat(101) { i ->
                    jobScheduler.schedule(
                        JobInfo.Builder(
                            100000 + i, ComponentName(this, SystemJobService::class.java)
                        ).setMinimumLatency((10 * 60 * 1000).toLong()).build()
                    )
                }
                repeat(100) {
                    workManager.enqueue(
                        OneTimeWorkRequest.Builder(TestWorker::class.java)
                            .setInitialDelay(10L, TimeUnit.MINUTES).build()
                    )
                }
            }
        } else {
            hundredJobExceptionButton.visibility = View.GONE
        }
    }

    private fun oneTimeWorkRemoteWorkRequest(componentName: ComponentName): OneTimeWorkRequest {
        val data = workDataOf(
            ARGUMENT_PACKAGE_NAME to componentName.packageName,
            ARGUMENT_CLASS_NAME to componentName.className,
        )
        return OneTimeWorkRequest.Builder(RemoteWorker::class.java).setInputData(data)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED)).build()
    }
}

private const val PACKAGE_NAME = "androidx.work.integration.testapp"
private const val TAG = "MainActivity"
private const val CONSTRAINT_TRACKING_TAG = "ConstraintTrackingWorker"
private const val UNIQUE_WORK_NAME = "importantUniqueWork"
private const val REPLACE_COMPLETED_WORK = "replaceCompletedWork"
private const val NUM_WORKERS = 150
private const val ARGUMENT_CLASS_NAME =
    "androidx.work.impl.workers.ConstraintTrackingWorker.ARGUMENT_CLASS_NAME"
