package androidx.health.connect.client.records

import android.os.Build
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.celsius
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.metersPerSecond
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class RecordSdkExtensionTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun plannedExerciseSessionRecord_onU_withoutExtension13_doesNotCrash() {
        PlannedExerciseSessionRecord(
            startTime = Instant.ofEpochMilli(1234L),
            startZoneOffset = null,
            endTime = Instant.ofEpochMilli(1235L),
            endZoneOffset = null,
            metadata = Metadata.manualEntry(),
            blocks = listOf(),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun skinTemperatureRecord_onU_withoutExtension13_doesNotCrash() {
        SkinTemperatureRecord(
            startTime = Instant.ofEpochMilli(1234L),
            startZoneOffset = null,
            endTime = Instant.ofEpochMilli(1235L),
            endZoneOffset = null,
            metadata = Metadata.manualEntry(),
            deltas = listOf(),
            baseline = 37.celsius,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun mindfulnessSessionRecord_onU_withoutExtension15_doesNotCrash() {
        MindfulnessSessionRecord(
            startTime = Instant.ofEpochMilli(1234L),
            startZoneOffset = null,
            endTime = Instant.ofEpochMilli(1235L),
            endZoneOffset = null,
            metadata = Metadata.manualEntry(),
            mindfulnessSessionType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun activityIntensityRecord_onU_withoutExtension16_doesNotCrash() {
        ActivityIntensityRecord(
            startTime = Instant.ofEpochMilli(1234L),
            startZoneOffset = null,
            endTime = Instant.ofEpochMilli(1235L),
            endZoneOffset = null,
            metadata = Metadata.manualEntry(),
            activityIntensityType = ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun exerciseSessionRecord_withPlannedId_onU_withoutExtension13_doesNotCrash() {
        ExerciseSessionRecord(
            startTime = Instant.ofEpochMilli(1234L),
            startZoneOffset = null,
            endTime = Instant.ofEpochMilli(1235L),
            endZoneOffset = null,
            metadata = Metadata.manualEntry(),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            plannedExerciseSessionId = "some_id",
            exerciseRoute = null,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun nutritionRecord_withTransFat_onU_withoutExtension10_doesNotCrash() {
        NutritionRecord(
            startTime = Instant.ofEpochMilli(1234L),
            startZoneOffset = null,
            endTime = Instant.ofEpochMilli(1235L),
            endZoneOffset = null,
            metadata = Metadata.manualEntry(),
            transFat = 10.grams,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun speedRecord_onU_withoutExtension10_doesNotCrash() {
        SpeedRecord(
            startTime = Instant.ofEpochMilli(1234L),
            startZoneOffset = null,
            endTime = Instant.ofEpochMilli(1235L),
            endZoneOffset = null,
            metadata = Metadata.manualEntry(),
            samples =
                listOf(
                    SpeedRecord.Sample(
                        time = Instant.ofEpochMilli(1234L),
                        speed = 10.metersPerSecond,
                    )
                ),
        )
    }
}
