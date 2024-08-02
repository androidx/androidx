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

package androidx.health.connect.client.testing

import androidx.health.connect.client.ExperimentalHealthConnectApi
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.testing.stubs.Stub

/**
 * Used to override or intercept responses to emulate scenarios that [FakeHealthConnectClient]
 * doesn't support.
 *
 * Every call in [FakeHealthConnectClient] can be overridden.
 *
 * For example:
 *
 * @sample androidx.health.connect.testing.samples.StubResponse
 * @sample androidx.health.connect.testing.samples.StubResponseException
 * @sample androidx.health.connect.testing.samples.AggregationResult
 * @sample androidx.health.connect.testing.samples.AggregationByDurationResult
 * @sample androidx.health.connect.testing.samples.AggregationByPeriodResult
 * @sample androidx.health.connect.testing.samples.AggregationResult
 * @param getChanges A [Stub] used only to throw exceptions in [getChanges].
 * @param getChangesToken A [Stub] used only to throw exceptions in [getChangesToken].
 * @param readRecords A [Stub] used only to throw exceptions in [readRecords].
 * @param readRecord A [Stub] used only to throw exceptions in [readRecord].
 * @param insertRecords A [Stub] used only to throw exceptions in [insertRecords].
 * @param updateRecords A [Stub] used only to throw exceptions in [updateRecords].
 * @param deleteRecords A [Stub] used only to throw exceptions in [deleteRecords].
 * @param aggregate A [Stub] used to set the next responses used in
 *   [FakeHealthConnectClient.aggregate].
 * @param aggregateGroupByDuration A [Stub] used to set the next responses used in
 *   [FakeHealthConnectClient.aggregateGroupByDuration].
 * @param aggregateGroupByPeriod A [Stub] used to set the next responses used in
 *   [FakeHealthConnectClient.aggregateGroupByPeriod].
 */
@ExperimentalHealthConnectApi
public class FakeHealthConnectClientOverrides(
    /*  Changes stubs, only used to throw exceptions */
    public var getChanges: Stub<Nothing?, Nothing>? = null,
    public var getChangesToken: Stub<Nothing?, Nothing>? = null,

    /*  Records stubs, only used to throw exceptions */
    public var readRecords: Stub<Nothing?, Nothing>? = null,
    public var readRecord: Stub<Nothing?, Nothing>? = null,
    public var insertRecords: Stub<Nothing?, Nothing>? = null,
    public var updateRecords: Stub<Nothing?, Nothing>? = null,
    public var deleteRecords: Stub<Nothing?, Nothing>? = null,

    /*  Aggregation stubs */
    /**
     * A [Stub] used to set the next responses used in [FakeHealthConnectClient.aggregate].
     *
     * @sample androidx.health.connect.testing.samples.AggregationResult
     * @see aggregateGroupByDuration
     * @see aggregateGroupByPeriod
     */
    public var aggregate: Stub<AggregateRequest, AggregationResult>? = null,

    /**
     * A [Stub] used to set the next responses used in
     * [FakeHealthConnectClient.aggregateGroupByDuration].
     *
     * @sample androidx.health.connect.testing.samples.AggregationByDurationResult
     * @see aggregate
     * @see aggregateGroupByPeriod
     */
    public var aggregateGroupByDuration:
        Stub<AggregateGroupByDurationRequest, List<AggregationResultGroupedByDuration>>? =
        null,

    /**
     * A [Stub] used to set the next responses used in
     * [FakeHealthConnectClient.aggregateGroupByPeriod].
     *
     * @sample androidx.health.connect.testing.samples.AggregationByPeriodResult
     * @see aggregate
     * @see aggregateGroupByDuration
     */
    public var aggregateGroupByPeriod:
        Stub<AggregateGroupByPeriodRequest, List<AggregationResultGroupedByPeriod>>? =
        null
)
