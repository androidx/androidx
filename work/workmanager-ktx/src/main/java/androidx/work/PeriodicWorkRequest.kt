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

package androidx.work

import android.support.annotation.RequiresApi
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Creates a [PeriodicWorkRequest.Builder] with a given [Worker].
 *
 * @param repeatInterval @see [androidx.work.PeriodicWorkRequest.Builder]
 * @param repeatIntervalTimeUnit @see [androidx.work.PeriodicWorkRequest.Builder]
 */
inline fun <reified W : Worker> PeriodicWorkRequestBuilder(
        repeatInterval: Long,
        repeatIntervalTimeUnit: TimeUnit): PeriodicWorkRequest.Builder {
    return PeriodicWorkRequest.Builder(W::class.java, repeatInterval, repeatIntervalTimeUnit)
}

/**
 * Creates a [PeriodicWorkRequest.Builder] with a given [Worker].
 *
 * @param repeatInterval @see [androidx.work.PeriodicWorkRequest.Builder]
 */
@RequiresApi(26)
inline fun <reified W : Worker> PeriodicWorkRequestBuilder(
        repeatInterval: Duration): PeriodicWorkRequest.Builder {
    return PeriodicWorkRequest.Builder(W::class.java, repeatInterval)
}

/**
 * Creates a [PeriodicWorkRequest.Builder] with a given [Worker].
 *
 * @param repeatInterval @see [androidx.work.PeriodicWorkRequest.Builder]
 * @param repeatIntervalTimeUnit @see [androidx.work.PeriodicWorkRequest.Builder]
 * @param flexInterval @see [androidx.work.PeriodicWorkRequest.Builder]
 * @param flexIntervalTimeUnit @see [androidx.work.PeriodicWorkRequest.Builder]
 */
inline fun <reified W : Worker> PeriodicWorkRequestBuilder(
        repeatInterval: Long,
        repeatIntervalTimeUnit: TimeUnit,
        flexTimeInterval: Long,
        flexTimeIntervalUnit: TimeUnit): PeriodicWorkRequest.Builder {

    return PeriodicWorkRequest.Builder(
            W::class.java,
            repeatInterval,
            repeatIntervalTimeUnit,
            flexTimeInterval,
            flexTimeIntervalUnit)
}

/**
 * Creates a [PeriodicWorkRequest.Builder] with a given [Worker].
 *
 * @param repeatInterval @see [androidx.work.PeriodicWorkRequest.Builder]
 * @param flexInterval @see [androidx.work.PeriodicWorkRequest.Builder]
 */
@RequiresApi(26)
inline fun <reified W : Worker> PeriodicWorkRequestBuilder(
        repeatInterval: Duration,
        flexTimeInterval: Duration): PeriodicWorkRequest.Builder {
    return PeriodicWorkRequest.Builder(W::class.java, repeatInterval, flexTimeInterval)
}
