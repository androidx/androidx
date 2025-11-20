/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.watchface.complications.data.parser

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Icon
import android.icu.text.NumberFormat
import android.icu.util.TimeZone
import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import androidx.annotation.RequiresApi
import androidx.wear.watchface.complications.data.ColorRamp
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.data.TimeFormatComplicationText
import androidx.wear.watchface.complications.data.formatting.ComplicationTextFormatting
import java.io.IOException
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/** A container for parsed static complication preview data. */
class PreviewData internal constructor(private val data: Map<ComplicationType, ComplicationData>) {
    /** Returns the [ComplicationData] for the given type, or `null` if not found. */
    operator fun get(type: ComplicationType): ComplicationData? = data[type]

    internal companion object {
        private const val TAG = "PreviewData"

        // XML Tags
        private const val TAG_COMPLICATION = "complication"
        private const val TAG_TEXT = "text"
        private const val TAG_TITLE = "title"
        private const val TAG_PLAIN = "plain"
        private const val TAG_FORMATTED = "formatted"
        private const val TAG_TIME = "time"
        private const val TAG_DATE = "date"
        private const val TAG_PARAM = "param"
        private const val TAG_TIME_DIFFERENCE = "time-difference"

        // XML Attributes
        private const val ATTR_TYPE = "type"
        private const val ATTR_VALUE = "value"
        private const val ATTR_TARGET_VALUE = "targetValue"
        private const val ATTR_MIN = "min"
        private const val ATTR_MAX = "max"
        private const val ATTR_VALUE_TYPE = "valueType"
        private const val ATTR_MONOCHROMATIC_IMAGE = "monochromaticImage"
        private const val ATTR_SMALL_IMAGE = "smallImage"
        private const val ATTR_SMALL_IMAGE_TYPE = "smallImageType"
        private const val ATTR_FORMAT = "format"
        private const val ATTR_INSTANT = "instant"
        private const val ATTR_TARGET_INSTANT = "targetInstant"
        private const val ATTR_CURRENT_INSTANT = "currentInstant"
        private const val ATTR_SHOULD_SHORTEN_AM_PM = "shouldShortenAmPm"
        private const val ATTR_FORMATS = "formats"
        private const val ATTR_FALLBACK = "fallback"
        private const val ATTR_COLOR_RAMP = "colorRamp"
        private const val ATTR_COLOR_RAMP_INTERPOLATED = "colorRampInterpolated"
        private const val ATTR_MIN_UNIT = "minUnit"
        private const val ATTR_DISPLAY_AS_NOW = "displayAsNow"

        // Complication Type Strings
        private const val TYPE_STR_GOAL_PROGRESS = "GOAL_PROGRESS"
        private const val TYPE_STR_RANGED_VALUE = "RANGED_VALUE"
        private const val TYPE_STR_SHORT_TEXT = "SHORT_TEXT"
        private const val TYPE_STR_LONG_TEXT = "LONG_TEXT"
        private const val TYPE_STR_MONOCHROMATIC_IMAGE = "MONOCHROMATIC_IMAGE"
        private const val TYPE_STR_SMALL_IMAGE = "SMALL_IMAGE"

        // SMALL_IMAGE types:
        private const val SMALL_IMAGE_TYPE_ICON = "icon"
        private const val SMALL_IMAGE_TYPE_PHOTO = "photo"

        // Plain text types
        private const val PLAIN_TEXT_TYPE_STRING = "string"
        private const val PLAIN_TEXT_TYPE_INT = "integer"
        private const val PLAIN_TEXT_TYPE_LONG = "long"
        private const val PLAIN_TEXT_TYPE_FLOAT = "float"

        /**
         * Inflates a [PreviewData] object from an XML resource.
         *
         * @param providerContext The context of the complication provider application.
         * @param parser The [XmlResourceParser] for the preview data XML.
         * @return A [PreviewData] object containing the parsed complication data.
         */
        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(XmlPullParserException::class, IOException::class)
        fun inflate(providerContext: Context, parser: XmlResourceParser): PreviewData {
            val parsedDataMap = mutableMapOf<ComplicationType, ComplicationData>()
            val textUtils = ComplicationTextFormatting(Locale.getDefault())
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == TAG_COMPLICATION) {
                    val type = mapComplicationType(parser.getAttributeValue(null, ATTR_TYPE))
                    val data = parseComplicationTag(parser, type, providerContext, textUtils)
                    if (data != null) {
                        parsedDataMap[type] = data
                    }
                }
                eventType = parser.next()
            }
            return PreviewData(parsedDataMap)
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseComplicationTag(
            parser: XmlResourceParser,
            type: ComplicationType,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): ComplicationData? {
            return when (type) {
                ComplicationType.SHORT_TEXT ->
                    parseShortTextComplication(parser, providerContext, textUtils)
                ComplicationType.LONG_TEXT ->
                    parseLongTextComplication(parser, providerContext, textUtils)
                ComplicationType.RANGED_VALUE ->
                    parseRangedValueComplication(parser, providerContext, textUtils)
                ComplicationType.GOAL_PROGRESS ->
                    parseGoalProgressComplication(parser, providerContext, textUtils)
                ComplicationType.MONOCHROMATIC_IMAGE ->
                    parseMonochromaticImageComplication(parser, providerContext)
                ComplicationType.SMALL_IMAGE -> parseSmallImageComplication(parser, providerContext)
                else -> {
                    skip(parser)
                    null
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseShortTextComplication(
            parser: XmlResourceParser,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): ShortTextComplicationData {
            val monochromaticImage = parseMonochromaticImage(parser, providerContext)
            val smallImage = parseSmallImage(parser, providerContext)
            var text: ComplicationText? = null
            var title: ComplicationText? = null

            parseChildren(parser, providerContext, textUtils) { tagName, textContent ->
                when (tagName) {
                    TAG_TEXT -> text = textContent.let { PlainComplicationText.Builder(it).build() }
                    TAG_TITLE ->
                        title = textContent.let { PlainComplicationText.Builder(it).build() }
                }
            }

            return ShortTextComplicationData.Builder(
                    requireNotNull(text) { "'text' attribute is required for SHORT_TEXT" },
                    contentDescription = ComplicationText.EMPTY,
                )
                .setTitle(title)
                .setMonochromaticImage(monochromaticImage)
                .setSmallImage(smallImage)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseLongTextComplication(
            parser: XmlResourceParser,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): LongTextComplicationData {
            val monochromaticImage = parseMonochromaticImage(parser, providerContext)
            val smallImage = parseSmallImage(parser, providerContext)
            var text: ComplicationText? = null
            var title: ComplicationText? = null

            parseChildren(parser, providerContext, textUtils) { tagName, textContent ->
                when (tagName) {
                    TAG_TEXT -> text = textContent.let { PlainComplicationText.Builder(it).build() }
                    TAG_TITLE ->
                        title = textContent.let { PlainComplicationText.Builder(it).build() }
                }
            }

            return LongTextComplicationData.Builder(
                    requireNotNull(text) { "'text' attribute is required for LONG_TEXT" },
                    contentDescription = ComplicationText.EMPTY,
                )
                .setTitle(title)
                .setMonochromaticImage(monochromaticImage)
                .setSmallImage(smallImage)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseRangedValueComplication(
            parser: XmlResourceParser,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): RangedValueComplicationData {
            val value =
                requireNotNull(getFloatAttribute(parser, ATTR_VALUE)) {
                    "'value' attribute is required for RANGED_VALUE"
                }
            val min =
                requireNotNull(getFloatAttribute(parser, ATTR_MIN)) {
                    "'min' attribute is required for RANGED_VALUE"
                }
            val max =
                requireNotNull(getFloatAttribute(parser, ATTR_MAX)) {
                    "'max' attribute is required for RANGED_VALUE"
                }
            val valueType = getIntAttribute(parser, ATTR_VALUE_TYPE)
            val monochromaticImage = parseMonochromaticImage(parser, providerContext)
            val smallImage = parseSmallImage(parser, providerContext)
            val colorRamp = parseColorRampFromAttribute(parser, providerContext)
            var text: ComplicationText? = null
            var title: ComplicationText? = null

            parseChildren(parser, providerContext, textUtils) { tagName, textContent ->
                when (tagName) {
                    TAG_TEXT -> text = textContent.let { PlainComplicationText.Builder(it).build() }
                    TAG_TITLE ->
                        title = textContent.let { PlainComplicationText.Builder(it).build() }
                }
            }

            return RangedValueComplicationData.Builder(
                    value,
                    min,
                    max,
                    contentDescription = ComplicationText.EMPTY,
                )
                .setValueType(valueType ?: RangedValueComplicationData.TYPE_UNDEFINED)
                .setText(text)
                .setTitle(title)
                .setMonochromaticImage(monochromaticImage)
                .setSmallImage(smallImage)
                .setColorRamp(colorRamp)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseGoalProgressComplication(
            parser: XmlResourceParser,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): GoalProgressComplicationData {
            val value =
                requireNotNull(getFloatAttribute(parser, ATTR_VALUE)) {
                    "'value' attribute is required for GOAL_PROGRESS"
                }
            val targetValue =
                requireNotNull(getFloatAttribute(parser, ATTR_TARGET_VALUE)) {
                    "'targetValue' attribute is required for GOAL_PROGRESS"
                }
            val monochromaticImage = parseMonochromaticImage(parser, providerContext)
            val smallImage = parseSmallImage(parser, providerContext)
            val colorRamp = parseColorRampFromAttribute(parser, providerContext)
            var text: ComplicationText? = null
            var title: ComplicationText? = null

            parseChildren(parser, providerContext, textUtils) { tagName, textContent ->
                when (tagName) {
                    TAG_TEXT -> text = textContent.let { PlainComplicationText.Builder(it).build() }
                    TAG_TITLE ->
                        title = textContent.let { PlainComplicationText.Builder(it).build() }
                }
            }

            return GoalProgressComplicationData.Builder(
                    value,
                    targetValue,
                    contentDescription = ComplicationText.EMPTY,
                )
                .setText(text)
                .setTitle(title)
                .setMonochromaticImage(monochromaticImage)
                .setSmallImage(smallImage)
                .setColorRamp(colorRamp)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseMonochromaticImageComplication(
            parser: XmlResourceParser,
            providerContext: Context,
        ): MonochromaticImageComplicationData {
            val monochromaticImage =
                requireNotNull(parseMonochromaticImage(parser, providerContext)) {
                    "'monochromaticImage' attribute is required for MONOCHROMATIC_IMAGE"
                }
            skip(parser)
            return MonochromaticImageComplicationData.Builder(
                    monochromaticImage,
                    contentDescription = ComplicationText.EMPTY,
                )
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseSmallImageComplication(
            parser: XmlResourceParser,
            providerContext: Context,
        ): SmallImageComplicationData {
            val smallImage =
                requireNotNull(parseSmallImage(parser, providerContext)) {
                    "'smallImage' attribute is required for SMALL_IMAGE"
                }
            skip(parser)
            return SmallImageComplicationData.Builder(
                    smallImage,
                    contentDescription = ComplicationText.EMPTY,
                )
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseChildren(
            parser: XmlResourceParser,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
            block: (String, String) -> Unit,
        ) {
            while (parser.next() != XmlPullParser.END_TAG || parser.name != TAG_COMPLICATION) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                val textContent = parseTextElement(parser, providerContext, textUtils)
                if (textContent != null) {
                    block(parser.name, textContent)
                } else {
                    skip(parser)
                }
            }
        }

        private fun parseMonochromaticImage(
            parser: XmlResourceParser,
            providerContext: Context,
        ): MonochromaticImage? {
            val imageResId =
                getResourceIdFromAttribute(parser, ATTR_MONOCHROMATIC_IMAGE, providerContext)
            return if (imageResId != 0) {
                MonochromaticImage.Builder(Icon.createWithResource(providerContext, imageResId))
                    .build()
            } else {
                null
            }
        }

        private fun parseSmallImage(
            parser: XmlResourceParser,
            providerContext: Context,
        ): SmallImage? {
            val imageResId = getResourceIdFromAttribute(parser, ATTR_SMALL_IMAGE, providerContext)
            val imageType = parser.getAttributeValue(null, ATTR_SMALL_IMAGE_TYPE)
            return if (imageResId != 0 && imageType != null) {
                SmallImage.Builder(
                        Icon.createWithResource(providerContext, imageResId),
                        mapSmallImageType(imageType),
                    )
                    .build()
            } else {
                null
            }
        }

        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseTextElement(
            parser: XmlResourceParser,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): String? {
            var text: String? = null
            while (
                parser.next() != XmlPullParser.END_TAG ||
                    (parser.name != TAG_TEXT && parser.name != TAG_TITLE)
            ) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                text =
                    when (parser.name) {
                        TAG_PLAIN -> parsePlainText(parser, providerContext)
                        TAG_FORMATTED -> parseFormattedText(parser, providerContext, textUtils)
                        TAG_TIME -> parseTimeText(parser, providerContext, textUtils)
                        TAG_DATE -> parseDateText(parser, providerContext, textUtils)
                        TAG_TIME_DIFFERENCE -> parseTimeDifferenceText(parser, providerContext)
                        else -> null
                    }
            }
            return text
        }

        private fun parsePlainText(parser: XmlResourceParser, providerContext: Context): String? {
            val attributeValue = parser.getAttributeValue(null, ATTR_VALUE) ?: return null
            val valueType = parser.getAttributeValue(null, ATTR_VALUE_TYPE)

            if (attributeValue.startsWith("@")) {
                return resolveTextResource(providerContext, attributeValue)
            }
            val numberFormat = NumberFormat.getInstance(Locale.getDefault())
            val text =
                when (valueType) {
                    PLAIN_TEXT_TYPE_INT -> numberFormat.format(attributeValue.toLongOrNull() ?: 0L)
                    PLAIN_TEXT_TYPE_LONG -> numberFormat.format(attributeValue.toLongOrNull() ?: 0L)
                    PLAIN_TEXT_TYPE_FLOAT ->
                        numberFormat.format(attributeValue.toDoubleOrNull() ?: 0.0)

                    else -> attributeValue
                }
            return text
        }

        /**
         * Resolves a string from a resource identifier (i.e., "@string/name", "@integer/number").
         */
        private fun resolveTextResource(context: Context, resourceIdentifier: String): String? {
            val resources = context.resources
            val resourceName = resourceIdentifier.substring(1)
            val resId = resources.getIdentifier(resourceName, null, context.packageName)

            if (resId == 0) {
                Log.w(TAG, "Could not find resource ID: $resourceIdentifier")
                return null
            }

            return try {
                val typeName = resources.getResourceTypeName(resId)
                when (typeName) {
                    "string" -> resources.getString(resId)
                    "integer" ->
                        NumberFormat.getInstance(Locale.getDefault())
                            .format(resources.getInteger(resId))
                    else -> {
                        Log.w(TAG, "Unsupported resource type '$typeName' for plain text field.")
                        null
                    }
                }
            } catch (e: Resources.NotFoundException) {
                Log.w(TAG, "Could not find resource ID: $resourceIdentifier", e)
                null
            }
        }

        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseFormattedText(
            parser: XmlResourceParser,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): String {
            val formatString =
                providerContext.getString(
                    getResourceIdFromAttribute(parser, ATTR_FORMAT, providerContext)
                )

            val params = mutableListOf<Any>()
            var eventType: Int
            while (
                parser.next().also { eventType = it } != XmlPullParser.END_TAG ||
                    parser.name != TAG_FORMATTED
            ) {
                if (eventType == XmlPullParser.START_TAG && parser.name == TAG_PARAM) {
                    parser.nextTag() // Move to the param's type tag
                    when (parser.name) {
                        TAG_TIME ->
                            parseTimeText(parser, providerContext, textUtils)?.let {
                                params.add(it)
                            }
                        TAG_DATE ->
                            parseDateText(parser, providerContext, textUtils)?.let {
                                params.add(it)
                            }
                        TAG_PLAIN -> parsePlainText(parser, providerContext)?.let { params.add(it) }
                        TAG_TIME_DIFFERENCE ->
                            parseTimeDifferenceText(parser, providerContext)?.let { params.add(it) }
                    }
                    parser.nextTag() // Move to the param type's end tag
                }
            }
            return String.format(formatString, *params.toTypedArray())
        }

        private fun parseTimeText(
            parser: XmlResourceParser,
            context: Context,
            textUtils: ComplicationTextFormatting,
        ): String? {
            val instantStr = parser.getAttributeValue(null, ATTR_INSTANT) ?: return null
            try {
                val instant = Instant.ofEpochSecond(instantStr.toLong())
                val shouldShorten =
                    parser.getAttributeValue(null, ATTR_SHOULD_SHORTEN_AM_PM)?.toBoolean() ?: false

                val timePattern =
                    if (shouldShorten) {
                        if (DateFormat.is24HourFormat(context)) {
                            textUtils.shortTextTimeFormat24Hour
                        } else {
                            textUtils.shortTextTimeFormat12Hour
                        }
                    } else {
                        if (DateFormat.is24HourFormat(context)) {
                            textUtils.shortTextTimeFormat24HourWithoutAmPmShortening
                        } else {
                            textUtils.shortTextTimeFormat12HourWithoutAmPmShortening
                        }
                    }

                return TimeFormatComplicationText.Builder(
                        requireNotNull(timePattern) { "Invalid time pattern" }
                    )
                    .setTimeZone(TimeZone.GMT_ZONE)
                    .build()
                    .getTextAt(context.resources, instant)
                    .toString()
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid instant format for <time> tag", e)
                return null
            }
        }

        private fun parseDateText(
            parser: XmlResourceParser,
            context: Context,
            textUtils: ComplicationTextFormatting,
        ): String? {
            val targetInstantStr = parser.getAttributeValue(null, ATTR_INSTANT) ?: return null
            try {
                val instant = Instant.ofEpochSecond(targetInstantStr.toLong())
                val formatsStr = parser.getAttributeValue(null, ATTR_FORMATS)
                val fallback = parser.getAttributeValue(null, ATTR_FALLBACK)

                val pattern =
                    if (formatsStr != null && fallback != null) {
                        val skeletons = formatsStr.split(",").toTypedArray()
                        textUtils.getBestShortTextDateFormat(skeletons, fallback)
                    } else {
                        textUtils.shortTextDayMonthFormat
                    }

                return TimeFormatComplicationText.Builder(
                        requireNotNull(pattern) { "invalid date pattern" }
                    )
                    .setTimeZone(TimeZone.GMT_ZONE)
                    .build()
                    .getTextAt(context.resources, instant)
                    .toString()
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid instant format for <date> tag", e)
                return null
            }
        }

        private fun parseTimeDifferenceText(parser: XmlResourceParser, context: Context): String? {
            val typeStr = parser.getAttributeValue(null, ATTR_TYPE)
            val targetInstantStr = parser.getAttributeValue(null, ATTR_TARGET_INSTANT)
            val currentInstantStr = parser.getAttributeValue(null, ATTR_CURRENT_INSTANT)
            val minUnitStr = parser.getAttributeValue(null, ATTR_MIN_UNIT)
            val displayAsNow =
                parser.getAttributeValue(null, ATTR_DISPLAY_AS_NOW)?.toBoolean() ?: false

            if (typeStr == null || targetInstantStr == null || currentInstantStr == null) {
                return null
            }

            try {
                val instant = Instant.ofEpochSecond(targetInstantStr.toLong())
                val currentInstant = Instant.ofEpochSecond(currentInstantStr.toLong())
                val style = TimeDifferenceStyle.valueOf(typeStr)
                val minUnit =
                    if (minUnitStr != null) {
                        TimeUnit.valueOf(minUnitStr)
                    } else {
                        TimeUnit.MINUTES
                    }

                return TimeDifferenceComplicationText.Builder(
                        style,
                        CountDownTimeReference(instant),
                    )
                    .setMinimumTimeUnit(minUnit)
                    .setDisplayAsNow(displayAsNow)
                    .build()
                    .getTextAt(context.resources, currentInstant)
                    .toString()
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid attribute in <time-difference> tag", e)
                return null
            }
        }

        private fun parseColorRampFromAttribute(
            parser: XmlResourceParser,
            providerContext: Context,
        ): ColorRamp? {
            val colorRampResId =
                getResourceIdFromAttribute(parser, ATTR_COLOR_RAMP, providerContext)
            if (colorRampResId == 0) {
                return null
            }

            val interpolated =
                parser.getAttributeValue(null, ATTR_COLOR_RAMP_INTERPOLATED)?.toBoolean() ?: false
            val resources = providerContext.resources

            try {
                val typedArray = resources.obtainTypedArray(colorRampResId)
                val colors = IntArray(typedArray.length()) { i -> typedArray.getColor(i, 0) }
                typedArray.recycle()
                return ColorRamp(colors, interpolated)
            } catch (e: Resources.NotFoundException) {
                Log.w(TAG, "Color ramp array resource not found: $colorRampResId", e)
                return null
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun mapComplicationType(typeStr: String): ComplicationType {
            return when (typeStr) {
                TYPE_STR_GOAL_PROGRESS -> ComplicationType.GOAL_PROGRESS
                TYPE_STR_RANGED_VALUE -> ComplicationType.RANGED_VALUE
                TYPE_STR_SHORT_TEXT -> ComplicationType.SHORT_TEXT
                TYPE_STR_LONG_TEXT -> ComplicationType.LONG_TEXT
                TYPE_STR_MONOCHROMATIC_IMAGE -> ComplicationType.MONOCHROMATIC_IMAGE
                TYPE_STR_SMALL_IMAGE -> ComplicationType.SMALL_IMAGE
                else -> throw IllegalStateException("Unexpected complication type: $typeStr")
            }
        }

        private fun mapSmallImageType(smallImageType: String): SmallImageType {
            return when (smallImageType) {
                SMALL_IMAGE_TYPE_ICON -> SmallImageType.ICON
                SMALL_IMAGE_TYPE_PHOTO -> SmallImageType.PHOTO
                else -> throw IllegalStateException("Unexpected smallImage type: $smallImageType")
            }
        }

        @Throws(NumberFormatException::class)
        private fun getFloatAttribute(parser: XmlResourceParser, attributeName: String?): Float? {
            return parser.getAttributeValue(null, attributeName)?.toFloat()
        }

        @Throws(NumberFormatException::class)
        private fun getIntAttribute(parser: XmlResourceParser, attributeName: String?): Int? {
            return parser.getAttributeValue(null, attributeName)?.toInt()
        }

        private fun getResourceIdFromAttribute(
            parser: XmlResourceParser,
            attrName: String,
            providerContext: Context,
        ): Int {
            val resValue = parser.getAttributeValue(null, attrName) ?: return 0
            val resources = providerContext.resources
            val packageName = providerContext.packageName

            return when {
                resValue.startsWith("@") ->
                    resources.getIdentifier(resValue.substring(1), null, packageName)
                resValue.startsWith("?") -> {
                    val attrFullName = resValue.substring(1)
                    val attrNameOnly =
                        if (attrFullName.startsWith("attr/")) {
                            attrFullName.substring(5)
                        } else {
                            attrFullName
                        }
                    val attrResId = resources.getIdentifier(attrNameOnly, "attr", packageName)
                    if (attrResId == 0) {
                        return 0
                    }
                    val typedValue = TypedValue()
                    providerContext.theme.resolveAttribute(attrResId, typedValue, true)
                    typedValue.resourceId
                }
                else -> 0
            }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun skip(parser: XmlResourceParser) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                throw IllegalStateException()
            }
            var depth = 1
            while (depth != 0) {
                when (parser.next()) {
                    XmlPullParser.END_TAG -> depth--
                    XmlPullParser.START_TAG -> depth++
                }
            }
        }
    }
}
