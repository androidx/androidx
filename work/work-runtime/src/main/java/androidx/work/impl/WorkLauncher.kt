/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.work.StopReason
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.WorkerParameters.RuntimeExtras
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.StartWorkRunnable
import androidx.work.impl.utils.StopWorkRunnable
import androidx.work.impl.utils.taskexecutor.TaskExecutor

interface WorkLauncher {

    fun startWork(workSpecId: StartStopToken) {
        startWork(workSpecId, null)
    }

    /**
     * @param workSpecId The [WorkSpec] id to start
     * @param runtimeExtras The [WorkerParameters.RuntimeExtras] associated with this work
     */
    fun startWork(workSpecId: StartStopToken, runtimeExtras: RuntimeExtras?)

    /**
     * @param workSpecId The [WorkSpec] id to stop
     */
    fun stopWork(workSpecId: StartStopToken) {
        stopWork(workSpecId, WorkInfo.STOP_REASON_UNKNOWN)
    }

    fun stopWork(workSpecId: StartStopToken, @StopReason reason: Int)

    fun stopWorkWithReason(workSpecId: StartStopToken, @StopReason reason: Int) =
        stopWork(workSpecId, reason)
}

class WorkLauncherImpl(
    val processor: Processor,
    val workTaskExecutor: TaskExecutor,
) : WorkLauncher {
    override fun startWork(workSpecId: StartStopToken, runtimeExtras: RuntimeExtras?) {
        val startWork = StartWorkRunnable(processor, workSpecId, runtimeExtras)
        workTaskExecutor.executeOnTaskThread(startWork)
    }

    override fun stopWork(workSpecId: StartStopToken, @StopReason reason: Int) {
        workTaskExecutor.executeOnTaskThread(
            StopWorkRunnable(processor, workSpecId, false, reason)
        )
    }
}
