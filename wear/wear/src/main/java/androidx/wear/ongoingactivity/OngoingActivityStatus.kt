package androidx.wear.ongoingactivity

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import androidx.annotation.RestrictTo
import java.util.Objects

/**
 * Base class to serialize / deserialize [OngoingActivityStatus] into / from a Notification
 *
 * The classes here use timestamps for updating the displayed representation of the status, in
 * cases when this is needed (chronometers), as returned by
 * [android.os.SystemClock.elapsedRealtime].
 */
public abstract class OngoingActivityStatus {
    /**
     * Returns a textual representation of the ongoing activity status at the given time
     * represented as milliseconds timestamp
     *
     * For forward compatibility, the best way to display this is on a [TextView]
     * @param context may be used for internationalization. Only used while this method executed.
     * @param timeNowMillis the timestamp of the time we want to display, usually now, as
     * returned by [android.os.SystemClock.elapsedRealtime].
     */
    public abstract fun getText(context: Context, timeNowMillis: Long): CharSequence

    /**
     * Returns the timestamp of the next time when the display will be different from the current
     * one.
     *
     * @param fromTimeMillis current time, usually now as returned by
     * [android.os.SystemClock.elapsedRealtime]. In most cases [getText] and
     * [getNextChangeTimeMillis] should be called with the exact same timestamp, so changes
     * are not missed.
     * @return the first point in time after [fromTimeMillis] when the displayed value of this
     * status changes. returns Long.MAX_VALUE if the display will never change.
     */
    public abstract fun getNextChangeTimeMillis(fromTimeMillis: Long): Long

    /**
     * Serializes the information into the given [Bundle].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract fun extend(bundle: Bundle)

    public companion object {
        /**
         * Deserializes the information from the given [Bundle].
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun create(bundle: Bundle): OngoingActivityStatus? =
            if (bundle.getBoolean(KEY_USE_CHRONOMETER, false)) {
                TimerOngoingActivityStatus(
                    bundle.getLong(KEY_TIME_ZERO),
                    bundle.getBoolean(KEY_COUNT_DOWN, false),
                    bundle.getLong(KEY_PAUSED_AT, LONG_DEFAULT),
                    bundle.getLong(KEY_TOTAL_DURATION, LONG_DEFAULT)
                )
            } else {
                bundle[KEY_STATUS]?.let { TextOngoingActivityStatus(it.toString()) }
            }
    }
}

/**
 * Status representing a plain, static text.
 */
public class TextOngoingActivityStatus(private val str: String) : OngoingActivityStatus() {
    /**
     * See [OngoingActivityStatus.getText]
     */
    override fun getText(context: Context, timeNowMillis: Long): CharSequence = str

    /**
     * See [OngoingActivityStatus.getNextChangeTimeMillis]
     */
    override fun getNextChangeTimeMillis(fromTimeMillis: Long): Long = Long.MAX_VALUE

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun extend(bundle: Bundle) {
        bundle.putString(KEY_STATUS, str)
    }

    override fun equals(other: Any?): Boolean =
        other is TextOngoingActivityStatus && str == other.str
    override fun hashCode(): Int = str.hashCode()
}

/**
 * Status representing a timer or stopwatch.
 *
 * @param timeZeroMillis timestamp of the time at which this Timer should display 0, will be in the
 * past for a stopwatch and usually in the future for timers.
 * @param countDown indicates if this is a stopwatch (when `false`) or timer (when
 * `true`).
 * @param pausedAtMillis timestamp of the time when this timer was paused. Or `-1L` if this
 * timer is running.
 * @param totalDurationMillis total duration of this timer/stopwatch, useful to display as a
 * progress bar or similar.
 */
public class TimerOngoingActivityStatus(
    public val timeZeroMillis: Long,
    public val countDown: Boolean = false,
    public val pausedAtMillis: Long = LONG_DEFAULT,
    public val totalDurationMillis: Long = LONG_DEFAULT
) : OngoingActivityStatus() {
    private val stringBuffer = StringBuilder(8)
    private val negativeDurationPrefix = "-"

    /**
     * See [OngoingActivityStatus.getNextChangeTimeMillis]
     */
    public override fun getNextChangeTimeMillis(fromTimeMillis: Long): Long = if (isPaused()) {
        Long.MAX_VALUE
    } else {
        // We always want to return a value:
        //    * Strictly greater than fromTimeMillis.
        //    * Has the same millis as timeZero.
        //    * It's as small as possible.
        fromTimeMillis + ((timeZeroMillis - fromTimeMillis) % 1000 + 1999) % 1000 + 1
    }

    /**
     * Determines if this timer is paused. i.e. the display representation will not change.
     *
     * @return `true` if this timer is paused, `false` if it's running.
     */
    public fun isPaused(): Boolean = pausedAtMillis >= 0L

    /**
     * See [OngoingActivityStatus.getText]
     */
    override fun getText(context: Context, timeNowMillis: Long): CharSequence {
        val timeMillis = if (isPaused()) pausedAtMillis else timeNowMillis
        val milliSeconds = timeMillis - timeZeroMillis
        var seconds = if (milliSeconds >= 0) {
            milliSeconds / 1000
        } else {
            // Always round down (instead of the default round to 0) so all values are displayed
            // for 1 second.
            (milliSeconds - 999) / 1000
        }
        if (countDown) {
            seconds = -seconds
        }

        var prefix = ""
        if (seconds < 0) {
            seconds = -seconds
            prefix = negativeDurationPrefix
        }
        return prefix + DateUtils.formatElapsedTime(stringBuffer, seconds)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun extend(bundle: Bundle) {
        bundle.putBoolean(KEY_USE_CHRONOMETER, true)
        bundle.putLong(KEY_TIME_ZERO, timeZeroMillis)
        bundle.putBoolean(KEY_COUNT_DOWN, countDown)
        if (totalDurationMillis != LONG_DEFAULT) {
            bundle.putLong(KEY_TOTAL_DURATION, totalDurationMillis)
        }
        if (pausedAtMillis != LONG_DEFAULT) {
            bundle.putLong(KEY_PAUSED_AT, pausedAtMillis)
        }
    }

    override fun equals(other: Any?): Boolean = other is TimerOngoingActivityStatus &&
        timeZeroMillis == other.timeZeroMillis &&
        countDown == other.countDown &&
        pausedAtMillis == other.pausedAtMillis &&
        totalDurationMillis == other.totalDurationMillis

    override fun hashCode(): Int = Objects.hash(
        timeZeroMillis,
        countDown,
        pausedAtMillis,
        totalDurationMillis
    )
}

private const val KEY_COUNT_DOWN = "countdown"
private const val KEY_TIME_ZERO = "timeZero"
private const val KEY_USE_CHRONOMETER = "useChronometer"
private const val KEY_STATUS = "status"
private const val KEY_TOTAL_DURATION = "totalDuration"
private const val KEY_PAUSED_AT = "pausedAt"

// Invalid value to use for paused_at and duration, as suggested by api guidelines 5.15
private const val LONG_DEFAULT = -1L
