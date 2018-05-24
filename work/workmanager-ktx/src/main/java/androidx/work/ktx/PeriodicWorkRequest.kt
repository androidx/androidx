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

package androidx.work.ktx

import androidx.work.PeriodicWorkRequest
import androidx.work.Worker
import java.util.concurrent.TimeUnit

/**
 * Creates a [PeriodicWorkRequest.Builder] with a given [Worker].
 *
 * @param repeatInterval @see [androidx.work.PeriodicWorkRequest.Builder]
 * @param repeatIntervalTimeUnit @see [androidx.work.PeriodicWorkRequest.Builder]
 */
@Deprecated(
        replaceWith = ReplaceWith(
                expression = "",
                imports = arrayOf("androidx.work.PeriodicWorkRequestBuilder")),
        level = DeprecationLevel.WARNING,
        message = "Use androidx.work.PeriodicWorkRequestBuilder instead")
inline fun <reified W : Worker> PeriodicWorkRequestBuilder(
        repeatInterval: Long,
        repeatIntervalTimeUnit: TimeUnit): PeriodicWorkRequest.Builder {
    return PeriodicWorkRequest.Builder(W::class.java, repeatInterval, repeatIntervalTimeUnit)
}

/**
 * Creates a [PeriodicWorkRequest.Builder] with a given [Worker].
 *
 * @param repeatInterval @see [androidx.work.PeriodicWorkRequest.Builder]
 * @param repeatIntervalTimeUnit @see [androidx.work.PeriodicWorkRequest.Builder]
 * @param flexInterval @see [androidx.work.PeriodicWorkRequest.Builder]
 * @param flexIntervalTimeUnit @see [androidx.work.PeriodicWorkRequest.Builder]
 */
@Deprecated(
        replaceWith = ReplaceWith(
                expression = "",
                imports = arrayOf("androidx.work.PeriodicWorkRequestBuilder")),
        level = DeprecationLevel.WARNING,
        message = "Use androidx.work.PeriodicWorkRequestBuilder instead")
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
