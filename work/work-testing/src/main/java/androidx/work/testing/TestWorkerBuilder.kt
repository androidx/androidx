/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.testing

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.work.Data
import androidx.work.Worker
import java.util.concurrent.Executor

/**
 * Builds an instance of [TestWorkerBuilder].
 *
 * @param W The subtype of [Worker]
 * @param context The application [Context]
 * @param executor The [Executor] that the [Worker] runs on
 * @param inputData The input data for the [Worker]
 * @param runAttemptCount The run attempt count of the [Worker]
 * @param triggeredContentUris The list of triggered content [Uri]s
 * @param triggeredContentAuthorities The list of triggered content authorities
 * @return The instance of [TestWorkerBuilder]
 */
public inline fun <reified W : Worker> TestWorkerBuilder(
    context: Context,
    executor: Executor,
    inputData: Data = Data.EMPTY,
    tags: List<String> = emptyList(),
    runAttemptCount: Int = 1,
    triggeredContentUris: List<Uri> = emptyList(),
    triggeredContentAuthorities: List<String> = emptyList()
): TestWorkerBuilder<W> {
    val builder = TestWorkerBuilder.from(context, W::class.java, executor)
    builder.apply {
        setInputData(inputData)
        setTags(tags)
        setRunAttemptCount(runAttemptCount)
        if (Build.VERSION.SDK_INT >= 24) {
            setTriggeredContentUris(triggeredContentUris)
            setTriggeredContentAuthorities(triggeredContentAuthorities)
        }
    }
    return builder
}
