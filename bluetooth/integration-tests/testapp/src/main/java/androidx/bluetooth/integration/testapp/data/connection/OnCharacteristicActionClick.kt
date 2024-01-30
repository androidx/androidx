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

package androidx.bluetooth.integration.testapp.data.connection

import androidx.annotation.IntDef
import androidx.bluetooth.GattCharacteristic

interface OnCharacteristicActionClick {

    companion object {
        const val READ = 0
        const val WRITE = 1
        const val SUBSCRIBE = 2
    }

    @Target(AnnotationTarget.TYPE)
    @IntDef(
        READ,
        WRITE,
        SUBSCRIBE,
    )
    annotation class Action

    fun onClick(
        deviceConnection: DeviceConnection,
        characteristic: GattCharacteristic,
        action: @Action Int
    )
}
