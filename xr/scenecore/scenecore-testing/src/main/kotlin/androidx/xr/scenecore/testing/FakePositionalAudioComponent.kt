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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.PositionalAudioComponent
import androidx.xr.scenecore.testing.internal.FakePositionalAudioComponent as InternalFakePositionalAudioComponent

/** Test-only implementation of [PositionalAudioComponent]. */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakePositionalAudioComponent
internal constructor(
    internal val context: Context,
    internal val initialParams: PointSourceParams,
    internal var fakeInternal: InternalFakePositionalAudioComponent,
) : FakeComponent(), PositionalAudioComponent {

    public constructor(
        context: Context,
        initialParams: PointSourceParams,
    ) : this(context, initialParams, InternalFakePositionalAudioComponent(context, initialParams))

    public var params: PointSourceParams
        get() = fakeInternal.params
        set(value) {
            fakeInternal.params = value
        }

    public var getAudioOutputProviderCount: Int
        get() = fakeInternal.getAudioOutputProviderCount
        set(value) {
            fakeInternal.getAudioOutputProviderCount = value
        }

    override fun getAudioOutputProvider(): AudioOutputProvider {
        return fakeInternal.getAudioOutputProvider()
    }

    override fun setPointSourceParams(params: PointSourceParams) {
        fakeInternal.setPointSourceParams(params)
    }
}
