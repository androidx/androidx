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
package androidx.work.impl.utils

import androidx.work.Configuration
import androidx.work.Operation
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.launchOperation

/**
 * Prunes work in the background. Pruned work meets the following criteria:
 * - Is finished (succeeded, failed, or cancelled)
 * - Has zero unfinished dependents
 */
internal fun WorkDatabase.pruneWork(
    configuration: Configuration,
    executor: TaskExecutor
): Operation =
    launchOperation(
        tracer = configuration.tracer,
        label = "PruneWork",
        executor = executor.serialTaskExecutor
    ) {
        workSpecDao().pruneFinishedWorkWithZeroDependentsIgnoringKeepForAtLeast()
    }
