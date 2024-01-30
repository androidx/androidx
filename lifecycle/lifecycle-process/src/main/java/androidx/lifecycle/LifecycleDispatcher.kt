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
package androidx.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import java.util.concurrent.atomic.AtomicBoolean

/**
 * When initialized, it hooks into the Activity callback of the Application and observes
 * Activities. It is responsible to hook in child-fragments to activities and fragments to report
 * their lifecycle events. Another responsibility of this class is to mark as stopped all lifecycle
 * providers related to an activity as soon it is not safe to run a fragment transaction in this
 * activity.
 */
internal object LifecycleDispatcher {
    private val initialized = AtomicBoolean(false)

    @JvmStatic
    fun init(context: Context) {
        if (initialized.getAndSet(true)) {
            return
        }
        (context.applicationContext as Application)
            .registerActivityLifecycleCallbacks(DispatcherActivityCallback())
    }

    @VisibleForTesting
    internal class DispatcherActivityCallback : EmptyActivityLifecycleCallbacks() {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            ReportFragment.injectIfNeededIn(activity)
        }
    }
}
