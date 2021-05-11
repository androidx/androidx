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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView

/**
 * Activity with configurable text and launch time, for testing.
 */
public class ConfigurableActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            TextView(this).apply {
                text = intent.getStringExtra(EXTRA_TEXT)
            }
        )

        val sleepDurMs = intent.getLongExtra(EXTRA_SLEEP_DUR_MS, 0)
        if (sleepDurMs > 0) {
            Log.d(TAG, "sleeping $sleepDurMs ms")
            Thread.sleep(sleepDurMs)
            Log.d(TAG, "sleep complete")
        }
    }

    public companion object {
        private const val TAG = "ConfigurableActivity"
        public const val ACTION: String = "androidx.benchmark.macro.CONFIGURABLE_ACTIVITY"
        public const val EXTRA_TEXT: String = "TEXT"
        public const val EXTRA_SLEEP_DUR_MS: String = "SLEEP_DUR_MS"

        public fun createIntent(text: String, sleepDurMs: Long = 0): Intent {
            return Intent().apply {
                action = ACTION
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_SLEEP_DUR_MS, sleepDurMs)
            }
        }
    }
}
