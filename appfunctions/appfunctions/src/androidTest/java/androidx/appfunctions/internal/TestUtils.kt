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

package androidx.appfunctions.internal

import android.content.Intent
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.TestActivity
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeNotNull

internal fun runWithActivityAppFunctionManager(
    block:
        suspend CoroutineScope.(
            activity: TestActivity, activityAppFunctionManager: AppFunctionManager,
        ) -> Unit
) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val intent =
        Intent(context, TestActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    val activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent)
    assumeNotNull(activity)
    check(activity is TestActivity) { "Failed to start TestActivity, got $activity" }

    runBlocking {
        try {
            val activityAppFunctionManager = AppFunctionManager.getInstance(activity)
            checkNotNull(activityAppFunctionManager)
            block(activity, activityAppFunctionManager)
        } finally {
            activity.finish()
        }
    }
}
