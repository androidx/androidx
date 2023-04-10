/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.integration.macrobenchmark.target

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.tracing.trace
import kotlin.concurrent.thread
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.CountDownLatch

class BackgroundWorkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val notice = findViewById<TextView>(R.id.txtNotice)
        notice.setText(R.string.app_notice)

        startWork(this)
    }

    private fun startWork(context: Context) {
        val count = 20
        var countDownLatch = CountDownLatch(count)

        for (i in 0 until count) {
            var workRequest = OneTimeWorkRequestBuilder<NoOpWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).beginWith(workRequest)
                .enqueue()
            WorkManager.getInstance(context)
                .getWorkInfoByIdLiveData(workRequest.id)
            .observe(this) { workInfo ->
                if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                    countDownLatch.countDown()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT <= 23) {
            // temporary logging/tracing to debug b/204572406
            Log.d("Benchmark", "onResume")
            trace("onResume") {}
        }
    }

    init {
        if (Build.VERSION.SDK_INT <= 23) {
            // temporary tracing to debug b/204572406
            thread {
                while (true) {
                    trace("tracing") { Thread.sleep(50) }
                }
            }
        }
    }
}
