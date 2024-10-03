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

package androidx.health.connect.client.impl.platform.records

import android.annotation.SuppressLint
import android.os.Build
import androidx.health.connect.client.RECORD_CLASSES
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
import androidx.health.connect.client.records.ExerciseCompletionGoal
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExercisePerformanceTarget
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.InstantaneousRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.IntervalRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PlannedExerciseBlock
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.isAtLeastSdkExtension13
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
import androidx.health.connect.client.units.Velocity
import androidx.health.connect.client.units.Volume
import androidx.health.connect.client.units.celsius
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.collect.Iterables
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@SmallTest
// Comment the SDK suppress to run on emulators lower than U.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class RecordConvertersTest {

    private val tolerance = 1.0e-9

    @Test
    fun toPlatformRecordClass_supportsAllRecordTypes() {
        RECORD_CLASSES.forEach { assertThat(it.toPlatformRecordClass()).isNotNull() }
    }

    @Test
    fun stepsRecordClass_convertToPlatform() {
        val stepsSdkClass = StepsRecord::class
        val stepsPlatformClass = PlatformStepsRecord::class.java
        assertThat(stepsSdkClass.toPlatformRecordClass()).isEqualTo(stepsPlatformClass)
    }

    @Test
    fun activeCaloriesBurnedRecord_convertToPlatform() {
        val platformActiveCaloriesBurned =
            ActiveCaloriesBurnedRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    energy = Energy.calories(200.0),
                )
                .toPlatformRecord() as PlatformActiveCaloriesBurnedRecord

        assertPlatformRecord(platformActiveCaloriesBurned) {
            assertThat(energy).isEqualTo(PlatformEnergy.fromCalories(200.0))
        }
    }

    @Test
    fun basalBodyTemperatureRecord_convertToPlatform() {
        val platformBasalBodyTemperature =
            BasalBodyTemperatureRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    temperature = Temperature.celsius(37.0),
                    measurementLocation =
                        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FINGER
                )
                .toPlatformRecord() as PlatformBasalBodyTemperatureRecord

        assertPlatformRecord(platformBasalBodyTemperature) {
            assertThat(temperature).isEqualTo(PlatformTemperature.fromCelsius(37.0))
            assertThat(measurementLocation)
                .isEqualTo(PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FINGER)
        }
    }

    @Test
    fun basalMetabolicRateRecord_convertToPlatform() {
        val platformBasalMetabolicRate =
            BasalMetabolicRateRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    basalMetabolicRate = Power.watts(300.0),
                )
                .toPlatformRecord() as PlatformBasalMetabolicRateRecord

        assertPlatformRecord(platformBasalMetabolicRate) {
            assertThat(basalMetabolicRate).isEqualTo(PlatformPower.fromWatts(300.0))
        }
    }

    @Test
    fun bloodGlucoseRecord_convertToPlatform() {
        val platformBloodGlucose =
            BloodGlucoseRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    level = BloodGlucose.millimolesPerLiter(34.0),
                    specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_TEARS,
                    mealType = MealType.MEAL_TYPE_BREAKFAST,
                    relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL,
                )
                .toPlatformRecord() as PlatformBloodGlucoseRecord

        assertPlatformRecord(platformBloodGlucose) {
            assertThat(level).isEqualTo(PlatformBloodGlucose.fromMillimolesPerLiter(34.0))
            assertThat(specimenSource)
                .isEqualTo(PlatformBloodGlucoseSpecimenSource.SPECIMEN_SOURCE_TEARS)
            assertThat(mealType).isEqualTo(PlatformMealType.MEAL_TYPE_BREAKFAST)
            assertThat(relationToMeal)
                .isEqualTo(PlatformBloodGlucoseRelationToMealType.RELATION_TO_MEAL_AFTER_MEAL)
        }
    }

    @Test
    fun bloodPressureRecord_convertToPlatform() {
        val platformBloodPressure =
            BloodPressureRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    systolic = Pressure.millimetersOfMercury(23.0),
                    diastolic = Pressure.millimetersOfMercury(24.0),
                    bodyPosition = BloodPressureRecord.BODY_POSITION_STANDING_UP,
                    measurementLocation = BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_WRIST,
                )
                .toPlatformRecord() as PlatformBloodPressureRecord

        assertPlatformRecord(platformBloodPressure) {
            assertThat(systolic).isEqualTo(PlatformPressure.fromMillimetersOfMercury(23.0))
            assertThat(diastolic).isEqualTo(PlatformPressure.fromMillimetersOfMercury(24.0))
            assertThat(bodyPosition)
                .isEqualTo(PlatformBloodPressureBodyPosition.BODY_POSITION_STANDING_UP)
            assertThat(measurementLocation)
                .isEqualTo(
                    PlatformBloodPressureMeasurementLocation
                        .BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST
                )
        }
    }

    @Test
    fun bodyFatRecord_convertToPlatform() {
        val platformBodyFat =
            BodyFatRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    percentage = Percentage(99.0),
                )
                .toPlatformRecord() as PlatformBodyFatRecord

        assertPlatformRecord(platformBodyFat) {
            assertThat(percentage).isEqualTo(PlatformPercentage.fromValue(99.0))
        }
    }

    @Test
    fun bodyTemperatureRecord_convertToPlatform() {
        val platformBodyTemperature =
            BodyTemperatureRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    temperature = Temperature.celsius(30.0),
                    measurementLocation =
                        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_ARMPIT,
                )
                .toPlatformRecord() as PlatformBodyTemperatureRecord

        assertPlatformRecord(platformBodyTemperature) {
            PlatformTemperature.fromCelsius(30.0)
            assertThat(measurementLocation)
                .isEqualTo(PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_ARMPIT)
        }
    }

    @Test
    fun bodyWaterMassRecord_convertToPlatform() {
        val platformBodyWaterMass =
            BodyWaterMassRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    mass = Mass.grams(40.0),
                )
                .toPlatformRecord() as PlatformBodyWaterMassRecord

        assertPlatformRecord(platformBodyWaterMass) {
            assertThat(bodyWaterMass).isEqualTo(PlatformMass.fromGrams(40.0))
        }
    }

    @Test
    fun boneMassRecord_convertToPlatform() {
        val platformBoneMass =
            BoneMassRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    mass = Mass.grams(5.0),
                )
                .toPlatformRecord() as PlatformBoneMassRecord

        assertPlatformRecord(platformBoneMass) {
            assertThat(mass).isEqualTo(PlatformMass.fromGrams(5.0))
        }
    }

    @Test
    fun cervicalMucusRecord_convertToPlatform() {
        val platformCervicalMucus =
            CervicalMucusRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    appearance = CervicalMucusRecord.APPEARANCE_CREAMY,
                    sensation = CervicalMucusRecord.SENSATION_LIGHT,
                )
                .toPlatformRecord() as PlatformCervicalMucusRecord

        assertPlatformRecord(platformCervicalMucus) {
            assertThat(appearance).isEqualTo(PlatformCervicalMucusAppearance.APPEARANCE_CREAMY)
            assertThat(sensation).isEqualTo(PlatformCervicalMucusSensation.SENSATION_LIGHT)
        }
    }

    @Test
    fun cyclingPedalingCadenceRecord_convertToPlatform() {
        val platformCyclingPedalingCadence =
            CyclingPedalingCadenceRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    samples =
                        listOf(
                            CyclingPedalingCadenceRecord.Sample(START_TIME, 3.0),
                            CyclingPedalingCadenceRecord.Sample(END_TIME, 9.0)
                        ),
                )
                .toPlatformRecord() as PlatformCyclingPedalingCadenceRecord

        assertPlatformRecord(platformCyclingPedalingCadence) {
            assertThat(samples)
                .comparingElementsUsing(
                    Correspondence.from<
                        PlatformCyclingPedalingCadenceSample,
                        PlatformCyclingPedalingCadenceSample
                    >(
                        { actual, expected ->
                            actual!!.revolutionsPerMinute == expected!!.revolutionsPerMinute &&
                                actual.time == expected.time
                        },
                        "has same RPM and same time as"
                    )
                )
                .containsExactly(
                    PlatformCyclingPedalingCadenceSample(3.0, START_TIME),
                    PlatformCyclingPedalingCadenceSample(9.0, END_TIME)
                )
        }
    }

    @Test
    fun distanceRecord_convertToPlatform() {
        val platformDistance =
            DistanceRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    distance = Length.meters(50.0),
                )
                .toPlatformRecord() as PlatformDistanceRecord

        assertPlatformRecord(platformDistance) {
            assertThat(distance).isEqualTo(PlatformLength.fromMeters(50.0))
        }
    }

    @Test
    fun elevationGainedRecord_convertToPlatform() {
        val platformElevationGained =
            ElevationGainedRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    elevation = Length.meters(10.0),
                )
                .toPlatformRecord() as PlatformElevationGainedRecord

        assertPlatformRecord(platformElevationGained) {
            assertThat(elevation).isEqualTo(PlatformLength.fromMeters(10.0))
        }
    }

    @Test
    fun exerciseSessionRecord_convertToPlatform() {
        val platformExerciseSession =
            ExerciseSessionRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    exerciseType =
                        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                    title = "HIIT training",
                    notes = "Hard workout",
                    laps =
                        listOf(
                            ExerciseLap(
                                START_TIME.plusMillis(6),
                                START_TIME.plusMillis(10),
                                Length.meters(1.0)
                            ),
                            ExerciseLap(
                                START_TIME.plusMillis(11),
                                START_TIME.plusMillis(15),
                                Length.meters(1.5)
                            )
                        ),
                    segments =
                        listOf(
                            ExerciseSegment(
                                START_TIME.plusMillis(1),
                                START_TIME.plusMillis(10),
                                ExerciseSegment.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
                                10
                            )
                        ),
                    exerciseRoute =
                        ExerciseRoute(
                            listOf(
                                ExerciseRoute.Location(
                                    START_TIME,
                                    latitude = 23.5,
                                    longitude = -23.6,
                                    altitude = Length.meters(20.0),
                                    horizontalAccuracy = Length.meters(2.0),
                                    verticalAccuracy = Length.meters(3.0)
                                )
                            )
                        )
                )
                .toPlatformRecord() as PlatformExerciseSessionRecord

        assertPlatformRecord(platformExerciseSession) {
            assertThat(title).isEqualTo("HIIT training")
            assertThat(notes).isEqualTo("Hard workout")
            assertThat(exerciseType)
                .isEqualTo(
                    PlatformExerciseSessionType
                        .EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
                )
            assertThat(laps)
                .containsExactly(
                    PlatformExerciseLapBuilder(START_TIME.plusMillis(6), START_TIME.plusMillis(10))
                        .setLength(PlatformLength.fromMeters(1.0))
                        .build(),
                    PlatformExerciseLapBuilder(START_TIME.plusMillis(11), START_TIME.plusMillis(15))
                        .setLength(PlatformLength.fromMeters(1.5))
                        .build()
                )
            assertThat(segments)
                .containsExactly(
                    PlatformExerciseSegmentBuilder(
                            START_TIME.plusMillis(1),
                            START_TIME.plusMillis(10),
                            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS
                        )
                        .setRepetitionsCount(10)
                        .build()
                )
            assertThat(route)
                .isEqualTo(
                    PlatformExerciseRoute(
                        listOf(
                            PlatformExerciseRouteLocationBuilder(START_TIME, 23.5, -23.6)
                                .setAltitude(PlatformLength.fromMeters(20.0))
                                .setHorizontalAccuracy(PlatformLength.fromMeters(2.0))
                                .setVerticalAccuracy(PlatformLength.fromMeters(3.0))
                                .build()
                        )
                    )
                )
        }
    }

    @Test
    fun floorsClimbedRecord_convertToPlatform() {
        val platformFloorsClimbed =
            FloorsClimbedRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    floors = 3.9,
                )
                .toPlatformRecord() as PlatformFloorsClimbedRecord

        assertPlatformRecord(platformFloorsClimbed) { assertThat(floors).isEqualTo(3.9) }
    }

    @Test
    fun heartRateRecord_convertToPlatform() {
        val heartRate =
            HeartRateRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = METADATA,
                samples =
                    listOf(
                        HeartRateRecord.Sample(Instant.ofEpochMilli(1234L), 55L),
                        HeartRateRecord.Sample(Instant.ofEpochMilli(5678L), 57L)
                    )
            )

        val platformHeartRate = heartRate.toPlatformRecord() as PlatformHeartRateRecord

        assertPlatformRecord(platformHeartRate) {
            assertThat(samples)
                .comparingElementsUsing(
                    Correspondence.from<PlatformHeartRateSample, PlatformHeartRateSample>(
                        { actual, expected ->
                            actual!!.beatsPerMinute == expected!!.beatsPerMinute &&
                                actual.time == expected.time
                        },
                        "has same BPM and same time as"
                    )
                )
                .containsExactly(
                    PlatformHeartRateSample(55L, Instant.ofEpochMilli(1234L)),
                    PlatformHeartRateSample(57L, Instant.ofEpochMilli(5678L))
                )
        }
    }

    @Test
    fun heartRateVariabilityRmssdRecord_convertToPlatform() {
        val platformHeartRateVariabilityRmssd =
            HeartRateVariabilityRmssdRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    heartRateVariabilityMillis = 1.0,
                )
                .toPlatformRecord() as PlatformHeartRateVariabilityRmssdRecord

        assertPlatformRecord(platformHeartRateVariabilityRmssd) {
            assertThat(heartRateVariabilityMillis).isEqualTo(1.0)
        }
    }

    @Test
    fun heightRecord_convertToPlatform() {
        val platformHeight =
            HeightRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    height = Length.meters(1.8),
                )
                .toPlatformRecord() as PlatformHeightRecord

        assertPlatformRecord(platformHeight) {
            assertThat(height).isEqualTo(PlatformLength.fromMeters(1.8))
        }
    }

    @Test
    fun hydrationRecord_convertToPlatform() {
        val platformHydration =
            HydrationRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    volume = Volume.liters(90.0),
                )
                .toPlatformRecord() as PlatformHydrationRecord

        assertPlatformRecord(platformHydration) {
            assertThat(volume).isEqualTo(PlatformVolume.fromLiters(90.0))
        }
    }

    @Test
    fun intermenstrualBleedingRecord_convertToPlatform() {
        val platformIntermenstrualBleeding =
            IntermenstrualBleedingRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                )
                .toPlatformRecord() as PlatformIntermenstrualBleedingRecord

        assertPlatformRecord(platformIntermenstrualBleeding)
    }

    @Test
    fun leanBodyMassRecord_convertToPlatform() {
        val platformLeanBodyMass =
            LeanBodyMassRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    mass = Mass.grams(21.3),
                )
                .toPlatformRecord() as PlatformLeanBodyMassRecord

        assertPlatformRecord(platformLeanBodyMass) {
            assertThat(mass).isEqualTo(PlatformMass.fromGrams(21.3))
        }
    }

    @Test
    fun menstruationFlowRecord_convertToPlatform() {
        val platformMenstruationFlow =
            MenstruationFlowRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    flow = MenstruationFlowRecord.FLOW_MEDIUM,
                )
                .toPlatformRecord() as PlatformMenstruationFlowRecord

        assertPlatformRecord(platformMenstruationFlow) {
            assertThat(flow).isEqualTo(PlatformMenstruationFlowType.FLOW_MEDIUM)
        }
    }

    @Test
    fun menstruationPeriodRecord_convertToPlatform() {
        val platformMenstruationPeriod =
            MenstruationPeriodRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA
                )
                .toPlatformRecord() as PlatformMenstruationPeriodRecord

        assertPlatformRecord(platformMenstruationPeriod)
    }

    @Test
    fun nutritionRecord_convertToPlatform() {
        val nutrition =
            NutritionRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = METADATA,
                calcium = Mass.grams(15.0),
                caffeine = Mass.grams(20.0),
                chloride = Mass.grams(25.0),
                cholesterol = Mass.grams(30.0),
                chromium = Mass.grams(35.0),
                copper = Mass.grams(40.0),
                molybdenum = Mass.grams(45.0),
                monounsaturatedFat = Mass.grams(50.0),
                energy = Energy.calories(300.0)
            )

        val platformNutrition = nutrition.toPlatformRecord() as PlatformNutritionRecord

        assertPlatformRecord(platformNutrition) {
            assertThat(calcium!!.inGrams).isWithin(tolerance).of(15.0)
            assertThat(caffeine!!.inGrams).isWithin(tolerance).of(20.0)
            assertThat(chloride!!.inGrams).isWithin(tolerance).of(25.0)
            assertThat(cholesterol!!.inGrams).isWithin(tolerance).of(30.0)
            assertThat(chromium!!.inGrams).isWithin(tolerance).of(35.0)
            assertThat(copper!!.inGrams).isWithin(tolerance).of(40.0)
            assertThat(molybdenum!!.inGrams).isWithin(tolerance).of(45.0)
            assertThat(monounsaturatedFat!!.inGrams).isWithin(tolerance).of(50.0)
            assertThat(energy!!.inCalories).isWithin(tolerance).of(300.0)
        }
    }

    @Test
    fun ovulationTestRecord_convertToPlatform() {
        val platformOvulationTest =
            OvulationTestRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    result = OvulationTestRecord.RESULT_POSITIVE,
                )
                .toPlatformRecord() as PlatformOvulationTestRecord

        assertPlatformRecord(platformOvulationTest) {
            assertThat(result).isEqualTo(PlatformOvulationTestResult.RESULT_POSITIVE)
        }
    }

    @Test
    fun oxygenSaturationRecord_convertToPlatform() {
        val platformOxygenSaturation =
            OxygenSaturationRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    percentage = Percentage(15.0),
                )
                .toPlatformRecord() as PlatformOxygenSaturationRecord

        assertPlatformRecord(platformOxygenSaturation) {
            assertThat(percentage).isEqualTo(PlatformPercentage.fromValue(15.0))
        }
    }

    @Test
    fun powerRecord_convertToPlatform() {
        val platformPowerRecord =
            PowerRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    samples = listOf(PowerRecord.Sample(START_TIME, Power.watts(300.0))),
                )
                .toPlatformRecord() as PlatformPowerRecord

        assertPlatformRecord(platformPowerRecord) {
            assertThat(samples)
                .containsExactly(
                    PlatformPowerRecordSample(PlatformPower.fromWatts(300.0), START_TIME)
                )
        }
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun plannedExerciseSessionRecord_conversion() {
        assumeTrue(isAtLeastSdkExtension13())

        val blocks =
            listOf(
                PlannedExerciseBlock(
                    repetitions = 3,
                    steps =
                        listOf(
                            PlannedExerciseStep(
                                exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                                exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
                                completionGoal =
                                    ExerciseCompletionGoal.DurationGoal(Duration.ofMinutes(5)),
                                performanceTargets =
                                    listOf(
                                        ExercisePerformanceTarget.SpeedTarget(
                                            minSpeed = Velocity.metersPerSecond(2.0),
                                            maxSpeed = Velocity.metersPerSecond(3.0)
                                        )
                                    )
                            )
                        )
                )
            )

        val plannedExerciseSessionRecord =
            PlannedExerciseSessionRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                blocks = blocks,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                title = "Morning Run",
                notes = "Easy pace",
                metadata = METADATA,
            )

        val platformPlannedExerciseSessionRecord =
            plannedExerciseSessionRecord.toPlatformRecord() as PlatformPlannedExerciseSessionRecord

        assertPlatformRecord(platformPlannedExerciseSessionRecord) {
            assertThat(exerciseType)
                .isEqualTo(PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING)
            assertThat(title).isEqualTo("Morning Run")
            assertThat(notes).isEqualTo("Easy pace")
            assertThat(blocks).hasSize(1)

            val platformBlock = Iterables.getOnlyElement(this.blocks)
            assertThat(platformBlock.repetitions).isEqualTo(3)
            assertThat(platformBlock.steps).hasSize(1)

            val platformStep = platformBlock.steps[0]
            assertThat(platformStep.exerciseType)
                .isEqualTo(PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING)
            assertThat(platformStep.exerciseCategory)
                .isEqualTo(PlatformPlannedExerciseStep.EXERCISE_CATEGORY_WARMUP)
            assertThat(platformStep.completionGoal).isInstanceOf(PlatformDurationGoal::class.java)

            val durationGoal = platformStep.completionGoal as PlatformDurationGoal
            assertThat(durationGoal.duration).isEqualTo(Duration.ofMinutes(5))

            assertThat(platformStep.performanceGoals).hasSize(1)
            val performanceTarget = platformStep.performanceGoals[0]
            assertThat(performanceTarget).isInstanceOf(PlatformSpeedTarget::class.java)

            val speedTarget = performanceTarget as PlatformSpeedTarget
            assertThat(speedTarget.minSpeed).isEqualTo(PlatformVelocity.fromMetersPerSecond(2.0))
            assertThat(speedTarget.maxSpeed).isEqualTo(PlatformVelocity.fromMetersPerSecond(3.0))
        }

        val sdkRecord =
            platformPlannedExerciseSessionRecord.toSdkRecord() as PlannedExerciseSessionRecord
        assertSdkRecord(sdkRecord) {
            assertThat(exerciseType).isEqualTo(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING)
            assertThat(title).isEqualTo("Morning Run")
            assertThat(notes).isEqualTo("Easy pace")
            assertThat(blocks).hasSize(1)

            val sdkBlock = Iterables.getOnlyElement(this.blocks)
            assertThat(sdkBlock.repetitions).isEqualTo(3)
            assertThat(sdkBlock.steps).hasSize(1)

            val sdkStep = sdkBlock.steps[0]
            assertThat(sdkStep.exerciseType)
                .isEqualTo(ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING)
            assertThat(sdkStep.exercisePhase).isEqualTo(PlannedExerciseStep.EXERCISE_PHASE_WARMUP)
            assertThat(sdkStep.completionGoal)
                .isInstanceOf(ExerciseCompletionGoal.DurationGoal::class.java)

            val sdkDurationGoal = sdkStep.completionGoal as ExerciseCompletionGoal.DurationGoal
            assertThat(sdkDurationGoal.duration).isEqualTo(Duration.ofMinutes(5))

            assertThat(sdkStep.performanceTargets).hasSize(1)
            val sdkPerformanceTarget = sdkStep.performanceTargets[0]
            assertThat(sdkPerformanceTarget)
                .isInstanceOf(ExercisePerformanceTarget.SpeedTarget::class.java)

            val sdkSpeedTarget = sdkPerformanceTarget as ExercisePerformanceTarget.SpeedTarget
            assertThat(sdkSpeedTarget.minSpeed).isEqualTo(Velocity.metersPerSecond(2.0))
            assertThat(sdkSpeedTarget.maxSpeed).isEqualTo(Velocity.metersPerSecond(3.0))
        }
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun plannedExerciseSessionRecord_conversion_multipleBlocksAndSteps() {
        assumeTrue(isAtLeastSdkExtension13())

        val blocks =
            listOf(
                PlannedExerciseBlock(
                    repetitions = 5,
                    steps =
                        listOf(
                            PlannedExerciseStep(
                                exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL,
                                exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                                completionGoal =
                                    ExerciseCompletionGoal.DurationGoal(Duration.ofMinutes(5)),
                                performanceTargets =
                                    listOf(
                                        ExercisePerformanceTarget.SpeedTarget(
                                            minSpeed = Velocity.metersPerSecond(2.0),
                                            maxSpeed = Velocity.metersPerSecond(3.0)
                                        ),
                                        ExercisePerformanceTarget.CadenceTarget(
                                            minCadence = 60.0,
                                            maxCadence = 65.0
                                        )
                                    )
                            )
                        )
                ),
                PlannedExerciseBlock(
                    repetitions = 3,
                    steps =
                        listOf(
                            PlannedExerciseStep(
                                exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING,
                                exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_RECOVERY,
                                completionGoal =
                                    ExerciseCompletionGoal.DurationGoal(Duration.ofMinutes(5)),
                                performanceTargets =
                                    listOf(
                                        ExercisePerformanceTarget.PowerTarget(
                                            minPower = Power.watts(200.0),
                                            maxPower = Power.watts(240.0),
                                        )
                                    )
                            ),
                            PlannedExerciseStep(
                                exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                                exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_COOLDOWN,
                                completionGoal =
                                    ExerciseCompletionGoal.DurationGoal(Duration.ofMinutes(5)),
                                performanceTargets =
                                    listOf(
                                        ExercisePerformanceTarget.SpeedTarget(
                                            minSpeed = Velocity.metersPerSecond(2.0),
                                            maxSpeed = Velocity.metersPerSecond(3.0)
                                        )
                                    )
                            )
                        )
                )
            )

        val plannedExerciseSessionRecord =
            PlannedExerciseSessionRecord(
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                blocks = blocks,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
                title = "Triathlon",
                notes = "Some notes",
                metadata = METADATA,
            )

        assertThat(plannedExerciseSessionRecord.toPlatformRecord().toSdkRecord())
            .isEqualTo(plannedExerciseSessionRecord)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun plannedExerciseSessionRecord_conversion_localeDateConstructor() {
        assumeTrue(isAtLeastSdkExtension13())

        val plannedExerciseSessionRecord =
            PlannedExerciseSessionRecord(
                startDate = LocalDate.of(2024, 9, 20),
                duration = Duration.ofMinutes(45),
                blocks = emptyList(),
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
                title = "Workout",
                notes = "Some notes",
                metadata = METADATA,
            )

        val platformRecord =
            plannedExerciseSessionRecord.toPlatformRecord() as PlatformPlannedExerciseSessionRecord

        assertThat(platformRecord.hasExplicitTime()).isFalse()
        assertThat(platformRecord.toSdkRecord()).isEqualTo(plannedExerciseSessionRecord)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun distanceGoal_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val goal = ExerciseCompletionGoal.DistanceGoal(distance = Length.meters(1000.0))
        val platformGoal = goal.toPlatformExerciseCompletionGoal() as PlatformDistanceGoal
        assertThat(platformGoal.distance).isEqualTo(goal.distance.toPlatformLength())

        val sdkGoal =
            platformGoal.toSdkExerciseCompletionGoal() as ExerciseCompletionGoal.DistanceGoal
        assertThat(sdkGoal.distance).isEqualTo(goal.distance)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun distanceAndDurationGoal_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val goal =
            ExerciseCompletionGoal.DistanceAndDurationGoal(
                distance = Length.meters(1000.0),
                duration = Duration.ofMinutes(5)
            )
        val platformGoal =
            goal.toPlatformExerciseCompletionGoal() as PlatformDistanceAndDurationGoal
        assertThat(platformGoal.distance).isEqualTo(goal.distance.toPlatformLength())
        assertThat(platformGoal.duration).isEqualTo(goal.duration)
        val sdkGoal =
            platformGoal.toSdkExerciseCompletionGoal()
                as ExerciseCompletionGoal.DistanceAndDurationGoal
        assertThat(sdkGoal.distance).isEqualTo(goal.distance)
        assertThat(sdkGoal.duration).isEqualTo(goal.duration)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun stepsGoal_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val goal = ExerciseCompletionGoal.StepsGoal(steps = 1000)
        val platformGoal = goal.toPlatformExerciseCompletionGoal() as PlatformStepsGoal
        assertThat(platformGoal.steps).isEqualTo(goal.steps)
        val sdkGoal = platformGoal.toSdkExerciseCompletionGoal() as ExerciseCompletionGoal.StepsGoal
        assertThat(sdkGoal.steps).isEqualTo(goal.steps)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun durationGoal_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val goal = ExerciseCompletionGoal.DurationGoal(duration = Duration.ofMinutes(5))
        val platformGoal = goal.toPlatformExerciseCompletionGoal() as PlatformDurationGoal
        assertThat(platformGoal.duration).isEqualTo(goal.duration)
        val sdkGoal =
            platformGoal.toSdkExerciseCompletionGoal() as ExerciseCompletionGoal.DurationGoal
        assertThat(sdkGoal.duration).isEqualTo(goal.duration)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun repetitionsGoal_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val goal = ExerciseCompletionGoal.RepetitionsGoal(repetitions = 10)
        val platformGoal = goal.toPlatformExerciseCompletionGoal() as PlatformRepetitionsGoal
        assertThat(platformGoal.repetitions).isEqualTo(goal.repetitions)
        val sdkGoal =
            platformGoal.toSdkExerciseCompletionGoal() as ExerciseCompletionGoal.RepetitionsGoal
        assertThat(sdkGoal.repetitions).isEqualTo(goal.repetitions)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun totalCaloriesBurnedGoal_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val goal =
            ExerciseCompletionGoal.TotalCaloriesBurnedGoal(totalCalories = Energy.calories(100.0))
        val platformGoal =
            goal.toPlatformExerciseCompletionGoal() as PlatformTotalCaloriesBurnedGoal
        assertThat(platformGoal.totalCalories).isEqualTo(goal.totalCalories.toPlatformEnergy())
        val sdkGoal =
            platformGoal.toSdkExerciseCompletionGoal()
                as ExerciseCompletionGoal.TotalCaloriesBurnedGoal
        assertThat(sdkGoal.totalCalories).isEqualTo(goal.totalCalories)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun activeCaloriesBurnedGoal_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val goal =
            ExerciseCompletionGoal.ActiveCaloriesBurnedGoal(activeCalories = Energy.calories(100.0))
        val platformGoal =
            goal.toPlatformExerciseCompletionGoal() as PlatformActiveCaloriesBurnedGoal
        assertThat(platformGoal.activeCalories).isEqualTo(goal.activeCalories.toPlatformEnergy())
        val sdkGoal =
            platformGoal.toSdkExerciseCompletionGoal()
                as ExerciseCompletionGoal.ActiveCaloriesBurnedGoal
        assertThat(sdkGoal.activeCalories).isEqualTo(goal.activeCalories)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun unknownGoal_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val goal = ExerciseCompletionGoal.UnknownGoal
        val platformGoal = goal.toPlatformExerciseCompletionGoal() as PlatformUnknownCompletionGoal
        assertThat(platformGoal).isEqualTo(PlatformUnknownCompletionGoal.INSTANCE)
        assertThat(platformGoal.toSdkExerciseCompletionGoal()).isEqualTo(goal)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun manualCompletion_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val goal = ExerciseCompletionGoal.ManualCompletion
        val platformGoal = goal.toPlatformExerciseCompletionGoal() as PlatformManualCompletion
        assertThat(platformGoal).isEqualTo(PlatformManualCompletion.INSTANCE)
        assertThat(platformGoal.toSdkExerciseCompletionGoal()).isEqualTo(goal)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun powerTarget_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val target =
            ExercisePerformanceTarget.PowerTarget(
                minPower = Power.watts(1.0),
                maxPower = Power.watts(10.0)
            )
        val platformTarget = target.toPlatformExercisePerformanceTarget() as PlatformPowerTarget
        assertThat(platformTarget.minPower).isEqualTo(target.minPower.toPlatformPower())
        assertThat(platformTarget.maxPower).isEqualTo(target.maxPower.toPlatformPower())
        val sdkTarget =
            platformTarget.toSdkExercisePerformanceTarget() as ExercisePerformanceTarget.PowerTarget
        assertThat(sdkTarget.minPower).isEqualTo(target.minPower)
        assertThat(sdkTarget.maxPower).isEqualTo(target.maxPower)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun speedTarget_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val target =
            ExercisePerformanceTarget.SpeedTarget(
                minSpeed = Velocity.metersPerSecond(2.0),
                maxSpeed = Velocity.metersPerSecond(3.0)
            )
        val platformTarget = target.toPlatformExercisePerformanceTarget() as PlatformSpeedTarget
        assertThat(platformTarget.minSpeed).isEqualTo(target.minSpeed.toPlatformVelocity())
        assertThat(platformTarget.maxSpeed).isEqualTo(target.maxSpeed.toPlatformVelocity())
        val sdkTarget =
            platformTarget.toSdkExercisePerformanceTarget() as ExercisePerformanceTarget.SpeedTarget
        assertThat(sdkTarget.minSpeed).isEqualTo(target.minSpeed)
        assertThat(sdkTarget.maxSpeed).isEqualTo(target.maxSpeed)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun cadenceTarget_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val target = ExercisePerformanceTarget.CadenceTarget(minCadence = 80.0, maxCadence = 90.0)
        val platformTarget = target.toPlatformExercisePerformanceTarget() as PlatformCadenceTarget
        assertThat(platformTarget.minRpm).isEqualTo(target.minCadence)
        assertThat(platformTarget.maxRpm).isEqualTo(target.maxCadence)
        val sdkTarget =
            platformTarget.toSdkExercisePerformanceTarget()
                as ExercisePerformanceTarget.CadenceTarget
        assertThat(sdkTarget.minCadence).isEqualTo(target.minCadence)
        assertThat(sdkTarget.maxCadence).isEqualTo(target.maxCadence)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun heartRateTarget_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val target =
            ExercisePerformanceTarget.HeartRateTarget(minHeartRate = 160.0, maxHeartRate = 170.0)
        val platformTarget = target.toPlatformExercisePerformanceTarget() as PlatformHeartRateTarget
        assertThat(platformTarget.minBpm).isEqualTo(target.minHeartRate.roundToInt())
        assertThat(platformTarget.maxBpm).isEqualTo(target.maxHeartRate.roundToInt())
        val sdkTarget =
            platformTarget.toSdkExercisePerformanceTarget()
                as ExercisePerformanceTarget.HeartRateTarget
        assertThat(sdkTarget.minHeartRate).isEqualTo(target.minHeartRate)
        assertThat(sdkTarget.maxHeartRate).isEqualTo(target.maxHeartRate)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun weightTarget_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val target = ExercisePerformanceTarget.WeightTarget(mass = Mass.kilograms(100.0))
        val platformTarget = target.toPlatformExercisePerformanceTarget() as PlatformWeightTarget
        assertThat(platformTarget.mass).isEqualTo(target.mass.toPlatformMass())
        val sdkTarget =
            platformTarget.toSdkExercisePerformanceTarget()
                as ExercisePerformanceTarget.WeightTarget
        assertThat(sdkTarget.mass).isEqualTo(target.mass)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun rateOfPerceivedExertionTarget_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val target = ExercisePerformanceTarget.RateOfPerceivedExertionTarget(rpe = 8)
        val platformTarget =
            target.toPlatformExercisePerformanceTarget() as PlatformRateOfPerceivedExertionTarget
        assertThat(platformTarget.rpe).isEqualTo(target.rpe)
        val sdkTarget =
            platformTarget.toSdkExercisePerformanceTarget()
                as ExercisePerformanceTarget.RateOfPerceivedExertionTarget
        assertThat(sdkTarget.rpe).isEqualTo(target.rpe)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun amrapTarget_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val target = ExercisePerformanceTarget.AmrapTarget
        val platformTarget = target.toPlatformExercisePerformanceTarget() as PlatformAmrapTarget
        assertThat(platformTarget).isEqualTo(PlatformAmrapTarget.INSTANCE)
        assertThat(platformTarget.toSdkExercisePerformanceTarget()).isEqualTo(target)
    }

    @SuppressLint("NewApi") // Guarded by sdk extension check
    @Test
    fun unknownTarget_conversion() {
        assumeTrue(isAtLeastSdkExtension13())
        val target = ExercisePerformanceTarget.UnknownTarget
        val platformTarget =
            target.toPlatformExercisePerformanceTarget() as PlatformUnknownPerformanceTarget
        assertThat(platformTarget).isEqualTo(PlatformUnknownPerformanceTarget.INSTANCE)
        assertThat(platformTarget.toSdkExercisePerformanceTarget()).isEqualTo(target)
    }

    @Test
    fun respiratoryRateRecord_convertToPlatform() {
        val platformRespiratoryRate =
            RespiratoryRateRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    rate = 12.0,
                )
                .toPlatformRecord() as PlatformRespiratoryRateRecord

        assertPlatformRecord(platformRespiratoryRate) { assertThat(rate).isEqualTo(12.0) }
    }

    @Test
    fun restingHeartRateRecord_convertToPlatform() {
        val platformRestingHeartRate =
            RestingHeartRateRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    beatsPerMinute = 57L,
                )
                .toPlatformRecord() as PlatformRestingHeartRateRecord

        assertPlatformRecord(platformRestingHeartRate) { assertThat(beatsPerMinute).isEqualTo(57L) }
    }

    @Test
    fun sexualActivityRecord_convertToPlatform() {
        val platformSexualActivity =
            SexualActivityRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    protectionUsed = SexualActivityRecord.PROTECTION_USED_PROTECTED,
                )
                .toPlatformRecord() as PlatformSexualActivityRecord

        assertPlatformRecord(platformSexualActivity) {
            assertThat(protectionUsed)
                .isEqualTo(PlatformSexualActivityProtectionUsed.PROTECTION_USED_PROTECTED)
        }
    }

    @SuppressLint("NewApi") // Using assumeTrue to only run on the new API version
    @Test
    fun skinTemperatureRecord_convertToPlatform() {
        assumeTrue(isAtLeastSdkExtension13())

        val platformSkinTemperatureRecord =
            SkinTemperatureRecord(
                    startTime = START_TIME,
                    endTime = END_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    baseline = 37.celsius,
                    measurementLocation = SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST,
                    deltas =
                        listOf(
                            SkinTemperatureRecord.Delta(
                                time = START_TIME.plusMillis(10),
                                delta = TemperatureDelta.celsius(0.5)
                            )
                        )
                )
                .toPlatformRecord() as PlatformSkinTemperatureRecord

        assertPlatformRecord(platformSkinTemperatureRecord) {
            assertThat(baseline).isEqualTo(PlatformTemperature.fromCelsius(37.0))
            assertThat(measurementLocation)
                .isEqualTo(PlatformSkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST)
            assertThat(deltas)
                .containsExactly(
                    PlatformSkinTemperatureDelta(
                        PlatformTemperatureDelta.fromCelsius(0.5),
                        START_TIME.plusMillis(10),
                    )
                )
        }
    }

    @Test
    fun sleepSessionRecord_convertToPlatform() {
        val platformSleepSession =
            SleepSessionRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    title = "Night night",
                    notes = "Many dreams",
                    stages =
                        listOf(
                            SleepSessionRecord.Stage(
                                START_TIME,
                                START_TIME.plusMillis(40),
                                SleepSessionRecord.STAGE_TYPE_DEEP
                            )
                        )
                )
                .toPlatformRecord() as PlatformSleepSessionRecord

        assertPlatformRecord(platformSleepSession) {
            assertThat(title).isEqualTo("Night night")
            assertThat(notes).isEqualTo("Many dreams")
            assertThat(stages)
                .containsExactly(
                    PlatformSleepSessionStage(
                        START_TIME,
                        START_TIME.plusMillis(40),
                        PlatformSleepStageType.STAGE_TYPE_SLEEPING_DEEP
                    )
                )
        }
    }

    @Test
    fun speedRecord_convertToPlatform() {
        val platformSpeed =
            SpeedRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    samples = listOf(SpeedRecord.Sample(END_TIME, Velocity.metersPerSecond(3.0))),
                )
                .toPlatformRecord() as PlatformSpeedRecord

        assertPlatformRecord(platformSpeed) {
            assertThat(samples)
                .comparingElementsUsing(
                    Correspondence.from<PlatformSpeedSample, PlatformSpeedSample>(
                        { actual, expected ->
                            actual!!.speed.inMetersPerSecond ==
                                expected!!.speed.inMetersPerSecond && actual.time == expected.time
                        },
                        "has same speed and same time as"
                    )
                )
                .containsExactly(
                    PlatformSpeedSample(PlatformVelocity.fromMetersPerSecond(3.0), END_TIME)
                )
        }
    }

    @Test
    fun stepsRecord_convertToPlatform() {
        val platformSteps =
            StepsRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    count = 10,
                )
                .toPlatformRecord() as PlatformStepsRecord

        assertPlatformRecord(platformSteps) { assertThat(count).isEqualTo(10) }
    }

    @Test
    fun stepsCadenceRecord_convertToPlatform() {
        val platformStepsCadence =
            StepsCadenceRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    samples = listOf(StepsCadenceRecord.Sample(END_TIME, 99.0)),
                )
                .toPlatformRecord() as PlatformStepsCadenceRecord

        assertPlatformRecord(platformStepsCadence) {
            assertThat(samples)
                .comparingElementsUsing(
                    Correspondence.from<PlatformStepsCadenceSample, PlatformStepsCadenceSample>(
                        { actual, expected ->
                            actual!!.rate == expected!!.rate && actual.time == expected.time
                        },
                        "has same rate and same time as"
                    )
                )
                .containsExactly(PlatformStepsCadenceSample(99.0, END_TIME))
        }
    }

    @Test
    fun totalCaloriesBurnedRecord_convertToPlatform() {
        val platformTotalCaloriesBurned =
            TotalCaloriesBurnedRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    energy = Energy.calories(100.0),
                )
                .toPlatformRecord() as PlatformTotalCaloriesBurnedRecord

        assertPlatformRecord(platformTotalCaloriesBurned) {
            assertThat(energy).isEqualTo(PlatformEnergy.fromCalories(100.0))
        }
    }

    @Test
    fun vo2MaxRecord_convertToPlatform() {
        val platformVo2Max =
            Vo2MaxRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    vo2MillilitersPerMinuteKilogram = 5.0,
                    measurementMethod = Vo2MaxRecord.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST
                )
                .toPlatformRecord() as PlatformVo2MaxRecord

        assertPlatformRecord(platformVo2Max) {
            assertThat(vo2MillilitersPerMinuteKilogram).isEqualTo(5.0)
            assertThat(measurementMethod)
                .isEqualTo(
                    PlatformVo2MaxMeasurementMethod.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST
                )
        }
    }

    @Test
    fun weightRecord_convertToPlatform() {
        val platformWeight =
            WeightRecord(
                    time = TIME,
                    zoneOffset = ZONE_OFFSET,
                    metadata = METADATA,
                    weight = Mass.grams(100.0),
                )
                .toPlatformRecord() as PlatformWeightRecord

        assertPlatformRecord(platformWeight) {
            assertThat(weight).isEqualTo(PlatformMass.fromGrams(100.0))
        }
    }

    @Test
    fun wheelChairPushesRecord_convertToPlatform() {
        val platformWheelchairPushes =
            WheelchairPushesRecord(
                    startTime = START_TIME,
                    startZoneOffset = START_ZONE_OFFSET,
                    endTime = END_TIME,
                    endZoneOffset = END_ZONE_OFFSET,
                    metadata = METADATA,
                    count = 10,
                )
                .toPlatformRecord() as PlatformWheelchairPushesRecord

        assertPlatformRecord(platformWheelchairPushes) { assertThat(count).isEqualTo(10) }
    }

    @Test
    fun activeCaloriesBurnedRecord_convertToSdk() {
        val sdkActiveCaloriesBurned =
            PlatformActiveCaloriesBurnedRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    PlatformEnergy.fromCalories(300.0)
                )
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as ActiveCaloriesBurnedRecord

        assertSdkRecord(sdkActiveCaloriesBurned) {
            assertThat(energy).isEqualTo(Energy.calories(300.0))
        }
    }

    @Test
    fun basalBodyTemperatureRecord_convertToSdk() {
        val sdkBasalBodyTemperature =
            PlatformBasalBodyTemperatureRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_RECTUM,
                    PlatformTemperature.fromCelsius(37.0)
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as BasalBodyTemperatureRecord

        assertSdkRecord(sdkBasalBodyTemperature) {
            assertThat(measurementLocation)
                .isEqualTo(BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_RECTUM)
            assertThat(temperature).isEqualTo(Temperature.celsius(37.0))
        }
    }

    @Test
    fun basalMetabolicRateRecord_convertToSdk() {
        val sdkBasalMetabolicRate =
            PlatformBasalMetabolicRateRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformPower.fromWatts(100.0)
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as BasalMetabolicRateRecord

        assertSdkRecord(sdkBasalMetabolicRate) {
            assertThat(basalMetabolicRate).isEqualTo(Power.watts(100.0))
        }
    }

    @Test
    fun bloodGlucoseRecord_convertToSdk() {
        val sdkBloodGlucose =
            PlatformBloodGlucoseRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformBloodGlucoseSpecimenSource.SPECIMEN_SOURCE_TEARS,
                    PlatformBloodGlucose.fromMillimolesPerLiter(10.2),
                    PlatformBloodGlucoseRelationToMealType.RELATION_TO_MEAL_FASTING,
                    PlatformMealType.MEAL_TYPE_SNACK
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as BloodGlucoseRecord

        assertSdkRecord(sdkBloodGlucose) {
            assertThat(level).isEqualTo(BloodGlucose.millimolesPerLiter(10.2))
            assertThat(specimenSource).isEqualTo(BloodGlucoseRecord.SPECIMEN_SOURCE_TEARS)
            assertThat(mealType).isEqualTo(MealType.MEAL_TYPE_SNACK)
            assertThat(relationToMeal).isEqualTo(BloodGlucoseRecord.RELATION_TO_MEAL_FASTING)
        }
    }

    @Test
    fun bloodPressureRecord_convertToSdk() {
        val sdkBloodPressure =
            PlatformBloodPressureRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformBloodPressureMeasurementLocation
                        .BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST,
                    PlatformPressure.fromMillimetersOfMercury(20.0),
                    PlatformPressure.fromMillimetersOfMercury(15.0),
                    PlatformBloodPressureBodyPosition.BODY_POSITION_STANDING_UP
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as BloodPressureRecord

        assertSdkRecord(sdkBloodPressure) {
            assertThat(measurementLocation)
                .isEqualTo(BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_WRIST)
            assertThat(systolic).isEqualTo(Pressure.millimetersOfMercury(20.0))
            assertThat(diastolic).isEqualTo(Pressure.millimetersOfMercury(15.0))
            assertThat(bodyPosition).isEqualTo(BloodPressureRecord.BODY_POSITION_STANDING_UP)
        }
    }

    @Test
    fun bodyFatRecord_convertToSdk() {
        val sdkBodyFat =
            PlatformBodyFatRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformPercentage.fromValue(18.0)
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as BodyFatRecord

        assertSdkRecord(sdkBodyFat) { assertThat(percentage).isEqualTo(Percentage(18.0)) }
    }

    @Test
    fun bodyTemperatureRecord_convertToSdk() {
        val sdkBodyTemperature =
            PlatformBodyTemperatureRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_WRIST,
                    PlatformTemperature.fromCelsius(27.0)
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as BodyTemperatureRecord

        assertSdkRecord(sdkBodyTemperature) {
            assertThat(measurementLocation)
                .isEqualTo(BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_WRIST)
            assertThat(temperature).isEqualTo(Temperature.celsius(27.0))
        }
    }

    @Test
    fun bodyWaterMassRecord_convertToSdk() {
        val sdkBodyWaterMass =
            PlatformBodyWaterMassRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformMass.fromGrams(12.0)
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as BodyWaterMassRecord

        assertSdkRecord(sdkBodyWaterMass) { assertThat(mass).isEqualTo(Mass.grams(12.0)) }
    }

    @Test
    fun boneMassRecord_convertToSdk() {
        val sdkBoneMass =
            PlatformBoneMassRecordBuilder(PLATFORM_METADATA, TIME, PlatformMass.fromGrams(73.0))
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as BoneMassRecord

        assertSdkRecord(sdkBoneMass) { assertThat(mass).isEqualTo(Mass.grams(73.0)) }
    }

    @Test
    fun cervicalMucusRecord_convertToSdk() {
        val sdkCervicalMucus =
            PlatformCervicalMucusRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformCervicalMucusSensation.SENSATION_HEAVY,
                    PlatformCervicalMucusAppearance.APPEARANCE_DRY
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as CervicalMucusRecord

        assertSdkRecord(sdkCervicalMucus) {
            assertThat(sensation).isEqualTo(CervicalMucusRecord.SENSATION_HEAVY)
            assertThat(appearance).isEqualTo(CervicalMucusRecord.APPEARANCE_DRY)
        }
    }

    @Test
    fun cyclingPedalingCadenceRecord_convertToSdk() {
        val sdkCyclingPedalingCadence =
            PlatformCyclingPedalingCadenceRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    listOf(PlatformCyclingPedalingCadenceSample(23.0, END_TIME))
                )
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as CyclingPedalingCadenceRecord

        assertSdkRecord(sdkCyclingPedalingCadence) {
            assertThat(samples).containsExactly(CyclingPedalingCadenceRecord.Sample(END_TIME, 23.0))
        }
    }

    @Test
    fun distanceRecord_convertToSdk() {
        val sdkDistance =
            PlatformDistanceRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    PlatformLength.fromMeters(500.0)
                )
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as DistanceRecord

        assertSdkRecord(sdkDistance) { assertThat(distance).isEqualTo(Length.meters(500.0)) }
    }

    @Test
    fun elevationGainedRecord_convertToSdk() {
        val sdkElevationGained =
            PlatformElevationGainedRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    PlatformLength.fromMeters(10.0)
                )
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as ElevationGainedRecord

        assertSdkRecord(sdkElevationGained) { assertThat(elevation).isEqualTo(Length.meters(10.0)) }
    }

    @Test
    fun exerciseSessionRecord_convertToSdk() {
        val platformExerciseSessionBuilder =
            PlatformExerciseSessionRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    PlatformExerciseSessionType
                        .EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
                )
                .setTitle("Training")
                .setNotes("Improve jump serve")
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .setLaps(
                    listOf(
                        PlatformExerciseLapBuilder(
                                START_TIME.plusMillis(6),
                                START_TIME.plusMillis(10)
                            )
                            .setLength(PlatformLength.fromMeters(1.0))
                            .build(),
                        PlatformExerciseLapBuilder(
                                START_TIME.plusMillis(11),
                                START_TIME.plusMillis(15)
                            )
                            .setLength(PlatformLength.fromMeters(1.5))
                            .build()
                    )
                )
                .setSegments(
                    listOf(
                        PlatformExerciseSegmentBuilder(
                                START_TIME.plusMillis(1),
                                START_TIME.plusMillis(10),
                                PlatformExerciseSegmentType
                                    .EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS
                            )
                            .setRepetitionsCount(10)
                            .build()
                    )
                )
                .setRoute(
                    PlatformExerciseRoute(
                        listOf(
                            PlatformExerciseRouteLocationBuilder(START_TIME, 23.4, -23.4)
                                .setAltitude(PlatformLength.fromMeters(10.0))
                                .setHorizontalAccuracy(PlatformLength.fromMeters(2.0))
                                .setVerticalAccuracy(PlatformLength.fromMeters(3.0))
                                .build()
                        )
                    )
                )

        var sdkExerciseSession =
            platformExerciseSessionBuilder.build().toSdkRecord() as ExerciseSessionRecord

        assertSdkRecord(sdkExerciseSession) {
            assertThat(title).isEqualTo("Training")
            assertThat(notes).isEqualTo("Improve jump serve")
            assertThat(exerciseType)
                .isEqualTo(ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING)
            assertThat(laps)
                .containsExactly(
                    ExerciseLap(
                        START_TIME.plusMillis(6),
                        START_TIME.plusMillis(10),
                        Length.meters(1.0)
                    ),
                    ExerciseLap(
                        START_TIME.plusMillis(11),
                        START_TIME.plusMillis(15),
                        Length.meters(1.5)
                    )
                )
            assertThat(segments)
                .containsExactly(
                    ExerciseSegment(
                        START_TIME.plusMillis(1),
                        START_TIME.plusMillis(10),
                        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
                        10
                    )
                )
            assertThat(exerciseRouteResult as ExerciseRouteResult.Data)
                .isEqualTo(
                    ExerciseRouteResult.Data(
                        ExerciseRoute(
                            listOf(
                                ExerciseRoute.Location(
                                    time = START_TIME,
                                    latitude = 23.4,
                                    longitude = -23.4,
                                    altitude = Length.meters(10.0),
                                    horizontalAccuracy = Length.meters(2.0),
                                    verticalAccuracy = Length.meters(3.0)
                                )
                            )
                        )
                    )
                )
        }

        sdkExerciseSession =
            platformExerciseSessionBuilder.setRoute(null).build().toSdkRecord()
                as ExerciseSessionRecord

        assertSdkRecord(sdkExerciseSession) {
            assertThat(exerciseRouteResult).isEqualTo(ExerciseRouteResult.NoData())
        }
    }

    @Test
    fun exerciseSessionRecord_plannedExerciseSessionId() {
        assumeTrue(isAtLeastSdkExtension13())

        val sdkRecord =
            ExerciseSessionRecord(
                plannedExerciseSessionId = "some-id",
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
                metadata = METADATA,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                title = "HIIT training",
                notes = "Hard workout",
                laps = emptyList(),
                segments = emptyList(),
                exerciseRoute = null,
            )

        val roundTripConvertedRecord =
            sdkRecord.toPlatformRecord().toSdkRecord() as ExerciseSessionRecord
        assertThat(roundTripConvertedRecord.plannedExerciseSessionId).isEqualTo("some-id")
    }

    @Test
    fun floorsClimbedRecord_convertToSdk() {
        val sdkFloorsClimbed =
            PlatformFloorsClimbedRecordBuilder(PLATFORM_METADATA, START_TIME, END_TIME, 10.0)
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as FloorsClimbedRecord

        assertSdkRecord(sdkFloorsClimbed) { assertThat(floors).isEqualTo(10.0) }
    }

    @Test
    fun heartRateRecord_convertToSdk() {
        val sdkHeartRate =
            PlatformHeartRateRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    listOf(PlatformHeartRateSample(83, START_TIME))
                )
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as HeartRateRecord

        assertSdkRecord(sdkHeartRate) {
            assertThat(samples).containsExactly(HeartRateRecord.Sample(START_TIME, 83))
        }
    }

    @Test
    fun heartRateVariabilityRmssdRecord_convertToSdk() {
        val sdkHeartRateVariabilityRmssd =
            PlatformHeartRateVariabilityRmssdRecordBuilder(PLATFORM_METADATA, TIME, 1.6)
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as HeartRateVariabilityRmssdRecord

        assertSdkRecord(sdkHeartRateVariabilityRmssd) {
            assertThat(heartRateVariabilityMillis).isEqualTo(1.6)
        }
    }

    @Test
    fun heightRecord_convertToSdk() {
        val sdkHeight =
            PlatformHeightRecordBuilder(PLATFORM_METADATA, TIME, PlatformLength.fromMeters(1.7))
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as HeightRecord

        assertSdkRecord(sdkHeight) { assertThat(height).isEqualTo(Length.meters(1.7)) }
    }

    @Test
    fun hydrationRecord_convertToSdk() {
        val sdkHydration =
            PlatformHydrationRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    PlatformVolume.fromLiters(90.0)
                )
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as HydrationRecord

        assertSdkRecord(sdkHydration) { assertThat(volume).isEqualTo(Volume.liters(90.0)) }
    }

    @Test
    fun intermenstrualBleedingRecord_convertToSdk() {
        val sdkIntermenstrualBleeding =
            PlatformIntermenstrualBleedingRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as IntermenstrualBleedingRecord

        assertSdkRecord(sdkIntermenstrualBleeding)
    }

    @Test
    fun leanBodyMassRecord_convertToSdk() {
        val sdkLeanBodyMass =
            PlatformLeanBodyMassRecordBuilder(PLATFORM_METADATA, TIME, PlatformMass.fromGrams(9.0))
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as LeanBodyMassRecord

        assertSdkRecord(sdkLeanBodyMass) { assertThat(mass).isEqualTo(Mass.grams(9.0)) }
    }

    @Test
    fun menstruationFlowRecord_convertToSdk() {
        val sdkMenstruationFlow =
            PlatformMenstruationFlowRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformMenstruationFlowType.FLOW_MEDIUM
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as MenstruationFlowRecord

        assertSdkRecord(sdkMenstruationFlow) {
            assertThat(flow).isEqualTo(MenstruationFlowRecord.FLOW_MEDIUM)
        }
    }

    @Test
    fun menstruationPeriodRecord_convertToSdk() {
        val sdkMenstruationPeriod =
            PlatformMenstruationPeriodRecordBuilder(PLATFORM_METADATA, START_TIME, END_TIME)
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as MenstruationPeriodRecord

        assertSdkRecord(sdkMenstruationPeriod)
    }

    @Test
    fun nutritionRecord_convertToSdk() {
        val sdkNutrition =
            PlatformNutritionRecordBuilder(PLATFORM_METADATA, START_TIME, END_TIME)
                .setMealName("Cheat meal")
                .setMealType(PlatformMealType.MEAL_TYPE_DINNER)
                .setChromium(PlatformMass.fromGrams(0.01))
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as NutritionRecord

        assertSdkRecord(sdkNutrition) {
            assertThat(name).isEqualTo("Cheat meal")
            assertThat(mealType).isEqualTo(MealType.MEAL_TYPE_DINNER)
            assertThat(chromium).isEqualTo(Mass.grams(0.01))
        }
    }

    @Test
    fun ovulationTestRecord_convertToSdk() {
        val sdkOvulationTest =
            PlatformOvulationTestRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformOvulationTestResult.RESULT_NEGATIVE
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as OvulationTestRecord

        assertSdkRecord(sdkOvulationTest) {
            assertThat(result).isEqualTo(OvulationTestRecord.RESULT_NEGATIVE)
        }
    }

    @Test
    fun oxygenSaturationRecord_convertToSdk() {
        val sdkOxygenSaturation =
            PlatformOxygenSaturationRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformPercentage.fromValue(21.0)
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as OxygenSaturationRecord

        assertSdkRecord(sdkOxygenSaturation) { assertThat(percentage).isEqualTo(Percentage(21.0)) }
    }

    @Test
    fun powerRecord_convertToSdk() {
        val sdkPower =
            PlatformPowerRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    listOf(PlatformPowerRecordSample(PlatformPower.fromWatts(300.0), START_TIME))
                )
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as PowerRecord

        assertSdkRecord(sdkPower) {
            assertThat(samples).containsExactly(PowerRecord.Sample(START_TIME, Power.watts(300.0)))
        }
    }

    @Test
    fun respiratoryRateRecord_convertToSdk() {
        val sdkRespiratoryRate =
            PlatformRespiratoryRateRecordBuilder(PLATFORM_METADATA, TIME, 12.0)
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as RespiratoryRateRecord

        assertSdkRecord(sdkRespiratoryRate) { assertThat(rate).isEqualTo(12.0) }
    }

    @Test
    fun restingHeartRateRecord_convertToSdk() {
        val sdkRestingHeartRate =
            PlatformRestingHeartRateRecordBuilder(PLATFORM_METADATA, TIME, 37)
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as RestingHeartRateRecord

        assertSdkRecord(sdkRestingHeartRate) { assertThat(beatsPerMinute).isEqualTo(37) }
    }

    @Test
    fun sexualActivityRecord_convertToSdk() {
        val sdkSexualActivity =
            PlatformSexualActivityRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformSexualActivityProtectionUsed.PROTECTION_USED_PROTECTED
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as SexualActivityRecord

        assertSdkRecord(sdkSexualActivity) {
            assertThat(protectionUsed).isEqualTo(SexualActivityRecord.PROTECTION_USED_PROTECTED)
        }
    }

    @SuppressLint("NewApi") // Using assumeTrue to only run on the new API version
    @Test
    fun skinTemperatureRecord_convertToSdk() {
        assumeTrue(isAtLeastSdkExtension13())

        val sdkSkinTemperatureRecord =
            PlatformSkinTemperatureRecordBuilder(PLATFORM_METADATA, START_TIME, END_TIME)
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .setBaseline(PlatformTemperature.fromCelsius(35.5))
                .setDeltas(
                    listOf(
                        PlatformSkinTemperatureDelta(
                            PlatformTemperatureDelta.fromCelsius(0.2),
                            START_TIME.plusMillis(20)
                        )
                    )
                )
                .setMeasurementLocation(PlatformSkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER)
                .build()
                .toSdkRecord() as SkinTemperatureRecord

        assertSdkRecord(sdkSkinTemperatureRecord) {
            assertThat(baseline).isEqualTo(35.5.celsius)
            assertThat(deltas)
                .containsExactly(
                    SkinTemperatureRecord.Delta(
                        START_TIME.plusMillis(20),
                        TemperatureDelta.celsius(0.2)
                    )
                )
            assertThat(measurementLocation)
                .isEqualTo(SkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER)
        }
    }

    @Test
    fun sleepSessionRecord_convertToSdk() {
        val sdkSleepSession =
            PlatformSleepSessionRecordBuilder(PLATFORM_METADATA, START_TIME, END_TIME)
                .setTitle("nap")
                .setNotes("Afternoon reset")
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .setStages(
                    listOf(
                        PlatformSleepSessionStage(
                            START_TIME,
                            START_TIME.plusMillis(1),
                            PlatformSleepStageType.STAGE_TYPE_AWAKE
                        ),
                        PlatformSleepSessionStage(
                            END_TIME.minusMillis(1),
                            END_TIME,
                            PlatformSleepStageType.STAGE_TYPE_SLEEPING
                        )
                    )
                )
                .build()
                .toSdkRecord() as SleepSessionRecord

        assertSdkRecord(sdkSleepSession) {
            assertThat(title).isEqualTo("nap")
            assertThat(notes).isEqualTo("Afternoon reset")
            assertThat(stages)
                .containsExactly(
                    SleepSessionRecord.Stage(
                        START_TIME,
                        START_TIME.plusMillis(1),
                        SleepSessionRecord.STAGE_TYPE_AWAKE
                    ),
                    SleepSessionRecord.Stage(
                        END_TIME.minusMillis(1),
                        END_TIME,
                        SleepSessionRecord.STAGE_TYPE_SLEEPING
                    )
                )
        }
    }

    @Test
    fun speedRecord_convertToSdk() {
        val sdkSpeed =
            PlatformSpeedRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    listOf(
                        PlatformSpeedSample(PlatformVelocity.fromMetersPerSecond(99.0), END_TIME)
                    )
                )
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as SpeedRecord

        assertSdkRecord(sdkSpeed) {
            assertThat(samples)
                .containsExactly(SpeedRecord.Sample(END_TIME, Velocity.metersPerSecond(99.0)))
        }
    }

    @Test
    fun stepsCadenceRecord_convertToSdk() {
        val sdkStepsCadence =
            PlatformStepsCadenceRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    listOf(PlatformStepsCadenceSample(10.0, END_TIME))
                )
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as StepsCadenceRecord

        assertSdkRecord(sdkStepsCadence) {
            assertThat(samples).containsExactly(StepsCadenceRecord.Sample(END_TIME, 10.0))
        }
    }

    @Test
    fun stepsRecord_convertToSdk() {
        val sdkSteps =
            PlatformStepsRecordBuilder(PLATFORM_METADATA, START_TIME, END_TIME, 10)
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as StepsRecord

        assertSdkRecord(sdkSteps) { assertThat(count).isEqualTo(10) }
    }

    @Test
    fun totalCaloriesBurnedRecord_convertToSdk() {
        val sdkTotalCaloriesBurned =
            PlatformTotalCaloriesBurnedRecordBuilder(
                    PLATFORM_METADATA,
                    START_TIME,
                    END_TIME,
                    PlatformEnergy.fromCalories(333.0)
                )
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as TotalCaloriesBurnedRecord

        assertSdkRecord(sdkTotalCaloriesBurned) {
            assertThat(energy).isEqualTo(Energy.calories(333.0))
        }
    }

    @Test
    fun vo2MaxRecord_convertToSdk() {
        val sdkVo2Max =
            PlatformVo2MaxRecordBuilder(
                    PLATFORM_METADATA,
                    TIME,
                    PlatformVo2MaxMeasurementMethod.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST,
                    13.0
                )
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as Vo2MaxRecord

        assertSdkRecord(sdkVo2Max) {
            assertThat(measurementMethod)
                .isEqualTo(Vo2MaxRecord.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST)
            assertThat(vo2MillilitersPerMinuteKilogram).isEqualTo(13.0)
        }
    }

    @Test
    fun weightRecord_convertToSdk() {
        val sdkWeight =
            PlatformWeightRecordBuilder(PLATFORM_METADATA, TIME, PlatformMass.fromGrams(63.0))
                .setZoneOffset(ZONE_OFFSET)
                .build()
                .toSdkRecord() as WeightRecord

        assertSdkRecord(sdkWeight) { assertThat(weight).isEqualTo(Mass.grams(63.0)) }
    }

    @Test
    fun wheelChairPushesRecord_convertToSdk() {
        val sdkWheelchairPushes =
            PlatformWheelchairPushesRecordBuilder(PLATFORM_METADATA, START_TIME, END_TIME, 18)
                .setStartZoneOffset(START_ZONE_OFFSET)
                .setEndZoneOffset(END_ZONE_OFFSET)
                .build()
                .toSdkRecord() as WheelchairPushesRecord

        assertSdkRecord(sdkWheelchairPushes) { assertThat(count).isEqualTo(18) }
    }

    private fun <T : PlatformIntervalRecord> assertPlatformRecord(platformRecord: T) {
        assertPlatformRecord(platformRecord) {}
    }

    private fun <T : PlatformIntervalRecord> assertPlatformRecord(
        platformRecord: T,
        typeSpecificAssertions: T.() -> Unit
    ) {
        assertThat(platformRecord.startTime).isEqualTo(START_TIME)
        assertThat(platformRecord.startZoneOffset).isEqualTo(START_ZONE_OFFSET)
        assertThat(platformRecord.endTime).isEqualTo(END_TIME)
        assertThat(platformRecord.endZoneOffset).isEqualTo(END_ZONE_OFFSET)
        assertThat(platformRecord.metadata).isEqualTo(PLATFORM_METADATA)
        platformRecord.typeSpecificAssertions()
    }

    private fun <T : PlatformInstantRecord> assertPlatformRecord(platformRecord: T) =
        assertPlatformRecord(platformRecord) {}

    private fun <T : PlatformInstantRecord> assertPlatformRecord(
        platformRecord: T,
        typeSpecificAssertions: T.() -> Unit
    ) {
        assertThat(platformRecord.time).isEqualTo(TIME)
        assertThat(platformRecord.zoneOffset).isEqualTo(ZONE_OFFSET)
        assertThat(platformRecord.metadata).isEqualTo(PLATFORM_METADATA)
        platformRecord.typeSpecificAssertions()
    }

    private fun <T : IntervalRecord> assertSdkRecord(sdkRecord: T) = assertSdkRecord(sdkRecord) {}

    private fun <T : IntervalRecord> assertSdkRecord(
        sdkRecord: T,
        typeSpecificAssertions: T.() -> Unit
    ) {
        assertThat(sdkRecord.startTime).isEqualTo(START_TIME)
        assertThat(sdkRecord.startZoneOffset).isEqualTo(START_ZONE_OFFSET)
        assertThat(sdkRecord.endTime).isEqualTo(END_TIME)
        assertThat(sdkRecord.endZoneOffset).isEqualTo(END_ZONE_OFFSET)
        assertThat(sdkRecord.metadata.id).isEqualTo(METADATA.id)
        assertThat(sdkRecord.metadata.dataOrigin).isEqualTo(METADATA.dataOrigin)
        assertThat(sdkRecord.metadata.device).isEqualTo(METADATA.device)
        sdkRecord.typeSpecificAssertions()
    }

    private fun <T : InstantaneousRecord> assertSdkRecord(sdkRecord: T) =
        assertSdkRecord(sdkRecord) {}

    private fun <T : InstantaneousRecord> assertSdkRecord(
        sdkRecord: T,
        typeSpecificAssertions: T.() -> Unit
    ) {
        assertThat(sdkRecord.time).isEqualTo(TIME)
        assertThat(sdkRecord.zoneOffset).isEqualTo(ZONE_OFFSET)
        assertThat(sdkRecord.metadata.id).isEqualTo(METADATA.id)
        assertThat(sdkRecord.metadata.dataOrigin).isEqualTo(METADATA.dataOrigin)
        sdkRecord.typeSpecificAssertions()
    }

    private companion object {
        val TIME: Instant = Instant.ofEpochMilli(1235L)
        val ZONE_OFFSET: ZoneOffset = ZoneOffset.UTC

        val START_TIME: Instant = Instant.ofEpochMilli(1234L)
        val END_TIME: Instant = Instant.ofEpochMilli(56780L)
        val START_ZONE_OFFSET: ZoneOffset = ZoneOffset.UTC
        val END_ZONE_OFFSET: ZoneOffset = ZoneOffset.ofHours(2)

        val METADATA =
            Metadata(
                id = "someId",
                dataOrigin = DataOrigin("somePackage"),
                device =
                    Device(
                        manufacturer = "ACME Corp",
                        model = "Smartphone",
                        type = Device.TYPE_PHONE
                    )
            )

        val PLATFORM_METADATA =
            PlatformMetadataBuilder()
                .setId("someId")
                .setDataOrigin(PlatformDataOriginBuilder().setPackageName("somePackage").build())
                .setDevice(
                    PlatformDeviceBuilder()
                        .setManufacturer("ACME Corp")
                        .setModel("Smartphone")
                        .setType(Device.TYPE_PHONE)
                        .build()
                )
                .build()
    }
}
