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
package android.arch.background.workmanager;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.support.annotation.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A {@link Processor} that handles execution when the app is in the foreground.
 */

public class ForegroundProcessor extends Processor
        implements Observer<List<String>>, LifecycleObserver {

    // TODO(sumir): Be more intelligent about this.
    private ExecutorService mExecutorService = Executors.newScheduledThreadPool(4);
    private Map<String, Future<?>> mEnqueuedWork = new LinkedHashMap<>();
    private LifecycleOwner mLifecycleOwner;

    public ForegroundProcessor(
            Context appContext,
            WorkDatabase workDatabase,
            LifecycleOwner lifecycleOwner) {
        super(appContext, workDatabase);
        mLifecycleOwner = lifecycleOwner;
        mLifecycleOwner.getLifecycle().addObserver(this);
        mWorkDatabase.workSpecDao().getRunnableWorkIds().observe(mLifecycleOwner, this);
    }

    @Override
    public void onChanged(@Nullable List<String> runnableWorkIds) {
        // TODO(sumir): Optimize this.  Also, do we need to worry about items *removed* from the
        // list or can we safely ignore them as we are doing right now?
        // Note that this query currently gets triggered when items are REMOVED from the runnable
        // status as well.
        if (runnableWorkIds == null) {
            return;
        }

        for (String workId : runnableWorkIds) {
            if (!mEnqueuedWork.containsKey(workId)) {
                process(workId);
            }
        }
    }

    @Override
    public void process(String id) {
        if (isActive()) {
            WorkerWrapper workWrapper = new WorkerWrapper(mAppContext, mWorkDatabase, id, this);
            Future<?> future = mExecutorService.submit(workWrapper);   // TODO(sumir): Delays
            mEnqueuedWork.put(id, future);
        }
    }

    private boolean isActive() {
        return mLifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    @Override
    public void cancel(String id) {
        // TODO(sumir)
    }

    @Override
    public void onExecuted(String workSpecId, int result) {
        mEnqueuedWork.remove(workSpecId);
        // TODO(sumir): Bubble this upstream.
    }

    /**
     * Called when the process lifecycle is considered stopped.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onLifecycleStop() {
        Iterator<Map.Entry<String, Future<?>>> it = mEnqueuedWork.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Future<?>> entry = it.next();
            if (entry.getValue().cancel(false)) {
                it.remove();
            }
        }
    }
}
