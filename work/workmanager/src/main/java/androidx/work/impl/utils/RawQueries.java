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

package androidx.work.impl.utils;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.work.WorkInfo;
import androidx.work.WorkQuery;
import androidx.work.impl.model.WorkTypeConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A helper to build raw SQLite Queries.
 */
public final class RawQueries {
    private RawQueries() {
        // Does nothing
    }

    /**
     * Converts a {@link WorkQuery} to a raw {@link SupportSQLiteQuery}.
     *
     * @param querySpec The instance of {@link WorkQuery}
     * @return a {@link SupportSQLiteQuery} instance
     */
    @NonNull
    public static SupportSQLiteQuery workQueryToRawQuery(@NonNull WorkQuery querySpec) {
        List<Object> arguments = new ArrayList<>();
        StringBuilder builder = new StringBuilder("SELECT * FROM workspec");
        String conjunction = " WHERE";

        List<WorkInfo.State> states = querySpec.getStates();
        if (!states.isEmpty()) {
            List<Integer> stateIds = new ArrayList<>(states.size());
            for (WorkInfo.State state : states) {
                stateIds.add(WorkTypeConverters.stateToInt(state));
            }
            builder.append(conjunction)
                    .append(" state IN (");
            bindings(builder, stateIds.size());
            builder.append(")");
            arguments.addAll(stateIds);
            conjunction = " AND";
        }

        List<UUID> ids = querySpec.getIds();
        if (!ids.isEmpty()) {
            List<String> workSpecIds = new ArrayList<>(ids.size());
            for (UUID id : ids) {
                workSpecIds.add(id.toString());
            }
            builder.append(conjunction)
                    .append(" id IN (");
            bindings(builder, ids.size());
            builder.append(")");
            arguments.addAll(workSpecIds);
            conjunction = " AND";
        }

        List<String> tags = querySpec.getTags();
        if (!tags.isEmpty()) {
            builder.append(conjunction)
                    .append(" id IN (SELECT work_spec_id FROM worktag WHERE tag IN (");
            bindings(builder, tags.size());
            builder.append("))");
            arguments.addAll(tags);
            conjunction = " AND";
        }

        List<String> uniqueWorkNames = querySpec.getUniqueWorkNames();
        if (!uniqueWorkNames.isEmpty()) {
            builder.append(conjunction)
                    .append(" id IN (SELECT work_spec_id FROM workname WHERE name IN (");
            bindings(builder, uniqueWorkNames.size());
            builder.append("))");
            arguments.addAll(uniqueWorkNames);
            conjunction = " AND";
        }
        builder.append(";");
        return new SimpleSQLiteQuery(builder.toString(), arguments.toArray());
    }

    private static void bindings(@NonNull StringBuilder builder, int count) {
        if (count <= 0) {
            return;
        }
        builder.append("?");
        for (int i = 1; i < count; i++) {
            builder.append(",");
            builder.append("?");
        }
    }
}
