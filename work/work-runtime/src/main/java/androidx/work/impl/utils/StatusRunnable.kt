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
@file:JvmName("StatusRunnable")

package androidx.work.impl.utils

import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.executeAsync
import androidx.work.impl.WorkDatabase
import androidx.work.impl.model.WorkSpec.Companion.WORK_INFO_MAPPER
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID

internal fun WorkDatabase.forStringIds(
    executor: TaskExecutor,
    ids: List<String>,
): ListenableFuture<List<WorkInfo>> = loadStatusFuture(executor) { db ->
    WORK_INFO_MAPPER.apply(db.workSpecDao().getWorkStatusPojoForIds(ids))
}

internal fun WorkDatabase.forUUID(
    executor: TaskExecutor,
    id: UUID,
): ListenableFuture<WorkInfo?> = loadStatusFuture(executor) { db ->
    db.workSpecDao().getWorkStatusPojoForId(id.toString())?.toWorkInfo()
}

internal fun WorkDatabase.forTag(
    executor: TaskExecutor,
    tag: String
): ListenableFuture<List<WorkInfo>> = loadStatusFuture(executor) { db ->
    WORK_INFO_MAPPER.apply(db.workSpecDao().getWorkStatusPojoForTag(tag))
}

internal fun WorkDatabase.forUniqueWork(
    executor: TaskExecutor,
    name: String,
): ListenableFuture<List<WorkInfo>> = loadStatusFuture(executor) { db ->
    WORK_INFO_MAPPER.apply(db.workSpecDao().getWorkStatusPojoForName(name))
}

internal fun WorkDatabase.forWorkQuerySpec(
    executor: TaskExecutor,
    querySpec: WorkQuery
): ListenableFuture<List<WorkInfo>> = loadStatusFuture(executor) { db ->
    WORK_INFO_MAPPER.apply(db.rawWorkInfoDao().getWorkInfoPojos(querySpec.toRawQuery()))
}

// it should be rewritten via SuspendToFutureAdapter.launchFuture once it is stable.
private fun <T> WorkDatabase.loadStatusFuture(
    executor: TaskExecutor,
    block: (WorkDatabase) -> T
): ListenableFuture<T> = executor.serialTaskExecutor.executeAsync("loadStatusFuture") {
    block(this@loadStatusFuture)
}
