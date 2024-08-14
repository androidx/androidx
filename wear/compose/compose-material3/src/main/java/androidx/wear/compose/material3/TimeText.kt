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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.foundation.sizeIn
import androidx.wear.compose.foundation.weight
import androidx.wear.compose.material3.TimeTextDefaults.CurvedTextSeparator
import androidx.wear.compose.material3.TimeTextDefaults.TextSeparator
import androidx.wear.compose.material3.TimeTextDefaults.timeFormat
import androidx.wear.compose.materialcore.currentTimeMillis
import androidx.wear.compose.materialcore.is24HourFormat
import androidx.wear.compose.materialcore.isRoundDevice
import java.util.Calendar
import java.util.Locale

/**
 * Layout to show the current time and a label at the top of the screen. If device has a round
 * screen, then the time will be curved along the top edge of the screen, if rectangular - then the
 * text and the time will be straight.
 *
 * Note that Wear Material UX guidance recommends that time text should not be larger than
 * [TimeTextDefaults.MaxSweepAngle] of the screen edge on round devices, which is enforced by
 * default. It is recommended that additional content, if any, is limited to short status messages
 * before the [TimeTextScope.time] using the MaterialTheme.colorScheme.primary color.
 *
 * For more information, see the
 * [Curved Text](https://developer.android.com/training/wearables/components/curved-text) guide.
 *
 * Different components of [TimeText] can be added through methods of [TimeTextScope].
 *
 * A simple [TimeText] which shows the current time:
 *
 * @sample androidx.wear.compose.material3.samples.TimeTextClockOnly
 *
 * A [TimeText] with a short app status message shown:
 *
 * @sample androidx.wear.compose.material3.samples.TimeTextWithStatus
 * @param modifier The modifier to be applied to the component.
 * @param curvedModifier The [CurvedModifier] used to restrict the arc in which [TimeText] is drawn.
 * @param maxSweepAngle The default maximum sweep angle in degrees.
 * @param timeSource [TimeSource] which retrieves the current time and formats it.
 * @param timeTextStyle [TextStyle] for the time text itself.
 * @param contentColor [Color] of content of displayed through [TimeTextScope.text] and
 *   [TimeTextScope.composable].
 * @param contentPadding The spacing values between the container and the content.
 * @param content The content of the [TimeText].
 */
@Composable
fun TimeText(
    modifier: Modifier = Modifier,
    curvedModifier: CurvedModifier = CurvedModifier,
    maxSweepAngle: Float = TimeTextDefaults.MaxSweepAngle,
    timeSource: TimeSource = TimeTextDefaults.rememberTimeSource(timeFormat()),
    timeTextStyle: TextStyle = TimeTextDefaults.timeTextStyle(),
    contentColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = TimeTextDefaults.ContentPadding,
    content: TimeTextScope.() -> Unit
) {
    val timeText = timeSource.currentTime()

    if (isRoundDevice()) {
        CurvedLayout(modifier = modifier) {
            curvedRow(
                modifier =
                    curvedModifier
                        .sizeIn(maxSweepDegrees = maxSweepAngle)
                        .padding(contentPadding.toArcPadding()),
                radialAlignment = CurvedAlignment.Radial.Center
            ) {
                CurvedTimeTextScope(timeText, timeTextStyle, maxSweepAngle, contentColor).apply {
                    content()
                    Show()
                }
            }
        }
    } else {
        Box(modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                LinearTimeTextScope(timeText, timeTextStyle, contentColor).apply {
                    content()
                    Show()
                }
            }
        }
    }
}

/** Receiver scope which is used by [TimeText]. */
sealed class TimeTextScope {
    /**
     * Adds a composable [Text] for non-round devices and [curvedText] for round devices to
     * [TimeText] content. Typically used to add a short status message ahead of the time text.
     *
     * @param text The text to display.
     * @param style configuration for the [text] such as color, font etc.
     * @param weight Size the text's width proportional to its weight relative to other weighted
     *   sibling elements in the TimeText. Specify NaN to make this text not have a weight
     *   specified. The default value, [TimeTextDefaults.AutoTextWeight], makes this text have
     *   weight 1f if it's the only one, and not have weight if there are two or more.
     */
    abstract fun text(
        text: String,
        style: TextStyle? = null,
        weight: Float = TimeTextDefaults.AutoTextWeight
    )

    /** Adds a text displaying current time. */
    abstract fun time()

    /**
     * Adds a separator in [TimeText].
     *
     * @param style configuration for the [separator] such as color, font etc.
     */
    abstract fun separator(style: TextStyle? = null)

    /**
     * Adds a composable in content of [TimeText]. This can be used to display non-text information
     * such as an icon.
     *
     * @param content Slot for the [composable] to be displayed.
     */
    abstract fun composable(content: @Composable () -> Unit)
}

/** Contains the default values used by [TimeText]. */
object TimeTextDefaults {
    /** The default padding from the edge of the screen. */
    private val Padding = PaddingDefaults.edgePadding
    /** Default format for 24h clock. */
    const val TimeFormat24Hours = "HH:mm"

    /** Default format for 12h clock. */
    const val TimeFormat12Hours = "h:mm"

    /**
     * The default maximum sweep angle in degrees used by [TimeText].
     *
     * This is calculated by keeping the length of the corresponding chord on the circle to be
     * approximately 57% of the screen width.
     */
    const val MaxSweepAngle: Float = 70f

    /** The default content padding used by [TimeText]. */
    val ContentPadding: PaddingValues = PaddingValues(top = Padding)

    /**
     * Retrieves default timeFormat for the device. Depending on settings, it can be either 12h or
     * 24h format.
     */
    @Composable
    fun timeFormat(): String {
        val format = if (is24HourFormat()) TimeFormat24Hours else TimeFormat12Hours
        return DateFormat.getBestDateTimePattern(Locale.getDefault(), format)
            .replace("a", "")
            .trim()
    }

    /**
     * Creates a [TextStyle] with default parameters used for showing time on square screens. By
     * default a copy of MaterialTheme.typography.arcMedium style is created.
     *
     * @param background The background color.
     * @param color The main color.
     * @param fontSize The font size.
     */
    @Composable
    fun timeTextStyle(
        background: Color = Color.Unspecified,
        color: Color = MaterialTheme.colorScheme.onBackground,
        fontSize: TextUnit = TextUnit.Unspecified,
    ) =
        MaterialTheme.typography.arcMedium +
            TextStyle(color = color, background = background, fontSize = fontSize)

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
    fun rememberTimeSource(timeFormat: String): TimeSource =
        remember(timeFormat) { DefaultTimeSource(timeFormat) }

    /**
     * A default implementation of Separator shown between any text/composable and the time on
     * non-round screens.
     *
     * @param modifier A default modifier for the separator.
     * @param textStyle A [TextStyle] for the separator.
     * @param contentPadding The spacing values between the container and the separator.
     */
    @Composable
    internal fun TextSeparator(
        modifier: Modifier = Modifier,
        textStyle: TextStyle = timeTextStyle(),
        contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp)
    ) {
        Text(text = "·", style = textStyle, modifier = modifier.padding(contentPadding))
    }

    /**
     * A default implementation of Separator shown between any text/composable and the time on round
     * screens.
     *
     * @param curvedTextStyle A [CurvedTextStyle] for the separator.
     * @param contentArcPadding [ArcPaddingValues] for the separator text.
     */
    internal fun CurvedScope.CurvedTextSeparator(
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
     * Weight value used to specify that the value is automatic. It will be 1f when there is one
     * text, and no weight will be used if there are 2 or more texts. For the 2+ texts case, usually
     * one of them should have weight manually specified to ensure its properly cut and ellipsized.
     */
    val AutoTextWeight = -1f
}

interface TimeSource {

    /**
     * A method responsible for returning updated time string.
     *
     * @return Formatted time string.
     */
    @Composable fun currentTime(): String
}

/** Implementation of [TimeTextScope] for round devices. */
internal class CurvedTimeTextScope(
    private val timeText: String,
    private val timeTextStyle: TextStyle,
    private val maxSweepAngle: Float,
    contentColor: Color,
) : TimeTextScope() {
    private var textCount = 0
    private val pending = mutableListOf<CurvedScope.() -> Unit>()
    private val contentTextStyle = timeTextStyle.merge(contentColor)

    override fun text(text: String, style: TextStyle?, weight: Float) {
        textCount++
        pending.add {
            curvedText(
                text = text,
                overflow = TextOverflow.Ellipsis,
                maxSweepAngle = maxSweepAngle,
                style = CurvedTextStyle(style = contentTextStyle.merge(style)),
                modifier =
                    if (weight.isValidWeight()) CurvedModifier.weight(weight)
                    // Note that we are creating a lambda here, but textCount is actually read
                    // later, during the call to Show, when the pending list is fully constructed.
                    else if (weight == TimeTextDefaults.AutoTextWeight && textCount <= 1)
                        CurvedModifier.weight(1f)
                    else CurvedModifier
            )
        }
    }

    override fun time() {
        pending.add {
            curvedText(
                timeText,
                maxSweepAngle = maxSweepAngle,
                style = CurvedTextStyle(timeTextStyle)
            )
        }
    }

    override fun separator(style: TextStyle?) {
        pending.add { CurvedTextSeparator(CurvedTextStyle(style = timeTextStyle.merge(style))) }
    }

    override fun composable(content: @Composable () -> Unit) {
        pending.add {
            curvedComposable {
                CompositionLocalProvider(
                    LocalContentColor provides contentTextStyle.color,
                    LocalTextStyle provides contentTextStyle,
                    content = content
                )
            }
        }
    }

    fun CurvedScope.Show() {
        pending.fastForEach { it() }
    }
}

/** Implementation of [TimeTextScope] for non-round devices. */
internal class LinearTimeTextScope(
    private val timeText: String,
    private val timeTextStyle: TextStyle,
    contentColor: Color,
) : TimeTextScope() {
    private var textCount = 0
    private val pending = mutableListOf<@Composable RowScope.() -> Unit>()
    private val contentTextStyle = timeTextStyle.merge(contentColor)

    override fun text(text: String, style: TextStyle?, weight: Float) {
        textCount++
        pending.add {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = contentTextStyle.merge(style),
                modifier =
                    if (weight.isValidWeight()) Modifier.weight(weight, fill = false)
                    // Note that we are creating a lambda here, but textCount is actually read
                    // later, during the call to Show, when the pending list is fully constructed.
                    else if (weight == TimeTextDefaults.AutoTextWeight && textCount <= 1)
                        Modifier.weight(1f, fill = false)
                    else Modifier
            )
        }
    }

    override fun time() {
        pending.add { Text(timeText, style = timeTextStyle) }
    }

    override fun separator(style: TextStyle?) {
        pending.add { TextSeparator(textStyle = timeTextStyle.merge(style)) }
    }

    override fun composable(content: @Composable () -> Unit) {
        pending.add {
            CompositionLocalProvider(
                LocalContentColor provides contentTextStyle.color,
                LocalTextStyle provides contentTextStyle,
                content = content
            )
        }
    }

    @Composable
    fun RowScope.Show() {
        pending.fastForEach { it() }
    }
}

private fun Float.isValidWeight() = !isNaN() && this > 0f

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
