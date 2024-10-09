/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.health.connect.client.impl.platform.records

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.health.connect.datatypes.units.Energy as PlatformEnergy
import android.health.connect.datatypes.units.Length as PlatformLength
import android.health.connect.datatypes.units.Mass as PlatformMass
import android.health.connect.datatypes.units.Power as PlatformPower
import android.health.connect.datatypes.units.Volume as PlatformVolume
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.impl.platform.response.buildAggregationResult
import androidx.health.connect.client.impl.platform.response.getDoubleMetricValues
import androidx.health.connect.client.impl.platform.response.getLongMetricValues
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@SmallTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
// Comment the SDK suppress to run on emulators lower than U.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class ResponseConvertersTest {

    private val tolerance = Correspondence.tolerance(1e-6)

    @Test
    fun buildAggregationResult() {
        val aggregationResult =
            buildAggregationResult(
                metrics =
                    setOf(HeartRateRecord.BPM_MIN, ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                aggregationValueGetter = { aggregationType ->
                    when (aggregationType) {
                        PlatformHeartRateRecord.BPM_MIN -> 53L
                        PlatformExerciseSessionRecord.EXERCISE_DURATION_TOTAL -> 60_000L
                        else -> null
                    }
                },
                platformDataOriginsGetter = { aggregationType ->
                    when (aggregationType) {
                        PlatformHeartRateRecord.BPM_MIN ->
                            setOf(
                                PlatformDataOriginBuilder().setPackageName("HR App1").build(),
                                PlatformDataOriginBuilder().setPackageName("HR App2").build()
                            )
                        PlatformExerciseSessionRecord.EXERCISE_DURATION_TOTAL ->
                            setOf(PlatformDataOriginBuilder().setPackageName("Workout app").build())
                        else -> emptySet()
                    }
                }
            )

        assertThat(aggregationResult[HeartRateRecord.BPM_MIN]).isEqualTo(53L)
        assertThat(aggregationResult[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL])
            .isEqualTo(Duration.ofMinutes(1))
        assertThat(aggregationResult.dataOrigins)
            .containsExactly(
                DataOrigin("HR App1"),
                DataOrigin("HR App2"),
                DataOrigin("Workout app")
            )
    }

    @Test
    fun getLongMetricValues_convertsValueAccurately() {
        val metricValues =
            getLongMetricValues(
                mapOf(
                    HeartRateRecord.BPM_MIN as AggregateMetric<Any> to 53L,
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL as AggregateMetric<Any> to 60_000L
                )
            )
        assertThat(metricValues)
            .containsExactly(
                HeartRateRecord.BPM_MIN.metricKey,
                53L,
                ExerciseSessionRecord.EXERCISE_DURATION_TOTAL.metricKey,
                60_000L
            )
    }

    @Test
    fun getLongMetricValues_ignoresNonLongMetricTypes() {
        val metricValues =
            getLongMetricValues(
                mapOf(
                    NutritionRecord.ENERGY_TOTAL as AggregateMetric<Any> to
                        PlatformEnergy.fromCalories(418_400.0)
                )
            )
        assertThat(metricValues).isEmpty()
    }

    @Test
    fun getDoubleMetricValues_convertsEnergyToKilocalories() {
        val metricValues =
            getDoubleMetricValues(
                mapOf(
                    NutritionRecord.ENERGY_TOTAL as AggregateMetric<Any> to
                        PlatformEnergy.fromCalories(418_400.0)
                )
            )
        assertThat(metricValues)
            .comparingValuesUsing(tolerance)
            .containsExactly(NutritionRecord.ENERGY_TOTAL.metricKey, 418.4)
    }

    @Test
    fun getDoubleMetricValues_convertsLengthToMeters() {
        val metricValues =
            getDoubleMetricValues(
                mapOf(
                    DistanceRecord.DISTANCE_TOTAL as AggregateMetric<Any> to
                        PlatformLength.fromMeters(50.0)
                )
            )
        assertThat(metricValues).containsExactly(DistanceRecord.DISTANCE_TOTAL.metricKey, 50.0)
    }

    @Test
    fun getDoubleMetricValues_convertsMassToGrams() {
        val metricValues =
            getDoubleMetricValues(
                mapOf(
                    NutritionRecord.BIOTIN_TOTAL as AggregateMetric<Any> to
                        PlatformMass.fromGrams(88.0)
                )
            )
        assertThat(metricValues).containsExactly(NutritionRecord.BIOTIN_TOTAL.metricKey, 88.0)
    }

    @Test
    fun getDoubleMetricValue_convertsMassToKilograms() {
        val metricValues =
            getDoubleMetricValues(
                mapOf(
                    WeightRecord.WEIGHT_MAX as AggregateMetric<Any> to
                        PlatformMass.fromGrams(100_000.0)
                )
            )

        assertThat(metricValues).containsExactly(WeightRecord.WEIGHT_MAX.metricKey, 100.0)
    }

    @Test
    fun getDoubleMetricValues_convertsPowerToWatts() {
        val metricValues =
            getDoubleMetricValues(
                mapOf(
                    PowerRecord.POWER_AVG as AggregateMetric<Any> to PlatformPower.fromWatts(366.0)
                )
            )
        assertThat(metricValues).containsExactly(PowerRecord.POWER_AVG.metricKey, 366.0)
    }

    @Test
    fun getDoubleMetricValues_convertsPressureToMillimetersOfMercury() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)
        val metricValues =
            getDoubleMetricValues(
                mapOf(
                    BloodPressureRecord.SYSTOLIC_MAX as AggregateMetric<Any> to
                        PlatformPressure.fromMillimetersOfMercury(120.0)
                )
            )

        assertThat(metricValues).containsExactly(BloodPressureRecord.SYSTOLIC_MAX.metricKey, 120.0)
    }

    @SuppressLint("NewApi") // Api 35 is covered by sdk extension check
    @Test
    fun getDoubleMetricValues_convertsTemperatureDeltaToCelsius() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 13)
        val metricValues =
            getDoubleMetricValues(
                mapOf(
                    SkinTemperatureRecord.TEMPERATURE_DELTA_AVG as AggregateMetric<Any> to
                        PlatformTemperatureDelta.fromCelsius(25.0)
                )
            )
        assertThat(metricValues)
            .containsExactly(SkinTemperatureRecord.TEMPERATURE_DELTA_AVG.metricKey, 25.0)
    }

    @Test
    fun getDoubleMetricValues_convertsVelocityToMetersPerSecond() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)
        val metricValues =
            getDoubleMetricValues(
                mapOf(
                    SpeedRecord.SPEED_AVG as AggregateMetric<Any> to
                        PlatformVelocity.fromMetersPerSecond(2.8)
                )
            )

        assertThat(metricValues).containsExactly(SpeedRecord.SPEED_AVG.metricKey, 2.8)
    }

    @Test
    fun getDoubleMetricValues_convertsVolumeToLiters() {
        val metricValues =
            getDoubleMetricValues(
                mapOf(
                    HydrationRecord.VOLUME_TOTAL as AggregateMetric<Any> to
                        PlatformVolume.fromLiters(1.5)
                )
            )
        assertThat(metricValues).containsExactly(HydrationRecord.VOLUME_TOTAL.metricKey, 1.5)
    }

    @Test
    fun getDoubleMetricValues_ignoresNonDoubleMetricTypes() {
        val metricValues =
            getDoubleMetricValues(mapOf(HeartRateRecord.BPM_MIN as AggregateMetric<Any> to 53L))
        assertThat(metricValues).isEmpty()
    }

    @Test
    fun getLongMetricValues_handlesMultipleMetrics() {
        val metricValues =
            getLongMetricValues(
                mapOf(
                    HeartRateRecord.BPM_MIN as AggregateMetric<Any> to 53L,
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL as AggregateMetric<Any> to
                        60_000L,
                    NutritionRecord.ENERGY_TOTAL as AggregateMetric<Any> to
                        PlatformEnergy.fromCalories(418_400.0),
                    DistanceRecord.DISTANCE_TOTAL as AggregateMetric<Any> to
                        PlatformLength.fromMeters(50.0),
                    NutritionRecord.BIOTIN_TOTAL as AggregateMetric<Any> to
                        PlatformMass.fromGrams(88.0),
                    PowerRecord.POWER_AVG as AggregateMetric<Any> to PlatformPower.fromWatts(366.0),
                    HydrationRecord.VOLUME_TOTAL as AggregateMetric<Any> to
                        PlatformVolume.fromLiters(1.5),
                    FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL as AggregateMetric<Any> to 10L,
                    BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL as AggregateMetric<Any> to
                        PlatformPower.fromWatts(500.0),
                )
            )
        assertThat(metricValues)
            .containsExactly(
                HeartRateRecord.BPM_MIN.metricKey,
                53L,
                ExerciseSessionRecord.EXERCISE_DURATION_TOTAL.metricKey,
                60_000L
            )
    }

    @Test
    fun getDoubleMetricValues_handlesMultipleMetrics() {
        val metricValues =
            getDoubleMetricValues(
                mapOf(
                    HeartRateRecord.BPM_MIN as AggregateMetric<Any> to 53L,
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL as AggregateMetric<Any> to
                        60_000L,
                    NutritionRecord.ENERGY_TOTAL as AggregateMetric<Any> to
                        PlatformEnergy.fromCalories(418_400.0),
                    DistanceRecord.DISTANCE_TOTAL as AggregateMetric<Any> to
                        PlatformLength.fromMeters(50.0),
                    NutritionRecord.BIOTIN_TOTAL as AggregateMetric<Any> to
                        PlatformMass.fromGrams(88.0),
                    PowerRecord.POWER_AVG as AggregateMetric<Any> to PlatformPower.fromWatts(366.0),
                    HydrationRecord.VOLUME_TOTAL as AggregateMetric<Any> to
                        PlatformVolume.fromLiters(1.5),
                    FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL as AggregateMetric<Any> to 10.0,
                    BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL as AggregateMetric<Any> to
                        PlatformEnergy.fromCalories(836_800.0),
                )
            )

        assertThat(metricValues)
            .comparingValuesUsing(tolerance)
            .containsExactly(
                NutritionRecord.ENERGY_TOTAL.metricKey,
                418.4,
                DistanceRecord.DISTANCE_TOTAL.metricKey,
                50.0,
                NutritionRecord.BIOTIN_TOTAL.metricKey,
                88,
                PowerRecord.POWER_AVG.metricKey,
                366.0,
                HydrationRecord.VOLUME_TOTAL.metricKey,
                1.5,
                FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL.metricKey,
                10.0,
                BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL.metricKey,
                836.8
            )
    }
}
