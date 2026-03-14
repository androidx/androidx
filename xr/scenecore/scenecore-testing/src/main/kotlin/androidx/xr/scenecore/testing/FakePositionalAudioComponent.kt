/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.testing

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.PositionalAudioComponent

/** Test-only implementation of [PositionalAudioComponent]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakePositionalAudioComponent(
    internal val context: Context,
    public var params: PointSourceParams,
) : FakeComponent(), PositionalAudioComponent {

    public var getAudioOutputProviderCount: Int = 0

    override fun getAudioOutputProvider(): AudioOutputProvider {
        getAudioOutputProviderCount++
        return AudioTrackAudioOutputProvider.Builder(context).build()
    }

    override fun setPointSourceParams(params: PointSourceParams) {
        this.params = params
    }
}
