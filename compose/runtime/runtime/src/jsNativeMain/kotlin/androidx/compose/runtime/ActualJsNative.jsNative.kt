/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.SnapshotMutableState
import kotlin.coroutines.cancellation.CancellationException

internal actual fun <T> createSnapshotMutableState(
    value: T,
    policy: SnapshotMutationPolicy<T>
): SnapshotMutableState<T> = SnapshotMutableStateImpl(value, policy)

internal actual fun createSnapshotMutableIntState(
    value: Int
): MutableIntState = SnapshotMutableIntStateImpl(value)

internal actual fun createSnapshotMutableLongState(
    value: Long
): MutableLongState = SnapshotMutableLongStateImpl(value)

internal actual fun createSnapshotMutableFloatState(
    value: Float
): MutableFloatState = SnapshotMutableFloatStateImpl(value)

internal actual fun createSnapshotMutableDoubleState(
    value: Double
): MutableDoubleState = SnapshotMutableDoubleStateImpl(value)

internal actual abstract class PlatformOptimizedCancellationException actual constructor(
    message: String?
) : CancellationException(message)
