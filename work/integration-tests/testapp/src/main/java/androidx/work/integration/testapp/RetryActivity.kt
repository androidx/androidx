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
package androidx.work.integration.testapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * An activity to test retries.
 */
class RetryActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var textView: TextView
    private val workManager by lazy { WorkManager.getInstance(this@RetryActivity) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.retry_activity)
        button = findViewById(R.id.btn)
        textView = findViewById(R.id.textview)
        button.setOnClickListener {
            scheduleWork("one", 3, 0.2)
            scheduleWork("two", 5, 0.5)
        }
        workManager.getWorkInfosByTagLiveData("test").observe(this) { workInfos ->
            textView.text = workInfos?.joinToString("\n") { workInfo ->
                "id: ${workInfo.id.toString().take(4)} (${workInfo.state})"
            } ?: "nothing to show"
        }
    }

    private fun scheduleWork(name: String, timeTaken: Int, errorRate: Double) {
        val constraints = Constraints(requiredNetworkType = NetworkType.CONNECTED)
        val workRequest = OneTimeWorkRequest.Builder(Worker::class.java)
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    NAME to name,
                    TIME_TAKEN to timeTaken,
                    ERROR_RATE to errorRate,
                )
            )
            .addTag("test")
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()
        workManager.beginUniqueWork(name, ExistingWorkPolicy.KEEP, workRequest).enqueue()
    }

    /**
     * A Worker to test retries.
     */
    class Worker(context: Context, workerParams: WorkerParameters) :
        androidx.work.Worker(context, workerParams) {
        @SuppressLint("BanThreadSleep")
        override fun doWork(): Result {
            val name = inputData.getString(NAME)
            val timeTaken = inputData.getInt(TIME_TAKEN, 3)
            val errorRate = inputData.getDouble(ERROR_RATE, 0.5)
            return try {
                Log.i(TAG, "[$name] $id started (run attempt = $runAttemptCount)")
                for (i in 0 until timeTaken) {
                    Thread.sleep(1000L)
                    Log.v(TAG, "[$name] $id completed stage = $i")
                }
                if (Math.random() < errorRate) {
                    throw RuntimeException("random failure")
                }
                Log.i(TAG, "[$name] $id successful")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "[$name] $id failed: ${e.message}")
                Result.retry()
            }
        }
    }
}

private const val NAME = "name"
private const val TIME_TAKEN = "time_taken"
private const val ERROR_RATE = "error_rate"
private const val TAG = "RetryActivity"
