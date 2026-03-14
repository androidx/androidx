/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.projected

import androidx.xr.runtime.Config
import androidx.xr.runtime.TrackingState

/** Object that holds resources that are used in the XR session. */
internal class XrResources {

    /** The session config. */
    internal var config: Config = Config()

    /** Pose */
    val arDevice: ProjectedArDevice = ProjectedArDevice()

    /** The data of Geospatial */
    val geospatial: ProjectedGeospatial = ProjectedGeospatial(this)

    /** The perception service. */
    lateinit internal var service: IProjectedPerceptionService

    /** The tracking state of the device */
    internal var trackingState: TrackingState = TrackingState.STOPPED

    /** The tracking state of geospatial */
    internal var geospatialTrackingState: TrackingState = TrackingState.STOPPED
}
