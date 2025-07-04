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

package androidx.health.connect.client.impl.platform.aggregate

import androidx.health.connect.client.records.Record

/**
 * Interface to filter and aggregate records. Response [R] should be one of the aggregation response
 * types defined by the API.
 */
internal interface Aggregator<T : Record, R> {

    /** Filters and aggregates the parts of the record that are relevant for aggregation. */
    fun filterAndAggregate(record: T)

    fun getResult(): R
}
