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

import androidx.annotation.RestrictTo
import androidx.work.Logger
import androidx.work.StopReason
import androidx.work.WorkInfo
import androidx.work.impl.Processor
import androidx.work.impl.StartStopToken

/**
 * A [Runnable] that requests [androidx.work.impl.Processor] to stop the work
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StopWorkRunnable(
    private val processor: Processor,
    private val token: StartStopToken,
    private val stopInForeground: Boolean,
    @StopReason
    private val reason: Int,
) : Runnable {

    // java compatibility, can't use default args because @JvmOverloads doesn't work with
    // inline classes
    constructor(
        processor: Processor,
        token: StartStopToken,
        stopInForeground: Boolean,
    ) : this(processor, token, stopInForeground, WorkInfo.STOP_REASON_UNKNOWN)

    override fun run() {
        val isStopped = if (stopInForeground) {
            processor.stopForegroundWork(token, reason)
        } else {
            // This call is safe to make for foreground work because Processor ignores requests
            // to stop for foreground work.
            processor.stopWork(token, reason)
        }
        Logger.get().debug(
            Logger.tagWithPrefix("StopWorkRunnable"),
            "StopWorkRunnable for ${token.id.workSpecId}; Processor.stopWork = $isStopped"
        )
    }
}
