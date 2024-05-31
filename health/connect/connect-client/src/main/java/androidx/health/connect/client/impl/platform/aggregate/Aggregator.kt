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

@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform.aggregate

import androidx.annotation.RequiresApi
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.metadata.DataOrigin

internal interface Aggregator<T> {
    val doubleValues: Map<String, Double>
    val dataOrigins: Set<DataOrigin>

    operator fun plusAssign(value: T)

    fun getResult(): AggregationResult {
        if (dataOrigins.isEmpty()) {
            return emptyAggregationResult()
        }
        return AggregationResult(
            longValues = emptyMap(),
            doubleValues = doubleValues,
            dataOrigins = dataOrigins
        )
    }
}
