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

package androidx.xr.scenecore

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.SoundFieldAttributes as RtSoundFieldAttributes
import androidx.xr.runtime.internal.SpatializerConstants as RtSpatializerConstants

/** Configures ambisonics sound sources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SoundFieldAttributes(@SpatializerConstants.AmbisonicsOrder public val order: Int) {

    internal val rtSoundFieldAttributes: RtSoundFieldAttributes

    init {
        val rtOrder =
            when (order) {
                SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER ->
                    RtSpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
                SpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER ->
                    RtSpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER
                SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER ->
                    RtSpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER
                else -> {
                    Log.e(TAG, "Unknown ambisonics order.")
                    order
                }
            }

        rtSoundFieldAttributes = RtSoundFieldAttributes(rtOrder)
    }

    private companion object {
        const val TAG = "SoundFieldAttributes"
    }
}

internal fun RtSoundFieldAttributes.toSoundFieldAttributes(): SoundFieldAttributes {
    return SoundFieldAttributes(this.ambisonicsOrder.ambisonicsOrderToJxr())
}
