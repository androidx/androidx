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

import androidx.xr.scenecore.runtime.SoundFieldAttributes as RtSoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants as RtSpatializerConstants

/**
 * Configures a sound source to be played as an ambisonics sound field.
 *
 * Ambisonic audio (or ambisonics) is like a skybox for audio, providing an immersive soundscape for
 * your users. Ambisonic sounds must be in AmbiX format in first, second, or third order. For more
 * information about playing ambisonics, see:
 * [Add ambisonic sound fields to your app](https://developer.android.com/develop/xr/jetpack-xr-sdk/add-spatial-audio#ambionics_example).
 *
 * @property order The [SpatializerConstants.AmbisonicsOrder] of the sound to be played.
 */
public class SoundFieldAttributes(public val order: SpatializerConstants.AmbisonicsOrder) {

    internal val rtSoundFieldAttributes: RtSoundFieldAttributes

    init {
        val rtOrder =
            when (order) {
                SpatializerConstants.AmbisonicsOrder.FIRST_ORDER ->
                    RtSpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
                SpatializerConstants.AmbisonicsOrder.SECOND_ORDER ->
                    RtSpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER
                SpatializerConstants.AmbisonicsOrder.THIRD_ORDER ->
                    RtSpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER
                else -> RtSpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
            }

        rtSoundFieldAttributes = RtSoundFieldAttributes(rtOrder)
    }
}

internal fun RtSoundFieldAttributes.toSoundFieldAttributes(): SoundFieldAttributes {
    return SoundFieldAttributes(this.ambisonicsOrder.ambisonicsOrderToJxr())
}
