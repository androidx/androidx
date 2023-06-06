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

package androidx.core.performance.testing

import androidx.core.performance.DevicePerformanceSupplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A DevicePerformanceSupplier that immediately emits the `mediaPerformanceClass` provided and
 * completes the flow.
 *
 * @param mediaPerformanceClass The media performance class value to emit.
 */
class FakeDevicePerformanceSupplier(private val mediaPerformanceClass: Int) :
    DevicePerformanceSupplier {
    override val mediaPerformanceClassFlow: Flow<Int> = flow {
        emit(mediaPerformanceClass)
    }
}