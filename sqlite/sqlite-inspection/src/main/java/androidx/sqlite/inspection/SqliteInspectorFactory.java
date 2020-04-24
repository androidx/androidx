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

package androidx.sqlite.inspection;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.inspection.Connection;
import androidx.inspection.InspectorEnvironment;
import androidx.inspection.InspectorFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Factory for SqliteInspector
 */
public final class SqliteInspectorFactory extends InspectorFactory<SqliteInspector> {
    private static final String SQLITE_INSPECTOR_ID = "androidx.sqlite.inspection";
    private final Executor mIOExecutor;
    private final ScheduledExecutorService mScheduledExecutorService;

    @VisibleForTesting
    public SqliteInspectorFactory(@NonNull Executor ioExecutor,
            @NonNull ScheduledExecutorService scheduledExecutorService) {
        super(SQLITE_INSPECTOR_ID);
        mIOExecutor = ioExecutor;
        mScheduledExecutorService = scheduledExecutorService;
    }

    @SuppressWarnings("unused") // called by ServiceLoader
    public SqliteInspectorFactory() {
        this(SqliteInspectionExecutors.ioExecutor(), SqliteInspectionExecutors.scheduledExecutor());
    }

    @NonNull
    @Override
    public SqliteInspector createInspector(@NonNull Connection connection,
            @NonNull InspectorEnvironment environment) {
        return new SqliteInspector(connection, environment, mIOExecutor, mScheduledExecutorService);
    }
}
