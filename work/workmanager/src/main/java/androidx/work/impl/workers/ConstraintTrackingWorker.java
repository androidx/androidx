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
import android.util.Log;

import androidx.work.Worker;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.WorkerWrapper;
import androidx.work.impl.constraints.WorkConstraintsCallback;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;

import java.util.Collections;
import java.util.List;

/**
 * Is an implementation of a {@link Worker} that can delegate to a different {@link Worker}
 * when the constraints are met.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ConstraintTrackingWorker extends Worker implements WorkConstraintsCallback {

    private static final String TAG = "ConstraintTrkngWrkr";

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
    public @NonNull Result doWork() {
        String className = getInputData().getString(ARGUMENT_CLASS_NAME);
        if (TextUtils.isEmpty(className)) {
            Log.d(TAG, "No worker to delegate to.");
            return Result.FAILURE;
        }
        // Instantiate the delegated worker. Use the same workSpecId, and the same Data
        // as this Worker's Data are a superset of the delegate's Worker's Data.
        mDelegate = WorkerWrapper.workerFromClassName(
                getApplicationContext(),
                className,
                getId(),
                getExtras());

        if (mDelegate == null) {
            Log.d(TAG, "No worker to delegate to.");
            return Result.FAILURE;
        }

        WorkDatabase workDatabase = getWorkDatabase();

        // We need to know what the real constraints are for the delegate.
        WorkSpec workSpec = workDatabase.workSpecDao().getWorkSpec(getId().toString());
        if (workSpec == null) {
            return Result.FAILURE;
        }
        WorkConstraintsTracker workConstraintsTracker =
                new WorkConstraintsTracker(getApplicationContext(), this);

        // Start tracking
        workConstraintsTracker.replace(Collections.singletonList(workSpec));

        if (workConstraintsTracker.areAllConstraintsMet(getId().toString())) {
            Log.d(TAG, String.format("Constraints met for delegate %s", className));

            // Wrapping the call to mDelegate#doWork() in a try catch, because
            // changes in constraints can cause the worker to throw RuntimeExceptions, and
            // that should cause a retry.
            try {
                Result result = mDelegate.doWork();
                synchronized (mLock) {
                    if (mAreConstraintsUnmet) {
                        return Result.RETRY;
                    } else {
                        setOutputData(mDelegate.getOutputData());
                        return result;
                    }
                }
            } catch (Throwable exception) {
                Log.d(TAG, String.format(
                        "Delegated worker %s threw a runtime exception.", className), exception);
                synchronized (mLock) {
                    if (mAreConstraintsUnmet) {
                        Log.d(TAG, "Constraints were unmet, Retrying.");
                        return Result.RETRY;
                    } else {
                        return Result.FAILURE;
                    }
                }
            }
        } else {
            Log.d(TAG, String.format(
                    "Constraints not met for delegate %s. Requesting retry.", className));
            return Result.RETRY;
        }
    }

    /**
     * @return The instance of {@link WorkDatabase}.
     */
    @VisibleForTesting
    public WorkDatabase getWorkDatabase() {
        return WorkManagerImpl.getInstance().getWorkDatabase();
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        // WorkConstraintTracker notifies on the main thread. So we don't want to trampoline
        // between the background thread and the main thread in this case.
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        // If at any point, constraints are not met mark it so we can retry the work.
        Log.d(TAG, String.format("Constraints changed for %s", workSpecIds));
        synchronized (mLock) {
            mAreConstraintsUnmet = true;
        }
    }
}
