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

import android.os.Build
import androidx.annotation.NonNull
import kotlin.reflect.KClass

/**
 * A [WorkRequest] for non-repeating work.
 *
 * OneTimeWorkRequests can be put in simple or complex graphs of work by using methods like
 * [WorkManager.enqueue] or [WorkManager.beginWith].
 */
class OneTimeWorkRequest internal constructor(builder: Builder) :
    WorkRequest(builder.id, builder.workSpec, builder.tags) {
    /**
     * Builder for [OneTimeWorkRequest]s.
     *
     * @param workerClass The [ListenableWorker] class to run for this work
     */
    class Builder(workerClass: Class<out ListenableWorker>) :
        WorkRequest.Builder<Builder, OneTimeWorkRequest>(workerClass) {

        /**
         * Specifies the [InputMerger] class name for this [OneTimeWorkRequest].
         *
         * Before workers run, they receive input [Data] from their parent workers, as well as
         * anything specified directly to them via [WorkRequest.Builder.setInputData].
         * An InputMerger takes all of these objects and converts them to a single merged
         * [Data] to be used as the worker input.  The default InputMerger is
         * [OverwritingInputMerger].  This library also offers
         * [ArrayCreatingInputMerger]; you can also specify your own.
         *
         * @param inputMerger The class name of the [InputMerger] for this
         * [OneTimeWorkRequest]
         * @return The current [Builder]
         */
        fun setInputMerger(inputMerger: Class<out InputMerger>): Builder {
            workSpec.inputMergerClassName = inputMerger.name
            return this
        }

        override fun buildInternal(): OneTimeWorkRequest {
            require(
                !(backoffCriteriaSet && Build.VERSION.SDK_INT >= 23 &&
                    workSpec.constraints.requiresDeviceIdle())
            ) { "Cannot set backoff criteria on an idle mode job" }
            return OneTimeWorkRequest(this)
        }

        override val thisObject: Builder
            get() = this
    }

    companion object {
        /**
         * Creates a [OneTimeWorkRequest] with defaults from a  [ListenableWorker] class
         * name.
         *
         * @param workerClass An [ListenableWorker] class name
         * @return A [OneTimeWorkRequest] constructed by using defaults in the [Builder]
         */
        @JvmStatic
        fun from(workerClass: Class<out ListenableWorker>): OneTimeWorkRequest {
            return Builder(workerClass).build()
        }

        /**
         * Creates a list of [OneTimeWorkRequest]s with defaults from an array of
         * [ListenableWorker] class names.
         *
         * @param workerClasses A list of [ListenableWorker] class names
         * @return A list of [OneTimeWorkRequest] constructed by using defaults in the [ ]
         */
        @JvmStatic
        fun from(workerClasses: List<Class<out ListenableWorker>>): List<OneTimeWorkRequest> {
            return workerClasses.map { Builder(it).build() }
        }
    }
}

/**
 * Creates a [OneTimeWorkRequest] with the given [ListenableWorker].
 */
public inline fun <reified W : ListenableWorker> OneTimeWorkRequestBuilder():
    OneTimeWorkRequest.Builder = OneTimeWorkRequest.Builder(W::class.java)

/**
 * Sets an [InputMerger] on the [OneTimeWorkRequest.Builder].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun OneTimeWorkRequest.Builder.setInputMerger(
    @NonNull inputMerger: KClass<out InputMerger>
): OneTimeWorkRequest.Builder = setInputMerger(inputMerger.java)
