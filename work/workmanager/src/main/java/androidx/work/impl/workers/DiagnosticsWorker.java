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

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
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
        long startAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        List<WorkSpec> completed = workSpecDao.getRecentlyCompletedWork(startAt);
        List<WorkSpec> running = workSpecDao.getRunningWork();
        List<WorkSpec> enqueued = workSpecDao.getAllEligibleWorkSpecsForScheduling();

        if (completed != null && !completed.isEmpty()) {
            Logger.get().info(TAG, "Recently completed work:\n\n");
            Logger.get().info(TAG, workSpecRows(workNameDao, workTagDao, completed));
        }
        if (running != null && !running.isEmpty()) {
            Logger.get().info(TAG, "Running work:\n\n");
            Logger.get().info(TAG, workSpecRows(workNameDao, workTagDao, running));
        }
        if (enqueued != null && !enqueued.isEmpty()) {
            Logger.get().info(TAG, "Enqueued work:\n\n");
            Logger.get().info(TAG, workSpecRows(workNameDao, workTagDao, enqueued));
        }
        return Result.success();
    }

    @NonNull
    private static String workSpecRows(
            @NonNull WorkNameDao workNameDao,
            @NonNull WorkTagDao workTagDao,
            @NonNull List<WorkSpec> workSpecs) {

        StringBuilder sb = new StringBuilder();
        // Add header
        sb.append(" \n Id\tClass Name\t State\tUnique Name\tTags\t");
        for (WorkSpec workSpec : workSpecs) {
            List<String> names = workNameDao.getNamesForWorkSpecId(workSpec.id);
            List<String> tags = workTagDao.getTagsForWorkSpecId(workSpec.id);
            sb.append(workSpecRow(
                    workSpec,
                    TextUtils.join(",", names),
                    TextUtils.join(",", tags)
            ));
        }
        return sb.toString();
    }

    @NonNull
    private static String workSpecRow(
            @NonNull WorkSpec workSpec,
            @Nullable String name,
            @NonNull String tags) {
        return String.format(
                "\n%s\t %s\t %s\t%s\t%s\t",
                workSpec.id,
                workSpec.workerClassName,
                workSpec.state.name(),
                name,
                tags);
    }
}
