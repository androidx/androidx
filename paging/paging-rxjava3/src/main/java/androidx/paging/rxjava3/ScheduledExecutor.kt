/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.paging.rxjava3

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.Executor

/**
 * To be used interchangeably as a Scheduler and an Executor, which retains both references to an
 * [Executor] and its [Scheduler] even after it has been converted by [Schedulers.from].
 */
internal class ScheduledExecutor : Scheduler, Executor {
    private val executor: Executor
    private val scheduler: Scheduler

    constructor(scheduler: Scheduler) {
        val worker = scheduler.createWorker()
        executor = Executor { command -> worker.schedule(command) }
        this.scheduler = scheduler
    }

    constructor(executor: Executor) {
        this.executor = executor
        scheduler = Schedulers.from(executor)
    }

    override fun createWorker() = scheduler.createWorker()

    override fun execute(command: Runnable) {
        executor.execute(command)
    }
}
