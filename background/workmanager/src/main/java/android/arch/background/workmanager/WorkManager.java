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

import static android.arch.background.workmanager.Work.STATUS_BLOCKED;

import android.arch.background.workmanager.foreground.ForegroundProcessor;
import android.arch.background.workmanager.model.Dependency;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.systemjob.SystemJobScheduler;
import android.arch.background.workmanager.utils.BaseWorkHelper;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WorkManager is a class used to enqueue persisted work that is guaranteed to run after its
 * constraints are met.
 */
public final class WorkManager {
    private static final String FIREBASE_SCHEDULER_CLASSNAME =
            "android.arch.background.workmanager.firebase.FirebaseJobScheduler";
    private static final String TAG = "WorkManager";

    private Context mContext;
    private WorkDatabase mWorkDatabase;
    private ExecutorService mEnqueueExecutor = Executors.newSingleThreadExecutor();
    private Scheduler mScheduler;

    private static WorkManager sInstance = null;

    /**
     * Creates/Retrieves the static instance of {@link WorkManager}
     *
     * @param context {@link Context} used to create static instance
     * @return {@link WorkManager} object
     */
    public static synchronized WorkManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WorkManager(context, false);
        }
        return sInstance;
    }

    WorkManager(Context context, boolean useTestDatabase) {
        mContext = context.getApplicationContext();
        mWorkDatabase = WorkDatabase.create(mContext, useTestDatabase);
        mScheduler = createBackgroundScheduler(context);
        new ForegroundProcessor(mContext, mWorkDatabase, mScheduler, ProcessLifecycleOwner.get());
    }

    @Nullable
    private Scheduler createBackgroundScheduler(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            Log.d(TAG, "Created SystemJobScheduler");
            return new SystemJobScheduler(mContext);
        }
        //TODO(sumir): AlarmManagerJobScheduler
        return tryCreateFirebaseJobScheduler(context);
    }

    @Nullable
    private Scheduler tryCreateFirebaseJobScheduler(Context context) {
        Scheduler scheduler = null;
        try {
            Class firebaseSchedulerClass = Class.forName(FIREBASE_SCHEDULER_CLASSNAME);
            scheduler = (Scheduler) firebaseSchedulerClass
                    .getConstructor(Context.class)
                    .newInstance(context);
            Log.d(TAG, "Created FirebaseJobScheduler");
        } catch (Exception e) {
            // Catch all for class cast, invoke, no such method, security exceptions and more.
            // Also thrown if Play Services was not found on device.
            Log.e(TAG, "Could not instantiate FirebaseJobScheduler", e);
        }
        return scheduler;
    }

    /**
     * @return The {@link WorkDatabase} instance associated with this WorkManager.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkDatabase getWorkDatabase() {
        return mWorkDatabase;
    }

    /**
     * @return The {@link Scheduler} associated with this WorkManager based on the device's
     * capabilities, SDK version, etc.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Scheduler getScheduler() {
        return mScheduler;
    }

    /**
     * Gets the {@link BaseWork.WorkStatus} for a given work id.
     *
     * @param id The id of the {@link Work}.
     * @return A {@link LiveData} of the status.
     */
    public LiveData<Integer> getWorkStatus(String id) {
        return mWorkDatabase.workSpecDao().getWorkSpecLiveDataStatus(id);
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param work One or more {@link Work} to enqueue
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input work
     */
    public WorkContinuation enqueue(Work... work) {
        return enqueue(work, null);
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param workBuilders One or more {@link Work.Builder} to enqueue; internally {@code build} is
     *                     called on each of them
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input workBuilders
     */
    public WorkContinuation enqueue(Work.Builder... workBuilders) {
        return enqueue(BaseWorkHelper.convertBuilderArrayToWorkArray(workBuilders), null);
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param workerClasses One or more {@link Worker}s to enqueue; this is a convenience method
     *                      that makes a {@link Work} object with default arguments for each Worker
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input workerClasses
     */
    @SafeVarargs
    public final WorkContinuation enqueue(Class<? extends Worker>... workerClasses) {
        return enqueue(BaseWorkHelper.convertWorkerClassArrayToWorkArray(workerClasses), null);
    }

    /**
     * Enqueues one or more periodic work items for background processing.
     *
     * @param periodicWork One or more {@link PeriodicWork} to enqueue
     */
    public void enqueue(PeriodicWork... periodicWork) {
        mEnqueueExecutor.execute(new EnqueueRunnable(periodicWork, null));
    }

    /**
     * Enqueues one or more periodic work items for background processing.
     *
     * @param periodicWorkBuilders One or more {@link PeriodicWork.Builder} to enqueue; internally
     *                             {@code build} is called on each of them
     */
    public void enqueue(PeriodicWork.Builder... periodicWorkBuilders) {
        mEnqueueExecutor.execute(
                new EnqueueRunnable(
                        BaseWorkHelper.convertBuilderArrayToPeriodicWorkArray(periodicWorkBuilders),
                        null));
    }

    /**
     * Gets a list of work ids for all work that is unfinished for a given tag.
     *
     * @param tag The tag used to identify the work
     * @return A {@link LiveData} list of all the work ids matching this criteria
     */
    public LiveData<List<String>> getAllUnfinishedWorkWithTag(@NonNull final String tag) {
        return mWorkDatabase.workSpecDao().getUnfinishedWorkWithTag(tag);
    }

    /**
     * Gets a list of work ids for all work that is unfinished for a given tag prefix.
     *
     * @param tagPrefix The tag prefix used to identify the work
     * @return A {@link LiveData} list of all the work ids matching this criteria
     */
    public LiveData<List<String>> getAllUnfinishedWorkWithTagPrefix(
            @NonNull final String tagPrefix) {
        return mWorkDatabase.workSpecDao().getUnfinishedWorkWithTagPrefix(tagPrefix);
    }

    /**
     * Clears all work with the given tag prefix, regardless of the current state of the work.
     *
     * @param tagPrefix The tag prefix used to identify the work
     */
    public void clearAllWorkWithTagPrefix(@NonNull final String tagPrefix) {
        mEnqueueExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mWorkDatabase.workSpecDao().clearAllWithTagPrefix(tagPrefix + "%");
            }
        });
    }

    /**
     * Clears all work with the given tag, regardless of the current state of the work.
     *
     * @param tag The tag used to identify the work
     */
    public void clearAllWorkWithTag(@NonNull final String tag) {
        mEnqueueExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mWorkDatabase.workSpecDao().clearAllWithTag(tag);
            }
        });
    }

    /**
     * Clears all work regardless of the current state of the work.  This is dangerous to use if you
     * have multiple modules/libraries that reference WorkManager.  Consider using
     * {@link #clearAllWorkWithTag(String)} or {@link #clearAllWorkWithTagPrefix(String)} instead.
     */
    public void clearAllWork() {
        mEnqueueExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mWorkDatabase.workSpecDao().clearAll();
            }
        });
    }

    WorkContinuation enqueue(Work[] work, String[] prerequisiteIds) {
        WorkContinuation workContinuation = new WorkContinuation(this, work);
        mEnqueueExecutor.execute(new EnqueueRunnable(work, prerequisiteIds));
        return workContinuation;
    }

    /**
     * A Runnable to enqueue a {@link Work} in the database.
     */
    private class EnqueueRunnable implements Runnable {
        private BaseWork[] mWorkArray;
        private String[] mPrerequisiteIds;

        EnqueueRunnable(BaseWork[] workArray, String[] prerequisiteIds) {
            mWorkArray = workArray;
            mPrerequisiteIds = prerequisiteIds;
        }

        @Override
        public void run() {
            mWorkDatabase.beginTransaction();
            try {
                boolean hasPrerequisite = (mPrerequisiteIds != null && mPrerequisiteIds.length > 0);
                for (BaseWork work : mWorkArray) {
                    WorkSpec workSpec = work.getWorkSpec();
                    if (hasPrerequisite) {
                        workSpec.setStatus(STATUS_BLOCKED);
                    }
                    mWorkDatabase.workSpecDao().insertWorkSpec(workSpec);

                    if (hasPrerequisite) {
                        for (String prerequisiteId : mPrerequisiteIds) {
                            Dependency dep = new Dependency(work.getId(), prerequisiteId);
                            mWorkDatabase.dependencyDao().insertDependency(dep);
                        }
                    }
                }
                mWorkDatabase.setTransactionSuccessful();

                // Schedule in the background if there are no prerequisites.  Foreground scheduling
                // happens automatically because we instantiated ForegroundProcessor earlier.
                // TODO(janclarin): Remove mScheduler != null check when Scheduler added for 23-.
                if (mScheduler != null && !hasPrerequisite) {
                    for (BaseWork work : mWorkArray) {
                        mScheduler.schedule(work.getWorkSpec());
                    }
                }
            } finally {
                mWorkDatabase.endTransaction();
            }
        }
    }
}

