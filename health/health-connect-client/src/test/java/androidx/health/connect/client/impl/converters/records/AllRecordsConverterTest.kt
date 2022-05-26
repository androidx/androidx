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

import androidx.health.connect.client.metadata.DataOrigin
import androidx.health.connect.client.metadata.Device
import androidx.health.connect.client.metadata.Metadata
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ActiveEnergyBurnedRecord
import androidx.health.connect.client.records.ActivityEventRecord
import androidx.health.connect.client.records.ActivityEventRecord.EventType
import androidx.health.connect.client.records.ActivityLapRecord
import androidx.health.connect.client.records.ActivitySessionRecord
import androidx.health.connect.client.records.ActivitySessionRecord.ActivityType
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CervicalMucusRecord.Amount
import androidx.health.connect.client.records.CervicalMucusRecord.Texture
import androidx.health.connect.client.records.CyclingPedalingCadence
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRate
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityDifferentialIndexRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeartRateVariabilitySRecord
import androidx.health.connect.client.records.HeartRateVariabilitySd2Record
import androidx.health.connect.client.records.HeartRateVariabilitySdannRecord
import androidx.health.connect.client.records.HeartRateVariabilitySdnnRecord
import androidx.health.connect.client.records.HeartRateVariabilitySdnnIndexRecord
import androidx.health.connect.client.records.HeartRateVariabilitySdsdRecord
import androidx.health.connect.client.records.HeartRateVariabilityTinnRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HipCircumferenceRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationRecord
import androidx.health.connect.client.records.MenstruationRecord.Flow
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OvulationTestRecord.Result
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Power
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RepetitionsRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.records.SleepStageRecord.StageType
import androidx.health.connect.client.records.Speed
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.StepsCadence
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.SwimmingStrokesRecord
import androidx.health.connect.client.records.SwimmingStrokesRecord.SwimmingType
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalEnergyBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WaistCircumferenceRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
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
        uid = "uid",
        clientId = "clientId",
        clientVersion = 10,
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
                temperatureDegreesCelsius = 1.0,
                measurementLocation = null,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        val dataAllFields =
            BasalBodyTemperatureRecord(
                temperatureDegreesCelsius = 1.0,
                measurementLocation = BodyTemperatureMeasurementLocation.ARMPIT,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(dataOnlyRequired.toProto())).isEqualTo(dataOnlyRequired)
        assertThat(toRecord(dataAllFields.toProto())).isEqualTo(dataAllFields)
    }

    @Test
    fun testBasalMetabolicRate() {
        val data =
            BasalMetabolicRateRecord(
                kcalPerDay = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBloodGlucose() {
        val data =
            BloodGlucoseRecord(
                levelMillimolesPerLiter = 1.0,
                specimenSource = null,
                mealType = null,
                relationToMeal = null,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBloodPressure() {
        val data =
            BloodPressureRecord(
                systolicMillimetersOfMercury = 20.0,
                diastolicMillimetersOfMercury = 10.0,
                bodyPosition = null,
                measurementLocation = null,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBodyFat() {
        val data =
            BodyFatRecord(
                percentage = 1,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBodyTemperature() {
        val data =
            BodyTemperatureRecord(
                temperatureDegreesCelsius = 1.0,
                measurementLocation = null,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBodyWaterMass() {
        val data =
            BodyWaterMassRecord(
                massKg = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBoneMass() {
        val data =
            BoneMassRecord(
                massKg = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testCervicalMucus() {
        val data =
            CervicalMucusRecord(
                texture = Texture.CLEAR,
                amount = Amount.HEAVY,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

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
                        CyclingPedalingCadence(
                            time = START_TIME,
                            revolutionsPerMinute = 1.0,
                        ),
                        CyclingPedalingCadence(
                            time = START_TIME,
                            revolutionsPerMinute = 2.0,
                        ),
                    ),
                metadata = TEST_METADATA,
            )

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
                        HeartRate(
                            time = START_TIME,
                            beatsPerMinute = 100L,
                        ),
                        HeartRate(
                            time = START_TIME,
                            beatsPerMinute = 110L,
                        ),
                        HeartRate(
                            time = START_TIME,
                            beatsPerMinute = 120L,
                        ),
                    ),
                metadata = TEST_METADATA,
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeight() {
        val data =
            HeightRecord(
                heightMeters = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHipCircumference() {
        val data =
            HipCircumferenceRecord(
                circumferenceMeters = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilityDifferentialIndex() {
        val data =
            HeartRateVariabilityDifferentialIndexRecord(
                heartRateVariabilityMillis = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilityRmssd() {
        val data =
            HeartRateVariabilityRmssdRecord(
                heartRateVariabilityMillis = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilityS() {
        val data =
            HeartRateVariabilitySRecord(
                heartRateVariabilityMillis = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilitySd2() {
        val data =
            HeartRateVariabilitySd2Record(
                heartRateVariabilityMillis = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilitySdann() {
        val data =
            HeartRateVariabilitySdannRecord(
                heartRateVariabilityMillis = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilitySdnnIndex() {
        val data =
            HeartRateVariabilitySdnnIndexRecord(
                heartRateVariabilityMillis = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilitySdnn() {
        val data =
            HeartRateVariabilitySdnnRecord(
                heartRateVariabilityMillis = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilitySdsd() {
        val data =
            HeartRateVariabilitySdsdRecord(
                heartRateVariabilityMillis = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRateVariabilityTinn() {
        val data =
            HeartRateVariabilityTinnRecord(
                heartRateVariabilityMillis = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testLeanBodyMass() {
        val data =
            LeanBodyMassRecord(
                massKg = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testMenstruation() {
        val data =
            MenstruationRecord(
                flow = Flow.HEAVY,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testOvulationTest() {
        val data =
            OvulationTestRecord(
                result = Result.NEGATIVE,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testOxygenSaturation() {
        val data =
            OxygenSaturationRecord(
                percentage = 1,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

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
                        Power(
                            time = START_TIME,
                            watts = 1.0,
                        ),
                        Power(
                            time = START_TIME,
                            watts = 2.0,
                        ),
                    ),
                metadata = TEST_METADATA,
            )

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

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSexualActivity() {
        val data =
            SexualActivityRecord(
                protectionUsed = null,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

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
                        Speed(
                            time = START_TIME,
                            metersPerSecond = 1.0,
                        ),
                        Speed(
                            time = START_TIME,
                            metersPerSecond = 2.0,
                        ),
                    ),
                metadata = TEST_METADATA,
            )

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
                        StepsCadence(
                            time = START_TIME,
                            rate = 1.0,
                        ),
                        StepsCadence(
                            time = START_TIME,
                            rate = 2.0,
                        ),
                    ),
                metadata = TEST_METADATA,
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testVo2Max() {
        val data =
            Vo2MaxRecord(
                vo2MillilitersPerMinuteKilogram = 1.0,
                measurementMethod = null,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testWaistCircumference() {
        val data =
            WaistCircumferenceRecord(
                circumferenceMeters = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testWeight() {
        val data =
            WeightRecord(
                weightKg = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testActiveCaloriesBurned() {
        val data =
            ActiveCaloriesBurnedRecord(
                energyKcal = 1.0,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testActiveEnergyBurned() {
        val data =
            ActiveEnergyBurnedRecord(
                energyKcal = 1.0,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testActivityEvent() {
        val data =
            ActivityEventRecord(
                eventType = EventType.PAUSE,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testActivityLap() {
        val data =
            ActivityLapRecord(
                lengthMeters = 1.0,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testActivitySession() {
        val data =
            ActivitySessionRecord(
                activityType = ActivityType.BACK_EXTENSION,
                title = null,
                notes = null,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testDistance() {
        val data =
            DistanceRecord(
                distanceMeters = 1.0,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testElevationGained() {
        val data =
            ElevationGainedRecord(
                elevationMeters = 1.0,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

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

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHydration() {
        val data =
            HydrationRecord(
                volumeLiters = 1.0,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testNutrition() {
        val data =
            NutritionRecord(
                biotinGrams = 1.0,
                caffeineGrams = 1.0,
                calciumGrams = 1.0,
                kcal = 1.0,
                kcalFromFat = 1.0,
                chlorideGrams = 1.0,
                cholesterolGrams = 1.0,
                chromiumGrams = 1.0,
                copperGrams = 1.0,
                dietaryFiberGrams = 1.0,
                folateGrams = 1.0,
                folicAcidGrams = 1.0,
                iodineGrams = 1.0,
                ironGrams = 1.0,
                magnesiumGrams = 1.0,
                manganeseGrams = 1.0,
                molybdenumGrams = 1.0,
                monounsaturatedFatGrams = 1.0,
                niacinGrams = 1.0,
                pantothenicAcidGrams = 1.0,
                phosphorusGrams = 1.0,
                polyunsaturatedFatGrams = 1.0,
                potassiumGrams = 1.0,
                proteinGrams = 1.0,
                riboflavinGrams = 1.0,
                saturatedFatGrams = 1.0,
                seleniumGrams = 1.0,
                sodiumGrams = 1.0,
                sugarGrams = 1.0,
                thiaminGrams = 1.0,
                totalCarbohydrateGrams = 1.0,
                totalFatGrams = 1.0,
                transFatGrams = 1.0,
                unsaturatedFatGrams = 1.0,
                vitaminAGrams = 1.0,
                vitaminB12Grams = 1.0,
                vitaminB6Grams = 1.0,
                vitaminCGrams = 1.0,
                vitaminDGrams = 1.0,
                vitaminEGrams = 1.0,
                vitaminKGrams = 1.0,
                zincGrams = 1.0,
                mealType = null,
                name = null,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testRepetitions() {
        val data =
            RepetitionsRecord(
                count = 1,
                type = ActivityType.JUMPING_JACK,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSleepSession() {
        val data =
            SleepSessionRecord(
                title = null,
                notes = null,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSleepStage() {
        val data =
            SleepStageRecord(
                stage = StageType.AWAKE,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

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

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSwimmingStrokes() {
        val data =
            SwimmingStrokesRecord(
                count = 1,
                type = SwimmingType.BACKSTROKE,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testTotalCaloriesBurned() {
        val data =
            TotalCaloriesBurnedRecord(
                energyKcal = 1.0,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testTotalEnergyBurned() {
        val data =
            TotalEnergyBurnedRecord(
                energyKcal = 1.0,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

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

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }
}
