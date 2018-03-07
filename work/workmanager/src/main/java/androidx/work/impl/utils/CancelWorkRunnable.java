/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.work.impl.utils;

import static androidx.work.State.CANCELLED;
import static androidx.work.State.FAILED;
import static androidx.work.State.SUCCEEDED;

import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.List;

import androidx.work.State;
import androidx.work.impl.Processor;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkSpecDao;

/**
 * A {@link Runnable} to cancel work.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CancelWorkRunnable implements Runnable {

    private WorkManagerImpl mWorkManagerImpl;
    private WorkDatabase mWorkDatabase;
    private String mId;
    private String mTag;

    public CancelWorkRunnable(WorkManagerImpl workManagerImpl, String id, String tag) {
        mWorkManagerImpl = workManagerImpl;
        mWorkDatabase = mWorkManagerImpl.getWorkDatabase();
        mId = id;
        mTag = tag;
    }

    @WorkerThread
    @Override
    public void run() {
        mWorkDatabase.beginTransaction();
        try {
            WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
            if (mTag != null) {
                List<String> workSpecIds = workSpecDao.getUnfinishedWorkWithTag(mTag);
                for (String workSpecId : workSpecIds) {
                    cancel(workSpecId);
                }
            } else if (mId != null) {
                cancel(mId);
            }
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }
    }

    private void cancel(String workSpecId) {
        recursivelyCancelWorkAndDependents(workSpecId);

        Processor processor = mWorkManagerImpl.getProcessor();
        processor.stopWork(workSpecId, true);
        processor.setCancelled(workSpecId);

        for (Scheduler scheduler : mWorkManagerImpl.getSchedulers()) {
            scheduler.cancel(workSpecId);
        }
    }

    private void recursivelyCancelWorkAndDependents(String workSpecId) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        DependencyDao dependencyDao = mWorkDatabase.dependencyDao();

        List<String> dependentIds = dependencyDao.getDependentWorkIds(workSpecId);
        for (String id : dependentIds) {
            recursivelyCancelWorkAndDependents(id);
        }

        State state = workSpecDao.getState(workSpecId);
        if (state != SUCCEEDED && state != FAILED) {
            workSpecDao.setState(CANCELLED, workSpecId);
        }
    }
}
