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

package androidx.xr.scenecore.projected.testing

import androidx.xr.scenecore.projected.ISpatialStateChangedCallback
import androidx.xr.scenecore.projected.SpatialState

/**
 * A fake implementation of [ISpatialStateChangedCallback] for use in tests.
 *
 * This class records the spatial states that have been received via the callback, allowing tests to
 * verify that the callback was triggered correctly by the service.
 */
public class FakeSpatialStateChangedCallback : ISpatialStateChangedCallback.Stub() {

    // A list to track all spatial states received.
    public val receivedSpatialStates: MutableList<SpatialState> = mutableListOf()

    override fun onSpatialStateChanged(spatialState: SpatialState) {
        receivedSpatialStates.add(spatialState)
    }

    override fun getInterfaceVersion(): Int {
        return 0
    }
}
