/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.background
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.foundation.sizeIn
import androidx.wear.compose.material3.TimeTextDefaults.timeFormat
import androidx.wear.compose.material3.TimeTextDefaults.timeTextStyle
import androidx.wear.compose.materialcore.currentTimeMillis
import androidx.wear.compose.materialcore.is24HourFormat
import java.util.Calendar
import java.util.Locale

/**
 * Layout to show the current time and a label, they will be drawn in a curve, following the top
 * edge of the screen.
 *
 * Note that Wear Material UX guidance recommends that time text should not be larger than
 * [TimeTextDefaults.MaxSweepAngle] of the screen edge, which is enforced by default. It is
 * recommended that additional content, if any, is limited to short status messages before the time
 * using the MaterialTheme.colorScheme.primary color.
 *
 * For more information, see the
 * [Curved Text](https://developer.android.com/training/wearables/components/curved-text) guide.
 *
 * A simple [TimeText] which shows the current time:
 *
 * @sample androidx.wear.compose.material3.samples.TimeTextClockOnly
 *
 * A [TimeText] with a short app status message shown:
 *
 * @sample androidx.wear.compose.material3.samples.TimeTextWithStatus
 *
 * A [TimeText] with a long status message, that needs ellipsizing:
 *
 * @sample androidx.wear.compose.material3.samples.TimeTextWithStatusEllipsized
 * @param modifier The modifier to be applied to the component.
 * @param curvedModifier The [CurvedModifier] used to restrict the arc in which [TimeText] is drawn.
 * @param maxSweepAngle The default maximum sweep angle in degrees.
 * @param backgroundColor The background color of the arc drawn behind the [TimeText].
 * @param timeSource [TimeSource] which retrieves the current time and formats it.
 * @param contentPadding The spacing values between the container and the content.
 * @param content The content of the [TimeText] - displays the current time by default. This lambda
 *   receives the current time as a String and should display it using curvedText. Note that if long
 *   curved text is included here, it should specify [CurvedModifier.weight] on it so that the space
 *   available is suitably allocated.
 */
@Composable
public fun TimeText(
    modifier: Modifier = Modifier,
    curvedModifier: CurvedModifier = CurvedModifier,
    maxSweepAngle: Float = TimeTextDefaults.MaxSweepAngle,
    backgroundColor: Color = TimeTextDefaults.backgroundColor(),
    timeSource: TimeSource = TimeTextDefaults.rememberTimeSource(timeFormat()),
    contentPadding: PaddingValues = TimeTextDefaults.ContentPadding,
    content: CurvedScope.(String) -> Unit = { time -> timeTextCurvedText(time) }
) {
    val currentTime = timeSource.currentTime()

    CurvedLayout(modifier = modifier) {
        curvedRow(
            modifier =
                curvedModifier
                    .sizeIn(maxSweepDegrees = maxSweepAngle)
                    .padding(contentPadding.toArcPadding())
                    .background(backgroundColor, StrokeCap.Round),
            radialAlignment = CurvedAlignment.Radial.Center
        ) {
            content(currentTime)
        }
    }
}

/** Contains the default values used by [TimeText]. */
public object TimeTextDefaults {
    /** The default padding from the edge of the screen. */
    private val Padding = PaddingDefaults.edgePadding

    /** Default format for 24h clock. */
    public const val TimeFormat24Hours: String = "HH:mm"

    /** Default format for 12h clock. */
    public const val TimeFormat12Hours: String = "h:mm"

    /**
     * The default maximum sweep angle in degrees used by [TimeText].
     *
     * This is calculated by keeping the length of the corresponding chord on the circle to be
     * approximately 57% of the screen width.
     */
    public const val MaxSweepAngle: Float = 70f

    /** The default content padding used by [TimeText]. */
    public val ContentPadding: PaddingValues = PaddingValues(top = Padding)

    /**
     * Retrieves default timeFormat for the device. Depending on settings, it can be either 12h or
     * 24h format.
     */
    @Composable
    public fun timeFormat(): String {
        val format = if (is24HourFormat()) TimeFormat24Hours else TimeFormat12Hours
        return DateFormat.getBestDateTimePattern(Locale.getDefault(), format)
            .replace("a", "")
            .trim()
    }

    /**
     * Creates a [CurvedTextStyle] with default parameters used for showing time. By default a copy
     * of MaterialTheme.typography.arcMedium style is created.
     *
     * @param background The background color.
     * @param color The main color.
     * @param fontSize The font size.
     */
    @Composable
    public fun timeTextStyle(
        background: Color = Color.Unspecified,
        color: Color = MaterialTheme.colorScheme.onBackground,
        fontSize: TextUnit = TextUnit.Unspecified,
    ): CurvedTextStyle =
        MaterialTheme.typography.arcMedium +
            CurvedTextStyle(color = color, background = background, fontSize = fontSize)

    /**
     * Creates a default implementation of [TimeSource] and remembers it. Once the system time
     * changes, it triggers an update of the [TimeSource.currentTime] which is formatted using
     * [timeFormat] param.
     *
     * [DefaultTimeSource] for Android uses [android.text.format.DateFormat] [timeFormat] should
     * follow the standard
     * [Date and Time patterns](https://developer.android.com/reference/java/text/SimpleDateFormat#date-and-time-patterns)
     * Examples: "h:mm a" - 12:08 PM "yyyy.MM.dd HH:mm:ss" - 2021.11.01 14:08:56 More examples can
     * be found [here](https://developer.android.com/reference/java/text/SimpleDateFormat#examples).
     *
     * @param timeFormat Date and time string pattern.
     */
    @Composable
    public fun rememberTimeSource(timeFormat: String): TimeSource =
        remember(timeFormat) { DefaultTimeSource(timeFormat) }

    /**
     * The recommended background color to use when displaying curved text so it is visible on top
     * of other content.
     */
    @Composable public fun backgroundColor(): Color = CurvedTextDefaults.backgroundColor()
}

/**
 * Default curved text to use in a [TimeText], for displaying the time
 *
 * @param time The time to display.
 * @param style A [CurvedTextStyle] to override the style used.
 */
public fun CurvedScope.timeTextCurvedText(time: String, style: CurvedTextStyle? = null) {
    basicCurvedText(
        time,
    ) {
        style?.let { timeTextStyle() + it } ?: timeTextStyle()
    }
}

/**
 * A default implementation of Separator, to be shown between any text/composable and the time.
 *
 * @param curvedTextStyle A [CurvedTextStyle] for the separator.
 * @param contentArcPadding [ArcPaddingValues] for the separator text.
 */
public fun CurvedScope.timeTextSeparator(
    curvedTextStyle: CurvedTextStyle? = null,
    contentArcPadding: ArcPaddingValues = ArcPaddingValues(angular = 4.dp)
) {
    curvedText(
        text = "·",
        style = curvedTextStyle,
        modifier = CurvedModifier.padding(contentArcPadding)
    )
}

public interface TimeSource {

    /**
     * A method responsible for returning updated time string.
     *
     * @return Formatted time string.
     */
    @Composable public fun currentTime(): String
}

internal class DefaultTimeSource(timeFormat: String) : TimeSource {
    private val _timeFormat = timeFormat

    @Composable
    override fun currentTime(): String = currentTime({ currentTimeMillis() }, _timeFormat).value
}

@Composable
@VisibleForTesting
internal fun currentTime(time: () -> Long, timeFormat: String): State<String> {

    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    var currentTime by remember { mutableLongStateOf(time()) }

    val timeText = remember { derivedStateOf { formatTime(calendar, currentTime, timeFormat) } }

    val context = LocalContext.current
    val updatedTimeLambda by rememberUpdatedState(time)

    DisposableEffect(context, updatedTimeLambda) {
        val receiver =
            TimeBroadcastReceiver(
                onTimeChanged = { currentTime = updatedTimeLambda() },
                onTimeZoneChanged = { calendar = Calendar.getInstance() }
            )
        receiver.register(context)
        onDispose { receiver.unregister(context) }
    }
    return timeText
}

/** An extension function, which converts [PaddingValues] into [ArcPaddingValues]. */
private fun PaddingValues.toArcPadding() =
    object : ArcPaddingValues {
        override fun calculateOuterPadding(radialDirection: CurvedDirection.Radial) =
            calculateTopPadding()

        override fun calculateInnerPadding(radialDirection: CurvedDirection.Radial) =
            calculateBottomPadding()

        override fun calculateAfterPadding(
            layoutDirection: LayoutDirection,
            angularDirection: CurvedDirection.Angular
        ) = calculateRightPadding(layoutDirection)

        override fun calculateBeforePadding(
            layoutDirection: LayoutDirection,
            angularDirection: CurvedDirection.Angular
        ) = calculateLeftPadding(layoutDirection)
    }

/** A [BroadcastReceiver] to receive time tick, time change, and time zone change events. */
private class TimeBroadcastReceiver(
    val onTimeChanged: () -> Unit,
    val onTimeZoneChanged: () -> Unit
) : BroadcastReceiver() {
    private var registered = false

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            onTimeZoneChanged()
        } else {
            onTimeChanged()
        }
    }

    fun register(context: Context) {
        if (!registered) {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_TIME_TICK)
            filter.addAction(Intent.ACTION_TIME_CHANGED)
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
            context.registerReceiver(this, filter)
            registered = true
        }
    }

    fun unregister(context: Context) {
        if (registered) {
            context.unregisterReceiver(this)
            registered = false
        }
    }
}

private fun formatTime(calendar: Calendar, currentTime: Long, timeFormat: String): String {
    calendar.timeInMillis = currentTime
    return DateFormat.format(timeFormat, calendar).toString()
}
