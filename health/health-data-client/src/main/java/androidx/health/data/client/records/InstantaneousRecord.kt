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
package androidx.health.data.client.records

import java.time.Instant
import java.time.ZoneOffset

/**
 * A record that contains an instantaneous measurement.
 *
 * @see IntervalRecord for records with measurement of a time interval.
 */
@PublishedApi
internal interface InstantaneousRecord : Record {
    /** Time the record happened. */
    public val time: Instant
    /**
     * User experienced zone offset at [time], or null if unknown. Providing these will help history
     * aggregations results stay consistent should user travel.
     */
    public val zoneOffset: ZoneOffset?
}
