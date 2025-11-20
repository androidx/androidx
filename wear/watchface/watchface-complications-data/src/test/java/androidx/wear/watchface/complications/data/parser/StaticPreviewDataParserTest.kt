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
import androidx.wear.watchface.complications.data.test.R
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.Locale
import kotlin.use
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(SharedRobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
@SuppressLint("NewApi")
class StaticPreviewDataParserTest {
    @Mock private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        openMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun endToEndParsing_fromComponentName_US() {
        runTestForLocale(Locale.US) { context ->
            val provider = ComponentName(context, "TestProvider")
            val serviceInfo = ServiceInfo()
            serviceInfo.metaData =
                Bundle().apply {
                    putInt(
                        "com.google.android.wearable.complications.STATIC_PREVIEW_DATA",
                        R.xml.static_preview_data_1,
                    )
                }

            whenever(packageManager.getServiceInfo(provider, PackageManager.GET_META_DATA))
                .thenReturn(serviceInfo)
            whenever(
                    context.createPackageContext(
                        provider.packageName,
                        Context.CONTEXT_IGNORE_SECURITY,
                    )
                )
                .thenReturn(context)

            val previewData = StaticPreviewDataParser.parsePreviewData(context, provider)

            assertThat(previewData).isNotNull()
            val complicationData =
                previewData!![ComplicationType.SHORT_TEXT] as ShortTextComplicationData
            assertThat(complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0)))
                .isEqualTo("Short")
        }
    }

    @Test
    @Throws(Exception::class)
    fun endToEndParsing_fromComponentName_DE() {
        runTestForLocale(Locale.GERMANY) { context ->
            val provider = ComponentName(context, "TestProvider")
            val serviceInfo = ServiceInfo()
            serviceInfo.metaData =
                Bundle().apply {
                    putInt(
                        "com.google.android.wearable.complications.STATIC_PREVIEW_DATA",
                        R.xml.static_preview_data_formatted,
                    )
                }

            whenever(packageManager.getServiceInfo(provider, PackageManager.GET_META_DATA))
                .thenReturn(serviceInfo)
            whenever(
                    context.createPackageContext(
                        provider.packageName,
                        Context.CONTEXT_IGNORE_SECURITY,
                    )
                )
                .thenReturn(context)

            val previewData = StaticPreviewDataParser.parsePreviewData(context, provider)

            assertThat(previewData).isNotNull()
            val complicationData =
                previewData!![ComplicationType.SHORT_TEXT] as ShortTextComplicationData
            val text = complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
            assertThat(text).isEqualTo("Steps: 10, Time: 02:40")
        }
    }

    @Test
    @Throws(Exception::class)
    fun shortTextComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                assertThat(
                        complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                    )
                    .isEqualTo("Short")
                assertThat(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("Title")
                assertThat(complicationData.monochromaticImage).isNotNull()
                assertThat(complicationData.smallImage).isNotNull()
                assertThat(complicationData.smallImage!!.type).isEqualTo(SmallImageType.ICON)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun longTextComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.LONG_TEXT] as LongTextComplicationData
                assertThat(
                        complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                    )
                    .isEqualTo("Long Text")
                assertThat(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("Long Title")
                assertThat(complicationData.monochromaticImage).isNotNull()
                assertThat(complicationData.smallImage).isNotNull()
                assertThat(complicationData.smallImage!!.type).isEqualTo(SmallImageType.PHOTO)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun rangedValueComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.RANGED_VALUE] as RangedValueComplicationData
                assertThat(complicationData.value).isEqualTo(75f)
                assertThat(complicationData.min).isEqualTo(0f)
                assertThat(complicationData.max).isEqualTo(100f)
                assertThat(
                        complicationData.text!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("75")
                assertThat(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("Level")
                assertThat(complicationData.monochromaticImage).isNotNull()
                assertThat(complicationData.smallImage).isNull()
                assertThat(complicationData.colorRamp).isNotNull()
                assertThat(complicationData.colorRamp!!.colors)
                    .isEqualTo(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                assertThat(complicationData.colorRamp.interpolated).isTrue()
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun goalProgressComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.GOAL_PROGRESS] as GoalProgressComplicationData
                assertThat(complicationData.value).isEqualTo(1200f)
                assertThat(complicationData.targetValue).isEqualTo(10000f)
                assertThat(
                        complicationData.text!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("1.2k")
                assertThat(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("Steps")
                assertThat(complicationData.monochromaticImage).isNotNull()
                assertThat(complicationData.smallImage).isNull()
                assertThat(complicationData.colorRamp).isNotNull()
                assertThat(complicationData.colorRamp!!.colors)
                    .isEqualTo(intArrayOf(Color.YELLOW, Color.MAGENTA))
                assertThat(complicationData.colorRamp.interpolated).isFalse()
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun monochromaticImageComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.MONOCHROMATIC_IMAGE]
                        as MonochromaticImageComplicationData
                assertThat(complicationData.monochromaticImage).isNotNull()
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun smallImageComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_1).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.SMALL_IMAGE] as SmallImageComplicationData
                assertThat(complicationData.smallImage).isNotNull()
                assertThat(complicationData.smallImage.type).isEqualTo(SmallImageType.PHOTO)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun monochromaticImageComplication_withThemeAttr() {
        runTestForLocale(Locale.US) { context ->
            context.setTheme(R.style.TestTheme)
            context.resources.getXml(R.xml.static_preview_data_attr).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.MONOCHROMATIC_IMAGE]
                        as MonochromaticImageComplicationData
                assertThat(complicationData.monochromaticImage).isNotNull()
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun timeDifferenceComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_time_diff).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                assertThat(text).isEqualTo("4d")
            }
        }
        runTestForLocale(Locale.GERMANY) { context ->
            context.resources.getXml(R.xml.static_preview_data_time_diff).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                assertThat(text).isEqualTo("4 Tage")
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun formattedTextComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_formatted).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                assertThat(text).isEqualTo("Steps: 10, Time: 2:40AM")
            }
        }
        runTestForLocale(Locale.GERMANY) { context ->
            context.resources.getXml(R.xml.static_preview_data_formatted).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.SHORT_TEXT] as ShortTextComplicationData
                val text =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                assertThat(text).isEqualTo("Steps: 10, Time: 02:40")
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun integerResourceComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_2).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.RANGED_VALUE] as RangedValueComplicationData
                assertThat(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("123")
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun longIntegerResourceComplication() {
        runTestForLocale(Locale("ar", "SA")) { context ->
            context.resources.getXml(R.xml.static_preview_data_long_number).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.RANGED_VALUE] as RangedValueComplicationData
                assertThat(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("٨٬٤١٨")
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun longIntegerResourceComplicationGermany() {
        runTestForLocale(Locale.GERMANY) { context ->
            context.resources.getXml(R.xml.static_preview_data_long_number).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.RANGED_VALUE] as RangedValueComplicationData
                assertThat(
                        complicationData.title!!.getTextAt(
                            context.resources,
                            Instant.ofEpochMilli(0),
                        )
                    )
                    .isEqualTo("8.418")
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun dateAndTimeFormattingComplication() {
        runTestForLocale(Locale.US) { context ->
            context.resources.getXml(R.xml.static_preview_data_2).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.LONG_TEXT] as LongTextComplicationData

                val dateText =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                val timeText =
                    complicationData.title!!.getTextAt(context.resources, Instant.ofEpochMilli(0))
                assertThat(dateText).isEqualTo("Jan 1")
                assertThat(timeText).isEqualTo("1:01AM")
            }
        }
        runTestForLocale(Locale.GERMANY) { context ->
            context.resources.getXml(R.xml.static_preview_data_2).use { parser ->
                val previewData = PreviewData.inflate(context, parser)
                val complicationData =
                    previewData[ComplicationType.LONG_TEXT] as LongTextComplicationData

                val dateText =
                    complicationData.text.getTextAt(context.resources, Instant.ofEpochMilli(0))
                val timeText =
                    complicationData.title!!.getTextAt(context.resources, Instant.ofEpochMilli(0))
                assertThat(dateText).isEqualTo("01.01.")
                assertThat(timeText).isEqualTo("01:01")
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
}
