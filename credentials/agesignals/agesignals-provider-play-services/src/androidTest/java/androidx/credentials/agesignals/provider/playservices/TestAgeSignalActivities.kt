/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.agesignals.provider.playservices

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Host activity providing an `Activity` context in instrumentation tests. */
class TestAgeSignalHostActivity : Activity()

/**
 * Fake consent activity launched via [android.app.PendingIntent] during end-to-end interactive
 * fulfillment tests. Immediately finishes with the configured result code and extras.
 */
class FakeAgeSignalConsentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, RESULT_CANCELED)
        val resultBundle = intent.getBundleExtra(EXTRA_RESULT_BUNDLE)
        val resultIntent = resultBundle?.let { Intent().putExtras(it) }
        setResult(resultCode, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_RESULT_CODE =
            "androidx.credentials.agesignals.provider.playservices.test.EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_BUNDLE =
            "androidx.credentials.agesignals.provider.playservices.test.EXTRA_RESULT_BUNDLE"
    }
}
