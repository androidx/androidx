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

package androidx.xr.arcore

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.GeospatialPose

/**
 * Result of a [Earth.createGeospatialPoseFromPose] or [Earth.createGeospatialPoseFromDevicePose]
 * call.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public sealed class CreateGeospatialPoseFromPoseResult

/**
 * Result of a successful [Earth.createGeospatialPoseFromPose] or
 * [Earth.createGeospatialPoseFromDevicePose] call.
 *
 * We define horizontal accuracy as the radius of the 68th percentile confidence level around the
 * estimated horizontal location. In other words, if you draw a circle centered at this
 * GeospatialPose's latitude and longitude, and with a radius equal to the horizontal accuracy, then
 * there is a 68% probability that the true location is inside the circle. Larger numbers indicate
 * lower accuracy.
 *
 * For example, if the latitude is 10°, longitude is 10°, and the returned value is 15, then there
 * is a 68% probability that the true location is within 15 meters of the (10°, 10°)
 * latitude/longitude coordinate.
 *
 * We define vertical accuracy as the radius of the 68th percentile confidence level around the
 * estimated altitude. In other words, there is a 68% probability that the true altitude is within
 * the output value (in meters) of this GeospatialPose's altitude (above or below). Larger numbers
 * indicate lower accuracy.
 *
 * For example, if this GeospatialPose's altitude is 100 meters, and the output value is 20 meters,
 * there is a 68% chance that the true altitude is within 20 meters of 100 meters.
 *
 * Yaw rotation is the angle between the pose's compass direction and north, and can be determined
 * from [eastUpSouthQuaternion].
 *
 * We define yaw accuracy as the estimated radius of the 68th percentile confidence level around yaw
 * angles from [eastUpSouthQuaternion]. In other words, there is a 68% probability that the true yaw
 * angle is within [orientationYawAccuracy] of this GeospatialPose's orientation. Larger values
 * indicate lower accuracy.
 *
 * For example, if the estimated yaw angle is 60°, and the orientation yaw accuracy is 10°, then
 * there is an estimated 68% probability of the true yaw angle being between 50° and 70°.
 *
 * @property pose the [GeospatialPose] that was created.
 * @property horizontalAccuracy the estimated horizontal accuracy in meters with respect to latitude
 *   and longitude.
 * @property verticalAccuracy the estimated altitude accuracy in meters.
 * @property orientationYawAccuracy the estimated orientation yaw angle accuracy.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CreateGeospatialPoseFromPoseSuccess(
    public val pose: GeospatialPose,
    public val horizontalAccuracy: Double,
    public val verticalAccuracy: Double,
    public val orientationYawAccuracy: Double,
) : CreateGeospatialPoseFromPoseResult()

/**
 * Result of an unsuccessful [Earth.createGeospatialPoseFromPose] or
 * [Earth.createGeospatialPoseFromDevicePose] call. Required tracking is not available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CreateGeospatialPoseFromPoseNotTracking() : CreateGeospatialPoseFromPoseResult()

/**
 * Result of an unsuccessful [Earth.createGeospatialPoseFromPose] or
 * [Earth.createGeospatialPoseFromDevicePose] call. The [Earth] encountered an error, such as if
 * Geospatial was not enabled.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CreateGeospatialPoseFromPoseIllegalState() : CreateGeospatialPoseFromPoseResult()
