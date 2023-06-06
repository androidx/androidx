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

package androidx.core.performance.play.services

import android.content.Context
import androidx.core.performance.DevicePerformance
import androidx.core.performance.DevicePerformanceSupplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Uses Google Play Services to supply media performance class data. */
class PlayServicesDevicePerformanceSupplier : DevicePerformanceSupplier {

    companion object {
        /**
         * Create DevicePerformance from the context backed by StaticDevicePerformanceSupplier.
         *
         * This should be done in [android.app.Application.onCreate].
         */
        @JvmStatic
        fun createDevicePerformance(
            // Real implementations will require a context
            @Suppress("UNUSED_PARAMETER") context: Context
        ): DevicePerformance =
            DevicePerformance.create(PlayServicesDevicePerformanceSupplier())
    }

    override val mediaPerformanceClassFlow: Flow<Int> = flow {
        emit(0)
        // TODO(281079628): implement
    }
}