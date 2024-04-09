/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.work.impl

import android.content.Context
import androidx.work.impl.background.systemalarm.RescheduleReceiver
import androidx.work.impl.utils.PackageManagerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal fun CoroutineScope.launchUnfinishedWorkListener(appContext: Context, db: WorkDatabase) =
    db.workSpecDao().hasUnfinishedWorkFlow()
        .conflate()
        .distinctUntilChanged()
        .onEach { hasUnfinishedWork ->
            PackageManagerHelper.setComponentEnabled(
                appContext, RescheduleReceiver::class.java, hasUnfinishedWork
            )
        }.launchIn(this)
