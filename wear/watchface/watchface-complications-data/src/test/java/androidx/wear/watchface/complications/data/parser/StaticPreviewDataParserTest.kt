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

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.SharedRobolectricTestRunner
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData
import androidx.wear.watchface.complications.data.test.R
import com.google.common.truth.Expect
import java.time.Instant
import java.util.Locale
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.doThrow
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@RunWith(SharedRobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
@SuppressLint("NewApi")
class StaticPreviewDataParserTest {
    @Rule @JvmField val expect = Expect.create()
    @Mock private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        openMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun endToEndParsing_fromComponentName_US() {
        runTestForLocale(Locale.US) { context ->
            val serviceInfo = ServiceInfo()
            serviceInfo.metaData =
                Bundle().apply {
                    putInt(
                        "com.google.android.wearable.complications.STATIC_PREVIEW_DATA",
                        R.xml.static_preview_data_1,
                    )
                }

            whenever(packageManager.getServiceInfo(TEST_PROVIDER, PackageManager.GET_META_DATA))
                .thenReturn(serviceInfo)
            whenever(
                    context.createPackageContext(
                        TEST_PROVIDER.packageName,
                        Context.CONTEXT_IGNORE_SECURITY,
                    )
                )
                .thenReturn(context)

            val previewData = StaticPreviewDataParser.parsePreviewData(context, TEST_PROVIDER)

            expect.that(previewData).isNotNull()
            val complicationData =
                previewData!![ComplicationType.SHORT_TEXT] as ShortTextComplicationData
            expect
                .that(complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0)))
                .isEqualTo("Short")
            expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
        }
    }

    @Test
    @Throws(Exception::class)
    fun endToEndParsing_fromComponentName() {
        runTestForLocale(Locale.US) { context ->
            val serviceInfo = ServiceInfo()
            serviceInfo.metaData =
                Bundle().apply {
                    putInt(
                        "com.google.android.wearable.complications.STATIC_PREVIEW_DATA",
                        R.xml.static_preview_data_extended,
                    )
                }

            packageManager.stub {
                on { getServiceInfo(TEST_PROVIDER, PackageManager.GET_META_DATA) } doReturn
                    serviceInfo
            }
            context.stub {
                on {
                    createPackageContext(TEST_PROVIDER.packageName, Context.CONTEXT_IGNORE_SECURITY)
                } doReturn context
            }

            val previewData = StaticPreviewDataParser.parsePreviewData(context, TEST_PROVIDER)
            val complicationData =
                previewData!![ComplicationType.RANGED_VALUE] as RangedValueComplicationData
            val text = complicationData.text?.getTextAt(context.resources, Instant.ofEpochMilli(0))
            val value = complicationData.value
            val minValue = complicationData.min
            val maxValue = complicationData.max
            val extendedData = complicationData.extras.getString("extended_data_key")
            val extendedDataDictionary =
                complicationData.extras.getPersistableBundle("dictionary_key")

            expect.that(text).isEqualTo("Progress: 20%")
            expect.that(value).isEqualTo(20)
            expect.that(minValue).isEqualTo(10)
            expect.that(maxValue).isEqualTo(30)
            expect.that(extendedData).isEqualTo("GiAKDgoCNTAiCG1pblZhbHVlEg4KAjcwIghtYXhWYWx1ZQ==")
            expect.that(extendedDataDictionary?.size()).isEqualTo(2)
            expect.that(extendedDataDictionary?.getString("minValue")).isEqualTo("Progress: 10%")
            expect.that(extendedDataDictionary?.getString("maxValue")).isEqualTo("Progress: 30%")
            expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
        }
    }

    @Test
    @Throws(Exception::class)
    fun endToEndParsing_fromComponentName_DE() {
        runTestForLocale(Locale.GERMANY) { context ->
            val serviceInfo = ServiceInfo()
            serviceInfo.metaData =
                Bundle().apply {
                    putInt(
                        "com.google.android.wearable.complications.STATIC_PREVIEW_DATA",
                        R.xml.static_preview_data_formatted,
                    )
                }

            whenever(packageManager.getServiceInfo(TEST_PROVIDER, PackageManager.GET_META_DATA))
                .thenReturn(serviceInfo)
            whenever(
                    context.createPackageContext(
                        TEST_PROVIDER.packageName,
                        Context.CONTEXT_IGNORE_SECURITY,
                    )
                )
                .thenReturn(context)

            val previewData = StaticPreviewDataParser.parsePreviewData(context, TEST_PROVIDER)

            expect.that(previewData).isNotNull()
            val complicationData =
                previewData!![ComplicationType.SHORT_TEXT] as ShortTextComplicationData
            val text = complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
            expect.that(text).isEqualTo("Steps: 343, Time: 02:40")
            expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
        }
    }

    @Test
    @Throws(Exception::class)
    fun shortTextComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                expect
                    .that(
                        complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                    )
                    .isEqualTo("Short")
                expect
                    .that(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("Title")
                expect.that(complicationData.monochromaticImage).isNotNull()
                expect.that(complicationData.smallImage).isNotNull()
                expect.that(complicationData.smallImage!!.type).isEqualTo(SmallImageType.ICON)
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun longTextComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.LONG_TEXT] as LongTextComplicationData
                expect
                    .that(
                        complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                    )
                    .isEqualTo("Long Text")
                expect
                    .that(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("Long Title")
                expect.that(complicationData.monochromaticImage).isNotNull()
                expect.that(complicationData.smallImage).isNotNull()
                expect.that(complicationData.smallImage!!.type).isEqualTo(SmallImageType.PHOTO)
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun rangedValueComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.RANGED_VALUE] as RangedValueComplicationData
                expect.that(complicationData.value).isEqualTo(75f)
                expect.that(complicationData.min).isEqualTo(0f)
                expect.that(complicationData.max).isEqualTo(100f)
                expect
                    .that(
                        complicationData.text!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("75")
                expect
                    .that(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("Level")
                expect.that(complicationData.monochromaticImage).isNotNull()
                expect.that(complicationData.smallImage).isNull()
                expect.that(complicationData.colorRamp).isNotNull()
                expect
                    .that(complicationData.colorRamp!!.colors)
                    .isEqualTo(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                expect.that(complicationData.colorRamp.interpolated).isTrue()
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun goalProgressComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.GOAL_PROGRESS] as GoalProgressComplicationData
                expect.that(complicationData.value).isEqualTo(1200f)
                expect.that(complicationData.targetValue).isEqualTo(10000f)
                expect
                    .that(
                        complicationData.text!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("1.2k")
                expect
                    .that(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("Steps")
                expect.that(complicationData.monochromaticImage).isNotNull()
                expect.that(complicationData.smallImage).isNull()
                expect.that(complicationData.colorRamp).isNotNull()
                expect
                    .that(complicationData.colorRamp!!.colors)
                    .isEqualTo(intArrayOf(Color.YELLOW, Color.MAGENTA))
                expect.that(complicationData.colorRamp.interpolated).isFalse()
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun monochromaticImageComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.MONOCHROMATIC_IMAGE]
                        as MonochromaticImageComplicationData
                expect.that(complicationData.monochromaticImage).isNotNull()
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun smallImageComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.SMALL_IMAGE] as SmallImageComplicationData
                expect.that(complicationData.smallImage).isNotNull()
                expect.that(complicationData.smallImage.type).isEqualTo(SmallImageType.PHOTO)
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun monochromaticImageComplication_withThemeAttr() {
        runTestForLocale(Locale.US) { context ->
            context.setTheme(R.style.TestTheme)
            context.resources.getXml(R.xml.static_preview_data_attr).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.MONOCHROMATIC_IMAGE]
                        as MonochromaticImageComplicationData
                expect.that(complicationData.monochromaticImage).isNotNull()
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun timeDifferenceComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_time_diff).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                expect.that(text).isEqualTo("4d")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
        runTestForLocale(Locale.GERMANY) { context ->
            context.resources.getXml(R.xml.static_preview_data_time_diff).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                expect.that(text).isEqualTo("4 Tage")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun formattedTextComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_formatted).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                expect.that(text).isEqualTo("Steps: 343, Time: 2:40AM")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
        runTestForLocale(Locale.GERMANY) { context ->
            context.resources.getXml(R.xml.static_preview_data_formatted).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                expect.that(text).isEqualTo("Steps: 343, Time: 02:40")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun formattedTextComplicationWithNumberFormat() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_formatted).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.LONG_TEXT] as LongTextComplicationData
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                expect.that(text).isEqualTo("Steps: 343, Time: 2:40AM, Progress: 10%")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
        runTestForLocale(Locale.GERMANY) { context ->
            context.resources.getXml(R.xml.static_preview_data_formatted).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.LONG_TEXT] as LongTextComplicationData
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                expect.that(text).isEqualTo("Steps: 343, Time: 02:40, Progress: 10%")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun integerResourceComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_2).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.RANGED_VALUE] as RangedValueComplicationData
                expect
                    .that(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("123")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun longIntegerResourceComplication() {
        runTestForLocale(Locale("ar", "SA")) { context ->
            context.resources.getXml(R.xml.static_preview_data_long_number).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.RANGED_VALUE] as RangedValueComplicationData
                expect
                    .that(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("٨٬٤١٨")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun longIntegerResourceComplicationGermany() {
        runTestForLocale(Locale.GERMANY) { context ->
            context.resources.getXml(R.xml.static_preview_data_long_number).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.RANGED_VALUE] as RangedValueComplicationData
                expect
                    .that(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("8.418")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun dateAndTimeFormattingComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_2).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.LONG_TEXT] as LongTextComplicationData

                val dateText =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                val timeText =
                    complicationData.title!!.getTextAt(context.resources, Instant.ofEpochMilli(0))
                expect.that(dateText).isEqualTo("Jan 1")
                expect.that(timeText).isEqualTo("1:01AM")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
        runTestForLocale(Locale.GERMANY) { context ->
            context.resources.getXml(R.xml.static_preview_data_2).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.LONG_TEXT] as LongTextComplicationData

                val dateText =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                val timeText =
                    complicationData.title!!.getTextAt(context.resources, Instant.ofEpochMilli(0))
                expect.that(dateText).isEqualTo("01.01.")
                expect.that(timeText).isEqualTo("01:01")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun timeComponentComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_time_component).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val shortTextComplication =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                val longTextComplication =
                    previewData[ComplicationType.LONG_TEXT] as LongTextComplicationData

                val timeOnlyText =
                    shortTextComplication.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                val amPmOnlyText =
                    longTextComplication.text.getTextAt(context.resources, Instant.ofEpochMilli(0))

                expect.that(timeOnlyText).isEqualTo("1:01")
                expect.that(amPmOnlyText).isEqualTo("AM")
                expect.that(shortTextComplication.dataSource).isEqualTo(TEST_PROVIDER)
                expect.that(longTextComplication.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun timeComponentAndShortenAmPmThrows() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_time_component_invalid).use { parser
                ->
                assertThrows(IllegalArgumentException::class.java) {
                    PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun inflate_internalOverloadWithDistinguishedContexts_reliesOnParserContextForGetTextAt() {
        runTestForLocale(Locale.UK) { parserContext ->
            val providerContext = spy(parserContext)
            providerContext.stub { on { resources } doReturn null }

            parserContext.resources.getXml(R.xml.static_preview_data_time_diff).use { parser ->
                val previewData =
                    PreviewData.inflate(
                        TEST_PROVIDER,
                        parserContext = parserContext,
                        providerContext = providerContext,
                        parser = parser,
                    )
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                val text =
                    complicationData.text.getTextAt(
                        parserContext.resources,
                        Instant.ofEpochMilli(0),
                    )

                expect.that(text.toString()).isEqualTo("4d")
                expect.that(previewData).isNotNull()
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun parsePreviewData_usesProviderContextToParseXml() {
        runTestForLocale(Locale.US) { parserContext ->
            val providerContext = spy(parserContext)

            val provider = ComponentName(providerContext, "TestProvider")
            whenever(
                    parserContext.createPackageContext(
                        provider.packageName,
                        Context.CONTEXT_IGNORE_SECURITY,
                    )
                )
                .thenReturn(providerContext)
            parserContext.stub { on { resources } doReturn null }
            val serviceInfo =
                ServiceInfo().apply {
                    metaData =
                        Bundle().apply {
                            putInt(
                                "com.google.android.wearable.complications.STATIC_PREVIEW_DATA",
                                R.xml.static_preview_data_1,
                            )
                        }
                }
            whenever(packageManager.getServiceInfo(TEST_PROVIDER, PackageManager.GET_META_DATA))
                .thenReturn(serviceInfo)

            val previewData = StaticPreviewDataParser.parsePreviewData(parserContext, provider)

            expect.that(previewData).isNotNull()
            expect
                .that(previewData!![ComplicationType.SHORT_TEXT])
                .isInstanceOf(ShortTextComplicationData::class.java)
            expect
                .that(previewData[ComplicationType.SHORT_TEXT]!!.dataSource)
                .isEqualTo(TEST_PROVIDER)
        }
    }

    @Test
    @Throws(Exception::class)
    fun inflate_internalOverload_usesSelfContextForQuantityStrings() {
        runTestForLocale(Locale.UK) { context ->
            // Create a Provider context that would fail handling getQuantityString [used in
            // getTextAt]
            val providerContext = spy(context)
            val brokenResources = spy(context.resources)
            whenever(providerContext.resources).thenReturn(brokenResources)
            doThrow(IllegalStateException("An exception that shouldn't happen"))
                .whenever(brokenResources)
                .getQuantityString(anyInt(), anyInt(), any())
            context.resources.getXml(R.xml.static_preview_data_time_diff).use { parser ->
                val previewData =
                    PreviewData.inflate(
                        TEST_PROVIDER,
                        parserContext = context,
                        providerContext = providerContext,
                        parser = parser,
                    )
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                // It shouldn't throw, as is it does not use the intentionally broken provider
                // context for getTextAt
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))

                expect.that(text.toString()).isEqualTo("4d")
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun weightedElementsComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_weighted_element).use { parser ->
                val previewData = PreviewData.inflate(TEST_PROVIDER, context, context, parser)
                val complicationData =
                    previewData[ComplicationType.WEIGHTED_ELEMENTS]
                        as WeightedElementsComplicationData

                expect.that(complicationData.elements).hasSize(3)
                expect.that(complicationData.elements[0].weight).isEqualTo(1f)
                expect.that(complicationData.elements[0].color).isEqualTo(Color.RED)
                expect.that(complicationData.elements[1].weight).isEqualTo(2f)
                expect.that(complicationData.elements[1].color).isEqualTo(Color.GREEN)
                expect.that(complicationData.elements[2].weight).isEqualTo(3f)
                expect.that(complicationData.elements[2].color).isEqualTo(Color.BLUE)
                expect.that(complicationData.elementBackgroundColor).isEqualTo(Color.YELLOW)
                expect
                    .that(
                        complicationData.text!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("Text")
                expect
                    .that(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("Title")
                expect.that(complicationData.monochromaticImage).isNotNull()
                expect.that(complicationData.smallImage).isNull()
                expect.that(complicationData.dataSource).isEqualTo(TEST_PROVIDER)
            }
        }
    }

    private fun runTestForLocale(locale: Locale, testLogic: (Context) -> Unit) {
        Locale.setDefault(locale)
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        val localeContext = baseContext.createConfigurationContext(config)
        val finalContext = spy(localeContext)
        whenever(finalContext.packageManager).thenReturn(packageManager)
        testLogic(finalContext)
    }

    private companion object {
        val TEST_PROVIDER =
            ComponentName(ApplicationProvider.getApplicationContext<Context>(), "TestProvider")
    }
}
