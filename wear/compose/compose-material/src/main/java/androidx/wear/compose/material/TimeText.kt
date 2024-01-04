/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.material.TimeTextDefaults.CurvedTextSeparator
import androidx.wear.compose.material.TimeTextDefaults.TextSeparator
import androidx.wear.compose.material.TimeTextDefaults.timeFormat
import androidx.wear.compose.materialcore.isRoundDevice
import java.util.Calendar
import java.util.Locale

/**
 * Layout to show the current time and a label at the top of the screen.
 * If device has a round screen, then the time will be curved along the top edge of the screen,
 * if rectangular - then the text and the time will be straight
 *
 * This composable supports additional composable views to the left and to the right
 * of the clock: Start Content and End Content.
 * [startCurvedContent], [endCurvedContent] and [textCurvedSeparator] are used
 * for Round screens.
 * [startLinearContent], [endLinearContent] and [textLinearSeparator] are used
 * for Square screens.
 * For proper support of Square and Round screens both Linear and Curved methods should
 * be implemented.
 *
 * Note that Wear Material UX guidance recommends that time text should not be larger than 90
 * degrees of the screen edge on round devices and prefers short status messages be shown in start
 * content only using the MaterialTheme.colors.primary color for the status message.
 *
 * For more information, see the
 * [Curved Text](https://developer.android.com/training/wearables/components/curved-text)
 * guide.
 *
 * A [TimeText] with a short app status message shown in the start content:
 * @sample androidx.wear.compose.material.samples.TimeTextWithStatus
 *
 * An example of a [TimeText] with a different date and time format:
 * @sample androidx.wear.compose.material.samples.TimeTextWithFullDateAndTimeFormat
 *
 * An example of a [TimeText] animating a message that is added or removed
 * @sample androidx.wear.compose.material.samples.TimeTextAnimation
 *
 * @param modifier Current modifier.
 * @param timeSource [TimeSource] which retrieves the current time and formats it.
 * @param timeTextStyle Optional textStyle for the time text itself
 * @param contentPadding The spacing values between the container and the content
 * @param startLinearContent a slot before the time which is used only on Square screens
 * @param startCurvedContent a slot before the time which is used only on Round screens
 * @param endLinearContent a slot after the time which is used only on Square screens
 * @param endCurvedContent a slot after the time which is used only on Round screens
 * @param textLinearSeparator a separator slot which is used only on Square screens
 * @param textCurvedSeparator a separator slot which is used only on Round screens
 */
@Composable
public fun TimeText(
    modifier: Modifier = Modifier,
    timeSource: TimeSource = TimeTextDefaults.timeSource(timeFormat()),
    timeTextStyle: TextStyle = TimeTextDefaults.timeTextStyle(),
    contentPadding: PaddingValues = TimeTextDefaults.ContentPadding,
    startLinearContent: (@Composable () -> Unit)? = null,
    startCurvedContent: (CurvedScope.() -> Unit)? = null,
    endLinearContent: (@Composable () -> Unit)? = null,
    endCurvedContent: (CurvedScope.() -> Unit)? = null,
    textLinearSeparator: @Composable () -> Unit = { TextSeparator(textStyle = timeTextStyle) },
    textCurvedSeparator: CurvedScope.() -> Unit = {
        CurvedTextSeparator(curvedTextStyle = CurvedTextStyle(timeTextStyle))
    },
) {
    val timeText = timeSource.currentTime

    if (isRoundDevice()) {
        CurvedLayout(modifier = modifier) {
            curvedRow(modifier = CurvedModifier.padding(contentPadding.toArcPadding())) {
                startCurvedContent?.let {
                    it.invoke(this)
                    textCurvedSeparator()
                }
                curvedText(
                    text = timeText,
                    style = CurvedTextStyle(timeTextStyle)
                )
                endCurvedContent?.let {
                    textCurvedSeparator()
                    it.invoke(this)
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            startLinearContent?.let {
                it.invoke()
                textLinearSeparator()
            }
            Text(
                text = timeText,
                maxLines = 1,
                style = timeTextStyle,
            )
            endLinearContent?.let {
                textLinearSeparator()
                it.invoke()
            }
        }
    }
}

/**
 * Contains the default values used by [TimeText]
 */
public object TimeTextDefaults {

    private val Padding = 2.dp

    /**
     * Default format for 24h clock.
     */
    const val TimeFormat24Hours = "HH:mm"

    /**
     * Default format for 12h clock.
     */
    const val TimeFormat12Hours = "h:mm"

    /**
     * The default content padding used by [TimeText]
     */
    public val ContentPadding: PaddingValues = PaddingValues(Padding)

    /**
     * Retrieves default timeFormat for the device. Depending on settings, it can be either
     * 12h or 24h format
     */
    @Composable
    public fun timeFormat(): String {
        val format = if (is24HourFormat()) TimeFormat24Hours else TimeFormat12Hours
        return DateFormat.getBestDateTimePattern(Locale.getDefault(), format)
            .replace("a", "").trim()
    }

    /**
     * Creates a [TextStyle] with default parameters used for showing time
     * on square screens. By default a copy of MaterialTheme.typography.caption1 style is created
     *
     * @param background The background color
     * @param color The main color
     * @param fontSize The font size
     */
    @Composable
    public fun timeTextStyle(
        background: Color = Color.Unspecified,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
    ) = MaterialTheme.typography.caption1 +
        TextStyle(color = color, background = background, fontSize = fontSize)

    /**
     * A default implementation of Separator shown between start/end content and the time
     * on square screens
     * @param modifier A default modifier for the separator
     * @param textStyle A [TextStyle] for the separator
     * @param contentPadding The spacing values between the container and the separator
     */
    @Composable
    public fun TextSeparator(
        modifier: Modifier = Modifier,
        textStyle: TextStyle = timeTextStyle(),
        contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp)
    ) {
        Text(
            text = "·",
            style = textStyle,
            modifier = modifier.padding(contentPadding)
        )
    }

    /**
     * A default implementation of Separator shown between start/end content and the time
     * on round screens
     * @param curvedTextStyle A [CurvedTextStyle] for the separator
     * @param contentArcPadding A [ArcPaddingValues] for the separator text
     */
    public fun CurvedScope.CurvedTextSeparator(
        curvedTextStyle: CurvedTextStyle? = null,
        contentArcPadding: ArcPaddingValues = ArcPaddingValues(angular = 4.dp)
    ) {
        curvedText(
            text = "·",
            style = curvedTextStyle,
            modifier = CurvedModifier.padding(contentArcPadding)
        )
    }

    /**
     * A default implementation of [TimeSource].
     * Once the system time changes, it triggers an update of the [TimeSource.currentTime]
     * which is formatted before that using [timeFormat] param.
     *
     * Android implementation:
     * [DefaultTimeSource] for Android uses [android.text.format.DateFormat]
     * [timeFormat] should follow the standard
     * [Date and Time patterns](https://developer.android.com/reference/java/text/SimpleDateFormat#date-and-time-patterns)
     * Examples:
     * "h:mm a" - 12:08 PM
     * "yyyy.MM.dd HH:mm:ss" - 2021.11.01 14:08:56
     * More examples can be found [here](https://developer.android.com/reference/java/text/SimpleDateFormat#examples)
     *
     * Desktop implementation: TBD
     *
     * @param timeFormat Date and time string pattern
     */
    public fun timeSource(timeFormat: String): TimeSource = DefaultTimeSource(timeFormat)
}

/**
 *  An interface which is responsible for retrieving a formatted time.
 */
public interface TimeSource {

    /**
     * A method responsible for returning updated time string.
     * @return Formatted time string.
     */
    val currentTime: String
        @Composable get
}

/**
 * An extension function, which converts PaddingValues into ArcPaddingValues
 */
private fun PaddingValues.toArcPadding() = object : ArcPaddingValues {
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

internal class DefaultTimeSource constructor(timeFormat: String) : TimeSource {
    private val _timeFormat = timeFormat

    override val currentTime: String
        @Composable
        get() = currentTime({ currentTimeMillis() }, _timeFormat).value
}

@Composable
@VisibleForTesting
internal fun currentTime(
    time: () -> Long,
    timeFormat: String
): State<String> {

    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    var currentTime by remember { mutableLongStateOf(time()) }

    val timeText = remember {
        derivedStateOf { formatTime(calendar, currentTime, timeFormat) }
    }

    val context = LocalContext.current
    val updatedTimeLambda by rememberUpdatedState(time)

    DisposableEffect(context, updatedTimeLambda) {
        val receiver = TimeBroadcastReceiver(
            onTimeChanged = { currentTime = updatedTimeLambda() },
            onTimeZoneChanged = { calendar = Calendar.getInstance() }
        )
        receiver.register(context)
        onDispose {
            receiver.unregister(context)
        }
    }
    return timeText
}

/**
 * A [BroadcastReceiver] to receive time tick, time change, and time zone change events.
 */
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

private fun formatTime(
    calendar: Calendar,
    currentTime: Long,
    timeFormat: String
): String {
    calendar.timeInMillis = currentTime
    return DateFormat.format(timeFormat, calendar).toString()
}
