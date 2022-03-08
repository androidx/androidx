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

package androidx.wear.watchface.client.test

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.XmlResourceParser
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.client.WatchFaceMetadataClient
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
public class WatchFaceMetadataServiceTest {
    private val exampleWatchFaceComponentName = ComponentName(
        "androidx.wear.watchface.client.test",
        "androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService"
    )
    private val nopCanvasWatchFaceServiceComponentName = ComponentName(
        "androidx.wear.watchface.client.test",
        "androidx.wear.watchface.client.test.TestNopCanvasWatchFaceService"
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun createWatchFaceMetadataClient(componentName: ComponentName) = runBlocking {
        WatchFaceMetadataClient.createImpl(
            context,
            Intent(context, WatchFaceControlTestService::class.java).apply {
                action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
            },
            componentName,
            object : WatchFaceMetadataClient.Companion.ParserProvider() {
                override fun getParser(
                    context: Context,
                    watchFaceName: ComponentName
                ): XmlResourceParser? = null
            }
        )
    }

    @Before
    public fun setUp() {
        WatchFaceControlTestService.apiVersionOverride = null
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun userStyleSchema() {
        val client = createWatchFaceMetadataClient(exampleWatchFaceComponentName)
        val schema = client.getUserStyleSchema()

        Truth.assertThat(schema.userStyleSettings.size).isEqualTo(5)
        Truth.assertThat(schema.userStyleSettings[0].id.value).isEqualTo(
            "color_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[1].id.value).isEqualTo(
            "draw_hour_pips_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[2].id.value).isEqualTo(
            "watch_hand_length_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[3].id.value).isEqualTo(
            "complications_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[4].id.value).isEqualTo(
            "hours_draw_freq_style_setting"
        )

        Truth.assertThat(client.isUserStyleSchemaStatic).isFalse()
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun userStyleSchema_oldApi() {
        WatchFaceControlTestService.apiVersionOverride = 1
        val client = createWatchFaceMetadataClient(exampleWatchFaceComponentName)
        val schema = client.getUserStyleSchema()

        Truth.assertThat(schema.userStyleSettings.size).isEqualTo(5)
        Truth.assertThat(schema.userStyleSettings[0].id.value).isEqualTo(
            "color_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[1].id.value).isEqualTo(
            "draw_hour_pips_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[2].id.value).isEqualTo(
            "watch_hand_length_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[3].id.value).isEqualTo(
            "complications_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[4].id.value).isEqualTo(
            "hours_draw_freq_style_setting"
        )

        Truth.assertThat(client.isUserStyleSchemaStatic).isFalse()
    }

    @Test
    public fun getComplicationSlotMetadataMap() {
        val service = createWatchFaceMetadataClient(exampleWatchFaceComponentName)
        val complicationSlotMetadataMap = service.getComplicationSlotMetadataMap()
        Truth.assertThat(complicationSlotMetadataMap.size).isEqualTo(2)

        val leftComplicationMetadata = complicationSlotMetadataMap[
            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
        ]!!
        Truth.assertThat(
            leftComplicationMetadata.bounds!!.perComplicationTypeBounds[ComplicationType.SHORT_TEXT]
        ).isEqualTo(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        Truth.assertThat(leftComplicationMetadata.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        Truth.assertThat(
            leftComplicationMetadata.defaultDataSourcePolicy.systemDataSourceFallback
        ).isEqualTo(SystemDataSources.DATA_SOURCE_DAY_OF_WEEK)
        Truth.assertThat(
            leftComplicationMetadata.defaultDataSourcePolicy
                .systemDataSourceFallbackDefaultType
        ).isEqualTo(ComplicationType.SHORT_TEXT)
        Truth.assertThat(leftComplicationMetadata.supportedTypes).containsExactly(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )

        val rightComplicationMetadata = complicationSlotMetadataMap[
            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
        ]!!
        Truth.assertThat(
            rightComplicationMetadata.bounds!!.perComplicationTypeBounds[
                ComplicationType.SHORT_TEXT
            ]
        ).isEqualTo(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        Truth.assertThat(rightComplicationMetadata.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        Truth.assertThat(
            rightComplicationMetadata.defaultDataSourcePolicy.systemDataSourceFallback
        ).isEqualTo(SystemDataSources.DATA_SOURCE_STEP_COUNT)
        Truth.assertThat(
            rightComplicationMetadata.defaultDataSourcePolicy
                .systemDataSourceFallbackDefaultType
        ).isEqualTo(ComplicationType.SHORT_TEXT)
        Truth.assertThat(rightComplicationMetadata.supportedTypes).containsExactly(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )
    }

    @Test
    public fun getComplicationSlotMetadataMap_watchFaceWithNoComplications() {
        val service = createWatchFaceMetadataClient(nopCanvasWatchFaceServiceComponentName)
        Truth.assertThat(service.getComplicationSlotMetadataMap()).isEmpty()
    }

    @Test
    public fun userStyleSchema_static_metadata() {
        runBlocking {
            val client = WatchFaceMetadataClient.createImpl(
                context,
                Intent(context, WatchFaceControlTestService::class.java).apply {
                    action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
                },
                exampleWatchFaceComponentName,
                object : WatchFaceMetadataClient.Companion.ParserProvider() {
                    override fun getParser(
                        context: Context,
                        watchFaceName: ComponentName
                    ) = context.resources.getXml(R.xml.xml_watchface)
                }
            )
            val schema = client.getUserStyleSchema()

            Truth.assertThat(
                @Suppress("Deprecation")
                schema.userStyleSettings.toString()
            ).isEqualTo(
                "[{TimeStyle : minimal, seconds}]"
            )

            Truth.assertThat(client.isUserStyleSchemaStatic).isTrue()
        }
    }

    @Test
    public fun getComplicationSlotMetadataMap_static_metadata() {
        runBlocking {
            val client = WatchFaceMetadataClient.createImpl(
                context,
                Intent(context, WatchFaceControlTestService::class.java).apply {
                    action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
                },
                exampleWatchFaceComponentName,
                object : WatchFaceMetadataClient.Companion.ParserProvider() {
                    override fun getParser(
                        context: Context,
                        watchFaceName: ComponentName
                    ) = context.resources.getXml(R.xml.xml_watchface)
                }
            )
            val complications = client.getComplicationSlotMetadataMap()

            Truth.assertThat(complications.keys).containsExactly(10, 20)

            Truth.assertThat(complications[10]!!.boundsType)
                .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)

            Truth.assertThat(complications[10]!!.supportedTypes)
                .containsExactly(
                    ComplicationType.SHORT_TEXT,
                    ComplicationType.RANGED_VALUE,
                    ComplicationType.SMALL_IMAGE
                )

            Truth.assertThat(complications[10]!!.defaultDataSourcePolicy.primaryDataSource)
                .isEqualTo(ComponentName("com.app.example1", "com.app.example1.Class"))

            Truth.assertThat(
                complications[10]!!.defaultDataSourcePolicy.primaryDataSourceDefaultType
            ).isEqualTo(ComplicationType.SHORT_TEXT)

            Truth.assertThat(complications[10]!!.defaultDataSourcePolicy.secondaryDataSource)
                .isEqualTo(ComponentName("com.app.example2", "com.app.example2.Class"))

            Truth.assertThat(
                complications[10]!!.defaultDataSourcePolicy.secondaryDataSourceDefaultType
            ).isEqualTo(ComplicationType.SMALL_IMAGE)

            Truth.assertThat(complications[10]!!.defaultDataSourcePolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.DATA_SOURCE_WATCH_BATTERY)

            Truth.assertThat(
                complications[10]!!.defaultDataSourcePolicy
                    .systemDataSourceFallbackDefaultType
            ).isEqualTo(ComplicationType.RANGED_VALUE)

            Truth.assertThat(
                complications[10]!!.bounds!!.perComplicationTypeBounds[ComplicationType.SHORT_TEXT]
            ).isEqualTo(RectF(0.3f, 0.7f, 0.7f, 0.9f))

            Truth.assertThat(complications[20]!!.boundsType)
                .isEqualTo(ComplicationSlotBoundsType.BACKGROUND)

            Truth.assertThat(complications[20]!!.supportedTypes)
                .containsExactly(ComplicationType.PHOTO_IMAGE)

            Truth.assertThat(complications[20]!!.defaultDataSourcePolicy.primaryDataSource)
                .isEqualTo(ComponentName("com.package", "com.app"))

            Truth.assertThat(
                complications[20]!!.defaultDataSourcePolicy.primaryDataSourceDefaultType
            ).isEqualTo(ComplicationType.PHOTO_IMAGE)

            Truth.assertThat(complications[20]!!.defaultDataSourcePolicy.secondaryDataSource)
                .isNull()

            Truth.assertThat(
                complications[20]!!.defaultDataSourcePolicy.secondaryDataSourceDefaultType
            ).isNull()

            Truth.assertThat(complications[20]!!.defaultDataSourcePolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET)

            Truth.assertThat(
                complications[20]!!.defaultDataSourcePolicy
                    .systemDataSourceFallbackDefaultType
            ).isEqualTo(ComplicationType.PHOTO_IMAGE)
        }
    }
}
