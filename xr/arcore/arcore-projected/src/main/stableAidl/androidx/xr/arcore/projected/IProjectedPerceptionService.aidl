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

package androidx.xr.arcore.projected;

import androidx.xr.arcore.projected.IVpsAvailabilityCallback;
import androidx.xr.arcore.projected.ProjectedEarthPose;
import androidx.xr.arcore.projected.ProjectedPose;
import androidx.xr.arcore.projected.ProjectedUpdateResult;
import androidx.xr.arcore.projected.ProjectedConfig;

/**
 * Projected Perception service interface.
 */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IProjectedPerceptionService {

  /** Returns if VPS is available and the status is returned in the callback */
  boolean checkVpsAvailability(
      double latDeg, double lonDeg, IVpsAvailabilityCallback callback) = 0;

  /** Starts a perception session */
  void start(boolean enableVps, String apiKey) = 1;

  /** Stops a perception session */
  void stop() = 2;

  /** Converts a GeospatialPose to a Pose in GL world coordinates. */
  ProjectedPose createPoseFromGeospatialPose(in ProjectedEarthPose geospatialPose) = 3;

  /** Converts a Pose in GL world coordinates to a GeospatialPose. */
  ProjectedEarthPose createGeospatialPoseFromPose(in ProjectedPose pose) = 4;

  /** Gets the device's current GeospatialPose. */
  ProjectedEarthPose createGeospatialPoseFromDevicePose() = 5;

  /** Updates the session and returns tracking states. */
  ProjectedUpdateResult update() = 6;

  /** Starts a perception session with the current configuration. */
  byte startWithConfiguration(in ProjectedConfig config) = 7;
}

