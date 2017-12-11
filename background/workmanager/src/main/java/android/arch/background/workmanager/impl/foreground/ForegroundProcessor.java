/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.arch.background.workmanager.impl.foreground;

import static android.arch.background.workmanager.impl.BaseWork.STATUS_ENQUEUED;

import android.arch.background.workmanager.Constraints;
import android.arch.background.workmanager.impl.Processor;
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.impl.WorkDatabase;
import android.arch.background.workmanager.impl.constraints.ConstraintsMetCallback;
import android.arch.background.workmanager.impl.constraints.ConstraintsTracker;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.utils.LiveDataUtils;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A {@link Processor} that handles execution when the app is in the foreground.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ForegroundProcessor extends Processor
        implements Observer<List<WorkSpec>>, LifecycleObserver, ConstraintsMetCallback {

    private static final String TAG = "ForegroundProcessor";

    private LifecycleOwner mLifecycleOwner;
    private ConstraintsTracker mConstraintsTracker;

    public ForegroundProcessor(
            Context appContext,
            WorkDatabase workDatabase,
            Scheduler scheduler,
            LifecycleOwner lifecycleOwner) {
        // TODO(sumir): Be more intelligent about the executor.
        this(
                appContext,
                workDatabase,
                scheduler,
                lifecycleOwner,
                Executors.newFixedThreadPool(4));

    }

    @VisibleForTesting
    ForegroundProcessor(
            Context appContext,
            WorkDatabase workDatabase,
            Scheduler scheduler,
            LifecycleOwner lifecycleOwner,
            ExecutorService executorService) {
        super(appContext, workDatabase, scheduler, executorService);
        mLifecycleOwner = lifecycleOwner;
        mLifecycleOwner.getLifecycle().addObserver(this);
        mConstraintsTracker = new ConstraintsTracker(mAppContext, this);
        LiveDataUtils.dedupedLiveDataFor(
                mWorkDatabase.workSpecDao().getForegroundEligibleWorkSpecs())
                .observe(mLifecycleOwner, this);
    }

    private boolean isActive() {
        return mLifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    @Override
    public void process(String id) {
        Log.d(TAG, "Trying to process " + id);
        if (isActive()) {
            super.process(id);
        } else {
            Log.d(TAG, "Inactive lifecycle; not processing " + id);
        }
    }

    @Override
    public void onChanged(List<WorkSpec> workSpecs) {
        // TODO(sumir): Optimize this.  Also, do we need to worry about items *removed* from the
        // list or can we safely ignore them as we are doing right now?
        // Note that this query currently gets triggered when items are REMOVED from the runnable
        // status as well.
        Log.d(TAG, "Enqueued WorkSpecs updated. Size : " + workSpecs.size());
        for (WorkSpec workSpec : workSpecs) {
            if (workSpec.getStatus() == STATUS_ENQUEUED
                    && Constraints.NONE.equals(workSpec.getConstraints())) {
                Log.d(TAG, workSpec + " can be processed immediately");
                process(workSpec.getId());
            }
        }
        // ConstraintsTracker will only consider WorkSpecs which have constraints that it can
        // monitor. The rest will be ignored.
        mConstraintsTracker.replace(workSpecs);
    }

    /**
     * Called when the process lifecycle is considered stopped.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onLifecycleStop() {
        Log.d(TAG, "onLifecycleStop");
        mConstraintsTracker.reset();
        Iterator<Map.Entry<String, Future<?>>> it = mEnqueuedWorkMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Future<?>> entry = it.next();
            if (entry.getValue().cancel(false)) {
                it.remove();
                Log.d(TAG, "Canceled " + entry.getKey());
            } else {
                Log.d(TAG, "Cannot cancel " + entry.getKey());
            }
        }
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            process(workSpecId);
        }
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            cancel(workSpecId, true);
        }
    }
}
