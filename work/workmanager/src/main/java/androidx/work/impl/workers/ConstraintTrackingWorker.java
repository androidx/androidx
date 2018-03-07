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

package androidx.work.impl.workers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import java.util.Collections;
import java.util.List;

import androidx.work.Worker;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.WorkerWrapper;
import androidx.work.impl.constraints.WorkConstraintsCallback;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.logger.Logger;
import androidx.work.impl.model.WorkSpec;

/**
 * Is an implementation of a {@link Worker} that can delegate to a different {@link Worker}
 * when the constraints are met.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ConstraintTrackingWorker extends Worker implements WorkConstraintsCallback {

    private static final String TAG = "ConstraintTrackingWorker";

    /**
     * The {@code className} of the {@link Worker} to delegate to.
     */
    public static final String ARGUMENT_CLASS_NAME =
            "androidx.work.impl.workers.ConstraintTrackingWorker.ARGUMENT_CLASS_NAME";

    @Nullable
    private Worker mDelegate;

    private final Object mLock;
    private boolean mAreConstraintsUnmet;

    public ConstraintTrackingWorker() {
        mLock = new Object();
        mAreConstraintsUnmet = false;
    }

    @Override
    public WorkerResult doWork() {
        String className = getArguments().getString(ARGUMENT_CLASS_NAME, null);
        if (TextUtils.isEmpty(className)) {
            Logger.debug(TAG, "No worker to delegate to.");
            return WorkerResult.FAILURE;
        }
        // Instantiate the delegated worker. Use the same workSpecId, and the same Arguments
        // as this Worker's Arguments are a superset of the delegate's Worker's Arguments.
        mDelegate = WorkerWrapper.workerFromClassName(getAppContext(), className, getId(),
                getArguments());

        if (mDelegate == null) {
            Logger.debug(TAG, "No worker to delegate to.");
            return WorkerResult.FAILURE;
        }

        WorkDatabase workDatabase = getWorkDatabase();

        // We need to know what the real constraints are for the delegate.
        WorkSpec workSpec = workDatabase.workSpecDao().getWorkSpec(getId());
        WorkConstraintsTracker workConstraintsTracker =
                new WorkConstraintsTracker(getAppContext(), this);

        // Start tracking
        workConstraintsTracker.replace(Collections.singletonList(workSpec));

        if (workConstraintsTracker.areAllConstraintsMet(getId())) {
            Logger.debug(TAG, "Constraints met for delegate %s", className);

            // Wrapping the call to mDelegate#doWork() in a try catch, because
            // changes in constraints can cause the worker to throw RuntimeExceptions, and
            // that should cause a retry.
            try {
                WorkerResult result = mDelegate.doWork();
                synchronized (mLock) {
                    if (mAreConstraintsUnmet) {
                        return WorkerResult.RETRY;
                    } else {
                        setOutput(mDelegate.getOutput());
                        return result;
                    }
                }
            } catch (Throwable exception) {
                Logger.debug(TAG, "Delegated worker %s threw a runtime exception.", exception,
                        className);
                synchronized (mLock) {
                    if (mAreConstraintsUnmet) {
                        Logger.debug(TAG, "Constraints were unmet, Retrying.");
                        return WorkerResult.RETRY;
                    } else {
                        return WorkerResult.FAILURE;
                    }
                }
            }
        } else {
            Logger.debug(TAG, "Constraints not met for delegate %s. Requesting retry.", className);
            return WorkerResult.RETRY;
        }
    }

    /**
     * @return The instance of {@link WorkDatabase}.
     */
    @VisibleForTesting
    public WorkDatabase getWorkDatabase() {
        return WorkManagerImpl.getInstance(getAppContext()).getWorkDatabase();
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        // WorkConstraintTracker notifies on the main thread. So we don't want to trampoline
        // between the background thread and the main thread in this case.
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        // If at any point, constraints are not met mark it so we can retry the work.
        Logger.debug(TAG, "Constraints changed for %s", workSpecIds);
        synchronized (mLock) {
            mAreConstraintsUnmet = true;
        }
    }
}
