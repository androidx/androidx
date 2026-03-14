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

import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.PointSourceParams as RtPointSourceParams

/**
 * Configures a sound source to be spatialized at a 3D location.
 *
 * For more information, see
 * [Add positional audio to your app][https://developer.android.com/develop/xr/jetpack-xr-sdk/add-spatial-audio#add-positional].
 */
// TODO: b/430650745 - reevaluate the usefulness of this class prior to the beta release
// TODO: b/426001209 - add additional parameters to PointSourceParams
public class PointSourceParams() {
    internal val rtPointSourceParams: RtPointSourceParams by lazy { RtPointSourceParams() }
}

internal fun RtPointSourceParams.toPointSourceParams(session: Session): PointSourceParams? {
    return PointSourceParams()
}
