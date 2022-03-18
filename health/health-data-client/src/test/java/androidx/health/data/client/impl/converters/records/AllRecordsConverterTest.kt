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
                bmr = 1.0,
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
                level = 1.0,
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
                systolic = 20.0,
                diastolic = 10.0,
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
                temperature = 1.0,
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
                rpm = 1.0,
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
                bpm = 1,
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
                heartRateVariability = 1.0,
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
                heartRateVariability = 1.0,
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
                heartRateVariability = 1.0,
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
                heartRateVariability = 1.0,
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
                heartRateVariability = 1.0,
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
                heartRateVariability = 1.0,
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
                heartRateVariability = 1.0,
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
                heartRateVariability = 1.0,
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
                heartRateVariability = 1.0,
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
                bpm = 1,
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
                speed = 1.0,
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
                vo2 = 1.0,
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
                energy = 1.0,
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
                volume = 1.0,
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
                biotin = 1.0,
                caffeine = 1.0,
                calcium = 1.0,
                calories = 1.0,
                caloriesFromFat = 1.0,
                chloride = 1.0,
                cholesterol = 1.0,
                chromium = 1.0,
                copper = 1.0,
                dietaryFiber = 1.0,
                folate = 1.0,
                folicAcid = 1.0,
                iodine = 1.0,
                iron = 1.0,
                magnesium = 1.0,
                manganese = 1.0,
                molybdenum = 1.0,
                monounsaturatedFat = 1.0,
                niacin = 1.0,
                pantothenicAcid = 1.0,
                phosphorus = 1.0,
                polyunsaturatedFat = 1.0,
                potassium = 1.0,
                protein = 1.0,
                riboflavin = 1.0,
                saturatedFat = 1.0,
                selenium = 1.0,
                sodium = 1.0,
                sugar = 1.0,
                thiamin = 1.0,
                totalCarbohydrate = 1.0,
                totalFat = 1.0,
                transFat = 1.0,
                unsaturatedFat = 1.0,
                vitaminA = 1.0,
                vitaminB12 = 1.0,
                vitaminB6 = 1.0,
                vitaminC = 1.0,
                vitaminD = 1.0,
                vitaminE = 1.0,
                vitaminK = 1.0,
                zinc = 1.0,
                mealType = null,
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
                energy = 1.0,
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
