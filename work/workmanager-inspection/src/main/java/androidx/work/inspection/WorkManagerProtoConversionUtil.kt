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

package androidx.work.inspection

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.WorkInfo

fun WorkInfo.State.toProto(): WorkManagerInspectorProtocol.WorkInfo.State =
    WorkManagerInspectorProtocol.WorkInfo.State
        .forNumber(ordinal + 1)

fun Data.toProto(): WorkManagerInspectorProtocol.Data {
    return WorkManagerInspectorProtocol.Data.newBuilder()
        .addAllEntries(
            keyValueMap.map {
                WorkManagerInspectorProtocol.DataEntry.newBuilder()
                    .setKey(it.key)
                    .setValue(it.value.toString())
                    .build()
            }
        )
        .build()
}

fun Constraints.toProto(): WorkManagerInspectorProtocol.Constraints =
    WorkManagerInspectorProtocol.Constraints.newBuilder()
        .setRequiredNetworkType(
            WorkManagerInspectorProtocol.Constraints.NetworkType
                .forNumber(requiredNetworkType.ordinal + 1)
        )
        .setRequiresCharging(requiresCharging())
        .setRequiresDeviceIdle(requiresDeviceIdle())
        .setRequiresBatteryNotLow(requiresBatteryNotLow())
        .setRequiresStorageNotLow(requiresStorageNotLow())
        .build()

fun StackTraceElement.toProto(): WorkManagerInspectorProtocol.CallStack.Frame =
    WorkManagerInspectorProtocol.CallStack.Frame.newBuilder()
        .setClassName(className)
        .setFileName(fileName ?: "")
        .setMethodName(methodName)
        .setLineNumber(lineNumber)
        .build()
