package androidx.wear.ongoing

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import androidx.versionedparcelable.ParcelUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(PatchedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Build.VERSION_CODES.Q])
open class OngoingActivityPartTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testTextOngoingActivityStatusBasic() {
        val text = "Text"
        val textStatus = Status.TextPart(text)

        assertEquals(Long.MAX_VALUE, textStatus.getNextChangeTimeMillis(0))

        assertEquals(text, textStatus.getText(context, 0))

        assertEquals(Status.TextPart(text), textStatus)
        assertNotEquals(Status.TextPart("Other"), textStatus)

        assertEquals(Status.TextPart(text).hashCode(), textStatus.hashCode())
        assertEquals(
            Status.TextPart("Other").hashCode(),
            Status.TextPart("Other").hashCode()
        )
    }

    @Test
    fun testTimerOngoingActivityStatusBasic() {
        assertEquals(
            Status.StopwatchPart(1234L),
            Status.StopwatchPart(1234L)
        )
        assertEquals(
            Status.StopwatchPart(1234L).hashCode(),
            Status.StopwatchPart(1234L).hashCode()
        )
        assertNotEquals(
            Status.StopwatchPart(1234L),
            Status.StopwatchPart(1235L)
        )

        assertEquals(
            Status.TimerPart(1234L),
            Status.TimerPart(1234L)
        )
        assertEquals(
            Status.TimerPart(1234L).hashCode(),
            Status.TimerPart(1234L).hashCode()
        )
        assertNotEquals(
            Status.StopwatchPart(1234L),
            Status.TimerPart(1234L)
        )

        assertEquals(
            Status.TimerPart(1234L, 5678L),
            Status.TimerPart(1234L, 5678L)
        )
        assertEquals(
            Status.TimerPart(1234L, 5678L).hashCode(),
            Status.TimerPart(1234L, 5678L).hashCode()
        )
        assertNotEquals(
            Status.TimerPart(1234L, 5678L),
            Status.TimerPart(1234L, 5679L)
        )
        assertNotEquals(
            Status.TimerPart(1234L, 5678L),
            Status.TimerPart(1234L)
        )

        assertEquals(
            Status.TimerPart(1234L, 5678L, 100L),
            Status.TimerPart(1234L, 5678L, 100L)
        )
        assertEquals(
            Status.TimerPart(1234L, 5678L, 100L).hashCode(),
            Status.TimerPart(1234L, 5678L, 100L).hashCode()
        )
        assertNotEquals(
            Status.TimerPart(1234L, 5678L, 100L),
            Status.TimerPart(1234L, 5678L, 101L)
        )
        assertNotEquals(
            Status.TimerPart(1234L, 5678L, 100L),
            Status.TimerPart(1234L, 5678L)
        )
        assertNotEquals(
            Status.TimerPart(1234L, 5678L, 100L),
            Status.TimerPart(1234L)
        )
    }

    @Test
    fun testOngoingActivityStatusSerialization() {
        val key = "KEY"
        listOf(
            Status.StopwatchPart(1234L),
            Status.TextPart("Text1"),
            Status.TimerPart(1234L),
            Status.TimerPart(1234L, 5678L),
            Status.TextPart("Text2"),
            Status.StopwatchPart(1234L, 5678L, 100L)
        ).forEach { original ->
            val bundle = Bundle()
            ParcelUtils.putVersionedParcelable(bundle, key, original.toVersionedParcelable())

            val p = Parcel.obtain()
            p.writeParcelable(bundle, 0)
            p.setDataPosition(0)

            val receivedBundle = p.readParcelable<Bundle>(Bundle::class.java.classLoader)!!

            val received = Status.Part.fromVersionedParcelable(
                ParcelUtils.getVersionedParcelable<StatusPart>(receivedBundle, key)
            )!!
            assertEquals(original, received)
            assertEquals(original.hashCode(), received.hashCode())
        }
    }

    @Test
    fun testTimerOngoingActivityStatusGetters() {
        Status.StopwatchPart(123L).also {
            assertEquals(123L, it.timeZeroMillis)
            assertFalse(it.isCountDown)
            assertFalse(it.isPaused)
            assertFalse(it.hasTotalDuration())
        }

        Status.TimerPart(12345L).also {
            assertEquals(12345L, it.timeZeroMillis)
            assertTrue(it.isCountDown)
            assertFalse(it.isPaused)
            assertFalse(it.hasTotalDuration())
        }

        Status.StopwatchPart(2345L, 3456L).also {
            assertEquals(2345L, it.timeZeroMillis)
            assertFalse(it.isCountDown)
            assertTrue(it.isPaused)
            assertEquals(3456L, it.pausedAtMillis)
            assertFalse(it.hasTotalDuration())
        }

        Status.TimerPart(4567L, 7890L, 12000L).also {
            assertEquals(4567L, it.timeZeroMillis)
            assertTrue(it.isCountDown)
            assertTrue(it.isPaused)
            assertEquals(7890L, it.pausedAtMillis)
            assertTrue(it.hasTotalDuration())
            assertEquals(12000L, it.totalDurationMillis)
        }
    }

    @Test
    fun testTimerOngoingActivityStatusChronometer() {
        // Create a chronometer, starting at the given timestamp (around 2 minutes after
        // timestamp 0).
        val t0 = 123456L
        val timerStatus =
            Status.StopwatchPart(/* timeZeroMillis = */ t0)

        // The chronometer is not paused.
        assertFalse(timerStatus.isPaused)

        // The chronometer will always change at timestamps ending in 456.
        assertEquals(456L, timerStatus.getNextChangeTimeMillis(0L))
        assertEquals(456L, timerStatus.getNextChangeTimeMillis(455L))
        assertEquals(1456L, timerStatus.getNextChangeTimeMillis(456L))
        assertEquals(1456L, timerStatus.getNextChangeTimeMillis(457L))
        assertEquals(1456L, timerStatus.getNextChangeTimeMillis(1000L))

        assertEquals(t0, timerStatus.getNextChangeTimeMillis(t0 - 1))
        assertEquals(t0 + 1000, timerStatus.getNextChangeTimeMillis(t0))

        // Check default formatting.
        assertEquals("00:00", timerStatus.getText(context, t0))
        assertEquals("00:00", timerStatus.getText(context, t0 + 999))
        assertEquals("00:01", timerStatus.getText(context, t0 + 1000))

        assertEquals("00:59", timerStatus.getText(context, t0 + 60 * 1000 - 1))
        assertEquals("01:00", timerStatus.getText(context, t0 + 60 * 1000))

        assertEquals("59:59", timerStatus.getText(context, t0 + 3600 * 1000 - 1))
        assertEquals("1:00:00", timerStatus.getText(context, t0 + 3600 * 1000))
    }

    @Test
    fun testTimerOngoingActivityStatusTimer() {
        // Create a timer, set to expire at the given timestamp (around 2 minutes after
        // timestamp 0).
        val t0 = 123456L
        val timerStatus = Status.TimerPart(t0)

        // The Timer is not paused.
        assertFalse(timerStatus.isPaused)

        // The timer will always change at timestamps ending in 456.
        assertEquals(456L, timerStatus.getNextChangeTimeMillis(0L))
        assertEquals(456L, timerStatus.getNextChangeTimeMillis(455L))
        assertEquals(1456L, timerStatus.getNextChangeTimeMillis(456L))
        assertEquals(1456L, timerStatus.getNextChangeTimeMillis(457L))
        assertEquals(1456L, timerStatus.getNextChangeTimeMillis(1000L))

        assertEquals(t0, timerStatus.getNextChangeTimeMillis(t0 - 1))
        assertEquals(t0 + 1000, timerStatus.getNextChangeTimeMillis(t0))

        // Check default formatting.
        assertEquals("00:01", timerStatus.getText(context, t0 - 1))
        assertEquals("00:00", timerStatus.getText(context, t0))
        assertEquals("00:00", timerStatus.getText(context, t0 + 999))
        assertEquals("-00:01", timerStatus.getText(context, t0 + 1000))

        assertEquals("02:00", timerStatus.getText(context, 3456L))

        assertEquals("-1:00:00", timerStatus.getText(context, t0 + 3600 * 1000))
    }

    @Test
    fun testTimerOngoingActivityStatusChronometerPaused() {
        val t0 = 123456L
        var timerStatus = Status.StopwatchPart(
            /* timeZeroMillis = */ t0,
            /* pausedAt = */ t0 + 1999
        )

        // The Timer is paused.
        assertTrue(timerStatus.isPaused())
        assertEquals(t0 + 1999, timerStatus.pausedAtMillis)

        // The timer is paused, will never change.
        assertEquals(Long.MAX_VALUE, timerStatus.getNextChangeTimeMillis(0L))
        assertEquals(Long.MAX_VALUE, timerStatus.getNextChangeTimeMillis(t0))
        assertEquals(Long.MAX_VALUE, timerStatus.getNextChangeTimeMillis(t0 + 3600 * 1000))

        // Check formatting. Current Time doesn't mater.
        assertEquals("00:01", timerStatus.getText(context, 0))
        assertEquals("00:01", timerStatus.getText(context, t0))
        assertEquals("00:01", timerStatus.getText(context, t0 + 2000))
        assertEquals("00:01", timerStatus.getText(context, t0 + 3600 * 1000))
    }

    @Test
    fun testTimerOngoingActivityStatusTimerPaused() {
        val t0 = 123456L
        var timerStatus = Status.TimerPart(
            /* timeZeroMillis = */ t0,
            /* pausedAt = */ t0 + 1999
        )

        // The Timer is paused.
        assertTrue(timerStatus.isPaused())
        assertEquals(t0 + 1999, timerStatus.pausedAtMillis)

        // The timer is paused, will never change.
        assertEquals(Long.MAX_VALUE, timerStatus.getNextChangeTimeMillis(0L))
        assertEquals(Long.MAX_VALUE, timerStatus.getNextChangeTimeMillis(t0))
        assertEquals(Long.MAX_VALUE, timerStatus.getNextChangeTimeMillis(t0 + 3600 * 1000))

        // Check formatting. Current Time doesn't mater.
        assertEquals("-00:01", timerStatus.getText(context, 0))
        assertEquals("-00:01", timerStatus.getText(context, t0))
        assertEquals("-00:01", timerStatus.getText(context, t0 + 2000))
        assertEquals("-00:01", timerStatus.getText(context, t0 + 3600 * 1000))
    }
}
