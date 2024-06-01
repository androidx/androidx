/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.work.impl

import android.content.Context
import androidx.work.Configuration
import androidx.work.Logger
import androidx.work.impl.background.systemalarm.RescheduleReceiver
import androidx.work.impl.utils.PackageManagerHelper
import androidx.work.impl.utils.isDefaultProcess
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen

private val TAG = Logger.tagWithPrefix("UnfinishedWorkListener")

// Backoff policies.
private const val DELAY_MS = 30_000
private val MAX_DELAY_MS = TimeUnit.HOURS.toMillis(1)

/**
 * Keeps track of unfinished work and enables [RescheduleReceiver] when applicable.
 *
 * Note: The listener is only ever registered in the designated process. This avoids interference
 * from other processes given subsequent registrations are redundant.
 */
internal fun CoroutineScope.maybeLaunchUnfinishedWorkListener(
    appContext: Context,
    configuration: Configuration,
    db: WorkDatabase
) {
    // Only register this in the designated process.
    if (isDefaultProcess(appContext, configuration)) {
        db.workSpecDao()
            .hasUnfinishedWorkFlow()
            .retryWhen { throwable, attempt ->
                Logger.get().error(TAG, "Cannot check for unfinished work", throwable)
                // Linear backoff is good enough here.
                val delayBy = minOf(attempt * DELAY_MS, MAX_DELAY_MS)
                delay(delayBy)
                true
            }
            .conflate()
            .distinctUntilChanged()
            .onEach { hasUnfinishedWork ->
                PackageManagerHelper.setComponentEnabled(
                    appContext,
                    RescheduleReceiver::class.java,
                    hasUnfinishedWork
                )
            }
            .launchIn(this)
    }
}
