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

package androidx.activity

import android.app.Activity
import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule

@Throws(Throwable::class)
internal fun <T : Activity> recreateActivity(activityRule: ActivityTestRule<T>): T {
    val previous = activityRule.activity
    val monitor = Instrumentation.ActivityMonitor(previous.javaClass.canonicalName, null, false)
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.addMonitor(monitor)
    activityRule.runOnUiThread { previous.recreate() }
    var result = previous

    // this guarantee that we will reinstall monitor between notifications about onDestroy
    // and onCreate

    synchronized(monitor) {
        do {
            // the documentation says "Block until an Activity is created
            // that matches this monitor." This statement is true, but there are some other
            // true statements like: "Block until an Activity is destroyed" or
            // "Block until an Activity is resumed"...

            // this call will release synchronization monitor's monitor
            @Suppress("UNCHECKED_CAST")
            result = monitor.waitForActivityWithTimeout(4000) as T?
                    ?: throw RuntimeException("Timeout. Failed to recreate an activity")
        } while (result === previous)
    }
    return result
}