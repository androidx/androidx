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
package androidx.health.connect.client.records

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the user's route during exercise, e.g. during running or cycling. Each record represents
 * a series of location points.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ExerciseRouteRecord(
  override val startTime: Instant,
  override val startZoneOffset: ZoneOffset?,
  override val endTime: Instant,
  override val endZoneOffset: ZoneOffset?,
  override val samples: List<Location>,
  override val metadata: Metadata = Metadata.EMPTY,
) : SeriesRecord<ExerciseRouteRecord.Location> {

  init {
    require(!startTime.isAfter(endTime)) { "startTime must not be after endTime." }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ExerciseRouteRecord) return false

    if (startTime != other.startTime) return false
    if (startZoneOffset != other.startZoneOffset) return false
    if (endTime != other.endTime) return false
    if (endZoneOffset != other.endZoneOffset) return false
    if (samples != other.samples) return false
    if (metadata != other.metadata) return false

    return true
  }

  override fun hashCode(): Int {
    var result = startTime.hashCode()
    result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
    result = 31 * result + endTime.hashCode()
    result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
    result = 31 * result + samples.hashCode()
    result = 31 * result + metadata.hashCode()
    return result
  }

  /**
   * Represents a single location in an exercise route.
   *
   * @param time The point in time when the measurement was taken.
   * @param latitude Latitude of a location represented as a float, in degrees. Valid
   * range: -180 - 180 degrees.
   * @param longitude Longitude of a location represented as a float, in degrees. Valid
   * range: -180 - 180 degrees.
   * @param horizontalAccuracy The radius of uncertainty for the location, in [Length] unit.
   * @param altitude An altitude of a location represented as a float, in [Length] unit above sea
   * level.
   * @param verticalAccuracy The validity of the altitude values, and their estimated uncertainty,
   * in [Length] unit.
   *
   * @see ExerciseRouteRecord
   */
  public class Location(
    val time: Instant,
    @FloatRange(from = MIN_COORDINATE, to = MAX_COORDINATE) val latitude: Float,
    @FloatRange(from = MIN_COORDINATE, to = MAX_COORDINATE) val longitude: Float,
    val horizontalAccuracy: Length? = null,
    val altitude: Length? = null,
    val verticalAccuracy: Length? = null
  ) {

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Location) return false

      if (time != other.time) return false
      if (latitude != other.latitude) return false
      if (longitude != other.longitude) return false
      if (horizontalAccuracy != other.horizontalAccuracy) return false
      if (altitude != other.altitude) return false
      if (verticalAccuracy != other.verticalAccuracy) return false

      return true
    }

    override fun hashCode(): Int {

      var result = time.hashCode()
      result = 31 * result + latitude.hashCode()
      result = 31 * result + longitude.hashCode()
      result = 31 * result + horizontalAccuracy.hashCode()
      result = 31 * result + altitude.hashCode()
      result = 31 * result + verticalAccuracy.hashCode()
      return result
    }
  }

  private companion object {
    private const val MIN_COORDINATE = -180.0
    private const val MAX_COORDINATE = 180.0
  }
}
