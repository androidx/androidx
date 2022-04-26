/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.uwb

/** A data class for ranging result update. */
abstract class RangingResult {
    abstract val device: UwbDevice
}

/** A ranging result with the device position update. */
class RangingResultPosition(
    override val device: UwbDevice,
    val position: RangingPosition
) : RangingResult()

/** A ranging result with peer disconnected status update. */
class RangingResultPeerDisconnected(override val device: UwbDevice) : RangingResult()