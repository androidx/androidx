/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Activity with configurable text and launch time, for testing. */
class ConfigurableActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textContent = intent.getStringExtra(EXTRA_TEXT)

        val view = TextView(this).apply { text = textContent }
        setContentView(view)

        val sleepDurMs = intent.getLongExtra(EXTRA_SLEEP_DUR_MS, 0)
        if (sleepDurMs > 0) {
            Log.d(TAG, "sleeping $sleepDurMs ms")
            Thread.sleep(sleepDurMs)
            Log.d(TAG, "sleep complete")
        }

        val reportFullyDrawnDelayMs =
            intent.getLongExtra(EXTRA_REPORT_FULLY_DRAWN_DELAY_MS, /* default */ -1)
        when (reportFullyDrawnDelayMs) {
            -1L -> {} // ignore
            0L -> reportFullyDrawn() // report immediately
            else -> {
                // report delayed, modify text
                val runnable = {
                    view.text =
                        if (textContent == INNER_ACTIVITY_TEXT) {
                            INNER_ACTIVITY_FULLY_DRAWN_TEXT
                        } else {
                            FULLY_DRAWN_TEXT
                        }
                    reportFullyDrawn()
                }
                view.postDelayed(runnable, reportFullyDrawnDelayMs)
            }
        }
        // enable in-app navigation, which carries forward report fully drawn delay
        view.setOnClickListener {
            startActivity(
                createIntent(
                        text = INNER_ACTIVITY_TEXT,
                        reportFullyDrawnDelayMs = reportFullyDrawnDelayMs
                    )
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    companion object {
        private const val TAG = "ConfigurableActivity"
        private const val ACTION: String = "androidx.benchmark.macro.CONFIGURABLE_ACTIVITY"
        private const val EXTRA_TEXT: String = "TEXT"
        private const val EXTRA_SLEEP_DUR_MS: String = "SLEEP_DUR_MS"
        private const val EXTRA_REPORT_FULLY_DRAWN_DELAY_MS = "REPORT_FULLY_DRAWN_DELAY_MS"
        const val FULLY_DRAWN_TEXT = "FULLY DRAWN"
        const val INNER_ACTIVITY_TEXT = "INNER ACTIVITY"
        const val INNER_ACTIVITY_FULLY_DRAWN_TEXT = "INNER ACTIVITY FULLY DRAWN"

        fun createIntent(
            text: String,
            sleepDurMs: Long = 0,
            reportFullyDrawnDelayMs: Long? = null
        ): Intent {
            return Intent().apply {
                action = ACTION
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_SLEEP_DUR_MS, sleepDurMs)
                putExtra(EXTRA_REPORT_FULLY_DRAWN_DELAY_MS, reportFullyDrawnDelayMs)
            }
        }
    }
}
