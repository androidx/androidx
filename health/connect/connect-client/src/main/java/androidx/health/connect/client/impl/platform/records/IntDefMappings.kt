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

@file:RestrictTo(RestrictTo.Scope.LIBRARY)
@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform.records

import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.metadata.Metadata

internal val SDK_TO_PLATFORM_CERVICAL_MUCUS_APPEARANCE: Map<Int, Int> =
    mapOf(
        CervicalMucusRecord.APPEARANCE_EGG_WHITE to
            PlatformCervicalMucusAppearance.APPEARANCE_EGG_WHITE,
        CervicalMucusRecord.APPEARANCE_DRY to PlatformCervicalMucusAppearance.APPEARANCE_DRY,
        CervicalMucusRecord.APPEARANCE_STICKY to PlatformCervicalMucusAppearance.APPEARANCE_STICKY,
        CervicalMucusRecord.APPEARANCE_CREAMY to PlatformCervicalMucusAppearance.APPEARANCE_CREAMY,
        CervicalMucusRecord.APPEARANCE_WATERY to PlatformCervicalMucusAppearance.APPEARANCE_WATERY,
        CervicalMucusRecord.APPEARANCE_UNUSUAL to
            PlatformCervicalMucusAppearance.APPEARANCE_UNUSUAL,
    )

internal val PLATFORM_TO_SDK_CERVICAL_MUCUS_APPEARANCE =
    SDK_TO_PLATFORM_CERVICAL_MUCUS_APPEARANCE.reversed()

internal val SDK_TO_PLATFORM_BLOOD_PRESSURE_BODY_POSITION: Map<Int, Int> =
    mapOf(
        BloodPressureRecord.BODY_POSITION_STANDING_UP to
            PlatformBloodPressureBodyPosition.BODY_POSITION_STANDING_UP,
        BloodPressureRecord.BODY_POSITION_SITTING_DOWN to
            PlatformBloodPressureBodyPosition.BODY_POSITION_SITTING_DOWN,
        BloodPressureRecord.BODY_POSITION_LYING_DOWN to
            PlatformBloodPressureBodyPosition.BODY_POSITION_LYING_DOWN,
        BloodPressureRecord.BODY_POSITION_RECLINING to
            PlatformBloodPressureBodyPosition.BODY_POSITION_RECLINING,
    )

internal val PLATFORM_TO_SDK_BLOOD_PRESSURE_BODY_POSITION =
    SDK_TO_PLATFORM_BLOOD_PRESSURE_BODY_POSITION.reversed()

internal val SDK_TO_PLATFORM_EXERCISE_SESSION_TYPE: Map<Int, Int> =
    mapOf(
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_OTHER_WORKOUT,
        ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON,
        ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_BASEBALL,
        ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_BASKETBALL,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING_STATIONARY,
        ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_BOOT_CAMP,
        ExerciseSessionRecord.EXERCISE_TYPE_BOXING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_BOXING,
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_CALISTHENICS,
        ExerciseSessionRecord.EXERCISE_TYPE_CRICKET to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_CRICKET,
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_DANCING,
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_ELLIPTICAL,
        ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_EXERCISE_CLASS,
        ExerciseSessionRecord.EXERCISE_TYPE_FENCING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_FENCING,
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN,
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AUSTRALIAN,
        ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_FRISBEE_DISC,
        ExerciseSessionRecord.EXERCISE_TYPE_GOLF to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_GOLF,
        ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_GUIDED_BREATHING,
        ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_GYMNASTICS,
        ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_HANDBALL,
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_HIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_ICE_HOCKEY,
        ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_ICE_SKATING,
        ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_MARTIAL_ARTS,
        ExerciseSessionRecord.EXERCISE_TYPE_PADDLING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_PADDLING,
        ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_PARAGLIDING,
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_PILATES,
        ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_RACQUETBALL,
        ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_ROCK_CLIMBING,
        ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_ROLLER_HOCKEY,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_ROWING_MACHINE,
        ExerciseSessionRecord.EXERCISE_TYPE_RUGBY to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_RUGBY,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING_TREADMILL,
        ExerciseSessionRecord.EXERCISE_TYPE_SAILING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SAILING,
        ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SCUBA_DIVING,
        ExerciseSessionRecord.EXERCISE_TYPE_SKATING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SKATING,
        ExerciseSessionRecord.EXERCISE_TYPE_SKIING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SKIING,
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SNOWBOARDING,
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SNOWSHOEING,
        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SOCCER,
        ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SOFTBALL,
        ExerciseSessionRecord.EXERCISE_TYPE_SQUASH to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SQUASH,
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_STAIR_CLIMBING,
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_STAIR_CLIMBING_MACHINE,
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_STRETCHING,
        ExerciseSessionRecord.EXERCISE_TYPE_SURFING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SURFING,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_TABLE_TENNIS,
        ExerciseSessionRecord.EXERCISE_TYPE_TENNIS to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_TENNIS,
        ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_VOLLEYBALL,
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_WATER_POLO,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_WEIGHTLIFTING,
        ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_WHEELCHAIR,
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA to
            PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_YOGA,
    )

internal val PLATFORM_TO_SDK_EXERCISE_SESSION_TYPE =
    SDK_TO_PLATFORM_EXERCISE_SESSION_TYPE.reversed()

internal val SDK_TO_PLATFORM_MEAL_TYPE: Map<Int, Int> =
    mapOf(
        MealType.MEAL_TYPE_BREAKFAST to PlatformMealType.MEAL_TYPE_BREAKFAST,
        MealType.MEAL_TYPE_LUNCH to PlatformMealType.MEAL_TYPE_LUNCH,
        MealType.MEAL_TYPE_DINNER to PlatformMealType.MEAL_TYPE_DINNER,
        MealType.MEAL_TYPE_SNACK to PlatformMealType.MEAL_TYPE_SNACK,
    )

internal val PLATFORM_TO_SDK_MEAL_TYPE = SDK_TO_PLATFORM_MEAL_TYPE.reversed()

internal val SDK_TO_PLATFORM_VO2_MAX_MEASUREMENT_METHOD: Map<Int, Int> =
    mapOf(
        Vo2MaxRecord.MEASUREMENT_METHOD_METABOLIC_CART to
            PlatformVo2MaxMeasurementMethod.MEASUREMENT_METHOD_METABOLIC_CART,
        Vo2MaxRecord.MEASUREMENT_METHOD_HEART_RATE_RATIO to
            PlatformVo2MaxMeasurementMethod.MEASUREMENT_METHOD_HEART_RATE_RATIO,
        Vo2MaxRecord.MEASUREMENT_METHOD_COOPER_TEST to
            PlatformVo2MaxMeasurementMethod.MEASUREMENT_METHOD_COOPER_TEST,
        Vo2MaxRecord.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST to
            PlatformVo2MaxMeasurementMethod.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST,
        Vo2MaxRecord.MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST to
            PlatformVo2MaxMeasurementMethod.MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST,
    )

internal val PLATFORM_TO_SDK_VO2_MAX_MEASUREMENT_METHOD =
    SDK_TO_PLATFORM_VO2_MAX_MEASUREMENT_METHOD.reversed()

internal val SDK_TO_PLATFORM_MENSTRUATION_FLOW_TYPE: Map<Int, Int> =
    mapOf(
        MenstruationFlowRecord.FLOW_LIGHT to PlatformMenstruationFlowType.FLOW_LIGHT,
        MenstruationFlowRecord.FLOW_MEDIUM to PlatformMenstruationFlowType.FLOW_MEDIUM,
        MenstruationFlowRecord.FLOW_HEAVY to PlatformMenstruationFlowType.FLOW_HEAVY,
    )

internal val PLATFORM_TO_SDK_MENSTRUATION_FLOW_TYPE =
    SDK_TO_PLATFORM_MENSTRUATION_FLOW_TYPE.reversed()

internal val SDK_TO_PLATFORM_BODY_TEMPERATURE_MEASUREMENT_LOCATION: Map<Int, Int> =
    mapOf(
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_ARMPIT to
            PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_ARMPIT,
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FINGER to
            PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FINGER,
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FOREHEAD to
            PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FOREHEAD,
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH to
            PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH,
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_RECTUM to
            PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_RECTUM,
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TEMPORAL_ARTERY to
            PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TEMPORAL_ARTERY,
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TOE to
            PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TOE,
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR to
            PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR,
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_WRIST to
            PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_WRIST,
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_VAGINA to
            PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_VAGINA,
    )

internal val PLATFORM_TO_SDK_BODY_TEMPERATURE_MEASUREMENT_LOCATION =
    SDK_TO_PLATFORM_BODY_TEMPERATURE_MEASUREMENT_LOCATION.reversed()

internal val SDK_TO_PLATFORM_BLOOD_PRESSURE_MEASUREMENT_LOCATION: Map<Int, Int> =
    mapOf(
        BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_WRIST to
            PlatformBloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST,
        BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_WRIST to
            PlatformBloodPressureMeasurementLocation
                .BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST,
        BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM to
            PlatformBloodPressureMeasurementLocation
                .BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
        BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM to
            PlatformBloodPressureMeasurementLocation
                .BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_UPPER_ARM,
    )

internal val PLATFORM_TO_SDK_BLOOD_PRESSURE_MEASUREMENT_LOCATION =
    SDK_TO_PLATFORM_BLOOD_PRESSURE_MEASUREMENT_LOCATION.reversed()

internal val SDK_TO_PLATFORM_OVULATION_TEST_RESULT: Map<Int, Int> =
    mapOf(
        OvulationTestRecord.RESULT_POSITIVE to PlatformOvulationTestResult.RESULT_POSITIVE,
        OvulationTestRecord.RESULT_HIGH to PlatformOvulationTestResult.RESULT_HIGH,
        OvulationTestRecord.RESULT_NEGATIVE to PlatformOvulationTestResult.RESULT_NEGATIVE,
        OvulationTestRecord.RESULT_INCONCLUSIVE to PlatformOvulationTestResult.RESULT_INCONCLUSIVE,
    )

internal val PLATFORM_TO_SDK_OVULATION_TEST_RESULT =
    SDK_TO_PLATFORM_OVULATION_TEST_RESULT.reversed()

internal val SDK_TO_PLATFORM_CERVICAL_MUCUS_SENSATION: Map<Int, Int> =
    mapOf(
        CervicalMucusRecord.SENSATION_LIGHT to PlatformCervicalMucusSensation.SENSATION_LIGHT,
        CervicalMucusRecord.SENSATION_MEDIUM to PlatformCervicalMucusSensation.SENSATION_MEDIUM,
        CervicalMucusRecord.SENSATION_HEAVY to PlatformCervicalMucusSensation.SENSATION_HEAVY,
    )

internal val PLATFORM_TO_SDK_CERVICAL_MUCUS_SENSATION =
    SDK_TO_PLATFORM_CERVICAL_MUCUS_SENSATION.reversed()

internal val SDK_TO_PLATFORM_SEXUAL_ACTIVITY_PROTECTION_USED: Map<Int, Int> =
    mapOf(
        SexualActivityRecord.PROTECTION_USED_PROTECTED to
            PlatformSexualActivityProtectionUsed.PROTECTION_USED_PROTECTED,
        SexualActivityRecord.PROTECTION_USED_UNPROTECTED to
            PlatformSexualActivityProtectionUsed.PROTECTION_USED_UNPROTECTED,
    )

internal val PLATFORM_TO_SDK_SEXUAL_ACTIVITY_PROTECTION_USED =
    SDK_TO_PLATFORM_SEXUAL_ACTIVITY_PROTECTION_USED.reversed()

internal val SDK_TO_PLATFORM_SKIN_TEMPERATURE_MEASUREMENT_LOCATION: Map<Int, Int> =
    mapOf(
        SkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER to
            PlatformSkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER,
        SkinTemperatureRecord.MEASUREMENT_LOCATION_TOE to
            PlatformSkinTemperatureRecord.MEASUREMENT_LOCATION_TOE,
        SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST to
            PlatformSkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST,
    )

internal val PLATFORM_TO_SDK_SKIN_TEMPERATURE_MEASUREMENT_LOCATION =
    SDK_TO_PLATFORM_SKIN_TEMPERATURE_MEASUREMENT_LOCATION.reversed()

internal val SDK_TO_PLATFORM_BLOOD_GLUCOSE_SPECIMEN_SOURCE: Map<Int, Int> =
    mapOf(
        BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID to
            PlatformBloodGlucoseSpecimenSource.SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
        BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD to
            PlatformBloodGlucoseSpecimenSource.SPECIMEN_SOURCE_CAPILLARY_BLOOD,
        BloodGlucoseRecord.SPECIMEN_SOURCE_PLASMA to
            PlatformBloodGlucoseSpecimenSource.SPECIMEN_SOURCE_PLASMA,
        BloodGlucoseRecord.SPECIMEN_SOURCE_SERUM to
            PlatformBloodGlucoseSpecimenSource.SPECIMEN_SOURCE_SERUM,
        BloodGlucoseRecord.SPECIMEN_SOURCE_TEARS to
            PlatformBloodGlucoseSpecimenSource.SPECIMEN_SOURCE_TEARS,
        BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD to
            PlatformBloodGlucoseSpecimenSource.SPECIMEN_SOURCE_WHOLE_BLOOD,
    )

internal val PLATFORM_TO_SDK_GLUCOSE_SPECIMEN_SOURCE =
    SDK_TO_PLATFORM_BLOOD_GLUCOSE_SPECIMEN_SOURCE.reversed()

internal val SDK_TO_PLATFORM_BLOOD_GLUCOSE_RELATION_TO_MEAL: Map<Int, Int> =
    mapOf(
        BloodGlucoseRecord.RELATION_TO_MEAL_GENERAL to
            PlatformBloodGlucoseRelationToMeal.RELATION_TO_MEAL_GENERAL,
        BloodGlucoseRecord.RELATION_TO_MEAL_FASTING to
            PlatformBloodGlucoseRelationToMeal.RELATION_TO_MEAL_FASTING,
        BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL to
            PlatformBloodGlucoseRelationToMeal.RELATION_TO_MEAL_BEFORE_MEAL,
        BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL to
            PlatformBloodGlucoseRelationToMeal.RELATION_TO_MEAL_AFTER_MEAL,
    )

internal val PLATFORM_TO_SDK_BLOOD_GLUCOSE_RELATION_TO_MEAL =
    SDK_TO_PLATFORM_BLOOD_GLUCOSE_RELATION_TO_MEAL.reversed()

internal val SDK_TO_PLATFORM_SLEEP_STAGE_TYPE: Map<Int, Int> =
    mapOf(
        SleepSessionRecord.STAGE_TYPE_AWAKE to PlatformSleepStageType.STAGE_TYPE_AWAKE,
        SleepSessionRecord.STAGE_TYPE_SLEEPING to PlatformSleepStageType.STAGE_TYPE_SLEEPING,
        SleepSessionRecord.STAGE_TYPE_OUT_OF_BED to
            PlatformSleepStageType.STAGE_TYPE_AWAKE_OUT_OF_BED,
        SleepSessionRecord.STAGE_TYPE_LIGHT to PlatformSleepStageType.STAGE_TYPE_SLEEPING_LIGHT,
        SleepSessionRecord.STAGE_TYPE_DEEP to PlatformSleepStageType.STAGE_TYPE_SLEEPING_DEEP,
        SleepSessionRecord.STAGE_TYPE_REM to PlatformSleepStageType.STAGE_TYPE_SLEEPING_REM,
        SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED to PlatformSleepStageType.STAGE_TYPE_AWAKE_IN_BED
    )

internal val PLATFORM_TO_SDK_SLEEP_STAGE_TYPE = SDK_TO_PLATFORM_SLEEP_STAGE_TYPE.reversed()

internal val SDK_TO_PLATFORM_EXERCISE_SEGMENT_TYPE: Map<Int, Int> =
    mapOf(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_ARM_CURL to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BACK_EXTENSION to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BACK_EXTENSION,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BALL_SLAM to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BALL_SLAM,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_PRESS to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BENCH_PRESS,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BURPEE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_CRUNCH to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_CRUNCH,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DEADLIFT to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DEADLIFT,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_ELLIPTICAL to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ELLIPTICAL,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_FORWARD_TWIST to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_FORWARD_TWIST,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_FRONT_RAISE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_FRONT_RAISE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIP_THRUST to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HIP_THRUST,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_HULA_HOOP to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HULA_HOOP,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMPING_JACK to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_JUMPING_JACK,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_JUMP_ROPE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_LATERAL_RAISE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LATERAL_RAISE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_CURL to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_CURL,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_EXTENSION to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_EXTENSION,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_PRESS to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_PRESS,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_RAISE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_RAISE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_LUNGE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LUNGE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PAUSE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_PILATES to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PILATES,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PLANK,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PULL_UP,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_PUNCH to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PUNCH,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_REST,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SIT_UP to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SIT_UP,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SQUAT,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_STRETCHING to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STRETCHING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_UPPER_TWIST to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_UPPER_TWIST,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WALKING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_WHEELCHAIR to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WHEELCHAIR,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_YOGA to
            PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_YOGA,
    )

internal val PLATFORM_TO_SDK_EXERCISE_SEGMENT_TYPE =
    SDK_TO_PLATFORM_EXERCISE_SEGMENT_TYPE.reversed()

internal val SDK_TO_PLATFORM_RECORDING_METHOD: Map<Int, Int> =
    mapOf(
        Metadata.RECORDING_METHOD_ACTIVELY_RECORDED to
            PlatformMetadata.RECORDING_METHOD_ACTIVELY_RECORDED,
        Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED to
            PlatformMetadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED,
        Metadata.RECORDING_METHOD_MANUAL_ENTRY to PlatformMetadata.RECORDING_METHOD_MANUAL_ENTRY
    )

internal val PLATFORM_TO_SDK_RECORDING_METHOD: Map<Int, Int> =
    SDK_TO_PLATFORM_RECORDING_METHOD.reversed()

internal fun Int.toPlatformCervicalMucusAppearance(): Int {
    return SDK_TO_PLATFORM_CERVICAL_MUCUS_APPEARANCE[this]
        ?: PlatformCervicalMucusAppearance.APPEARANCE_UNKNOWN
}

internal fun Int.toPlatformBloodPressureBodyPosition(): Int {
    return SDK_TO_PLATFORM_BLOOD_PRESSURE_BODY_POSITION[this]
        ?: PlatformBloodPressureBodyPosition.BODY_POSITION_UNKNOWN
}

internal fun Int.toPlatformExerciseSessionType(): Int {
    return SDK_TO_PLATFORM_EXERCISE_SESSION_TYPE[this]
        ?: PlatformExerciseSessionType.EXERCISE_SESSION_TYPE_UNKNOWN
}

internal fun Int.toPlatformExerciseSegmentType(): Int {
    return SDK_TO_PLATFORM_EXERCISE_SEGMENT_TYPE[this]
        ?: PlatformExerciseSegmentType.EXERCISE_SEGMENT_TYPE_UNKNOWN
}

internal fun Int.toPlatformMealType(): Int {
    return SDK_TO_PLATFORM_MEAL_TYPE[this] ?: PlatformMealType.MEAL_TYPE_UNKNOWN
}

internal fun Int.toPlatformVo2MaxMeasurementMethod(): Int {
    return SDK_TO_PLATFORM_VO2_MAX_MEASUREMENT_METHOD[this]
        ?: PlatformVo2MaxMeasurementMethod.MEASUREMENT_METHOD_OTHER
}

internal fun Int.toPlatformMenstruationFlow(): Int {
    return SDK_TO_PLATFORM_MENSTRUATION_FLOW_TYPE[this] ?: PlatformMenstruationFlowType.FLOW_UNKNOWN
}

internal fun Int.toPlatformBodyTemperatureMeasurementLocation(): Int {
    return SDK_TO_PLATFORM_BODY_TEMPERATURE_MEASUREMENT_LOCATION[this]
        ?: PlatformBodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN
}

internal fun Int.toPlatformBloodPressureMeasurementLocation(): Int {
    return SDK_TO_PLATFORM_BLOOD_PRESSURE_MEASUREMENT_LOCATION[this]
        ?: PlatformBloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_UNKNOWN
}

internal fun Int.toPlatformOvulationTestResult(): Int {
    return SDK_TO_PLATFORM_OVULATION_TEST_RESULT[this]
        ?: PlatformOvulationTestResult.RESULT_INCONCLUSIVE
}

internal fun Int.toPlatformCervicalMucusSensation(): Int {
    return SDK_TO_PLATFORM_CERVICAL_MUCUS_SENSATION[this]
        ?: PlatformCervicalMucusSensation.SENSATION_UNKNOWN
}

internal fun Int.toPlatformSexualActivityProtectionUsed(): Int {
    return SDK_TO_PLATFORM_SEXUAL_ACTIVITY_PROTECTION_USED[this]
        ?: PlatformSexualActivityProtectionUsed.PROTECTION_USED_UNKNOWN
}

internal fun Int.toPlatformSkinTemperatureMeasurementLocation(): Int {
    return SDK_TO_PLATFORM_SKIN_TEMPERATURE_MEASUREMENT_LOCATION[this]
        ?: PlatformSkinTemperatureRecord.MEASUREMENT_LOCATION_UNKNOWN
}

internal fun Int.toPlatformBloodGlucoseSpecimenSource(): Int {
    return SDK_TO_PLATFORM_BLOOD_GLUCOSE_SPECIMEN_SOURCE[this]
        ?: PlatformBloodGlucoseSpecimenSource.SPECIMEN_SOURCE_UNKNOWN
}

internal fun Int.toPlatformBloodGlucoseRelationToMeal(): Int {
    return SDK_TO_PLATFORM_BLOOD_GLUCOSE_RELATION_TO_MEAL[this]
        ?: PlatformBloodGlucoseRelationToMeal.RELATION_TO_MEAL_UNKNOWN
}

internal fun Int.toPlatformSleepStageType(): Int {
    return SDK_TO_PLATFORM_SLEEP_STAGE_TYPE[this] ?: PlatformSleepStageType.STAGE_TYPE_UNKNOWN
}

internal fun Int.toPlatformRecordingMethod(): Int {
    return SDK_TO_PLATFORM_RECORDING_METHOD[this] ?: PlatformMetadata.RECORDING_METHOD_UNKNOWN
}

internal fun Int.toSdkBloodPressureBodyPosition(): Int {
    return PLATFORM_TO_SDK_BLOOD_PRESSURE_BODY_POSITION[this]
        ?: BloodPressureRecord.BODY_POSITION_UNKNOWN
}

internal fun Int.toSdkBloodPressureMeasurementLocation(): Int {
    return PLATFORM_TO_SDK_BLOOD_PRESSURE_MEASUREMENT_LOCATION[this]
        ?: BloodPressureRecord.MEASUREMENT_LOCATION_UNKNOWN
}

internal fun Int.toSdkExerciseSessionType(): Int {
    return PLATFORM_TO_SDK_EXERCISE_SESSION_TYPE[this]
        ?: ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
}

internal fun Int.toSdkExerciseSegmentType(): Int {
    return PLATFORM_TO_SDK_EXERCISE_SEGMENT_TYPE[this]
        ?: ExerciseSegment.EXERCISE_SEGMENT_TYPE_UNKNOWN
}

internal fun Int.toSdkVo2MaxMeasurementMethod(): Int {
    return PLATFORM_TO_SDK_VO2_MAX_MEASUREMENT_METHOD[this] ?: Vo2MaxRecord.MEASUREMENT_METHOD_OTHER
}

internal fun Int.toSdkMenstruationFlow(): Int {
    return PLATFORM_TO_SDK_MENSTRUATION_FLOW_TYPE[this] ?: MenstruationFlowRecord.FLOW_UNKNOWN
}

internal fun Int.toSdkProtectionUsed(): Int {
    return PLATFORM_TO_SDK_SEXUAL_ACTIVITY_PROTECTION_USED[this]
        ?: SexualActivityRecord.PROTECTION_USED_UNKNOWN
}

internal fun Int.toSdkCervicalMucusSensation(): Int {
    return PLATFORM_TO_SDK_CERVICAL_MUCUS_SENSATION[this] ?: CervicalMucusRecord.SENSATION_UNKNOWN
}

internal fun Int.toSdkBloodGlucoseSpecimenSource(): Int {
    return PLATFORM_TO_SDK_GLUCOSE_SPECIMEN_SOURCE[this]
        ?: BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN
}

internal fun Int.toSdkMealType(): Int {
    return PLATFORM_TO_SDK_MEAL_TYPE[this] ?: MealType.MEAL_TYPE_UNKNOWN
}

internal fun Int.toSdkOvulationTestResult(): Int {
    return PLATFORM_TO_SDK_OVULATION_TEST_RESULT[this] ?: OvulationTestRecord.RESULT_INCONCLUSIVE
}

internal fun Int.toSdkRelationToMeal(): Int {
    return PLATFORM_TO_SDK_BLOOD_GLUCOSE_RELATION_TO_MEAL[this]
        ?: BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN
}

internal fun Int.toSdkBodyTemperatureMeasurementLocation(): Int {
    return PLATFORM_TO_SDK_BODY_TEMPERATURE_MEASUREMENT_LOCATION[this]
        ?: BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN
}

internal fun Int.toSdkCervicalMucusAppearance(): Int {
    return PLATFORM_TO_SDK_CERVICAL_MUCUS_APPEARANCE[this] ?: CervicalMucusRecord.APPEARANCE_UNKNOWN
}

internal fun Int.toSdkSleepStageType(): Int {
    return PLATFORM_TO_SDK_SLEEP_STAGE_TYPE[this] ?: SleepSessionRecord.STAGE_TYPE_UNKNOWN
}

internal fun Int.toSdkSkinTemperatureMeasurementLocation(): Int {
    return PLATFORM_TO_SDK_SKIN_TEMPERATURE_MEASUREMENT_LOCATION[this]
        ?: SkinTemperatureRecord.MEASUREMENT_LOCATION_UNKNOWN
}

internal fun Int.toSdkRecordingMethod(): Int {
    return PLATFORM_TO_SDK_RECORDING_METHOD[this] ?: Metadata.RECORDING_METHOD_UNKNOWN
}

private fun Map<Int, Int>.reversed(): Map<Int, Int> {
    return entries.associate { (k, v) -> v to k }
}
