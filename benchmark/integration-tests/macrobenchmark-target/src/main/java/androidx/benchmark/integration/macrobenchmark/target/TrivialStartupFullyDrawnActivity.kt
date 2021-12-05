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

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.tracing.Trace

/**
 * Trivial activity which triggers reportFullyDrawn ~500ms after resume
 */
@SuppressLint("SyntheticAccessor")
class TrivialStartupFullyDrawnActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Trace.forceEnableAppTracing() // ensure reportFullyDrawn will be traced
    }

    override fun onResume() {
        super.onResume()

        val notice = findViewById<TextView>(R.id.txtNotice)
        notice.text = "INITIAL DISPLAY"

        // report delayed, modify text
        val runnable = {
            notice.text = "FULL DISPLAY"
            reportFullyDrawn()
        }
        notice.postDelayed(runnable, 500 /* ms */)
    }
}
