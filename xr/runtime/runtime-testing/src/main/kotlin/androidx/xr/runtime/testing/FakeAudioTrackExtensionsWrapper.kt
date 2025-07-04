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

package androidx.xr.runtime.testing

import android.media.AudioTrack
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.AudioTrackExtensionsWrapper
import androidx.xr.runtime.internal.PointSourceParams
import androidx.xr.runtime.internal.SoundFieldAttributes
import androidx.xr.runtime.internal.SpatializerConstants

// TODO: b/405218432 - Implement this correctly instead of stubbing it out.
/** Test-only implementation of [AudioTrackExtensionsWrapper] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeAudioTrackExtensionsWrapper : AudioTrackExtensionsWrapper {
    override fun getPointSourceParams(track: AudioTrack): PointSourceParams? = null

    override fun getSoundFieldAttributes(track: AudioTrack): SoundFieldAttributes? = null

    @SpatializerConstants.SourceType
    override fun getSpatialSourceType(track: AudioTrack): Int =
        SpatializerConstants.SourceType.SOURCE_TYPE_BYPASS

    override fun setPointSourceParams(track: AudioTrack, params: PointSourceParams) {}

    override fun setPointSourceParams(
        builder: AudioTrack.Builder,
        params: PointSourceParams,
    ): AudioTrack.Builder = builder

    override fun setSoundFieldAttributes(
        builder: AudioTrack.Builder,
        attributes: SoundFieldAttributes,
    ): AudioTrack.Builder = builder
}
