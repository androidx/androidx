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
package androidx.health.data.client.impl.converters.records

import androidx.health.data.client.metadata.DataOrigin
import androidx.health.data.client.metadata.Device
import androidx.health.data.client.metadata.Metadata
import androidx.health.data.client.records.ActiveEnergyBurned
import androidx.health.data.client.records.ActivityEvent
import androidx.health.data.client.records.ActivityEventTypes
import androidx.health.data.client.records.ActivityLap
import androidx.health.data.client.records.ActivitySession
import androidx.health.data.client.records.ActivityTypes
import androidx.health.data.client.records.BasalMetabolicRate
import androidx.health.data.client.records.BloodGlucose
import androidx.health.data.client.records.BloodPressure
import androidx.health.data.client.records.BodyFat
import androidx.health.data.client.records.BodyTemperature
import androidx.health.data.client.records.BoneMass
import androidx.health.data.client.records.CervicalMucus
import androidx.health.data.client.records.CervicalMucusAmounts
import androidx.health.data.client.records.CervicalMucusTextures
import androidx.health.data.client.records.CervicalPosition
import androidx.health.data.client.records.CyclingPedalingCadence
import androidx.health.data.client.records.Distance
import androidx.health.data.client.records.ElevationGained
import androidx.health.data.client.records.FloorsClimbed
import androidx.health.data.client.records.HeartRate
import androidx.health.data.client.records.HeartRateVariabilityDifferentialIndex
import androidx.health.data.client.records.HeartRateVariabilityRmssd
import androidx.health.data.client.records.HeartRateVariabilityS
import androidx.health.data.client.records.HeartRateVariabilitySd2
import androidx.health.data.client.records.HeartRateVariabilitySdann
import androidx.health.data.client.records.HeartRateVariabilitySdnn
import androidx.health.data.client.records.HeartRateVariabilitySdnnIndex
import androidx.health.data.client.records.HeartRateVariabilitySdsd
import androidx.health.data.client.records.HeartRateVariabilityTinn
import androidx.health.data.client.records.Height
import androidx.health.data.client.records.HipCircumference
import androidx.health.data.client.records.Hydration
import androidx.health.data.client.records.LeanBodyMass
import androidx.health.data.client.records.Menstruation
import androidx.health.data.client.records.MenstruationFlows
import androidx.health.data.client.records.Nutrition
import androidx.health.data.client.records.OvulationTest
import androidx.health.data.client.records.OvulationTestResults
import androidx.health.data.client.records.OxygenSaturation
import androidx.health.data.client.records.Power
import androidx.health.data.client.records.Repetitions
import androidx.health.data.client.records.RespiratoryRate
import androidx.health.data.client.records.RestingHeartRate
import androidx.health.data.client.records.SexualActivity
import androidx.health.data.client.records.SleepSession
import androidx.health.data.client.records.SleepStage
import androidx.health.data.client.records.SleepStageTypes
import androidx.health.data.client.records.Speed
import androidx.health.data.client.records.Steps
import androidx.health.data.client.records.StepsCadence
import androidx.health.data.client.records.SwimmingStrokes
import androidx.health.data.client.records.SwimmingTypes
import androidx.health.data.client.records.TotalEnergyBurned
import androidx.health.data.client.records.Vo2Max
import androidx.health.data.client.records.WaistCircumference
import androidx.health.data.client.records.Weight
import androidx.health.data.client.records.WheelchairPushes
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
        device = Device(identifier = "identifier", manufacturer = "manufacturer"),
        lastModifiedTime = END_TIME,
        dataOrigin = DataOrigin(packageName = "appId")
    )

@RunWith(AndroidJUnit4::class)
class AllRecordsConverterTest {
    @Test
    fun testBasalMetabolicRate() {
        val data =
            BasalMetabolicRate(
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
            BloodGlucose(
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
            BloodPressure(
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
            BodyFat(
                percentage = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBodyTemperature() {
        val data =
            BodyTemperature(
                temperatureDegreesCelsius = 1.0,
                measurementLocation = null,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testBoneMass() {
        val data =
            BoneMass(
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
            CervicalMucus(
                texture = CervicalMucusTextures.CLEAR,
                amount = CervicalMucusAmounts.HEAVY,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testCervicalPosition() {
        val data =
            CervicalPosition(
                position = null,
                dilation = null,
                firmness = null,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testCyclingPedalingCadence() {
        val data =
            CyclingPedalingCadence(
                revolutionsPerMinute = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeartRate() {
        val data =
            HeartRate(
                beatsPerMinute = 1,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testHeight() {
        val data =
            Height(
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
            HipCircumference(
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
            HeartRateVariabilityDifferentialIndex(
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
            HeartRateVariabilityRmssd(
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
            HeartRateVariabilityS(
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
            HeartRateVariabilitySd2(
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
            HeartRateVariabilitySdann(
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
            HeartRateVariabilitySdnnIndex(
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
            HeartRateVariabilitySdnn(
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
            HeartRateVariabilitySdsd(
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
            HeartRateVariabilityTinn(
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
            LeanBodyMass(
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
            Menstruation(
                flow = MenstruationFlows.HEAVY,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testOvulationTest() {
        val data =
            OvulationTest(
                result = OvulationTestResults.NEGATIVE,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testOxygenSaturation() {
        val data =
            OxygenSaturation(
                percentage = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testPower() {
        val data =
            Power(
                power = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testRespiratoryRate() {
        val data =
            RespiratoryRate(
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
            RestingHeartRate(
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
            SexualActivity(
                protectionUsed = null,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testSpeed() {
        val data =
            Speed(
                speedMetersPerSecond = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testStepsCadence() {
        val data =
            StepsCadence(
                rate = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testVo2Max() {
        val data =
            Vo2Max(
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
            WaistCircumference(
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
            Weight(
                weightKg = 1.0,
                time = START_TIME,
                zoneOffset = END_ZONE_OFFSET,
                metadata = TEST_METADATA
            )

        assertThat(toRecord(data.toProto())).isEqualTo(data)
    }

    @Test
    fun testActiveEnergyBurned() {
        val data =
            ActiveEnergyBurned(
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
            ActivityEvent(
                eventType = ActivityEventTypes.PAUSE,
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
            ActivityLap(
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
            ActivitySession(
                activityType = ActivityTypes.BACK_EXTENSION,
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
            Distance(
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
            ElevationGained(
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
            FloorsClimbed(
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
            Hydration(
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
            Nutrition(
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
            Repetitions(
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
    fun testSleepSession() {
        val data =
            SleepSession(
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
            SleepStage(
                stage = SleepStageTypes.AWAKE,
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
            Steps(
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
            SwimmingStrokes(
                count = 1,
                type = SwimmingTypes.BACKSTROKE,
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
            TotalEnergyBurned(
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
            WheelchairPushes(
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
