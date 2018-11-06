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

import static androidx.work.ListenableWorker.Result.FAILURE;
import static androidx.work.ListenableWorker.Result.RETRY;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Logger;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.constraints.WorkConstraintsCallback;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;

/**
 * Is an implementation of a {@link Worker} that can delegate to a different {@link Worker}
 * when the constraints are met.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ConstraintTrackingWorker extends ListenableWorker implements WorkConstraintsCallback {

    private static final String TAG = "ConstraintTrkngWrkr";

    /**
     * The {@code className} of the {@link Worker} to delegate to.
     */
    public static final String ARGUMENT_CLASS_NAME =
            "androidx.work.impl.workers.ConstraintTrackingWorker.ARGUMENT_CLASS_NAME";

    private WorkerParameters mWorkerParameters;

    // These are package-private to avoid synthetic accessor.
    final Object mLock;
    // Marking this volatile as the delegated workers could switch threads.
    volatile boolean mAreConstraintsUnmet;
    SettableFuture<Payload> mFuture;

    @Nullable private ListenableWorker mDelegate;

    public ConstraintTrackingWorker(@NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
        mWorkerParameters = workerParams;
        mLock = new Object();
        mAreConstraintsUnmet = false;
        mFuture = SettableFuture.create();
    }

    @NonNull
    @Override
    public ListenableFuture<Payload> startWork() {
        getBackgroundExecutor().execute(new Runnable() {
            @Override
            public void run() {
                setupAndRunConstraintTrackingWork();
            }
        });
        return mFuture;
    }

    // Package-private to avoid synthetic accessor.
    void setupAndRunConstraintTrackingWork() {
        String className = getInputData().getString(ARGUMENT_CLASS_NAME);
        if (TextUtils.isEmpty(className)) {
            Logger.error(TAG, "No worker to delegate to.");
            setFutureFailed();
            return;
        }

        mDelegate = getWorkerFactory().createWorkerWithDefaultFallback(
                getApplicationContext(),
                className,
                mWorkerParameters);

        if (mDelegate == null) {
            Logger.debug(TAG, "No worker to delegate to.");
            setFutureFailed();
            return;
        }

        WorkDatabase workDatabase = getWorkDatabase();

        // We need to know what the real constraints are for the delegate.
        WorkSpec workSpec = workDatabase.workSpecDao().getWorkSpec(getId().toString());
        if (workSpec == null) {
            setFutureFailed();
            return;
        }
        WorkConstraintsTracker workConstraintsTracker =
                new WorkConstraintsTracker(getApplicationContext(), this);

        // Start tracking
        workConstraintsTracker.replace(Collections.singletonList(workSpec));

        if (workConstraintsTracker.areAllConstraintsMet(getId().toString())) {
            Logger.debug(TAG, String.format("Constraints met for delegate %s", className));

            // Wrapping the call to mDelegate#doWork() in a try catch, because
            // changes in constraints can cause the worker to throw RuntimeExceptions, and
            // that should cause a retry.
            try {
                final ListenableFuture<Payload> innerFuture = mDelegate.startWork();
                innerFuture.addListener(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mLock) {
                            if (mAreConstraintsUnmet) {
                                setFutureRetry();
                            } else {
                                mFuture.setFuture(innerFuture);
                            }
                        }
                    }
                }, getBackgroundExecutor());
            } catch (Throwable exception) {
                Logger.debug(TAG, String.format(
                        "Delegated worker %s threw exception in startWork.", className),
                        exception);
                synchronized (mLock) {
                    if (mAreConstraintsUnmet) {
                        Logger.debug(TAG, "Constraints were unmet, Retrying.");
                        setFutureRetry();
                    } else {
                        setFutureFailed();
                    }
                }
            }
        } else {
            Logger.debug(TAG, String.format(
                    "Constraints not met for delegate %s. Requesting retry.", className));
            setFutureRetry();
        }

    }

    // Package-private to avoid synthetic accessor.
    void setFutureFailed() {
        mFuture.set(new Payload(FAILURE, Data.EMPTY));
    }

    // Package-private to avoid synthetic accessor.
    void setFutureRetry() {
        mFuture.set(new Payload(RETRY, Data.EMPTY));
    }

    @Override
    public void onStopped() {
        super.onStopped();
        if (mDelegate != null) {
            // Stop is the method that sets the stopped and cancelled bits and invokes onStopped.
            mDelegate.stop();
        }
    }

    /**
     * @return The instance of {@link WorkDatabase}
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    public WorkDatabase getWorkDatabase() {
        return WorkManagerImpl.getInstance().getWorkDatabase();
    }

    /**
     * @return The {@link Worker} used for delegated work
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    public ListenableWorker getDelegate() {
        return mDelegate;
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        // WorkConstraintTracker notifies on the main thread. So we don't want to trampoline
        // between the background thread and the main thread in this case.
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        // If at any point, constraints are not met mark it so we can retry the work.
        Logger.debug(TAG, String.format("Constraints changed for %s", workSpecIds));
        synchronized (mLock) {
            mAreConstraintsUnmet = true;
        }
    }
}