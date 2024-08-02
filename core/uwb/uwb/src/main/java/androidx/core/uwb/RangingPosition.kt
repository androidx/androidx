/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.uwb

/**
 * Position of a device during ranging.
 *
 * @property distance The line-of-sight distance in meters of the ranging device, or null if not
 *   available.
 * @property azimuth The azimuth angle in degrees of the ranging device, or null if not available.
 *   The range is [-90, 90].
 * @property elevation The elevation angle in degrees of the ranging device, or null if not
 *   available. The range is [-90, 90].
 * @property elapsedRealtimeNanos The elapsed realtime in nanos from when the system booted up to
 *   this position measurement.
 */
public class RangingPosition(
    public val distance: RangingMeasurement?,
    public val azimuth: RangingMeasurement?,
    public val elevation: RangingMeasurement?,
    public val elapsedRealtimeNanos: Long
)
