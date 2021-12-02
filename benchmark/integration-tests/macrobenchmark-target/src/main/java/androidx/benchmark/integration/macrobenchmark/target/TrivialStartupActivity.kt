/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.tracing.trace
import kotlin.concurrent.thread

class TrivialStartupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val notice = findViewById<TextView>(R.id.txtNotice)
        notice.setText(R.string.app_notice)
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
