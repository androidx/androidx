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

import android.content.ComponentName
import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Color
import android.graphics.drawable.Icon
import android.icu.text.NumberFormat
import android.icu.util.TimeZone
import android.os.Build
import android.os.PersistableBundle
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
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
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData
import androidx.wear.watchface.complications.data.formatting.ComplicationTextFormatting
import java.io.IOException
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/** A container for parsed static complication preview data. */
public class PreviewData
internal constructor(private val data: Map<ComplicationType, ComplicationData>) {
    /** Returns the [ComplicationData] for the given type, or `null` if not found. */
    public operator fun get(type: ComplicationType): ComplicationData? = data[type]

    internal companion object {
        private const val TAG = "PreviewData"

        // XML Tags
        private const val TAG_COMPLICATION = "complication"
        private const val TAG_TEXT = "text"
        private const val TAG_TITLE = "title"
        private const val TAG_ELEMENT = "element"
        private const val TAG_PLAIN = "plain"
        private const val TAG_FORMATTED = "formatted"
        private const val TAG_TIME = "time"
        private const val TAG_DATE = "date"
        private const val TAG_PARAM = "param"
        private const val TAG_TIME_DIFFERENCE = "time-difference"
        private const val TAG_EXTENDED_DATA = "extended-data"
        private const val TAG_STRING_REPLACEMENT = "string-replacement"

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
        private const val ATTR_KEY = "key"
        private const val ATTR_DICTIONARY_KEY = "dictionaryKey"
        private const val ATTR_SKELETON_VALUE = "skeletonValue"
        private const val ATTR_WEIGHT = "weight"
        private const val ATTR_COLOR = "color"
        private const val ATTR_BACKGROUND_COLOR = "backgroundColor"
        private const val ATTR_TIME_COMPONENT = "timeComponent"

        // Complication Type Strings
        private const val TYPE_STR_GOAL_PROGRESS = "GOAL_PROGRESS"
        private const val TYPE_STR_WEIGHTED_ELEMENTS = "WEIGHTED_ELEMENTS"
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

        // Time component types
        private const val TIME_COMPONENT_TIME_ONLY = "timeOnly"
        private const val TIME_COMPONENT_AM_PM_ONLY = "amPmOnly"

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
        @RequiresPermission("com.google.wear.permission.SET_COMPLICATION_EXTRAS")
        fun inflate(providerContext: Context, parser: XmlResourceParser): PreviewData {
            // We use providerContext for both parser and provider context.
            // This preserves the old (slightly broken) behavior for external callers.
            // TODO(471212833): Amend the parsing API (inflate) to take both provider and parser
            // contexts, so that we can access resources using the parser ID space
            return inflateInternal(
                providerComponent = null,
                providerContext,
                providerContext,
                parser,
            )
        }

        /**
         * Inflates a [PreviewData] object from an XML resource.
         *
         * @param providerComponent The component name of the complication provider. If not passed,
         *   datasource field of the generated complication data will be null.
         * @param parserContext The context of the parsing application.
         * @param providerContext The context of the complication provider application.
         * @param parser The [XmlResourceParser] for the preview data XML.
         * @return A [PreviewData] object containing the parsed complication data.
         */
        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(XmlPullParserException::class, IOException::class)
        @RequiresPermission("com.google.wear.permission.SET_COMPLICATION_EXTRAS")
        internal fun inflate(
            providerComponent: ComponentName,
            parserContext: Context,
            providerContext: Context,
            parser: XmlResourceParser,
        ): PreviewData {
            return inflateInternal(providerComponent, parserContext, providerContext, parser)
        }

        /**
         * Inflates a [PreviewData] object from an XML resource.
         *
         * @param providerComponent The component name of the complication provider application. if
         *   not set, datasource field of the generated complication data will be empty.
         * @param parserContext The context of the parsing application.
         * @param providerContext The context of the complication provider application.
         * @param parser The [XmlResourceParser] for the preview data XML.
         * @return A [PreviewData] object containing the parsed complication data.
         */
        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(XmlPullParserException::class, IOException::class)
        @RequiresPermission("com.google.wear.permission.SET_COMPLICATION_EXTRAS")
        private fun inflateInternal(
            providerComponent: ComponentName?,
            parserContext: Context,
            providerContext: Context,
            parser: XmlResourceParser,
        ): PreviewData {
            val parsedDataMap = mutableMapOf<ComplicationType, ComplicationData>()
            val textUtils = ComplicationTextFormatting(Locale.getDefault())
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == TAG_COMPLICATION) {
                    val type = mapComplicationType(parser.getAttributeValue(null, ATTR_TYPE))
                    val data =
                        parseComplicationTag(
                            parser,
                            type,
                            providerComponent,
                            parserContext,
                            providerContext,
                            textUtils,
                        )
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
        @RequiresPermission("com.google.wear.permission.SET_COMPLICATION_EXTRAS")
        private fun parseComplicationTag(
            parser: XmlResourceParser,
            type: ComplicationType,
            providerComponent: ComponentName?,
            parserContext: Context,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): ComplicationData? {
            return when (type) {
                ComplicationType.SHORT_TEXT ->
                    parseShortTextComplication(
                        parser,
                        providerComponent,
                        parserContext,
                        providerContext,
                        textUtils,
                    )
                ComplicationType.LONG_TEXT ->
                    parseLongTextComplication(
                        parser,
                        providerComponent,
                        parserContext,
                        providerContext,
                        textUtils,
                    )
                ComplicationType.RANGED_VALUE ->
                    parseRangedValueComplication(
                        parser,
                        providerComponent,
                        parserContext,
                        providerContext,
                        textUtils,
                    )
                ComplicationType.GOAL_PROGRESS ->
                    parseGoalProgressComplication(
                        parser,
                        providerComponent,
                        parserContext,
                        providerContext,
                        textUtils,
                    )
                ComplicationType.WEIGHTED_ELEMENTS ->
                    parseWeightedElementsComplication(
                        parser,
                        providerComponent,
                        parserContext,
                        providerContext,
                        textUtils,
                    )
                ComplicationType.MONOCHROMATIC_IMAGE ->
                    parseMonochromaticImageComplication(parser, providerComponent, providerContext)
                ComplicationType.SMALL_IMAGE ->
                    parseSmallImageComplication(parser, providerComponent, providerContext)
                else -> {
                    skip(parser)
                    null
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        @RequiresPermission("com.google.wear.permission.SET_COMPLICATION_EXTRAS")
        private fun parseShortTextComplication(
            parser: XmlResourceParser,
            providerComponent: ComponentName?,
            parserContext: Context,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): ShortTextComplicationData {
            val monochromaticImage = parseMonochromaticImage(parser, providerContext)
            val smallImage = parseSmallImage(parser, providerContext)
            var text: ComplicationText? = null
            var title: ComplicationText? = null
            val extras = PersistableBundle()

            parseChildren(parser, parserContext, providerContext, textUtils, extras) {
                tagName,
                textContent ->
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
                .setExtras(extras)
                .setDataSource(providerComponent)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        @RequiresPermission("com.google.wear.permission.SET_COMPLICATION_EXTRAS")
        private fun parseLongTextComplication(
            parser: XmlResourceParser,
            providerComponent: ComponentName?,
            parserContext: Context,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): LongTextComplicationData {
            val monochromaticImage = parseMonochromaticImage(parser, providerContext)
            val smallImage = parseSmallImage(parser, providerContext)
            var text: ComplicationText? = null
            var title: ComplicationText? = null
            val extras = PersistableBundle()

            parseChildren(parser, parserContext, providerContext, textUtils, extras) {
                tagName,
                textContent ->
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
                .setExtras(extras)
                .setDataSource(providerComponent)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        @RequiresPermission("com.google.wear.permission.SET_COMPLICATION_EXTRAS")
        private fun parseRangedValueComplication(
            parser: XmlResourceParser,
            providerComponent: ComponentName?,
            parserContext: Context,
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
            val extras = PersistableBundle()

            parseChildren(parser, parserContext, providerContext, textUtils, extras) {
                tagName,
                textContent ->
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
                .setExtras(extras)
                .setDataSource(providerComponent)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        @RequiresPermission("com.google.wear.permission.SET_COMPLICATION_EXTRAS")
        private fun parseGoalProgressComplication(
            parser: XmlResourceParser,
            providerComponent: ComponentName?,
            parserContext: Context,
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
            val extras = PersistableBundle()

            parseChildren(parser, parserContext, providerContext, textUtils, extras) {
                tagName,
                textContent ->
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
                .setExtras(extras)
                .setDataSource(providerComponent)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        @RequiresPermission("com.google.wear.permission.SET_COMPLICATION_EXTRAS")
        private fun parseWeightedElementsComplication(
            parser: XmlResourceParser,
            providerComponent: ComponentName?,
            parserContext: Context,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): WeightedElementsComplicationData {
            val elementBackgroundColor =
                getResourceIdFromAttribute(parser, ATTR_BACKGROUND_COLOR, providerContext).let {
                    if (it != 0) providerContext.getColor(it) else Color.TRANSPARENT
                }
            val monochromaticImage = parseMonochromaticImage(parser, providerContext)
            val smallImage = parseSmallImage(parser, providerContext)
            var text: ComplicationText? = null
            var title: ComplicationText? = null
            val elements = mutableListOf<WeightedElementsComplicationData.Element>()
            val extras = PersistableBundle()

            parseChildren(
                parser,
                parserContext,
                providerContext,
                textUtils,
                extras,
                onNonCommonTag = {
                    if (it.name == TAG_ELEMENT) {
                        val weight =
                            requireNotNull(getFloatAttribute(it, ATTR_WEIGHT)) {
                                "'weight' attribute is required for element"
                            }
                        require(weight > 0) { "'weight' attribute must be > 0" }

                        val colorRes = getResourceIdFromAttribute(it, ATTR_COLOR, providerContext)
                        require(colorRes != 0) { "'color' attribute is required for element" }
                        val color = providerContext.getColor(colorRes)

                        elements.add(WeightedElementsComplicationData.Element(weight, color))
                        skip(it)
                        true
                    } else {
                        false
                    }
                },
            ) { tagName, textContent ->
                when (tagName) {
                    TAG_TEXT -> text = textContent.let { PlainComplicationText.Builder(it).build() }
                    TAG_TITLE ->
                        title = textContent.let { PlainComplicationText.Builder(it).build() }
                }
            }

            return WeightedElementsComplicationData.Builder(
                    elements,
                    contentDescription = ComplicationText.EMPTY,
                )
                .setElementBackgroundColor(elementBackgroundColor)
                .setText(text)
                .setTitle(title)
                .setMonochromaticImage(monochromaticImage)
                .setSmallImage(smallImage)
                .setExtras(extras)
                .setDataSource(providerComponent)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseMonochromaticImageComplication(
            parser: XmlResourceParser,
            providerComponent: ComponentName?,
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
                .setDataSource(providerComponent)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseSmallImageComplication(
            parser: XmlResourceParser,
            providerComponent: ComponentName?,
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
                .setDataSource(providerComponent)
                .build()
        }

        /**
         * Parses children of a complication tag.
         *
         * Such tags can be common across different complication types such as text, title,
         * extended-data, etc. or specific to a particular complication type.
         *
         * Handling of non-common tags is delegated back as is to the caller through onNonCommonTag
         * callback, where if the delegate returns true [parsing succeeded], we move to next child.
         *
         * Text tags such as title and text and parsed into their final string representation then
         * passed back to the caller through onTextResolved.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Throws(IOException::class, XmlPullParserException::class)
        private fun parseChildren(
            parser: XmlResourceParser,
            parserContext: Context,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
            extras: PersistableBundle,
            onNonCommonTag: ((XmlResourceParser) -> Boolean)? = null,
            onTextResolved: (String, String) -> Unit,
        ) {
            while (parser.next() != XmlPullParser.END_TAG || parser.name != TAG_COMPLICATION) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }

                if (onNonCommonTag != null && onNonCommonTag(parser)) {
                    continue
                }

                val tagName = parser.name

                if (tagName == TAG_EXTENDED_DATA) {
                    parseExtendedData(parser, parserContext, providerContext, textUtils, extras)
                    continue
                }

                val textContent =
                    parseTextElement(parser, parserContext, providerContext, textUtils)
                if (textContent != null) {
                    onTextResolved(tagName, textContent)
                } else {
                    skip(parser)
                }
            }
        }

        private fun parseExtendedData(
            parser: XmlResourceParser,
            parserContext: Context,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
            extras: PersistableBundle,
        ) {
            val key = parser.getAttributeValue(null, ATTR_KEY)
            val skeletonValue = parser.getAttributeValue(null, ATTR_SKELETON_VALUE)
            val dictionaryKey = parser.getAttributeValue(null, ATTR_DICTIONARY_KEY)

            if (key != null && skeletonValue != null) {
                extras.putString(key, skeletonValue)
            }

            if (dictionaryKey != null) {
                val replacements = PersistableBundle()
                while (parser.next() != XmlPullParser.END_TAG || parser.name != TAG_EXTENDED_DATA) {
                    if (parser.eventType != XmlPullParser.START_TAG) {
                        continue
                    }
                    if (parser.name == TAG_STRING_REPLACEMENT) {
                        val replacementKey = parser.getAttributeValue(null, ATTR_KEY)
                        val replacementText =
                            parseInnerTextElements(
                                parser,
                                parserContext,
                                providerContext,
                                textUtils,
                                TAG_STRING_REPLACEMENT,
                            )
                        if (replacementKey != null && replacementText != null) {
                            replacements.putString(replacementKey, replacementText)
                        }
                    } else {
                        skip(parser)
                    }
                }
                extras.putPersistableBundle(dictionaryKey, replacements)
            } else {
                skip(parser)
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
            parserContext: Context,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): String? {
            return parseInnerTextElements(
                parser,
                parserContext,
                providerContext,
                textUtils,
                parser.name,
            )
        }

        private fun parseInnerTextElements(
            parser: XmlResourceParser,
            parserContext: Context,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
            parentTagName: String,
        ): String? {
            var text: String? = null
            while (parser.next() != XmlPullParser.END_TAG || parser.name != parentTagName) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                text =
                    when (parser.name) {
                        TAG_PLAIN -> parsePlainTextToString(parser, providerContext)
                        TAG_FORMATTED ->
                            parseFormattedText(parser, parserContext, providerContext, textUtils)
                        TAG_TIME -> parseTimeText(parser, parserContext, providerContext, textUtils)
                        TAG_DATE -> parseDateText(parser, parserContext, textUtils)
                        TAG_TIME_DIFFERENCE -> parseTimeDifferenceText(parser, parserContext)
                        else -> null
                    }
            }
            return text
        }

        /**
         * Returns string representation of plain text.
         *
         * Numbers will be formatted into text using the default locale.
         */
        private fun parsePlainTextToString(
            parser: XmlResourceParser,
            providerContext: Context,
        ): String? {
            return when (val plainText = parsePlainTextToAny(parser, providerContext)) {
                is Number -> {
                    NumberFormat.getInstance(Locale.getDefault()).format(plainText)
                }

                else -> plainText.toString()
            }
        }

        /**
         * Parses plain text and keeps its type.
         *
         * Number entries for example will retain their types.
         */
        private fun parsePlainTextToAny(parser: XmlResourceParser, providerContext: Context): Any? {
            val attributeValue = parser.getAttributeValue(null, ATTR_VALUE) ?: return null
            val valueType = parser.getAttributeValue(null, ATTR_VALUE_TYPE)

            if (attributeValue.startsWith("@")) {
                return resolveTextResource(providerContext, attributeValue)
            }

            return when (valueType) {
                PLAIN_TEXT_TYPE_INT -> attributeValue.toIntOrNull()
                PLAIN_TEXT_TYPE_LONG -> attributeValue.toLongOrNull()
                PLAIN_TEXT_TYPE_FLOAT -> attributeValue.toDoubleOrNull()
                else -> attributeValue
            }
        }

        /**
         * Resolves a string from a resource identifier (i.e., "@string/name", "@integer/number").
         */
        private fun resolveTextResource(context: Context, resourceIdentifier: String): Any? {
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
                    "integer" -> resources.getInteger(resId)
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
            parserContext: Context,
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
                            parseTimeText(parser, parserContext, providerContext, textUtils)?.let {
                                params.add(it)
                            }
                        TAG_DATE ->
                            parseDateText(parser, parserContext, textUtils)?.let { params.add(it) }
                        TAG_PLAIN ->
                            parsePlainTextToAny(parser, providerContext)?.let { params.add(it) }
                        TAG_TIME_DIFFERENCE ->
                            parseTimeDifferenceText(parser, parserContext)?.let { params.add(it) }
                    }
                    parser.nextTag() // Move to the param type's end tag
                }
            }
            return String.format(formatString, *params.toTypedArray())
        }

        private fun parseTimeText(
            parser: XmlResourceParser,
            parserContext: Context,
            providerContext: Context,
            textUtils: ComplicationTextFormatting,
        ): String? {
            val instantStr = parser.getAttributeValue(null, ATTR_INSTANT) ?: return null
            try {
                val instant = Instant.ofEpochSecond(instantStr.toLong())
                val shouldShorten =
                    parser.getAttributeValue(null, ATTR_SHOULD_SHORTEN_AM_PM)?.toBoolean() ?: false
                val timeComponent = parser.getAttributeValue(null, ATTR_TIME_COMPONENT)

                require(timeComponent == null || !shouldShorten) {
                    "shouldShortenAmPm should not be used when timeComponent is specified"
                }

                val timePattern =
                    if (shouldShorten) {
                        if (DateFormat.is24HourFormat(providerContext)) {
                            textUtils.shortTextTimeFormat24Hour
                        } else {
                            textUtils.shortTextTimeFormat12Hour
                        }
                    } else {
                        if (DateFormat.is24HourFormat(providerContext)) {
                            textUtils.shortTextTimeFormat24HourWithoutAmPmShortening
                        } else {
                            textUtils.shortTextTimeFormat12HourWithoutAmPmShortening
                        }
                    }

                val format = requireNotNull(timePattern) { "Invalid time pattern" }

                if (timeComponent != null) {
                    return when (timeComponent) {
                        TIME_COMPONENT_TIME_ONLY -> {
                            val timeOnlyFormat = format.replace("a", "").trim()
                            DateFormat.format(timeOnlyFormat, Date.from(instant)).toString()
                        }
                        TIME_COMPONENT_AM_PM_ONLY -> {
                            DateFormat.format("a", Date.from(instant)).toString()
                        }
                        else -> {
                            Log.w(TAG, "Unknown timeComponent: $timeComponent")
                            TimeFormatComplicationText.Builder(format)
                                .setTimeZone(TimeZone.GMT_ZONE)
                                .build()
                                .getTextAt(parserContext.resources, instant)
                                .toString()
                        }
                    }
                }

                return TimeFormatComplicationText.Builder(format)
                    .setTimeZone(TimeZone.GMT_ZONE)
                    .build()
                    .getTextAt(parserContext.resources, instant)
                    .toString()
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid instant format for <time> tag", e)
                return null
            }
        }

        private fun parseDateText(
            parser: XmlResourceParser,
            parserContext: Context,
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
                    .getTextAt(parserContext.resources, instant)
                    .toString()
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid instant format for <date> tag", e)
                return null
            }
        }

        private fun parseTimeDifferenceText(
            parser: XmlResourceParser,
            parserContext: Context,
        ): String? {
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
                    .getTextAt(parserContext.resources, currentInstant)
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
                TYPE_STR_WEIGHTED_ELEMENTS -> ComplicationType.WEIGHTED_ELEMENTS
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
