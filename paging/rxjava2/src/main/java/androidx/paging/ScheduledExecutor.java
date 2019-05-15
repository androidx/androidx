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

package androidx.paging;

import java.util.concurrent.Executor;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

/**
 * To be used interchangeably as a Scheduler and an Executor, which retains both references to an
 * {@link Executor} and its {@link Scheduler} even after it has been converted by
 * {@link Schedulers#from(Executor)}.
 */
final class ScheduledExecutor extends Scheduler implements Executor {
    private final Executor mExecutor;
    private final Scheduler mScheduler;

    ScheduledExecutor(Scheduler scheduler) {
        final Scheduler.Worker worker = scheduler.createWorker();
        mExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                worker.schedule(command);
            }
        };
        mScheduler = scheduler;
    }

    ScheduledExecutor(Executor executor) {
        mExecutor = executor;
        mScheduler = Schedulers.from(executor);
    }

    @Override
    public Worker createWorker() {
        return mScheduler.createWorker();
    }

    @Override
    public void execute(Runnable command) {
        mExecutor.execute(command);
    }
}

