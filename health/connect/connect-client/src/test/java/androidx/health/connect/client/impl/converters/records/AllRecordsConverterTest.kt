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
package androidx.health.connect.client.impl.converters.records

import androidx.health.connect.client.impl.converters.datatype.toDataTypeName
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BADMINTON
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.records.SleepStageRecord.Companion.STAGE_TYPE_AWAKE
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.celsius
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.kilocaloriesPerDay
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.liters
import androidx.health.connect.client.units.meters
import androidx.health.connect.client.units.metersPerSecond
import androidx.health.connect.client.units.millimetersOfMercury
import androidx.health.connect.client.units.percent
import androidx.health.connect.client.units.watts
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test
import org.junit.runner.RunWith

@SuppressWarnings("GoodTime") // Safe to use in test
private val START_TIME = Instant.ofEpochMilli(1234L)
@SuppressWarnings("GoodTime") // Safe to use in test
private val END_TIME = Instant.ofEpochMilli(5678L)
@SuppressWarnings("GoodTime") // Safe to use in test
private val START_ZONE_OFFSET = ZoneOffset.ofHours(1)
@SuppressWarnings("GoodTime") // Safe to use in test
private val END_ZONE_OFFSET = ZoneOffset.ofHours(2)
private val TEST_METADATA =
    Metadata(
        id = "uid",
        clientRecordId = "clientId",
        clientRecordVersion = 10,
        device = Device(manufacturer = "manufacturer"),
        lastModifiedTime = END_TIME,
        dataOrigin = DataOrigin(packageName = "appId")
    )

// TODO(b/228314623): add tests which set optional fields
@RunWith(AndroidJUnit4::class)
class AllRecordsConverterTest {
    @Test
    fun testBasalBodyTemperature() {
        val dataOnlyRequired =
            BasalBodyTemperatureRecord(
                temperature = 1.celsius,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        val dataAllFields =
            BasalBodyTemperatureRecord(
                temperature = 1.celsius,
                measurementLocation =
                    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_ARMPIT,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(dataOnlyRequired)
        assertThat(toRecord(dataOnlyRequired.toProto())).isEqualTo(dataOnlyRequired)
        assertThat(toRecord(dataAllFields.toProto())).isEqualTo(dataAllFields)
    }

    @Test
    fun testBasalMetabolicRate() {
        val data =
            BasalMetabolicRateRecord(
                basalMetabolicRate = 1.kilocaloriesPerDay,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBloodGlucose() {
        val data =
            BloodGlucoseRecord(
                level = BloodGlucose.millimolesPerLiter(1.0),
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBloodPressure() {
        val data =
            BloodPressureRecord(
                systolic = 20.millimetersOfMercury,
                diastolic = 10.millimetersOfMercury,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBodyFat() {
        val data =
            BodyFatRecord(
                percentage = 1.percent,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBodyTemperature() {
        val data =
            BodyTemperatureRecord(
                temperature = 1.celsius,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBodyWaterMass() {
        val data =
            BodyWaterMassRecord(
                mass = 1.kilograms,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBoneMass() {
        val data =
            BoneMassRecord(
                mass = 1.kilograms,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testCervicalMucus() {
        val data =
            CervicalMucusRecord(
                appearance = CervicalMucusRecord.APPEARANCE_EGG_WHITE,
                sensation = CervicalMucusRecord.SENSATION_HEAVY,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testCyclingPedalingCadenceSeries() {
        val data =
            CyclingPedalingCadenceRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                samples =
                    listOf(
                        CyclingPedalingCadenceRecord.Sample(
                            time = START_TIME,
                            revolutionsPerMinute = 1.0,
                        ),
                        CyclingPedalingCadenceRecord.Sample(
                            time = START_TIME,
                            revolutionsPerMinute = 2.0,
                        ),
                    ),
                metadata = TEST_METADATA,
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateSeries() {
        val data =
            HeartRateRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                samples =
                    listOf(
                        HeartRateRecord.Sample(
                            time = START_TIME,
                            beatsPerMinute = 100L,
                        ),
                        HeartRateRecord.Sample(
                            time = START_TIME,
                            beatsPerMinute = 110L,
                        ),
                        HeartRateRecord.Sample(
                            time = START_TIME,
                            beatsPerMinute = 120L,
                        ),
                    ),
                metadata = TEST_METADATA,
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeight() {
        val data =
            HeightRecord(
                height = 1.meters,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilityRmssd() {
        val data =
            HeartRateVariabilityRmssdRecord(
                heartRateVariabilityMillis = 5.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilityRmssd_adjustValueToRange() {
        val data =
            HeartRateVariabilityRmssdRecord(
                heartRateVariabilityMillis = HeartRateVariabilityRmssdRecord.MIN_HRV_RMSSD,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        val dataProto =
            data
                .toProto()
                .toBuilder()
                .apply { putValues("heartRateVariability", doubleVal(0.5)) }
                .build()

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(dataProto)).isEqualTo(data)
    }

    @Test
    fun testIntermenstrualBleeding() {
        val data =
            IntermenstrualBleedingRecord(
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testLeanBodyMass() {
        val data =
            LeanBodyMassRecord(
                mass = 1.kilograms,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testMenstruation() {
        val data =
            MenstruationFlowRecord(
                flow = MenstruationFlowRecord.FLOW_HEAVY,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testMenstruationPeriod() {
        val data =
            MenstruationPeriodRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testOvulationTest() {
        val data =
            OvulationTestRecord(
                result = OvulationTestRecord.RESULT_NEGATIVE,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testOxygenSaturation() {
        val data =
            OxygenSaturationRecord(
                percentage = 1.percent,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testPowerSeries() {
        val data =
            PowerRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                samples =
                    listOf(
                        PowerRecord.Sample(
                            time = START_TIME,
                            power = 1.watts,
                        ),
                        PowerRecord.Sample(
                            time = START_TIME,
                            power = 2.watts,
                        ),
                    ),
                metadata = TEST_METADATA,
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testRespiratoryRate() {
        val data =
            RespiratoryRateRecord(
                rate = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testRestingHeartRate() {
        val data =
            RestingHeartRateRecord(
                beatsPerMinute = 1,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSexualActivity() {
        val data =
            SexualActivityRecord(
                protectionUsed = SexualActivityRecord.PROTECTION_USED_PROTECTED,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSpeedSeries() {
        val data =
            SpeedRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                samples =
                    listOf(
                        SpeedRecord.Sample(
                            time = START_TIME,
                            speed = 1.metersPerSecond,
                        ),
                        SpeedRecord.Sample(
                            time = START_TIME,
                            speed = 2.metersPerSecond,
                        ),
                    ),
                metadata = TEST_METADATA,
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testStepsCadenceSeries() {
        val data =
            StepsCadenceRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                samples =
                    listOf(
                        StepsCadenceRecord.Sample(
                            time = START_TIME,
                            rate = 1.0,
                        ),
                        StepsCadenceRecord.Sample(
                            time = START_TIME,
                            rate = 2.0,
                        ),
                    ),
                metadata = TEST_METADATA,
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testVo2Max() {
        val data =
            Vo2MaxRecord(
                vo2MillilitersPerMinuteKilogram = 1.0,
                measurementMethod = Vo2MaxRecord.MEASUREMENT_METHOD_COOPER_TEST,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testWeight() {
        val data =
            WeightRecord(
                weight = 1.kilograms,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testActiveCaloriesBurned() {
        val data =
            ActiveCaloriesBurnedRecord(
                energy = 1.kilocalories,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testActivitySession() {
        val data =
            ExerciseSessionRecord(
                exerciseType = EXERCISE_TYPE_BADMINTON,
                title = null,
                notes = null,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testDistance() {
        val data =
            DistanceRecord(
                distance = 1.meters,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testElevationGained() {
        val data =
            ElevationGainedRecord(
                elevation = 1.meters,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testFloorsClimbed() {
        val data =
            FloorsClimbedRecord(
                floors = 1.0,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHydration() {
        val data =
            HydrationRecord(
                volume = 1.liters,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testNutrition() {
        val data =
            NutritionRecord(
                biotin = 1.grams,
                caffeine = 1.grams,
                calcium = 1.grams,
                energy = 1.kilocalories,
                energyFromFat = 1.kilocalories,
                chloride = 1.grams,
                cholesterol = 1.grams,
                chromium = 1.grams,
                copper = 1.grams,
                dietaryFiber = 1.grams,
                folate = 1.grams,
                folicAcid = 1.grams,
                iodine = 1.grams,
                iron = 1.grams,
                magnesium = 1.grams,
                manganese = 1.grams,
                molybdenum = 1.grams,
                monounsaturatedFat = 1.grams,
                niacin = 1.grams,
                pantothenicAcid = 1.grams,
                phosphorus = 1.grams,
                polyunsaturatedFat = 1.grams,
                potassium = 1.grams,
                protein = 1.grams,
                riboflavin = 1.grams,
                saturatedFat = 1.grams,
                selenium = 1.grams,
                sodium = 1.grams,
                sugar = 1.grams,
                thiamin = 1.grams,
                totalCarbohydrate = 1.grams,
                totalFat = 1.grams,
                transFat = 1.grams,
                unsaturatedFat = 1.grams,
                vitaminA = 1.grams,
                vitaminB12 = 1.grams,
                vitaminB6 = 1.grams,
                vitaminC = 1.grams,
                vitaminD = 1.grams,
                vitaminE = 1.grams,
                vitaminK = 1.grams,
                zinc = 1.grams,
                mealType = MealType.MEAL_TYPE_BREAKFAST,
                name = null,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSleepSession() {
        val data =
            SleepSessionRecord(
                title = "title",
                notes = "notes",
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA,
                stages = listOf(
                    SleepSessionRecord.Stage(
                        startTime = Instant.ofEpochMilli(1234L),
                        endTime = Instant.ofEpochMilli(1236L),
                        stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                    )
                )
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSleepSessionWithEmptyStageList() {
        val data =
            SleepSessionRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSleepStage() {
        val data =
            SleepStageRecord(
                stage = STAGE_TYPE_AWAKE,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSteps() {
        val data =
            StepsRecord(
                count = 1,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testTotalCaloriesBurned() {
        val data =
            TotalCaloriesBurnedRecord(
                energy = 1.kilocalories,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testWheelchairPushes() {
        val data =
            WheelchairPushesRecord(
                count = 1,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        checkProtoAndRecordTypeNameMatch(data)
        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    private inline fun <reified T : Record> checkProtoAndRecordTypeNameMatch(record: T) {
        val serializedTypeName = record.toProto().dataType.name

        assertThat(T::class.toDataTypeName()).isEqualTo(serializedTypeName)
    }
}
