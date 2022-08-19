/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.services.client.data

import androidx.health.services.client.data.DataType.Companion.ABSOLUTE_ELEVATION
import androidx.health.services.client.data.DataType.Companion.ABSOLUTE_ELEVATION_STATS
import androidx.health.services.client.data.DataType.Companion.ACTIVE_EXERCISE_DURATION_TOTAL
import androidx.health.services.client.data.DataType.Companion.CALORIES
import androidx.health.services.client.data.DataType.Companion.CALORIES_TOTAL
import androidx.health.services.client.data.DataType.Companion.CALORIES_DAILY
import androidx.health.services.client.data.DataType.Companion.DISTANCE_DAILY
import androidx.health.services.client.data.DataType.Companion.FLOORS_DAILY
import androidx.health.services.client.data.DataType.Companion.STEPS_DAILY
import androidx.health.services.client.data.DataType.Companion.FORMAT_BYTE_ARRAY
import androidx.health.services.client.data.DataType.Companion.LOCATION
import androidx.health.services.client.data.DataType.Companion.STEPS
import androidx.health.services.client.data.DataType.Companion.SWIMMING_LAP_COUNT
import androidx.health.services.client.data.DataType.TimeType.Companion.UNKNOWN
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.DataType.TimeType.TIME_TYPE_UNKNOWN
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KVisibility
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

@RunWith(RobolectricTestRunner::class)
internal class DataTypeTest {

    @Test
    fun aggregateProtoRoundTrip() {
        val proto = CALORIES_TOTAL.proto

        val dataType = DataType.aggregateFromProto(proto)

        assertThat(dataType).isEqualTo(CALORIES_TOTAL)
    }

    @Test
    fun deltaProtoRoundTrip() {
        val proto = CALORIES.proto

        val dataType = DataType.deltaFromProto(proto)

        assertThat(dataType).isEqualTo(CALORIES)
    }

    @Test
    fun dailyProtoRoundTrip() {
        val proto = CALORIES_DAILY.proto

        val dataType = DataType.deltaFromProto(proto)

        assertThat(dataType).isEqualTo(CALORIES_DAILY)
    }

    @Test
    fun equalsDelta() {
        assertThat(CALORIES).isEqualTo(CALORIES)
        assertThat(CALORIES).isNotEqualTo(CALORIES_TOTAL)
        assertThat(CALORIES).isNotEqualTo(STEPS)
        assertThat(CALORIES).isNotEqualTo(CALORIES_DAILY)
    }

    @Test
    fun equalsAggregate() {
        assertThat(CALORIES_TOTAL).isEqualTo(CALORIES_TOTAL)
        assertThat(CALORIES_TOTAL).isNotEqualTo(CALORIES)
        assertThat(CALORIES_TOTAL).isNotEqualTo(CALORIES_DAILY)
    }

    @Test
    fun equalsDaily() {
        assertThat(CALORIES_DAILY).isEqualTo(CALORIES_DAILY)
        assertThat(CALORIES_DAILY).isNotEqualTo(CALORIES)
        assertThat(CALORIES_DAILY).isNotEqualTo(CALORIES_TOTAL)
        assertThat(CALORIES_DAILY).isNotEqualTo(STEPS)
    }

    @Test
    fun deltaFromProtoDoesNotThrowExceptionForNewDataType() {
        val proto = DataProto.DataType.newBuilder()
            .setName("some unknown type")
            .setTimeType(TIME_TYPE_UNKNOWN)
            .setFormat(FORMAT_BYTE_ARRAY)
            .build()

        val dataType = DataType.deltaFromProto(proto)

        assertThat(dataType.name).isEqualTo("some unknown type")
        assertThat(dataType.timeType).isEqualTo(UNKNOWN)
    }

    @Test
    fun aggregateFromProtoDoesNotThrowExceptionForNewDataType() {
        val proto = DataProto.DataType.newBuilder()
            .setName("some unknown type")
            .setTimeType(TIME_TYPE_UNKNOWN)
            .setFormat(FORMAT_BYTE_ARRAY)
            .build()

        val dataType = DataType.aggregateFromProto(proto)

        assertThat(dataType.name).isEqualTo("some unknown type")
        assertThat(dataType.timeType).isEqualTo(UNKNOWN)
    }

    @Test
    fun deltaAndAggregateShouldContainBothIfAvailable() {
        // The proto representation of deltas and aggregates is identical.
        val deltaProto = CALORIES_TOTAL.proto

        val list = DataType.deltaAndAggregateFromProto(deltaProto)

        assertThat(list).containsExactly(CALORIES, CALORIES_TOTAL)
    }

    @Test
    fun deltaAndAggregateShouldOnlyContainDeltaIfNoAggregateIsAvailable() {
        // The proto representation of deltas and aggregates is identical, but LOCATION does not
        // have an aggregate version
        val deltaProto = LOCATION.proto

        val list = DataType.deltaAndAggregateFromProto(deltaProto)

        assertThat(list).containsExactly(LOCATION)
    }

    @Test
    fun deltaAndAggregateShouldContainBothForNewDataTypes() {
        val proto = DataProto.DataType.newBuilder()
            .setName("new")
            .setTimeType(TIME_TYPE_UNKNOWN)
            .setFormat(FORMAT_BYTE_ARRAY)
            .build()

        val list = DataType.deltaAndAggregateFromProto(proto)

        val item1 = list[0]
        val item2 = list[1]
        assertThat(item1.name).isEqualTo("new")
        assertThat(item1.timeType).isEqualTo(UNKNOWN)
        assertThat(item1.valueClass).isEqualTo(ByteArray::class)
        assertThat(item1::class).isEqualTo(DeltaDataType::class)
        assertThat(item1.isAggregate).isFalse()
        assertThat(item2.name).isEqualTo("new")
        assertThat(item2.timeType).isEqualTo(UNKNOWN)
        assertThat(item2.valueClass).isEqualTo(ByteArray::class)
        assertThat(item2::class).isEqualTo(AggregateDataType::class)
        assertThat(item2.isAggregate).isTrue()
    }

    @Test
    fun aggregatesShouldContainAllExpectedDeltas() {
        val aggregateNames = DataType.aggregateDataTypes.toMutableSet().apply {
            // Active duration is special cased and does not have a delta form. Developers get the
            // Active duration not from a DataPoint, but instead from from a property in the
            // ExerciseUpdate directly. The DataType is only used to enable setting an ExerciseGoal,
            // which only operate on aggregates. So, we do not have a delta datatype for this and
            // instead only have an aggregate.
            remove(ACTIVE_EXERCISE_DURATION_TOTAL)
        }.map { it.name }
        // Certain deltas are expected to not have aggregates
        val deltaNames = DataType.deltaDataTypes.toMutableSet().apply {
            // Swimming lap count is already aggregated
            remove(SWIMMING_LAP_COUNT)
            // Aggregate location doesn't make a lot of sense
            remove(LOCATION)
            // Dailies are used in passive and passive only deals with deltas
            remove(CALORIES_DAILY)
            remove(DISTANCE_DAILY)
            remove(FLOORS_DAILY)
            remove(STEPS_DAILY)
        }.map { it.name }

        assertThat(aggregateNames).containsExactlyElementsIn(deltaNames)
    }

    @Test
    fun allDataTypesShouldBeInEitherDeltaOrAggregateDataTypeSets() {
        // If this test fails, you haven't added a new DataType to one of the sets below:
        val joinedSet = DataType.deltaDataTypes + DataType.aggregateDataTypes
        // Use reflection get all public data types defined. Reflection is yuck, but not being able
        // to return the same DataType object from a proto is worse.
        val dataTypesThroughReflection = DataType.Companion::class
            .declaredMemberProperties
            .filter { it.visibility == KVisibility.PUBLIC }
            .filter {
                it.javaField != null && DataType::class.java.isAssignableFrom(it.javaField!!.type)
            }
            .map { it.get(DataType.Companion) }

        assertThat(dataTypesThroughReflection).contains(LOCATION)
        assertThat(dataTypesThroughReflection).contains(ABSOLUTE_ELEVATION)
        assertThat(dataTypesThroughReflection).contains(ABSOLUTE_ELEVATION_STATS)
        assertThat(joinedSet).containsExactlyElementsIn(dataTypesThroughReflection)
    }
}