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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.BasicCurvedText
import androidx.wear.compose.foundation.CurvedRow
import androidx.wear.compose.foundation.CurvedRowScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material.TimeTextDefaults.CurvedTextSeparator
import androidx.wear.compose.material.TimeTextDefaults.TextSeparator
import androidx.wear.compose.material.TimeTextDefaults.timeFormat

/**
 * Layout to show the current time and a label at the top of the screen.
 * If device has a round screen, then the time will be curved along the top edge of the screen,
 * if rectangular - then the text and the time will be straight
 *
 * This composable supports leading and trailing content - additional composable views to the left
 * and to the right of the clock.
 * [leadingCurvedContent], [trailingCurvedContent] and [textCurvedSeparator] are used
 * for Round screens.
 * [leadingLinearContent], [trailingLinearContent] and [textLinearSeparator] are used
 * for Square screens.
 * For proper support of Square and Round screens both Linear and Curved methods should
 * be implemented.
 *
 * The full customization for square and round devices can be checked here:
 * @sample androidx.wear.compose.material.samples.TimeTextWithCustomSeparator
 *
 * For more information, see the
 * [Curved Text](https://developer.android.com/training/wearables/components/curved-text)
 * guide.
 *
 * @param modifier Current modifier.
 * @param timeSource [TimeSource] which retrieves the current time.
 * @param timeTextStyle Optional textStyle for the time text itself
 * @param contentPadding The spacing values between the container and the content
 * @param leadingLinearContent a slot before the time which is used only on Square screens
 * @param leadingCurvedContent a slot before the time which is used only on Round screens
 * @param trailingLinearContent a slot after the time which is used only on Square screens
 * @param trailingCurvedContent a slot after the time which is used only on Round screens
 * @param textLinearSeparator a separator slot which is used only on Square screens
 * @param textCurvedSeparator a separator slot which is used only on Round screens
 */
@ExperimentalWearMaterialApi
@Composable
fun TimeText(
    modifier: Modifier = Modifier,
    timeSource: TimeSource = TimeTextDefaults.timeSource(timeFormat()),
    timeTextStyle: TextStyle = TimeTextDefaults.timeTextStyle(),
    contentPadding: PaddingValues = TimeTextDefaults.ContentPadding,
    leadingLinearContent: (@Composable () -> Unit)? = null,
    leadingCurvedContent: (@Composable CurvedRowScope.() -> Unit)? = null,
    trailingLinearContent: (@Composable () -> Unit)? = null,
    trailingCurvedContent: (@Composable CurvedRowScope.() -> Unit)? = null,
    textLinearSeparator: @Composable () -> Unit = { TextSeparator(textStyle = timeTextStyle) },
    textCurvedSeparator: @Composable CurvedRowScope.() -> Unit = {
        CurvedTextSeparator(curvedTextStyle = CurvedTextStyle(timeTextStyle))
    },
) {

    val timeText = timeSource.currentTime

    if (isRoundDevice()) {
        CurvedRow(modifier.padding(contentPadding)) {
            leadingCurvedContent?.let {
                it.invoke(this)
                textCurvedSeparator()
            }
            BasicCurvedText(
                text = timeText,
                style = CurvedTextStyle(timeTextStyle)
            )
            trailingCurvedContent?.let {
                textCurvedSeparator()
                it.invoke(this)
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
            leadingLinearContent?.let {
                it.invoke()
                textLinearSeparator()
            }
            Text(
                text = timeText,
                style = timeTextStyle,
            )
            trailingLinearContent?.let {
                textLinearSeparator()
                it.invoke()
            }
        }
    }
}

/**
 * Contains the default values used by [TimeText]
 */
@ExperimentalWearMaterialApi
public object TimeTextDefaults {

    private val Padding = 4.dp

    /**
     * Default format for 24h clock.
     */
    const val Skeleton24Hr = "Hm"

    /**
     * Default format for 12h clock.
     */
    const val Skeleton12Hr = "hm"

    /**
     * The default content padding used by [TimeText]
     */
    public val ContentPadding: PaddingValues = PaddingValues(Padding)

    /**
     * Retrieves default timeFormat for the device. Depending on settings, it can be either
     * 12h or 24h format
     */
    @Composable
    public fun timeFormat(): String = if (is24HourFormat()) Skeleton24Hr else Skeleton12Hr

    /**
     * Creates a [TextStyle] with default parameters used for showing time
     * on square screens
     *
     * @param color The main color
     * @param background The background color
     * @param fontSize The font size
     */
    @Composable
    public fun timeTextStyle(
        color: Color = Color.White,
        background: Color = Color.Transparent,
        fontSize: TextUnit = MaterialTheme.typography.button.fontSize,
    ) = TextStyle(
        color = color,
        background = background,
        fontSize = fontSize
    )

    /**
     * Creates a [CurvedTextStyle] with default parameters used for showing time
     * on round screens
     *
     * @param color The main color
     * @param background The background color
     * @param fontSize The font size
     */
    @Composable
    public fun timeCurvedTextStyle(
        color: Color = Color.White,
        background: Color = Color.Transparent,
        fontSize: TextUnit = MaterialTheme.typography.button.fontSize,
    ) = CurvedTextStyle(
        color = color,
        background = background,
        fontSize = fontSize
    )

    /**
     * A default implementation of Separator shown between trailing/leading content and the time
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
     * A default implementation of Separator shown between trailing/leading content and the time
     * on round screens
     * @param modifier Current modifier.
     * @param curvedTextStyle A [CurvedTextStyle] for the separator
     * @param contentArcPadding A [ArcPaddingValues] for the separator text
     */
    @Composable
    public fun CurvedRowScope.CurvedTextSeparator(
        modifier: Modifier = Modifier,
        curvedTextStyle: CurvedTextStyle = timeCurvedTextStyle(),
        contentArcPadding: ArcPaddingValues = ArcPaddingValues(angular = 4.dp)
    ) {
        BasicCurvedText(
            modifier = modifier,
            text = "·",
            contentArcPadding = contentArcPadding,
            style = curvedTextStyle
        )
    }

    /**
     * A default implementation of [TimeSource].
     * @param timeFormat Param for formatting time
     */
    fun timeSource(timeFormat: String): TimeSource = DefaultTimeSource(timeFormat)
}

@ExperimentalWearMaterialApi
internal expect class DefaultTimeSource(timeFormat: String) : TimeSource

/**
 *  An interface which is responsible for retrieving time and formatting it.
 */
@ExperimentalWearMaterialApi
public interface TimeSource {

    /**
     * A method responsible for returning updated time string.
     * @return Formatted time string.
     */
    val currentTime: String
        @Composable get
}
