package androidx.health.connect.client.impl.converters.records

import androidx.health.connect.client.records.SleepStageRecord
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SuppressWarnings("GoodTime") // Safe to use in test
private val START_TIME = Instant.ofEpochMilli(1234L)
@SuppressWarnings("GoodTime") // Safe to use in test
private val END_TIME = Instant.ofEpochMilli(5678L)
@SuppressWarnings("GoodTime") // Safe to use in test
private val START_ZONE_OFFSET = ZoneOffset.ofHours(1)
@SuppressWarnings("GoodTime") // Safe to use in test
private val END_ZONE_OFFSET = ZoneOffset.ofHours(2)

@RunWith(JUnit4::class)
class SleepStageConverterTest {

    // Because the APK schema has stage type required
    // we will need to serialize "unknown" explicitly.
    @Test
    fun unknownStageType_serializesUnknown() {
        val record =
            SleepStageRecord(
                stage = SleepStageRecord.STAGE_TYPE_UNKNOWN,
                startTime = START_TIME,
                startZoneOffset = START_ZONE_OFFSET,
                endTime = END_TIME,
                endZoneOffset = END_ZONE_OFFSET,
            )
        val proto = record.toProto()
        assertThat(proto.valuesMap).containsExactly("stage", enumVal("unknown"))
    }
}
