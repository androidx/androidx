/*
 * Copyright 2020 The Android Open Source Project
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

import static androidx.work.impl.Scheduler.MAX_GREEDY_SCHEDULER_LIMIT;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.SystemIdInfo;
import androidx.work.impl.model.SystemIdInfoDao;
import androidx.work.impl.model.WorkNameDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.model.WorkTagDao;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The {@link androidx.work.Worker} which dumps diagnostic information.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DiagnosticsWorker extends Worker {

    private static final String TAG = Logger.tagWithPrefix("DiagnosticsWrkr");

    public DiagnosticsWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        WorkManagerImpl workManager = WorkManagerImpl.getInstance(getApplicationContext());
        WorkDatabase database = workManager.getWorkDatabase();
        WorkSpecDao workSpecDao = database.workSpecDao();
        WorkNameDao workNameDao = database.workNameDao();
        WorkTagDao workTagDao = database.workTagDao();
        SystemIdInfoDao systemIdInfoDao = database.systemIdInfoDao();
        long startAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        List<WorkSpec> completed = workSpecDao.getRecentlyCompletedWork(startAt);
        List<WorkSpec> running = workSpecDao.getRunningWork();
        List<WorkSpec> enqueued = workSpecDao.getAllEligibleWorkSpecsForScheduling(
                MAX_GREEDY_SCHEDULER_LIMIT);

        if (completed != null && !completed.isEmpty()) {
            Logger.get().info(TAG, "Recently completed work:\n\n");
            Logger.get().info(TAG,
                    workSpecRows(workNameDao, workTagDao, systemIdInfoDao, completed));
        }
        if (running != null && !running.isEmpty()) {
            Logger.get().info(TAG, "Running work:\n\n");
            Logger.get().info(TAG, workSpecRows(workNameDao, workTagDao, systemIdInfoDao, running));
        }
        if (enqueued != null && !enqueued.isEmpty()) {
            Logger.get().info(TAG, "Enqueued work:\n\n");
            Logger.get().info(TAG,
                    workSpecRows(workNameDao, workTagDao, systemIdInfoDao, enqueued));
        }
        return Result.success();
    }

    @NonNull
    private static String workSpecRows(
            @NonNull WorkNameDao workNameDao,
            @NonNull WorkTagDao workTagDao,
            @NonNull SystemIdInfoDao systemIdInfoDao,
            @NonNull List<WorkSpec> workSpecs) {

        StringBuilder sb = new StringBuilder();
        String systemIdHeader = Build.VERSION.SDK_INT >= 23 ? "Job Id" : "Alarm Id";
        String header = String.format("\n Id \t Class Name\t %s\t State\t Unique Name\t Tags\t",
                systemIdHeader);
        sb.append(header);
        for (WorkSpec workSpec : workSpecs) {
            Integer systemId = null;
            SystemIdInfo info = systemIdInfoDao.getSystemIdInfo(workSpec.id);
            if (info != null) {
                systemId = info.systemId;
            }
            List<String> names = workNameDao.getNamesForWorkSpecId(workSpec.id);
            List<String> tags = workTagDao.getTagsForWorkSpecId(workSpec.id);
            sb.append(workSpecRow(
                    workSpec,
                    TextUtils.join(",", names),
                    systemId,
                    TextUtils.join(",", tags)
            ));
        }
        return sb.toString();
    }

    @NonNull
    private static String workSpecRow(
            @NonNull WorkSpec workSpec,
            @Nullable String name,
            @Nullable Integer systemId,
            @NonNull String tags) {
        return String.format(
                "\n%s\t %s\t %s\t %s\t %s\t %s\t",
                workSpec.id,
                workSpec.workerClassName,
                systemId,
                workSpec.state.name(),
                name,
                tags);
    }
}
